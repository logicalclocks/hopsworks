package se.kth.bbc.yarn;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Vector;
import java.util.jar.JarFile;
import java.util.jar.Manifest;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FileStatus;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.yarn.api.ApplicationConstants;
import org.apache.hadoop.yarn.api.protocolrecords.GetNewApplicationResponse;
import org.apache.hadoop.yarn.api.records.ApplicationId;
import org.apache.hadoop.yarn.api.records.ApplicationReport;
import org.apache.hadoop.yarn.api.records.ApplicationSubmissionContext;
import org.apache.hadoop.yarn.api.records.ContainerLaunchContext;
import org.apache.hadoop.yarn.api.records.FinalApplicationStatus;
import org.apache.hadoop.yarn.api.records.LocalResource;
import org.apache.hadoop.yarn.api.records.LocalResourceType;
import org.apache.hadoop.yarn.api.records.LocalResourceVisibility;
import org.apache.hadoop.yarn.api.records.NodeReport;
import org.apache.hadoop.yarn.api.records.NodeState;
import org.apache.hadoop.yarn.api.records.QueueACL;
import org.apache.hadoop.yarn.api.records.QueueInfo;
import org.apache.hadoop.yarn.api.records.QueueUserACLInfo;
import org.apache.hadoop.yarn.api.records.Resource;
import org.apache.hadoop.yarn.api.records.YarnApplicationState;
import org.apache.hadoop.yarn.api.records.YarnClusterMetrics;
import org.apache.hadoop.yarn.client.api.YarnClient;
import org.apache.hadoop.yarn.client.api.YarnClientApplication;
import org.apache.hadoop.yarn.conf.YarnConfiguration;
import org.apache.hadoop.yarn.exceptions.YarnException;
import org.apache.hadoop.yarn.util.ConverterUtils;
import se.kth.bbc.lims.EnvironmentVariableFacade;

/**
 *
 * @author stig
 */
public class YarnRunner {

    private static final Logger logger = Logger.getLogger(YarnRunner.class.getName());
    //Some hard-coded paths for the AM
    private final static String amJarHdfsPath = "appMaster.jar";
    private ApplicationId appId = null;

    //Signify whether the application has finished, started.
    private boolean finished = false;
    private boolean started = false;

    /*
    
     CONSTRUCTION
    
     */
    private final String appMasterJarPath;
    private final String amQueue;
    private int amMemory;
    private int amVCores;
    private final String appName;
    private final String amMainClass;
    private final String amArgs;
    private final Map<String, String> amLocalResources;

    //Non-constructor-passed attributes
    private final Configuration conf;
    private final YarnClient yarnClient;

    private YarnRunner(Builder builder) {
        this.appMasterJarPath = builder.appMasterJarPath;
        this.amQueue = builder.amQueue;
        this.amMemory = builder.amMemory;
        this.amVCores = builder.amVCores;
        this.appName = builder.appName;
        this.amMainClass = builder.appMasterMainClass;
        this.amArgs = builder.extraArgs;
        this.conf = builder.conf;
        this.yarnClient = builder.yarnClient;
        this.amLocalResources = builder.amLocalResources;
    }

    public static class Builder {

        private EnvironmentVariableFacade environmentVariableFacade;

        //Required attributes
        private final String appMasterJarPath;

        //Optional attributes
        // Queue for App master
        private String amQueue = "default"; //TODO: enable changing this, or infer from user data
        // Memory for App master
        private int amMemory = 10;
        //Number of cores for appMaster
        private int amVCores = 1;
        // Application name
        private String appName = "BbcYarnApp";
        //The name of the application app master class
        private String appMasterMainClass;
        //Arguments to pass on in invocation of Application master
        private String extraArgs;
        //List of paths to resources that should be copied to application master
        private Map<String, String> amLocalResources = null;

        //Hadoop Configuration
        private Configuration conf;
        //YarnClient
        private YarnClient yarnClient;

        //Constructor
        public Builder(String appMasterJarPath) {
            this.appMasterJarPath = appMasterJarPath;
        }

        public Builder appMasterArgs(String amArgs) {
            this.extraArgs = amArgs;
            return this;
        }

        public Builder appMasterMemory(int amMem) {
            this.amMemory = amMem;
            return this;
        }

        public Builder appMasterVCores(int amVCores) {
            this.amVCores = amVCores;
            return this;
        }

        public Builder appName(String appName) {
            this.appName = appName;
            return this;
        }

        public Builder appMasterMainClass(String amMainClass) {
            this.appMasterMainClass = amMainClass;
            return this;
        }

        /**
         * Map from a local resource name to a path.
         *
         * @param localResources
         * @return
         */
        public Builder setLocalResources(Map<String, String> localResources) {
            this.amLocalResources = localResources;
            return this;
        }

        public Builder addLocalResourcePath(String name, String path) {
            if (amLocalResources == null) {
                amLocalResources = new HashMap<>();
            }
            amLocalResources.put(name, path);
            return this;
        }

        public Builder addAllLocalResourcesPaths(Map<String, String> resources) {
            if (amLocalResources == null) {
                amLocalResources = new HashMap<>();
            }
            amLocalResources.putAll(resources);
            return this;
        }

        /**
         * Build the YarnRunner instance.
         *
         * @return
         */
        public YarnRunner build() throws IllegalStateException {
            //Set configuration
            setConfiguration();

            //Set YarnClient
            yarnClient = YarnClient.createYarnClient();
            yarnClient.init(conf);

            if (appMasterMainClass == null) {
                appMasterMainClass = getMainClassNameFromJar();
                if(appMasterMainClass == null){
                    throw new IllegalStateException("Could not infer main class name.");
                }
            }
            return new YarnRunner(this);
        }

        private EnvironmentVariableFacade lookupEnvironmentVariableFacadeBean() throws NamingException {
            Context c = new InitialContext();
            //return (EnvironmentVariableFacade) c.lookup("java:global/se.kth.hop_hop-dashboard_war_1.0-SNAPSHOT/EnvironmentVariableFacade!se.kth.bbc.lims.EnvironmentVariableFacade");
            return null;
        }

        private void setConfiguration() throws IllegalStateException {
            //Get the path to the Yarn configuration file from environment variables
            String yarnConfDir = System.getenv("YARN_CONF_DIR");
            //If not found in environment variables: try DB
            if (yarnConfDir == null) {
                try {
                    environmentVariableFacade = lookupEnvironmentVariableFacadeBean();
                } catch (NamingException e) {
                    logger.log(Level.SEVERE, "Could not find Environment bean.", e);
                    throw new IllegalStateException();
                }
                yarnConfDir = environmentVariableFacade.getValue("YARN_CONF_DIR");
            }
            //If still not found: throw exception
            if (yarnConfDir == null) {
                logger.log(Level.SEVERE, "No configuration path set!");
                throw new IllegalStateException("No configuration path set.");
            }

            //Get the configuration file at found path
            Path confPath = new Path(yarnConfDir);
            File confFile = new File(confPath + File.separator + "yarn-site.xml");
            if (!confFile.exists()) {
                logger.log(Level.SEVERE, "Unable to locate configuration file in {0}", confFile);
                throw new IllegalStateException("No conf file");
            }

            //Set the Configuration object for the returned YarnClient
            Path yarnPath = new Path(confFile.getAbsolutePath());
            conf = new Configuration();
            conf.addResource(yarnPath);
        }

        private String getMainClassNameFromJar() {
            String fileName = appMasterJarPath;
            File file = new File(fileName);
            String mainClassName = null;

            JarFile jarFile;
            try {
                jarFile = new JarFile(fileName);
                Manifest manifest = jarFile.getManifest();
                if (manifest != null) {
                    mainClassName = manifest.getMainAttributes().getValue("Main-Class");
                }
                jarFile.close();
            } catch (IOException io) {
                logger.log(Level.SEVERE, "Could not open jar file.", io);
                return null;
            }

            if (mainClassName != null) {
                return mainClassName.replaceAll("/", ".");
            } else {
                return null;
            }
        }

    }

    /*
    
     STARTING APPLICATION MASTER & RUNNING APP
    
     */
    public ApplicationId startAppMaster() throws YarnException, IOException {
        logger.info("Running Client");
        yarnClient.start();
        YarnClusterMetrics clusterMetrics = yarnClient.getYarnClusterMetrics();
        logger.info("Got Cluster metric info from ASM"
                + ", numNodeManagers=" + clusterMetrics.getNumNodeManagers());
        List<NodeReport> clusterNodeReports = yarnClient.getNodeReports(
                NodeState.RUNNING);
        logger.info("Got Cluster node info from ASM");
        for (NodeReport node : clusterNodeReports) {
            logger.info("Got node report from ASM for"
                    + ", nodeId=" + node.getNodeId()
                    + ", nodeAddress" + node.getHttpAddress()
                    + ", nodeRackName" + node.getRackName()
                    + ", nodeNumContainers" + node.getNumContainers());
        }
        QueueInfo queueInfo = yarnClient.getQueueInfo(this.amQueue);
        logger.info("Queue info"
                + ", queueName=" + queueInfo.getQueueName()
                + ", queueCurrentCapacity=" + queueInfo.getCurrentCapacity()
                + ", queueMaxCapacity=" + queueInfo.getMaximumCapacity()
                + ", queueApplicationCount=" + queueInfo.getApplications().size()
                + ", queueChildQueueCount=" + queueInfo.getChildQueues().size());
        List<QueueUserACLInfo> listAclInfo = yarnClient.getQueueAclsInfo();
        for (QueueUserACLInfo aclInfo : listAclInfo) {
            for (QueueACL userAcl : aclInfo.getUserAcls()) {
                logger.info("User ACL Info for Queue"
                        + ", queueName=" + aclInfo.getQueueName()
                        + ", userAcl=" + userAcl.name());
            }
        }
        // Get a new application id
        YarnClientApplication app = yarnClient.createApplication();
        GetNewApplicationResponse appResponse = app.getNewApplicationResponse();
        // TODO get min/max resource capabilities from RM and change memory ask if needed
        // If we do not have min/max, we may not be able to correctly request
        // the required resources from the RM for the app master
        // Memory ask has to be a multiple of min and less than max.
        // Dump out information about cluster capability as seen by the resource manager
        int maxMem = appResponse.getMaximumResourceCapability().getMemory();
        logger.info("Max mem capabililty of resources in this cluster " + maxMem);
        // A resource ask cannot exceed the max.
        if (amMemory > maxMem) {
            logger.info("AM memory specified above max threshold of cluster. Using max value."
                    + ", specified=" + amMemory
                    + ", max=" + maxMem);
            amMemory = maxMem;
        }
        int maxVCores = appResponse.getMaximumResourceCapability().getVirtualCores();
        logger.info("Max virtual cores capabililty of resources in this cluster " + maxVCores);
        if (amVCores > maxVCores) {
            logger.info("AM virtual cores specified above max threshold of cluster. "
                    + "Using max value." + ", specified=" + amVCores
                    + ", max=" + maxVCores);
            amVCores = maxVCores;
        }
        // set the application name
        ApplicationSubmissionContext appContext = app.getApplicationSubmissionContext();
        appId = appContext.getApplicationId();
        //TODO
        //appContext.setKeepContainersAcrossApplicationAttempts(keepContainers);
        appContext.setApplicationName(appName);

        // set local resources for the application master
        Map<String, LocalResource> localResources = addAllToLocalResources();

        // Set the env variables to be setup in the env where the application master will be run
        logger.info("Set the environment for the application master");
        Map<String, String> env = new HashMap<>();

        setUpClassPath(env);
        List<String> commands = setUpCommands();

        // Set up the container launch context for the application master
        ContainerLaunchContext amContainer = ContainerLaunchContext.newInstance(
                localResources, env, commands, null, null, null);
        // Set up resource type requirements
        // For now, both memory and vcores are supported, so we set memory and
        // vcores requirements
        Resource capability = Resource.newInstance(amMemory, amVCores);
        appContext.setResource(capability);
        // Service data is a binary blob that can be passed to the application
        // Not needed in this scenario
        // amContainer.setServiceData(serviceData);
        // Setup security tokens
        appContext.setAMContainerSpec(amContainer);
        // Set the priority for the application master
        // TODO - what is the range for priority? how to decide?
        //TODO
        //Priority pri = Priority.newInstance(amPriority);
        //appContext.setPriority(pri);
        // Set the queue to which this application is to be submitted in the RM
        appContext.setQueue(amQueue);
        // Submit the application to the applications manager
        // SubmitApplicationResponse submitResp = applicationsManager.submitApplication(appRequest);
        // Ignore the response as either a valid response object is returned on success
        // or an exception thrown to denote some form of a failure
        logger.info("Submitting application to ASM");
        yarnClient.submitApplication(appContext);
        started = true;
        return appId;
    }

    /**
     * Add all the resources that should be copied to the ApplicationMaster's
     * container to the LocalResources.
     */
    private Map<String, LocalResource> addAllToLocalResources() throws IOException {
        // local files or archives as needed
        // Consider the jar file for the application master as part of the local resources
        Map<String, LocalResource> localResources = new HashMap<>();
        logger.info("Copy App Master jar from local filesystem and add to local environment");
        // Copy the application master jar to the filesystem
        // Create a local resource to point to the destination jar path
        FileSystem fs = FileSystem.get(conf);
        addFileToLocalResources(fs, appMasterJarPath, amJarHdfsPath, appId.toString(),
                localResources);

        for (String key : amLocalResources.keySet()) {
            addFileToLocalResources(fs, amLocalResources.get(key), key, appId.toString(), localResources);
        }
        return localResources;
    }

    private void addFileToLocalResources(FileSystem fs, String fileSrcPath, String fileDstPath, String appId, Map<String, LocalResource> localResources) throws IOException {
        String suffix = appName + "/" + appId + "/" + fileDstPath;
        Path dst = new Path(fs.getHomeDirectory(), suffix);
        fs.copyFromLocalFile(new Path(fileSrcPath), dst);
        FileStatus scFileStatus = fs.getFileStatus(dst);
        LocalResource scRsrc
                = LocalResource.newInstance(
                        ConverterUtils.getYarnUrlFromURI(dst.toUri()),
                        LocalResourceType.FILE, LocalResourceVisibility.APPLICATION,
                        scFileStatus.getLen(), scFileStatus.getModificationTime());
        localResources.put(fileDstPath, scRsrc);
    }

    private void setUpClassPath(Map<String, String> env) {
        // Add AppMaster.jar location to classpath
        // At some point we should not be required to add
        // the hadoop specific classpaths to the env.
        // It should be provided out of the box.
        // For now setting all required classpaths including
        // the classpath to "." for the application jar
        StringBuilder classPathEnv = new StringBuilder(ApplicationConstants.Environment.CLASSPATH.$())
                .append(":").append("./*");
        for (String c : conf.getStrings(
                YarnConfiguration.YARN_APPLICATION_CLASSPATH, YarnConfiguration.DEFAULT_YARN_APPLICATION_CLASSPATH)) {
            classPathEnv.append(":").append(c.trim());
        }
        classPathEnv.append(":").append("./log4j.properties");
        // add the runtime classpath needed for tests to work
        if (conf.getBoolean(YarnConfiguration.IS_MINI_YARN_CLUSTER, false)) {
            classPathEnv.append(':');
            classPathEnv.append(System.getProperty("java.class.path"));
        }
        env.put("CLASSPATH", classPathEnv.toString());
    }

    private List<String> setUpCommands() {
        // Set the necessary command to execute the application master
        Vector<CharSequence> vargs = new Vector<CharSequence>(30);
        // Set java executable command
        logger.info("Setting up app master command");
        vargs.add(ApplicationConstants.Environment.JAVA_HOME.$() + "/bin/java");
        // Set Xmx based on am memory size
        vargs.add("-Xmx" + amMemory + "m");
        // Set class name
        vargs.add(amMainClass);
        // Set params for Application Master
        vargs.add(amArgs);
        // Get final commmand
        StringBuilder amcommand = new StringBuilder();
        for (CharSequence str : vargs) {
            amcommand.append(str).append(" ");
        }
        logger.log(Level.INFO, "Completed setting up app master command: {0}", amcommand.toString());
        List<String> commands = new ArrayList<>();
        commands.add(amcommand.toString());
        return commands;
    }

    /**
     * Kill a submitted application by sending a call to the ASM
     *
     * @param appId Application Id to be killed.
     * @throws YarnException
     * @throws IOException
     */
    public void forceKillApplication() throws YarnException, IOException {
        // TODO clarify whether multiple jobs with the same app id can be submitted and be running at
        // the same time.
        // If yes, can we kill a particular attempt only?
        // Response can be ignored as it is non-null on success or
        // throws an exception in case of failures
        yarnClient.killApplication(appId);
    }

    /*
    
    
     MONITORING UTILITY METHODS
    
    
     */
    public boolean isFinished() throws IOException, YarnException {
        if (!finished) {
            finished = (YarnApplicationState.FINISHED == getApplicationReport().getYarnApplicationState());;
        }
        return finished;
    }

    public boolean isStarted() {
        return started;
    }

    public ApplicationReport getApplicationReport() throws YarnException, IOException {
        return yarnClient.getApplicationReport(appId);
    }

    public FinalApplicationStatus getFinalYarnApplicationState() throws IOException, YarnException {
        if (isFinished()) {
            return getApplicationReport().getFinalApplicationStatus();
        } else {
            return null;
        }
    }

    public String getFormattedReport() throws YarnException, IOException {
        // Get application report for the appId we are interested in
        ApplicationReport report = getApplicationReport();
        StringBuilder sb = new StringBuilder();
        sb.append("Got application report from ASM for"
                + ", appId=" + appId.getId()
                + ", clientToAMToken=" + report.getClientToAMToken()
                + ", appDiagnostics=" + report.getDiagnostics()
                + ", appMasterHost=" + report.getHost()
                + ", appQueue=" + report.getQueue()
                + ", appMasterRpcPort=" + report.getRpcPort()
                + ", appStartTime=" + report.getStartTime()
                + ", yarnAppState=" + report.getYarnApplicationState().toString()
                + ", distributedFinalState=" + report.getFinalApplicationStatus().toString()
                + ", appTrackingUrl=" + report.getTrackingUrl()
                + ", appUser=" + report.getUser());
        YarnApplicationState state = report.getYarnApplicationState();
        FinalApplicationStatus dsStatus = report.getFinalApplicationStatus();
        if (YarnApplicationState.FINISHED == state) {
            if (FinalApplicationStatus.SUCCEEDED == dsStatus) {
                sb.append('\n').append("Application has completed successfully.");
                return sb.toString();
            } else {
                sb.append('\n').append("Application finished unsuccessfully."
                        + " YarnState=" + state.toString() + ", DSFinalStatus=" + dsStatus.toString()
                        + ".");
                return sb.toString();
            }
        } else if (YarnApplicationState.KILLED == state
                || YarnApplicationState.FAILED == state) {
            sb.append('\n').append("Application did not finish."
                    + " YarnState=" + state.toString() + ", DSFinalStatus=" + dsStatus.toString()
                    + ".");
            return sb.toString();
        }
        return sb.toString();
    }

}
