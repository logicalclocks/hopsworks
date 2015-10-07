package se.kth.bbc.jobs.jobhistory;

/**
 *
 * @author stig
 */
public enum JobType {

  CUNEIFORM("Cuneiform"),
  YARN("Yarn"),
  FLINK("Flink"),
  SPARK("Spark"),
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
