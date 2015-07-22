package se.kth.hopsworks.controller;

import java.io.File;
import java.io.IOException;
import java.util.jar.Attributes;
import java.util.jar.Attributes.Name;
import java.util.jar.JarInputStream;
import java.util.jar.Manifest;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.ejb.EJB;
import javax.ejb.Stateless;
import se.kth.bbc.activity.ActivityFacade;
import se.kth.bbc.fileoperations.FileOperations;
import se.kth.bbc.jobs.AsynchronousJobExecutor;
import se.kth.bbc.jobs.jobhistory.Execution;
import se.kth.bbc.jobs.jobhistory.ExecutionFacade;
import se.kth.bbc.jobs.jobhistory.Job;
import se.kth.bbc.jobs.spark.SparkJob;
import se.kth.bbc.jobs.spark.SparkJobConfiguration;
import se.kth.bbc.jobs.spark.SparkYarnRunnerBuilder;
import se.kth.bbc.jobs.yarn.YarnRunner;
import se.kth.bbc.lims.Constants;
import se.kth.bbc.lims.Utils;
import se.kth.bbc.project.Project;
import se.kth.bbc.project.ProjectFacade;
import se.kth.hopsworks.user.model.Users;

/**
 * Interaction point between the Spark front- and backend.
 * <p>
 * @author stig
 */
@Stateless
public class SparkController {

  private static final Logger logger = Logger.getLogger(SparkController.class.
          getName());

  @EJB
  private FileOperations fops;
  @EJB
  private ExecutionFacade executionFacade;
  @EJB
  private AsynchronousJobExecutor submitter;
  @EJB
  private ActivityFacade activityFacade;
  @EJB
  private ProjectFacade projects;

  /**
   * Start the Spark job as the given user.
   * <p>
   * @param job
   * @param user
   * @return
   * @throws IllegalStateException If Spark is not set up properly.
   * @throws IOException If starting the job fails.
   * @throws NullPointerException If job or user is null.
   * @throws IllegalArgumentException If the given job does not represent a
   * Spark job.
   */
  public Execution startJob(Job job, Users user) throws IllegalStateException,
          IOException, NullPointerException, IllegalArgumentException {
    //First: some parameter checking.
    if(job == null){
      throw new NullPointerException("Cannot run a null job.");
    }else if(user == null){
      throw new NullPointerException("Cannot run a job as a null user.");
    }else if(! (job.getJobConfig() instanceof SparkJobConfiguration)){
      throw new IllegalArgumentException("Job configuration is not a Spark job configuration.");
    } else if (!isSparkJarAvailable()) {
      throw new IllegalStateException("Spark is not installed on this system.");
    }
    //Then: actually get to running.
    SparkJobConfiguration jobconfig = (SparkJobConfiguration)job.getJobConfig();
    if (jobconfig.getAppName() == null || jobconfig.getAppName().isEmpty()) {
      jobconfig.setAppName("Untitled Spark Job");
    }
    SparkYarnRunnerBuilder runnerbuilder = new SparkYarnRunnerBuilder(
            jobconfig.getJarPath(), jobconfig.getMainClass());
    runnerbuilder.setJobName(jobconfig.getAppName());
    String[] jobArgs = jobconfig.getArgs().trim().split(" ");
    runnerbuilder.addAllJobArgs(jobArgs);
    //Set spark runner options
    runnerbuilder.setExecutorCores(jobconfig.getExecutorCores());
    runnerbuilder.setExecutorMemory(jobconfig.getExecutorMemory());
    runnerbuilder.setNumberOfExecutors(jobconfig.getNumberOfExecutors());
    //Set Yarn running options
    runnerbuilder.setDriverMemoryMB(jobconfig.getAmMemory());
    runnerbuilder.setDriverCores(jobconfig.getAmVCores());
    runnerbuilder.setDriverQueue(jobconfig.getAmQueue());

    //TODO: runnerbuilder.setExtraFiles(config.getExtraFiles());
    YarnRunner r;
    try {
      r = runnerbuilder.getYarnRunner();
    } catch (IOException e) {
      logger.log(Level.SEVERE,
              "Failed to create YarnRunner.", e);
      throw new IOException("Failed to start Yarn client.", e);
    }

    SparkJob sparkjob = new SparkJob(executionFacade, r, fops);
    Project project = job.getProject();
    Execution jh = sparkjob.requestJobId(job, user);
    if (jh != null) {
      String stdOutFinalDestination = Utils.getHdfsRootPath(project.getName())
              + Constants.SPARK_DEFAULT_OUTPUT_PATH + jh.getId()
              + File.separator + "stdout.log";
      String stdErrFinalDestination = Utils.getHdfsRootPath(project.getName())
              + Constants.SPARK_DEFAULT_OUTPUT_PATH + jh.getId()
              + File.separator + "stderr.log";
      sparkjob.setStdOutFinalDestination(stdOutFinalDestination);
      sparkjob.setStdErrFinalDestination(stdErrFinalDestination);
      submitter.startExecution(sparkjob);
    } else {
      logger.log(Level.SEVERE,
              "Failed to persist JobHistory. Aborting execution.");
      throw new IOException("Failed to persist JobHistory.");
    }
    activityFacade.persistActivity(ActivityFacade.RAN_JOB, project, user.asUser());
    return jh;
  }

  /**
   * Check if the Spark jar is in HDFS. If it's not, try and copy it there from
   * the local filesystem. If it's still not there, then return false.
   * <p>
   * @return
   */
  private boolean isSparkJarAvailable() {
    boolean isInHdfs;
    try {
      isInHdfs = fops.exists(Constants.DEFAULT_SPARK_JAR_HDFS_PATH);
    } catch (IOException e) {
      //Can't connect to HDFS: return false
      return false;
    }
    if (isInHdfs) {
      return true;
    }

    File localSparkJar = new File(Constants.DEFAULT_SPARK_JAR_PATH);
    if (localSparkJar.exists()) {
      try {
        fops.copyToHDFSFromLocal(false, Constants.DEFAULT_SPARK_JAR_PATH,
                Constants.DEFAULT_SPARK_JAR_HDFS_PATH);
      } catch (IOException e) {
        return false;
      }
    } else {
      return false;
    }
    return true;
  }

  /**
   * Inspect the jar on the given path for execution. Returns a
   * SparkJobConfiguration object with a default configuration for this job.
   * <p>
   * @param path
   * @return
   * @throws IOException
   */
  public SparkJobConfiguration inspectJar(String path) throws IOException,
          IllegalArgumentException {
    if (!path.endsWith(".jar")) {
      throw new IllegalArgumentException("Path does not point to a jar file.");
    }
    JarInputStream jis = new JarInputStream(fops.getInputStream(path));
    Manifest mf = jis.getManifest();
    Attributes atts = mf.getMainAttributes();
    SparkJobConfiguration config = new SparkJobConfiguration();
    if (atts.containsKey(Name.MAIN_CLASS)) {
      config.setMainClass(atts.getValue(Name.MAIN_CLASS));
    }
    config.setJarPath(path);
    return config;
  }
}
