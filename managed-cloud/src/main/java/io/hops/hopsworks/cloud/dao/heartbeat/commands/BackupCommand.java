/*
 * Copyright (C) 2022, Hopsworks AB. All rights reserved
 */
package io.hops.hopsworks.cloud.dao.heartbeat.commands;

public class BackupCommand extends CloudCommand {

  private String backupId;

  public BackupCommand(String id, String backupId) {
    super(id, CloudCommandType.BACKUP);
    this.backupId = backupId;
  }

  public BackupCommand(String id, String backupId, CloudCommandType type) {
    super(id, type);
    this.backupId = backupId;
  }
  
  public String getBackupId() {
    return backupId;
  }

}
