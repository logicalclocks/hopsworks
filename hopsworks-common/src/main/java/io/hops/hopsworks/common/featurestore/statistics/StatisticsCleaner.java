/*
 * This file is part of Hopsworks
 * Copyright (C) 2024, Hopsworks AB. All rights reserved
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

import io.hops.hopsworks.common.hdfs.DistributedFileSystemOps;
import io.hops.hopsworks.common.hdfs.DistributedFsService;
import io.hops.hopsworks.common.util.PayaraClusterManager;
import io.hops.hopsworks.common.util.Settings;
import io.hops.hopsworks.persistence.entity.featurestore.statistics.FeatureDescriptiveStatistics;
import org.javatuples.Pair;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.annotation.Resource;
import javax.ejb.AccessTimeout;
import javax.ejb.DependsOn;
import javax.ejb.EJB;
import javax.ejb.Lock;
import javax.ejb.LockType;
import javax.ejb.Singleton;
import javax.ejb.Startup;
import javax.ejb.Timeout;
import javax.ejb.Timer;
import javax.ejb.TimerConfig;
import javax.ejb.TimerService;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import java.io.IOException;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

@Singleton
@Startup
@DependsOn("Settings")
@TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
public class StatisticsCleaner {
  
  private final static Logger LOG = Logger.getLogger(StatisticsCleaner.class.getName());
  
  private int batchSize;
  
  @EJB
  private Settings settings;
  @EJB
  private FeatureDescriptiveStatisticsFacade featureDescriptiveStatisticsFacade;
  @EJB
  private DistributedFsService dfs;
  @EJB
  private PayaraClusterManager payaraClusterManager;
  @Resource
  private TimerService timerService;
  private Timer timer;
  
  @PostConstruct
  @TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
  public void init() {
    batchSize = settings.getStatisticsCleanerBatchSize();
    timer = timerService.createIntervalTimer(10L, settings.getStatisticsCleanerInterval(),
      new TimerConfig("StatisticsCleaner", false));
  }
  
  @PreDestroy
  private void destroyTimer() {
    if (timer != null) {
      timer.cancel();
    }
  }
  
  /**
   * Timer bean to periodically delete feature descriptive statistics that do not belong to any feature group,
   * feature view, training dataset or feature monitoring result. ON DELETE is set to NO ACTION on all the
   * FK constraints between these tables and feature descriptive statistics table in the database. As a result,
   * feature descriptive statistics need to be cleaned up periodically in this timer. This is done to avoid having
   * the database trying to delete too many feature descriptive statistics records when the corresponding related
   * table is deleted which would results in transactions timeouts and potential database overload.
   * We do not use orphanRemoval on the JPA entity to allow for a more flexible UX where there number of feature
   * descriptive statistics can grow to more than what the database can handle to remove in a single transaction.
   * This timer accepts a batch size that defines how many feature descriptive statistics are to be deleted par
   * trigger event. For example, if a feature group with 10000 feature descriptive statistics is deleted, the timer
   * can remove the feature descriptive statistics in batches of 1000.
   *
   * @param timer
   *   timer
   */
  @Lock(LockType.WRITE)
  @AccessTimeout(value = 1000)
  @TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
  @Timeout
  public void deleteOrphanStatistics(Timer timer) {
    if (!payaraClusterManager.amIThePrimary()) {
      return;
    }
    
    DistributedFileSystemOps udfso = null;
    try {
      // delete as superuser as the user might have been removed from the system
      udfso = dfs.getDfsOps();
      
      LOG.log(Level.FINE, "DeleteOrphanStatistics start");
      List<FeatureDescriptiveStatistics> fds =
        featureDescriptiveStatisticsFacade.findOrphaned(new Pair<>(0, batchSize))
            .stream()
            .filter(f -> !f.getFeatureName().equals("for-migration") && !f.getFeatureName().equals("to-be-deleted"))
            .collect(Collectors.toList());
      
      while (!fds.isEmpty()) {
        String fdsIds =
          fds.stream().map(FeatureDescriptiveStatistics::getId).map(Object::toString).collect(Collectors.joining(", "));
        LOG.log(Level.INFO, "Deleting orphaned statistics: " + fdsIds);
        // delete extended stats files
        deleteExtendedStatsFiles(fds, udfso);
        // delete from database
        featureDescriptiveStatisticsFacade.batchDelete(fds);
        // get next batch
        fds = featureDescriptiveStatisticsFacade.findOrphaned(new Pair<>(0, batchSize));
      }
    } catch (Exception ex) {
      LOG.log(Level.SEVERE, "StatisticsCleaner timer error", ex);
    } finally {
      dfs.closeDfsClient(udfso);
    }
    LOG.log(Level.FINE, "DeleteOrphanStatistics end");
  }
  
  private void deleteExtendedStatsFiles(List<FeatureDescriptiveStatistics> fds, DistributedFileSystemOps udfso)
      throws IOException {
    for (FeatureDescriptiveStatistics stats : fds) {
      if (stats.getExtendedStatisticsPath() != null) {
        udfso.rm(stats.getExtendedStatisticsPath(), true);
      }
    }
  }
}