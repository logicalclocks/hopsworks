package se.kth.bbc.jobs.adam;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.annotation.PostConstruct;
import javax.ejb.EJB;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.ManagedProperty;
import javax.faces.bean.ViewScoped;
import se.kth.bbc.fileoperations.FileOperations;
import se.kth.bbc.jobs.AsynchronousJobExecutor;
import se.kth.bbc.jobs.FileSelectionController;
import se.kth.bbc.jobs.JobController;
import se.kth.bbc.jobs.jobhistory.JobHistoryFacade;
import se.kth.bbc.jobs.jobhistory.JobType;
import se.kth.bbc.jobs.spark.SparkYarnRunnerBuilder;
import se.kth.bbc.jobs.yarn.YarnRunner;
import se.kth.bbc.lims.ClientSessionState;
import se.kth.bbc.lims.Constants;
import se.kth.bbc.lims.MessagesController;
import se.kth.bbc.lims.StagingManager;
import se.kth.bbc.lims.Utils;

/**
 *
 * @author stig
 */
@ManagedBean
@ViewScoped
public final class AdamController extends JobController {

  private static final Logger logger = Logger.getLogger(AdamController.class.
          getName());

  private String jobName;
  private List<AdamInvocationArgument> args;
  private List<AdamInvocationOption> opts;

  private AdamCommand selectedCommand = null;

  @ManagedProperty(value = "#{clientSessionState}")
  private ClientSessionState sessionState;

  @ManagedProperty(value = "#{fileSelectionController}")
  private FileSelectionController fileSelectionController;

  @EJB
  private JobHistoryFacade history;

  @EJB
  private FileOperations fops;

  @EJB
  private StagingManager stagingManager;

  @EJB
  private AsynchronousJobExecutor submitter;

  @Override
  protected void registerMainFile(String filename,
          Map<String, String> attributes) {
    throw new UnsupportedOperationException(
            "The operation uploadMainFile is not supported in AdamController since it has no concept of main execution files.");
  }

  @Override
  protected void registerExtraFile(String filename,
          Map<String, String> attributes) {
    if (attributes == null || !attributes.containsKey("type") || !attributes.
            containsKey("name")) {
      throw new IllegalArgumentException(
              "AdamController expects attributes 'type' and 'name'.");
    }
    String type = attributes.get("type");
    if (null != type) {
      switch (type) {
        case "argument":
          String argname = attributes.get("name");
          for (AdamInvocationArgument aia : args) {
            if (aia.getArg().getName().equals(argname)) {
              aia.setValue(filename);
            }
          }
          break;
        case "option":
          String optname = attributes.get("name");
          for (AdamInvocationOption aio : opts) {
            if (aio.getOpt().getName().equals(optname)) {
              aio.setStringValue(filename);
            }
          }
          break;
        default:
          throw new IllegalArgumentException("Illegal attribute value for type.");
      }
    }
  }

  @PostConstruct
  public void init() {
    try {
      String path = stagingManager.getStagingPath() + File.separator
              + sessionState.getLoggedInUsername() + File.separator
              + sessionState.getActiveStudyname();
      super.setBasePath(path);
      super.setJobHistoryFacade(history);
      super.setFileOperations(fops);
      super.setFileSelector(fileSelectionController);
    } catch (IOException c) {
      logger.log(Level.SEVERE,
              "Failed to initialize ADAM staging folder for uploading.", c);
      MessagesController.addErrorMessage(
              "Failed to initialize ADAM controller. Running ADAM jobs will not work.");
    }
  }

  public void startJob() {
    //First: check if all required arguments have been filled in
    if (!checkIfRequiredPresent()) {
      return;
    }
    //Second: for all output path arguments and options: translate to internal format
    translateOutputPaths();
    //Third: submit ADAM job
    if (jobName == null || jobName.isEmpty()) {
      jobName = "Untitled ADAM Job";
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

    builder.addAllJobArgs(constructArgs());

    //Add all ADAM jars to local resources
    addAllAdamJarsToLocalResourcesAndClasspath(builder);

    //Set the job name
    builder.setJobName(jobName);

    YarnRunner r;
    try {
      r = builder.getYarnRunner();
    } catch (IOException e) {
      logger.log(Level.SEVERE,
              "Unable to create temp directory for logs. Aborting execution.", e);
      MessagesController.addErrorMessage("Failed to start Yarn client.");
      return;
    }

    AdamJob job = new AdamJob(history, r, fops, args, opts);
    setJobId(job.requestJobId(jobName, sessionState.getLoggedInUsername(),
            sessionState.getActiveStudyname(), JobType.ADAM));
    if (isJobSelected()) {
      String stdOutFinalDestination = Utils.getHdfsRootPath(sessionState.
              getActiveStudyname())
              + Constants.ADAM_DEFAULT_OUTPUT_PATH + getJobId()
              + File.separator + "stdout.log";
      String stdErrFinalDestination = Utils.getHdfsRootPath(sessionState.
              getActiveStudyname())
              + Constants.ADAM_DEFAULT_OUTPUT_PATH + getJobId()
              + File.separator + "stderr.log";
      job.setStdOutFinalDestination(stdOutFinalDestination);
      job.setStdErrFinalDestination(stdErrFinalDestination);
      submitter.startExecution(job);
      MessagesController.addInfoMessage("Job submitted!");
      resetArguments();
    } else {
      logger.log(Level.SEVERE,
              "Failed to persist JobHistory. Aborting execution.");
      MessagesController.addErrorMessage(
              "Failed to write job history. Aborting execution.");
    }
  }

  /**
   * Check if all required arguments have been filled in.
   * <p>
   * @return False if not all required arguments are present, true if they are.
   */
  private boolean checkIfRequiredPresent() {
    boolean retval = true;
    for (AdamInvocationArgument aia : args) {
      if (aia.getArg().isRequired() && (aia.getValue() == null
              || aia.getValue().isEmpty())) {
        //Required attribute is empty
        MessagesController.addInfoMessage("Argument required.", "The argument "
                + aia.getArg().getName() + " is required.");
        retval = false;
      }
    }
    return retval;
  }

  /**
   * Translate all output paths to their internal representation. I.e.:
   * - The path the user specified should begin with the studyname. If it does
   * not, we prepend it.
   * - The final path should start with
   */
  private void translateOutputPaths() {
    for (AdamInvocationArgument aia : args) {
      if (aia.getArg().isOutputPath() && aia.getValue() != null && !aia.
              getValue().isEmpty()) {
        aia.setValue(getPathForString(aia.getValue()));
      }
    }
    for (AdamInvocationOption aio : opts) {
      if (aio.getOpt().isValuePath() && aio.getStringValue() != null && !aio.
              getStringValue().isEmpty()) {
        aio.setStringValue(getPathForString(aio.getStringValue()));
      }
    }
  }

  private String getPathForString(String t) {
    t = t.replaceAll("\\\\", "/");
    while (t.startsWith("/")) {
      t = t.substring(1);
    }
    String strippedPath;
    if (t.equals(sessionState.getActiveStudyname())) {
      strippedPath = t;
    } else if (t.startsWith(sessionState.getActiveStudyname() + "/")) {
      strippedPath = t;
    } else {
      strippedPath = sessionState.getActiveStudyname() + "/" + t;
    }
    return File.separator + Constants.DIR_ROOT + File.separator + strippedPath;
  }

  private List<String> constructArgs() {
    List<String> adamargs = new ArrayList<>();
    //First: add command
    adamargs.add(selectedCommand.getCommand());
    //Loop over arguments
    for (AdamInvocationArgument aia : args) {
      if (aia.getArg().isPath() && !aia.getArg().isOutputPath()) {
        String filepath = getFilePath(aia.getValue());
        if (filepath.startsWith("hdfs:")) {
          adamargs.add(filepath);
        } else {
          adamargs.add("file:" + filepath);
        }
      } else {
        adamargs.add(aia.getValue());
      }
    }
    //Loop over options
    for (AdamInvocationOption aio : opts) {
      if (aio.getOpt().isFlag()) {
        //flag: just add the name of the flag
        if (aio.getBooleanValue()) {
          adamargs.add(aio.getOpt().getCliVal());
        }
      } else if (aio.getStringValue() != null && !aio.getStringValue().isEmpty()) {
        //Not a flag: add the name of the option
        adamargs.add(aio.getOpt().getCliVal());
        if (aio.getOpt().isValuePath() && !aio.getOpt().isOutputPath()) {
          String filepath = getFilePath(aio.getStringValue());
          if (filepath.startsWith("hdfs:")) {
            adamargs.add(filepath);
          } else {
            adamargs.add("file:" + filepath);
          }
        } else {
          //output path or string or sthn: add option value
          adamargs.add(aio.getStringValue());
        }
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

  public AdamCommand[] getAdamCommands() {
    return AdamCommand.values();
  }

  public void setSelectedCommand(AdamCommand ac) {
    if (ac != selectedCommand) {
      this.selectedCommand = ac;
      resetArguments();
    }
  }

  private void resetArguments() {
    if(selectedCommand == null){
      args = new ArrayList<>();
      opts = new ArrayList<>();
      return;
    }
    args = new ArrayList<>(selectedCommand.getArguments().length);
    opts = new ArrayList<>(selectedCommand.getOptions().length);
    for (AdamArgument aa : selectedCommand.getArguments()) {
      args.add(new AdamInvocationArgument(aa));
    }
    for (AdamOption ao : selectedCommand.getOptions()) {
      opts.add(new AdamInvocationOption(ao));
    }
  }

  public String getJobName() {
    return jobName;
  }

  public void setJobName(String jobName) {
    this.jobName = jobName;
  }

  public List<AdamInvocationArgument> getSelectedCommandArgs() {
    return args;
  }

  public void setSelectedCommandArgs(List<AdamInvocationArgument> args) {
    this.args = args;
  }

  public List<AdamInvocationOption> getSelectedCommandOpts() {
    return opts;
  }

  public void setSelectedCommandOpts(List<AdamInvocationOption> opts) {
    this.opts = opts;
  }

  public AdamCommand getSelectedCommand() {
    return selectedCommand;
  }

  public void setSessionState(ClientSessionState sessionState) {
    this.sessionState = sessionState;
  }

  public void setFileSelectionController(FileSelectionController fs) {
    this.fileSelectionController = fs;
  }

  public boolean isFileUploadedForArg(String name) {
    for (AdamInvocationArgument aia : args) {
      if (aia.getArg().getName().equals(name)) {
        return aia.getValue() != null && !aia.getValue().isEmpty();
      }
    }
    return false;
  }

  public boolean isFileUploadedForOpt(String name) {
    for (AdamInvocationOption aio : opts) {
      if (aio.getOpt().getName().equals(name)) {
        return aio.getStringValue() != null && !aio.getStringValue().isEmpty();
      }
    }
    return false;
  }

  public String getFileNameForArg(String name) {
    for (AdamInvocationArgument aia : args) {
      if (aia.getArg().getName().equals(name)) {
        return Utils.getFileName(aia.getValue());
      }
    }
    return null;
  }

  public String getFileNameForOpt(String name) {
    for (AdamInvocationOption aio : opts) {
      if (aio.getOpt().getName().equals(name)) {
        return Utils.getFileName(aio.getStringValue());
      }
    }
    return null;
  }

}
