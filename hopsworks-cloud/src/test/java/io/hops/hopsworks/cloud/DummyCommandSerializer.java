/*
 * Copyright (C) 2020, Logical Clocks AB. All rights reserved
 */
package io.hops.hopsworks.cloud;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonSerializationContext;
import io.hops.hopsworks.cloud.dao.heartbeat.commands.CloudCommandSerializer;

import java.lang.reflect.Type;

public class DummyCommandSerializer extends CloudCommandSerializer<DummyCommand> {
  @Override
  public JsonElement serialize(DummyCommand dummyCommand, Type type,
          JsonSerializationContext jsonSerializationContext) {
    JsonObject jcommand = getBaseCloudCommandSerialized(dummyCommand);
    jcommand.addProperty("arguments", dummyCommand.getArguments());
    return jcommand;
  }
}
