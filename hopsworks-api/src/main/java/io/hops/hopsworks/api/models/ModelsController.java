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

package io.hops.hopsworks.api.models;

import io.hops.hopsworks.api.models.dto.ModelDTO;
import io.hops.hopsworks.api.models.dto.ModelsEndpointDTO;
import io.hops.hopsworks.common.dataset.DatasetController;
import io.hops.hopsworks.common.hdfs.DistributedFileSystemOps;
import io.hops.hopsworks.common.hdfs.Utils;
import io.hops.hopsworks.common.hdfs.xattrs.XAttrsController;
import io.hops.hopsworks.common.jobs.JobController;
import io.hops.hopsworks.common.jupyter.JupyterController;
import io.hops.hopsworks.common.provenance.core.Provenance;
import io.hops.hopsworks.common.provenance.state.ProvStateParamBuilder;
import io.hops.hopsworks.common.provenance.state.ProvStateParser;
import io.hops.hopsworks.common.provenance.state.ProvStateController;
import io.hops.hopsworks.common.provenance.state.dto.ProvStateDTO;
import io.hops.hopsworks.common.util.Settings;
import io.hops.hopsworks.exceptions.DatasetException;
import io.hops.hopsworks.exceptions.JobException;
import io.hops.hopsworks.exceptions.MetadataException;
import io.hops.hopsworks.exceptions.ModelsException;
import io.hops.hopsworks.exceptions.ProvenanceException;
import io.hops.hopsworks.exceptions.ServiceException;
import io.hops.hopsworks.persistence.entity.dataset.Dataset;
import io.hops.hopsworks.persistence.entity.jobs.configuration.spark.SparkJobConfiguration;
import io.hops.hopsworks.persistence.entity.jobs.description.Jobs;
import io.hops.hopsworks.persistence.entity.project.Project;
import io.hops.hopsworks.persistence.entity.user.Users;
import org.apache.hadoop.fs.Path;
import org.apache.parquet.Strings;

import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;
import java.util.stream.Collectors;

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
  
  public void attachModel(DistributedFileSystemOps udfso, Project modelProject, String userFullName,
    ModelDTO modelDTO)
    throws DatasetException, ModelsException, MetadataException {

    modelDTO.setUserFullName(userFullName);
    
    String modelPath = Utils.getProjectPath(modelProject.getName()) + Settings.HOPS_MODELS_DATASET + "/" +
      modelDTO.getName() + "/" + modelDTO.getVersion();
    
    byte[] modelSummaryB = modelConverter.marshalDescription(modelDTO);
    xattrCtrl.upsertProvXAttr(udfso, modelPath, ModelsBuilder.MODEL_SUMMARY_XATTR_NAME, modelSummaryB);
  }

  public ProvStateDTO getModel(Project project, String mlId) throws ProvenanceException {
    ProvStateParamBuilder provFilesParamBuilder = new ProvStateParamBuilder()
      .filterByField(ProvStateParser.FieldsP.PROJECT_I_ID, project.getInode().getId())
      .filterByField(ProvStateParser.FieldsP.ML_TYPE, Provenance.MLType.MODEL.name())
      .filterByField(ProvStateParser.FieldsP.ML_ID, mlId)
      .paginate(0, 1);
    ProvStateDTO fileState = provenanceController.provFileStateList(project, provFilesParamBuilder);
    if (fileState != null) {
      List<ProvStateDTO> experiments = fileState.getItems();
      if (experiments != null && !experiments.isEmpty()) {
        return experiments.iterator().next();
      }
    }
    return null;
  }
  
  public String versionProgram(Accessor accessor, String jobName, String kernelId, String modelName, int modelVersion)
    throws JobException, ServiceException {
    if(!Strings.isNullOrEmpty(jobName)) {
      //model in job
      Jobs experimentJob = jobController.getJob(accessor.experimentProject, jobName);
      SparkJobConfiguration sparkJobConf = (SparkJobConfiguration)experimentJob.getJobConfig();
      String suffix = sparkJobConf.getAppPath().substring(sparkJobConf.getAppPath().lastIndexOf("."));
      String relativePath = Settings.HOPS_MODELS_DATASET + "/" + modelName + "/" + modelVersion + "/program" + suffix;
      Path path = new Path(Utils.getProjectPath(accessor.modelProject.getName()) + relativePath);
      jobController.versionProgram(sparkJobConf, accessor.udfso, path);
      return relativePath;
    } else {
      //model in jupyter
      String relativePath = Settings.HOPS_MODELS_DATASET + "/" + modelName + "/" + modelVersion + "/program.ipynb";
      Path path = new Path(Utils.getProjectPath(accessor.modelProject.getName()) + relativePath);
      jupyterController.versionProgram(accessor.hdfsUser, kernelId, path, accessor.udfso);
      return relativePath;
    }
  }
  
  public List<ModelsEndpointDTO> getModelsEndpoints(Project project) {
    List<Dataset> modelsDatasets = datasetController.getAllByName(project, Settings.HOPS_MODELS_DATASET);
    List<ModelsEndpointDTO> modelsEndpoints = modelsDatasets.stream()
      .map(ModelsEndpointDTO::fromDataset)
      .collect(Collectors.toCollection(ArrayList::new));
    return modelsEndpoints;
  }
  
  public ModelsEndpointDTO getModelsEndpoint(Project project) throws DatasetException {
    return ModelsEndpointDTO.fromDataset(datasetController.getByName(project, Settings.HOPS_MODELS_DATASET));
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