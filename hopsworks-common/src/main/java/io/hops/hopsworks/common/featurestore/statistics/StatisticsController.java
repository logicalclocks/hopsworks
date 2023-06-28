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

package io.hops.hopsworks.common.featurestore.statistics;

import io.hops.hopsworks.common.dataset.DatasetController;
import io.hops.hopsworks.common.featurestore.activity.FeaturestoreActivityFacade;
import io.hops.hopsworks.common.featurestore.featuregroup.cached.FeatureGroupCommitController;
import io.hops.hopsworks.common.hdfs.DistributedFileSystemOps;
import io.hops.hopsworks.common.hdfs.DistributedFsService;
import io.hops.hopsworks.common.hdfs.HdfsUsersController;
import io.hops.hopsworks.common.hdfs.Utils;
import io.hops.hopsworks.common.provenance.core.Provenance;
import io.hops.hopsworks.common.util.Settings;
import io.hops.hopsworks.exceptions.DatasetException;
import io.hops.hopsworks.exceptions.FeaturestoreException;
import io.hops.hopsworks.exceptions.HopsSecurityException;
import io.hops.hopsworks.persistence.entity.dataset.Dataset;
import io.hops.hopsworks.persistence.entity.dataset.DatasetAccessPermission;
import io.hops.hopsworks.persistence.entity.featurestore.featuregroup.Featuregroup;
import io.hops.hopsworks.persistence.entity.featurestore.featuregroup.FeaturegroupType;
import io.hops.hopsworks.persistence.entity.featurestore.featuregroup.cached.FeatureGroupCommit;
import io.hops.hopsworks.persistence.entity.featurestore.featuregroup.cached.TimeTravelFormat;
import io.hops.hopsworks.persistence.entity.featurestore.statistics.FeaturestoreStatistic;
import io.hops.hopsworks.persistence.entity.featurestore.trainingdataset.TrainingDataset;
import io.hops.hopsworks.persistence.entity.project.Project;
import io.hops.hopsworks.persistence.entity.user.Users;
import io.hops.hopsworks.restutils.RESTCodes;
import org.apache.hadoop.fs.Path;
import org.json.JSONException;
import org.json.JSONObject;

import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.sql.Timestamp;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;

@Stateless
@TransactionAttribute(TransactionAttributeType.NEVER)
public class StatisticsController {
  
  private static final Logger logger = Logger.getLogger(StatisticsController.class.getName());
  
  @EJB
  private DistributedFsService dfs;
  @EJB
  private DatasetController datasetController;
  @EJB
  private HdfsUsersController hdfsUsersController;
  @EJB
  private FeaturestoreStatisticFacade featurestoreStatisticFacade;
  @EJB
  private FeatureGroupCommitController featureGroupCommitCommitController;
  @EJB
  private FeaturestoreActivityFacade fsActivityFacade;
  @EJB
  private Settings settings;

  public String readOrDeleteStatistics(Project project, Users user, FeaturestoreStatistic statistic)
      throws FeaturestoreException {
    return readOrDeleteStatisticsFile(project, user, statistic, statistic.getFilePath());
  }

  public String readOrDeleteStatistics(Project project, Users user, FeaturestoreStatistic statistic,
      String splitName) throws FeaturestoreException {
    String fileName = splitStatisticsFileName(splitName, statistic.getCommitTime().getTime());
    String path = statistic.getFilePath() + "/" + fileName;
    return readOrDeleteStatisticsFile(project, user, statistic, path);
  }

  public FeaturestoreStatistic registerStatistics(Project project, Users user, Long statisticsCommitTimeStamp,
                                                  Long fgCommitId, String content, Featuregroup featuregroup)
      throws FeaturestoreException, DatasetException, HopsSecurityException, IOException {
    JSONObject statisticsJson = extractJsonFromContent(content);
    Optional<FeatureGroupCommit> featureGroupCommit = Optional.empty();
    if ((featuregroup.getFeaturegroupType() == FeaturegroupType.CACHED_FEATURE_GROUP &&
        featuregroup.getCachedFeaturegroup().getTimeTravelFormat() == TimeTravelFormat.HUDI) ||
        featuregroup.getFeaturegroupType() == FeaturegroupType.STREAM_FEATURE_GROUP
    ) {
      featureGroupCommit = featureGroupCommitCommitController.findCommitByDate(featuregroup, fgCommitId);
      // Statistics commitTimeStamp will be always system time sent from client if user wants to recompute
      // statistics on particular commit id (i.e. fgCommitId was provided). If fgCommitId is null
      // it means: 1) client issued save or insert method; here statistics commitTimeStamp will be featureGroupCommit;
      // 2) Or it is recomputing statistics of existing time travel enabled feature group. Here latest fg commit
      // timestamp will be used to read dataset and client system time provided will be used as statistics commit time.
      if (fgCommitId == null && featureGroupCommit.isPresent()) {
        Optional<FeaturestoreStatistic> featurestoreStatistic =
          featurestoreStatisticFacade.findByFeatureGroupAndCommitDate(
            featuregroup, new Date(featureGroupCommit.get().getCommittedOn()));
        if (!featurestoreStatistic.isPresent()){
          statisticsCommitTimeStamp = featureGroupCommit.get().getCommittedOn();
        }
      }
    }

    String statisticsFilePath = registerStatistics(project, user, statisticsCommitTimeStamp, statisticsJson.toString(),
        featuregroup.getName(), "FeatureGroups", featuregroup.getVersion(), null, false);
    Timestamp commitTime = new Timestamp(statisticsCommitTimeStamp);

    FeaturestoreStatistic statistic = new FeaturestoreStatistic(commitTime, statisticsFilePath, featuregroup);
    if (featureGroupCommit.isPresent()) {
      statistic.setFeatureGroupCommit(featureGroupCommit.get());
    }
    
    statistic = featurestoreStatisticFacade.update(statistic);

    // Log statistics activity
    fsActivityFacade.logStatisticsActivity(user, featuregroup, new Date(commitTime.getTime()), statistic);

    return statistic;
  }

  public FeaturestoreStatistic registerStatistics(Project project, Users user, Long commitTimeStamp, String content,
                                                  TrainingDataset trainingDataset, Map<String, String> splitStatistics,
                                                  boolean forTransformation)
      throws FeaturestoreException, DatasetException, HopsSecurityException, IOException {

    String statContent =  null;
    if (content != null) {
      JSONObject statisticsJson = extractJsonFromContent(content);
      statContent = statisticsJson.toString();
    }

    Map<String, JSONObject> splitStatJson = null;
    if (splitStatistics != null) {
      splitStatJson =  new HashMap<>();
      for (Map.Entry<String, String> entry: splitStatistics.entrySet()){
        splitStatJson.put(entry.getKey(), extractJsonFromContent(entry.getValue()));
      }
    }

    String statisticsFilePath = registerStatistics(project, user, commitTimeStamp, statContent,
        trainingDataset.getName(), "TrainingDatasets", trainingDataset.getVersion(), splitStatJson,
        forTransformation);
    Timestamp commitTime = new Timestamp(commitTimeStamp);
    FeaturestoreStatistic featurestoreStatistic =
        new FeaturestoreStatistic(commitTime, statisticsFilePath, trainingDataset);
    featurestoreStatistic.setForTransformation(forTransformation);
    featurestoreStatistic = featurestoreStatisticFacade.update(featurestoreStatistic);

    // Log statistics activity (we don't log if this is for transformation function)
    if (!forTransformation) {
      fsActivityFacade
          .logStatisticsActivity(user, trainingDataset, new Date(commitTime.getTime()), featurestoreStatistic);
    }

    return featurestoreStatistic;
  }

  private String registerStatistics(Project project, Users user, Long commitTime, String content, String entityName,
                                   String entitySubDir, Integer version, Map<String, JSONObject> splitStatistics,
                                   boolean forTransformation)
      throws DatasetException, HopsSecurityException, IOException {

    DistributedFileSystemOps udfso = null;
    try {
      udfso = dfs.getDfsOps(hdfsUsersController.getHdfsUserName(project, user));

      String dirName = entityName + "_" + version;
      Dataset statistics = getOrCreateStatisticsDataset(project, user);

      // Create the directory
      Path subDir = new Path(Utils.getDatasetPath(statistics, settings), entitySubDir);
      if (!udfso.isDir(subDir.toString())) {
        udfso.mkdir(subDir.toString());
      }
      Path dirPath = new Path(subDir, dirName);
      if (!udfso.isDir(dirPath.toString())) {
        udfso.mkdir(dirPath.toString());
      }

      String path;
      if (splitStatistics != null && !splitStatistics.isEmpty()){
        for (Map.Entry<String, JSONObject> entry: splitStatistics.entrySet()){
          Path filePath = new Path(dirPath,  splitStatisticsFileName(entry.getKey(), commitTime));
          udfso.create(filePath, entry.getValue().toString());
        }
        path = dirPath.toString();
      } else {
        Path filePath;
        if (forTransformation) {
          filePath = new Path(dirPath, transformationFnStatisticsFileName(commitTime));
        } else {
          filePath = new Path(dirPath, commitTime + ".json");
        }
        udfso.create(filePath, content);
        path = filePath.toString();
      }
      return path;
    } finally {
      dfs.closeDfsClient(udfso);
    }
  }
  
  public void deleteStatistics(Project project, Users user, Featuregroup featuregroup)
      throws FeaturestoreException {
    deleteStatisticsDir(project, user, featuregroup.getName(), "FeatureGroups", featuregroup.getVersion());
    featurestoreStatisticFacade.deleteByFeatureGroup(featuregroup);
  }

  public void deleteStatistics(Project project, Users user, TrainingDataset trainingDataset)
      throws FeaturestoreException {
    deleteStatisticsDir(project, user, trainingDataset.getName(), "TrainingDatasets", trainingDataset.getVersion());
    featurestoreStatisticFacade.deleteByTrainingDataset(trainingDataset);
  }
  
  private void deleteStatisticsDir(Project project, Users user, String entityName, String entitySubDir,
    Integer version) throws FeaturestoreException {
    DistributedFileSystemOps udfso = null;
    try {
      udfso = dfs.getDfsOps(hdfsUsersController.getHdfsUserName(project, user));

      String dirName = entityName + "_" + version;
      Dataset statistics = getOrCreateStatisticsDataset(project, user);

      // Construct the directory path
      Path subDir = new Path(Utils.getDatasetPath(statistics, settings), entitySubDir);
      Path dirPath = new Path(subDir, dirName);

      // delete json files
      udfso.rm(dirPath, true);
    } catch (DatasetException | HopsSecurityException | IOException e) {
      throw new FeaturestoreException(RESTCodes.FeaturestoreErrorCode.ERROR_DELETING_STATISTICS,
          Level.WARNING, "", e.getMessage(), e);
    } finally {
      dfs.closeDfsClient(udfso);
    }
  }
  
  private Dataset getOrCreateStatisticsDataset(Project project, Users user)
      throws DatasetException, HopsSecurityException {
    Optional<Dataset> statsDataset = project.getDatasetCollection().stream()
        .filter(d -> d.getName().equals(Settings.ServiceDataset.STATISTICS.getName()))
        .findFirst();
    // This is the case of an old project without STATISTICS dataset, create it.
    if (statsDataset.isPresent()) {
      return statsDataset.get();
    } else {
      return createStatisticsDataset(project, user);
    }
  }

  private Dataset createStatisticsDataset(Project project, Users user) throws DatasetException, HopsSecurityException {
    // Needs super user privileges as we are creating a dataset
    DistributedFileSystemOps dfso = null;
    try {
      dfso = dfs.getDfsOps();
      return datasetController.createDataset(user, project,
          Settings.ServiceDataset.STATISTICS.getName(),
          Settings.ServiceDataset.STATISTICS.getDescription(),
          Provenance.Type.DISABLED.dto, false, DatasetAccessPermission.EDITABLE, dfso);
    } finally {
      dfs.closeDfsClient(dfso);
    }
  }

  private JSONObject extractJsonFromContent(String content) throws FeaturestoreException {
    // In some cases Deequ returns NaN. Having NaNs in the frontend causes issue to the display
    // By converting the string to JSONObject and back to string, JSONObject is going to fix them and
    // potentially other errors
    JSONObject statisticsJson = null;
    try {
      statisticsJson = new JSONObject(content);
    } catch (JSONException jex) {
      throw new FeaturestoreException(RESTCodes.FeaturestoreErrorCode.ERROR_SAVING_STATISTICS,
          Level.WARNING, "Not a valid JSON", jex.getMessage(), jex);
    }
    return statisticsJson;
  }

  private String splitStatisticsFileName(String name, Long commitTime) {
    return name + "_" +  commitTime + ".json";
  }

  private String transformationFnStatisticsFileName(Long commitTime) {
    return "transformation_fn" + "_" +  commitTime + ".json";
  }
  
  private String readOrDeleteStatisticsFile(Project project, Users user, FeaturestoreStatistic statistic,
      String filePath) throws FeaturestoreException {
    try {
      return readFile(project, user, filePath);
    } catch (FeaturestoreException e) {
      if (e.getCause() instanceof FileNotFoundException) {
        // if statistics file doesn't exist, remove statistics instance in the DB
        logger.info("Deleting statistics due to missing statistics file with path: " + filePath);
        featurestoreStatisticFacade.delete(statistic);
      }
      throw e;
    }
  }
  
  private String readFile(Project project, Users user, String path) throws FeaturestoreException {
    DistributedFileSystemOps udfso = null;
    try {
      udfso = dfs.getDfsOps(hdfsUsersController.getHdfsUserName(project, user));
      return udfso.cat(path);
    } catch (IOException e) {
      throw new FeaturestoreException(RESTCodes.FeaturestoreErrorCode.STATISTICS_READ_ERROR,
          Level.WARNING, "Statistics file not found", e.getMessage(), e);
    } finally {
      dfs.closeDfsClient(udfso);
    }
  }
}
