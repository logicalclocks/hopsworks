package se.kth.bbc.jobs.yarn;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.jar.JarFile;
import java.util.jar.Manifest;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FileStatus;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.FileUtil;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.yarn.api.ApplicationConstants;
import org.apache.hadoop.yarn.api.protocolrecords.GetNewApplicationResponse;
import org.apache.hadoop.yarn.api.records.ApplicationId;
import org.apache.hadoop.yarn.api.records.ApplicationSubmissionContext;
import org.apache.hadoop.yarn.api.records.ContainerLaunchContext;
import org.apache.hadoop.yarn.api.records.LocalResource;
import org.apache.hadoop.yarn.api.records.LocalResourceType;
import org.apache.hadoop.yarn.api.records.LocalResourceVisibility;
import org.apache.hadoop.yarn.api.records.Resource;
import org.apache.hadoop.yarn.client.api.YarnClient;
import org.apache.hadoop.yarn.client.api.YarnClientApplication;
import org.apache.hadoop.yarn.conf.YarnConfiguration;
import org.apache.hadoop.yarn.exceptions.YarnException;
import org.apache.hadoop.yarn.util.ConverterUtils;
import se.kth.bbc.lims.Constants;
import se.kth.bbc.lims.Utils;

/**
 *
 * @author stig
 */
public class YarnRunner {

  private static final Logger logger = Logger.getLogger(YarnRunner.class.
          getName());
  public static final String APPID_PLACEHOLDER = "$APPID";
  private static final String APPID_REGEX = "\\$APPID";
  private static final String KEY_CLASSPATH = "CLASSPATH";

  private YarnClient yarnClient;
  private Configuration conf;
  private ApplicationId appId = null;

  private final String amJarLocalName;
  private final String amJarPath;
  private final String amQueue;
  private int amMemory;
  private int amVCores;
  private String appName;
  private final String amMainClass;
  private String amArgs;
  private final Map<String, String> amLocalResourcesToCopy;
  private final Map<String, String> amLocalResourcesOnHDFS;
  private final Map<String, String> amEnvironment;
  private String localResourcesBasePath;
  private String stdOutPath;
  private String stdErrPath;
  private final boolean shouldCopyAmJarToLocalResources;
  private final List<String> filesToBeCopied;
  private final boolean logPathsAreHdfs;
  private final List<YarnSetupCommand> commands;
  private final List<String> javaOptions;
  private final List<String> filesToRemove;

  private boolean readyToSubmit = false;
  private ApplicationSubmissionContext appContext;

  //---------------------------------------------------------------------------
  //-------------- CORE METHOD: START APPLICATION MASTER ----------------------
  //---------------------------------------------------------------------------
  /**
   * Start the Yarn Application Master.
   * <p>
   * @return The received ApplicationId identifying the application.
   * @throws YarnException
   * @throws IOException Can occur upon opening and moving execution and input
   * files.
   */
  public YarnMonitor startAppMaster() throws YarnException, IOException {
    logger.info("Starting application master.");

    //Get application id
    yarnClient.start();
    YarnClientApplication app = yarnClient.createApplication();
    GetNewApplicationResponse appResponse = app.getNewApplicationResponse();
    appId = appResponse.getApplicationId();
    //And replace all occurences of $APPID with the real id.
    fillInAppid(appId.toString());

    //Check resource requests and availabilities
    checkAmResourceRequest(appResponse);

    //Set application name and type
    appContext = app.getApplicationSubmissionContext();
    appContext.setApplicationName(appName);
    appContext.setApplicationType("Hops Yarn");

    //Add local resources to AM container
    Map<String, LocalResource> localResources = addAllToLocalResources();

    //Copy files to HDFS that are expected to be there
    copyAllToHDFS();

    //Set up environment
    Map<String, String> env = new HashMap<>();
    env.putAll(amEnvironment);
    setUpClassPath(env);

    //Set up commands
    List<String> amCommands = setUpCommands();

    //TODO: set up security tokens
    //Set up container launch context
    ContainerLaunchContext amContainer = ContainerLaunchContext.newInstance(
            localResources, env, amCommands, null, null, null);

    //Finally set up context
    appContext.setAMContainerSpec(amContainer); //container spec
    appContext.setResource(Resource.newInstance(amMemory, amVCores)); //resources
    appContext.setQueue(amQueue); //Queue

    // Signify that ready to submit
    readyToSubmit = true;

    //Run any remaining commands
    for (YarnSetupCommand c : commands) {
      c.execute(this);
    }

    //And submit
    logger.
            log(Level.INFO,
                    "Submitting application {0} to applications manager.", appId);
    yarnClient.submitApplication(appContext);

    yarnClient.close();

    // Create a new client for monitoring
    YarnClient newClient = YarnClient.createYarnClient();
    newClient.init(conf);
    YarnMonitor monitor = new YarnMonitor(appId, newClient);

    //Clean up some
    removeAllNecessary();
    yarnClient = null;
    conf = null;
    appId = null;
    appContext = null;

    return monitor;
  }

  //---------------------------------------------------------------------------
  //--------------------------- CALLBACK METHODS ------------------------------
  //---------------------------------------------------------------------------
  /**
   * Get the ApplicationSubmissionContext used to submit the app. This method
   * should only be called from registered Commands. Invoking it before the
   * ApplicationSubmissionContext is properly set up will result in an
   * IllegalStateException.
   * <p>
   * @return
   */
  public ApplicationSubmissionContext getAppContext() {
    if (!readyToSubmit) {
      throw new IllegalStateException(
              "ApplicationSubmissionContext cannot be requested before it is set up.");
    }
    return appContext;
  }

  //---------------------------------------------------------------------------
  //------------------------- UTILITY METHODS ---------------------------------
  //---------------------------------------------------------------------------
  private void fillInAppid(String id) {
    localResourcesBasePath = localResourcesBasePath.replaceAll(APPID_REGEX, id);
    appName = appName.replaceAll(APPID_REGEX, id);
    amArgs = amArgs.replaceAll(APPID_REGEX, id);
    stdOutPath = stdOutPath.replaceAll(APPID_REGEX, id);
    stdErrPath = stdErrPath.replaceAll(APPID_REGEX, id);
    for (Entry<String, String> entry : amLocalResourcesToCopy.entrySet()) {
      entry.setValue(entry.getValue().replaceAll(APPID_REGEX, id));
    }
    //TODO: thread-safety?
    for (Entry<String, String> entry : amEnvironment.entrySet()) {
      entry.setValue(entry.getValue().replaceAll(APPID_REGEX, id));
    }
  }

  private void checkAmResourceRequest(GetNewApplicationResponse appResponse) {
    int maxMem = appResponse.getMaximumResourceCapability().getMemory();
    if (amMemory > maxMem) {
      logger.log(Level.WARNING,
              "AM memory specified above max threshold of cluster. Using max value. Specified: {0}, max: {1}",
              new Object[]{amMemory,
                maxMem});
      amMemory = maxMem;
    }
    int maxVcores = appResponse.getMaximumResourceCapability().getVirtualCores();
    if (amVCores > maxVcores) {
      logger.log(Level.WARNING,
              "AM vcores specified above max threshold of cluster. Using max value. Specified: {0}, max: {1}",
              new Object[]{amVCores,
                maxVcores});
      amVCores = maxVcores;
    }
  }

  private Map<String, LocalResource> addAllToLocalResources() throws IOException {
    Map<String, LocalResource> localResources = new HashMap<>();
    //If an AM jar has been specified: include that one
    if (shouldCopyAmJarToLocalResources && amJarLocalName != null
            && !amJarLocalName.isEmpty() && amJarPath != null
            && !amJarPath.isEmpty()) {
      if (amJarPath.startsWith("hdfs")) {
        amLocalResourcesOnHDFS.put(amJarLocalName, amJarPath);
      } else {
        amLocalResourcesToCopy.put(amJarLocalName, amJarPath);
      }
    }
    //Get filesystem
    FileSystem fs = FileSystem.get(conf);
    //Construct basepath
    String hdfsPrefix = conf.get("fs.defaultFS");
    String basePath = hdfsPrefix + localResourcesBasePath;
    logger.log(Level.FINER, "Base path: {0}", basePath);
    //For all local resources with local path: copy and add local resource
    for (Entry<String, String> entry : amLocalResourcesToCopy.entrySet()) {
      String key = entry.getKey();
      String source = entry.getValue();
      String filename = Utils.getFileName(source);
      Path dst = new Path(basePath + File.separator + filename);
      fs.copyFromLocalFile(new Path(source), dst);
      logger.log(Level.INFO, "Copying from: {0} to: {1}",
              new Object[]{source,
                dst});
      FileStatus scFileStat = fs.getFileStatus(dst);
      LocalResource scRsrc = LocalResource.newInstance(ConverterUtils.
              getYarnUrlFromPath(dst),
              LocalResourceType.FILE, LocalResourceVisibility.PUBLIC,
              scFileStat.getLen(),
              scFileStat.getModificationTime());
      localResources.put(key, scRsrc);
    }
    //For all local resources with hdfs path: add local resource
    for (Entry<String, String> entry : amLocalResourcesOnHDFS.entrySet()) {
      String key = entry.getKey();
      Path src = new Path(entry.getValue());
      FileStatus scFileStat = fs.getFileStatus(src);
      LocalResource scRsrc = LocalResource.newInstance(ConverterUtils.
              getYarnUrlFromPath(src),
              LocalResourceType.FILE, LocalResourceVisibility.PUBLIC,
              scFileStat.getLen(),
              scFileStat.getModificationTime());
      localResources.put(key, scRsrc);
    }
    return localResources;
  }

  private void copyAllToHDFS() throws IOException {
    FileSystem fs = FileSystem.get(conf);
    String hdfsPrefix = conf.get("fs.defaultFS");
    String basePath = hdfsPrefix + localResourcesBasePath;
    for (String path : filesToBeCopied) {
      String destination = basePath + File.separator + Utils.getFileName(path);
      Path dst = new Path(destination);
      //copy the input file to where cuneiform expects it
      if (!path.startsWith("hdfs:")) {
        //First, remove any checksum files that are present
        //Since the file may have been downloaded from HDFS, modified and now trying to upload,
        //may run into bug HADOOP-7199 (https://issues.apache.org/jira/browse/HADOOP-7199)
        String dirPart = Utils.getDirectoryPart(path);
        String filename = Utils.getFileName(path);
        String crcName = dirPart + "." + filename + ".crc";
        Files.deleteIfExists(Paths.get(crcName));
        fs.copyFromLocalFile(new Path(path), dst);
      } else {
        Path srcPath = new Path(path);
        Path[] srcs = FileUtil.stat2Paths(fs.globStatus(srcPath), srcPath);
        if (srcs.length > 1 && !fs.isDirectory(dst)) {
          throw new IOException("When copying multiple files, "
                  + "destination should be a directory.");
        }
        for (Path src1 : srcs) {
          FileUtil.copy(fs, src1, fs, dst, false, conf);
        }
      }
      logger.log(Level.INFO, "Copying from: {0} to: {1}",
              new Object[]{path,
                dst});
    }
  }

  private void setUpClassPath(Map<String, String> env) {
    // Add AppMaster.jar location to classpath
    StringBuilder classPathEnv = new StringBuilder().append("./*");
    for (String c : conf.getStrings(
            YarnConfiguration.YARN_APPLICATION_CLASSPATH,
            YarnConfiguration.DEFAULT_YARN_APPLICATION_CLASSPATH)) {
      classPathEnv.append(":").append(c.trim());
    }
    classPathEnv.append(":").append("./log4j.properties");
    // add the runtime classpath needed for tests to work
    if (conf.getBoolean(YarnConfiguration.IS_MINI_YARN_CLUSTER, false)) {
      classPathEnv.append(':');
      classPathEnv.append(System.getProperty("java.class.path"));
    }
    //Check whether a classpath variable was already set, and if so: merge them
    //TODO: clean this up so no doubles are found in the classpath.
    if (env.containsKey(KEY_CLASSPATH)) {
      String clpth = env.get(KEY_CLASSPATH) + ":" + classPathEnv.toString();
      env.put(KEY_CLASSPATH, clpth);
    } else {
      env.put(KEY_CLASSPATH, classPathEnv.toString());
    }
    //Put some environment vars in env
    env.
            put(Constants.HADOOP_COMMON_HOME_KEY,
                    Constants.HADOOP_COMMON_HOME_VALUE);
    env.put(Constants.HADOOP_CONF_DIR_KEY, Constants.HADOOP_CONF_DIR_VALUE);
    env.put(Constants.HADOOP_HDFS_HOME_KEY, Constants.HADOOP_HDFS_HOME_VALUE);
    env.put(Constants.HADOOP_YARN_HOME_KEY, Constants.HADOOP_YARN_HOME_VALUE);
  }

  private List<String> setUpCommands() {
    // Set the necessary command to execute the application master
    List<CharSequence> vargs = new ArrayList<>();
    // Set java executable command
    logger.info("Setting up app master command");
    vargs.add(ApplicationConstants.Environment.JAVA_HOME.$() + "/bin/java");
    // Set Xmx based on am memory size
    vargs.add("-Xmx" + amMemory + "M");
    //Add jvm options
    for (String s : javaOptions) {
      vargs.add(s);
    }
    // Set class name
    vargs.add(amMainClass);
    // Set params for Application Master
    vargs.add(amArgs);

    vargs.add("1> ");
    vargs.add(stdOutPath);

    vargs.add("2> ");
    vargs.add(stdErrPath);

    // Get final commmand
    StringBuilder amcommand = new StringBuilder();
    for (CharSequence str : vargs) {
      amcommand.append(str).append(" ");
    }
    logger.log(Level.INFO, "Completed setting up app master command: {0}",
            amcommand.toString());
    List<String> amCommands = new ArrayList<>();
    amCommands.add(amcommand.toString());
    return amCommands;
  }

  private void removeAllNecessary() throws IOException {
    FileSystem fs = FileSystem.get(conf);
    for (String s : filesToRemove) {
      if (s.startsWith("hdfs:")) {
        fs.delete(new Path(s), true);
      } else {
        Files.deleteIfExists(Paths.get(s));
      }
    }
  }

  //---------------------------------------------------------------------------        
  //------------------------- CONSTRUCTOR -------------------------------------
  //---------------------------------------------------------------------------
  private YarnRunner(Builder builder) {
    this.amJarLocalName = builder.amJarLocalName;
    this.amJarPath = builder.amJarPath;
    this.amQueue = builder.amQueue;
    this.amMemory = builder.amMemory;
    this.amVCores = builder.amVCores;
    this.appName = builder.appName;
    this.amMainClass = builder.amMainClass;
    this.amArgs = builder.amArgs;
    this.amLocalResourcesToCopy = builder.amLocalResourcesToCopy;
    this.amLocalResourcesOnHDFS = builder.amLocalResourcesOnHDFS;
    this.amEnvironment = builder.amEnvironment;
    this.localResourcesBasePath = builder.localResourcesBasePath;
    this.yarnClient = builder.yarnClient;
    this.conf = builder.conf;
    this.shouldCopyAmJarToLocalResources
            = builder.shouldAddAmJarToLocalResources;
    this.filesToBeCopied = builder.filesToBeCopied;
    this.logPathsAreHdfs = builder.logPathsAreRelativeToResources;
    this.stdOutPath = builder.stdOutPath;
    this.stdErrPath = builder.stdErrPath;
    this.commands = builder.commands;
    this.javaOptions = builder.javaOptions;
    this.filesToRemove = builder.filesToRemove;
  }

  //---------------------------------------------------------------------------
  //-------------------------- GETTERS ----------------------------------------
  //---------------------------------------------------------------------------
  public String getAmArgs() {
    return amArgs;
  }

  public String getLocalResourcesBasePath() {
    return localResourcesBasePath;
  }

  public String getStdOutPath() {
    if (logPathsAreHdfs) {
      return localResourcesBasePath + File.separator + stdOutPath;
    } else {
      return stdOutPath;
    }
  }

  public String getStdErrPath() {
    if (logPathsAreHdfs) {

      return localResourcesBasePath + File.separator + stdErrPath;
    } else {
      return stdErrPath;
    }
  }

  public boolean areLogPathsHdfs() {
    return logPathsAreHdfs;
  }
  //---------------------------------------------------------------------------
  //-------------------------- BUILDER ----------------------------------------
  //---------------------------------------------------------------------------

  public static final class Builder {

    //Possibly equired attributes
    //The name of the application app master class
    private String amMainClass;
    //Path to the application master jar
    private String amJarPath;
    //The name of the application master jar in the local resources
    private String amJarLocalName;

    //Optional attributes
    // Queue for App master
    private String amQueue = "default"; //TODO: enable changing this, or infer from user data
    // Memory for App master (in MB)
    private int amMemory = 1024;
    //Number of cores for appMaster
    private int amVCores = 1;
    // Application name
    private String appName = "Hops Yarn";
    //Arguments to pass on in invocation of Application master
    private String amArgs;
    //List of paths to resources that should be copied to application master
    private Map<String, String> amLocalResourcesToCopy = new HashMap<>();
    //List of paths to resources that are already in HDFS, but AM should know about
    private Map<String, String> amLocalResourcesOnHDFS = new HashMap<>();
    //Application master environment
    private Map<String, String> amEnvironment = new HashMap<>();
    //Path where the application master expects its local resources to be (added to fs.getHomeDirectory)
    private String localResourcesBasePath;
    //Path to file where stdout should be written, default in tmp folder
    private String stdOutPath;
    //Path to file where stderr should be written, default in tmp folder
    private String stdErrPath;
    //Signify whether the log paths are relative to the localResourcesBasePath
    private boolean logPathsAreRelativeToResources = false;
    //Signify whether the application master jar should be added to local resources
    private boolean shouldAddAmJarToLocalResources = true;
    //List of files to be copied to localResourcesBasePath
    private List<String> filesToBeCopied = new ArrayList<>();
    //List of commands to execute before submission
    private List<YarnSetupCommand> commands = new ArrayList<>();
    //List of options to add to the JVM invocation
    private List<String> javaOptions = new ArrayList<>();
    //List of files to be removed after starting AM.
    private List<String> filesToRemove = new ArrayList<>();

    //Hadoop Configuration
    private Configuration conf;
    //YarnClient
    private YarnClient yarnClient;

    //Constructors
    public Builder(String amMainClass) {
      this.amMainClass = amMainClass;
    }

    public Builder(String amJarPath, String amJarLocalName) {
      this.amJarPath = amJarPath;
      this.amJarLocalName = amJarLocalName;
    }

    //Setters
    /**
     * Sets the arguments to be passed to the Application Master.
     * <p>
     * @param amArgs
     * @return
     */
    public Builder amArgs(String amArgs) {
      this.amArgs = amArgs;
      return this;
    }

    /**
     * Set the amount of memory allocated to the Application Master (in MB).
     * <p>
     * @param amMem Memory in MB.
     */
    public Builder amMemory(int amMem) {
      this.amMemory = amMem;
      return this;
    }

    /**
     * Set the amount of cores allocated to the Application Master.
     * <p>
     * @param amVCores
     */
    public Builder amVCores(int amVCores) {
      this.amVCores = amVCores;
      return this;
    }

    public Builder appName(String appName) {
      this.appName = appName;
      return this;
    }

    public Builder amMainClass(String amMainClass) {
      this.amMainClass = amMainClass;
      return this;
    }

    public Builder amJar(String amJarPath, String amJarLocalName) {
      this.amJarLocalName = amJarLocalName;
      this.amJarPath = amJarPath;
      return this;
    }

    public Builder addAmJarToLocalResources(boolean value) {
      this.shouldAddAmJarToLocalResources = value;
      return this;
    }

    /**
     * Set a file to be copied over to HDFS. It will be copied to
     * localresourcesBasePath/filename and the original will be removed.
     * Equivalent to addFileToBeCopied(path,true).
     * <p>
     * @param path
     * @return
     */
    public Builder addFilePathToBeCopied(String path) {
      return addFilePathToBeCopied(path, true);
    }

    /**
     * Set a file to be copied over to HDFS. It will be copied to
     * localresourcesBasePath/filename. If removeAfterCopy is true, the file
     * will also be removed after copying.
     * <p>
     * @param path
     * @param removeAfterCopy
     * @return
     */
    public Builder addFilePathToBeCopied(String path, boolean removeAfterCopy) {
      filesToBeCopied.add(path);
      if (removeAfterCopy) {
        filesToRemove.add(path);
      }
      return this;
    }

    /**
     * Sets the path to which to write the Application Master's stdout.
     * <p>
     * @param path
     * @return
     */
    public Builder stdOutPath(String path) {
      this.stdOutPath = path;
      return this;
    }

    /**
     * Sets the path to which to write the Application Master's stderr.
     * <p>
     * @param path
     * @return
     */
    public Builder stdErrPath(String path) {
      this.stdErrPath = path;
      return this;
    }

    public Builder logPathsRelativeToResourcesPath(boolean value) {
      this.logPathsAreRelativeToResources = value;
      return this;
    }

    /**
     * Set the base path for local resources for the application master.
     * This is the path where the AM expects its local resources to be. Use
     * "$APPID" as a replacement for the appId, which will be replaced once
     * it is available.
     * <p>
     * If this method is not invoked, a default path will be used.
     *
     * @param basePath
     * @return
     */
    public Builder localResourcesBasePath(String basePath) {
      while (basePath.endsWith(File.separator)) {
        basePath = basePath.substring(0, basePath.length() - 1);
      }
      //TODO: handle paths like "hdfs://"
      if (!basePath.startsWith("/")) {
        basePath = "/" + basePath;
      }
      this.localResourcesBasePath = basePath;
      return this;
    }

    /**
     * Add a local resource that should be added to the AM container. The
     * name is the key as used in the LocalResources map passed to the
     * container. The source is the local path to the file. The file will be
     * copied into HDFS under the path
     * <i>localResourcesBasePath</i>/<i>filename</i> and the source file will be
     * removed.
     *
     * @param name The name of the local resource, key in the local resource
     * map.
     * @param source The local path to the file.
     * @return
     */
    public Builder addLocalResource(String name, String source) {
      return addLocalResource(name, source, true);
    }

    /**
     * Add a local resource that should be added to the AM container. The
     * name is the key as used in the LocalResources map passed to the
     * container. The source is the local path to the file. The file will be
     * copied into HDFS under the path
     * <i>localResourcesBasePath</i>/<i>filename</i> and if removeAfterCopy is
     * true, the original will be removed after starting the AM.
     * <p>
     * @param name
     * @param source
     * @param removeAfterCopy
     * @return
     */
    public Builder addLocalResource(String name, String source,
            boolean removeAfterCopy) {
      if (source.startsWith("hdfs")) {
        amLocalResourcesOnHDFS.put(name, source);
      } else {
        amLocalResourcesToCopy.put(name, source);
      }
      if (removeAfterCopy) {
        filesToRemove.add(source);
      }
      return this;
    }

    public Builder addToAppMasterEnvironment(String key, String value) {
      amEnvironment.put(key, value);
      return this;
    }

    public Builder addAllToAppMasterEnvironment(Map<String, String> env) {
      amEnvironment.putAll(env);
      return this;
    }

    /**
     * Add a Command that should be executed before submission of the
     * application to the ResourceManager. The commands will be executed in
     * order of addition.
     * <p>
     * @param c
     * @return
     */
    public Builder addCommand(YarnSetupCommand c) {
      commands.add(c);
      return this;
    }

    /**
     * Add a java option that will be added in the invocation of the java
     * command. Should be provided in a form that is accepted by the java
     * command, i.e. including a dash in the beginning etc.
     * <p>
     * @param option
     * @return
     */
    public Builder addJavaOption(String option) {
      javaOptions.add(option);
      return this;
    }

    /**
     * Build the YarnRunner instance
     * <p>
     * @return
     * @throws IllegalStateException Thrown if (a) configuration is not found,
     * (b) invalid main class name
     * @throws IOException Thrown if stdOut and/or stdErr path have not been set
     * and temp files could not be created
     */
    public YarnRunner build() throws IllegalStateException, IOException {
      //Set configuration
      try {
        setConfiguration();
      } catch (IllegalStateException e) {
        throw new IllegalStateException("Failed to load configuration", e);
      }

      //Set YarnClient
      yarnClient = YarnClient.createYarnClient();
      yarnClient.init(conf);

      //Set main class
      if (amMainClass == null) {
        amMainClass = getMainClassNameFromJar();
        if (amMainClass == null) {
          throw new IllegalStateException(
                  "Could not infer main class name from jar and was not specified.");
        }
      }
      //Default localResourcesBasePath
      if (localResourcesBasePath == null) {
        localResourcesBasePath = File.separator + APPID_PLACEHOLDER;
      }
      //Default log locations: tmp files
      if (stdOutPath == null || stdOutPath.isEmpty()) {
        try {
          stdOutPath = Files.createTempFile("stdOut", "").toString();
        } catch (IOException e) {
          throw new IOException("Failed to create tmp log file.", e);
        }
      }
      if (stdErrPath == null || stdErrPath.isEmpty()) {
        try {
          stdErrPath = Files.createTempFile("stdErr", "").toString();
        } catch (IOException e) {
          throw new IOException("Failed to create tmp log file.", e);
        }
      }
      return new YarnRunner(this);
    }

    private void setConfiguration() throws IllegalStateException {
      //Get the path to the Yarn configuration file from environment variables
      String yarnConfDir = System.getenv(Constants.ENV_KEY_YARN_CONF_DIR);
      //If not found in environment variables: warn and use default
      if (yarnConfDir == null) {
        logger.log(Level.WARNING,
                "Environment variable " + Constants.ENV_KEY_YARN_CONF_DIR
                + " not found, using default "
                + Constants.DEFAULT_YARN_CONF_DIR);
        yarnConfDir = Constants.DEFAULT_YARN_CONF_DIR;
      }

      //Get the configuration file at found path
      Path confPath = new Path(yarnConfDir);
      File confFile = new File(confPath + File.separator
              + Constants.DEFAULT_YARN_CONFFILE_NAME);
      if (!confFile.exists()) {
        logger.log(Level.SEVERE,
                "Unable to locate Yarn configuration file in {0}. Aborting exectution.",
                confFile);
        throw new IllegalStateException("No Yarn conf file");
      }

      //Also add the hadoop config
      String hadoopConfDir = System.getenv(Constants.ENV_KEY_HADOOP_CONF_DIR);
      //If not found in environment variables: warn and use default
      if (hadoopConfDir == null) {
        logger.log(Level.WARNING,
                "Environment variable " + Constants.ENV_KEY_HADOOP_CONF_DIR
                + " not found, using default "
                + Constants.DEFAULT_HADOOP_CONF_DIR);
        hadoopConfDir = Constants.DEFAULT_HADOOP_CONF_DIR;
      }
      confPath = new Path(hadoopConfDir);
      File hadoopConf = new File(confPath + File.separator
              + Constants.DEFAULT_HADOOP_CONFFILE_NAME);
      if (!hadoopConf.exists()) {
        logger.log(Level.SEVERE,
                "Unable to locate Hadoop configuration file in {0}. Aborting exectution.",
                hadoopConf);
        throw new IllegalStateException("No Hadoop conf file");
      }

      //And the hdfs config
      File hdfsConf = new File(confPath + File.separator
              + Constants.DEFAULT_HDFS_CONFFILE_NAME);
      if (!hdfsConf.exists()) {
        logger.log(Level.SEVERE,
                "Unable to locate HDFS configuration file in {0}. Aborting exectution.",
                hdfsConf);
        throw new IllegalStateException("No HDFS conf file");
      }

      //Set the Configuration object for the returned YarnClient
      conf = new Configuration();
      conf.addResource(new Path(confFile.getAbsolutePath()));
      conf.addResource(new Path(hadoopConf.getAbsolutePath()));
      conf.addResource(new Path(hdfsConf.getAbsolutePath()));

      addPathToConfig(conf, confFile);
      addPathToConfig(conf, hadoopConf);
      setDefaultConfValues(conf);
    }

    private static void addPathToConfig(Configuration conf, File path) {
      // chain-in a new classloader
      URL fileUrl = null;
      try {
        fileUrl = path.toURL();
      } catch (MalformedURLException e) {
        throw new RuntimeException("Erroneous config file path", e);
      }
      URL[] urls = {fileUrl};
      ClassLoader cl = new URLClassLoader(urls, conf.getClassLoader());
      conf.setClassLoader(cl);
    }

    private static void setDefaultConfValues(Configuration conf) {
      if (conf.get("fs.hdfs.impl", null) == null) {
        conf.set("fs.hdfs.impl", "org.apache.hadoop.hdfs.DistributedFileSystem");
      }
      if (conf.get("fs.file.impl", null) == null) {
        conf.set("fs.file.impl", "org.apache.hadoop.fs.LocalFileSystem");
      }
    }

    private String getMainClassNameFromJar() {
      if (amJarPath == null) {
        throw new IllegalStateException(
                "Main class name and amJar path cannot both be null.");
      }
      String fileName = amJarPath;
      String mainClassName = null;

      try (JarFile jarFile = new JarFile(fileName)) {
        Manifest manifest = jarFile.getManifest();
        if (manifest != null) {
          mainClassName = manifest.getMainAttributes().getValue("Main-Class");
        }
      } catch (IOException io) {
        logger.log(Level.SEVERE, "Could not open jar file " + amJarPath
                + " to load main class.", io);
        return null;
      }

      if (mainClassName != null) {
        return mainClassName.replaceAll("/", ".");
      } else {
        return null;
      }
    }

  }

  //---------------------------------------------------------------------------        
  //---------------------------- TOSTRING -------------------------------------
  //---------------------------------------------------------------------------
  @Override
  public String toString() {
    if (!readyToSubmit) {
      return "YarnRunner: application context not requested yet.";
    } else {
      return "YarnRunner, ApplicationSubmissionContext: " + appContext;
    }
  }
}
