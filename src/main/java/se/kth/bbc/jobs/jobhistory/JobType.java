package se.kth.bbc.jobs.jobhistory;

/**
 *
 * @author stig
 */
public enum JobType {
  CUNEIFORM, YARN, FLINK;
  
  @Override
  public String toString(){
    switch(this){
      case CUNEIFORM:
        return "Cuneiform";
      case YARN:
        return "Yarn";
      case FLINK:
        return "Flink";
      default:
        throw new IllegalStateException("Illegal enum value.");
    }
  }
}
