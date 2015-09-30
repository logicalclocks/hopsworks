package se.kth.bbc.jobs.spark;

import com.google.common.base.Strings;
import javax.xml.bind.annotation.XmlRootElement;
import se.kth.bbc.jobs.MutableJsonObject;
import se.kth.bbc.jobs.jobhistory.JobType;
import se.kth.bbc.jobs.yarn.YarnJobConfiguration;

/**
 * Contains Spark-specific run information for a Spark job, on top of Yarn
 * configuration.
 * <p/>
 * @author stig
 */
@XmlRootElement
public class SparkJobConfiguration extends YarnJobConfiguration {

  private String jarPath;
  private String mainClass;
  private String args;

  private int numberOfExecutors = 1;
  private int executorCores = 1;
  private int executorMemory = 512;

  protected static final String KEY_JARPATH = "JARPATH";
  protected static final String KEY_MAINCLASS = "MAINCLASS";
  protected static final String KEY_ARGS = "ARGS";
  protected static final String KEY_NUMEXECS = "NUMEXECS";
  protected static final String KEY_EXECCORES = "EXECCORES";
  protected static final String KEY_EXECMEM = "EXECMEM";

  public SparkJobConfiguration() {
    super();
  }

  public String getJarPath() {
    return jarPath;
  }

  /**
   * Set the path to the main executable jar. No default value.
   * <p/>
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
   * <p/>
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
   * <p/>
   * @param args
   */
  public void setArgs(String args) {
    this.args = args;
  }

  public int getNumberOfExecutors() {
    return numberOfExecutors;
  }

  /**
   * Set the number of executors to be requested for this job. This should be
   * greater than or equal to 1.
   * <p/>
   * @param numberOfExecutors
   * @throws IllegalArgumentException If the argument is smaller than 1.
   */
  public void setNumberOfExecutors(int numberOfExecutors) throws
          IllegalArgumentException {
    if (numberOfExecutors < 1) {
      throw new IllegalArgumentException(
              "Number of executors has to be greater than or equal to 1.");
    }
    this.numberOfExecutors = numberOfExecutors;
  }

  public int getExecutorCores() {
    return executorCores;
  }

  /**
   * Set the number of cores to be requested for each executor.
   * <p/>
   * @param executorCores
   * @throws IllegalArgumentException If the number of cores is smaller than 1.
   */
  public void setExecutorCores(int executorCores) throws
          IllegalArgumentException {
    if (executorCores < 1) {
      throw new IllegalArgumentException(
              "Number of executor cores has to be greater than or equal to 1.");
    }
    this.executorCores = executorCores;
  }

  public int getExecutorMemory() {
    return executorMemory;
  }

  /**
   * Set the memory requested for each executor in MB.
   * <p/>
   * @param executorMemory
   * @throws IllegalArgumentException If the given value is not strictly positive.
   */
  public void setExecutorMemory(int executorMemory) throws
          IllegalArgumentException {
    if (executorMemory < 1) {
      throw new IllegalArgumentException(
              "Executor memory must be greater than 1MB.");
    }
    this.executorMemory = executorMemory;
  }

  @Override
  public JobType getType() {
    return JobType.SPARK;
  }

  @Override
  public MutableJsonObject getReducedJsonObject() {
    MutableJsonObject obj = super.getReducedJsonObject();
    //First: fields that are possibly null or empty:
    if (!Strings.isNullOrEmpty(args)) {
      obj.set(KEY_ARGS, args);
    }
    if (!Strings.isNullOrEmpty(mainClass)) {
      obj.set(KEY_MAINCLASS, mainClass);
    }
    if (!Strings.isNullOrEmpty(mainClass)) {
      obj.set(KEY_JARPATH, jarPath);
    }
    //Then: fields that can never be null or emtpy.
    obj.set(KEY_EXECCORES, "" + executorCores);
    obj.set(KEY_EXECMEM, ""+executorMemory);
    obj.set(KEY_NUMEXECS, "" + numberOfExecutors);
    obj.set(KEY_TYPE, JobType.SPARK.name());
    return obj;
  }

  @Override
  public void updateFromJson(MutableJsonObject json) throws
          IllegalArgumentException {
    //First: make sure the given object is valid by getting the type and AdamCommandDTO
    JobType type;
    String jsonArgs, jsonJarpath, jsonMainclass, jsonNumexecs;
    int jsonExecmem, jsonExeccors;
    try {
      String jsonType = json.getString(KEY_TYPE);
      type = JobType.valueOf(jsonType);
      if (type != JobType.SPARK) {
        throw new IllegalArgumentException("JobType must be SPARK.");
      }
      //First: fields that can be null or empty
      jsonArgs = json.getString(KEY_ARGS, null);
      jsonJarpath = json.getString(KEY_JARPATH, null);
      jsonMainclass = json.getString(KEY_MAINCLASS, null);
      //Then: fields that cannot be null or emtpy.
      jsonExeccors = Integer.parseInt(json.getString(KEY_EXECCORES));
      jsonExecmem = Integer.parseInt(json.getString(KEY_EXECMEM));
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
    this.executorCores = jsonExeccors;
    this.executorMemory = jsonExecmem;
    this.jarPath = jsonJarpath;
    this.mainClass = jsonMainclass;
    this.numberOfExecutors = Integer.parseInt(jsonNumexecs);
  }

}
