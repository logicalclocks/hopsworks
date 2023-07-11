/*
 * This file is part of Hopsworks
 * Copyright (C) 2020, Logical Clocks AB. All rights reserved
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

package io.hops.hopsworks.common.featurestore.app;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.hops.hopsworks.common.dao.jobs.description.JobFacade;
import io.hops.hopsworks.common.dataset.DatasetController;
import io.hops.hopsworks.common.featurestore.FeaturestoreController;
import io.hops.hopsworks.common.featurestore.OptionDTO;
import io.hops.hopsworks.common.featurestore.featuregroup.FeaturegroupFacade;
import io.hops.hopsworks.common.featurestore.featuregroup.ImportFgJobConf;
import io.hops.hopsworks.common.featurestore.featuregroup.IngestionDataFormat;
import io.hops.hopsworks.common.featurestore.featuregroup.IngestionJob;
import io.hops.hopsworks.common.featurestore.featuregroup.stream.DeltaStreamerJobConf;
import io.hops.hopsworks.common.featurestore.query.Query;
import io.hops.hopsworks.common.featurestore.query.QueryBuilder;
import io.hops.hopsworks.common.featurestore.query.QueryController;
import io.hops.hopsworks.common.featurestore.query.QueryDTO;
import io.hops.hopsworks.common.featurestore.storageconnectors.FeaturestoreStorageConnectorDTO;
import io.hops.hopsworks.common.featurestore.trainingdatasets.TrainingDatasetController;
import io.hops.hopsworks.common.featurestore.utils.FeaturestoreUtils;
import io.hops.hopsworks.common.hdfs.DistributedFileSystemOps;
import io.hops.hopsworks.common.hdfs.DistributedFsService;
import io.hops.hopsworks.common.hdfs.HdfsUsersController;
import io.hops.hopsworks.common.hdfs.Utils;
import io.hops.hopsworks.common.jobs.JobController;
import io.hops.hopsworks.common.jobs.execution.ExecutionController;
import io.hops.hopsworks.common.provenance.core.Provenance;
import io.hops.hopsworks.common.util.Settings;
import io.hops.hopsworks.exceptions.DatasetException;
import io.hops.hopsworks.exceptions.FeaturestoreException;
import io.hops.hopsworks.exceptions.GenericException;
import io.hops.hopsworks.exceptions.HopsSecurityException;
import io.hops.hopsworks.exceptions.JobException;
import io.hops.hopsworks.exceptions.ProjectException;
import io.hops.hopsworks.exceptions.ServiceException;
import io.hops.hopsworks.persistence.entity.dataset.DatasetAccessPermission;
import io.hops.hopsworks.persistence.entity.featurestore.Featurestore;
import io.hops.hopsworks.persistence.entity.featurestore.featuregroup.Featuregroup;
import io.hops.hopsworks.persistence.entity.featurestore.featuregroup.FeaturegroupType;
import io.hops.hopsworks.persistence.entity.featurestore.featureview.FeatureView;
import io.hops.hopsworks.persistence.entity.featurestore.storageconnector.FeaturestoreConnectorType;
import io.hops.hopsworks.persistence.entity.featurestore.trainingdataset.TrainingDataset;
import io.hops.hopsworks.persistence.entity.jobs.configuration.JobType;
import io.hops.hopsworks.persistence.entity.jobs.configuration.spark.SparkJobConfiguration;
import io.hops.hopsworks.persistence.entity.jobs.description.Jobs;
import io.hops.hopsworks.persistence.entity.project.Project;
import io.hops.hopsworks.persistence.entity.user.Users;
import io.hops.hopsworks.restutils.RESTCodes;
import org.apache.hadoop.fs.FSDataOutputStream;

import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.inject.Inject;
import java.io.IOException;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.stream.Collectors;

@Stateless
@TransactionAttribute(TransactionAttributeType.NEVER)
public class FsJobManagerController {

  @EJB
  private FeaturestoreController featurestoreController;
  @EJB
  private DistributedFsService dfs;
  @EJB
  private HdfsUsersController hdfsUsersController;
  @EJB
  private DatasetController datasetController;
  @EJB
  private JobController jobController;
  @EJB
  private JobFacade jobFacade;
  @Inject
  private ExecutionController executionController;
  @EJB
  private Settings settings;
  @EJB
  private TrainingDatasetController trainingDatasetController;
  @EJB
  private QueryController queryController;
  @EJB
  private QueryBuilder queryBuilder;
  @EJB
  private FeaturegroupFacade featuregroupFacade;
  @EJB
  private FeaturestoreUtils featurestoreUtils;

  private ObjectMapper objectMapper = new ObjectMapper();
  private SimpleDateFormat formatter = new SimpleDateFormat("ddMMyyyyHHmmss");

  // fg
  private final static String INSERT_FG_OP = "insert_fg";
  private final static String COMPUTE_STATS_OP = "compute_stats";
  private final static String DELTA_STREAMER_OP = "offline_fg_materialization";
  private final static String GE_VALIDATE_OP = "ge_validate";
  private final static String IMPORT_FEATUREGROUP_OP = "import_fg";
  // td
  private final static String TRAINING_DATASET_OP = "create_td";
  // fv
  private final static String FEATURE_VIEW_TRAINING_DATASET_OP = "create_fv_td";

  public IngestionJob setupIngestionJob(Project project, Users user, Featuregroup featureGroup,
                                        SparkJobConfiguration sparkJobConfiguration, IngestionDataFormat dataFormat,
                                        Map<String, String> writeOptions,
                                        Map<String, String> dataOptions)
      throws FeaturestoreException, DatasetException, HopsSecurityException, JobException {
    DistributedFileSystemOps udfso = dfs.getDfsOps(hdfsUsersController.getHdfsUserName(project, user));

    try {
      String dataPath = getIngestionPath(project, user, featureGroup, udfso);

      Map<String, Object> jobConfiguration = new HashMap<>();
      jobConfiguration.put("feature_store",
          featurestoreController.getOfflineFeaturestoreDbName(featureGroup.getFeaturestore().getProject()));
      jobConfiguration.put("name", featureGroup.getName());
      jobConfiguration.put("version", String.valueOf(featureGroup.getVersion()));
      jobConfiguration.put("data_path", dataPath);
      jobConfiguration.put("data_format", dataFormat.toString());
      jobConfiguration.put("data_options", dataOptions);
      jobConfiguration.put("write_options", writeOptions);

      String jobConfigurationStr = objectMapper.writeValueAsString(jobConfiguration);
      String jobName = getJobName(INSERT_FG_OP, Utils.getFeaturegroupName(featureGroup), true);
      String jobFolder = createJobFolder(project, user, jobName);
      String jobConfigurationPath = getJobConfigurationPath(jobFolder);
      writeToHDFS(jobConfigurationPath, jobConfigurationStr, udfso);
      Jobs ingestionJob = configureJob(user, project, sparkJobConfiguration, jobName,
          getJobArgs(INSERT_FG_OP, jobConfigurationPath), JobType.PYSPARK);

      // For ingestion we cannot start the job directly, as the client needs to upload the data in the directory
      // we created above. So, we return the information regarding the path and the job.
      // the client will trigger the job once the data upload is done.
      return new IngestionJob(dataPath, ingestionJob);
    } catch (IOException e) {
      throw new FeaturestoreException(RESTCodes.FeaturestoreErrorCode.ERROR_JOB_SETUP, Level.SEVERE,
          "Error setting up feature group import job", e.getMessage(), e);
    } finally {
      dfs.closeDfsClient(udfso);
    }
  }

  // Create the directoy where the client should upload the data.
  private String getIngestionPath(Project project, Users user, Featuregroup featureGroup,
                                  DistributedFileSystemOps udfso)
      throws IOException, DatasetException, HopsSecurityException {
    if (!ingestionDatasetExists(project)) {
      createIngestionDataset(project, user);
    }

    String ingestionPath = Paths.get(Utils.getProjectPath(project.getName()),
        Settings.ServiceDataset.INGESTION.getName(),
        Utils.getFeaturegroupName(featureGroup) + System.currentTimeMillis()).toString();
    udfso.mkdir(ingestionPath);

    return ingestionPath;
  }

  private boolean ingestionDatasetExists(Project project) {
    return project.getDatasetCollection().stream()
        .anyMatch(ds -> ds.getName().equalsIgnoreCase(Settings.ServiceDataset.INGESTION.getName()));
  }

  private void createIngestionDataset(Project project, Users user) throws DatasetException, HopsSecurityException {
    DistributedFileSystemOps dfso = dfs.getDfsOps();
    try {
      datasetController.createDataset(user, project,
          Settings.ServiceDataset.INGESTION.getName(),
          Settings.ServiceDataset.INGESTION.getDescription(),
          Provenance.Type.DISABLED.dto, false, DatasetAccessPermission.EDITABLE, dfso);
    } finally {
      dfs.closeDfsClient(dfso);
    }
  }
  
  private Jobs setupAndStartJob(Project project, Users user, Featurestore featurestore, String entityName,
      Integer entityVersion, JobEntityType type, String op, String configPrefix, boolean nameIncludeTimestamp)
      throws FeaturestoreException, JobException, GenericException, ProjectException, ServiceException {
    DistributedFileSystemOps udfso = dfs.getDfsOps(hdfsUsersController.getHdfsUserName(project, user));
    Map<String, String> jobConfiguration = new HashMap<>();

    try {
      String jobName = getJobName(op, Utils.getFeatureStoreEntityName(entityName, entityVersion),
          nameIncludeTimestamp);
      String jobFolder = createJobFolder(project, user, jobName);
      String jobConfigurationPath = getJobConfigurationPath(jobFolder);

      jobConfiguration.put("feature_store",
        featurestoreController.getOfflineFeaturestoreDbName(featurestore.getProject()));
      jobConfiguration.put("type", type.toString());
      jobConfiguration.put("name", entityName);
      jobConfiguration.put("version", String.valueOf(entityVersion));

      String jobConfigurationStr = objectMapper.writeValueAsString(jobConfiguration);
      writeToHDFS(jobConfigurationPath, jobConfigurationStr, udfso);

      String jobArgs = getJobArgs(op, jobConfigurationPath);

      Jobs job = configureJob(user, project, null, jobName,
        jobArgs, JobType.PYSPARK);

      // Differently from the ingestion job. At this stage, no other action is required by the client.
      // So we can start the job directly
      executionController.start(job, jobArgs, user);

      return job;
    } catch (IOException e) {
      throw new FeaturestoreException(RESTCodes.FeaturestoreErrorCode.ERROR_JOB_SETUP, Level.SEVERE,
        "Error setting up " + configPrefix + " job", e.getMessage(), e);
    } finally {
      dfs.closeDfsClient(udfso);
    }
  }
  
  /**
   *  setup job configuration passed as argument for creating and launching job
   * @param project
   * @param user
   * @param featurestore
   * @param entityName
   * @param entityVersion
   * @param type
   * @param op
   * @param configPrefix
   * @param jobConfiguration
   * @return Jobs
   * @throws FeaturestoreException
   * @throws JobException
   * @throws GenericException
   * @throws ProjectException
   * @throws ServiceException
   */
  private Jobs setupAndStartJob(Project project, Users user, Featurestore featurestore, String entityName,
    Integer entityVersion, JobEntityType type, String op, String configPrefix, Map<String, String> jobConfiguration)
          throws FeaturestoreException, JobException, GenericException, ProjectException, ServiceException {
    DistributedFileSystemOps udfso = dfs.getDfsOps(hdfsUsersController.getHdfsUserName(project, user));
    try {
      String jobName = getJobName(op, Utils.getFeatureStoreEntityName(entityName, entityVersion), false);
      String jobConfigurationPath = getJobConfigurationPath(createJobFolder(project, user, jobName));
      jobConfiguration.put("feature_store",
        featurestoreController.getOfflineFeaturestoreDbName(featurestore.getProject()));
      jobConfiguration.put("type", type.toString());
      String jobConfigurationStr = objectMapper.writeValueAsString(jobConfiguration);
      writeToHDFS(jobConfigurationPath, jobConfigurationStr, udfso);
      
      String jobArgs = getJobArgs(op, jobConfigurationPath);
      Jobs job = configureJob(user, project, null, jobName,
              jobArgs, JobType.PYSPARK);

      executionController.start(job, jobArgs, user);
      return job;
    } catch (IOException e) {
      throw new FeaturestoreException(RESTCodes.FeaturestoreErrorCode.ERROR_JOB_SETUP, Level.SEVERE,
              "Error setting up " + configPrefix + " job", e.getMessage(), e);
    } finally {
      dfs.closeDfsClient(udfso);
    }
  }

  public Jobs setupStatisticsJob(Project project, Users user, Featurestore featurestore,
                                 Featuregroup featureGroup, TrainingDataset trainingDataset)
      throws FeaturestoreException, JobException, GenericException, ProjectException, ServiceException {
    String entityName;
    Integer entityVersion;
    JobEntityType type;

    if (featureGroup != null) {
      // check feature group type
      if (featureGroup.getFeaturegroupType().equals(FeaturegroupType.ON_DEMAND_FEATURE_GROUP)) {
        type = JobEntityType.EXTERNAL_FG;
      } else {
        type = JobEntityType.FG;
      }
      entityName = featureGroup.getName();
      entityVersion = featureGroup.getVersion();
    } else {
      entityName = trainingDataset.getName();
      entityVersion = trainingDataset.getVersion();
      type = JobEntityType.TD;
    }

    return setupAndStartJob(project, user, featurestore, entityName, entityVersion, type, COMPUTE_STATS_OP,
      "statistics", true);
  }

  public Jobs setupValidationJob(Project project, Users user, Featurestore featurestore, Featuregroup featureGroup)
      throws FeaturestoreException, JobException, GenericException, ProjectException, ServiceException {
    return setupAndStartJob(project, user, featurestore, featureGroup.getName(), featureGroup.getVersion(),
      JobEntityType.FG, GE_VALIDATE_OP, "validation", false);
  }

  public Jobs setupTrainingDatasetJob(Project project, Users user, FeatureView featureView,
      Integer trainingDatasetVersion,
      Boolean overwrite, Map<String, String> writeOptions, SparkJobConfiguration sparkJobConfiguration)
      throws FeaturestoreException, JobException, GenericException, ProjectException, ServiceException {
    TrainingDataset trainingDataset = trainingDatasetController.getTrainingDatasetByFeatureViewAndVersion(
        featureView, trainingDatasetVersion);
    Query query = queryController.makeQuery(featureView, project, user, true, false);
    QueryDTO queryDTO = queryBuilder.build(query, featureView.getFeaturestore(), project, user);
    return setupTrainingDatasetJob(project, user, trainingDataset, queryDTO, overwrite, writeOptions,
        sparkJobConfiguration, FEATURE_VIEW_TRAINING_DATASET_OP);
  }

  public Jobs setupTrainingDatasetJob(Project project, Users user, TrainingDataset trainingDataset,
      QueryDTO queryDTO, Boolean overwrite, Map<String, String> writeOptions,
      SparkJobConfiguration sparkJobConfiguration)
      throws FeaturestoreException, JobException, GenericException, ProjectException, ServiceException {
    return setupTrainingDatasetJob(project, user, trainingDataset, queryDTO, overwrite, writeOptions,
        sparkJobConfiguration, TRAINING_DATASET_OP);
  }

  private Jobs setupTrainingDatasetJob(Project project, Users user, TrainingDataset trainingDataset,
                                      QueryDTO queryDTO, Boolean overwrite, Map<String, String> writeOptions,
                                      SparkJobConfiguration sparkJobConfiguration, String jobType)
      throws FeaturestoreException, JobException, GenericException, ProjectException, ServiceException {
    // verifyTrainingDatasetDataOwnerOrSelf is needed to avoid Data Scientist changing Training Dataset
    // not belonging to him. ex: td.insert(fg.select_all(), True)
    featurestoreUtils.verifyTrainingDatasetDataOwnerOrSelf(user, project, trainingDataset,
        FeaturestoreUtils.ActionMessage.SETUP_TRAINING_DATASET_JOB);

    DistributedFileSystemOps udfso = dfs.getDfsOps(hdfsUsersController.getHdfsUserName(project, user));

    try {
      String jobConfigurationPath;
      Map<String, Object> jobConfiguration = new HashMap<>();
      String jobName = null;
      jobConfiguration.put("feature_store",
          featurestoreController.getOfflineFeaturestoreDbName(trainingDataset.getFeaturestore().getProject()));
      if (trainingDataset.getFeatureView() != null) {
        String featureViewName = trainingDataset.getFeatureView().getName();
        Integer featureViewVersion = trainingDataset.getFeatureView().getVersion();
        jobConfiguration.put("name", featureViewName);
        jobConfiguration.put("version", String.valueOf(featureViewVersion));
        jobConfiguration.put("td_version", String.valueOf(trainingDataset.getVersion()));
        jobName = getJobName(jobType, Utils.getFeatureStoreEntityName(featureViewName, featureViewVersion),
            true);
        String jobFolder = createJobFolder(project, user, jobName);
        jobConfigurationPath = getJobConfigurationPath(jobFolder);
      } else {
        jobConfiguration.put("name", trainingDataset.getName());
        jobConfiguration.put("version", String.valueOf(trainingDataset.getVersion()));
        jobName = getJobName(jobType, Utils.getFeatureStoreEntityName(trainingDataset.getName(),
                trainingDataset.getVersion()), true);
        String jobFolder = createJobFolder(project, user, jobName);
        // For FeatureView, query is constructed from scratch when launching the job.
        jobConfigurationPath = getJobConfigurationPath(jobFolder);
        jobConfiguration.put("query", queryDTO);
      }
      jobConfiguration.put("write_options", writeOptions);
      jobConfiguration.put("overwrite", overwrite);

      String jobConfigurationStr = objectMapper.writeValueAsString(jobConfiguration);
      writeToHDFS(jobConfigurationPath, jobConfigurationStr, udfso);

      String jobArgs = getJobArgs(jobType, jobConfigurationPath);

      Jobs trainingDatasetJob = configureJob(user, project, sparkJobConfiguration,
          jobName, jobArgs, JobType.PYSPARK);

      executionController.start(trainingDatasetJob, jobArgs, user);

      return trainingDatasetJob;
    } catch (IOException e) {
      throw new FeaturestoreException(RESTCodes.FeaturestoreErrorCode.ERROR_JOB_SETUP, Level.SEVERE,
          "Error setting up training dataset job", e.getMessage(), e);
    } finally {
      dfs.closeDfsClient(udfso);
    }
  }
  
  public Jobs setupHudiDeltaStreamerJob(Project project, Users user, Featuregroup featuregroup,
    DeltaStreamerJobConf deltaStreamerJobConf)
    throws FeaturestoreException, JobException {
    DistributedFileSystemOps udfso = dfs.getDfsOps(hdfsUsersController.getHdfsUserName(project, user));
    Map<String, String> writeOptions = null;
    SparkJobConfiguration sparkJobConfiguration = null;
    
    if (deltaStreamerJobConf != null) {
      if (deltaStreamerJobConf.getWriteOptions() != null) {
        writeOptions = deltaStreamerJobConf.getWriteOptions().stream()
          .collect(Collectors.toMap(OptionDTO::getName, OptionDTO::getValue));
      }
      sparkJobConfiguration = deltaStreamerJobConf.getSparkJobConfiguration();
    }
  
    try {
      String jobName = getJobName(DELTA_STREAMER_OP, Utils.getFeaturegroupName(featuregroup), false);
      String jobFolder = createJobFolder(project, user, jobName);
      String jobConfigurationPath = getJobConfigurationPath(jobFolder);
      Map<String, Object> jobConfiguration = new HashMap<>();
      jobConfiguration.put("feature_store",
        featurestoreController.getOfflineFeaturestoreDbName(featuregroup.getFeaturestore().getProject()));
      jobConfiguration.put("name", featuregroup.getName());
      jobConfiguration.put("version", String.valueOf(featuregroup.getVersion()));
      jobConfiguration.put("write_options", writeOptions);
      
      String jobConfigurationStr = objectMapper.writeValueAsString(jobConfiguration);
      writeToHDFS(jobConfigurationPath, jobConfigurationStr, udfso);
      String jobArgs = getJobArgs(DELTA_STREAMER_OP, jobConfigurationPath);
  
      //setup job
      return configureJob(user, project, sparkJobConfiguration, jobName, jobArgs, JobType.SPARK);
    } catch (IOException e) {
      throw new FeaturestoreException(RESTCodes.FeaturestoreErrorCode.ERROR_JOB_SETUP, Level.SEVERE,
        "Error setting up delta streamer job", e.getMessage(), e);
    } finally {
      dfs.closeDfsClient(udfso);
    }
  }
  
  private String createJobFolder(Project project, Users user, String jobName) throws JobException{
    return jobController.createJobFolder(project, user, jobName);
  }

  private String getJobConfigurationPath(String jobFolder) {
    return jobFolder + "/config_" + System.currentTimeMillis();
  }

  private String getJobName(String op, String entityName, boolean withTimeStamp) {
    String name = entityName + "_" + op;
    if (withTimeStamp) {
      name = name + "_" + formatter.format(new Date());
    }
    return name;
  }

  private String getJobArgs(String op, String jobConfigurationPath) {
    return "-op " + op + " -path " + jobConfigurationPath;
  }

  private void writeToHDFS(String filePath, String content, DistributedFileSystemOps udfso) throws IOException {
    try (FSDataOutputStream outStream = udfso.create(filePath)) {
      outStream.writeBytes(content);
      outStream.hflush();
    }
  }

  private Jobs configureJob(Users user, Project project, SparkJobConfiguration sparkJobConfiguration,
                            String jobName, String defaultArgs, JobType jobType)
      throws JobException {
    Jobs job = jobFacade.findByProjectAndName(project, jobName);

    if (sparkJobConfiguration == null) {
      // set defaults for spark job size
      sparkJobConfiguration = new SparkJobConfiguration();
    }

    sparkJobConfiguration.setAppName(jobName);
    sparkJobConfiguration.setMainClass(jobType.equals(JobType.PYSPARK) ? Settings.SPARK_PY_MAINCLASS:
        Settings.HSFS_UTIL_MAIN_CLASS);
    sparkJobConfiguration.setAppPath(jobType.equals(JobType.PYSPARK) ? settings.getFSPyJobUtilPath():
        settings.getFSJavaJobUtilPath());
    sparkJobConfiguration.setDefaultArgs(defaultArgs);

    return jobController.putJob(user, project, job, sparkJobConfiguration);
  }

  public void deleteJobs(Project project, Users user, Featuregroup featuregroup)
      throws JobException {
    String jobNameRegex = String.format("%s_(%s|%s|%s|%s|%s).*", Utils.getFeaturegroupName(featuregroup),
        INSERT_FG_OP, COMPUTE_STATS_OP, DELTA_STREAMER_OP, GE_VALIDATE_OP, IMPORT_FEATUREGROUP_OP);
    deleteJobs(project, user, jobNameRegex);
  }

  public void deleteJobs(Project project, Users user, TrainingDataset trainingDataset)
      throws JobException {
    String jobNameRegex = String.format("%s_(%s).*", Utils.getTrainingDatasetName(trainingDataset),
        TRAINING_DATASET_OP);
    deleteJobs(project, user, jobNameRegex);
  }

  public void deleteJobs(Project project, Users user, FeatureView featureView)
      throws JobException {
    String jobNameRegex = String.format("%s_(%s).*", Utils.getFeatureViewName(featureView),
        FEATURE_VIEW_TRAINING_DATASET_OP);
    deleteJobs(project, user, jobNameRegex);
  }

  private void deleteJobs(Project project, Users user, String jobPrefix)
      throws JobException {
    List<Jobs> jobsList = jobController.getJobsWithJobNameRegex(project, jobPrefix);
    for (Jobs job: jobsList) {
      jobController.deleteJob(job, user);
    }
  }

  public Jobs setupImportFgJob(Project project, Users user, Featurestore featurestore, ImportFgJobConf importFgJobConf)
          throws FeaturestoreException, JobException, GenericException, ProjectException, ServiceException {
    Map jobConfiguration =new HashMap<>();
    FeaturestoreStorageConnectorDTO storageConnector = importFgJobConf.getStorageConnectorDTO();
    FeaturestoreConnectorType connectorType = storageConnector.getStorageConnectorType();
    HashMap<String, String> options = new HashMap<String, String>();
    // set spark options as per connector type
    if (importFgJobConf.getTable() != null) {
      switch (connectorType) {
        case SNOWFLAKE:
        case REDSHIFT:
          options.put("dbtable", importFgJobConf.getTable());
          break;
        case BIGQUERY:
          options.put("table", importFgJobConf.getTable());
          break;
        default:
          throw new FeaturestoreException(RESTCodes.FeaturestoreErrorCode.ILLEGAL_STORAGE_CONNECTOR_ARG, Level.WARNING,
            "Storage connector type "+ connectorType.name() + " not supported for importing data");
      }
    }
    // get new version
    int version = getNewFeatureGroupVersion(featurestore,importFgJobConf.getFeatureGroupName());

    jobConfiguration.put("storageConnectorName", storageConnector.getName());
    jobConfiguration.put("featureGroupName", importFgJobConf.getFeatureGroupName());
    jobConfiguration.put("query", importFgJobConf.getQuery());
    jobConfiguration.put("primaryKey", importFgJobConf.getPrimaryKey());
    jobConfiguration.put("version", String.valueOf(version));
    jobConfiguration.put("options", options);
    jobConfiguration.put("connectorType", connectorType.name());
    // optional fields
    if (importFgJobConf.getPartitionKey() != null) {
      jobConfiguration.put("partitionKey", importFgJobConf.getPartitionKey());
    }
    if (importFgJobConf.getStatisticsConfigDTO() != null) {
      jobConfiguration.put("statisticsConfig", importFgJobConf.getStatisticsConfigDTO());
    }
    if ( importFgJobConf.isOnlineEnabled()) {
      jobConfiguration.put("onlineEnabled", importFgJobConf.isOnlineEnabled());
    }
    if ( importFgJobConf.getEventTime() != null) {
      jobConfiguration.put("eventTime", importFgJobConf.getEventTime());
    }
    if ( importFgJobConf.getDescription() != null) {
      jobConfiguration.put("description", importFgJobConf.getDescription());
    }
  
    return setupAndStartJob(project, user, featurestore, importFgJobConf.getFeatureGroupName(),
      version,JobEntityType.FG,IMPORT_FEATUREGROUP_OP,
      "importData", jobConfiguration);
  }
  
  public int getNewFeatureGroupVersion(Featurestore featurestore, String featureGroupName){
    List<Featuregroup> fgPrevious = featuregroupFacade.findByNameAndFeaturestoreOrderedDescVersion(
      featureGroupName, featurestore);
    if (fgPrevious != null && !fgPrevious.isEmpty()) {
      return fgPrevious.get(0).getVersion()+1;
    } else {
      return 1;
    }
  }
  
}
