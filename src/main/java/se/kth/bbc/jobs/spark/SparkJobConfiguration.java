package se.kth.bbc.jobs.spark;

import javax.json.JsonObjectBuilder;
import javax.xml.bind.annotation.XmlRootElement;
import se.kth.bbc.jobs.DatabaseJsonObject;
import se.kth.bbc.jobs.jobhistory.JobType;
import se.kth.bbc.jobs.yarn.YarnJobConfiguration;

/**
 * Contains Spark-specific run information for a Spark job, on top of Yarn
 * configuration.
 * <p>
 * @author stig
 */
@XmlRootElement
public class SparkJobConfiguration extends YarnJobConfiguration{

  private String jarPath;
  private String mainClass;
  private String args;

  private int numberOfExecutors = 1;
  private int executorCores = 1;
  private String executorMemory = "1g";
  
  protected static final String KEY_JARPATH = "JARPATH";
  protected static final String KEY_MAINCLASS = "MAINCLASS";
  protected static final String KEY_ARGS = "ARGS";
  protected static final String KEY_NUMEXECS = "NUMEXECS";
  protected static final String KEY_EXECCORES = "EXECCORES";
  protected static final String KEY_EXECMEM = "EXECMEM";

  public String getJarPath() {
    return jarPath;
  }

  /**
   * Set the path to the main executable jar. No default value.
   * <p>
   * @param jarPath
   */
  public void setJarPath(String jarPath) {
    this.jarPath = jarPath;
  }

  public String getMainClass() {
    return mainClass;
  }

  /**
   * Set the name of the main class to be executed. No default value.
   * <p>
   * @param mainClass
   */
  public void setMainClass(String mainClass) {
    this.mainClass = mainClass;
  }

  public String getArgs() {
    return args;
  }

  /**
   * Set the arguments to be passed to the job. No default value.
   * <p>
   * @param args
   */
  public void setArgs(String args) {
    this.args = args;
  }

  public int getNumberOfExecutors() {
    return numberOfExecutors;
  }

  /**
   * Set the number of executors to be requested for this job.
   * <p>
   * @param numberOfExecutors
   */
  public void setNumberOfExecutors(int numberOfExecutors) {
    this.numberOfExecutors = numberOfExecutors;
  }

  public int getExecutorCores() {
    return executorCores;
  }

  /**
   * Set the number of cores to be requested for each executor.
   * <p>
   * @param executorCores
   */
  public void setExecutorCores(int executorCores) {
    this.executorCores = executorCores;
  }

  public String getExecutorMemory() {
    return executorMemory;
  }

  /**
   * Set the memory requested for each executor. The given string should have
   * the form of a number followed by a 'm' or 'g' signifying the metric.
   * <p>
   * @param executorMemory
   */
  public void setExecutorMemory(String executorMemory) {
    this.executorMemory = executorMemory;
  }
  
  @Override
  public DatabaseJsonObject getReducedJsonObject() {
    DatabaseJsonObject obj = super.getReducedJsonObject();
    obj.set(KEY_ARGS, args);
    obj.set(KEY_EXECCORES, ""+executorCores);
    obj.set(KEY_EXECMEM, executorMemory);
    obj.set(KEY_JARPATH, jarPath);
    obj.set(KEY_MAINCLASS, mainClass);
    obj.set(KEY_NUMEXECS, ""+numberOfExecutors);    
    obj.set(KEY_TYPE, JobType.SPARK.name());
    return obj;
  }

  @Override
  public void updateFromJson(DatabaseJsonObject json) throws
          IllegalArgumentException {
    //First: make sure the given object is valid by getting the type and AdamCommandDTO
    JobType type;
    String jsonArgs,jsonExeccors, jsonExecmem, jsonJarpath, jsonMainclass, jsonNumexecs;
    try {
      String jsonType = json.getString(KEY_TYPE);
      type = JobType.valueOf(jsonType);
      if (type != JobType.SPARK) {
        throw new IllegalArgumentException("JobType must be SPARK.");
      }
      jsonArgs = json.getString(KEY_ARGS);
      jsonExeccors = json.getString(KEY_EXECCORES);
      jsonExecmem = json.getString(KEY_EXECMEM);
      jsonJarpath = json.getString(KEY_JARPATH);
      jsonMainclass = json.getString(KEY_MAINCLASS);
      jsonNumexecs = json.getString(KEY_NUMEXECS);      
    } catch (Exception e) {
      throw new IllegalArgumentException(
              "Cannot convert object into SparkJobConfiguration.", e);
    }
    //Second: allow all superclasses to check validity. To do this: make sure that the type will get recognized correctly.
    json.set(KEY_TYPE, JobType.YARN.name());
    super.updateFromJson(json);
    //Third: we're now sure everything is valid: actually update the state
    this.args = jsonArgs;
    this.executorCores = Integer.parseInt(jsonExeccors);
    this.executorMemory = jsonExecmem;
    this.jarPath = jsonJarpath;
    this.mainClass = jsonMainclass;
    this.numberOfExecutors = Integer.parseInt(jsonNumexecs);
  }

}
