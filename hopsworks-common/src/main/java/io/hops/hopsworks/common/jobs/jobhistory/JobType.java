package io.hops.hopsworks.common.jobs.jobhistory;

public enum JobType {

  YARN("Yarn"),
  FLINK("Flink"),
  SPARK("Spark"),
  PYSPARK("PySpark"),
  TFSPARK("TensorFlowOnSpark"),
  ADAM("ADAM"),
  ERASURE_CODING("ERASURE_CODING");

  private final String name;

  private JobType(String name) {
    this.name = name;
  }

  public String getName() {
    return name;
  }

  @Override
  public String toString() {
    return name;
  }
}
