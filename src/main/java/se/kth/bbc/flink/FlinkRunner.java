package se.kth.bbc.flink;

import java.io.File;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.concurrent.Executors;
import java.util.logging.Logger;

import org.apache.flink.client.program.PackagedProgram;
import org.apache.flink.client.program.ProgramInvocationException;
import org.apache.hadoop.yarn.exceptions.YarnException;
import org.apache.flink.client.program.Client;
import org.apache.flink.configuration.ConfigConstants;
import org.apache.flink.configuration.Configuration;
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

    private final static String flinkJarPath = "/home/stig/projects/flink-yarn-0.7.0-incubating/lib/flink-dist-0.7.0-incubating-yarn-uberjar.jar";
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

        // stuff that has not yet been set and might be needed
//		public final static String ENV_APP_ID = "_APP_ID";
//		public final static String ENV_APP_NUMBER = "_APP_NUMBER";
//		public final static String FLINK_JAR_PATH = "_FLINK_JAR_PATH"; // the Flink jar resource location (in HDFS).
//		public static final String ENV_CLIENT_HOME_DIR = "_CLIENT_HOME_DIR";
//		public static final String ENV_CLIENT_SHIP_FILES = "_CLIENT_SHIP_FILES";
//		public static final String ENV_AM_PRC_PORT = "_AM_PRC_PORT";
//		public static final String ENV_SLOTS = "_SLOTS";
//		public static final String ENV_DYNAMIC_PROPERTIES = "_DYNAMIC_PROPERTIES";
        //todo: make more elegant
        while (yarnRunner.getApplicationReport().getYarnApplicationState() != YarnApplicationState.RUNNING && yarnRunner.getApplicationReport().getYarnApplicationState() != YarnApplicationState.FAILED) {
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {

            }
            System.out.println("Waiting for running container");
        }

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

        private int parallelism = 1;
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
            this.yarnBuilder.addLocalResource("flink-conf.yaml", "/home/stig/projects/flink-yarn-0.7.0-incubating/conf/flink-conf.yaml", "flink-conf.yaml");
            return new FlinkRunner(this);
        }

    }

    public static void maint() throws Exception {
        Executors.newSingleThreadExecutor().execute(new Runnable() {

            @Override
            public void run() {
                try {
                    FlinkBuilder b = new FlinkBuilder(new File("/home/stig/projects/flink-yarn-0.7.0-incubating/examples/flink-java-examples-0.7.0-incubating-WordCount.jar"), "org.apache.flink.examples.java.wordcount.WordCount");
                    b.setJobArgs("file:///home/stig/Downloads/Frodo.vcf", "file:///home/stig/tmp/wordoutput");
                    b.setParallelism(1);
                    FlinkRunner r = b.build();
                    r.runJob();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });

    }

}
