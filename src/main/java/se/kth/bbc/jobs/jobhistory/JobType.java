package se.kth.bbc.jobs.jobhistory;

/**
 *
 * @author stig
 */
public enum JobType {
  CUNEIFORM, YARN, FLINK, SPARK;
  
  @Override
  public String toString(){
    switch(this){
      case CUNEIFORM:
        return "Cuneiform";
      case YARN:
        return "Yarn";
      case FLINK:
        return "Flink";
      case SPARK:
        return "Spark";
      default:
        throw new IllegalStateException("Illegal enum value.");
    }
  }
}
