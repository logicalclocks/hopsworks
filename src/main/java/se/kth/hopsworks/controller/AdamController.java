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
import se.kth.bbc.jobs.jobhistory.JobHistory;
import se.kth.bbc.jobs.jobhistory.JobHistoryFacade;
import se.kth.bbc.jobs.jobhistory.JobType;
import se.kth.bbc.jobs.spark.SparkYarnRunnerBuilder;
import se.kth.bbc.jobs.yarn.YarnRunner;
import se.kth.bbc.lims.Constants;
import se.kth.bbc.lims.Utils;
import se.kth.bbc.project.Project;
import se.kth.bbc.project.ProjectFacade;

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
  private JobHistoryFacade history;
  @EJB
  private ProjectFacade projects;
  @EJB
  private AsynchronousJobExecutor submitter;
  @EJB
  private ActivityFacade activityFacade;

  public JobHistory startJob(AdamJobConfiguration config, String user,
          Integer projectId) throws IllegalStateException,
          IllegalArgumentException,
          IOException {
    //Check if all the jars are available
    if (!areJarsAvailable()) {
      throw new IllegalStateException(
              "Some ADAM jars are not in HDFS and could not be copied over.");
    }
    //First: check if all required arguments have been filled in
    checkIfRequiredPresent(config); //thows an IllegalArgumentException if not ok.

    //Second: for all output path arguments and options: translate to internal format
    Project project = projects.find(projectId);
    translateOutputPaths(config, project.getName());
    //Third: submit ADAM job
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
    builder.addSystemProperty("spark.kryoserializer.buffer.mb", "4");
    builder.addSystemProperty("spark.kryo.referenceTracking", "true");
    builder.setExecutorMemoryGB(1);

    builder.addAllJobArgs(constructArgs(config));

    //Add all ADAM jars to local resources
    addAllAdamJarsToLocalResourcesAndClasspath(builder);

    //Set the job name
    builder.setJobName(config.getAppName());

    YarnRunner r;
    r = builder.getYarnRunner();

    AdamJob job = new AdamJob(history, r, fops, config.getSelectedCommand().
            getArguments(), config.getSelectedCommand().getOptions());
    JobHistory jh = job.requestJobId(config.getAppName(), user, project,
            JobType.ADAM);
    if (jh != null) {
      String stdOutFinalDestination = Utils.getHdfsRootPath(project.getName())
              + Constants.ADAM_DEFAULT_OUTPUT_PATH + jh.getId()
              + File.separator + "stdout.log";
      String stdErrFinalDestination = Utils.getHdfsRootPath(project.getName())
              + Constants.ADAM_DEFAULT_OUTPUT_PATH + jh.getId()
              + File.separator + "stderr.log";
      job.setStdOutFinalDestination(stdOutFinalDestination);
      job.setStdErrFinalDestination(stdErrFinalDestination);
      submitter.startExecution(job);
    } else {
      logger.log(Level.SEVERE,
              "Failed to persist JobHistory. Aborting execution.");
      throw new IOException("Failed to persist JobHistory.");
    }
    activityFacade.persistActivity(ActivityFacade.RAN_JOB, project, user);
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

  /**
   * Translate all output paths to their internal representation. I.e.:
   * - The path the user specified should begin with the projectname. If it does
   * not, we prepend it.
   */
  private void translateOutputPaths(AdamJobConfiguration ajc, String projectname) {
    for (AdamArgumentDTO arg : ajc.getSelectedCommand().getArguments()) {
      if (arg.isOutputPath() && arg.getValue() != null && !arg.getValue().
              isEmpty()) {
        arg.setValue(getPathForString(projectname, arg.getValue()));
      }
    }
    for (AdamOptionDTO opt : ajc.getSelectedCommand().getOptions()) {
      if (opt.isOutputPath() && opt.getValue() != null && !opt.getValue().
              isEmpty()) {
        opt.setValue(getPathForString(projectname, opt.getValue()));
      }
    }
  }

  private String getPathForString(String projectname, String t) {
    t = t.replaceAll("\\\\", "/");
    while (t.startsWith("/")) {
      t = t.substring(1);
    }
    String strippedPath;
    if (t.equals(projectname)) {
      strippedPath = t;
    } else if (t.startsWith(projectname + "/")) {
      strippedPath = t;
    } else {
      strippedPath = projectname + "/" + t;
    }
    return File.separator + Constants.DIR_ROOT + File.separator + strippedPath;
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
