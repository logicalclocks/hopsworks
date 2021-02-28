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
import io.hops.hopsworks.common.dataset.DatasetController;
import io.hops.hopsworks.common.featurestore.FeaturestoreController;
import io.hops.hopsworks.common.featurestore.featuregroup.IngestionDataFormat;
import io.hops.hopsworks.common.featurestore.featuregroup.IngestionJob;
import io.hops.hopsworks.common.featurestore.query.QueryDTO;
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
import io.hops.hopsworks.persistence.entity.featurestore.trainingdataset.TrainingDataset;
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
import java.util.Map;
import java.util.logging.Level;

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
  @Inject
  private ExecutionController executionController;
  @EJB
  private Settings settings;

  private ObjectMapper objectMapper = new ObjectMapper();
  private SimpleDateFormat formatter = new SimpleDateFormat("ddMMyyyyHHmmss");

  private final static String INSERT_FG_OP = "insert_fg";
  private final static String TRAINING_DATASET_OP = "create_td";
  private final static String COMPUTE_STATS_OP = "compute_stats";

  public IngestionJob setupIngestionJob(Project project, Users user, Featuregroup featureGroup,
                                        SparkJobConfiguration sparkJobConfiguration, IngestionDataFormat dataFormat,
                                        Map<String, String> writeOptions,
                                        Map<String, String> dataOptions)
      throws FeaturestoreException, DatasetException, HopsSecurityException, JobException {
    DistributedFileSystemOps udfso = dfs.getDfsOps(hdfsUsersController.getHdfsUserName(project, user));

    try {
      String dataPath = getIngestionPath(project, user, featureGroup, udfso);
      String jobConfigurationPath = getJobConfigurationPath(project, featureGroup.getName(),
          featureGroup.getVersion(), "ingestion");

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
      writeToHDFS(jobConfigurationPath, jobConfigurationStr, udfso);

      Jobs ingestionJob = configureJob(user, project, sparkJobConfiguration,
          getJobName(INSERT_FG_OP, Utils.getFeaturegroupName(featureGroup)),
          getJobArgs(INSERT_FG_OP, jobConfigurationPath));

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

  public Jobs setupStatisticsJob(Project project, Users user, Featurestore featurestore,
                                 Featuregroup featureGroup, TrainingDataset trainingDataset)
      throws FeaturestoreException, JobException, GenericException, ProjectException, ServiceException {
    DistributedFileSystemOps udfso = dfs.getDfsOps(hdfsUsersController.getHdfsUserName(project, user));
    Map<String, String> jobConfiguration = new HashMap<>();

    try {
      String entityName = featureGroup != null ? featureGroup.getName() : trainingDataset.getName();
      Integer entityVersion = featureGroup != null ? featureGroup.getVersion() : trainingDataset.getVersion();
      String jobConfigurationPath = getJobConfigurationPath(project, entityName, entityVersion, "statistics");

      jobConfiguration.put("feature_store",
          featurestoreController.getOfflineFeaturestoreDbName(featurestore.getProject()));
      jobConfiguration.put("type", featureGroup != null ? "fg" : "td");
      jobConfiguration.put("name", entityName);
      jobConfiguration.put("version", String.valueOf(entityVersion));

      String jobConfigurationStr = objectMapper.writeValueAsString(jobConfiguration);
      writeToHDFS(jobConfigurationPath, jobConfigurationStr, udfso);

      String jobArgs = getJobArgs(COMPUTE_STATS_OP, jobConfigurationPath);

      Jobs statisticsJob = configureJob(user, project, null,
          getJobName(COMPUTE_STATS_OP, Utils.getFeatureStoreEntityName(entityName, entityVersion)),
          jobArgs);

      // Differently from the ingestion job. At this stage, no other action is required by the client.
      // So we can start the job directly
      executionController.start(statisticsJob, jobArgs, user);

      return statisticsJob;
    } catch (IOException e) {
      throw new FeaturestoreException(RESTCodes.FeaturestoreErrorCode.ERROR_JOB_SETUP, Level.SEVERE,
          "Error setting up statistics job", e.getMessage(), e);
    } finally {
      dfs.closeDfsClient(udfso);
    }
  }

  public Jobs setupTrainingDatasetJob(Project project, Users user, TrainingDataset trainingDataset,
                                      QueryDTO queryDTO, Boolean overwrite, Map<String, String> writeOptions,
                                      SparkJobConfiguration sparkJobConfiguration)
      throws FeaturestoreException, JobException, GenericException, ProjectException, ServiceException {
    DistributedFileSystemOps udfso = dfs.getDfsOps(hdfsUsersController.getHdfsUserName(project, user));

    try {
      String jobConfigurationPath =
          getJobConfigurationPath(project, trainingDataset.getName(), trainingDataset.getVersion(), "td");

      Map<String, Object> jobConfiguration = new HashMap<>();
      jobConfiguration.put("feature_store",
          featurestoreController.getOfflineFeaturestoreDbName(trainingDataset.getFeaturestore().getProject()));
      jobConfiguration.put("name", trainingDataset.getName());
      jobConfiguration.put("version", String.valueOf(trainingDataset.getVersion()));
      jobConfiguration.put("query", queryDTO);
      jobConfiguration.put("write_options", writeOptions);
      jobConfiguration.put("overwrite", overwrite);

      String jobConfigurationStr = objectMapper.writeValueAsString(jobConfiguration);
      writeToHDFS(jobConfigurationPath, jobConfigurationStr, udfso);

      String jobArgs = getJobArgs(TRAINING_DATASET_OP, jobConfigurationPath);

      Jobs trainingDatasetJob = configureJob(user, project, sparkJobConfiguration,
          getJobName(TRAINING_DATASET_OP, Utils.getTrainingDatasetName(trainingDataset)),
          jobArgs);

      executionController.start(trainingDatasetJob, jobArgs, user);

      return trainingDatasetJob;
    } catch (IOException e) {
      throw new FeaturestoreException(RESTCodes.FeaturestoreErrorCode.ERROR_JOB_SETUP, Level.SEVERE,
          "Error setting up training dataset job", e.getMessage(), e);
    } finally {
      dfs.closeDfsClient(udfso);
    }
  }

  public String getJobConfigurationPath(Project project, String entityName, Integer entityVersion, String prefix) {
    return Paths.get(Utils.getProjectPath(project.getName()), Settings.BaseDataset.RESOURCES.getName(),
        String.join("_", new String[]{prefix, entityName,
                String.valueOf(entityVersion), String.valueOf(System.currentTimeMillis())})).toString();
  }

  private String getJobName(String op, String entity) {
    return entity + "_" + op + "_" + formatter.format(new Date());
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
                            String jobName, String defaultArgs)
      throws JobException {
    if (sparkJobConfiguration == null) {
      // set defaults for spark job size
      sparkJobConfiguration = new SparkJobConfiguration();
    }

    sparkJobConfiguration.setAppName(jobName);
    sparkJobConfiguration.setMainClass(Settings.SPARK_PY_MAINCLASS);
    sparkJobConfiguration.setAppPath(settings.getFSJobUtilPath());
    sparkJobConfiguration.setDefaultArgs(defaultArgs);

    return jobController.putJob(user, project, null, sparkJobConfiguration);
  }
}
