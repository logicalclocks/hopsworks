package se.kth.bbc.yarn;

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FileStatus;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.yarn.api.ApplicationConstants;
import org.apache.hadoop.yarn.api.ApplicationConstants.Environment;
import org.apache.hadoop.yarn.api.records.ApplicationId;
import org.apache.hadoop.yarn.api.records.ApplicationReport;
import org.apache.hadoop.yarn.api.records.ApplicationSubmissionContext;
import org.apache.hadoop.yarn.api.records.ContainerLaunchContext;
import org.apache.hadoop.yarn.api.records.LocalResource;
import org.apache.hadoop.yarn.api.records.LocalResourceType;
import org.apache.hadoop.yarn.api.records.LocalResourceVisibility;
import org.apache.hadoop.yarn.api.records.Resource;
import org.apache.hadoop.yarn.api.records.YarnApplicationState;
import org.apache.hadoop.yarn.client.api.YarnClient;
import org.apache.hadoop.yarn.client.api.YarnClientApplication;
import org.apache.hadoop.yarn.conf.YarnConfiguration;
import org.apache.hadoop.yarn.util.Apps;
import org.apache.hadoop.yarn.util.ConverterUtils;
import org.apache.hadoop.yarn.util.Records;

/**
 * Source:
 * https://github.com/hortonworks/simple-yarn-app/blob/master/src/main/java/com/hortonworks/simpleyarnapp/Client.java
 *
 * @author stig
 */
public class YarnFlinkClient {

    YarnConfiguration conf;

    public YarnFlinkClient() {
        Path confPath = new Path(System.getenv("YARN_CONF_DIR"));
        File confFile = new File(confPath + File.separator + "yarn-site.xml");
        if (!confFile.exists()) {
            System.err.println("Unable to locate configuration file in " + confFile);
            throw new IllegalStateException("No conf file");
        }
        Path yarnPath = new Path(confFile.getAbsolutePath());
        Configuration c = new Configuration();
        c.addResource(yarnPath);
        conf = new YarnConfiguration(c);
    }

    public void run(String command, int n, Path jarPath) throws Exception {
// Create yarnClient
        YarnClient yarnClient = YarnClient.createYarnClient();
        yarnClient.init(conf);
        yarnClient.start();
// Create application via yarnClient
        YarnClientApplication app = yarnClient.createApplication();
// Set up the container launch context for the application master
        ContainerLaunchContext amContainer = Records.newRecord(ContainerLaunchContext.class);
        amContainer.setCommands(
                Collections.singletonList(
                        "$JAVA_HOME/bin/java"
                        + " -Xmx256M"
                        + " se.kth.bbc.yarn.ApplicationMaster"
                        + " " + command
                        + " " + String.valueOf(n)
                        + " 1>" + ApplicationConstants.LOG_DIR_EXPANSION_VAR + "/stdout"
                        + " 2>" + ApplicationConstants.LOG_DIR_EXPANSION_VAR + "/stderr"
                )
        );
// Setup jar for ApplicationMaster
        LocalResource appMasterJar = Records.newRecord(LocalResource.class);

        setupAppMasterJar(jarPath, appMasterJar);

        amContainer.setLocalResources(
                Collections.singletonMap("simpleapp.jar", appMasterJar));
// Setup CLASSPATH for ApplicationMaster
        Map<String, String> appMasterEnv = new HashMap<String, String>();

        setupAppMasterEnv(appMasterEnv);

        amContainer.setEnvironment(appMasterEnv);
// Set up resource type requirements for ApplicationMaster
        Resource capability = Records.newRecord(Resource.class);

        capability.setMemory(
                1200);
        capability.setVirtualCores(
                1);
// Finally, set-up ApplicationSubmissionContext for the application
        ApplicationSubmissionContext appContext
                = app.getApplicationSubmissionContext();

        appContext.setApplicationName(
                "flink-jobmanager"); // application name
        appContext.setAMContainerSpec(amContainer);

        appContext.setResource(capability);

        appContext.setQueue(
                "default"); // queue
// Submit application
        ApplicationId appId = appContext.getApplicationId();

        System.out.println(
                "Submitting application " + appId);
        yarnClient.submitApplication(appContext);
        ApplicationReport appReport = yarnClient.getApplicationReport(appId);
        YarnApplicationState appState = appReport.getYarnApplicationState();
        while (appState != YarnApplicationState.FINISHED
                && appState != YarnApplicationState.KILLED
                && appState != YarnApplicationState.FAILED) {
            Thread.sleep(100);
            appReport = yarnClient.getApplicationReport(appId);
            appState = appReport.getYarnApplicationState();
        }

        System.out.println(
                "Application " + appId + " finished with"
                + " state " + appState
                + " at " + appReport.getFinishTime());
    }

    private void setupAppMasterJar(Path jarPath, LocalResource appMasterJar) throws IOException {
        FileStatus jarStat = FileSystem.get(conf).getFileStatus(jarPath);
        appMasterJar.setResource(ConverterUtils.getYarnUrlFromPath(jarPath));
        appMasterJar.setSize(jarStat.getLen());
        appMasterJar.setTimestamp(jarStat.getModificationTime());
        appMasterJar.setType(LocalResourceType.FILE);
        appMasterJar.setVisibility(LocalResourceVisibility.PUBLIC);
    }

    private void setupAppMasterEnv(Map<String, String> appMasterEnv) {
        for (String c : conf.getStrings(
                YarnConfiguration.YARN_APPLICATION_CLASSPATH,
                YarnConfiguration.DEFAULT_YARN_APPLICATION_CLASSPATH)) {
            Apps.addToEnvironment(appMasterEnv, Environment.CLASSPATH.name(),
                    c.trim());
        }
        Apps.addToEnvironment(appMasterEnv,
                Environment.CLASSPATH.name(),
                Environment.PWD.$() + File.separator + "*");
    }

}
