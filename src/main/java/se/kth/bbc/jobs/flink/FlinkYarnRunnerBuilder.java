package se.kth.bbc.jobs.flink;

import org.apache.flink.api.java.tuple.Tuple2;
import org.apache.flink.client.CliFrontend;
import org.apache.flink.client.FlinkYarnSessionCli;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.security.UserGroupInformation;
import org.apache.hadoop.yarn.api.records.LocalResource;
import org.apache.hadoop.yarn.util.Records;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import org.apache.flink.yarn.ApplicationMaster;
import org.apache.flink.yarn.FlinkYarnClient;
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

    private static final Logger LOG = LoggerFactory.getLogger(FlinkYarnClient.class);

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

    /**
     * Minimum memory requirements, checked by the Client.
     */
    private static final int MIN_JM_MEMORY = 768; // the minimum memory should be higher than the min heap cutoff
    private static final int MIN_TM_MEMORY = 768;

    private Configuration conf;
//    private YarnClient yarnClient;
//    private YarnClientApplication yarnApplication;
    //Jar paths for AM and app
     private final String appJarPath, mainClass;
    /**
     * Files (usually in a distributed file system) used for the YARN session of
     * Flink. Contains configuration files and jar files.
     */
    private Path sessionFilesDir;

    /**
     * If the user has specified a different number of slots, we store them here
     */
    private int slots = -1;
    private int jobManagerMemoryMb = 1024;
    private int taskManagerMemoryMb = 1024;
    private int taskManagerCount = 1;
    private String yarnQueue = null;
    private String configurationDirectory;
    private Path flinkConfigurationPath;
    private Path flinkLoggingConfigurationPath; // optional
    private Path flinkJarPath;
    private String dynamicPropertiesEncoded;
    private List<File> shipFiles = new ArrayList<File>();
    private org.apache.flink.configuration.Configuration flinkConfiguration;
    private boolean detached;
    private boolean streamingMode;
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

    protected Class<?> getApplicationMasterClass() {
		return ApplicationMaster.class;
    }

    //@Override
    public void setJobManagerMemory(int memoryMb) {
        if (memoryMb < MIN_JM_MEMORY) {
            throw new IllegalArgumentException("The JobManager memory (" + memoryMb + ") is below the minimum required memory amount "
                    + "of " + MIN_JM_MEMORY + " MB");
        }
        this.jobManagerMemoryMb = memoryMb;
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
    public void setFlinkConfigurationObject(org.apache.flink.configuration.Configuration conf) {
        this.flinkConfiguration = conf;
    }

    //@Override
    public void setTaskManagerSlots(int slots) {
        if (slots <= 0) {
            throw new IllegalArgumentException("Number of TaskManager slots must be positive");
        }
        this.slots = slots;
    }

    //@Override
    public int getTaskManagerSlots() {
        return this.slots;
    }

    //@Override
    public void setQueue(String queue) {
        this.yarnQueue = queue;
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
        if (this.flinkConfiguration == null) {
            throw new YarnDeploymentException("Flink configuration object has not been set");
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
        isReadyForDeployment();

        //Create the YarnRunner builder for Flink, proceed with setting values
        YarnRunner.Builder builder = new YarnRunner.Builder(flinkDir+"/flink.jar", "flink.jar");
         
        //Set Classpath and Jar Path
        String flinkClasspath = Settings.getFlinkDefaultClasspath(flinkDir);
        String hdfsFlinkJarPath = Settings.getHdfsFlinkJarPath(flinkUser);
        builder.addToAppMasterEnvironment("CLASSPATH", flinkClasspath);
        String stagingPath = File.separator + "Projects" + File.separator + project
            + File.separator
            + Settings.PROJECT_STAGING_DIR + File.separator
            + YarnRunner.APPID_PLACEHOLDER;
        builder.localResourcesBasePath(stagingPath);
        
        //Add Flink jar
        builder.addLocalResource(Settings.FLINK_LOCRSC_SPARK_JAR, hdfsFlinkJarPath,
                false);
        //Add app jar
        builder.addLocalResource(Settings.FLINK_LOCRSC_APP_JAR, appJarPath,
            !appJarPath.startsWith("hdfs:"));
    
    
    	// ------------------ Add dynamic properties to local flinkConfiguraton ------
        List<Tuple2<String, String>> dynProperties = CliFrontend.getDynamicProperties(dynamicPropertiesEncoded);
        for (Tuple2<String, String> dynProperty : dynProperties) {
            flinkConfiguration.setString(dynProperty.f0, dynProperty.f1);
        }

	

		// ------------------ Check if the YARN Cluster has the requested resources --------------
		// the yarnMinAllocationMB specifies the smallest possible container allocation size.
        // all allocations below this value are automatically set to this value.
//        final int yarnMinAllocationMB = conf.getInt("yarn.scheduler.minimum-allocation-mb", 0);
//        if (jobManagerMemoryMb < yarnMinAllocationMB || taskManagerMemoryMb < yarnMinAllocationMB) {
//            LOG.warn("The JobManager or TaskManager memory is below the smallest possible YARN Container size. "
//                    + "The value of 'yarn.scheduler.minimum-allocation-mb' is '" + yarnMinAllocationMB + "'. Please increase the memory size."
//                    + "YARN will allocate the smaller containers but the scheduler will account for the minimum-allocation-mb, maybe not all instances "
//                    + "you requested will start.");
//        }
//
//        // set the memory to minAllocationMB to do the next checks correctly
//        if (jobManagerMemoryMb < yarnMinAllocationMB) {
//            jobManagerMemoryMb = yarnMinAllocationMB;
//        }
//        if (taskManagerMemoryMb < yarnMinAllocationMB) {
//            taskManagerMemoryMb = yarnMinAllocationMB;
//        }

        String logbackFile = configurationDirectory + File.separator + FlinkYarnSessionCli.CONFIG_FILE_LOGBACK_NAME;
        boolean hasLogback = new File(logbackFile).exists();
        String log4jFile = configurationDirectory + File.separator + FlinkYarnSessionCli.CONFIG_FILE_LOG4J_NAME;

        boolean hasLog4j = new File(log4jFile).exists();
        if (hasLogback) {
            shipFiles.add(new File(logbackFile));
        }
        if (hasLog4j) {
            shipFiles.add(new File(log4jFile));
        }


        // Setup jar for ApplicationMaster
        LocalResource appMasterJar = Records.newRecord(LocalResource.class);
        LocalResource flinkConf = Records.newRecord(LocalResource.class);
        Map<String, LocalResource> localResources = new HashMap<String, LocalResource>(2);
        localResources.put("flink.jar", appMasterJar);
        localResources.put("flink-conf.yaml", flinkConf);

               
        StringBuilder envShipFileList = new StringBuilder();
        // upload ship files
        for (int i = 0; i < shipFiles.size(); i++) {
            File shipFile = shipFiles.get(i);
            //TODO: Check if we need absolute or relative path of shipFile
            builder.addLocalResource(shipFile.getName(), shipFile.getAbsolutePath());
        }


        builder.addToAppMasterEnvironment(FlinkYarnClient.ENV_TM_COUNT, String.valueOf(taskManagerCount));
        builder.addToAppMasterEnvironment(FlinkYarnClient.ENV_TM_MEMORY, String.valueOf(taskManagerMemoryMb));
        builder.addToAppMasterEnvironment(FlinkYarnClient.FLINK_JAR_PATH, flinkJarPath.toString());
        builder.addToAppMasterEnvironment(FlinkYarnClient.ENV_CLIENT_SHIP_FILES, envShipFileList.toString());
        try {
            builder.addToAppMasterEnvironment(FlinkYarnClient.ENV_CLIENT_USERNAME, UserGroupInformation.getCurrentUser().getShortUserName());
        } catch (IOException ex) {
            LOG.error("Error while getting Flink client username", ex);
        }
        builder.addToAppMasterEnvironment(FlinkYarnClient.ENV_SLOTS, String.valueOf(slots));
        builder.addToAppMasterEnvironment(FlinkYarnClient.ENV_DETACHED, String.valueOf(detached));
        builder.addToAppMasterEnvironment(FlinkYarnClient.ENV_STREAMING_MODE, String.valueOf(streamingMode));
        if (dynamicPropertiesEncoded != null) {
            //appMasterEnv.put(FlinkYarnClient.ENV_DYNAMIC_PROPERTIES, dynamicPropertiesEncoded);
            builder.addToAppMasterEnvironment(FlinkYarnClient.ENV_DYNAMIC_PROPERTIES, dynamicPropertiesEncoded);
        }

        // Set up resource type requirements for ApplicationMaster

        builder.amMemory(jobManagerMemoryMb);
        builder.amVCores(1);
        
        String name;
        if (customName == null) {
            name = "Flink session with " + taskManagerCount + " TaskManagers";
            if (detached) {
                name += " (detached)";
            }
        } else {
            name = customName;
        }
        
        //HOPS YarnRunner 
        builder.appName(name);

        return builder.build(hadoopDir, flinkDir, nameNodeIpPort);
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
