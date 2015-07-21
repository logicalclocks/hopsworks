package se.kth.bbc.jobs.spark;

import javax.json.JsonObjectBuilder;
import javax.xml.bind.annotation.XmlRootElement;
import se.kth.bbc.jobs.jobhistory.JobType;
import se.kth.bbc.jobs.yarn.YarnJobConfiguration;

/**
 * Contains Spark-specific run information for a Spark job, on top of Yarn
 * configuration.
 * <p>
 * @author stig
 */
@XmlRootElement
public class SparkJobConfiguration extends YarnJobConfiguration {

  private String jarPath;
  private String mainClass;
  private String args;

  private int numberOfExecutors = 1;
  private int executorCores = 1;
  private String executorMemory = "1g";

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
  public String toJson(){
    JsonObjectBuilder builder = getTypeLessJsonBuilder();
    builder.add("type",JobType.SPARK.name());
    return builder.build().toString();
  }
  
  @Override
  public JsonObjectBuilder getTypeLessJsonBuilder(){
    JsonObjectBuilder builder = super.getTypeLessJsonBuilder();
    builder.add("jarPath", jarPath);
    builder.add("mainClass", mainClass);
    builder.add("args", args);
    builder.add("numberOfExecutors",numberOfExecutors);
    builder.add("executorCores", executorCores);
    builder.add("executorMemory", executorMemory);
    return builder;
  }

}
