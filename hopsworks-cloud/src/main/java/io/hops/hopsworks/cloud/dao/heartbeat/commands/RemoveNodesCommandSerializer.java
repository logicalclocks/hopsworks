/*
 * Copyright (C) 2020, Logical Clocks AB. All rights reserved
 */
package io.hops.hopsworks.cloud.dao.heartbeat.commands;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonSerializationContext;

import java.lang.reflect.Type;
import java.util.Map;

public class RemoveNodesCommandSerializer extends CloudCommandSerializer<RemoveNodesCommand> {

  @Override
  public JsonElement serialize(RemoveNodesCommand removeNodesCommand, Type type,
          JsonSerializationContext jsonSerializationContext) {
    JsonObject jcommand = getBaseCloudCommandSerialized(removeNodesCommand);
    JsonObject jnodesToRemove = new JsonObject();
    for (Map.Entry<String, Integer> nodeToRemove : removeNodesCommand.getNodesToRemove().entrySet()) {
      jnodesToRemove.addProperty(nodeToRemove.getKey(), nodeToRemove.getValue());
    }
    jcommand.add("nodesToRemove", jnodesToRemove);
    return jcommand;
  }
}
