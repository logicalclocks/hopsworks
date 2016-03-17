package se.kth.hopsworks.controller;

import io.hops.hdfs.HdfsLeDescriptorsFacade;
import java.io.File;
import java.io.IOException;
import java.security.PrivilegedExceptionAction;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.ejb.EJB;
import org.apache.hadoop.security.UserGroupInformation;
import se.kth.bbc.activity.ActivityFacade;
import se.kth.bbc.fileoperations.FileOperations;
import se.kth.bbc.jobs.AsynchronousJobExecutor;
import se.kth.bbc.jobs.flink.FlinkJob;
import se.kth.bbc.jobs.jobhistory.Execution;
import se.kth.bbc.jobs.jobhistory.JobType;
import se.kth.bbc.jobs.model.description.JobDescription;
import se.kth.hopsworks.hdfs.fileoperations.DistributedFsService;
import se.kth.hopsworks.hdfs.fileoperations.UserGroupInformationService;
import se.kth.hopsworks.hdfsUsers.controller.HdfsUsersController;
import se.kth.hopsworks.user.model.Users;
import se.kth.hopsworks.util.Settings;

/**
 * Interaction point between the Flink front- and backend.
 *
 */
public class FlinkController {

    private static final Logger logger = Logger.getLogger(FlinkController.class.
            getName());

    @EJB
    private FileOperations fops;
    @EJB
    private AsynchronousJobExecutor submitter;
    @EJB
    private ActivityFacade activityFacade;
    @EJB
    private DistributedFsService dfs;
    @EJB
    private UserGroupInformationService ugiService;
    @EJB
    private HdfsUsersController hdfsUsersBean;
    @EJB
    private Settings settings;
    @EJB
    private HdfsLeDescriptorsFacade hdfsLeDescriptorsFacade;

    /**
     * Start the Flink job as the given user.
     * <p/>
     * @param job
     * @param user
     * @return
     * @throws IllegalStateException If Spark is not set up properly.
     * @throws IOException If starting the job fails.
     * @throws NullPointerException If job or user is null.
     * @throws IllegalArgumentException If the given job does not represent a
     * Spark job.
     */
    public Execution startJob(final JobDescription job, final Users user) throws
            IllegalStateException,
            IOException, NullPointerException, IllegalArgumentException {
        //First: some parameter checking.
        if (job == null) {
            throw new NullPointerException("Cannot run a null job.");
        } else if (user == null) {
            throw new NullPointerException("Cannot run a job as a null user.");
        } else if (job.getJobType() != JobType.FLINK) {
            throw new IllegalArgumentException(
                    "Job configuration is not a Spark job configuration.");
        } else if (!isFlinkJarAvailable()) {
            throw new IllegalStateException("Spark is not installed on this system.");
        }
        String username = hdfsUsersBean.getHdfsUserName(job.getProject(), user);
        UserGroupInformation proxyUser = ugiService.getProxyUser(username);
        FlinkJob flinkjob = null;
        try {
            flinkjob = proxyUser.doAs(new PrivilegedExceptionAction<FlinkJob>() {
                @Override
                public FlinkJob run() throws Exception {
                    return new FlinkJob(job, submitter, user, settings.
                            getHadoopDir(), settings.getFlinkDir(), hdfsLeDescriptorsFacade.getSingleEndpoint(),
                            settings.getFlinkUser());
                }
            });
        } catch (InterruptedException ex) {
            logger.log(Level.SEVERE, null, ex);
        }
        if (flinkjob == null) {
            throw new NullPointerException("Could not instantiate Flink job.");
        }
        Execution execution = flinkjob.requestExecutionId();
        if (execution != null) {
            submitter.startExecution(flinkjob);
        } else {
            logger.log(Level.SEVERE,
                    "Failed to persist JobHistory. Aborting execution.");
            throw new IOException("Failed to persist JobHistory.");
        }
        activityFacade.persistActivity(ActivityFacade.RAN_JOB, job.getProject(),
                user.asUser());
        return execution;
    }

    public void stopJob(JobDescription job, Users user, String appid) throws
            IllegalStateException,
            IOException, NullPointerException, IllegalArgumentException {
        //First: some parameter checking.
        if (job == null) {
            throw new NullPointerException("Cannot stop a null job.");
        } else if (user == null) {
            throw new NullPointerException("Cannot stop a job as a null user.");
        } else if (job.getJobType() != JobType.FLINK) {
            throw new IllegalArgumentException(
                    "Job configuration is not a Flink job configuration.");
        } else if (!isFlinkJarAvailable()) {
            throw new IllegalStateException("Flink is not installed on this system.");
        }

        FlinkJob sparkjob = new FlinkJob(job, submitter, user, settings.getHadoopDir(), settings.getSparkDir(),
                hdfsLeDescriptorsFacade.getSingleEndpoint(), settings.getSparkUser());

        submitter.stopExecution(sparkjob, appid);

    }

    /**
     * Check if the Flink jar is in HDFS. If it's not, try and copy it there
     * from the local filesystem. If it's still not there, then return false.
     * <p/>
     * @return
     */
    public boolean isFlinkJarAvailable() {
        boolean isInHdfs;
        try {
            isInHdfs = fops.exists(settings.getHdfsSparkJarPath());
        } catch (IOException e) {
            logger.log(Level.WARNING, "Cannot get Flink jar file from HDFS: {0}",
                    settings.getHdfsSparkJarPath());
            //Can't connect to HDFS: return false
            return false;
        }
        if (isInHdfs) {
            return true;
        }

        File localSparkJar = new File(settings.getLocalSparkJarPath());
        if (localSparkJar.exists()) {
            try {
                String hdfsJarPath = settings.getHdfsSparkJarPath();
                fops.copyToHDFSFromLocal(false, settings.getLocalSparkJarPath(),
                        hdfsJarPath);
            } catch (IOException e) {
                return false;
            }
        } else {
            logger.log(Level.WARNING, "Cannot find Flink jar file locally: {0}",
                    settings.getLocalSparkJarPath());
            return false;
        }
        return true;
    }
}
