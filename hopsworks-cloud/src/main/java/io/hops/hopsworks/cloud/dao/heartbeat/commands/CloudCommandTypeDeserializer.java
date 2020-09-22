/*
 * Copyright (C) 2020, Logical Clocks AB. All rights reserved
 */
package io.hops.hopsworks.cloud.dao.heartbeat.commands;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;

import java.lang.reflect.Type;

public class CloudCommandTypeDeserializer implements JsonDeserializer<CloudCommandType> {
  @Override
  public CloudCommandType deserialize(JsonElement jsonElement, Type type,
          JsonDeserializationContext jsonDeserializationContext) throws JsonParseException {
    return CloudCommandType.of(jsonElement.getAsString());
  }
}
