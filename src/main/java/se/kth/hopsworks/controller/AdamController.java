package se.kth.hopsworks.controller;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.ejb.EJB;
import javax.ejb.Stateless;
import se.kth.bbc.activity.ActivityFacade;
import se.kth.bbc.fileoperations.FileOperations;
import se.kth.bbc.jobs.AsynchronousJobExecutor;
import se.kth.bbc.jobs.adam.AdamArgumentDTO;
import se.kth.bbc.jobs.adam.AdamJob;
import se.kth.bbc.jobs.adam.AdamJobConfiguration;
import se.kth.bbc.jobs.adam.AdamOptionDTO;
import se.kth.bbc.jobs.jobhistory.Execution;
import se.kth.bbc.jobs.jobhistory.ExecutionFacade;
import se.kth.bbc.jobs.jobhistory.JobDescription;
import se.kth.bbc.jobs.jobhistory.JobOutputFileFacade;
import se.kth.bbc.jobs.spark.SparkYarnRunnerBuilder;
import se.kth.bbc.jobs.yarn.YarnRunner;
import se.kth.bbc.lims.Constants;
import se.kth.bbc.lims.Utils;
import se.kth.bbc.project.Project;
import se.kth.hopsworks.user.model.Users;

/**
 * Acts as the interaction point between the Adam frontend and backend.
 * <p>
 * @author stig
 */
@Stateless
public class AdamController {

  private static final Logger logger = Logger.getLogger(AdamController.class.
          getName());

  @EJB
  private FileOperations fops;
  @EJB
  private ExecutionFacade executionFacade;
  @EJB
  private JobOutputFileFacade outputFacade;
  @EJB
  private AsynchronousJobExecutor submitter;
  @EJB
  private ActivityFacade activityFacade;

  /**
   * Start an execution of the given job, ordered by the given User.
   * <p>
   * @param job
   * @param user
   * @return
   * @throws IllegalStateException If Adam is not set up properly.
   * @throws IllegalArgumentException If the JobDescription is not set up properly.
   * @throws IOException If starting the job fails.
   * @throws NullPointerException If job or user is null.
   */
  public Execution startJob(JobDescription job, Users user) throws IllegalStateException,
          IllegalArgumentException, IOException, NullPointerException {
    //First: do some parameter checking.
    if (job == null) {
      throw new NullPointerException("Cannot run a null job.");
    } else if (user == null) {
      throw new NullPointerException("Cannot run a job as a null user.");
    } else if (!(job.getJobConfig() instanceof AdamJobConfiguration)) {
      throw new IllegalArgumentException(
              "The given job does not represent an Adam job configuration.");
    } else if (!areJarsAvailable()) {
      //Check if all the jars are available
      throw new IllegalStateException(
              "Some ADAM jars are not in HDFS and could not be copied over.");
    }
    //Get to starting the job
    AdamJobConfiguration config = (AdamJobConfiguration) job.getJobConfig();
    checkIfRequiredPresent(config); //thows an IllegalArgumentException if not ok.

    Project project = job.getProject();
    //Then: submit ADAM job
    if (config.getAppName() == null || config.getAppName().isEmpty()) {
      config.setAppName("Untitled ADAM Job");
    }
    SparkYarnRunnerBuilder builder = new SparkYarnRunnerBuilder(
            Constants.ADAM_DEFAULT_JAR_HDFS_PATH, Constants.ADAM_MAINCLASS);
    //Set some ADAM-specific property values   
    builder.addSystemProperty("spark.serializer",
            "org.apache.spark.serializer.KryoSerializer");
    builder.addSystemProperty("spark.kryo.registrator",
            "org.bdgenomics.adam.serialization.ADAMKryoRegistrator");
    builder.addSystemProperty("spark.kryoserializer.buffer", "4m");
    builder.addSystemProperty("spark.kryo.referenceTracking", "true");
    builder.setExecutorMemoryGB(1);

    builder.addAllJobArgs(constructArgs(config));

    //Add all ADAM jars to local resources
    addAllAdamJarsToLocalResourcesAndClasspath(builder);

    //Set the job name
    builder.setJobName(config.getAppName());

    YarnRunner r;
    r = builder.getYarnRunner();

    AdamJob adamjob = new AdamJob(executionFacade, outputFacade, r, fops,
            config.getSelectedCommand().getArguments(), config.
            getSelectedCommand().getOptions());
    Execution jh = adamjob.requestJobId(job, user);
    if (jh != null) {
      String stdOutFinalDestination = Utils.getHdfsRootPath(project.getName())
              + Constants.ADAM_DEFAULT_OUTPUT_PATH + jh.getId()
              + File.separator + "stdout.log";
      String stdErrFinalDestination = Utils.getHdfsRootPath(project.getName())
              + Constants.ADAM_DEFAULT_OUTPUT_PATH + jh.getId()
              + File.separator + "stderr.log";
      adamjob.setStdOutFinalDestination(stdOutFinalDestination);
      adamjob.setStdErrFinalDestination(stdErrFinalDestination);
      submitter.startExecution(adamjob);
    } else {
      logger.log(Level.SEVERE,
              "Failed to persist JobHistory. Aborting execution.");
      throw new IOException("Failed to persist JobHistory.");
    }
    activityFacade.persistActivity(ActivityFacade.RAN_JOB, project, user.
            asUser());
    return jh;
  }

  /**
   * Check if all required arguments have been filled in. Throws an
   * IllegalArgumentException if not all required arguments are present.
   */
  private void checkIfRequiredPresent(AdamJobConfiguration ajc) throws
          IllegalArgumentException {
    boolean retval = true;
    StringBuilder missing = new StringBuilder();

    for (AdamArgumentDTO arg : ajc.getSelectedCommand().getArguments()) {
      if (arg.isRequired() && (arg.getValue() == null || arg.getValue().
              isEmpty())) {
        //Required argument is missing
        missing.append(arg.getName()).append(", ");
        retval = false;
      }
    }
    if (!retval) {
      String vals = missing.substring(0, missing.length());
      throw new IllegalArgumentException("Argument(s) " + vals
              + " is (are) missing.");
    }
  }

  private List<String> constructArgs(AdamJobConfiguration ajc) {
    List<String> adamargs = new ArrayList<>();
    //First: add command
    adamargs.add(ajc.getSelectedCommand().getCommand());
    //Loop over arguments
    for (AdamArgumentDTO arg : ajc.getSelectedCommand().getArguments()) {
      adamargs.add(arg.getValue());
    }
    //Loop over options
    for (AdamOptionDTO opt : ajc.getSelectedCommand().getOptions()) {
      if (opt.isFlag()) {
        //flag: just add the name of the flag
        if (opt.getSet()) {
          adamargs.add(opt.toAdamOption().getCliVal());
        }
      } else if (opt.getValue() != null && !opt.getValue().isEmpty()) {
        //Not a flag: add the name of the option
        adamargs.add(opt.toAdamOption().getCliVal());
        adamargs.add(opt.getValue());
      }
    }
    return adamargs;
  }

  /**
   * Add all the ADAM jar to the local resources and to the classpath.
   * <p>
   * @param builder
   */
  private void addAllAdamJarsToLocalResourcesAndClasspath(
          SparkYarnRunnerBuilder builder) {
    //Add all to local resources and to classpath
    for (String sourcePath : Constants.ADAM_HDFS_JARS) {
      String filename = Utils.getFileName(sourcePath);
      builder.addExtraFile(filename, sourcePath);
      builder.addToClassPath(filename);
    }
  }

  /**
   * Check if the Spark jar is in HDFS. If it's not, try and copy it there from
   * the local filesystem. If it's still not there, then return false.
   * <p>
   * @return
   */
  private boolean areJarsAvailable() {
    try {
      boolean adamJarMissing = false;
      for (String s : Constants.ADAM_HDFS_JARS) {
        if (!fops.exists(s)) {
          adamJarMissing = true;
          logger.log(Level.WARNING, "Missing Adam jar: {0}", s);
        }
      }
      if (adamJarMissing) {
        return false;
      }
    } catch (IOException e) {
      return false;
    }

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
}
