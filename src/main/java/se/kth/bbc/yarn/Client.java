package se.kth.bbc.yarn;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Vector;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.ejb.EJB;
import org.apache.commons.io.IOUtils;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FSDataOutputStream;
import org.apache.hadoop.fs.FileStatus;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.fs.permission.FsPermission;
import org.apache.hadoop.yarn.api.ApplicationConstants;
import org.apache.hadoop.yarn.api.ApplicationConstants.Environment;
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
public class Client implements Runnable {

    @EJB
    private static EnvironmentVariableFacade envs;

    private static final Logger logger = Logger.getLogger(Client.class.getName());

    private Configuration conf;
    private YarnClient yarnClient;

    private String[] args;

    /*
     * APPLICATIION AND CONTAINER PARAMETERS
     * (+default values)
     */
    // Queue for App master
    private String amQueue = "default";
    // Memory for App master
    private int amMemory = 10;
    //Number of cores for appMaster
    private int amVCores = 1;
    // Application name
    private String appName = "BbcYarn";
    //Location of appmaster jar    
    private String appMasterJarPath;
    //The command to execute
    private String command = "date";
    //The name of the application app master class    
    private String appMasterMainClass = "org.apache.hadoop.yarn.applications.distributedshell.ApplicationMaster";

    //Amount of memory for worker containers
    private int containerMemory = 10;
    //Number of cores for worker containers
    private int containerVirtualCores = 1;
    //Number of worker containers
    private int numContainers = 1;

    //Some hard coded stuff
    private String appMasterJarHdfsPath = "AppMaster.jar";
    private static final String shellCommandPath = "shellCommands";
    private static final String shellArgsPath = "shellArgs";

    //Timeout variables
    // Start time for client
    private final long clientStartTime = System.currentTimeMillis();
    // Timeout threshold for client. Kill app after time interval expires.
    private long clientTimeout = 600000;

    private Client(Configuration conf) {
        this.conf = conf;
        yarnClient = YarnClient.createYarnClient();
        yarnClient.init(conf);
    }

    /**
     * Get an initialized Client.
     *
     * @param args
     * @return
     * @throws Exception
     */
    public static Client getInitializedClient(String appMasterJarPath, String[] args) throws Exception {
        //Get the path to the Yarn configuration file from environment variables
        String yarnConfDir = System.getenv("YARN_CONF_DIR");
        //If not found in environment variables: try DB
        if (yarnConfDir == null) {
            yarnConfDir = envs.getValue("YARN_CONF_DIR");
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
            System.err.println("Unable to locate configuration file in " + confFile);
            throw new IllegalStateException("No conf file");
        }

        //Set the Configuration object for the returned YarnClient
        Path yarnPath = new Path(confFile.getAbsolutePath());
        Configuration c = new Configuration();
        c.addResource(yarnPath);
        Client client = new Client(c);
        client.init(appMasterJarPath, args);
        return client;
    }

    private void init(String jarPath, String[] args) {
        appMasterJarPath = jarPath;
        this.args = args;
    }

    /**
     * Main run function for the client
     *
     * @return true if application completed successfully
     * @throws IOException
     * @throws YarnException
     */
    public void run() {
        try {
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
            ApplicationId appId = appContext.getApplicationId();
        //TODO
            //appContext.setKeepContainersAcrossApplicationAttempts(keepContainers);
            appContext.setApplicationName(appName);
        // set local resources for the application master
            // local files or archives as needed
            // In this scenario, the jar file for the application master is part of the local resources
            Map<String, LocalResource> localResources = new HashMap<>();
            logger.info("Copy App Master jar from local filesystem and add to local environment");
        // Copy the application master jar to the filesystem
            // Create a local resource to point to the destination jar path
            FileSystem fs = FileSystem.get(conf);
            addToLocalResources(fs, appMasterJarPath, appMasterJarHdfsPath, appId.toString(),
                    localResources, null);

            addToLocalResources(fs, null, shellCommandPath, appId.toString(),
                    localResources, command);

        //TODO
        /*
             if (shellArgs.length > 0) {
             addToLocalResources(fs, null, shellArgsPath, appId.toString(),
             localResources, StringUtils.join(shellArgs, " "));
             }*/
            // Set the env variables to be setup in the env where the application master will be run
            logger.info("Set the environment for the application master");
            Map<String, String> env = new HashMap<>();
        // Add AppMaster.jar location to classpath
            // At some point we should not be required to add
            // the hadoop specific classpaths to the env.
            // It should be provided out of the box.
            // For now setting all required classpaths including
            // the classpath to "." for the application jar
            StringBuilder classPathEnv = new StringBuilder(Environment.CLASSPATH.$$())
                    .append(ApplicationConstants.CLASS_PATH_SEPARATOR).append("./*");
            for (String c : conf.getStrings(
                    YarnConfiguration.YARN_APPLICATION_CLASSPATH,
                    YarnConfiguration.DEFAULT_YARN_CROSS_PLATFORM_APPLICATION_CLASSPATH)) {
                classPathEnv.append(ApplicationConstants.CLASS_PATH_SEPARATOR);
                classPathEnv.append(c.trim());
            }
            classPathEnv.append(ApplicationConstants.CLASS_PATH_SEPARATOR).append(
                    "./log4j.properties");
            // add the runtime classpath needed for tests to work
            if (conf.getBoolean(YarnConfiguration.IS_MINI_YARN_CLUSTER, false)) {
                classPathEnv.append(':');
                classPathEnv.append(System.getProperty("java.class.path"));
            }
            env.put("CLASSPATH", classPathEnv.toString());
            // Set the necessary command to execute the application master
            Vector<CharSequence> vargs = new Vector<CharSequence>(30);
            // Set java executable command
            logger.info("Setting up app master command");
            vargs.add(Environment.JAVA_HOME.$$() + "/bin/java");
            // Set Xmx based on am memory size
            vargs.add("-Xmx" + amMemory + "m");
            // Set class name
            vargs.add(appMasterMainClass);
            // Set params for Application Master
            vargs.add("--container_memory " + String.valueOf(containerMemory));
            vargs.add("--container_vcores " + String.valueOf(containerVirtualCores));
            vargs.add("--num_containers " + String.valueOf(numContainers));
            vargs.add("1>" + ApplicationConstants.LOG_DIR_EXPANSION_VAR + "/AppMaster.stdout");
            vargs.add("2>" + ApplicationConstants.LOG_DIR_EXPANSION_VAR + "/AppMaster.stderr");
            // Get final commmand
            StringBuilder amcommand = new StringBuilder();
            for (CharSequence str : vargs) {
                amcommand.append(str).append(" ");
            }
            logger.info("Completed setting up app master command " + amcommand.toString());
            List<String> commands = new ArrayList<>();
            commands.add(amcommand.toString());
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
        // TODO
            // Try submitting the same request again
            // app submission failure?
            // Monitor the application
            monitorApplication(appId);
        } catch (IOException | YarnException e) {

        }
    }

    /**
     * Monitor the submitted application for completion. Kill application if
     * time expires.
     *
     * @param appId Application Id of application to be monitored
     * @return true if application completed successfully
     * @throws YarnException
     * @throws IOException
     */
    private boolean monitorApplication(ApplicationId appId)
            throws YarnException, IOException {
        while (true) {
// Check app status every 1 second.
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                logger.log(Level.INFO, "Thread sleep in monitoring loop interrupted");
            }
// Get application report for the appId we are interested in
            ApplicationReport report = yarnClient.getApplicationReport(appId);
            logger.info("Got application report from ASM for"
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
                    logger.info("Application has completed successfully. Breaking monitoring loop");
                    return true;
                } else {
                    logger.info("Application did finished unsuccessfully."
                            + " YarnState=" + state.toString() + ", DSFinalStatus=" + dsStatus.toString()
                            + ". Breaking monitoring loop");
                    return false;
                }
            } else if (YarnApplicationState.KILLED == state
                    || YarnApplicationState.FAILED == state) {
                logger.info("Application did not finish."
                        + " YarnState=" + state.toString() + ", DSFinalStatus=" + dsStatus.toString()
                        + ". Breaking monitoring loop");
                return false;
            }
            if (System.currentTimeMillis() > (clientStartTime + clientTimeout)) {
                logger.info("Reached client specified timeout for application. Killing application");
                forceKillApplication(appId);
                return false;
            }
        }
    }

    /**
     * Kill a submitted application by sending a call to the ASM
     *
     * @param appId Application Id to be killed.
     * @throws YarnException
     * @throws IOException
     */
    private void forceKillApplication(ApplicationId appId)
            throws YarnException, IOException {
        // TODO clarify whether multiple jobs with the same app id can be submitted and be running at
        // the same time.
        // If yes, can we kill a particular attempt only?
        // Response can be ignored as it is non-null on success or
        // throws an exception in case of failures
        yarnClient.killApplication(appId);
    }

    private void addToLocalResources(FileSystem fs, String fileSrcPath,
            String fileDstPath, String appId, Map<String, LocalResource> localResources,
            String resources) throws IOException {
        String suffix
                = appName + "/" + appId + "/" + fileDstPath;
        Path dst
                = new Path(fs.getHomeDirectory(), suffix);
        if (fileSrcPath == null) {
            FSDataOutputStream ostream = null;
            try {
                ostream = FileSystem
                        .create(fs, dst, new FsPermission((short) 0710));
                ostream.writeUTF(resources);
            } finally {
                IOUtils.closeQuietly(ostream);
            }
        } else {
            fs.copyFromLocalFile(new Path(fileSrcPath), dst);
        }
        FileStatus scFileStatus = fs.getFileStatus(dst);
        LocalResource scRsrc
                = LocalResource.newInstance(
                        ConverterUtils.getYarnUrlFromURI(dst.toUri()),
                        LocalResourceType.FILE, LocalResourceVisibility.APPLICATION,
                        scFileStatus.getLen(), scFileStatus.getModificationTime());
        localResources.put(fileDstPath, scRsrc);
    }

}
