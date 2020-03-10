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
import io.hops.hopsworks.common.hdfs.DistributedFileSystemOps;
import io.hops.hopsworks.common.hdfs.DistributedFsService;
import io.hops.hopsworks.common.hdfs.HdfsUsersController;
import io.hops.hopsworks.common.hdfs.Utils;
import io.hops.hopsworks.common.jobs.JobController;
import io.hops.hopsworks.common.jupyter.JupyterController;
import io.hops.hopsworks.common.provenance.core.Provenance;
import io.hops.hopsworks.common.provenance.state.ProvFileStateParamBuilder;
import io.hops.hopsworks.common.provenance.state.ProvStateController;
import io.hops.hopsworks.common.provenance.state.dto.ProvStateElastic;
import io.hops.hopsworks.common.provenance.state.dto.ProvStateListDTO;
import io.hops.hopsworks.common.util.Settings;
import io.hops.hopsworks.exceptions.DatasetException;
import io.hops.hopsworks.exceptions.JobException;
import io.hops.hopsworks.exceptions.ProvenanceException;
import io.hops.hopsworks.exceptions.ServiceException;
import io.hops.hopsworks.persistence.entity.jobs.configuration.spark.SparkJobConfiguration;
import io.hops.hopsworks.persistence.entity.jobs.description.Jobs;
import io.hops.hopsworks.persistence.entity.project.Project;
import io.hops.hopsworks.persistence.entity.user.Users;
import io.hops.hopsworks.restutils.RESTCodes;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.fs.XAttrSetFlag;
import org.apache.parquet.Strings;
import org.eclipse.persistence.jaxb.JAXBContextFactory;
import org.eclipse.persistence.jaxb.MarshallerProperties;

import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.ws.rs.core.MediaType;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import java.io.IOException;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.util.EnumSet;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

@Stateless
@TransactionAttribute(TransactionAttributeType.NEVER)
public class ExperimentsController {

  private static final Logger LOGGER = Logger.getLogger(ExperimentsController.class.getName());

  @EJB
  private DistributedFsService dfs;
  @EJB
  private ProvStateController provenanceController;
  @EJB
  private JobController jobController;
  @EJB
  private DistributedFsService dfsService;
  @EJB
  private HdfsUsersController hdfsUsersController;
  @EJB
  private JupyterController jupyterController;


  public void attachExperiment(String id, Project project, String usersFullName, ExperimentDTO experimentSummary,
                               ExperimentDTO.XAttrSetFlag xAttrSetFlag) throws DatasetException, ProvenanceException {

    experimentSummary.setUserFullName(usersFullName);
    String experimentPath = Utils.getProjectPath(project.getName()) + Settings.HOPS_EXPERIMENTS_DATASET + "/" + id;

    if(xAttrSetFlag.equals(ExperimentDTO.XAttrSetFlag.REPLACE)
        && experimentSummary.getFinished() == null
        && experimentSummary.getDuration() != null) {
      ProvStateElastic fileState = getExperiment(project, id);
      if(fileState != null) {
        experimentSummary.setFinished(fileState.getCreateTime() + experimentSummary.getDuration());
      }
    }

    DistributedFileSystemOps dfso = null;
    try {
      dfso = dfs.getDfsOps();
      if(!Strings.isNullOrEmpty(experimentSummary.getAppId()) &&
          xAttrSetFlag.equals(ExperimentDTO.XAttrSetFlag.CREATE)) {
        byte[] appIdBytes = experimentSummary.getAppId().getBytes(StandardCharsets.UTF_8);
        EnumSet<XAttrSetFlag> flags = EnumSet.noneOf(XAttrSetFlag.class);
        flags.add(XAttrSetFlag.CREATE);
        dfso.setXAttr(experimentPath, "provenance.app_id", appIdBytes, flags);
      }

      JAXBContext sparkJAXBContext = JAXBContextFactory.createContext(new Class[] {ExperimentDTO.class},
          null);
      Marshaller marshaller = sparkJAXBContext.createMarshaller();
      marshaller.setProperty(MarshallerProperties.JSON_INCLUDE_ROOT, false);
      marshaller.setProperty(MarshallerProperties.MEDIA_TYPE, MediaType.APPLICATION_JSON);
      StringWriter sw = new StringWriter();
      marshaller.marshal(experimentSummary, sw);
      byte[] experiment = sw.toString().getBytes(StandardCharsets.UTF_8);

      EnumSet<XAttrSetFlag> flags = EnumSet.noneOf(XAttrSetFlag.class);
      flags.add(XAttrSetFlag.valueOf(xAttrSetFlag.name()));

      dfso.setXAttr(experimentPath, "provenance." +
          ExperimentsBuilder.EXPERIMENT_SUMMARY_XATTR_NAME, experiment, flags);

    } catch(IOException | JAXBException ex) {
      throw new DatasetException(RESTCodes.DatasetErrorCode.ATTACH_XATTR_ERROR, Level.SEVERE,
          "path: " + experimentPath, ex.getMessage(), ex);
    } finally {
      if (dfso != null) {
        dfs.closeDfsClient(dfso);
      }
    }
  }

  public void attachModel(String id, Project project, String model,
                               ExperimentDTO.XAttrSetFlag xAttrSetFlag)
      throws DatasetException {
    String experimentPath = Utils.getProjectPath(project.getName()) +
        Settings.HOPS_EXPERIMENTS_DATASET + "/" + id;
    DistributedFileSystemOps dfso = null;
    try {
      byte[] experiment = model.getBytes(StandardCharsets.UTF_8);
      dfso = dfs.getDfsOps();
      EnumSet<XAttrSetFlag> flags = EnumSet.noneOf(XAttrSetFlag.class);
      flags.add(XAttrSetFlag.valueOf(xAttrSetFlag.name()));
      dfso.setXAttr(experimentPath, "provenance." +
          ExperimentsBuilder.EXPERIMENT_MODEL_XATTR_NAME, experiment, flags);
    } catch(IOException ex) {
      throw new DatasetException(RESTCodes.DatasetErrorCode.ATTACH_XATTR_ERROR, Level.SEVERE,
          "path: " + experimentPath, ex.getMessage(), ex);
    } finally {
      if (dfso != null) {
        dfs.closeDfsClient(dfso);
      }
    }
  }

  public void delete(String id, Project project, String hdfsUser) throws DatasetException {
    boolean success = false;
    DistributedFileSystemOps dfso = null;
    String experimentPath = Utils.getProjectPath(project.getName()) + Settings.HOPS_EXPERIMENTS_DATASET + "/" + id;
    try {
      dfso = dfs.getDfsOps(hdfsUser);
      Path path = new Path(experimentPath);
      if(!dfso.exists(path)) {
        return;
      }
      success = dfso.rm(path, true);
    } catch (IOException ioe) {
      throw new DatasetException(RESTCodes.DatasetErrorCode.INODE_DELETION_ERROR, Level.SEVERE,
          "path: " + experimentPath, "Error occurred during deletion of experiment", ioe);
    } finally {
      if (dfso != null) {
        dfs.closeDfsClient(dfso);
      }
    }
    if (!success) {
      throw new DatasetException(RESTCodes.DatasetErrorCode.INODE_DELETION_ERROR, Level.FINE,
          "path: " + experimentPath);
    }
  }

  public ProvStateElastic getExperiment(Project project, String mlId) throws ProvenanceException {
    ProvFileStateParamBuilder provFilesParamBuilder = new ProvFileStateParamBuilder()
        .withProjectInodeId(project.getInode().getId())
        .withMlType(Provenance.MLType.EXPERIMENT.name())
        .withPagination(0, 1)
        .withAppExpansion()
        .filterByHasXAttr(ExperimentsBuilder.EXPERIMENT_SUMMARY_XATTR_NAME)
        .withMlId(mlId);
    ProvStateListDTO fileState = provenanceController.provFileStateList(project, provFilesParamBuilder);
    if (fileState != null && fileState.getItems() != null) {
      List<ProvStateElastic> experiments = fileState.getItems();
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
}
