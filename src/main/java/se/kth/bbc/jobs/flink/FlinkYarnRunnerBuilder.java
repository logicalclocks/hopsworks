package se.kth.bbc.jobs.flink;

import org.apache.hadoop.fs.Path;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import se.kth.bbc.jobs.jobhistory.JobType;
import se.kth.bbc.jobs.yarn.YarnRunner;
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

    private static final Logger LOG = LoggerFactory.getLogger(FlinkYarnRunnerBuilder.class);

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
    private static final String CONFIG_FILE_NAME = "flink-conf.yaml";
    public static final String CONFIG_FILE_LOGBACK_NAME = "logback.xml";
    public static final String CONFIG_FILE_LOG4J_NAME = "log4j.properties";
    /**
     * Minimum memory requirements, checked by the Client.
     */
    private static final int MIN_JM_MEMORY = 768; // the minimum memory should be higher than the min heap cutoff
    private static final int MIN_TM_MEMORY = 768;
    private static final int MIN_JM_CORES = 1;
    
    //Jar paths for AM and app
    private String appJarPath, mainClass;
    //Optional parameters
    private final List<String> jobArgs = new ArrayList<>();
    

    /**
     * If the user has specified a different number of slots, we store them here
     */
    private int taskManagerSlots = 1;
    private int jobManagerMemoryMb = 1024;
    private int jobManagerCores = 1;
    private String jobManagerQueue = "default";

    private int taskManagerMemoryMb = 1024;
    private int taskManagerCount = 1;
    private String configurationDirectory;
    private Path flinkConfigurationPath;
    private Path flinkLoggingConfigurationPath; // optional
    private Path flinkJarPath;
    private String dynamicPropertiesEncoded;
    private List<File> shipFiles = new ArrayList<File>();
    private boolean detached;
    private boolean streamingMode;
    private int parallelism;
    private String customName = null;

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
        this.mainClass = mainClass;
        
    }

    public FlinkYarnRunnerBuilder addAllJobArgs(String[] jobArgs) {
        this.jobArgs.addAll(Arrays.asList(jobArgs));
        return this;
    }
    public void setAppJarPath(String appJarPath){
        this.appJarPath = appJarPath;
    }
    
    //@Override
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
    
    //@Override
    public void setTaskManagerMemory(int memoryMb) {
        if (memoryMb < MIN_TM_MEMORY) {
            throw new IllegalArgumentException("The TaskManager memory (" + memoryMb + ") is below the minimum required memory amount "
                    + "of " + MIN_TM_MEMORY + " MB");
        }
        this.taskManagerMemoryMb = memoryMb;
    }


    //@Override
    public void setTaskManagerSlots(int slots) {
        if (slots <= 0) {
            throw new IllegalArgumentException("Number of TaskManager slots must be positive");
        }
        this.taskManagerSlots = slots;
    }

    //@Override
    public int getTaskManagerSlots() {
        return this.taskManagerSlots;
    }

    //@Override
    public void setQueue(String queue) {
        this.jobManagerQueue = queue;
    }

    //@Override
    public void setLocalJarPath(Path localJarPath) {
        if (!localJarPath.toString().endsWith("jar")) {
            throw new IllegalArgumentException("The passed jar path ('" + localJarPath + "') does not end with the 'jar' extension");
        }
        this.flinkJarPath = localJarPath;
    }

    //@Override
    public void setConfigurationFilePath(Path confPath) {
        flinkConfigurationPath = confPath;
    }

    public void setConfigurationDirectory(String configurationDirectory) {
        this.configurationDirectory = configurationDirectory;
    }

    //@Override
    public void setFlinkLoggingConfigurationPath(Path logConfPath) {
        flinkLoggingConfigurationPath = logConfPath;
    }

    //@Override
    public Path getFlinkLoggingConfigurationPath() {
        return flinkLoggingConfigurationPath;
    }

    //@Override
    public void setTaskManagerCount(int tmCount) {
        if (tmCount < 1) {
            throw new IllegalArgumentException("The TaskManager count has to be at least 1.");
        }
        this.taskManagerCount = tmCount;
    }

    //@Override
    public int getTaskManagerCount() {
        return this.taskManagerCount;
    }

    public boolean isStreamingMode() {
        return streamingMode;
    }

    public void setStreamingMode(boolean streamingMode) {
        this.streamingMode = streamingMode;
    }
    
    //@Override
    public void setShipFiles(List<File> shipFiles) {
        for (File shipFile : shipFiles) {
			// remove uberjar from ship list (by default everything in the lib/ folder is added to
            // the list of files to ship, but we handle the uberjar separately.
            if (!(shipFile.getName().startsWith("flink-dist") && shipFile.getName().endsWith("jar"))) {
                this.shipFiles.add(shipFile);
            }
        }
    }

    public void setDynamicPropertiesEncoded(String dynamicPropertiesEncoded) {
        this.dynamicPropertiesEncoded = dynamicPropertiesEncoded;
    }

    //@Override
    public String getDynamicPropertiesEncoded() {
        return this.dynamicPropertiesEncoded;
    }

    //@Override
    public void setName(String name) {
        if (name == null) {
            throw new IllegalArgumentException("The passed name is null");
        }
        customName = name;
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
            LOG.warn("Neither the HADOOP_CONF_DIR nor the YARN_CONF_DIR environment variable is set."
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

    //@Override
    public void setDetachedMode(boolean detachedMode) {
        this.detached = detachedMode;
    }

    //@Override
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
     * @param flinkUser
     * @param flinkDir
     * @param nameNodeIpPort
     * @return 
     * @throws java.io.IOException 
     */
    protected YarnRunner getYarnRunner(String project, final String flinkUser,
            String hadoopDir, final String flinkDir, 
            final String nameNodeIpPort) throws IOException {
              
        //Set Classpath and Jar Path
        String flinkClasspath = Settings.getFlinkDefaultClasspath(flinkDir);
        //Create the YarnRunner builder for Flink, proceed with setting values
        YarnRunner.Builder builder = new YarnRunner.Builder(Settings.FLINK_AM_MAIN);
         
        builder.addToAppMasterEnvironment("CLASSPATH", flinkClasspath);
        String stagingPath = File.separator + "Projects" + File.separator + project
            + File.separator
            + Settings.PROJECT_STAGING_DIR + File.separator
            + YarnRunner.APPID_PLACEHOLDER;
        builder.localResourcesBasePath(stagingPath);
        
        //Add Flink jar
        builder.addLocalResource(Settings.FLINK_LOCRSC_FLINK_JAR, "hdfs://"+nameNodeIpPort+"/user/"+flinkUser+"/"+Settings.FLINK_LOCRSC_FLINK_JAR,
                false);
        //Add Flink conf file
        builder.addLocalResource(Settings.FLINK_DEFAULT_CONF_FILE, "hdfs://"+nameNodeIpPort+"/user/"+flinkUser+"/"+Settings.FLINK_DEFAULT_CONF_FILE,
                false);
        //Add log4j properties file
//        builder.addLocalResource(Settings.FLINK_DEFAULT_LOG4J_FILE, "hdfs://10.0.2.15/user/glassfish/log4j.properties",
//                false);
//        //Add log4j properties file
//        builder.addLocalResource(Settings.FLINK_DEFAULT_LOGBACK_FILE, "hdfs://10.0.2.15/user/glassfish/logback.xml",
//                false);
        //Add app jar
//        builder.addLocalResource(Settings.FLINK_LOCRSC_APP_JAR, appJarPath,
//            !appJarPath.startsWith("hdfs:"));
//        
//        String logbackFile = "hdfs://"+nameNodeIpPort+"/user/glassfish/logback.xml";//configurationDirectory + File.separator + FlinkYarnRunnerBuilder.CONFIG_FILE_LOGBACK_NAME;
//        boolean hasLogback = new File(logbackFile).exists();
//        String log4jFile = "hdfs://"+nameNodeIpPort+"/user/glassfish/log4j.properties";//configurationDirectory + File.separator + FlinkYarnRunnerBuilder.CONFIG_FILE_LOG4J_NAME;
//
//        boolean hasLog4j = new File(log4jFile).exists();
//        if (hasLogback) {
//            shipFiles.add(new File(logbackFile));
//        }
//        if (hasLog4j) {
//            shipFiles.add(new File(log4jFile));
//        }


        //Set Flink ApplicationMaster env parameters
        builder.addToAppMasterEnvironment(FlinkYarnRunnerBuilder.ENV_APP_ID, YarnRunner.APPID_PLACEHOLDER);
        builder.addToAppMasterEnvironment(FlinkYarnRunnerBuilder.ENV_TM_COUNT, String.valueOf(taskManagerCount));
        builder.addToAppMasterEnvironment(FlinkYarnRunnerBuilder.ENV_TM_MEMORY, String.valueOf(taskManagerMemoryMb));
        builder.addToAppMasterEnvironment(FlinkYarnRunnerBuilder.ENV_SLOTS, String.valueOf(taskManagerSlots));

        builder.addToAppMasterEnvironment(FlinkYarnRunnerBuilder.FLINK_JAR_PATH, flinkJarPath.toString());
        builder.addToAppMasterEnvironment(FlinkYarnRunnerBuilder.ENV_CLIENT_SHIP_FILES, "");
        builder.addToAppMasterEnvironment(FlinkYarnRunnerBuilder.ENV_CLIENT_USERNAME, flinkUser);
        builder.addToAppMasterEnvironment(FlinkYarnRunnerBuilder.ENV_CLIENT_HOME_DIR, "hdfs://"+nameNodeIpPort+"/user/"+flinkUser+"/");
               
        builder.addToAppMasterEnvironment(FlinkYarnRunnerBuilder.ENV_DETACHED, String.valueOf(detached));
        builder.addToAppMasterEnvironment(FlinkYarnRunnerBuilder.ENV_STREAMING_MODE, String.valueOf(streamingMode));
        if (dynamicPropertiesEncoded != null) {
            builder.addToAppMasterEnvironment(FlinkYarnRunnerBuilder.ENV_DYNAMIC_PROPERTIES, dynamicPropertiesEncoded);
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

}
