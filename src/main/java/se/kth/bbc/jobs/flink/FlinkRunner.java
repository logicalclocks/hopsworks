package se.kth.bbc.jobs.flink;

import java.io.File;
import java.io.IOException;
import java.util.logging.Logger;

import org.apache.flink.client.program.PackagedProgram;
import org.apache.flink.client.program.ProgramInvocationException;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.yarn.exceptions.YarnException;
import org.apache.flink.client.program.Client;
import org.apache.flink.configuration.ConfigConstants;
import org.apache.flink.configuration.Configuration;
import org.apache.flink.client.FlinkYarnSessionCli;
import org.apache.flink.runtime.yarn.AbstractFlinkYarnClient;
import org.apache.flink.runtime.yarn.AbstractFlinkYarnCluster;

/**
 * 
 * @author martin
 */
public class FlinkRunner {

	private static final Logger logger = Logger
			.getLogger(FlinkRunner.class.getName());

	private final static Path fatJarPath = new Path("PUT A REAL PATH");
	private final File jobJarPath;
	private final String jobJarMain;
	private final String[] jobArgs;
	private final int parallelism;
	private AbstractFlinkYarnCluster cluster;

	private FlinkRunner(FlinkBuilder builder) throws Exception {
		this.cluster = builder.flinkYarnClient.deploy(builder.jobName);
		this.jobJarMain = builder.jobJarMain;
		this.jobJarPath = builder.jobJarPath;
		this.parallelism = builder.parallelism;
		this.jobArgs = builder.jobArgs;

	}

	public int runJob() throws YarnException, IOException,
			ProgramInvocationException {

		PackagedProgram pp = new PackagedProgram(jobJarPath, jobJarMain,
				jobArgs);

		Configuration conf = new Configuration();
		// TODO not sure the AM manager address can be given this way
		conf.setString(ConfigConstants.JOB_MANAGER_IPC_ADDRESS_KEY, cluster
				.getJobManagerAddress().getAddress().toString());
		conf.setInteger(ConfigConstants.JOB_MANAGER_IPC_PORT_KEY, cluster
				.getJobManagerAddress().getPort());

		Client flinkClient = new Client(conf, pp.getUserCodeClassLoader());
		flinkClient.run(pp, parallelism, true);

		// TODO kills the cluster after job ran -> needs to be build new
		cluster.shutdown();

		return 0;
	}

	public static class FlinkBuilder {

		private final AbstractFlinkYarnClient flinkYarnClient;
		private final File jobJarPath;
		private final String jobJarMain;

		private String[] jobArgs = new String[0];

		private int parallelism = 2;
		private String jobName = "flink-job";

		public FlinkBuilder(File jobJarPath, String jobJarMain) {
			this.flinkYarnClient = FlinkYarnSessionCli.getFlinkYarnClient();
			this.flinkYarnClient.setLocalJarPath(fatJarPath);
			this.jobJarMain = jobJarMain;
			this.jobJarPath = jobJarPath;
		}

		public FlinkBuilder setParallelism(int para) {
			this.parallelism = para;
			this.flinkYarnClient.setTaskManagerCount(para);
			return this;
		}

		public FlinkBuilder setAmMemory(int amMem) {
			this.flinkYarnClient.setJobManagerMemory(amMem);
			return this;
		}

		public FlinkBuilder setTmMemory(int tmMem) {
			this.flinkYarnClient.setTaskManagerMemory(tmMem);
			return this;
		}

		public FlinkBuilder setTmSlots(int slots) {
			this.flinkYarnClient.setTaskManagerSlots(slots);
			return this;
		}

		public FlinkBuilder setQueue(String queue) {
			this.flinkYarnClient.setQueue(queue);
			return this;
		}

		public FlinkBuilder setJobArgs(String... args) {
			this.jobArgs = args;
			return this;
		}

		public FlinkRunner build() throws Exception {
			return new FlinkRunner(this);
		}

	}
}
