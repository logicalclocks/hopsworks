/*
 * Copyright (C) 2022, Hopsworks AB. All rights reserved
 */
package io.hops.hopsworks.cloud.dao.heartbeat.commands;

public class BackupDoneCommand extends BackupCommand {


  public BackupDoneCommand(String id, String backupId) {
    super(id, backupId, CloudCommandType.BACKUP_DONE);
  }


}
