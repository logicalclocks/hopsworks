package se.kth.bbc.jobs.flink;

import org.apache.hadoop.fs.Path;


import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.hadoop.yarn.api.records.LocalResourceType;
import org.apache.hadoop.yarn.api.records.LocalResourceVisibility;
import se.kth.bbc.jobs.jobhistory.JobType;
import se.kth.bbc.jobs.yarn.YarnRunner;
import se.kth.hopsworks.controller.LocalResourceDTO;
import se.kth.hopsworks.util.Settings;

/**
* All classes in this package contain code taken from
* https://github.com/apache/hadoop-common/blob/trunk/hadoop-yarn-project/hadoop-yarn/hadoop-yarn-applications/hadoop-yarn-applications-distributedshell/src/main/java/org/apache/hadoop/yarn/applications/distributedshell/Client.java?source=cc
* and
* https://github.com/hortonworks/simple-yarn-app
* and
* https://github.com/yahoo/storm-yarn/blob/master/src/main/java/com/yahoo/storm/yarn/StormOnYarn.java
*
* The Flink jar is uploaded to HDFS by this client.
* The application master and all the TaskManager containers get the jar file downloaded
* by YARN into their local fs.
*
*/
public class FlinkYarnRunnerBuilder {

     private static final Logger logger = Logger.
      getLogger(FlinkYarnRunnerBuilder.class.getName());
    /**
     * Constants, all starting with ENV_ are used as environment variables to
     * pass values from the Client to the Application Master.
     */
    public final static String ENV_TM_MEMORY = "_CLIENT_TM_MEMORY";
    public final static String ENV_TM_COUNT = "_CLIENT_TM_COUNT";
    public final static String ENV_APP_ID = "_APP_ID";
    public final static String FLINK_JAR_PATH = "_FLINK_JAR_PATH"; // the Flink jar resource location (in HDFS).
    public static final String ENV_CLIENT_HOME_DIR = "_CLIENT_HOME_DIR";
    public static final String ENV_CLIENT_SHIP_FILES = "_CLIENT_SHIP_FILES";
    public static final String ENV_CLIENT_USERNAME = "_CLIENT_USERNAME";
    public static final String ENV_SLOTS = "_SLOTS";
    public static final String ENV_DETACHED = "_DETACHED";
    public static final String ENV_STREAMING_MODE = "_STREAMING_MODE";
    public static final String ENV_DYNAMIC_PROPERTIES = "_DYNAMIC_PROPERTIES";
    public static final String CONFIG_FILE_LOGBACK_NAME = "logback.xml";
    public static final String CONFIG_FILE_LOG4J_NAME = "log4j.properties";
    /**
     * Minimum memory requirements, checked by the Client.
     */
    private static final int MIN_JM_MEMORY = 768; // the minimum memory should be higher than the min heap cutoff
    private static final int MIN_TM_MEMORY = 768;
    private static final int MIN_JM_CORES = 1;
    
    //Jar paths for AM and app
    private String appJarPath;
    //Optional parameters
    private final List<String> jobArgs = new ArrayList<>();
    private List<LocalResourceDTO> extraFiles = new ArrayList<>();

    /**
     * If the user has specified a different number of slots, we store them here
     */
    private int taskManagerSlots = 1;
    private int jobManagerMemoryMb = 768;
    private int jobManagerCores = 1;
    private String jobManagerQueue = "default";

    private int taskManagerMemoryMb = 1024;
    private int taskManagerCount = 1;
    private String configurationDirectory;
    private Path flinkConfigurationPath;
    private Path flinkLoggingConfigurationPath; // optional
    private Path flinkJarPath;
    private StringBuilder dynamicPropertiesEncoded;
    private boolean detached;
    private boolean streamingMode = true;
    private int parallelism;
    private String customName = null;  
    private final Map<String, String> sysProps = new HashMap<>();
    private String sessionId;//used by Kafka
    private String kafkaAddress;
    private String restEndpoint;
    public FlinkYarnRunnerBuilder(String appJarPath, String mainClass) {
        if (appJarPath == null || appJarPath.isEmpty()) {
            throw new IllegalArgumentException(
              "Path to application jar cannot be empty!");
         }
        if (mainClass == null || mainClass.isEmpty()) {
            throw new IllegalArgumentException(
                  "Name of the main class cannot be empty!");
        }
        this.appJarPath = appJarPath;
    }
 
    public FlinkYarnRunnerBuilder addAllJobArgs(String[] jobArgs) {
        this.jobArgs.addAll(Arrays.asList(jobArgs));
        return this;
    }
    
    //@Override
    public void setAppJarPath(String appJarPath){
        this.appJarPath = appJarPath;
    }
    
    public void setJobManagerMemory(int memoryMb) {
        if (memoryMb < MIN_JM_MEMORY) {
            throw new IllegalArgumentException("The JobManager memory (" + memoryMb + ") is below the minimum required memory amount "
                    + "of " + MIN_JM_MEMORY + " MB");
        }
        this.jobManagerMemoryMb = memoryMb;
    }
    public void setJobManagerCores(int cores) {
        if (cores < MIN_JM_CORES) {
            throw new IllegalArgumentException("The JobManager cores (" + cores + ") are below the minimum required amount "
                    + "of " + MIN_JM_CORES);
        }
        this.jobManagerCores = cores;
    }
    
    public void setJobManagerQueue(String queue) {
        this.jobManagerQueue = queue;
    }
    
    public void setTaskManagerMemory(int memoryMb) {
        if (memoryMb < MIN_TM_MEMORY) {
            throw new IllegalArgumentException("The TaskManager memory (" + memoryMb + ") is below the minimum required memory amount "
                    + "of " + MIN_TM_MEMORY + " MB");
        }
        this.taskManagerMemoryMb = memoryMb;
    }

    public void setTaskManagerSlots(int slots) {
        if (slots <= 0) {
            throw new IllegalArgumentException("Number of TaskManager slots must be positive");
        }
        this.taskManagerSlots = slots;
    }

    public int getTaskManagerSlots() {
        return this.taskManagerSlots;
    }

    public void setQueue(String queue) {
        this.jobManagerQueue = queue;
    }

    public void setConfigurationFilePath(Path confPath) {
        flinkConfigurationPath = confPath;
    }

    public void setConfigurationDirectory(String configurationDirectory) {
        this.configurationDirectory = configurationDirectory;
    }

    public void setFlinkLoggingConfigurationPath(Path logConfPath) {
        flinkLoggingConfigurationPath = logConfPath;
    }

    public Path getFlinkLoggingConfigurationPath() {
        return flinkLoggingConfigurationPath;
    }

    public void setTaskManagerCount(int tmCount) {
        if (tmCount < 1) {
            throw new IllegalArgumentException("The TaskManager count has to be at least 1.");
        }
        this.taskManagerCount = tmCount;
    }

    public int getTaskManagerCount() {
        return this.taskManagerCount;
    }

    public boolean isStreamingMode() {
        return streamingMode;
    }

    public void setStreamingMode(boolean streamingMode) {
        this.streamingMode = streamingMode;
    }
    
    public void setDynamicPropertiesEncoded(StringBuilder dynamicPropertiesEncoded) {
        this.dynamicPropertiesEncoded = dynamicPropertiesEncoded;
    }

    public StringBuilder getDynamicPropertiesEncoded() {
        return this.dynamicPropertiesEncoded;
    }

    public void setName(String name) {
        if (name == null) {
            throw new IllegalArgumentException("The passed name is null");
        }
        customName = name;
    }
    
    public FlinkYarnRunnerBuilder setExtraFiles(List<LocalResourceDTO> extraFiles) {
        if (extraFiles == null) {
          throw new IllegalArgumentException("Map of extra files cannot be null.");
        }
        this.extraFiles = extraFiles;
        return this;
    }

    public FlinkYarnRunnerBuilder addExtraFile(LocalResourceDTO dto) {
        if (dto.getName() == null || dto.getName().isEmpty()) {
          throw new IllegalArgumentException(
                  "Filename in extra file mapping cannot be null or empty.");
        }
        if (dto.getPath() == null || dto.getPath().isEmpty()) {
          throw new IllegalArgumentException(
                  "Location in extra file mapping cannot be null or empty.");
        }
        this.extraFiles.add(dto);
        return this;
    }
    public FlinkYarnRunnerBuilder addExtraFiles(List<LocalResourceDTO> projectLocalResources) {
        if(projectLocalResources != null &&!projectLocalResources.isEmpty()){
            this.extraFiles.addAll(projectLocalResources);
        }
        return this;
    }
    public FlinkYarnRunnerBuilder addSystemProperty(String name, String value) {
      sysProps.put(name, value);
      return this;
    }
    public void setSessionId(String sessionId) {
      this.sessionId = sessionId;
    }

    public void setKafkaAddress(String kafkaAddress) {
      this.kafkaAddress = kafkaAddress;
    }
    public void setRestEndpoint(String restEndpoint) {
      this.restEndpoint = restEndpoint;
    }
    public void isReadyForDeployment() throws YarnDeploymentException {
        if (taskManagerCount <= 0) {
            throw new YarnDeploymentException("Taskmanager count must be positive");
        }
        if (this.flinkJarPath == null) {
            throw new YarnDeploymentException("The Flink jar path is null");
        }
        if (this.configurationDirectory == null) {
            throw new YarnDeploymentException("Configuration directory not set");
        }
        if (this.flinkConfigurationPath == null) {
            throw new YarnDeploymentException("Configuration path not set");
        }

        // check if required Hadoop environment variables are set. If not, warn user
        if (System.getenv(Settings.ENV_KEY_HADOOP_CONF_DIR) == null
            && System.getenv(Settings.ENV_KEY_YARN_CONF) == null) {
            logger.log(Level.WARNING,  "Neither the HADOOP_CONF_DIR nor the YARN_CONF_DIR environment variable is set."
                + "The Flink YARN Client needs one of these to be set to properly load the Hadoop "
                + "configuration for accessing YARN.");
        }
    }

    public static boolean allocateResource(int[] nodeManagers, int toAllocate) {
        for (int i = 0; i < nodeManagers.length; i++) {
            if (nodeManagers[i] >= toAllocate) {
                nodeManagers[i] -= toAllocate;
                return true;
            }
        }
        return false;
    }

    public void setDetachedMode(boolean detachedMode) {
        this.detached = detachedMode;
    }

    public boolean isDetached() {
        return detached;
    }

    public int getParallelism() {
        return parallelism;
    }

    public void setParallelism(int parallelism) {
        this.parallelism = parallelism;
    }
    
    /**
     * This method will block until the ApplicationMaster/JobManager have been
     * deployed on YARN.
     * @param project
     * @param hadoopDir
     * @param jobUser
     * @param flinkUser
     * @param flinkDir
     * @param nameNodeIpPort
     * @return 
     * @throws java.io.IOException 
     */
    protected YarnRunner getYarnRunner(String project, final String flinkUser,
            String jobUser, String hadoopDir, final String flinkDir, 
            final String nameNodeIpPort) throws IOException {
              
        //Create the YarnRunner builder for Flink, proceed with setting values
        YarnRunner.Builder builder = new YarnRunner.Builder(Settings.FLINK_AM_MAIN);
        //Set Jar Path
        builder.addToAppMasterEnvironment(YarnRunner.KEY_CLASSPATH, 
            "$PWD:$PWD/"+Settings.FLINK_DEFAULT_CONF_FILE + ":$PWD/"+Settings.FLINK_LOCRSC_FLINK_JAR);
        String stagingPath = File.separator + "Projects" + File.separator + project
            + File.separator
            + Settings.PROJECT_STAGING_DIR + File.separator
            + YarnRunner.APPID_PLACEHOLDER;
        builder.localResourcesBasePath(stagingPath);
        
        //Add Flink jar
        builder.addLocalResource(new LocalResourceDTO(
                Settings.FLINK_LOCRSC_FLINK_JAR, 
                "hdfs://"+nameNodeIpPort+"/user/"+flinkUser+"/"+
                        Settings.FLINK_LOCRSC_FLINK_JAR,
                LocalResourceVisibility.PUBLIC.toString(), 
                LocalResourceType.FILE.toString(), null), false);
                
        //Add Flink conf file
        builder.addLocalResource(new LocalResourceDTO(
                Settings.FLINK_DEFAULT_CONF_FILE, 
                "hdfs://"+nameNodeIpPort+"/user/"+flinkUser+"/"+
                        Settings.FLINK_DEFAULT_CONF_FILE,
                LocalResourceVisibility.PUBLIC.toString(), 
                LocalResourceType.FILE.toString(), null), false);
        StringBuilder shipfilesPaths = new StringBuilder();
        String shipfiles = "";
        //Add extra files to local resources, use filename as key
        for (LocalResourceDTO dto : extraFiles) {
                builder.addLocalResource(dto, false);
                builder.addToAppMasterEnvironment(YarnRunner.KEY_CLASSPATH,
                  "$PWD/" + dto.getName());
                shipfilesPaths.append(dto.getPath());
                shipfilesPaths.append(",");
        }
        if(shipfilesPaths.length()>0){
          shipfiles = shipfilesPaths.substring(0, shipfilesPaths.lastIndexOf(","));
        }
        addSystemProperty(Settings.KAFKA_SESSIONID_ENV_VAR, sessionId);
        addSystemProperty(Settings.KAFKA_BROKERADDR_ENV_VAR, kafkaAddress);
        addSystemProperty(Settings.KAFKA_REST_ENDPOINT_ENV_VAR, restEndpoint);
        
        //Set Flink ApplicationMaster env parameters
        builder.addToAppMasterEnvironment(FlinkYarnRunnerBuilder.ENV_APP_ID, YarnRunner.APPID_PLACEHOLDER);
        builder.addToAppMasterEnvironment(FlinkYarnRunnerBuilder.ENV_TM_COUNT, String.valueOf(taskManagerCount));
        builder.addToAppMasterEnvironment(FlinkYarnRunnerBuilder.ENV_TM_MEMORY, String.valueOf(taskManagerMemoryMb));
        builder.addToAppMasterEnvironment(FlinkYarnRunnerBuilder.ENV_SLOTS, String.valueOf(taskManagerSlots));

        builder.addToAppMasterEnvironment(FlinkYarnRunnerBuilder.FLINK_JAR_PATH, "hdfs://"+nameNodeIpPort+
                "/user/"+flinkUser+"/"+Settings.FLINK_LOCRSC_FLINK_JAR);
        builder.addToAppMasterEnvironment(FlinkYarnRunnerBuilder.ENV_CLIENT_SHIP_FILES, shipfiles);
        builder.addToAppMasterEnvironment(FlinkYarnRunnerBuilder.ENV_CLIENT_USERNAME, jobUser);
        builder.addToAppMasterEnvironment(FlinkYarnRunnerBuilder.ENV_CLIENT_HOME_DIR, "hdfs://"+nameNodeIpPort+"/user/"+flinkUser+"/");
               
        builder.addToAppMasterEnvironment(FlinkYarnRunnerBuilder.ENV_DETACHED, String.valueOf(detached));
        builder.addToAppMasterEnvironment(FlinkYarnRunnerBuilder.ENV_STREAMING_MODE, String.valueOf(streamingMode));
        if(!sysProps.isEmpty()){
          dynamicPropertiesEncoded = new StringBuilder();
        }
        for (String s : sysProps.keySet()) {
          String option = escapeForShell("-D" + s + "=" + sysProps.get(s));
          builder.addJavaOption(option);
          //dynamicPropertiesEncoded.append(s).append("=").append(sysProps.get(s)).append("@@");
        }
        
        if (dynamicPropertiesEncoded.length()>0) {
            builder.addToAppMasterEnvironment(FlinkYarnRunnerBuilder.ENV_DYNAMIC_PROPERTIES,  
                    dynamicPropertiesEncoded.substring(0, dynamicPropertiesEncoded.
                lastIndexOf("@@")));
        }

        // Set up resource type requirements for ApplicationMaster
        builder.amMemory(jobManagerMemoryMb);
        builder.amVCores(jobManagerCores);
        builder.amQueue(jobManagerQueue);
        
        builder.setJobType(JobType.FLINK);
        builder.setAppJarPath(appJarPath);
        builder.setParallelism(parallelism);
        
        
        String name;
        if (customName == null) {
            name = "Flink session with " + taskManagerCount + " TaskManagers";
            if (detached) {
                name += " (detached)";
            }
        } else {
            name = customName;
        }
        
        //Set name of application
        builder.appName(name);
        
        //Set up command
        StringBuilder amargs = new StringBuilder("");                
        //Pass job arguments
        for (String s : jobArgs) {
          amargs.append(" ").append(s);
        }
        builder.amArgs(amargs.toString());
        return builder.build(hadoopDir, flinkDir, nameNodeIpPort, JobType.FLINK);
    }
    
   
    public static class YarnDeploymentException extends RuntimeException {

        private static final long serialVersionUID = -812040641215388943L;

        public YarnDeploymentException() {
        }

        public YarnDeploymentException(String message) {
            super(message);
        }

        public YarnDeploymentException(String message, Throwable cause) {
            super(message, cause);
        }
    }
    
     /**
   * Taken from Apache Spark code: Escapes a string for inclusion in a command
   * line executed by Yarn. Yarn executes commands
   * using `bash -c "command arg1 arg2"` and that means plain quoting doesn't
   * really work. The
   * argument is enclosed in single quotes and some key characters are escaped.
   * <p/>
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
