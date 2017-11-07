package io.hops.hopsworks.common.dao.project.service;

public enum ProjectServiceEnum {

  ZEPPELIN("Zeppelin"),
  SSH("Ssh"),
  KAFKA("Kafka"),
  WORKFLOWS("Workflows"),
  HISTORY("History"),
  //  BIOBANKING("Biobanking"),
  //  CHARON("Charon"),
  DELA("Dela"),
  JUPYTER("Jupyter"),
  JOBS("Jobs"),
  HIVE("Hive");

  private final String readable;

  private ProjectServiceEnum(String readable) {
    this.readable = readable;
  }

  @Override
  public String toString() {
    return readable;
  }

}
