package se.kth.bbc.jobs.adam;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import se.kth.bbc.jobs.AsynchronousJobExecutor;
import se.kth.bbc.jobs.model.description.JobDescription;
import se.kth.bbc.jobs.spark.SparkYarnRunnerBuilder;
import se.kth.bbc.jobs.yarn.YarnJob;
import se.kth.bbc.lims.Constants;
import se.kth.bbc.lims.Utils;
import se.kth.hopsworks.user.model.Users;

/**
 *
 * @author stig
 */
public class AdamJob extends YarnJob {

  private static final Logger logger = Logger.getLogger(AdamJob.class.getName());

  private final AdamJobConfiguration jobconfig;

  public AdamJob(JobDescription job,
          AsynchronousJobExecutor services, Users user) {
    super(job, user, services);
    if (!(job.getJobConfig() instanceof AdamJobConfiguration)) {
      throw new IllegalArgumentException(
              "JobDescription must contain a AdamJobConfiguration object. Received: "
              + job.getJobConfig().getClass());
    }
    this.jobconfig = (AdamJobConfiguration) job.getJobConfig();
  }

  @Override
  protected void runJob() {
    //Try to start the AM
    boolean proceed = startApplicationMaster();
    //If success: monitor running job
    if (!proceed) {
      return;
    }
    proceed = monitor();
    //If not ok: return
    if (!proceed) {
      return;
    }
    copyLogs();
    makeOutputAvailable();
    updateState(getFinalState());
  }

  /**
   * For all the output files that were created, create an Inode for them and
   * create entries in the DB.
   */
  private void makeOutputAvailable() {
    for (AdamArgumentDTO arg : jobconfig.getSelectedCommand().getArguments()) {
      if (arg.isOutputPath() && !(arg.getValue() == null || arg.getValue().
              isEmpty())) {
        try {
          if (services.getFileOperations().exists(arg.getValue())) {
            services.getJobOutputFileFacade().create(getExecution(), Utils.
                    getFileName(arg.getValue()), arg.getValue());
          }
        } catch (IOException e) {
          logger.log(Level.SEVERE, "Failed to create Inodes for HDFS path "
                  + arg.getValue() + ".", e);
        }
      }
    }

    for (AdamOptionDTO opt : jobconfig.getSelectedCommand().getOptions()) {
      if (opt.isOutputPath() && opt.getValue() != null && !opt.getValue().
              isEmpty()) {
        try {
          if (services.getFileOperations().exists(opt.getValue())) {
            services.getJobOutputFileFacade().create(getExecution(), Utils.
                    getFileName(opt.getValue()), opt.getValue());
          }
        } catch (IOException e) {
          logger.log(Level.SEVERE, "Failed to create Inodes for HDFS path "
                  + opt.getValue() + ".", e);
        }
      }
    }
  }

  @Override
  protected boolean setupJob() {
    //Get to starting the job
    List<String> missingArgs = checkIfRequiredPresent(jobconfig); //thows an IllegalArgumentException if not ok.
    if (!missingArgs.isEmpty()) {
      writeToLogs(
              "Cannot execute ADAM command because some required arguments are missing: "
              + missingArgs);
      return false;
    }

    //Then: submit ADAM job
    if (jobconfig.getAppName() == null || jobconfig.getAppName().isEmpty()) {
      jobconfig.setAppName("Untitled ADAM Job");
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

    builder.addAllJobArgs(constructArgs(jobconfig));

    //Add all ADAM jars to local resources
    addAllAdamJarsToLocalResourcesAndClasspath(builder);

    //Set the job name
    builder.setJobName(jobconfig.getAppName());

    try {
      runner = builder.getYarnRunner();
    } catch (IOException e) {
      logger.log(Level.SEVERE,
              "Failed to create YarnRunner.", e);
      writeToLogs(new IOException("Failed to start Yarn client.", e));
      return false;
    }

    String stdOutFinalDestination = Utils.getHdfsRootPath(jobDescription.
            getProject().
            getName())
            + Constants.ADAM_DEFAULT_OUTPUT_PATH + getExecution().getId()
            + File.separator + "stdout.log";
    String stdErrFinalDestination = Utils.getHdfsRootPath(jobDescription.
            getProject().
            getName())
            + Constants.ADAM_DEFAULT_OUTPUT_PATH + getExecution().getId()
            + File.separator + "stderr.log";
    setStdOutFinalDestination(stdOutFinalDestination);
    setStdErrFinalDestination(stdErrFinalDestination);
    return true;
  }

  /**
   * Check if all required arguments have been filled in.
   * <p>
   * @return A list of missing argument names. If the list is empty, all
   * required arguments are present.
   */
  private List<String> checkIfRequiredPresent(AdamJobConfiguration ajc) throws
          IllegalArgumentException {
    List<String> missing = new ArrayList<>();

    for (AdamArgumentDTO arg : ajc.getSelectedCommand().getArguments()) {
      if (arg.isRequired() && (arg.getValue() == null || arg.getValue().
              isEmpty())) {
        //Required argument is missing
        missing.add(arg.getName());
      }
    }
    return missing;
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
    List<String> jars = services.getFileOperations().getChildNames(
            Constants.ADAM_DEFAULT_HDFS_REPO);
    for (String jarname : jars) {
      String sourcePath = "hdfs://" + Constants.ADAM_DEFAULT_HDFS_REPO + jarname;
      builder.addExtraFile(jarname, sourcePath);
    }
  }

  @Override
  protected void cleanup() {
    //Nothing to be done, really.
  }

}
