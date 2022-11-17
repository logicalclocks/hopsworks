/*
 * Copyright (C) 2020, Logical Clocks AB. All rights reserved
 */
package io.hops.hopsworks.cloud.dao.heartbeat.commands;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;

import java.lang.reflect.Type;

public class CloudCommandsDeserializer implements JsonDeserializer<CloudCommand> {
  @Override
  public CloudCommand deserialize(JsonElement jsonElement, Type type,
          JsonDeserializationContext jsonDeserializationContext) throws JsonParseException {
    String cmdType  = jsonElement.getAsJsonObject().get("type").getAsString();
    if (CloudCommandType.of(cmdType).equals(CloudCommandType.REMOVE_NODES)) {
      return jsonDeserializationContext.deserialize(jsonElement, RemoveNodesCommand.class);
    } else if(CloudCommandType.of(cmdType).equals(CloudCommandType.DECOMMISSION_NODE)){
      return jsonDeserializationContext.deserialize(jsonElement, DecommissionNodeCommand.class);
    } else if (CloudCommandType.of(cmdType).equals(CloudCommandType.BACKUP)) {
      return jsonDeserializationContext.deserialize(jsonElement, BackupCommand.class);
    } else if (CloudCommandType.of(cmdType).equals(CloudCommandType.RESTORE)) {
      return jsonDeserializationContext.deserialize(jsonElement, RestoreCommand.class);
    } else if (CloudCommandType.of(cmdType).equals(CloudCommandType.BACKUP_DONE)) {
      return jsonDeserializationContext.deserialize(jsonElement, BackupDoneCommand.class);
    } else if (CloudCommandType.of(cmdType).equals(CloudCommandType.DELETE_BACKUP)) {
      return jsonDeserializationContext.deserialize(jsonElement, DeleteBackupCommand.class);
    }
    throw new IllegalArgumentException("We don't know how to deserialize Cloud Command of type: " + cmdType);
  }
}
