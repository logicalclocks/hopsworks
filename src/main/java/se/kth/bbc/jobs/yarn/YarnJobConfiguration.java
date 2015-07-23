package se.kth.bbc.jobs.yarn;

import com.google.common.base.Strings;
import javax.xml.bind.annotation.XmlRootElement;
import se.kth.bbc.jobs.DatabaseJsonObject;
import se.kth.bbc.jobs.jobhistory.JobType;
import se.kth.bbc.jobs.model.configuration.JobConfiguration;

/**
 * Contains user-setable configuration parameters for a Yarn job.
 * <p>
 * @author stig
 */
@XmlRootElement
public class YarnJobConfiguration extends JobConfiguration {

  private String amQueue = "default";
  // Memory for App master (in MB)
  private int amMemory = 1024;
  //Number of cores for appMaster
  private int amVCores = 1;
  // Application name
  private String appName = "";

  protected final static String KEY_TYPE = "type";
  protected final static String KEY_QUEUE = "QUEUE";
  protected final static String KEY_AMMEM = "AMMEM";
  protected final static String KEY_AMCORS = "AMCORS";
  protected final static String KEY_APPNAME = "APPNAME";

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

  @Override
  public DatabaseJsonObject getReducedJsonObject() {
    DatabaseJsonObject obj = new DatabaseJsonObject();
    //First: fields that can be null or empty:
    if (!Strings.isNullOrEmpty(appName)) {
      obj.set(KEY_APPNAME, appName);
    }
    //Then: fields that cannot be null or emtpy:
    obj.set(KEY_AMCORS, "" + amVCores);
    obj.set(KEY_AMMEM, "" + amMemory);
    obj.set(KEY_QUEUE, amQueue);
    obj.set(KEY_TYPE, JobType.YARN.name());
    return obj;
  }

  @Override
  public void updateFromJson(DatabaseJsonObject json) throws
          IllegalArgumentException {
    //First: make sure the given object is valid by getting the type and AdamCommandDTO
    JobType type;
    String jsonCors, jsonMem, jsonName, jsonQueue;
    try {
      String jsonType = json.getString(KEY_TYPE);
      type = JobType.valueOf(jsonType);
      if (type != JobType.YARN) {
        throw new IllegalArgumentException("JobType must be YARN.");
      }
      //First: fields that can be null or empty:
      jsonName = json.getString(KEY_APPNAME, null);
      //Then: fields that cannot be null or empty
      jsonCors = json.getString(KEY_AMCORS);
      jsonMem = json.getString(KEY_AMMEM);
      jsonQueue = json.getString(KEY_QUEUE);
    } catch (Exception e) {
      throw new IllegalArgumentException(
              "Cannot convert object into YarnJobConfiguration.", e);
    }
    //Second: we're now sure everything is valid: actually update the state
    this.amMemory = Integer.parseInt(jsonMem);
    this.amQueue = jsonQueue;
    this.amVCores = Integer.parseInt(jsonCors);
    this.appName = jsonName;
  }

}
