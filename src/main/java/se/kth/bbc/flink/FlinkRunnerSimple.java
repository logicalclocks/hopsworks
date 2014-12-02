package se.kth.bbc.flink;

import java.io.File;
import java.io.IOException;
import java.util.logging.Logger;

import org.apache.flink.client.program.PackagedProgram;
import org.apache.flink.client.program.ProgramInvocationException;
import org.apache.hadoop.yarn.exceptions.YarnException;
import org.apache.flink.client.program.Client;
import org.apache.flink.configuration.ConfigConstants;
import org.apache.flink.configuration.Configuration;

/**
 *
 * @author martin
 */
public class FlinkRunnerSimple {

    private static final Logger logger = Logger.getLogger(FlinkRunnerSimple.class
            .getName());

    private final File jobJarPath;
    private final String jobJarMain;
    private final int amPort;
    private final String amHost;
    private final String[] jobArgs;
    private final int parallelism;

    private FlinkRunnerSimple(FlinkBuilder builder) {
        this.jobJarMain = builder.jobJarMain;
        this.jobJarPath = builder.jobJarPath;
        this.parallelism = builder.parallelism;
        this.jobArgs = builder.jobArgs;
        this.amHost = builder.amHost;
        this.amPort = builder.amPort;
        
    }

    public int runJob() throws YarnException, IOException,
            ProgramInvocationException {
        
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

        
        private final File jobJarPath;
        private final String jobJarMain;

        private String[] jobArgs = new String[0];

        private int parallelism = 2;
        private final int amPort;
        private final String amHost;
        
        public FlinkBuilder(File jobJarPath, String jobJarMain, String amHost, int amPort) {
            this.jobJarMain = jobJarMain;
            this.jobJarPath = jobJarPath;
            this.amHost = amHost;
            this.amPort = amPort;
        }

        public FlinkBuilder setParallelism(int para) {
            this.parallelism = para;
            return this;
        }

        public FlinkBuilder setJobArgs(String... args) {
            this.jobArgs = args;
            return this;
        }

        public FlinkRunnerSimple build() {
            return new FlinkRunnerSimple(this);
        }

    }
}
