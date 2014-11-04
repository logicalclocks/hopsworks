package se.kth.bbc.yarn;

import de.huberlin.wbi.hiway.app.HiWayConfiguration;
import de.huberlin.wbi.hiway.common.Data;
import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Vector;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.GnuParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.DataOutputBuffer;
import org.apache.hadoop.security.Credentials;
import org.apache.hadoop.security.UserGroupInformation;
import org.apache.hadoop.security.token.Token;
import org.apache.hadoop.yarn.api.ApplicationConstants;
import org.apache.hadoop.yarn.api.ApplicationConstants.Environment;
import org.apache.hadoop.yarn.api.protocolrecords.GetNewApplicationResponse;
import org.apache.hadoop.yarn.api.records.ApplicationId;
import org.apache.hadoop.yarn.api.records.ApplicationReport;
import org.apache.hadoop.yarn.api.records.ApplicationSubmissionContext;
import org.apache.hadoop.yarn.api.records.ContainerLaunchContext;
import org.apache.hadoop.yarn.api.records.FinalApplicationStatus;
import org.apache.hadoop.yarn.api.records.LocalResource;
import org.apache.hadoop.yarn.api.records.NodeReport;
import org.apache.hadoop.yarn.api.records.NodeState;
import org.apache.hadoop.yarn.api.records.Priority;
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
import org.apache.hadoop.yarn.util.Records;
import static se.kth.bbc.fileoperations.FileSystemOperations.nameNodeURI;

public class Client {
    private static final Logger logger = Logger.getLogger(Client.class.getName());
    
    public static Client getInitiatedClient(String[] args) throws Exception{
        Path confPath = new Path(System.getenv("YARN_CONF_DIR"));
        File confFile = new File(confPath + File.separator + "yarn-site.xml");
        if (!confFile.exists()) {
            System.err.println("Unable to locate configuration file in " + confFile);
            throw new IllegalStateException("No conf file");
        }
        Path yarnPath = new Path(confFile.getAbsolutePath());
        Configuration c = new Configuration();
        c.addResource(yarnPath);
        Client client = new Client(c);
        boolean ok = client.init(args);
        if(ok){
            return client;
        }else{
            return null;
        }
    }

    // amount of memory resource to request for to run the App Master
    private int amMemory = 4096;
    // the priority of the AM container
    private int amPriority = 0;
    // the queue to which this application is to be submitted in the RM
    private String amQueue = "";
    // start time for client
    private final long clientStartTime = System.currentTimeMillis();
    // timeout threshold for client. Kill app after time interval expires.
    private long clientTimeout;
    // the configuration of the Hadoop installation
    private Configuration conf;
    // debug flag
    boolean debugFlag = false;
    private Configuration hiWayConf;
    // command line options
    private Options opts;
    private String sandboxDir;
    private Data summary;
    // the workflow format and its path in the file system
    private Data workflow;
    private HiWayConfiguration.HIWAY_WORKFLOW_LANGUAGE_OPTS workflowType;
    // a handle to the YARN ApplicationsManager (ASM)
    private YarnClient yarnClient;

    public Client() throws Exception {
        this(new YarnConfiguration());
        conf.addResource("core-site.xml");        
        conf.set("fs.defaultFS", nameNodeURI);
    }

    public Client(Configuration conf) {
        this.conf = conf;        
        this.conf.set("fs.defaultFS", nameNodeURI);
        hiWayConf = new HiWayConfiguration();
        yarnClient = YarnClient.createYarnClient();
        yarnClient.init(conf);
        opts = new Options();
        opts.addOption("s", "summary", true, "The name of the json summary file. No file is created if this parameter is not specified.");
        opts.addOption("w", "workflow", true, "The workflow file to be executed by the Application Master");
        String workflowFormats = "";
        for (HiWayConfiguration.HIWAY_WORKFLOW_LANGUAGE_OPTS language : HiWayConfiguration.HIWAY_WORKFLOW_LANGUAGE_OPTS.values()) {
            workflowFormats += ", " + language.toString();
        }
        opts.addOption("l", "language", true, "The input file format. Valid arguments: " + workflowFormats.substring(2) + ". Default: "
                + HiWayConfiguration.HIWAY_WORKFLOW_LANGUAGE_OPTS.cuneiform);
        opts.addOption("debug", false, "Dump out debug information");
        opts.addOption("help", false, "Print usage");
    }

    /**
     * Kill a submitted application by sending a call to the ASM.
     *
     * @param appId Application Id to be killed.
     *
     * @throws YarnException
     * @throws IOException
     */
    private void forceKillApplication(ApplicationId appId) throws YarnException, IOException {
        // Response can be ignored as it is non-null on success or throws an exception in case of failures
        yarnClient.killApplication(appId);
    }

    /**
     * Parse command line options.
     *
     * @param args Parsed command line options.
     * @return Whether the init was successful to run the client.
     * @throws ParseException
     */
    public boolean init(String[] args) throws ParseException {

        CommandLine cliParser = new GnuParser().parse(opts, args);

        if (args.length == 0) {
            throw new IllegalArgumentException("No args specified for client to initialize");
        }

        if (cliParser.hasOption("help")) {
            printUsage();
            return false;
        }

        if (cliParser.hasOption("debug")) {
            debugFlag = true;
        }

        amPriority = hiWayConf.getInt(HiWayConfiguration.HIWAY_AM_PRIORITY, HiWayConfiguration.HIWAY_AM_PRIORITY_DEFAULT);
        amQueue = hiWayConf.get(HiWayConfiguration.HIWAY_AM_QUEUE, HiWayConfiguration.HIWAY_AM_QUEUE_DEFAULT);
        amMemory = hiWayConf.getInt(HiWayConfiguration.HIWAY_AM_MEMORY, HiWayConfiguration.HIWAY_AM_MEMORY_DEFAULT);
        if (amMemory < 0) {
            throw new IllegalArgumentException("Invalid memory specified for application master, exiting." + " Specified memory=" + amMemory);
        }

        if (cliParser.hasOption("summary")) {
            String summaryFile = cliParser.getOptionValue("summary");
            try {
                summary = new Data((new File(summaryFile)).getCanonicalPath());
            } catch (IOException e) {
                logger.log(Level.SEVERE,e.getLocalizedMessage());
                e.printStackTrace();
            }
        }

        String workflowPath = cliParser.getOptionValue("workflow");
        try {
            workflow = new Data((new File(workflowPath)).getCanonicalPath());
        } catch (IOException e) {
                logger.log(Level.SEVERE,e.getLocalizedMessage());
                e.printStackTrace();
        }
        workflowType = HiWayConfiguration.HIWAY_WORKFLOW_LANGUAGE_OPTS.valueOf(cliParser.getOptionValue("language",
                HiWayConfiguration.HIWAY_WORKFLOW_LANGUAGE_OPTS.cuneiform.toString()));

        clientTimeout = hiWayConf.getInt(HiWayConfiguration.HIWAY_CLIENT_TIMEOUT, HiWayConfiguration.HIWAY_CLIENT_TIMEOUT_DEFAULT) * 1000;

        return true;
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
    private boolean monitorApplication(ApplicationId appId) throws YarnException, IOException {
        while (true) {
            // Check app status every 1 second.
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                logger.log(Level.INFO,"Thread sleep in monitoring loop interrupted");
            }

            // Get application report for the appId we are interested in
            ApplicationReport report = yarnClient.getApplicationReport(appId);

            YarnApplicationState state = report.getYarnApplicationState();
            FinalApplicationStatus dsStatus = report.getFinalApplicationStatus();
            if (YarnApplicationState.FINISHED == state) {
                if (FinalApplicationStatus.SUCCEEDED == dsStatus) {
                    logger.info("Application has completed successfully. Breaking monitoring loop");
                    logger.info(report.getDiagnostics());
                    return true;
                } else {
                    logger.info("Application did finish unsuccessfully." + " YarnState=" + state.toString() + ", DSFinalStatus=" + dsStatus.toString()
                            + ". Breaking monitoring loop");
                    return false;
                }
            } else if (YarnApplicationState.KILLED == state || YarnApplicationState.FAILED == state) {
                logger.info("Application did not finish." + " YarnState=" + state.toString() + ", DSFinalStatus=" + dsStatus.toString()
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
     * Helper function to print out usage.
     */
    private void printUsage() {
        new HelpFormatter().printHelp("Client", opts);
    }

    /**
     * Main run function for the client.
     *
     * @return true if application completed successfully.
     * @throws IOException
     * @throws YarnException
     */
    public boolean run() throws IOException, YarnException {
        logger.info("Running Client");
        yarnClient.start();

        YarnClusterMetrics clusterMetrics = yarnClient.getYarnClusterMetrics();
        logger.info("Got Cluster metric info from ASM" + ", numNodeManagers=" + clusterMetrics.getNumNodeManagers());

        List<NodeReport> clusterNodeReports = yarnClient.getNodeReports(NodeState.RUNNING);
        logger.info("Got Cluster node info from ASM");
        for (NodeReport node : clusterNodeReports) {
            logger.info("Got node report from ASM for" + ", nodeId=" + node.getNodeId() + ", nodeAddress" + node.getHttpAddress() + ", nodeRackName"
                    + node.getRackName() + ", nodeNumContainers" + node.getNumContainers());
        }

        QueueInfo queueInfo = yarnClient.getQueueInfo(this.amQueue);
        logger.info("Queue info" + ", queueName=" + queueInfo.getQueueName() + ", queueCurrentCapacity=" + queueInfo.getCurrentCapacity() + ", queueMaxCapacity="
                + queueInfo.getMaximumCapacity() + ", queueApplicationCount=" + queueInfo.getApplications().size() + ", queueChildQueueCount="
                + queueInfo.getChildQueues().size());

        List<QueueUserACLInfo> listAclInfo = yarnClient.getQueueAclsInfo();
        for (QueueUserACLInfo aclInfo : listAclInfo) {
            for (QueueACL userAcl : aclInfo.getUserAcls()) {
                logger.info("User ACL Info for Queue" + ", queueName=" + aclInfo.getQueueName() + ", userAcl=" + userAcl.name());
            }
        }

        // Get a new application id
        YarnClientApplication app = yarnClient.createApplication();
        GetNewApplicationResponse appResponse = app.getNewApplicationResponse();

        // Get min/max resource capabilities from RM and change memory ask if needed
        int maxMem = appResponse.getMaximumResourceCapability().getMemory();
        logger.info("Max mem capabililty of resources in this cluster " + maxMem);

        // A resource ask cannot exceed the max.
        if (amMemory > maxMem) {
            logger.info("AM memory specified above max threshold of cluster. Using max value." + ", specified=" + amMemory + ", max=" + maxMem);
            amMemory = maxMem;
        }

        // set the application name
        ApplicationSubmissionContext appContext = app.getApplicationSubmissionContext();
        appContext.setApplicationType(hiWayConf.get(HiWayConfiguration.HIWAY_AM_APPLICATION_TYPE, HiWayConfiguration.HIWAY_AM_APPLICATION_TYPE_DEFAULT));
        appContext.setApplicationName("run " + workflow.getName() + " (type: " + workflowType.toString() + ")");
        ApplicationId appId = appContext.getApplicationId();
        sandboxDir = hiWayConf.get(HiWayConfiguration.HIWAY_AM_SANDBOX_DIRECTORY, HiWayConfiguration.HIWAY_AM_SANDBOX_DIRECTORY_DEFAULT);
        Data.setHdfsDirectoryPrefix(sandboxDir + "/" + appId);

        // Set up the container launch context for the application master
        ContainerLaunchContext amContainer = Records.newRecord(ContainerLaunchContext.class);

        // set local resources for the application master
        Map<String, LocalResource> localResources = new HashMap<String, LocalResource>();

        // Copy the application master jar to the filesystem
        logger.info("Copy App Master jar from local filesystem and add to local environment");
        FileSystem fs = FileSystem.get(conf);

        workflow.stageOut(fs, "");

        // set local resource info into app master container launch context
        amContainer.setLocalResources(localResources);

        /* set the env variables to be setup in the env where the application master will be run */
        logger.info("Set the environment for the application master");
        Map<String, String> env = new HashMap<String, String>();

        StringBuilder classPathEnv = new StringBuilder(Environment.CLASSPATH.$()).append(File.pathSeparatorChar).append("./*");
        for (String c : conf.getStrings(YarnConfiguration.YARN_APPLICATION_CLASSPATH, YarnConfiguration.DEFAULT_YARN_APPLICATION_CLASSPATH)) {
            classPathEnv.append(':');
            classPathEnv.append(File.pathSeparatorChar);
            classPathEnv.append(c.trim());
        }

        if (conf.getBoolean(YarnConfiguration.IS_MINI_YARN_CLUSTER, false)) {
            classPathEnv.append(':');
            classPathEnv.append(System.getProperty("java.class.path"));
        }

        env.put("CLASSPATH", classPathEnv.toString());

        amContainer.setEnvironment(env);

        // Set the necessary command to execute the application master
        Vector<CharSequence> vargs = new Vector<CharSequence>(30);

        // Set java executable command
        logger.info("Setting up app master command");
        vargs.add(Environment.JAVA_HOME.$() + "/bin/java");
        // Set Xmx based on am memory size
        vargs.add("-Xmx" + amMemory + "m");
		// Set class name

        switch (workflowType) {
            case dax:
                vargs.add(HiWayConfiguration.HIWAY_WORKFLOW_LANGUAGE_DAX_CLASS);
                break;
            default:
                vargs.add(HiWayConfiguration.HIWAY_WORKFLOW_LANGUAGE_CUNEIFORM_CLASS);
        }

        vargs.add("--workflow " + workflow.getLocalPath());
        if (summary != null) {
            vargs.add("--summary " + summary.getLocalPath());
        }
        vargs.add("--appid " + appId.toString());

        if (debugFlag) {
            vargs.add("--debug");
        }

        vargs.add("1>&1 | tee AppMaster.stdout > " + ApplicationConstants.LOG_DIR_EXPANSION_VAR + "/AppMaster.stdout");
        vargs.add("2>&2 | tee AppMaster.stderr > " + ApplicationConstants.LOG_DIR_EXPANSION_VAR + "/AppMaster.stderr");

        // Get final command
        StringBuilder command = new StringBuilder();
        for (CharSequence str : vargs) {
            command.append(str).append(" ");
        }

        logger.info("Completed setting up app master command " + command.toString());
        List<String> commands = new ArrayList<String>();
        commands.add(command.toString());
        amContainer.setCommands(commands);

        // Set up resource type requirements
        Resource capability = Records.newRecord(Resource.class);
        capability.setMemory(amMemory);
        appContext.setResource(capability);

        // Setup security tokens
        if (UserGroupInformation.isSecurityEnabled()) {
            Credentials credentials = new Credentials();
            String tokenRenewer = conf.get(YarnConfiguration.RM_PRINCIPAL);
            if (tokenRenewer == null || tokenRenewer.length() == 0) {
                throw new IOException("Can't get Master Kerberos principal for the RM to use as renewer");
            }

            // For now, only getting tokens for the default file-system.
            final Token<?> tokens[] = fs.addDelegationTokens(tokenRenewer, credentials);
            if (tokens != null) {
                for (Token<?> token : tokens) {
                    logger.info("Got dt for " + fs.getUri() + "; " + token);
                }
            }
            DataOutputBuffer dob = new DataOutputBuffer();
            credentials.writeTokenStorageToStream(dob);
            ByteBuffer fsTokens = ByteBuffer.wrap(dob.getData(), 0, dob.getLength());
            amContainer.setTokens(fsTokens);
        }

        appContext.setAMContainerSpec(amContainer);

        // Set the priority for the application master
        Priority pri = Records.newRecord(Priority.class);
        pri.setPriority(amPriority);
        appContext.setPriority(pri);

        // Set the queue to which this application is to be submitted in the RM
        appContext.setQueue(amQueue);

        // Submit the application to the applications manager
        logger.info("Submitting application to ASM");
        yarnClient.submitApplication(appContext);

        // Monitor the application
        boolean success = monitorApplication(appId);

        if (summary != null) {
            summary.stageIn(fs, "");
        }

        return success;

    }

}
