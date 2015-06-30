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
import se.kth.bbc.jobs.jobhistory.JobHistory;
import se.kth.bbc.jobs.jobhistory.JobHistoryFacade;
import se.kth.bbc.jobs.jobhistory.JobType;
import se.kth.bbc.jobs.spark.SparkJob;
import se.kth.bbc.jobs.spark.SparkJobConfiguration;
import se.kth.bbc.jobs.spark.SparkYarnRunnerBuilder;
import se.kth.bbc.jobs.yarn.YarnRunner;
import se.kth.bbc.lims.Constants;
import se.kth.bbc.lims.MessagesController;
import se.kth.bbc.lims.Utils;
import se.kth.bbc.project.Project;
import se.kth.bbc.project.ProjectFacade;

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
  private JobHistoryFacade history;
  @EJB
  private AsynchronousJobExecutor submitter;
  @EJB
  private ActivityFacade activityFacade;
  @EJB
  private ProjectFacade projects;
  

  /**
   * Start the job specified by jobconfig, as the user with the given username
   * in capacity of member of the given project.
   * <p>
   * @param jobconfig
   * @param user
   * @param projectId
   * @return
   * @throws IllegalStateException
   * @throws IOException
   */
  public JobHistory startJob(SparkJobConfiguration jobconfig, String user,
          Integer projectId) throws
          IllegalStateException, IOException {
    if (!isSparkJarAvailable()) {
      throw new IllegalStateException("Spark is not installed on this system.");
    }
    if (jobconfig.getAppName() == null || jobconfig.getAppName().isEmpty()) {
      jobconfig.setAppName("Untitled Spark Job");
    }
    SparkYarnRunnerBuilder runnerbuilder = new SparkYarnRunnerBuilder(
            jobconfig.getJarPath(), jobconfig.getMainClass());
    runnerbuilder.setJobName(jobconfig.getAppName());
    String[] jobArgs = jobconfig.getArgs().trim().split(" ");
    runnerbuilder.addAllJobArgs(jobArgs);
    //TODO: runnerbuilder.setExtraFiles(config.getExtraFiles());

    YarnRunner r;
    try {
      r = runnerbuilder.getYarnRunner();
    } catch (IOException e) {
      logger.log(Level.SEVERE,
              "Failed to create YarnRunner.", e);
      throw new IOException("Failed to start Yarn client.", e);
    }

    SparkJob job = new SparkJob(history, r, fops);
    Project project = projects.find(projectId);
    JobHistory jh = job.requestJobId(jobconfig.getAppName(), user, project,
            JobType.SPARK);
    if (jh != null) {
      String stdOutFinalDestination = Utils.getHdfsRootPath(project.getName())
              + Constants.SPARK_DEFAULT_OUTPUT_PATH + jh.getId()
              + File.separator + "stdout.log";
      String stdErrFinalDestination = Utils.getHdfsRootPath(project.getName())
              + Constants.SPARK_DEFAULT_OUTPUT_PATH + jh.getId()
              + File.separator + "stderr.log";
      job.setStdOutFinalDestination(stdOutFinalDestination);
      job.setStdErrFinalDestination(stdErrFinalDestination);
      submitter.startExecution(job);
      MessagesController.addInfoMessage("Job submitted!");
    } else {
      logger.log(Level.SEVERE,
              "Failed to persist JobHistory. Aborting execution.");
      throw new IOException("Failed to persist JobHistory.");
    }
    activityFacade.persistActivity(ActivityFacade.RAN_JOB, project, user);
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
        fops.copyToHDFSFromPath(Constants.DEFAULT_SPARK_JAR_PATH,
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
  public SparkJobConfiguration inspectJar(String path) throws IOException {
    JarInputStream jis = new JarInputStream(fops.getInputStream(path));
    Manifest mf = jis.getManifest();
    Attributes atts = mf.getMainAttributes();
    SparkJobConfiguration config = new SparkJobConfiguration();
    if(atts.containsKey(Name.MAIN_CLASS)){
      config.setMainClass(atts.getValue(Name.MAIN_CLASS));
    }
    config.setJarPath(path);
    return config;
  }
}
