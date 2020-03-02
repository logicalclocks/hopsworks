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
package io.hops.hopsworks.common.jobs.flink;

import io.hops.hopsworks.common.dao.kagent.HostServicesFacade;
import io.hops.hopsworks.common.hdfs.DistributedFileSystemOps;
import io.hops.hopsworks.common.hdfs.DistributedFsService;
import io.hops.hopsworks.common.hdfs.inode.InodeController;
import io.hops.hopsworks.persistence.entity.hdfs.inode.Inode;
import io.hops.hopsworks.persistence.entity.kagent.HostServices;
import org.apache.hadoop.fs.Path;

import javax.ejb.EJB;
import javax.ejb.Schedule;
import javax.ejb.Singleton;
import javax.ejb.Timer;
import java.io.File;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Timer that removes jobs from the Flink history server from deleted projects or jobs owned by removed project users.
 */
@Singleton
public class FlinkCleaner {
  
  private final static Logger LOGGER = Logger.getLogger(FlinkCleaner.class.getName());
  
  @EJB
  private FlinkController flinkController;
  @EJB
  private InodeController inodeController;
  @EJB
  private DistributedFsService dfs;
  @EJB
  private HostServicesFacade hostServicesFacade;

  @Schedule(persistent = false,
    minute = "0",
    hour = "1")
  public void deleteOrphanJobs(Timer timer) {
    LOGGER.log(Level.INFO, "Running FlinkCleaner.");
    //Get all jobs from history server
    DistributedFileSystemOps dfso = null;
    try {
      List<HostServices> hosts = hostServicesFacade.findServices("flinkhistoryserver");
      if(hosts.isEmpty()) {
        LOGGER.log(Level.INFO, "Could not find flinkhistoryserver service running on any server, " +
            "shutting down timer.");
        timer.cancel();
      }

      //Read all completed jobs from "historyserver.archive.fs.dir"
      String archiveDir = flinkController.getArchiveDir();
      //Delete all without hdfs user
      dfso = dfs.getDfsOps();
      List<Inode> jobs = inodeController.getChildren(archiveDir);
      for (Inode job : jobs) {
        if (job.getHdfsUser() == null) {
          dfso.rm(new Path(archiveDir + File.separator + job.getInodePK().getName()), false);
        }
      }
    } catch (Exception ex) {
      LOGGER.log(Level.SEVERE, "Could not access flink configuration file", ex);
    } finally {
      if (dfso != null) {
        dfs.closeDfsClient(dfso);
      }
    }
  }
}
