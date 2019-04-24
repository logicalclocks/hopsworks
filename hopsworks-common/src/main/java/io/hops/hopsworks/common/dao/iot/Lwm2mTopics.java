package io.hops.hopsworks.common.dao.iot;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public enum Lwm2mTopics {
  
  TEMPERATURE("topic-lwm2m-3303-temperature"),
  PRESENCE("topic-lwm2m-3302-presence");
  
  private final String name;
  
  @Override
  public String toString() {
    return name;
  }
  
  Lwm2mTopics(String name) {
    this.name = name;
  }
  
  public static List<String> getNamesAsList() {
    return Arrays
      .stream(Lwm2mTopics.values())
      .map(Lwm2mTopics::toString)
      .collect(Collectors.toList());
  }
}
