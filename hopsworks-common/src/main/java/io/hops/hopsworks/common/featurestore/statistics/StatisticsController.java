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
import io.hops.hopsworks.common.featurestore.featuregroup.cached.FeatureGroupCommitController;
import io.hops.hopsworks.common.featurestore.activity.FeaturestoreActivityFacade;
import io.hops.hopsworks.common.hdfs.DistributedFileSystemOps;
import io.hops.hopsworks.common.hdfs.DistributedFsService;
import io.hops.hopsworks.common.hdfs.HdfsUsersController;
import io.hops.hopsworks.common.hdfs.inode.InodeController;
import io.hops.hopsworks.common.provenance.core.Provenance;
import io.hops.hopsworks.common.util.Settings;
import io.hops.hopsworks.exceptions.DatasetException;
import io.hops.hopsworks.exceptions.FeaturestoreException;
import io.hops.hopsworks.exceptions.HopsSecurityException;
import io.hops.hopsworks.persistence.entity.dataset.Dataset;
import io.hops.hopsworks.persistence.entity.dataset.DatasetAccessPermission;
import io.hops.hopsworks.persistence.entity.featurestore.featuregroup.Featuregroup;
import io.hops.hopsworks.persistence.entity.featurestore.featuregroup.FeaturegroupType;
import io.hops.hopsworks.persistence.entity.featurestore.statistics.FeaturestoreStatistic;
import io.hops.hopsworks.persistence.entity.featurestore.featuregroup.cached.FeatureGroupCommit;
import io.hops.hopsworks.persistence.entity.featurestore.featuregroup.cached.TimeTravelFormat;
import io.hops.hopsworks.persistence.entity.featurestore.trainingdataset.TrainingDataset;
import io.hops.hopsworks.persistence.entity.hdfs.inode.Inode;
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
import java.io.IOException;
import java.sql.Timestamp;
import java.util.Date;
import java.util.Optional;
import java.util.logging.Level;

@Stateless
@TransactionAttribute(TransactionAttributeType.NEVER)
public class StatisticsController {

  @EJB
  private InodeController inodeController;
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

  public String readStatisticsContent(Project project, Users user, FeaturestoreStatistic statistic)
      throws FeaturestoreException {
    String path = inodeController.getPath(statistic.getInode());

    DistributedFileSystemOps udfso = null;
    try {
      udfso = dfs.getDfsOps(hdfsUsersController.getHdfsUserName(project, user));
      return udfso.cat(path);
    } catch (IOException e) {
      throw new FeaturestoreException(RESTCodes.FeaturestoreErrorCode.STATISTICS_READ_ERROR,
          Level.WARNING, e.getMessage(), e.getMessage(), e);
    } finally {
      dfs.closeDfsClient(udfso);
    }
  }

  public FeaturestoreStatistic registerStatistics(Project project, Users user, Long statisticsCommitTimeStamp,
                                                  Long fgCommitId, String content, Featuregroup featuregroup)
      throws FeaturestoreException, DatasetException, HopsSecurityException, IOException {
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

    FeatureGroupCommit featureGroupCommit = null;
    if (featuregroup.getFeaturegroupType() == FeaturegroupType.CACHED_FEATURE_GROUP &&
        featuregroup.getCachedFeaturegroup().getTimeTravelFormat() == TimeTravelFormat.HUDI) {
      featureGroupCommit = featureGroupCommitCommitController.findCommitByDate(featuregroup, fgCommitId);
      // Statistics commitTimeStamp will be always system time sent from client if user wants to recompute
      // statistics on particular commit id (i.e. fgCommitId was provided). If fgCommitId is null it means its
      // it means: 1) client issued save or insert method; here statistics commitTimeStamp will be featureGroupCommit;
      // 2) Or it is recomputing statistics of existing time travel enabled feature group. Here latest fg commit
      // timestamp will be used to read dataset and as statistics commit time client system time will be provided.

      // if statistics was never saved for this commit then it will return null
      FeaturestoreStatistic statisticsFgCommit = featurestoreStatisticFacade.findFGStatisticsByCommitTime(
          featuregroup, featureGroupCommit.getCommittedOn()).orElse(null);

      statisticsCommitTimeStamp = statisticsFgCommit == null
           ? featureGroupCommit.getCommittedOn()
           : statisticsCommitTimeStamp;
    }

    Inode statisticsInode = registerStatistics(project, user, statisticsCommitTimeStamp, statisticsJson.toString(),
        featuregroup.getName(), "FeatureGroups", featuregroup.getVersion());
    Timestamp commitTime = new Timestamp(statisticsCommitTimeStamp);

    FeaturestoreStatistic featurestoreStatistic = new FeaturestoreStatistic(commitTime, statisticsInode, featuregroup);
    if (featuregroup.getFeaturegroupType() == FeaturegroupType.CACHED_FEATURE_GROUP &&
        featuregroup.getCachedFeaturegroup().getTimeTravelFormat() == TimeTravelFormat.HUDI)  {
      featurestoreStatistic.setFeatureGroupCommit(featureGroupCommit);
    }
    featurestoreStatistic = featurestoreStatisticFacade.update(featurestoreStatistic);

    // Log statistics activity
    fsActivityFacade.logStatisticsActivity(user, featuregroup, new Date(commitTime.getTime()), featurestoreStatistic);

    return featurestoreStatistic;
  }

  public FeaturestoreStatistic registerStatistics(Project project, Users user, Long commitTimeStamp, String content,
                                                  TrainingDataset trainingDataset)
      throws FeaturestoreException, DatasetException, HopsSecurityException, IOException {
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

    Inode statisticsInode = registerStatistics(project, user, commitTimeStamp, statisticsJson.toString(),
        trainingDataset.getName(), "TrainingDatasets", trainingDataset.getVersion());
    Timestamp commitTime = new Timestamp(commitTimeStamp);
    FeaturestoreStatistic featurestoreStatistic =
      new FeaturestoreStatistic(commitTime, statisticsInode, trainingDataset);
    featurestoreStatistic = featurestoreStatisticFacade.update(featurestoreStatistic);

    // Log statistics activity
    fsActivityFacade
        .logStatisticsActivity(user, trainingDataset, new Date(commitTime.getTime()), featurestoreStatistic);

    return featurestoreStatistic;
  }

  private Inode registerStatistics(Project project, Users user, Long commitTime, String content, String entityName,
                                  String entitySubDir, Integer version)
      throws DatasetException, HopsSecurityException, IOException {

    DistributedFileSystemOps udfso = null;
    try {
      udfso = dfs.getDfsOps(hdfsUsersController.getHdfsUserName(project, user));

      String dirName = entityName + "_" + version;
      Dataset statistics = getOrCreateStatisticsDataset(project, user);

      // Create the directory
      Path subDir = new Path(datasetController.getDatasetPath(statistics), entitySubDir);
      if (!udfso.isDir(subDir.toString())) {
        udfso.mkdir(subDir.toString());
      }
      Path dirPath = new Path(subDir, dirName);
      if (!udfso.isDir(dirPath.toString())) {
        udfso.mkdir(dirPath.toString());
      }

      Path filePath = new Path(dirPath, commitTime + ".json");
      udfso.create(filePath, content);

      return inodeController.getInodeAtPath(filePath.toString());
    } finally {
      dfs.closeDfsClient(udfso);
    }
  }

  public void deleteStatistics(Project project, Users user, Featuregroup featuregroup)
    throws FeaturestoreException {
    deleteStatistics(project, user, featuregroup.getName(), "FeatureGroups", featuregroup.getVersion());
  }

  public void deleteStatistics(Project project, Users user, TrainingDataset trainingDataset)
    throws FeaturestoreException {
    deleteStatistics(project, user, trainingDataset.getName(), "TrainingDatasets", trainingDataset.getVersion());
  }

  private void deleteStatistics(Project project, Users user, String entityName, String entitySubDir, Integer version)
    throws FeaturestoreException {
    DistributedFileSystemOps udfso = null;
    try {
      udfso = dfs.getDfsOps(hdfsUsersController.getHdfsUserName(project, user));

      String dirName = entityName + "_" + version;
      Dataset statistics = getOrCreateStatisticsDataset(project, user);

      // Construct the directory path
      Path subDir = new Path(datasetController.getDatasetPath(statistics), entitySubDir);
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
}
