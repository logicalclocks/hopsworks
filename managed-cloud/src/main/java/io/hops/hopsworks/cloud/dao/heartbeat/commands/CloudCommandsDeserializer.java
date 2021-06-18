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
    }
    throw new IllegalArgumentException("We don't know how to deserialize Cloud Command of type: " + cmdType);
  }
}
