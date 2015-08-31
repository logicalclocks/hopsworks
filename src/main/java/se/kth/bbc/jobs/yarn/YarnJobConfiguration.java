package se.kth.bbc.jobs.yarn;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import javax.xml.bind.annotation.XmlRootElement;
import se.kth.bbc.jobs.MutableJsonObject;
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
  private int amMemory = 256;
  //Number of cores for appMaster
  private int amVCores = 1;
  //List of paths to be added to local resources
  private Map<String, String> localResources = Collections.EMPTY_MAP;

  protected final static String KEY_TYPE = "type";
  protected final static String KEY_QUEUE = "QUEUE";
  protected final static String KEY_AMMEM = "AMMEM";
  protected final static String KEY_AMCORS = "AMCORS";
  protected final static String KEY_RESOURCES = "RESOURCES";

  public YarnJobConfiguration() {
    super();
  }

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

  /**
   * Add a file to the local resources.
   * <p>
   * @param name
   * @param path
   */
  public final void addLocalResource(String name, String path) {
    if (localResources == null) {
      localResources = new HashMap<>();
    }
    localResources.put(name, path);
  }

  /**
   * Set the local resources.
   * <p>
   * @param localResources
   */
  public final void setLocalResources(Map<String, String> localResources) {
    this.localResources = new HashMap(localResources);
  }

  /**
   * Return a view on the current local resources. The Map returned is not
   * backed by this object.
   * <p>
   * @return
   */
  public final Map<String, String> getLocalResources() {
    return new HashMap<>(localResources);
  }

  @Override
  public JobType getType() {
    return JobType.YARN;
  }

  @Override
  public MutableJsonObject getReducedJsonObject() {
    MutableJsonObject obj = super.getReducedJsonObject();
    //First: fields that can be empty or null:
    if (localResources != null && !localResources.isEmpty()) {
      MutableJsonObject resources = new MutableJsonObject();
      for (Entry<String, String> e : localResources.entrySet()) {
        resources.set(e.getKey(), e.getValue());
      }
      obj.set(KEY_RESOURCES, resources);
    }
    //Then: fields that cannot be null or emtpy:
    obj.set(KEY_AMCORS, "" + amVCores);
    obj.set(KEY_AMMEM, "" + amMemory);
    obj.set(KEY_QUEUE, amQueue);
    obj.set(KEY_TYPE, JobType.YARN.name());
    return obj;
  }

  @Override
  public void updateFromJson(MutableJsonObject json) throws
          IllegalArgumentException {
    //First: make sure the given object is valid by getting the type and AdamCommandDTO
    JobType type;
    String jsonCors, jsonMem, jsonQueue;
    Map<String,String> jsonResources = Collections.EMPTY_MAP;
    try {
      String jsonType = json.getString(KEY_TYPE);
      type = JobType.valueOf(jsonType);
      if (type != JobType.YARN) {
        throw new IllegalArgumentException("JobType must be YARN.");
      }
      //First: fields that can be null or empty:
      if(json.containsKey(KEY_RESOURCES)){
        jsonResources = new HashMap<>();
        MutableJsonObject resources = json.getJsonObject(KEY_RESOURCES);
        for(String key:resources.keySet()){
          jsonResources.put(key, resources.getString(key));
        }
      }
      //Then: fields that cannot be null or empty
      jsonCors = json.getString(KEY_AMCORS);
      jsonMem = json.getString(KEY_AMMEM);
      jsonQueue = json.getString(KEY_QUEUE);
    } catch (Exception e) {
      throw new IllegalArgumentException(
              "Cannot convert object into YarnJobConfiguration.", e);
    }
    super.updateFromJson(json);
    //Second: we're now sure everything is valid: actually update the state
    this.localResources = jsonResources;
    this.amMemory = Integer.parseInt(jsonMem);
    this.amQueue = jsonQueue;
    this.amVCores = Integer.parseInt(jsonCors);
  }

}
