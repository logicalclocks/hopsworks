/*
 * Copyright (C) 2020, Logical Clocks AB. All rights reserved
 */
package io.hops.hopsworks.cloud;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import io.hops.hopsworks.cloud.dao.heartbeat.commands.CloudCommand;
import io.hops.hopsworks.cloud.dao.heartbeat.commands.CloudCommandType;
import io.hops.hopsworks.cloud.dao.heartbeat.commands.CloudCommandsDeserializer;

import java.lang.reflect.Type;

public class TestCloudCommandsDeserializer extends CloudCommandsDeserializer {
  @Override
  public CloudCommand deserialize(JsonElement jsonElement, Type type,
          JsonDeserializationContext jsonDeserializationContext) throws JsonParseException {
    String cmdType = jsonElement.getAsJsonObject().get("type").getAsString();
    if (CloudCommandType.of(cmdType).equals(DummyCommand.DUMMY_COMMAND)) {
      return jsonDeserializationContext.deserialize(jsonElement, DummyCommand.class);
    }
    return super.deserialize(jsonElement, type, jsonDeserializationContext);
  }
}
