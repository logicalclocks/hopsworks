package se.kth.bbc.jobs.spark;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import se.kth.bbc.jobs.yarn.YarnRunner;
import se.kth.bbc.lims.Constants;
import se.kth.bbc.lims.Utils;

/**
 * Builder class for a Spark YarnRunner. Implements the common logic needed
 * for any Spark job to be started and builds a YarnRunner instance.
 * <p>
 * @author stig
 */
public class SparkYarnRunnerBuilder {

  //Necessary parameters
  private final String appJarPath, mainClass;

  //Optional parameters
  private String jobArgs;
  private String jobName = "Untitled Spark Job";
  private Map<String, String> extraFiles = new HashMap<>();

  private int numberOfExecutors = 1;
  private int executorCores = 1;
  private String executorMemory = "512m";
  private String driverMemory = "1024m";
  private Map<String, String> envVars = new HashMap<>();

  public SparkYarnRunnerBuilder(String appJarPath, String mainClass) {
    if (appJarPath == null || appJarPath.isEmpty()) {
      throw new IllegalArgumentException(
              "Path to application jar cannot be empty!");
    }
    if (mainClass == null || mainClass.isEmpty()) {
      throw new IllegalArgumentException(
              "Name of the main class cannot be empty!");
    }
    this.appJarPath = appJarPath;
    this.mainClass = mainClass;
  }

  public YarnRunner getYarnRunner() throws IOException {
    
    //TODO: inlclude driver memory as am memory

    //Create a builder
    YarnRunner.Builder builder = new YarnRunner.Builder(Constants.SPARK_AM_MAIN);

    //Set Spark staging directory
    String stagingPath = File.separator + "user" + File.separator + Utils.
            getYarnUser() + File.separator + Constants.SPARK_STAGING_DIR
            + File.separator + YarnRunner.APPID_PLACEHOLDER;
    builder.localResourcesBasePath(stagingPath);

    //Add Spark jar
    builder.addLocalResource(Constants.SPARK_LOCRSC_SPARK_JAR,
            Constants.DEFAULT_SPARK_JAR_PATH);
    //Add app jar
    builder.addLocalResource(Constants.SPARK_LOCRSC_APP_JAR, appJarPath);

    //Add extra files to local resources, use filename as key
    for (Map.Entry<String, String> k : extraFiles.entrySet()) {
      builder.addLocalResource(k.getKey(), k.getValue());
    }

    //Set Spark specific environment variables
    builder.addToAppMasterEnvironment("SPARK_YARN_MODE", "true");
    builder.addToAppMasterEnvironment("SPARK_YARN_STAGING_DIR", stagingPath);
    builder.addToAppMasterEnvironment("SPARK_USER", Utils.getYarnUser());
    builder.addToAppMasterEnvironment("CLASSPATH",
            "/srv/spark/conf:/srv/spark/lib/spark-assembly-1.2.0-hadoop2.4.0.jar:/srv/spark/lib/datanucleus-core-3.2.10.jar:/srv/spark/lib/datanucleus-api-jdo-3.2.6.jar:/srv/spark/lib/datanucleus-rdbms-3.2.9.jar");
    for (String key : envVars.keySet()) {
      builder.addToAppMasterEnvironment(key, envVars.get(key));
    }

    //Add local resources to spark environment too
    builder.addCommand(new SparkSetEnvironmentCommand());

    //Set up command
    StringBuilder amargs = new StringBuilder("--class ");
    amargs.append(mainClass);
    amargs.append(" --num-executors ").append(numberOfExecutors);
    amargs.append(" --executor-cores ").append(executorCores);
    amargs.append(" --executor-memory ").append(executorMemory);
    if (jobArgs != null && !jobArgs.isEmpty()) {
      amargs.append(" --arg ");
      amargs.append(jobArgs);
    }
    builder.amArgs(amargs.toString());

    //Set app name
    builder.appName(jobName);

    return builder.build();
  }

  public SparkYarnRunnerBuilder setJobName(String jobName) {
    this.jobName = jobName;
    return this;
  }

  public SparkYarnRunnerBuilder setJobArgs(String jobArgs) {
    this.jobArgs = jobArgs;
    return this;
  }

  public SparkYarnRunnerBuilder setExtraFiles(Map<String, String> extraFiles) {
    if (extraFiles == null) {
      throw new IllegalArgumentException("Map of extra files cannot be null.");
    }
    this.extraFiles = extraFiles;
    return this;
  }

  public SparkYarnRunnerBuilder setNumberOfExecutors(int numberOfExecutors) {
    if (numberOfExecutors < 1) {
      throw new IllegalArgumentException(
              "Number of executors cannot be less than 1.");
    }
    this.numberOfExecutors = numberOfExecutors;
    return this;
  }

  public SparkYarnRunnerBuilder setExecutorCores(int executorCores) {
    if (executorCores < 1) {
      throw new IllegalArgumentException(
              "Number of executor cores cannot be less than 1.");
    }
    this.executorCores = executorCores;
    return this;
  }

  public SparkYarnRunnerBuilder setExecutorMemoryMB(int executorMemoryMB) {
    if (executorMemoryMB < 1) {
      throw new IllegalArgumentException(
              "Executor memory bust be greater than zero.");
    }
    this.executorMemory = "" + executorMemoryMB + "m";
    return this;
  }

  public SparkYarnRunnerBuilder setExecutorMemoryGB(float executorMemoryGB) {
    if (executorMemoryGB <= 0) {
      throw new IllegalArgumentException(
              "Executor memory must be greater than zero.");
    }
    int mem = (int) (executorMemoryGB * 1024);
    this.executorMemory = "" + mem + "m";
    return this;
  }

  public SparkYarnRunnerBuilder setDriverMemoryMB(int driverMemoryMB) {
    if (driverMemoryMB < 1) {
      throw new IllegalArgumentException(
              "Driver memory must be greater than zero.");
    }
    this.driverMemory = "" + driverMemory + "m";
    return this;
  }

  public SparkYarnRunnerBuilder setDriverMemoryGB(float driverMemoryGB) {
    if (driverMemoryGB <= 0) {
      throw new IllegalArgumentException(
              "Driver memory must be greater than zero.");
    }
    int mem = (int) (driverMemoryGB * 1024);
    this.driverMemory = "" + mem + "m";
    return this;
  }

  public SparkYarnRunnerBuilder addSystemVariable(String name, String value) {
    envVars.put(name, value);
    return this;
  }

}
