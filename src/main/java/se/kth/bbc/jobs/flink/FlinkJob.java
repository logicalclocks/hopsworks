package se.kth.bbc.jobs.flink;

import se.kth.bbc.jobs.AsynchronousJobExecutor;
import se.kth.bbc.jobs.model.description.JobDescription;
import se.kth.bbc.jobs.yarn.YarnJob;
import se.kth.hopsworks.user.model.Users;

/**
 * Orchestrates the execution of a Flink job: run job, update history object.
 * 
 */
public class FlinkJob extends YarnJob {

    private final FlinkJobConfiguration jobconfig; //Just for convenience
    private final String flinkDir;
    private final String flinkUser;

    /**
     * 
     * @param job
     * @param services
     * @param user
     * @param hadoopDir
     * @param flinkDir
     * @param nameNodeIpPort
     * @param flinkUser 
     */
    public FlinkJob(JobDescription job, AsynchronousJobExecutor services,
            Users user, final String hadoopDir,
            final String flinkDir, final String nameNodeIpPort, String flinkUser) {
        super(job, services, user, hadoopDir, nameNodeIpPort);
        if (!(job.getJobConfig() instanceof FlinkJobConfiguration)) {
            throw new IllegalArgumentException(
                    "JobDescription must contain a FlinkJobConfiguration object. Received: "
                    + job.getJobConfig().getClass());
        }
        this.jobconfig = (FlinkJobConfiguration) job.getJobConfig();
        this.flinkDir = flinkDir;
        this.flinkUser = flinkUser;
    }

    @Override
    protected boolean setupJob() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    protected void cleanup() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

}
