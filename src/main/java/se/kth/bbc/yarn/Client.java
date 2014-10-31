package se.kth.bbc.yarn;

//import java.io.BufferedReader;
//import java.io.File;
//import java.io.FileNotFoundException;
//import java.io.FileOutputStream;
//import java.io.FilenameFilter;
//import java.io.IOException;
//import java.io.InputStream;
//import java.io.InputStreamReader;
//import java.io.OutputStream;
//import java.net.InetSocketAddress;
//import java.util.ArrayList;
//import java.util.Arrays;
//import java.util.Collections;
//import java.util.HashMap;
//import java.util.List;
//import java.util.Map;
//import java.util.Properties;
//import java.util.jar.JarFile;
//
//import org.apache.commons.cli.CommandLine;
//import org.apache.commons.cli.CommandLineParser;
//import org.apache.commons.cli.HelpFormatter;
//import org.apache.commons.cli.MissingOptionException;
//import org.apache.commons.cli.Option;
//import org.apache.commons.cli.Options;
//import org.apache.commons.cli.PosixParser;
//import org.apache.commons.lang3.StringUtils;
//import org.slf4j.Logger;
//import org.slf4j.LoggerFactory;
//import org.apache.hadoop.conf.Configuration;
//import org.apache.hadoop.fs.FileSystem;
//import org.apache.hadoop.fs.Path;
//import org.apache.hadoop.fs.permission.FsAction;
//import org.apache.hadoop.fs.permission.FsPermission;
//import org.apache.hadoop.security.UserGroupInformation;
//import org.apache.hadoop.yarn.api.ApplicationConstants;
//import org.apache.hadoop.yarn.api.protocolrecords.GetNewApplicationResponse;
//import org.apache.hadoop.yarn.api.records.ApplicationId;
//import org.apache.hadoop.yarn.api.records.ApplicationReport;
//import org.apache.hadoop.yarn.api.records.ApplicationSubmissionContext;
//import org.apache.hadoop.yarn.api.records.ContainerLaunchContext;
//import org.apache.hadoop.yarn.api.records.LocalResource;
//import org.apache.hadoop.yarn.api.records.NodeReport;
//import org.apache.hadoop.yarn.api.records.NodeState;
//import org.apache.hadoop.yarn.api.records.QueueInfo;
//import org.apache.hadoop.yarn.api.records.Resource;
//import org.apache.hadoop.yarn.api.records.YarnApplicationState;
//import org.apache.hadoop.yarn.api.records.YarnClusterMetrics;
//import org.apache.hadoop.yarn.client.api.YarnClient;
//import org.apache.hadoop.yarn.client.api.YarnClientApplication;
//import org.apache.hadoop.yarn.exceptions.YarnException;
//import org.apache.hadoop.yarn.util.Records;

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
public class Client {/*
	private static final Logger LOG = LoggerFactory.getLogger(Client.class);
*/
	/**
	 * Constants,
	 * all starting with ENV_ are used as environment variables to pass values from the Client
	 * to the Application Master.
	 */
	public final static String ENV_TM_MEMORY = "_CLIENT_TM_MEMORY";
	public final static String ENV_TM_CORES = "_CLIENT_TM_CORES";
	public final static String ENV_TM_COUNT = "_CLIENT_TM_COUNT";
	public final static String ENV_APP_ID = "_APP_ID";
	public final static String ENV_APP_NUMBER = "_APP_NUMBER";
	public final static String FLINK_JAR_PATH = "_FLINK_JAR_PATH"; // the Flink jar resource location (in HDFS).
	public static final String ENV_CLIENT_HOME_DIR = "_CLIENT_HOME_DIR";
	public static final String ENV_CLIENT_SHIP_FILES = "_CLIENT_SHIP_FILES";
	public static final String ENV_CLIENT_USERNAME = "_CLIENT_USERNAME";
	public static final String ENV_AM_PRC_PORT = "_AM_PRC_PORT";
	public static final String ENV_SLOTS = "_SLOTS";
	public static final String ENV_DYNAMIC_PROPERTIES = "_DYNAMIC_PROPERTIES";

	private static final String CONFIG_FILE_NAME = "flink-conf.yaml";
	
	/**
	 * Seconds to wait between each status query to the AM.
	 */
	private static final int CLIENT_POLLING_INTERVALL = 3;
	/**
	 * Minimum memory requirements, checked by the Client.
	 *//*
	private static final int MIN_JM_MEMORY = 128;
	private static final int MIN_TM_MEMORY = 128;

	private Configuration conf;
	private YarnClient yarnClient;

	private ClientMasterControl cmc;

	private File yarnPropertiesFile;
*/
	/**
	 * Files (usually in a distributed file system) used for the YARN session of Flink.
	 * Contains configuration files and jar files.
	 *//*
	private Path sessionFilesDir;

	/**
	 * If the user has specified a different number of slots, we store them here
	 *//*
	private int slots = -1;
	
	public void run(String[] args) throws Exception {
*/
	




		

		/*

		
		
		// Setup jar for ApplicationMaster
		LocalResource appMasterJar = Records.newRecord(LocalResource.class);
		LocalResource flinkConf = Records.newRecord(LocalResource.class);
		Path remotePathJar = Utils.setupLocalResource(conf, fs, appId.toString(), localJarPath, appMasterJar, fs.getHomeDirectory());
		Path remotePathConf = Utils.setupLocalResource(conf, fs, appId.toString(), confPath, flinkConf, fs.getHomeDirectory());
		Map<String, LocalResource> localResources = new HashMap<String, LocalResource>(2);
		localResources.put("flink.jar", appMasterJar);
		localResources.put("flink-conf.yaml", flinkConf);


		// setup security tokens (code from apache storm)
		final Path[] paths = new Path[3 + shipFiles.size()];
		StringBuffer envShipFileList = new StringBuffer();
		// upload ship files
		for (int i = 0; i < shipFiles.size(); i++) {
			File shipFile = shipFiles.get(i);
			LocalResource shipResources = Records.newRecord(LocalResource.class);
			Path shipLocalPath = new Path("file://" + shipFile.getAbsolutePath());
			paths[3 + i] = Utils.setupLocalResource(conf, fs, appId.toString(),
					shipLocalPath, shipResources, fs.getHomeDirectory());
			localResources.put(shipFile.getName(), shipResources);

			envShipFileList.append(paths[3 + i]);
			if(i+1 < shipFiles.size()) {
				envShipFileList.append(',');
			}
		}

		paths[0] = remotePathJar;
		paths[1] = remotePathConf;
		sessionFilesDir = new Path(fs.getHomeDirectory(), ".flink/" + appId.toString() + "/");
		FsPermission permission = new FsPermission(FsAction.ALL, FsAction.ALL, FsAction.ALL);
		fs.setPermission(sessionFilesDir, permission); // set permission for path.
		Utils.setTokensFor(amContainer, paths, this.conf);


		amContainer.setLocalResources(localResources);
		fs.close();

		int amRPCPort = GlobalConfiguration.getInteger(ConfigConstants.YARN_AM_PRC_PORT, ConfigConstants.DEFAULT_YARN_AM_RPC_PORT);
		amRPCPort = Utils.offsetPort(amRPCPort, appNumber);
		// Setup CLASSPATH for ApplicationMaster
		Map<String, String> appMasterEnv = new HashMap<String, String>();
		Utils.setupEnv(conf, appMasterEnv);
		// set configuration values
		appMasterEnv.put(Client.ENV_TM_COUNT, String.valueOf(taskManagerCount));
		appMasterEnv.put(Client.ENV_TM_CORES, String.valueOf(tmCores));
		appMasterEnv.put(Client.ENV_TM_MEMORY, String.valueOf(tmMemory));
		appMasterEnv.put(Client.FLINK_JAR_PATH, remotePathJar.toString() );
		appMasterEnv.put(Client.ENV_APP_ID, appId.toString());
		appMasterEnv.put(Client.ENV_CLIENT_HOME_DIR, fs.getHomeDirectory().toString());
		appMasterEnv.put(Client.ENV_CLIENT_SHIP_FILES, envShipFileList.toString() );
		appMasterEnv.put(Client.ENV_CLIENT_USERNAME, UserGroupInformation.getCurrentUser().getShortUserName());
		appMasterEnv.put(Client.ENV_AM_PRC_PORT, String.valueOf(amRPCPort));
		appMasterEnv.put(Client.ENV_SLOTS, String.valueOf(slots));
		appMasterEnv.put(Client.ENV_APP_NUMBER, String.valueOf(appNumber));
		if(dynamicPropertiesEncoded != null) {
			appMasterEnv.put(Client.ENV_DYNAMIC_PROPERTIES, dynamicPropertiesEncoded);
		}

		amContainer.setEnvironment(appMasterEnv);

		// Set up resource type requirements for ApplicationMaster
		Resource capability = Records.newRecord(Resource.class);
		capability.setMemory(jmMemory);
		capability.setVirtualCores(1);

		appContext.setApplicationName("Flink"); // application name
		appContext.setAMContainerSpec(amContainer);
		appContext.setResource(capability);
		appContext.setQueue(queue);

		// file that we write into the conf/ dir containing the jobManager address and the dop.
		yarnPropertiesFile = new File(confDirPath + CliFrontend.YARN_PROPERTIES_FILE);


		LOG.info("Submitting application master " + appId);
		yarnClient.submitApplication(appContext);
		ApplicationReport appReport = yarnClient.getApplicationReport(appId);
		YarnApplicationState appState = appReport.getYarnApplicationState();
		boolean told = false;
		char[] el = { '/', '|', '\\', '-'};
		int i = 0;
		int numTaskmanagers = 0;
		int numMessages = 0;

		BufferedReader in = new BufferedReader(new InputStreamReader(System.in));

		while (appState != YarnApplicationState.FINISHED
				&& appState != YarnApplicationState.KILLED
				&& appState != YarnApplicationState.FAILED) {
			if(!told && appState ==  YarnApplicationState.RUNNING) {
				System.err.println("Flink JobManager is now running on "+appReport.getHost()+":"+jmPort);
				System.err.println("JobManager Web Interface: "+appReport.getTrackingUrl());
				// write jobmanager connect information
				Properties yarnProps = new Properties();
				yarnProps.setProperty(CliFrontend.YARN_PROPERTIES_JOBMANAGER_KEY, appReport.getHost()+":"+jmPort);
				if(slots != -1) {
					yarnProps.setProperty(CliFrontend.YARN_PROPERTIES_DOP, Integer.toString(slots * taskManagerCount) );
				}
				// add dynamic properties
				if(dynamicProperties != null) {
					yarnProps.setProperty(CliFrontend.YARN_PROPERTIES_DYNAMIC_PROPERTIES_STRING, dynamicPropertiesEncoded);
				}
				OutputStream out = new FileOutputStream(yarnPropertiesFile);
				yarnProps.store(out, "Generated YARN properties file");
				out.close();
				yarnPropertiesFile.setReadable(true, false); // readable for all.

				// connect RPC service
				cmc = new ClientMasterControl(new InetSocketAddress(appReport.getHost(), amRPCPort));
				cmc.start();
				Runtime.getRuntime().addShutdownHook(new ClientShutdownHook());
				told = true;
			}
			if(!told) {
				System.err.print(el[i++]+"\r");
				if(i == el.length) {
					i = 0;
				}
				Thread.sleep(500); // wait for the application to switch to RUNNING
			} else {
				int newTmCount = cmc.getNumberOfTaskManagers();
				if(numTaskmanagers != newTmCount) {
					System.err.println("Number of connected TaskManagers changed to "+newTmCount+". "
							+ "Slots available: "+cmc.getNumberOfAvailableSlots());
					numTaskmanagers = newTmCount;
				}
				// we also need to show new messages.
				if(cmc.getFailedStatus()) {
					System.err.println("The Application Master failed!\nMessages:\n");
					for(Message m: cmc.getMessages() ) {
						System.err.println("Message: "+m.getMessage());
					}
					System.err.println("Requesting Application Master shutdown");
					cmc.shutdownAM();
					cmc.close();
					System.err.println("Application Master closed.");
				}
				if(cmc.getMessages().size() != numMessages) {
					System.err.println("Received new message(s) from the Application Master");
					List<Message> msg = cmc.getMessages();
					while(msg.size() > numMessages) {
						System.err.println("Message: "+msg.get(numMessages).getMessage());
						numMessages++;
					}
				}

				// wait until CLIENT_POLLING_INTERVALL is over or the user entered something.
				long startTime = System.currentTimeMillis();
				while ((System.currentTimeMillis() - startTime) < CLIENT_POLLING_INTERVALL * 1000
						&& !in.ready()) {
					Thread.sleep(200);
				}
				if (in.ready()) {
					String command = in.readLine();
					evalCommand(command);
				}

			}

			appReport = yarnClient.getApplicationReport(appId);
			appState = appReport.getYarnApplicationState();
		}

		LOG.info("Application " + appId + " finished with"
				+ " state " + appState + " and "
				+ "final state " + appReport.getFinalApplicationStatus() + " at " + appReport.getFinishTime());

		if(appState == YarnApplicationState.FAILED || appState == YarnApplicationState.KILLED ) {
			LOG.warn("Application failed. Diagnostics "+appReport.getDiagnostics());
			LOG.warn("If log aggregation is activated in the Hadoop cluster, we recommend to retreive "
					+ "the full application log using this command:\n"
					+ "\tyarn logs -applicationId "+appReport.getApplicationId()+"\n"
					+ "(It sometimes takes a few seconds until the logs are aggregated)");
		}

	}

	private void printHelp() {
		System.err.println("Available commands:\n"
				+ "\t stop : Stop the YARN session\n"
				+ "\t allmsg : Show all messages\n");
	}
	private void evalCommand(String command) {
		if(command.equals("help")) {
			printHelp();
		} else if(command.equals("stop") || command.equals("quit") || command.equals("exit")) {
			stopSession();
			System.exit(0);
		} else if(command.equals("allmsg")) {
			System.err.println("All messages from the ApplicationMaster:");
			for(Message m: cmc.getMessages() ) {
				System.err.println("Message: "+m.getMessage());
			}
		} else if(command.startsWith("add")) {
			System.err.println("This feature is not implemented yet!");
//			String nStr = command.replace("add", "").trim();
//			int n = Integer.valueOf(nStr);
//			System.err.println("Adding "+n+" TaskManagers to the session");
//			cmc.addTaskManagers(n);
		} else {
			System.err.println("Unknown command '"+command+"'");
			printHelp();
		}
	}

	private void cleanUp() throws IOException {
		LOG.info("Deleting files in "+sessionFilesDir );
		FileSystem shutFS = FileSystem.get(conf);
		shutFS.delete(sessionFilesDir, true); // delete conf and jar file.
		shutFS.close();
	}
	
	private void stopSession() {
		try {
			LOG.info("Sending shutdown request to the Application Master");
			cmc.shutdownAM();
			cleanUp();
			cmc.close();
		} catch (Exception e) {
			LOG.warn("Exception while killing the YARN application", e);
		}
		try {
			yarnPropertiesFile.delete();
		} catch (Exception e) {
			LOG.warn("Exception while deleting the JobManager address file", e);
		}
		LOG.info("YARN Client is shutting down");
		yarnClient.stop();
	}

	public class ClientShutdownHook extends Thread {
		@Override
		public void run() {
			stopSession();
		}*/
	}



