package se.kth.hopsworks.controller;

import io.hops.hdfs.HdfsLeDescriptors;
import io.hops.hdfs.HdfsLeDescriptorsFacade;
import java.io.File;
import java.io.IOException;
import java.security.PrivilegedExceptionAction;
import java.util.jar.Attributes;
import java.util.jar.JarInputStream;
import java.util.jar.Manifest;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.ejb.EJB;
import javax.ejb.Stateless;
import org.apache.hadoop.security.AccessControlException;
import org.apache.hadoop.security.UserGroupInformation;
import se.kth.bbc.activity.ActivityFacade;
import se.kth.bbc.fileoperations.FileOperations;
import se.kth.bbc.jobs.AsynchronousJobExecutor;
import se.kth.bbc.jobs.flink.FlinkJob;
import se.kth.bbc.jobs.flink.FlinkJobConfiguration;
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
@Stateless
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
     * @throws IllegalStateException If Flink is not set up properly.
     * @throws IOException If starting the job fails.
     * @throws NullPointerException If job or user is null.
     * @throws IllegalArgumentException If the given job does not represent a
     * Flink job.
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
                    "Job configuration is not a Flink job configuration.");
        } else if (!isFlinkJarAvailable()) {
            throw new IllegalStateException("Flink is not installed on this system.");
        }   
       
        String username = hdfsUsersBean.getHdfsUserName(job.getProject(), user);
        UserGroupInformation proxyUser = ugiService.getProxyUser(username);
        FlinkJob flinkjob = null;
        try {
            flinkjob = proxyUser.doAs(new PrivilegedExceptionAction<FlinkJob>() {
                @Override
                public FlinkJob run() throws Exception {
                    return new FlinkJob(job, submitter, user,
                            settings.getHadoopDir(), settings.getFlinkDir(), 
                            settings.getFlinkConfDir(), 
                            settings.getFlinkConfFile(),
                            hdfsLeDescriptorsFacade.getSingleEndpoint(),
                            settings.getFlinkUser(), 
                            settings.getKafkaConnectStr());
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

        FlinkJob flinkJob = new FlinkJob(job, submitter, user, 
                settings.getHadoopDir(), settings.getFlinkDir(),
                settings.getFlinkConfDir(), settings.getFlinkConfFile(),
                hdfsLeDescriptorsFacade.getSingleEndpoint(), 
                settings.getFlinkUser(), settings.getKafkaConnectStr());

        submitter.stopExecution(flinkJob, appid);

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
            isInHdfs = fops.exists(settings.getHdfsFlinkJarPath());
        } catch (IOException e) {
            logger.log(Level.WARNING, "Cannot get Flink jar file from HDFS: {0}",
                    settings.getHdfsFlinkJarPath());
            //Can't connect to HDFS: return false
            return false;
        }
        if (isInHdfs) {
            return true;
        }

        File localFlinkJar = new File(settings.getLocalFlinkJarPath());
        if (localFlinkJar.exists()) {
            try {
                String hdfsJarPath = settings.getHdfsFlinkJarPath();
                fops.copyToHDFSFromLocal(false, settings.getLocalFlinkJarPath(),
                        hdfsJarPath);
            } catch (IOException e) {
                return false;
            }
        } else {
            logger.log(Level.WARNING, "Cannot find Flink jar file locally: {0}",
                    settings.getLocalFlinkJarPath());
            return false;
        }
        return true;
    }
    
    /**
   * Inspect the jar on the given path for execution. Returns a FlinkJobConfiguration object with a default
   * configuration for this job.
   * <p/>
   * @param path
   * @param username the user name in a project (projectName__username)
   * @return
   * @throws org.apache.hadoop.security.AccessControlException
   * @throws IOException
   */
  public FlinkJobConfiguration inspectJar(String path, String username) throws
		  AccessControlException, IOException,
		  IllegalArgumentException {
	logger.log(Level.INFO, "Executing Flink job by {0} at path: {1}", new Object[]{username, path});
	if (!path.endsWith(".jar")) {
	  throw new IllegalArgumentException("Path does not point to a jar file.");
	}
	HdfsLeDescriptors hdfsLeDescriptors = hdfsLeDescriptorsFacade.findEndpoint();
	// If the hdfs endpoint (ip:port - e.g., 10.0.2.15:8020) is missing, add it.
	path = path.replaceFirst("hdfs:/*Projects",
			"hdfs://" + hdfsLeDescriptors.getHostname() + "/Projects");
	logger.log(Level.INFO, "Really executing Flink job by {0} at path: {1}", new Object[]{username, path});
	
	JarInputStream jis = new JarInputStream(dfs.getDfsOps(username).open(path));
	Manifest mf = jis.getManifest();
	Attributes atts = mf.getMainAttributes();
	FlinkJobConfiguration config = new FlinkJobConfiguration();
	if (atts.containsKey(Attributes.Name.MAIN_CLASS)) {
	  config.setMainClass(atts.getValue(Attributes.Name.MAIN_CLASS));
	}
        //Set Flink config params
        config.setFlinkConfDir(settings.getFlinkConfDir());
        config.setFlinkConfFile(settings.getFlinkConfFile());
        
	config.setJarPath(path);
	return config;
  }
}
