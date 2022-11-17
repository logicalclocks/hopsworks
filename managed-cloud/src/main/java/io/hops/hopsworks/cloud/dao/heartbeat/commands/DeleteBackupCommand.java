/*
 * Copyright (C) 2022, Hopsworks AB. All rights reserved
 */
package io.hops.hopsworks.cloud.dao.heartbeat.commands;

public class DeleteBackupCommand extends BackupCommand {


  public DeleteBackupCommand(String id, String backupId) {
    super(id, backupId, CloudCommandType.DELETE_BACKUP);
  }


}
