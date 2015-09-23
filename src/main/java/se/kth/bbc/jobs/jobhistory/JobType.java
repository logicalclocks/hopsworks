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

  private final String readable;

  private JobType(String readable) {
    this.readable = readable;
  }

  @Override
  public String toString() {
    return readable;
  }
}
