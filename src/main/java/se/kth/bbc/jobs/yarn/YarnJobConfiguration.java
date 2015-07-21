package se.kth.bbc.jobs.yarn;

import javax.json.Json;
import javax.json.JsonObjectBuilder;
import javax.xml.bind.annotation.XmlRootElement;
import se.kth.bbc.jobs.jobhistory.JobType;

/**
 * Contains user-setable configuration parameters for a Yarn job.
 * <p>
 * @author stig
 */
@XmlRootElement
public class YarnJobConfiguration {

  private String amQueue = "default";
  // Memory for App master (in MB)
  private int amMemory = 1024;
  //Number of cores for appMaster
  private int amVCores = 1;
  // Application name
  private String appName = "";

  public final String getAmQueue() {
    return amQueue;
  }

  /**
   * Set the queue to which the application should be submitted to the
   * ResourceManager. Default value: "".
   * <p>
   * @param amQueue
   */
  public final void setAmQueue(String amQueue) {
    this.amQueue = amQueue;
  }

  public final int getAmMemory() {
    return amMemory;
  }

  /**
   * Set the amount of memory in MB to be allocated for the Application Master
   * container. Default value: 1024.
   * <p>
   * @param amMemory
   */
  public final void setAmMemory(int amMemory) {
    this.amMemory = amMemory;
  }

  public final int getAmVCores() {
    return amVCores;
  }

  /**
   * Set the number of virtual cores to be allocated for the Application Master
   * container. Default value: 1.
   * <p>
   * @param amVCores
   */
  public final void setAmVCores(int amVCores) {
    this.amVCores = amVCores;
  }

  public final String getAppName() {
    return appName;
  }

  /**
   * Set the name of the application. Default value: "Hops job".
   * <p>
   * @param appName
   */
  public final void setAppName(String appName) {
    this.appName = appName;
  }

  /**
   * As found in Effective Java, the equals contract cannot be satisfied for
   * inheritance hierarchies. Hence, these objects are not meant to be comared.
   * <p>
   * @param o
   * @return
   */
  @Override
  public final boolean equals(Object o) {
    throw new UnsupportedOperationException(
            "YarnJobConfiguration objects should not be compared.");
  }

  /**
   * Convert this instance into a JSON object.
   * <p>
   * @return
   */
  public String toJson() {
    JsonObjectBuilder builder = getTypeLessJsonBuilder();
    builder.add("type", JobType.YARN.name());
    return builder.build().toString();
  }

  /**
   * Return a JsonObjectBuilder object that incorporates all the basic fields of
   * this class.
   * <p>
   * @return
   */
  protected JsonObjectBuilder getTypeLessJsonBuilder() {
    JsonObjectBuilder builder = Json.createObjectBuilder();
    builder.add("amQueue", amQueue);
    builder.add("amMemory", amMemory);
    builder.add("amVCores", amVCores);
    builder.add("appName", appName);
    return builder;
  }

}
