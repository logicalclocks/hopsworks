/*
 * This file is part of Hopsworks
 * Copyright (C) 2019, Logical Clocks AB. All rights reserved
 *
 * Hopsworks is free software: you can redistribute it and/or modify it under the terms of
 * the GNU Affero General Public License as published by the Free Software Foundation,
 * either version 3 of the License, or (at your option) any later version.
 *
 * Hopsworks is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR
 * PURPOSE.  See the GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License along with this program.
 * If not, see <https://www.gnu.org/licenses/>.
 */

package io.hops.hopsworks.api.modelregistry.models;

import com.google.common.base.Strings;
import io.hops.hopsworks.api.modelregistry.dto.ModelRegistryDTO;
import io.hops.hopsworks.api.modelregistry.models.dto.ModelDTO;
import io.hops.hopsworks.common.dao.project.ProjectFacade;
import io.hops.hopsworks.common.dataset.DatasetController;
import io.hops.hopsworks.common.dataset.util.DatasetHelper;
import io.hops.hopsworks.common.dataset.util.DatasetPath;
import io.hops.hopsworks.common.hdfs.DistributedFileSystemOps;
import io.hops.hopsworks.common.hdfs.Utils;
import io.hops.hopsworks.common.hdfs.inode.InodeController;
import io.hops.hopsworks.common.hdfs.xattrs.XAttrsController;
import io.hops.hopsworks.common.jobs.JobController;
import io.hops.hopsworks.common.jupyter.JupyterController;
import io.hops.hopsworks.common.provenance.core.Provenance;
import io.hops.hopsworks.common.provenance.state.ProvStateParamBuilder;
import io.hops.hopsworks.common.provenance.state.ProvStateParser;
import io.hops.hopsworks.common.provenance.state.ProvStateController;
import io.hops.hopsworks.common.provenance.state.dto.ProvStateDTO;
import io.hops.hopsworks.common.serving.ServingController;
import io.hops.hopsworks.common.serving.ServingWrapper;
import io.hops.hopsworks.common.util.AccessController;
import io.hops.hopsworks.common.util.Settings;
import io.hops.hopsworks.exceptions.CryptoPasswordNotFoundException;
import io.hops.hopsworks.exceptions.DatasetException;
import io.hops.hopsworks.exceptions.JobException;
import io.hops.hopsworks.exceptions.KafkaException;
import io.hops.hopsworks.exceptions.MetadataException;
import io.hops.hopsworks.exceptions.ModelRegistryException;
import io.hops.hopsworks.exceptions.ProvenanceException;
import io.hops.hopsworks.exceptions.ServiceException;
import io.hops.hopsworks.exceptions.ServingException;
import io.hops.hopsworks.persistence.entity.dataset.Dataset;
import io.hops.hopsworks.persistence.entity.hdfs.inode.Inode;
import io.hops.hopsworks.persistence.entity.jobs.configuration.python.PythonJobConfiguration;
import io.hops.hopsworks.persistence.entity.dataset.DatasetType;
import io.hops.hopsworks.persistence.entity.jobs.configuration.spark.SparkJobConfiguration;
import io.hops.hopsworks.persistence.entity.jobs.description.Jobs;
import io.hops.hopsworks.persistence.entity.project.Project;
import io.hops.hopsworks.persistence.entity.user.Users;
import io.hops.hopsworks.restutils.RESTCodes;
import org.apache.hadoop.fs.Path;
import org.json.JSONObject;

import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.inject.Inject;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import static io.hops.hopsworks.api.modelregistry.models.ModelsBuilder.MODEL_SUMMARY_XATTR_NAME;

@Stateless
@TransactionAttribute(TransactionAttributeType.NEVER)
public class ModelsController {

  private static final Logger LOGGER = Logger.getLogger(ModelsController.class.getName());

  @EJB
  private ProvStateController provenanceController;
  @EJB
  private JobController jobController;
  @EJB
  private JupyterController jupyterController;
  @EJB
  private XAttrsController xattrCtrl;
  @EJB
  private DatasetController datasetController;
  @EJB
  private ModelConverter modelConverter;
  @EJB
  private DatasetHelper datasetHelper;
  @EJB
  private AccessController accessCtrl;
  @EJB
  private DatasetController datasetCtrl;
  @EJB
  private ProjectFacade projectFacade;
  @EJB
  private ModelUtils modelUtils;
  @EJB
  private InodeController inodeController;
  @EJB
  private Settings settings;
  @Inject
  private ServingController servingController;
  
  public void attachModel(DistributedFileSystemOps udfso, Project modelProject, String userFullName,
    ModelDTO modelDTO)
    throws DatasetException, ModelRegistryException, MetadataException {

    modelDTO.setUserFullName(userFullName);
    
    String modelPath = Utils.getProjectPath(modelProject.getName()) + Settings.HOPS_MODELS_DATASET + "/" +
      modelDTO.getName() + "/" + modelDTO.getVersion();
    
    byte[] modelSummaryB = modelConverter.marshalDescription(modelDTO);
    xattrCtrl.upsertProvXAttr(udfso, modelPath, MODEL_SUMMARY_XATTR_NAME, modelSummaryB);
  }

  public ProvStateDTO getModel(Project project, String mlId) throws ProvenanceException {
    Inode projectInode = inodeController.getProjectRoot(project.getName());
    ProvStateParamBuilder provFilesParamBuilder = new ProvStateParamBuilder()
      .filterByField(ProvStateParser.FieldsP.PROJECT_I_ID, projectInode.getId())
      .filterByField(ProvStateParser.FieldsP.ML_TYPE, Provenance.MLType.MODEL.name())
      .filterByField(ProvStateParser.FieldsP.ML_ID, mlId)
      .paginate(0, 1);
    ProvStateDTO fileState = provenanceController.provFileStateList(projectInode, provFilesParamBuilder);
    if (fileState != null) {
      List<ProvStateDTO> experiments = fileState.getItems();
      if (experiments != null && !experiments.isEmpty()) {
        return experiments.iterator().next();
      }
    }
    return null;
  }

  public void delete(Users user, Project userProject, Project parentProject, ProvStateDTO fileState)
    throws DatasetException, ModelRegistryException, KafkaException, ServingException, CryptoPasswordNotFoundException {
    if(userProject.getId().equals(parentProject.getId())) {
      delete(user, userProject, fileState);
    } else {
      verifyNoModelDeployments(userProject, fileState);
      
      JSONObject summary = new JSONObject(fileState.getXattrs().get(MODEL_SUMMARY_XATTR_NAME));
      ModelDTO modelSummary = modelConverter.unmarshalDescription(summary.toString());
      String modelPath = Utils.getProjectPath(userProject.getName())
        + parentProject.getName() + "::" + Settings.HOPS_MODELS_DATASET + "/" + modelSummary.getName()
        + "/" + modelSummary.getVersion();
      deleteInternal(user, userProject, modelPath);
    }
  }

  public void delete(Users user, Project project, ProvStateDTO fileState) throws DatasetException,
    ModelRegistryException, KafkaException, ServingException, CryptoPasswordNotFoundException {
    verifyNoModelDeployments(project, fileState);
    
    JSONObject summary = new JSONObject(fileState.getXattrs().get(MODEL_SUMMARY_XATTR_NAME));
    ModelDTO modelSummary = modelConverter.unmarshalDescription(summary.toString());
    String modelPath = Utils.getProjectPath(project.getName())
      + Settings.HOPS_MODELS_DATASET + "/" + modelSummary.getName() + "/" + modelSummary.getVersion();
    deleteInternal(user, project, modelPath);
  }

  private void deleteInternal(Users user, Project project, String path) throws DatasetException {
    DatasetPath datasetPath = datasetHelper.getDatasetPath(project, path, DatasetType.DATASET);
    datasetController.delete(project, user, datasetPath.getFullPath(), datasetPath.getDataset(),
      datasetPath.isTopLevelDataset());
  }
  
  public String versionProgram(Accessor accessor, String jobName, String kernelId, String modelName, int modelVersion)
    throws JobException, ServiceException {
    if(!Strings.isNullOrEmpty(jobName)) {
      //model in job
      Jobs experimentJob = jobController.getJob(accessor.experimentProject, jobName);
      switch(experimentJob.getJobType()) {
        case SPARK:
        case PYSPARK: {
          SparkJobConfiguration sparkJobConf = (SparkJobConfiguration) experimentJob.getJobConfig();
          String suffix = sparkJobConf.getAppPath().substring(sparkJobConf.getAppPath().lastIndexOf("."));
          String relativePath =
            Settings.HOPS_MODELS_DATASET + "/" + modelName + "/" + modelVersion + "/program" + suffix;
          Path path = new Path(Utils.getProjectPath(accessor.modelProject.getName()) + relativePath);
          jobController.versionProgram(sparkJobConf.getAppPath(), accessor.udfso, path);
          return relativePath;
        }
        case PYTHON: {
          PythonJobConfiguration pythonJobConf = (PythonJobConfiguration) experimentJob.getJobConfig();
          String suffix = pythonJobConf.getAppPath().substring(pythonJobConf.getAppPath().lastIndexOf("."));
          String relativePath =
            Settings.HOPS_MODELS_DATASET + "/" + modelName + "/" + modelVersion + "/program" + suffix;
          Path path = new Path(Utils.getProjectPath(accessor.modelProject.getName()) + relativePath);
          jobController.versionProgram(pythonJobConf.getAppPath(), accessor.udfso, path);
          return relativePath;
        }
        default:
          throw new IllegalArgumentException("cannot version program for job type:" + experimentJob.getJobType());
      }
    } else {
      //model in jupyter
      String relativePath = Settings.HOPS_MODELS_DATASET + "/" + modelName + "/" + modelVersion + "/program.ipynb";
      Path path = new Path(Utils.getProjectPath(accessor.modelProject.getName()) + relativePath);
      jupyterController.versionProgram(accessor.userProject, accessor.user, kernelId, path, accessor.udfso);
      return relativePath;
    }
  }

  public ModelRegistryDTO verifyModelRegistryAccess(Project userProject, Integer modelRegistryId)
          throws ModelRegistryException {

    //Validate existence of model registry project
    Project modelRegistryProject = projectFacade.findById(modelRegistryId)
            .orElseThrow(() -> new ModelRegistryException(RESTCodes.ModelRegistryErrorCode.MODEL_REGISTRY_ID_NOT_FOUND,
            Level.FINE, "No model registry found for ID " + modelRegistryId));

    //Validate existence of models dataset in registry project
    Dataset dataset;
    try {
      dataset = datasetCtrl.getByName(modelRegistryProject, Settings.HOPS_MODELS_DATASET);
    } catch(DatasetException de) {
      throw new ModelRegistryException(RESTCodes.ModelRegistryErrorCode.MODEL_REGISTRY_MODELS_DATASET_NOT_FOUND,
              Level.FINE, "Failed to verify existence of Models dataset in project " + modelRegistryProject.getName(),
              de.getMessage(), de);
    }

    //Validate user project has got access to the models dataset
    if (accessCtrl.hasAccess(userProject, dataset)) {
      Inode modelsDatasetInode = inodeController.getInodeAtPath(Utils.getDatasetPath(dataset, settings).toString());
      return ModelRegistryDTO.fromDataset(modelRegistryProject, modelsDatasetInode);
    } else {
      throw new ModelRegistryException(RESTCodes.ModelRegistryErrorCode.MODEL_REGISTRY_ACCESS_DENIED, Level.FINE,
              "The current project " + userProject.getName() + " does not have access to model registry of project " +
                      dataset.getProject().getName());
    }
  }
  
  public void verifyNoModelDeployments(Project project, ProvStateDTO fileState)
    throws ModelRegistryException, KafkaException, ServingException, CryptoPasswordNotFoundException {
    String[] nameVersionSplit = modelUtils.getModelNameAndVersion(fileState.getMlId());
    List<ServingWrapper> deployments = servingController.getAll(project, nameVersionSplit[0],
      Integer.valueOf(nameVersionSplit[1]), null);
    if (deployments != null && deployments.size() > 0) {
      throw new ModelRegistryException(RESTCodes.ModelRegistryErrorCode.MODEL_CANNOT_BE_DELETED, Level.FINE,
        "The model is used in one or more deployments. Please, delete the deployments before deleting the " +
          "model.");
    }
  }
  
  public static class Accessor {
    public final Users user;
    public final Project userProject;
    public final Project modelProject;
    public final Project experimentProject;
    public final DistributedFileSystemOps udfso;
    public final String hdfsUser;
    
    public Accessor(Users user, Project userProject, Project modelProject, Project experimentProject,
      DistributedFileSystemOps udfso, String hdfsUser) {
      this.user = user;
      this.userProject = userProject;
      this.modelProject = modelProject;
      this.experimentProject = experimentProject;
      this.udfso = udfso;
      this.hdfsUser = hdfsUser;
    }
  }
}