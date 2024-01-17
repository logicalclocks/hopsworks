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

import io.hops.hopsworks.common.dao.AbstractFacade;
import io.hops.hopsworks.common.dataset.DatasetController;
import io.hops.hopsworks.common.featurestore.activity.FeaturestoreActivityFacade;
import io.hops.hopsworks.common.featurestore.featuregroup.FeaturegroupController;
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
import io.hops.hopsworks.persistence.entity.featurestore.statistics.FeatureDescriptiveStatistics;
import io.hops.hopsworks.persistence.entity.featurestore.statistics.FeatureGroupDescriptiveStatistics;
import io.hops.hopsworks.persistence.entity.featurestore.statistics.FeatureGroupStatistics;
import io.hops.hopsworks.persistence.entity.featurestore.statistics.TrainingDatasetStatistics;
import io.hops.hopsworks.persistence.entity.featurestore.trainingdataset.TrainingDataset;
import io.hops.hopsworks.persistence.entity.featurestore.trainingdataset.split.SplitName;
import io.hops.hopsworks.persistence.entity.project.Project;
import io.hops.hopsworks.persistence.entity.user.Users;
import io.hops.hopsworks.restutils.RESTCodes;
import org.apache.hadoop.fs.Path;
import org.javatuples.Pair;
import org.json.JSONException;
import org.json.JSONObject;

import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import java.io.IOException;
import java.sql.Timestamp;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.logging.Level;
import java.util.stream.Collectors;

@Stateless
@TransactionAttribute(TransactionAttributeType.NEVER)
public class StatisticsController {
  
  @EJB
  private DistributedFsService dfs;
  @EJB
  private DatasetController datasetController;
  @EJB
  private HdfsUsersController hdfsUsersController;
  @EJB
  private FeatureGroupStatisticsFacade featureGroupStatisticsFacade;
  @EJB
  private FeatureGroupDescriptiveStatisticsFacade featureGroupDescriptiveStatisticsFacade;
  @EJB
  private TrainingDatasetStatisticsFacade trainingDatasetStatisticsFacade;
  @EJB
  private FeatureGroupCommitController featureGroupCommitController;
  @EJB
  private FeaturestoreActivityFacade fsActivityFacade;
  @EJB
  private Settings settings;
  @EJB
  private FeatureDescriptiveStatisticsFacade featureDescriptiveStatisticsFacade;
  
  // Feature Group Statistics: get, register, delete
  
  public AbstractFacade.CollectionInfo<FeatureGroupStatistics> getStatisticsByFeatureGroup(Integer offset,
    Integer limit, Set<? extends AbstractFacade.SortBy> sorts, Set<? extends AbstractFacade.FilterBy> filters,
    Featuregroup featuregroup) throws FeaturestoreException {
    // return one or more Feature Group Statistics with all the Feature Descriptive Statistics.
    overwriteFiltersIfNeeded(featuregroup, (Set) filters);
    return featureGroupStatisticsFacade.findByFeaturegroup(offset, limit, sorts, filters, featuregroup);
  }
  
  public AbstractFacade.CollectionInfo<FeatureGroupStatistics> getStatisticsByFeatureGroupAndFeatureNames(
    Integer offset, Integer limit, Set<? extends AbstractFacade.SortBy> sorts,
    Set<? extends AbstractFacade.FilterBy> filters, Set<String> featureNames, Featuregroup featuregroup)
    throws FeaturestoreException {
    // return one or more Feature Group Statistics with the specified Feature Descriptive Statistics.
    overwriteFiltersIfNeeded(featuregroup, (Set) filters);
    return featureGroupStatisticsFacade.findByFeaturegroupWithFeatureNames(offset, limit, sorts, filters,
      featureNames, featuregroup);
  }

  public FeatureGroupStatistics registerFeatureGroupStatistics(Project project, Users user,
    Long computationTime, Long windowStartCommitTime, Long windowEndCommitTime, Float rowPercentage,
    Collection<FeatureDescriptiveStatistics> descriptiveStatistics, Featuregroup featuregroup)
    throws FeaturestoreException, IOException, DatasetException, HopsSecurityException {
    // Register statistics computed on a specific commit, commit window or the whole feature group (no time-travel
    // enabled). In case of a specific commit, the commit window provided has the same start and end commit.
    Timestamp computationTimestamp = new Timestamp(computationTime);
    FeatureGroupStatistics statistics;
    if (!FeaturegroupController.isTimeTravelEnabled(featuregroup)) { // time-travel not enabled
      // register extended statistics (hdfs files)
      registerExtendedStatistics(project, user, featuregroup.getName(), featuregroup.getVersion(), "FeatureGroups",
        null, computationTime, false, null, descriptiveStatistics);
      statistics = featureGroupStatisticsFacade.update(
        new FeatureGroupStatistics(computationTimestamp, rowPercentage, descriptiveStatistics, featuregroup));
    } else {
      // otherwise, time-travel enabled
      Pair<Long, Long> startEndCommitTimes =
        featureGroupCommitController.getStartEndCommitTimesByWindowTime(featuregroup, windowStartCommitTime,
          windowEndCommitTime);
      windowStartCommitTime = startEndCommitTimes.getValue0();
      windowEndCommitTime = startEndCommitTimes.getValue1();
      
      registerExtendedStatistics(project, user, featuregroup.getName(), featuregroup.getVersion(), "FeatureGroups",
        windowStartCommitTime, windowEndCommitTime, false, null, descriptiveStatistics);
      
      // check if feature group statistics already exist by querying feature group statistics without the feature names.
      Set<StatisticsFilterBy> filters =
        buildStatisticsQueryFilters(windowStartCommitTime, windowEndCommitTime, rowPercentage, null);
      AbstractFacade.CollectionInfo<FeatureGroupStatistics> fgs =
        getStatisticsByFeatureGroup(0, 1, null, filters, featuregroup);
      if (fgs.getCount() > 0) {
        // if feature group statistics exist, append descriptive statistics
        statistics = registerFeatureGroupDescriptiveStatistics(filters, descriptiveStatistics, fgs.getItems().get(0),
          featuregroup);
      } else {
        // otherwise, create new feature group statistics
        statistics = featureGroupStatisticsFacade.update(
          new FeatureGroupStatistics(computationTimestamp, windowStartCommitTime, windowEndCommitTime, rowPercentage,
            descriptiveStatistics, featuregroup));
      }
    }
    // Log statistics activity
    fsActivityFacade.logStatisticsActivity(user, featuregroup, new Date(computationTimestamp.getTime()), statistics);
    return statistics;
  }
  
  public void deleteFeatureGroupStatistics(Project project, Users user, Featuregroup featuregroup)
      throws FeaturestoreException {
    deleteExtendedStatistics(project, user, featuregroup.getName(), featuregroup.getVersion(), "FeatureGroups");
  }
  
  // Training Dataset Statistics
  
  public AbstractFacade.CollectionInfo<TrainingDatasetStatistics> getStatisticsByTrainingDataset(
    Set<? extends AbstractFacade.FilterBy> filters, TrainingDataset trainingDataset) {
    return trainingDatasetStatisticsFacade.findByTrainingDataset(filters, trainingDataset);
  }
  
  public AbstractFacade.CollectionInfo<TrainingDatasetStatistics> getStatisticsByTrainingDatasetAndFeatureNames(
    Set<? extends AbstractFacade.FilterBy> filters, Set<String> featureNames, TrainingDataset trainingDataset)
    throws FeaturestoreException {
    return trainingDatasetStatisticsFacade.findByTrainingDatasetWithFeatureNames(filters, featureNames,
      trainingDataset);
  }

  public TrainingDatasetStatistics registerTrainingDatasetStatistics(Project project, Users user, Long computationTime,
    boolean beforeTransformation, Float rowPercentage, Collection<FeatureDescriptiveStatistics> descriptiveStatistics,
    TrainingDataset trainingDataset)
    throws DatasetException, HopsSecurityException, IOException, FeaturestoreException {
    // write extended statistics files into hopsFS
    registerExtendedStatistics(project, user, trainingDataset.getName(), trainingDataset.getVersion(),
      "TrainingDatasets", null, computationTime, beforeTransformation, null, descriptiveStatistics);
    
    Timestamp computationTimestamp = new Timestamp(computationTime);
    TrainingDatasetStatistics statistics = new TrainingDatasetStatistics(computationTimestamp, rowPercentage,
      descriptiveStatistics, trainingDataset);
    statistics.setForTransformation(beforeTransformation);
    
    // overwrite statistics if they already exist, keeping the same IDs
    setExistingTrainingDatasetStatisticsIDs(trainingDataset, statistics, beforeTransformation, rowPercentage);
    statistics = trainingDatasetStatisticsFacade.update(statistics);

    // Log statistics activity (we don't log if this is before transformation function)
    if (!beforeTransformation) {
      fsActivityFacade.logStatisticsActivity(user, trainingDataset, new Date(computationTimestamp.getTime()),
        statistics);
    }
    return statistics;
  }
  
  public TrainingDatasetStatistics registerTrainingDatasetSplitStatistics(Project project, Users user,
    Long computationTime, Float rowPercentage, Map<String, Collection<FeatureDescriptiveStatistics>> splitStatistics,
    TrainingDataset trainingDataset)
    throws DatasetException, HopsSecurityException, IOException, FeaturestoreException {
    // write extended statistics files into hopsFS
    for (Map.Entry<String, Collection<FeatureDescriptiveStatistics>> entry : splitStatistics.entrySet()) {
      registerExtendedStatistics(project, user, trainingDataset.getName(), trainingDataset.getVersion(),
        "TrainingDatasets", null, computationTime, false, entry.getKey(), entry.getValue());
    }
    Timestamp computationTimestamp = new Timestamp(computationTime);
    TrainingDatasetStatistics statistics = new TrainingDatasetStatistics(computationTimestamp, rowPercentage,
      splitStatistics.get(SplitName.TRAIN.getName()), splitStatistics.get(SplitName.TEST.getName()),
      splitStatistics.getOrDefault(SplitName.VALIDATION.getName(), null),
      trainingDataset);
      
    // overwrite statistics if they already exist, keeping the same IDs
    setExistingTrainingDatasetStatisticsIDs(trainingDataset, statistics, false, rowPercentage);
    statistics = trainingDatasetStatisticsFacade.update(statistics);
    
    // Log statistics activity
    fsActivityFacade.logStatisticsActivity(user, trainingDataset, new Date(computationTimestamp.getTime()), statistics);
    return statistics;
  }
  
  public void deleteTrainingDatasetStatistics(Project project, Users user, TrainingDataset trainingDataset)
    throws FeaturestoreException {
    deleteExtendedStatistics(project, user, trainingDataset.getName(), trainingDataset.getVersion(),
      "TrainingDatasets");
  }
  
  private void setExistingTrainingDatasetStatisticsIDs(TrainingDataset trainingDataset,
    TrainingDatasetStatistics statistics, Boolean beforeTransformation, Float rowPercentage) {
    Set<StatisticsFilterBy> filters = buildStatisticsQueryFilters(null, null, rowPercentage, beforeTransformation);
    AbstractFacade.CollectionInfo<TrainingDatasetStatistics> tdStatisticsList =
      trainingDatasetStatisticsFacade.findByTrainingDataset(filters, trainingDataset);
    if (tdStatisticsList != null && tdStatisticsList.getCount() == 1) {
      TrainingDatasetStatistics existingStatistics = tdStatisticsList.getItems().get(0);
      // set ID of existing statistics
      statistics.setId(existingStatistics.getId());
      // set IDs of existing descriptive statistics
      setExistingFeatureDescriptiveStatisticsIDs(statistics.getTrainFeatureDescriptiveStatistics(),
        existingStatistics.getTrainFeatureDescriptiveStatistics());
      setExistingFeatureDescriptiveStatisticsIDs(statistics.getTestFeatureDescriptiveStatistics(),
        existingStatistics.getTestFeatureDescriptiveStatistics());
      setExistingFeatureDescriptiveStatisticsIDs(statistics.getValFeatureDescriptiveStatistics(),
        existingStatistics.getValFeatureDescriptiveStatistics());
    }
  }
  
  // Feature Descriptive Statistics
  
  private FeatureGroupStatistics registerFeatureGroupDescriptiveStatistics(Set<StatisticsFilterBy> filters,
    Collection<FeatureDescriptiveStatistics> descriptiveStatistics, FeatureGroupStatistics fgStatistics,
    Featuregroup featuregroup) throws FeaturestoreException {
    // register descriptive statistics in an existing fg statistics. Descriptive statistics for a specific feature
    // might or might not be already computed and stored in the DB. If the feature descriptive statistics already
    // exists, it is updated.
    Set<String> featureNames =
      descriptiveStatistics.stream().map(FeatureDescriptiveStatistics::getFeatureName).collect(Collectors.toSet());
    
    // TODO: Optimize this query by using the Feature Group Statistics ID obtained in the previous step, instead of
    //  the filters and limit
    AbstractFacade.CollectionInfo<FeatureGroupStatistics> fgsWithFeatures =
      getStatisticsByFeatureGroupAndFeatureNames(0, 1, null, filters, featureNames, featuregroup);
    
    HashMap<String, FeatureDescriptiveStatistics> existingFds = null;
    if (fgsWithFeatures.getCount() > 0) {
      // filter existing feature descriptive statistics
      existingFds = fgsWithFeatures.getItems().get(0).getFeatureDescriptiveStatistics().stream()
        .filter(ds -> featureNames.contains(ds.getFeatureName()))
          .collect(Collectors.toMap(FeatureDescriptiveStatistics::getFeatureName,
            Function.identity(), (prev, next) -> next, HashMap::new));
    }
    
    // append / update feature descriptive statistics using the intermediate table FeatureGroupDescriptiveStatistics
    for (FeatureDescriptiveStatistics fds : descriptiveStatistics) {
      if (existingFds != null && existingFds.containsKey(fds.getFeatureName())) {
        // if feature descriptive statistics exists, set the ID so the statistics get updated
        fds.setId(existingFds.get(fds.getFeatureName()).getId());
      }
      // persist feature descriptive statistics
      fds = featureDescriptiveStatisticsFacade.update(fds);
      // persist feature group descriptive statistics to intermediate table
      FeatureGroupDescriptiveStatistics fgds = new FeatureGroupDescriptiveStatistics(fgStatistics, fds);
      featureGroupDescriptiveStatisticsFacade.update(fgds);
    }
    return fgStatistics;
  }
  
  public Set<StatisticsFilterBy> buildStatisticsQueryFilters(Long windowStartCommitTime,
    Long windowEndCommitTime, Float rowPercentage, Boolean beforeTransformation) {
    Set<StatisticsFilterBy> filters = new HashSet<>();
    filters.add(new StatisticsFilterBy(
      StatisticsFilters.Filters.ROW_PERCENTAGE_EQ,
      rowPercentage != null ? String.valueOf(rowPercentage) : null));
    if (windowStartCommitTime != null) {
      filters.add(new StatisticsFilterBy(
        StatisticsFilters.Filters.WINDOW_START_COMMIT_TIME_EQ, String.valueOf(windowStartCommitTime)));
    }
    if (windowEndCommitTime != null) {
      filters.add(new StatisticsFilterBy(
        StatisticsFilters.Filters.WINDOW_END_COMMIT_TIME_EQ, String.valueOf(windowEndCommitTime)));
    }
    if (beforeTransformation != null) {
      filters.add(new StatisticsFilterBy(
        StatisticsFilters.Filters.BEFORE_TRANSFORMATION_EQ, String.valueOf(beforeTransformation)));
    }
    return filters;
  }
  
  public void setExistingFeatureDescriptiveStatisticsIDs(Collection<FeatureDescriptiveStatistics> statistics,
    Collection<FeatureDescriptiveStatistics> existingStatistics) {
    if (statistics == null || existingStatistics == null) {
      return;
    }
    Map<String, Integer> existingStatsMap = existingStatistics.stream()
      .collect(Collectors.toMap(FeatureDescriptiveStatistics::getFeatureName, FeatureDescriptiveStatistics::getId));
    // overwrite the IDs
    for (FeatureDescriptiveStatistics fds : statistics) {
      if (existingStatsMap.containsKey(fds.getFeatureName())) {
        Integer id = existingStatsMap.get(fds.getFeatureName());
        fds.setId(id);
      }
    }
  }
  
  // Extended statistic: read, persist, delete statistics file in hopsfs
  
  public void appendExtendedStatistics(Project project, Users user,
    Collection<FeatureDescriptiveStatistics> descriptiveStatistics) throws FeaturestoreException {
    // read and append extended statistics (from hdfs files) to feature descriptive statistics
    for (FeatureDescriptiveStatistics fds : descriptiveStatistics) {
      if (fds.getExtendedStatisticsPath() != null) {
        String stats = readExtendedStatistics(project, user, fds.getExtendedStatisticsPath());
        fds.setExtendedStatistics(stats);
      }
    }
  }
  
  private void registerExtendedStatistics(Project project, Users user, String entityName, Integer version,
    String entitySubDir, Long startCommitTime, Long endCommitTime, Boolean beforeTransformation, String splitName,
    Collection<FeatureDescriptiveStatistics> descriptiveStatistics)
    throws IOException, DatasetException, HopsSecurityException, FeaturestoreException {
    DistributedFileSystemOps udfso = null;
    try {
      udfso = dfs.getDfsOps(hdfsUsersController.getHdfsUserName(project, user));
      Path dirPath = getExtendedStatisticsDirPath(project, user, udfso, entityName, version, entitySubDir);
      
      for (FeatureDescriptiveStatistics fds : descriptiveStatistics) {
        if (fds.getExtendedStatistics() != null) {
          String path = createExtendedStatisticsFile(startCommitTime, endCommitTime, fds.getFeatureName(),
            fds.getExtendedStatistics(), beforeTransformation, splitName, udfso, dirPath);
          fds.setExtendedStatisticsPath(path);
        }
      }
    } finally {
      dfs.closeDfsClient(udfso);
    }
  }
  
  private String createExtendedStatisticsFile(Long startTime, Long endTime, String featureName,
    String extendedStatistics, Boolean beforeTransformation, String splitName, DistributedFileSystemOps udfso,
    Path dirPath) throws IOException, FeaturestoreException {
    // Persist histograms and correlations on a file in hopsfs
    Path filePath;
    if (beforeTransformation) {
      filePath = new Path(dirPath, transformationFnStatisticsFileName(startTime, endTime, featureName));
    } else {
      if (splitName != null) {
        filePath = new Path(dirPath, splitStatisticsFileName(splitName, endTime, featureName));
      } else {
        filePath = new Path(dirPath, statisticsFileName(startTime, endTime, featureName));
      }
    }
    extendedStatistics = sanitizeExtendedStatistics(extendedStatistics);
    udfso.create(filePath, extendedStatistics);
    return filePath.toString();
  }
  
  private Path getExtendedStatisticsDirPath(Project project, Users user, DistributedFileSystemOps udfso,
    String entityName, Integer version, String entitySubDir) throws DatasetException, HopsSecurityException,
    IOException {
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
    return dirPath;
  }
  
  private void deleteExtendedStatistics(Project project, Users user, String entityName, Integer version,
    String entitySubDir)
    throws FeaturestoreException {
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
      throw new FeaturestoreException(RESTCodes.FeaturestoreErrorCode.ERROR_DELETING_STATISTICS, Level.WARNING, "",
        e.getMessage(), e);
    } finally {
      dfs.closeDfsClient(udfso);
    }
  }
  
  // Utils
  
  public Pair<Long, Long> extractTimeWindowFromFilters(Set<AbstractFacade.FilterBy> filters,
    StatisticsFilters.Filters windowStartFilter, StatisticsFilters.Filters windowEndFilter) {
    Long windowStartTime = null, windowEndTime = null;
    for (AbstractFacade.FilterBy filter : filters) {
      if (filter.getValue().equals(windowStartFilter.getValue())) {
        windowStartTime = Long.valueOf(filter.getParam());
        continue;
      }
      if (filter.getValue().equals(windowEndFilter.getValue())) {
        windowEndTime = Long.valueOf(filter.getParam());
      }
    }
    return new Pair<>(windowStartTime, windowEndTime);
  }
  
  private void overwriteFiltersIfNeeded(Featuregroup featuregroup, Set<AbstractFacade.FilterBy> filters)
    throws FeaturestoreException {
    if (FeaturegroupController.isTimeTravelEnabled(featuregroup)) {
      // if time-travel feature group, check if exact commit time windows are provided
      Set<AbstractFacade.FilterBy> filterSet = filters;
      Pair<Long, Long> windowCommitTimes = extractTimeWindowFromFilters(filterSet,
        StatisticsFilters.Filters.WINDOW_START_COMMIT_TIME_EQ,
        StatisticsFilters.Filters.WINDOW_END_COMMIT_TIME_EQ);
      if (windowCommitTimes.getValue0() != null || windowCommitTimes.getValue1() != null) {
        // if specific commit time window, we need to adjust the user-provided times to the actual commit times
        Pair<Long, Long> startEndCommits =
          featureGroupCommitController.getStartEndCommitTimesByWindowTime(
            featuregroup, windowCommitTimes.getValue0(), windowCommitTimes.getValue1());
        overwriteTimeWindowFilters(
          filterSet, startEndCommits.getValue0(), // new window start time
          startEndCommits.getValue1(), // new window end time
          StatisticsFilters.Filters.WINDOW_START_COMMIT_TIME_EQ,
          StatisticsFilters.Filters.WINDOW_END_COMMIT_TIME_EQ);
      }
    }
  }
  
  public void overwriteTimeWindowFilters(Set<AbstractFacade.FilterBy> filters, Long windowStartTime,
    Long windowEndTime, StatisticsFilters.Filters windowStartFilter,
    StatisticsFilters.Filters windowEndFilter) {
    filters.removeIf(
      f -> f.getValue().equals(windowStartFilter.getValue()) || f.getValue().equals(windowEndFilter.getValue()));
    filters.add(new StatisticsFilterBy(windowStartFilter, String.valueOf(windowStartTime)));
    filters.add(new StatisticsFilterBy(windowEndFilter, String.valueOf(windowEndTime)));
  }
  
  private Dataset getOrCreateStatisticsDataset(Project project, Users user)
    throws DatasetException, HopsSecurityException {
    Optional<Dataset> statsDataset = project.getDatasetCollection().stream()
      .filter(d -> d.getName().equals(Settings.ServiceDataset.STATISTICS.getName())).findFirst();
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
      return datasetController.createDataset(user, project, Settings.ServiceDataset.STATISTICS.getName(),
        Settings.ServiceDataset.STATISTICS.getDescription(), Provenance.Type.DISABLED.dto, false,
        DatasetAccessPermission.EDITABLE, dfso);
    } finally {
      dfs.closeDfsClient(dfso);
    }
  }
  
  private String sanitizeExtendedStatistics(String statistics) throws FeaturestoreException {
    // In some cases Deequ returns NaN. Having NaNs in the frontend causes issue to the display
    // By converting the string to JSONObject and back to string, JSONObject is going to fix them and
    // potentially other errors
    JSONObject statisticsJson = null;
    try {
      statisticsJson = new JSONObject(statistics);
    } catch (JSONException jex) {
      throw new FeaturestoreException(RESTCodes.FeaturestoreErrorCode.ERROR_SAVING_STATISTICS, Level.WARNING,
        "Not a valid JSON", jex.getMessage(), jex);
    }
    return statisticsJson.toString();
  }
  
  private String splitStatisticsFileName(String splitName, Long commitTime, String featureName) {
    return commitTime + "_" + splitName + "_" + featureName + ".json";
  }
  
  private String statisticsFileName(Long startCommitTime, Long endCommitTime, String featureName) {
    String name = startCommitTime != null ? startCommitTime + "_" : "";
    return name + endCommitTime + "_" + featureName + ".json";
  }
  
  private String transformationFnStatisticsFileName(Long startCommitTime, Long endCommitTime, String featureName) {
    String name = "transformation_fn" + "_";
    name += startCommitTime != null ? startCommitTime + "_" : "";
    return name + endCommitTime + "_" + featureName + ".json";
  }
  
  private String readExtendedStatistics(Project project, Users user, String path) throws FeaturestoreException {
    DistributedFileSystemOps udfso = null;
    try {
      udfso = dfs.getDfsOps(hdfsUsersController.getHdfsUserName(project, user));
      return udfso.cat(path);
    } catch (IOException e) {
      throw new FeaturestoreException(RESTCodes.FeaturestoreErrorCode.STATISTICS_READ_ERROR, Level.WARNING,
        e.getMessage(), e.getMessage(), e);
    } finally {
      dfs.closeDfsClient(udfso);
    }
  }
}