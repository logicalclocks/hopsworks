package se.kth.bbc.jobs.adam;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.hadoop.yarn.api.records.LocalResourceType;
import org.apache.hadoop.yarn.api.records.LocalResourceVisibility;
import se.kth.bbc.jobs.AsynchronousJobExecutor;
import se.kth.bbc.jobs.model.description.JobDescription;
import se.kth.bbc.jobs.spark.SparkJob;
import se.kth.bbc.jobs.spark.SparkYarnRunnerBuilder;
import se.kth.bbc.lims.Utils;
import se.kth.hopsworks.controller.LocalResourceDTO;
import se.kth.hopsworks.hdfs.fileoperations.DistributedFileSystemOps;
import se.kth.hopsworks.user.model.Users;
import se.kth.hopsworks.util.Settings;

/**
 *
 * @author stig
 */
public class AdamJob extends SparkJob {

  private static final Logger logger = Logger.getLogger(AdamJob.class.getName());

  private final AdamJobConfiguration jobconfig;
  private final String sparkDir;
  private final String adamJarPath;
  private final String adamUser; //must be glassfish

  public AdamJob(JobDescription job,
          AsynchronousJobExecutor services, Users user, String hadoopDir,
          String sparkDir, String adamUser, String jobUser,
          String nameNodeIpPort, String adamJarPath, String kafkaAddress) {
    super(job, services, user, hadoopDir, sparkDir, nameNodeIpPort, adamUser,
            jobUser, kafkaAddress);
    if (!(job.getJobConfig() instanceof AdamJobConfiguration)) {
      throw new IllegalArgumentException(
              "JobDescription must contain a AdamJobConfiguration object. Received: "
              + job.getJobConfig().getClass());
    }
    this.jobconfig = (AdamJobConfiguration) job.getJobConfig();
    this.sparkDir = sparkDir;
    this.adamJarPath = adamJarPath;
    this.adamUser = adamUser;
  }

  @Override
  protected void runJob(DistributedFileSystemOps udfso,
          DistributedFileSystemOps dfso) {
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
    copyLogs(udfso);
    makeOutputAvailable(dfso);
    updateState(getFinalState());
  }

  /**
   * For all the output files that were created, create an Inode for them and
   * create entries in the DB.
   */
  private void makeOutputAvailable(DistributedFileSystemOps dfso) {
    for (AdamArgumentDTO arg : jobconfig.getSelectedCommand().getArguments()) {
      if (arg.isOutputPath() && !(arg.getValue() == null || arg.getValue().
              isEmpty())) {
        try {
          if (dfso.exists(arg.getValue())) {
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
          if (dfso.exists(opt.getValue())) {
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
  protected boolean setupJob(DistributedFileSystemOps dfso) {
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

    runnerbuilder = new SparkYarnRunnerBuilder(
            adamJarPath, Settings.ADAM_MAINCLASS);
    super.setupJob(dfso);
    //Set some ADAM-specific property values   
    runnerbuilder.addSystemProperty("spark.serializer",
            "org.apache.spark.serializer.KryoSerializer");
    runnerbuilder.addSystemProperty("spark.kryo.registrator",
            "org.bdgenomics.adam.serialization.ADAMKryoRegistrator");
    runnerbuilder.addSystemProperty("spark.kryoserializer.buffer", "4m");
    runnerbuilder.addSystemProperty("spark.kryo.referenceTracking", "true");

//    builder.setExecutorCores(jobconfig.getExecutorCores());
//    builder.setExecutorMemory("" + jobconfig.getExecutorMemory() + "m");
//    builder.setNumberOfExecutors(jobconfig.getNumberOfExecutors());
//    if(jobconfig.isDynamicExecutors()){
//      builder.setDynamicExecutors(jobconfig.isDynamicExecutors());
//      builder.setNumberOfExecutorsMin(jobconfig.getSelectedMinExecutors());
//      builder.setNumberOfExecutorsMax(jobconfig.getSelectedMaxExecutors());
//      builder.setNumberOfExecutorsInit(jobconfig.getNumberOfExecutorsInit());
//    }
//    //Set Yarn running options
//    builder.setDriverMemoryMB(jobconfig.getAmMemory());
//    builder.setDriverCores(jobconfig.getAmVCores());
//    builder.setDriverQueue(jobconfig.getAmQueue());
//    builder.setSparkHistoryServerIp(jobconfig.getHistoryServerIp());
//    
    runnerbuilder.addAllJobArgs(constructArgs(jobconfig));
//
//    builder.addExtraFiles(Arrays.asList(jobconfig.getLocalResources()));
//    //Set project specific resources
//    builder.addExtraFiles(projectLocalResources);
//    if(jobSystemProperties != null && !jobSystemProperties.isEmpty()){
//      for(Map.Entry<String,String> jobSystemProperty: jobSystemProperties.entrySet()){
//        builder.addSystemProperty(jobSystemProperty.getKey(), jobSystemProperty.getValue());
//      }
//    }
    //Add ADAM jar to local resources
    runnerbuilder.addExtraFile(new LocalResourceDTO(adamJarPath.substring(
            adamJarPath.
            lastIndexOf("/") + 1), adamJarPath,
            LocalResourceVisibility.PUBLIC.toString(),
            LocalResourceType.FILE.toString(), null));
    //Set the job name
    runnerbuilder.setJobName(jobconfig.getAppName());

    try {
      runner = runnerbuilder.
              getYarnRunner(jobDescription.getProject().getName(),
                      adamUser, jobUser, hadoopDir, sparkDir, nameNodeIpPort);
    } catch (IOException e) {
      logger.log(Level.SEVERE,
              "Failed to create YarnRunner.", e);
      writeToLogs(new IOException("Failed to start Yarn client.", e));
      return false;
    }

    String stdOutFinalDestination = Utils.getHdfsRootPath(hadoopDir,
            jobDescription.
            getProject().
            getName())
            + Settings.ADAM_DEFAULT_OUTPUT_PATH;
    String stdErrFinalDestination = Utils.getHdfsRootPath(hadoopDir,
            jobDescription.getProject().getName())
            + Settings.ADAM_DEFAULT_OUTPUT_PATH;
    setStdOutFinalDestination(stdOutFinalDestination);
    setStdErrFinalDestination(stdErrFinalDestination);
    return true;
  }

  /**
   * Check if all required arguments have been filled in.
   * <p/>
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
   * <p/>
   * @param builder
   */
  private void addAllAdamJarsToLocalResourcesAndClasspath(
          SparkYarnRunnerBuilder builder) {
    //Add all to local resources and to classpath
    List<String> jars = this.services.getFsService().getChildNames(
            Settings.ADAM_DEFAULT_HDFS_REPO);
    for (String jarname : jars) {
      String sourcePath = "hdfs://" + Settings.ADAM_DEFAULT_HDFS_REPO + jarname;
      builder.addExtraFile(new LocalResourceDTO(jarname, sourcePath,
              LocalResourceVisibility.PUBLIC.toString(),
              LocalResourceType.FILE.toString(), null));
    }
  }

  @Override
  protected void cleanup() {
    //Nothing to be done, really.
  }
  

}
