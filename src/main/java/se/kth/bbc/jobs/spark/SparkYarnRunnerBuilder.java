package se.kth.bbc.jobs.spark;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
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
  private final List<String> jobArgs = new ArrayList<>();
  private String jobName = "Untitled Spark Job";
  private Map<String, String> extraFiles = new HashMap<>();

  private int numberOfExecutors = 1;
  private int executorCores = 1;
  private String executorMemory = "512m";
  private int driverMemory = 1024; // in MB
  private int driverCores = 1;
  private String driverQueue;
  private final Map<String, String> envVars = new HashMap<>();
  private final Map<String, String> sysProps = new HashMap<>();
  private String classPath;

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

  /**
   * Get a YarnRunner instance that will launch a Spark job.
   * <p>
   * @return The YarnRunner instance to launch the Spark job on Yarn.
   * @throws IOException If creation failed.
   */
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
            Constants.DEFAULT_SPARK_JAR_HDFS_PATH, false);
    //Add app jar
    builder.addLocalResource(Constants.SPARK_LOCRSC_APP_JAR, appJarPath,
            !appJarPath.startsWith("hdfs:"));

    //Add extra files to local resources, use filename as key
    for (Map.Entry<String, String> k : extraFiles.entrySet()) {
      builder.addLocalResource(k.getKey(), k.getValue(), !k.getValue().
              startsWith("hdfs:"));
    }

    //Set Spark specific environment variables
    builder.addToAppMasterEnvironment("SPARK_YARN_MODE", "true");
    builder.addToAppMasterEnvironment("SPARK_YARN_STAGING_DIR", stagingPath);
    builder.addToAppMasterEnvironment("SPARK_USER", Utils.getYarnUser());
    if (classPath == null || classPath.isEmpty()) {
      builder.addToAppMasterEnvironment("CLASSPATH",
              Constants.SPARK_DEFAULT_CLASSPATH);
    } else {
      builder.addToAppMasterEnvironment("CLASSPATH", classPath + ":"
              + Constants.SPARK_DEFAULT_CLASSPATH);
    }
    for (String key : envVars.keySet()) {
      builder.addToAppMasterEnvironment(key, envVars.get(key));
    }

    for (String s : sysProps.keySet()) {
      String option = escapeForShell("-D" + s + "=" + sysProps.get(s));
      builder.addJavaOption(option);
    }

    //Add local resources to spark environment too
    builder.addCommand(new SparkSetEnvironmentCommand());

    //Set up command
    StringBuilder amargs = new StringBuilder("--class ");
    amargs.append(mainClass);
    amargs.append(" --num-executors ").append(numberOfExecutors);
    amargs.append(" --executor-cores ").append(executorCores);
    amargs.append(" --executor-memory ").append(executorMemory);
    for (String s : jobArgs) {
      amargs.append(" --arg ").append(s);
    }
    builder.amArgs(amargs.toString());

    //Set up Yarn properties
    builder.amMemory(driverMemory);
    builder.amVCores(driverCores);
    builder.amQueue(driverQueue);

    //Set app name
    builder.appName(jobName);

    return builder.build();
  }

  public SparkYarnRunnerBuilder setJobName(String jobName) {
    this.jobName = jobName;
    return this;
  }

  public SparkYarnRunnerBuilder addAllJobArgs(List<String> jobArgs) {
    this.jobArgs.addAll(jobArgs);
    return this;
  }

  public SparkYarnRunnerBuilder addAllJobArgs(String[] jobArgs) {
    this.jobArgs.addAll(Arrays.asList(jobArgs));
    return this;
  }

  public SparkYarnRunnerBuilder addJobArg(String jobArg) {
    jobArgs.add(jobArg);
    return this;
  }

  public SparkYarnRunnerBuilder setExtraFiles(Map<String, String> extraFiles) {
    if (extraFiles == null) {
      throw new IllegalArgumentException("Map of extra files cannot be null.");
    }
    this.extraFiles = extraFiles;
    return this;
  }

  public SparkYarnRunnerBuilder addExtraFile(String filename, String location) {
    if (filename == null || filename.isEmpty()) {
      throw new IllegalArgumentException(
              "Filename in extra file mapping cannot be null or empty.");
    }
    if (location == null || location.isEmpty()) {
      throw new IllegalArgumentException(
              "Location in extra file mapping cannot be null or empty.");
    }
    this.extraFiles.put(filename, location);
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

  /**
   * Set the memory requested for each executor. The given string should have
   * the form of a number followed by a 'm' or 'g' signifying the metric.
   * <p>
   * @param memory
   * @return
   */
  public SparkYarnRunnerBuilder setExecutorMemory(String memory) {
    memory = memory.toLowerCase();
    if (!memory.endsWith("m") && !memory.endsWith("g")) {
      throw new IllegalArgumentException(
              "Memory string does not follow the necessary format.");
    } else {
      String memnum = memory.substring(0, memory.length() - 1);
      try {
        Integer.parseInt(memnum);
      } catch (NumberFormatException e) {
        throw new IllegalArgumentException(
                "Memory string does not follow the necessary format.", e);
      }
    }
    this.executorMemory = memory;
    return this;
  }

  public SparkYarnRunnerBuilder setDriverMemoryMB(int driverMemoryMB) {
    if (driverMemoryMB < 1) {
      throw new IllegalArgumentException(
              "Driver memory must be greater than zero.");
    }
    this.driverMemory = driverMemoryMB;
    return this;
  }

  public SparkYarnRunnerBuilder setDriverMemoryGB(int driverMemoryGB) {
    if (driverMemoryGB <= 0) {
      throw new IllegalArgumentException(
              "Driver memory must be greater than zero.");
    }
    int mem = driverMemoryGB * 1024;
    this.driverMemory = mem;
    return this;
  }

  public void setDriverCores(int driverCores) {
    this.driverCores = driverCores;
  }

  public void setDriverQueue(String driverQueue) {
    this.driverQueue = driverQueue;
  }

  public SparkYarnRunnerBuilder addEnvironmentVariable(String name, String value) {
    envVars.put(name, value);
    return this;
  }

  public SparkYarnRunnerBuilder addSystemProperty(String name, String value) {
    sysProps.put(name, value);
    return this;
  }

  public SparkYarnRunnerBuilder addToClassPath(String s) {
    if (classPath == null || classPath.isEmpty()) {
      classPath = s;
    } else {
      classPath = classPath + ":" + s;
    }
    return this;
  }

  /**
   * Taken from Apache Spark code: Escapes a string for inclusion in a command
   * line executed by Yarn. Yarn executes commands
   * using `bash -c "command arg1 arg2"` and that means plain quoting doesn't
   * really work. The
   * argument is enclosed in single quotes and some key characters are escaped.
   * <p>
   * @param s A single argument.
   * @return Argument quoted for execution via Yarn's generated shell script.
   */
  private String escapeForShell(String s) {
    if (s != null) {
      StringBuilder escaped = new StringBuilder("'");
      for (int i = 0; i < s.length(); i++) {
        switch (s.charAt(i)) {
          case '$':
            escaped.append("\\$");
            break;
          case '"':
            escaped.append("\\\"");
            break;
          case '\'':
            escaped.append("'\\''");
            break;
          default:
            escaped.append(s.charAt(i));
            break;
        }
      }
      return escaped.append("'").toString();
    } else {
      return s;
    }
  }

}
