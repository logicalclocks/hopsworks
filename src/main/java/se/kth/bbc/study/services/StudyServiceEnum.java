package se.kth.bbc.study.services;

//TODO: change db def, no enum there, just string and map via @Enumerated

/**
 *
 * @author stig
 */
public enum StudyServiceEnum {
  CUNEIFORM,
  FLINK,
  SAMPLES,
  STUDY_INFO,
  SPARK,
  ADAM,
  MAPREDUCE,
  YARN;

  
  @Override
  public String toString(){
    switch(this){
      case CUNEIFORM:
        return "Cuneiform";
      case FLINK:
        return "Flink";
      case SAMPLES:
        return "Samples";
      case STUDY_INFO:
        return "Study info";
      case SPARK:
        return "Spark";
      case ADAM:
        return "Adam";
      case MAPREDUCE:
        return "MapReduce";
      case YARN:
        return "Yarn";
      default:
        throw new IllegalStateException("Illegal enum value.");
    }
  }  
  
}
