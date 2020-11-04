/*
 * Copyright (C) 2020, Logical Clocks AB. All rights reserved
 */
package io.hops.hopsworks.cloud.dao.heartbeat.commands;

import com.google.gson.JsonObject;
import com.google.gson.JsonSerializer;

public abstract class CloudCommandSerializer<T extends CloudCommand> implements JsonSerializer<T> {
  protected JsonObject getBaseCloudCommandSerialized(T command) {
    JsonObject jcommand = new JsonObject();
    jcommand.addProperty("id", command.getId());
    jcommand.addProperty("type", command.getType().toString());
    return jcommand;
  }
}
