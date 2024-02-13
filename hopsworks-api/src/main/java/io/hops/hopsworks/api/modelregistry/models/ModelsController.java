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
import io.hops.hopsworks.common.jobs.JobController;
import io.hops.hopsworks.common.jupyter.JupyterController;
import io.hops.hopsworks.common.models.ModelFacade;
import io.hops.hopsworks.common.models.version.ModelVersionFacade;
import io.hops.hopsworks.common.python.environment.EnvironmentController;
import io.hops.hopsworks.common.serving.ServingController;
import io.hops.hopsworks.common.serving.ServingWrapper;
import io.hops.hopsworks.common.util.AccessController;
import io.hops.hopsworks.common.util.Settings;
import io.hops.hopsworks.exceptions.CryptoPasswordNotFoundException;
import io.hops.hopsworks.exceptions.DatasetException;
import io.hops.hopsworks.exceptions.JobException;
import io.hops.hopsworks.exceptions.KafkaException;
import io.hops.hopsworks.exceptions.ModelRegistryException;
import io.hops.hopsworks.exceptions.PythonException;
import io.hops.hopsworks.exceptions.ServiceException;
import io.hops.hopsworks.exceptions.ServingException;
import io.hops.hopsworks.persistence.entity.dataset.Dataset;
import io.hops.hopsworks.persistence.entity.hdfs.inode.Inode;
import io.hops.hopsworks.persistence.entity.jobs.configuration.python.PythonJobConfiguration;
import io.hops.hopsworks.persistence.entity.dataset.DatasetType;
import io.hops.hopsworks.persistence.entity.jobs.configuration.spark.SparkJobConfiguration;
import io.hops.hopsworks.persistence.entity.jobs.description.Jobs;
import io.hops.hopsworks.persistence.entity.models.Model;
import io.hops.hopsworks.persistence.entity.models.version.Metrics;
import io.hops.hopsworks.persistence.entity.models.version.ModelVersion;
import io.hops.hopsworks.persistence.entity.project.Project;
import io.hops.hopsworks.persistence.entity.user.Users;
import io.hops.hopsworks.restutils.RESTCodes;
import org.apache.hadoop.fs.Path;

import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.inject.Inject;
import java.util.Date;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

@Stateless
@TransactionAttribute(TransactionAttributeType.NEVER)
public class ModelsController {

  private static final Logger LOGGER = Logger.getLogger(ModelsController.class.getName());

  @EJB
  private JobController jobController;
  @EJB
  private JupyterController jupyterController;
  @EJB
  private DatasetController datasetController;
  @EJB
  private DatasetHelper datasetHelper;
  @EJB
  private AccessController accessCtrl;
  @EJB
  private DatasetController datasetCtrl;
  @EJB
  private ModelFacade modelFacade;
  @EJB
  private ModelVersionFacade modelVersionFacade;
  @EJB
  private ProjectFacade projectFacade;
  @EJB
  private ModelUtils modelUtils;
  @EJB
  private InodeController inodeController;
  @EJB
  private EnvironmentController environmentController;
  @EJB
  private Settings settings;
  @Inject
  private ServingController servingController;

  public ModelVersion createModelVersion(ModelsController.Accessor accessor, ModelDTO modelDTO,
                          String jobName, String kernelId)
    throws JobException, ServiceException, PythonException {

    Model model = modelFacade.findByProjectAndName(accessor.modelProject, modelDTO.getName());
    if(model == null) {
      model = modelFacade.put(accessor.modelProject, modelDTO.getName());
    }

    ModelVersion modelVersion = new ModelVersion();
    modelVersion.setCreated(new Date());
    modelVersion.setDescription(modelDTO.getDescription());
    modelVersion.setEnvironment(modelDTO.getEnvironment());
    modelVersion.setFramework(modelDTO.getFramework());
    modelVersion.setExperimentId(modelVersion.getExperimentId());
    Metrics metrics = new Metrics();
    metrics.setAttributes(modelDTO.getMetrics());
    modelVersion.setMetrics(metrics);
    modelVersion.setExperimentProjectName(modelDTO.getExperimentProjectName());
    modelVersion.setCreator(accessor.user);

    modelVersion.setVersion(modelDTO.getVersion());
    modelVersion.setModel(model);

    //Only attach program and environment if exporting inside Hopsworks
    if (!Strings.isNullOrEmpty(jobName) || !Strings.isNullOrEmpty(kernelId)) {
      modelVersion.setProgram(versionProgram(accessor, jobName, kernelId,
        modelDTO.getName(), modelDTO.getVersion()));
      //Export environment to correct path here
      modelVersion.setEnvironment(environmentController.exportEnv(accessor.experimentProject, accessor.user,
        modelUtils.getModelFullPath(accessor.modelProject, modelDTO.getName(), modelDTO.getVersion()) +
          "/" + Settings.ENVIRONMENT_FILE
      )[0]);
    }

    modelVersionFacade.put(modelVersion);

    return modelVersionFacade.findByProjectAndMlId(model.getId(), modelDTO.getVersion());
  }

  public ModelVersion getModel(Project project, String mlId) throws ModelRegistryException {
    int lastUnderscore = mlId.lastIndexOf("_");
    String[] nameVersionSplit =  {mlId.substring(0, lastUnderscore), mlId.substring(lastUnderscore + 1)};
    Model model = modelFacade.findByProjectAndName(project, nameVersionSplit[0]);
    if(model == null) {
      throw new ModelRegistryException(RESTCodes.ModelRegistryErrorCode.MODEL_NOT_FOUND,
        Level.FINE);
    }
    return modelVersionFacade.findByProjectAndMlId(model.getId(), Integer.valueOf(nameVersionSplit[1]));
  }

  public void delete(Users user, Project userProject, Project parentProject, ModelVersion modelVersion)
    throws DatasetException, ModelRegistryException, KafkaException, ServingException, CryptoPasswordNotFoundException {
    if(userProject.getId().equals(parentProject.getId())) {
      delete(user, userProject, modelVersion);
    } else {
      verifyNoModelDeployments(userProject, modelVersion);

      String modelPath = Utils.getProjectPath(userProject.getName())
        + parentProject.getName() + "::" + Settings.HOPS_MODELS_DATASET + "/" + modelVersion.getModel().getName()
        + "/" + modelVersion.getVersion();
      deleteInternal(user, userProject, modelPath, modelVersion);
    }
  }

  public void delete(Users user, Project project, ModelVersion modelVersion) throws DatasetException,
    ModelRegistryException, KafkaException, ServingException, CryptoPasswordNotFoundException {
    verifyNoModelDeployments(project, modelVersion);

    String modelPath = Utils.getProjectPath(project.getName())
      + Settings.HOPS_MODELS_DATASET + "/" + modelVersion.getModel().getName() + "/" + modelVersion.getVersion();
    deleteInternal(user, project, modelPath, modelVersion);
  }

  private void deleteInternal(Users user, Project project, String path, ModelVersion modelVersion)
    throws DatasetException {
    DatasetPath datasetPath = datasetHelper.getDatasetPath(project, path, DatasetType.DATASET);
    datasetController.delete(project, user, datasetPath.getFullPath(), datasetPath.getDataset(),
      datasetPath.isTopLevelDataset());
    Model model = modelFacade.findByProjectAndName(project, modelVersion.getModel().getName());
    modelVersionFacade.remove(modelVersion);
    if(model.getVersions().isEmpty()) {
      modelFacade.remove(model);
    }
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
  
  public void verifyNoModelDeployments(Project project, ModelVersion modelVersion)
    throws ModelRegistryException, KafkaException, ServingException, CryptoPasswordNotFoundException {
    String[] nameVersionSplit = modelUtils.getModelNameAndVersion(modelVersion.getMlId());
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