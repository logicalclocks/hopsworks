package se.kth.bbc.jobs.jobhistory;

/**
 *
 * @author stig
 */
public enum JobType {
  CUNEIFORM, YARN, FLINK, SPARK,ADAM;
  
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
      case ADAM:
        return "Adam";
      default:
        throw new IllegalStateException("Illegal enum value.");
    }
  }
}
