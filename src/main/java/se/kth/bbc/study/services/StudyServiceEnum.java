package se.kth.bbc.study.services;

/**
 *
 * @author stig
 */
public enum StudyServiceEnum {
  CUNEIFORM ("Cuneiform"),
  FLINK ("Flink"),
  SAMPLES ("Samples"),
  STUDY_INFO ("Study Info"),
  SPARK ("Spark"),
  ADAM ("Adam"),
  MAPREDUCE("Mapreduce"),
  YARN("Yarn");
  
  private final String service;
  
  private StudyServiceEnum(String service){
    this.service = service;
  }
  
  @Override
  public String toString(){
    return service;
  }
  
  public String getName(){
    return this.name();
  }
  
}
