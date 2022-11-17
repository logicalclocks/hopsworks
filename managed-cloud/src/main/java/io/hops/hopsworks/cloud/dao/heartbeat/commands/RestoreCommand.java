/*
 * Copyright (C) 2022, Hopsworks AB. All rights reserved
 */
package io.hops.hopsworks.cloud.dao.heartbeat.commands;

public class RestoreCommand extends BackupCommand {


  public RestoreCommand(String id, String backupId) {
    super(id, backupId, CloudCommandType.RESTORE);
  }


}
