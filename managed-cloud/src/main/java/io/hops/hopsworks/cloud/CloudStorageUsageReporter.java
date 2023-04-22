/*
 * Copyright (C) 2020, Logical Clocks AB. All rights reserved
 */
package io.hops.hopsworks.cloud;

import io.hops.hopsworks.common.hdfs.DistributedFileSystemOps;
import io.hops.hopsworks.common.hdfs.DistributedFsService;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.hdfs.protocol.LastUpdatedContentSummary;

import javax.ejb.EJB;
import javax.ejb.Schedule;
import javax.ejb.Singleton;
import javax.ejb.Startup;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

@Singleton
@Startup
@TransactionAttribute(TransactionAttributeType.NEVER)
public class CloudStorageUsageReporter {
  private static final Logger LOG = Logger.getLogger(CloudStorageUsageReporter.class.getName());

  @EJB
  private CloudClient cloudClient;
  @EJB
  private DistributedFsService dfs;
  
  @Schedule(minute = "*/15", info = "Cloud storage usage reporter")
  @TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
  public void reportStorageUsage() {
    DistributedFileSystemOps dfso = null;
    try {
      dfso = dfs.getDfsOps();
      LastUpdatedContentSummary summary = dfso.getFilesystem().getLastUpdatedContentSummary(new Path("/"));
      cloudClient.sendStorageUsage(summary.getSpaceConsumed(), summary.getFileAndDirCount());
    } catch (IOException ex) {
      LOG.log(Level.SEVERE, "failded to send cloud storage usage report", ex);
    } finally {
      dfs.closeDfsClient(dfso);
    }
  }
}
