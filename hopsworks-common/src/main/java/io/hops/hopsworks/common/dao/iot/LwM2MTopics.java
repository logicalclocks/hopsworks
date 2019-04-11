package io.hops.hopsworks.common.dao.iot;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public enum LwM2MTopics {
  
  TEMPERATURE("topic-lwm2m-3303-temperature");
  
  private final String name;
  
  @Override
  public String toString() {
    return name;
  }
  
  LwM2MTopics(String name) {
    this.name = name;
  }
  
  public static List<String> getNamesAsList() {
    return Arrays
      .stream(LwM2MTopics.values())
      .map(LwM2MTopics::toString)
      .collect(Collectors.toList());
  }
}
