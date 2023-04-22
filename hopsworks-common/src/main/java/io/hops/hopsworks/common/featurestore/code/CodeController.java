/*
 * This file is part of Hopsworks
 * Copyright (C) 2021, Logical Clocks AB. All rights reserved
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

package io.hops.hopsworks.common.featurestore.code;

import io.hops.hopsworks.common.dao.jobhistory.ExecutionFacade;
import io.hops.hopsworks.common.dao.jobs.description.JobFacade;
import io.hops.hopsworks.common.featurestore.activity.FeaturestoreActivityFacade;
import io.hops.hopsworks.common.featurestore.featuregroup.cached.FeatureGroupCommitController;
import io.hops.hopsworks.common.hdfs.DistributedFileSystemOps;
import io.hops.hopsworks.common.hdfs.DistributedFsService;
import io.hops.hopsworks.common.hdfs.HdfsUsersController;
import io.hops.hopsworks.common.hdfs.Utils;
import io.hops.hopsworks.common.jupyter.JupyterController;
import io.hops.hopsworks.common.util.Settings;
import io.hops.hopsworks.exceptions.FeaturestoreException;
import io.hops.hopsworks.exceptions.ServiceException;
import io.hops.hopsworks.persistence.entity.featurestore.code.FeaturestoreCode;
import io.hops.hopsworks.persistence.entity.featurestore.featuregroup.Featuregroup;
import io.hops.hopsworks.persistence.entity.featurestore.featuregroup.FeaturegroupType;
import io.hops.hopsworks.persistence.entity.featurestore.featuregroup.cached.FeatureGroupCommit;
import io.hops.hopsworks.persistence.entity.featurestore.featuregroup.cached.TimeTravelFormat;
import io.hops.hopsworks.persistence.entity.featurestore.trainingdataset.TrainingDataset;
import io.hops.hopsworks.persistence.entity.jobs.configuration.JobType;
import io.hops.hopsworks.persistence.entity.jobs.configuration.python.PythonJobConfiguration;
import io.hops.hopsworks.persistence.entity.jobs.configuration.spark.SparkJobConfiguration;
import io.hops.hopsworks.persistence.entity.jobs.description.Jobs;
import io.hops.hopsworks.persistence.entity.project.Project;
import io.hops.hopsworks.persistence.entity.user.Users;
import io.hops.hopsworks.restutils.RESTCodes;
import org.apache.commons.lang.NotImplementedException;
import org.apache.hadoop.fs.Path;

import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import java.io.IOException;
import java.sql.Timestamp;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;
import java.util.logging.Level;

@Stateless
@TransactionAttribute(TransactionAttributeType.NEVER)
public class CodeController {

  @EJB
  private Settings settings;
  @EJB
  private JobFacade jobFacade;
  @EJB
  private DistributedFsService dfs;
  @EJB
  private HdfsUsersController hdfsUsersController;
  @EJB
  private JupyterController jupyterController;
  @EJB
  private FeaturestoreCodeFacade featurestoreCodeFacade;
  @EJB
  private FeatureGroupCommitController featureGroupCommitCommitController;
  @EJB
  private FeaturestoreActivityFacade featurestoreActivityFacade;
  @EJB
  private ExecutionFacade executionFacade;

  private static final String CODE = "code";

  public CodeContentFormat getContentFormat(String path) throws FeaturestoreException {
    try {
      return CodeContentFormat.valueOf(Utils.getExtension(path)
          .orElse(CodeContentFormat.JAR.toString())
          .toUpperCase());
    } catch (IllegalArgumentException e) {
      throw new FeaturestoreException(RESTCodes.FeaturestoreErrorCode.CODE_READ_ERROR,
          Level.WARNING, e.getMessage(), e.getMessage(), e);
    }
  }

  public String readContent(Project project, Users user, String path, CodeContentFormat contentFormat,
                            JupyterController.NotebookConversion format)
      throws FeaturestoreException, ServiceException {
    //returns empty contents in the case of jar file
    switch (contentFormat) {
      case JAR:
        return null;
      case IPYNB:
        return readNotebookContent(project, user, path, format);
      case PY:
        return readPythonFileContent(project, user, path);
      case DBC:
        // For DBC (databricks archive) we read the ipynb version of the file instead so that
        // we can display it in the UI.
        return readNotebookContent(project, user,
            path.replace("." + CodeContentFormat.DBC.toString().toLowerCase(),
                "." + CodeContentFormat.IPYNB.toString().toLowerCase(Locale.ROOT)),
            format);
    }

    return null;
  }

  private String readNotebookContent(Project project, Users user, String path,
                                     JupyterController.NotebookConversion format) throws ServiceException {
    if (format == JupyterController.NotebookConversion.HTML) {
      return jupyterController.convertIPythonNotebook(project, user, path,  "", format);
    }
    //returns empty contents in the case of not supported notebookConversion
    return null;
  }

  private String readPythonFileContent(Project project, Users user, String path) throws FeaturestoreException {
    DistributedFileSystemOps udfso = dfs.getDfsOps(project, user);
    try {
      return udfso.cat(path);
    } catch (IOException e) {
      throw new FeaturestoreException(RESTCodes.FeaturestoreErrorCode.CODE_READ_ERROR,
          Level.WARNING, e.getMessage(), e.getMessage(), e);
    } finally {
      dfs.closeDfsClient(udfso);
    }
  }

  public FeaturestoreCode registerCode(Project project, Users user, Long codeCommitTimeStamp,
                                       Long fgCommitId, String applicationId, Featuregroup featuregroup,
                                       String entityId, String databricksNotebook, byte[] databricksArchive,
                                       CodeActions.RunType type)
      throws ServiceException, FeaturestoreException {

    String path = saveCode(project, user, applicationId, featuregroup, entityId,
        databricksNotebook, databricksArchive, type);

    Timestamp commitTime = new Timestamp(codeCommitTimeStamp);
    FeaturestoreCode featurestoreCode = new FeaturestoreCode(commitTime, featuregroup, path, applicationId);

    if ((featuregroup.getFeaturegroupType() == FeaturegroupType.CACHED_FEATURE_GROUP &&
        featuregroup.getCachedFeaturegroup().getTimeTravelFormat() == TimeTravelFormat.HUDI) ||
        featuregroup.getFeaturegroupType() == FeaturegroupType.STREAM_FEATURE_GROUP
    ) {
      Optional<FeatureGroupCommit> featureGroupCommit =
              featureGroupCommitCommitController.findCommitByDate(featuregroup, fgCommitId);
      featureGroupCommit.ifPresent(featurestoreCode::setFeatureGroupCommit);
    }

    return featurestoreCodeFacade.update(featurestoreCode);
  }

  public FeaturestoreCode registerCode(Project project, Users user, Long codeCommitTimeStamp,
                                       String applicationId, TrainingDataset trainingDataset,
                                       String entityId, String databricksNotebook, byte[] databricksArchive,
                                       CodeActions.RunType type)
          throws ServiceException, FeaturestoreException {

    String path = saveCode(project, user, applicationId, trainingDataset, entityId,
        databricksNotebook, databricksArchive, type);

    Timestamp commitTime = new Timestamp(codeCommitTimeStamp);
    FeaturestoreCode featurestoreCode = new FeaturestoreCode(commitTime, trainingDataset, path, applicationId);

    return featurestoreCodeFacade.update(featurestoreCode);
  }

  private String saveCode(Project project, Users user, String applicationId,
                         Featuregroup featureGroup, String entityId,
                         String databricksNotebook, byte[] databricksArchive,
                         CodeActions.RunType type)
          throws ServiceException, FeaturestoreException {
    if (type == CodeActions.RunType.JOB) {
      // Log the job activity before saving the code
      executionFacade.findByAppId(applicationId)
          .ifPresent(execution -> featurestoreActivityFacade.logExecutionActivity(featureGroup, execution));
    }

    return saveCode(project, user, applicationId, entityId, getCodeDirFullPath(project, featureGroup),
        databricksNotebook, databricksArchive, type);
  }

  private String saveCode(Project project, Users user, String applicationId,
                         TrainingDataset trainingDataset, String entityId,
                         String databricksNotebook, byte[] databricksArchive,
                         CodeActions.RunType type)
          throws ServiceException, FeaturestoreException {
    return saveCode(project, user, applicationId, entityId, getCodeDirFullPath(project, trainingDataset),
        databricksNotebook, databricksArchive, type);
  }

  private String saveCode(Project project, Users user, String applicationId,
                         String entityId, Path dirPath, String databricksNotebook, byte[] databricksArchive,
                         CodeActions.RunType type)
          throws ServiceException, FeaturestoreException {

    Path filePath;

    switch(type) {
      case JUPYTER:
        filePath = new Path(dirPath, applicationId + ".ipynb");
        jupyterController.versionProgram(project, user, entityId, filePath);
        break;
      case JOB:
        filePath = saveJob(project, user, entityId, dirPath, applicationId);
        break;
      case DATABRICKS:
        filePath = saveDatabricks(project, user, dirPath, databricksNotebook, databricksArchive);
        break;
      default:
        throw new NotImplementedException();
    }

    return filePath.getName();
  }

  private Path saveJob(Project project, Users user, String entityId, Path dirPath, String applicationId)
      throws FeaturestoreException {
    // get job path
    Jobs job = jobFacade.findByProjectAndName(project, entityId);

    // Currently we can save code only for (Py)Spark and Python jobs
    String appPath = null;
    if (job.getJobType() == JobType.SPARK || job.getJobType() == JobType.PYSPARK) {
      appPath = ((SparkJobConfiguration) job.getJobConfig()).getAppPath();
    } else if (job.getJobType() == JobType.PYTHON) {
      appPath = ((PythonJobConfiguration) job.getJobConfig()).getAppPath();
    }

    // generate file path
    String extension = Utils.getExtension(appPath).orElse("");
    Path path = new Path(dirPath, applicationId + "." + extension);

    // read job and save to file path
    String projectUsername = hdfsUsersController.getHdfsUserName(project, user);
    DistributedFileSystemOps udfso = null;
    try {
      udfso = dfs.getDfsOps(projectUsername);
      String notebookString = udfso.cat(appPath);
      udfso.create(path, notebookString);

    } catch (IOException e){
      throw new FeaturestoreException(RESTCodes.FeaturestoreErrorCode.CODE_READ_ERROR,
              Level.WARNING, e.getMessage(), e.getMessage(), e);
    } finally {
      dfs.closeDfsClient(udfso);
    }

    return path;
  }

  public Path saveDatabricks(Project project, Users user, Path dirPath,
                             String databricksNotebook, byte[] databricksArchive)
      throws FeaturestoreException {
    //Save contents
    String uuid = UUID.randomUUID().toString();
    Path notebookPath = new Path(dirPath, uuid + "." + CodeContentFormat.IPYNB.toString().toLowerCase());
    Path archivePath = new Path(dirPath, uuid + "." + CodeContentFormat.DBC.toString().toLowerCase());

    String projectUsername = hdfsUsersController.getHdfsUserName(project, user);
    DistributedFileSystemOps udfso = null;
    try {
      udfso = dfs.getDfsOps(projectUsername);
      udfso.create(notebookPath, databricksNotebook);
      udfso.create(archivePath, databricksArchive);
    } catch (IOException e) {
      throw new FeaturestoreException(RESTCodes.FeaturestoreErrorCode.ERROR_SAVING_CODE,
          Level.WARNING, e.getMessage(), e.getMessage(), e);
    } finally {
      dfs.closeDfsClient(udfso);
    }

    return notebookPath;
  }

  public Path getCodeDirFullPath(Project project, Featuregroup featureGroup) {
    Path datasetDir = new Path(Utils.getFeaturestorePath(project, settings));
    String datasetName = Utils.getFeaturegroupName(featureGroup);
    return getCodeDirFullPath(datasetDir, datasetName);
  }

  public Path getCodeDirFullPath(Project project, TrainingDataset trainingDataset) {
    Path datasetDir = new Path(Utils.getProjectPath(project.getName()),
        project.getName() + "_" + Settings.ServiceDataset.TRAININGDATASETS.getName());
    String datasetName = Utils.getTrainingDatasetName(trainingDataset);
    return getCodeDirFullPath(datasetDir, datasetName);
  }

  private Path getCodeDirFullPath(Path datasetDir, String datasetName) {
    Path codeDir = new Path(datasetDir, CODE);
    return new Path(codeDir, datasetName);
  }
}
