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
 *
*/

package io.hops.hopsworks.common.hive;

import io.hops.hopsworks.common.dao.hdfs.inode.Inode;
import io.hops.hopsworks.common.dao.jobhistory.YarnApplicationstateFacade;
import io.hops.hopsworks.common.hdfs.DistributedFileSystemOps;
import io.hops.hopsworks.common.hdfs.DistributedFsService;
import io.hops.hopsworks.common.hdfs.inode.InodeController;
import io.hops.hopsworks.common.util.Settings;
import io.hops.hopsworks.common.yarn.YarnClientService;
import io.hops.hopsworks.common.yarn.YarnClientWrapper;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.yarn.api.records.ApplicationReport;
import org.apache.hadoop.yarn.api.records.QueueInfo;
import org.apache.hadoop.yarn.api.records.YarnApplicationState;
import org.apache.hadoop.yarn.client.api.YarnClient;
import org.apache.hadoop.yarn.exceptions.YarnException;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import javax.ejb.DependsOn;
import javax.ejb.EJB;
import javax.ejb.Singleton;
import javax.ejb.Startup;
import javax.ejb.Timeout;
import javax.ejb.TimerService;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import java.io.IOException;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

@Singleton
@Startup
@DependsOn("Settings")
public class HiveScratchdirCleaner {

  @EJB
  private Settings settings;
  @EJB
  private InodeController inodeController;
  @EJB
  private DistributedFsService dfs;
  @EJB
  private YarnClientService yarnService;
  @EJB
  private YarnApplicationstateFacade yarnApplicationstateFacade;
  @Resource
  private TimerService timerService;

  private static final Logger logger = Logger.getLogger(HiveScratchdirCleaner.class.getName());

  private Set<String> applicationTypeSet = null;
  private EnumSet<YarnApplicationState> applicationStateEnumSet = null;

  @PostConstruct
  private void init() {
    // SparkSQL creates a scratchdir as well
    applicationTypeSet = new HashSet<>(Arrays.asList("TEZ", "SPARK"));

    applicationStateEnumSet = yarnApplicationstateFacade.getRunningStates();

    String intervalRaw = settings.getHiveScratchdirCleanerInterval();
    Long intervalValue = settings.getConfTimeValue(intervalRaw);
    TimeUnit intervalTimeunit = settings.getConfTimeTimeUnit(intervalRaw);
    logger.log(Level.INFO, "Hive scratchdir cleaner is configured to run every " + intervalValue + " " +
        intervalTimeunit.name());

    intervalValue = intervalTimeunit.toMillis(intervalValue);
    timerService.createTimer(intervalValue, intervalValue, "Hive scratchdir cleaner");
  }

  @Timeout
  @TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
  public void doCleanUp() {
    YarnClientWrapper yarnClientWrapper = null;
    DistributedFileSystemOps dfso = null;
    try {
      // This can throw a runtime exception. After 3 exceptions are thrown, the bean is not executed anymore.
      // Wrap everything into the try to avoid it.
      yarnClientWrapper = yarnService.getYarnClientSuper(settings.getConfiguration());
      YarnClient yarnClient = yarnClientWrapper.getYarnClient();

      dfso = dfs.getDfsOps();

      Set<String> queueSet = new HashSet<>();
      List<QueueInfo> queueInfoList = yarnClient.getAllQueues();
      queueInfoList.forEach(queue -> queueSet.add(queue.getQueueName()));

      String scratchDirParent = settings.getHiveScratchdir();

      Long delayValue = settings.getConfTimeValue(settings.getHiveScratchdirDelay());
      TimeUnit delayTimeunit = settings.getConfTimeTimeUnit(settings.getHiveScratchdirDelay());
      Long threshold = System.currentTimeMillis() - delayTimeunit.toMillis(delayValue);

      List<Inode> scratchDirs = inodeController.getChildren(scratchDirParent);

      for (Inode scratchDir : scratchDirs) {
        // Special case for the Hive superuser.
        if (scratchDir.getModificationTime().longValue() < threshold
            && !scratchDir.getInodePK().getName().equals(settings.getHiveSuperUser())){
          /*
           * Directories are in the format `scratchDirParent`/`hiveUser`
           *
           * If the nobody has touched the directory for more than `getHiveScratchdirDelay`
           * it's safe to delete the directory. Double check that there are no application
           * running for the user (same of the inode name)
           */
          Path fullScratchDirPath = new Path(scratchDirParent, scratchDir.getInodePK().getName());

          try {
            /*
             * Check that no applications are running for this user
             */
            String hiveUserStr = scratchDir.getInodePK().getName();

            Set<String> hiveUser = new HashSet<>();
            hiveUser.add(hiveUserStr);

            List<ApplicationReport> appReports = yarnClient.getApplications(queueSet, hiveUser,
                applicationTypeSet, applicationStateEnumSet);

            if (appReports.isEmpty()) {
              // No Hive query/SparkSQL is running for this user. Safe to delete.

              // Delete as superupser as the user might have been removed from the system
              // So we don't have the certificates
              dfso.rm(fullScratchDirPath, true);
            }
          } catch (IOException | YarnException | RuntimeException e) {
            logger.log(Level.SEVERE, "Could not remove Hive scratchdir for user: "
                    + scratchDir.getInodePK().getName(), e);
          }
        }
      }
    } catch (Exception e) {
      logger.log(Level.SEVERE, "Error while starting Hive scratchdir cleaning ", e);
    } finally {
      yarnService.closeYarnClient(yarnClientWrapper);

      if (dfso != null) {
        dfs.closeDfsClient(dfso);
      }
    }
  }
}
