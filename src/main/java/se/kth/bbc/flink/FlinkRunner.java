package se.kth.bbc.flink;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.util.Properties;
import java.util.logging.Logger;
import org.apache.flink.client.CliFrontend;

import org.apache.flink.client.program.PackagedProgram;
import org.apache.flink.client.program.ProgramInvocationException;
import org.apache.hadoop.yarn.exceptions.YarnException;
import org.apache.flink.client.program.Client;
import org.apache.flink.configuration.ConfigConstants;
import org.apache.flink.configuration.Configuration;
import org.apache.flink.yarn.ClientMasterControl;
import org.apache.flink.yarn.Utils;
import org.apache.hadoop.yarn.api.records.ApplicationReport;
import org.apache.hadoop.yarn.api.records.YarnApplicationState;
import org.apache.log4j.chainsaw.Main;

import se.kth.bbc.yarn.YarnRunner;
import se.kth.bbc.yarn.YarnRunner.Builder;

/**
 *
 * @author martin
 */
public class FlinkRunner {

    private static final Logger logger = Logger.getLogger(FlinkRunner.class
            .getName());

    private final static String flinkJarPath = "/home/stig/Downloads/flink-0.8-incubating-SNAPSHOT/lib/flink-dist-0.8-incubating-SNAPSHOT-yarn-uberjar.jar";
    private static final String appMasterMainClass = "org.apache.flink.yarn.appMaster.ApplicationMaster";
    private final YarnRunner yarnRunner;
    private final File jobJarPath;
    private final String jobJarMain;
    private final String[] jobArgs;

    private final int parallelism;

    private FlinkRunner(FlinkBuilder builder) {
        this.yarnRunner = builder.yarnBuilder.build();
        this.jobJarMain = builder.jobJarMain;
        this.jobJarPath = builder.jobJarPath;
        this.parallelism = builder.parallelism;
        this.jobArgs = builder.jobArgs;
    }

    public int runJob() throws YarnException, IOException,
            ProgramInvocationException {
        yarnRunner.startAppMaster();

        while (yarnRunner.getApplicationReport().getYarnApplicationState() != YarnApplicationState.RUNNING) {
            try {
                System.out.println("Waiting for app to run.");
                Thread.sleep(1000);
            } catch (InterruptedException e) {

            }
        }
/*
        ApplicationReport appReport = yarnRunner.getApplicationReport();
        File yarnPropertiesFile = new File("/home/stig/Downloads/flink-0.8-incubating-SNAPSHOT/conf/" + CliFrontend.YARN_PROPERTIES_FILE);
        // write jobmanager connect information
        Properties yarnProps = new Properties();
        int jmPort = Utils.offsetPort(6123, appReport.getApplicationId().getId());
        yarnProps.setProperty(CliFrontend.YARN_PROPERTIES_JOBMANAGER_KEY, appReport.getHost() + ":" + 6144);
        // add dynamic properties
        OutputStream out = new FileOutputStream(yarnPropertiesFile);
        yarnProps.store(out, "Generated YARN properties file");
        out.close();
        yarnPropertiesFile.setReadable(true, false); // readable for all.

        // connect RPC service
        ClientMasterControl cmc = new ClientMasterControl(new InetSocketAddress(appReport.getHost(), 10245));
        cmc.start();
*/

        String amHost = yarnRunner.getApplicationReport().getHost();
        Integer amPort = yarnRunner.getApplicationReport().getRpcPort();

        InetSocketAddress yarnAddress = InetSocketAddress.createUnresolved(
                amHost, amPort);
        PackagedProgram pp = new PackagedProgram(jobJarPath, jobJarMain,
                jobArgs);

        Configuration conf = new Configuration();
        conf.setString(ConfigConstants.JOB_MANAGER_IPC_ADDRESS_KEY, amHost);
        conf.setInteger(ConfigConstants.JOB_MANAGER_IPC_PORT_KEY, amPort);
        Client flinkClient = new Client(conf,
                pp.getUserCodeClassLoader());
        flinkClient.run(pp, parallelism, true);

        return 0;
    }

    public static class FlinkBuilder {

        private Builder yarnBuilder = new YarnRunner.Builder(flinkJarPath, "flink.jar");

        private final File jobJarPath;
        private final String jobJarMain;

        private String[] jobArgs = new String[0];

        private int parallelism = 2;
        private int tmMem = 1024;
        private int tmCor = 1;

        public FlinkBuilder(File jobJarPath, String jobJarMain) {
            this.jobJarMain = jobJarMain;
            this.jobJarPath = jobJarPath;
            this.yarnBuilder.appName("FlinkApp");
            this.yarnBuilder.addToAppMasterEnvironment("", "");
        }

        public FlinkBuilder setAmMemory(int mem) {
            this.yarnBuilder.appMasterMemory(mem);
            return this;
        }

        public FlinkBuilder setAmCores(int cor) {
            this.yarnBuilder.appMasterVCores(cor);
            return this;
        }

        public FlinkBuilder setParallelism(int para) {
            this.parallelism = para;
            return this;
        }

        public FlinkBuilder setTmMemory(int mem) {
            this.tmMem = mem;
            return this;
        }

        public FlinkBuilder setTmCores(int cor) {
            this.tmCor = cor;
            return this;
        }

        public FlinkBuilder setJobArgs(String... args) {
            this.jobArgs = args;
            return this;
        }

        public FlinkBuilder setAppName(String appName) {
            this.yarnBuilder.appName(appName);
            return this;
        }

        public FlinkRunner build() {
            this.yarnBuilder.addToAppMasterEnvironment(org.apache.flink.yarn.Client.ENV_TM_COUNT, String.valueOf(parallelism));
            this.yarnBuilder.addToAppMasterEnvironment(org.apache.flink.yarn.Client.ENV_TM_MEMORY, String.valueOf(tmMem));
            this.yarnBuilder.addToAppMasterEnvironment(org.apache.flink.yarn.Client.ENV_TM_CORES, String.valueOf(tmCor));
            //TODO: get from conf
            this.yarnBuilder.addToAppMasterEnvironment(org.apache.flink.yarn.Client.ENV_CLIENT_USERNAME, "stig");
            this.yarnBuilder.appMasterMainClass(appMasterMainClass);
            this.yarnBuilder.localResourcesBasePath(".flink/$APPID");
            this.yarnBuilder.addLocalResource("flink-conf.yaml", "/home/stig/Downloads/flink-0.8-incubating-SNAPSHOT/conf/flink-conf.yaml", "flink-conf.yaml");
            return new FlinkRunner(this);
        }

    }

    public static void maint() throws Exception {
        FlinkBuilder b = new FlinkBuilder(new File("/home/glassfish/stig/flink-0.8-incubating-SNAPSHOT/examples/flink-java-examples-0.8-incubating-SNAPSHOT-WordCount.jar"), "org.apache.flink.examples.java.wordcount.WordCount");
        b.setJobArgs("file:///home/glassfish/stig/helloWorld.cf", "file:///home/glassfish/stig/wordoutput");
        FlinkRunner r = b.build();
        r.runJob();
    }

}
