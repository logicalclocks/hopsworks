package se.kth.bbc.project.services;

/**
 *
 * @author stig
 */
public enum ProjectServiceEnum {

  CUNEIFORM("Cuneiform"),
  ZEPPELIN("Zeppelin"),
  SPARK("Spark"),
  ADAM("ADAM"),
  MAPREDUCE("MapReduce"),
  YARN("Yarn"),
  PRIVACY("Privacy");
  
  private final String readable;
  private ProjectServiceEnum(String readable){
    this.readable = readable;
  }

  @Override
  public String toString() {
    return readable;
  }

}
