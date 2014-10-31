package se.kth.bbc.yarn;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Collections;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.yarn.api.ApplicationConstants;
import org.apache.hadoop.yarn.api.protocolrecords.GetNewApplicationResponse;
import org.apache.hadoop.yarn.api.records.ApplicationId;
import org.apache.hadoop.yarn.api.records.ApplicationSubmissionContext;
import org.apache.hadoop.yarn.api.records.ContainerLaunchContext;
import org.apache.hadoop.yarn.api.records.NodeReport;
import org.apache.hadoop.yarn.api.records.NodeState;
import org.apache.hadoop.yarn.api.records.Resource;
import org.apache.hadoop.yarn.client.api.YarnClient;
import org.apache.hadoop.yarn.client.api.YarnClientApplication;
import org.apache.hadoop.yarn.conf.YarnConfiguration;
import org.apache.hadoop.yarn.exceptions.YarnException;
import org.apache.hadoop.yarn.util.Records;

/**
 *
 * @author stig
 */
public class YarnJob {

    //Path to the jar file containing the YARN application.
    private Path jarFile = null;
    private Configuration conf;
    private int containerCount = 0;
    private YarnClient yarnClient;
    private int jmMemory = 512;
    private int tmMemory = 1024;
    private int tmCores = 1;

    public void submit() throws YarnException, IOException {

        //Check if jar has been set
        if (jarFile == null) {
            //TODO: no idea what this does, replace it...
            jarFile = new Path("file://" + Client.class.getProtectionDomain().getCodeSource().getLocation().getPath());
        }

        //set configuration
        conf = initializeYarnConfiguration();

        yarnClient = YarnClient.createYarnClient();
        yarnClient.init(conf);
        yarnClient.start();

        if (containerCount == 0) {
            //TODO: add error message, add sensible defaults
            System.out.println("ERROR: taskManagerCount cannot be 0");
            return;
        }

        YarnClientApplication app = yarnClient.createApplication();
        GetNewApplicationResponse appResponse = app.getNewApplicationResponse();
        boolean noProb = checkResources(appResponse);
        if (!noProb) {
            yarnClient.stop();
            return;
        }

        // Set up the container launch context for the application master
        ContainerLaunchContext amContainer = Records.newRecord(ContainerLaunchContext.class);

        String amCommand = "$JAVA_HOME/bin/java"
                + " -Xmx256M ";

        amCommand += " " + ApplicationMasterFlink.class.getName() + " "
                + " 1>"
                + ApplicationConstants.LOG_DIR_EXPANSION_VAR + "/jobmanager-stdout.log"
                + " 2>" + ApplicationConstants.LOG_DIR_EXPANSION_VAR + "/jobmanager-stderr.log";
        amContainer.setCommands(Collections.singletonList(amCommand));

        // Set-up ApplicationSubmissionContext for the application
        ApplicationSubmissionContext appContext = app.getApplicationSubmissionContext();
        final ApplicationId appId = appContext.getApplicationId();


    }

    public void setJarPath(Path path) {
        this.jarFile = path;
    }

    public Path getJarPath() {
        return jarFile;
    }

    public static Configuration initializeYarnConfiguration() {
        Configuration conf = new YarnConfiguration();

        String[] envs = {"YARN_CONF_DIR", "HADOOP_CONF_DIR"};
        for (int i = 0; i < envs.length; ++i) {
            String confPath = System.getenv(envs[i]);
            if (confPath != null) {
                addPathToConfig(conf, new File(confPath));
                setDefaultConfValues(conf);
                return conf;
            }
        }
        return null;
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

    public void setContainerCount(int i) {
        this.containerCount = i;
    }

    public int getContainerCount() {
        return containerCount;
    }

    public int getJmMemory() {
        return jmMemory;
    }

    public void setJmMemory(int jmMemory) {
        this.jmMemory = jmMemory;
    }

    public int getTmMemory() {
        return tmMemory;
    }

    public void setTmMemory(int tmMemory) {
        this.tmMemory = tmMemory;
    }

    public int getTmCores() {
        return tmCores;
    }

    public void setTmCores(int tmCores) {
        this.tmCores = tmCores;
    }

    /**
     * Check if enough resources are available.
     *
     * @return True if enough resources are available, false otherwise.
     */
    private boolean checkResources(GetNewApplicationResponse appResponse) {
        try {
            Resource maxRes = appResponse.getMaximumResourceCapability();
            if (tmMemory > maxRes.getMemory() || tmCores > maxRes.getVirtualCores()) {
                //TODO: add real log message
                System.out.println("The cluster does not have the requested resources for the TaskManagers available!\n"
                        + "Maximum Memory: " + maxRes.getMemory() + ", Maximum Cores: " + tmCores);
                return false;
            }
            if (jmMemory > maxRes.getMemory()) {
                //TODO: add real log message
                System.out.println("The cluster does not have the requested resources for the JobManager available!\n"
                        + "Maximum Memory: " + maxRes.getMemory());
                return false;
            }
            int totalMemoryRequired = jmMemory + tmMemory * containerCount;
            YarnJob.ClusterResourceDescription freeClusterMem = getCurrentFreeClusterResources(yarnClient);
            if (freeClusterMem.totalFreeMemory < totalMemoryRequired) {
                //TODO: add real log message
                System.out.println("This YARN session requires " + totalMemoryRequired + "MB of memory in the cluster. "
                        + "There are currently only " + freeClusterMem.totalFreeMemory + "MB available.");
                return false;
            }
            if (tmMemory > freeClusterMem.containerLimit) {
                //TODO: add real log message
                System.out.println("The requested amount of memory for the TaskManagers (" + tmMemory + "MB) is more than "
                        + "the largest possible YARN container: " + freeClusterMem.containerLimit);
                return false;
            }
            if (jmMemory > freeClusterMem.containerLimit) {
                //TODO: add real log message
                System.out.println("The requested amount of memory for the JobManager (" + jmMemory + "MB) is more than "
                        + "the largest possible YARN container: " + freeClusterMem.containerLimit);
                return false;
            }
        } catch (YarnException | IOException ex) {
            Logger.getLogger(YarnJob.class.getName()).log(Level.SEVERE, null, ex);
            return false;
        }
        return false;
    }

    private static class ClusterResourceDescription {

        public int totalFreeMemory;
        public int containerLimit;
    }

    private YarnJob.ClusterResourceDescription getCurrentFreeClusterResources(YarnClient yarnClient) throws YarnException, IOException {
        YarnJob.ClusterResourceDescription crd = new YarnJob.ClusterResourceDescription();
        crd.totalFreeMemory = 0;
        crd.containerLimit = 0;
        List<NodeReport> nodes = yarnClient.getNodeReports(NodeState.RUNNING);
        for (NodeReport rep : nodes) {
            int free = rep.getCapability().getMemory() - (rep.getUsed() != null ? rep.getUsed().getMemory() : 0);
            crd.totalFreeMemory += free;
            if (free > crd.containerLimit) {
                crd.containerLimit = free;
            }
        }
        return crd;
    }

}
