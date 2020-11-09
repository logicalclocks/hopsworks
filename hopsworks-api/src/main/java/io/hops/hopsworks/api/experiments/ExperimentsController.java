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

package io.hops.hopsworks.api.experiments;

import io.hops.hopsworks.api.experiments.dto.ExperimentDTO;
import io.hops.hopsworks.api.experiments.dto.ExperimentsEndpointDTO;
import io.hops.hopsworks.common.dataset.DatasetController;
import io.hops.hopsworks.common.dataset.util.DatasetHelper;
import io.hops.hopsworks.common.dataset.util.DatasetPath;
import io.hops.hopsworks.common.hdfs.DistributedFileSystemOps;
import io.hops.hopsworks.common.hdfs.DistributedFsService;
import io.hops.hopsworks.common.hdfs.HdfsUsersController;
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
import io.hops.hopsworks.exceptions.ExperimentsException;
import io.hops.hopsworks.exceptions.JobException;
import io.hops.hopsworks.exceptions.MetadataException;
import io.hops.hopsworks.exceptions.ProvenanceException;
import io.hops.hopsworks.exceptions.ServiceException;
import io.hops.hopsworks.persistence.entity.dataset.DatasetType;
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
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;
import java.util.stream.Collectors;

@Stateless
@TransactionAttribute(TransactionAttributeType.NEVER)
public class ExperimentsController {

  private static final Logger LOGGER = Logger.getLogger(ExperimentsController.class.getName());

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
  private DatasetHelper datasetHelper;
  @EJB
  private ExperimentConverter experimentConverter;
  @EJB
  private HdfsUsersController hdfsUsersController;
  @EJB
  private DistributedFsService dfs;
  
  public void attachExperiment(Users user, Project project, String id, ExperimentDTO experimentSummary)
    throws DatasetException, ProvenanceException, MetadataException, ExperimentsException {
    
    String usersFullName = user.getFname() + " " + user.getLname();
    experimentSummary.setUserFullName(usersFullName);
    String experimentPath = Utils.getProjectPath(project.getName()) + Settings.HOPS_EXPERIMENTS_DATASET + "/" + id;
    
    if(experimentSummary.getFinished() == null && experimentSummary.getDuration() != null) {
      ProvStateDTO fileState = getExperiment(project, id);
      if(fileState != null) {
        experimentSummary.setFinished(fileState.getCreateTime() + experimentSummary.getDuration());
      }
    }
    
    if(!Strings.isNullOrEmpty(experimentSummary.getAppId())) {
      byte[] appIdBytes = experimentSummary.getAppId().getBytes(StandardCharsets.UTF_8);
      xattrCtrl.upsertProvXAttr(project, user, experimentPath,
        ExperimentsBuilder.EXPERIMENT_APP_ID_XATTR_NAME, appIdBytes);
    }
  
    String hdfsUserName = hdfsUsersController.getHdfsUserName(project, user);
    DistributedFileSystemOps udfso = dfs.getDfsOps(hdfsUserName);
    try {
      byte[] storedExpB = xattrCtrl.getProvXAttr(udfso, experimentPath,
        ExperimentsBuilder.EXPERIMENT_SUMMARY_XATTR_NAME);
      ExperimentDTO storedExp = null;
      if(storedExpB != null) {
        storedExp = experimentConverter.unmarshal(new String(storedExpB, StandardCharsets.UTF_8), ExperimentDTO.class);
      }
      ExperimentDTO experiment = ExperimentDTO.mergeExperiment(experimentSummary, storedExp);
      byte[] experimentB = experimentConverter.marshal(experiment);
      xattrCtrl.upsertProvXAttr(udfso, experimentPath, ExperimentsBuilder.EXPERIMENT_SUMMARY_XATTR_NAME, experimentB);
    } finally {
      if (udfso != null) {
        dfs.closeDfsClient(udfso);
      }
    }
  }
  
  public void attachModel(Users user, Project experimentProject, String experimentId, Project modelProject,
    String modelId)
    throws DatasetException, MetadataException, ExperimentsException {
    String experimentPath = Utils.getProjectPath(experimentProject.getName()) +
      Settings.HOPS_EXPERIMENTS_DATASET + "/" + experimentId;
    ModelXAttr modelSummary = new ModelXAttr(modelId, modelProject.getName());
    byte[] modelSummaryB = experimentConverter.marshal(modelSummary);
    xattrCtrl.upsertProvXAttr(experimentProject, user, experimentPath,
      ExperimentsBuilder.EXPERIMENT_MODEL_XATTR_NAME, modelSummaryB);
  }
  
  public void delete(Users user, Project userProject, Project parentProject, String experimentId)
    throws DatasetException {
    if(userProject.getId().equals(parentProject.getId())) {
      delete(user, userProject, experimentId);
    } else {
      String experimentPath = Utils.getProjectPath(userProject.getName())
        + parentProject.getName() + "::" + Settings.HOPS_EXPERIMENTS_DATASET + "/" + experimentId;
      deleteInternal(user, userProject, experimentPath);
    }
  }
  
  public void delete(Users user, Project project, String experimentId) throws DatasetException {
    String experimentPath = Utils.getProjectPath(project.getName())
      + Settings.HOPS_EXPERIMENTS_DATASET + "/" + experimentId;
    deleteInternal(user, project, experimentPath);
  }
  
  private void deleteInternal(Users user, Project project, String path) throws DatasetException {
    DatasetPath datasetPath = datasetHelper.getDatasetPath(project, path, DatasetType.DATASET);
    datasetController.delete(project, user, datasetPath.getFullPath(), datasetPath.getDataset(),
      datasetPath.isTopLevelDataset());
  }

  public ProvStateDTO getExperiment(Project project, String mlId) throws ProvenanceException {
    ProvStateParamBuilder provFilesParamBuilder = new ProvStateParamBuilder()
        .filterByField(ProvStateParser.FieldsP.PROJECT_I_ID, project.getInode().getId())
        .filterByField(ProvStateParser.FieldsP.ML_TYPE, Provenance.MLType.EXPERIMENT.name())
        .filterByField(ProvStateParser.FieldsP.ML_ID, mlId)
        .hasXAttr(ExperimentsBuilder.EXPERIMENT_SUMMARY_XATTR_NAME)
        .withAppExpansion()
        .paginate(0, 1);
    ProvStateDTO fileState = provenanceController.provFileStateList(project, provFilesParamBuilder);
    if (fileState != null && fileState.getItems() != null) {
      List<ProvStateDTO> experiments = fileState.getItems();
      if (experiments != null && !experiments.isEmpty()) {
        return experiments.iterator().next();
      }
    }
    return null;
  }

  public String versionProgram(Project project, Users user, String jobName, String kernelId, String mlId)
      throws JobException, ServiceException {
    if(!Strings.isNullOrEmpty(jobName)) {
      //experiment in job
      Jobs experimentJob = jobController.getJob(project, jobName);
      SparkJobConfiguration sparkJobConf = (SparkJobConfiguration)experimentJob.getJobConfig();
      String suffix = sparkJobConf.getAppPath().substring(sparkJobConf.getAppPath().lastIndexOf("."));
      String relativePath = Settings.HOPS_EXPERIMENTS_DATASET + "/" +
          mlId + "/program" + suffix;
      jobController.versionProgram(sparkJobConf, project, user,
          new Path(Utils.getProjectPath(project.getName()) + relativePath));
      return relativePath;
    } else {
      //experiment in jupyter
      String relativePath = Settings.HOPS_EXPERIMENTS_DATASET + "/" + mlId + "/program.ipynb";
      Path path = new Path(Utils.getProjectPath(project.getName())
          + "/" + relativePath);
      jupyterController.versionProgram(project, user, kernelId, path);
      return relativePath;
    }
  }
  
  public List<ExperimentsEndpointDTO> getExperimentsEndpoints(Project project) {
    return datasetController.getAllByName(project, Settings.HOPS_EXPERIMENTS_DATASET).stream()
      .map(ExperimentsEndpointDTO::fromDataset)
      .collect(Collectors.toCollection(ArrayList::new));
  }
  
  public ExperimentsEndpointDTO getExperimentsEndpoint(Project project) throws DatasetException {
    return ExperimentsEndpointDTO.fromDataset(datasetController.getByName(project, Settings.HOPS_EXPERIMENTS_DATASET));
  }
}
