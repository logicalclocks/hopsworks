package se.kth.bbc.jobs.adam;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.annotation.PostConstruct;
import javax.ejb.EJB;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.ManagedProperty;
import javax.faces.bean.ViewScoped;
import org.primefaces.event.FileUploadEvent;
import se.kth.bbc.fileoperations.FileOperations;
import se.kth.bbc.jobs.AsynchronousJobExecutor;
import se.kth.bbc.jobs.JobController;
import se.kth.bbc.jobs.jobhistory.JobHistoryFacade;
import se.kth.bbc.jobs.jobhistory.JobType;
import se.kth.bbc.jobs.spark.SparkJob;
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

  @EJB
  private JobHistoryFacade history;

  @EJB
  private FileOperations fops;

  @EJB
  private StagingManager stagingManager;
  
  @EJB
  private AsynchronousJobExecutor submitter;

  @Override
  protected void afterUploadMainFile(FileUploadEvent event) {
    throw new UnsupportedOperationException(
            "The operation uploadMainFile is not supported in AdamController since it has no concept of main execution files.");
  }

  @Override
  protected void afterUploadExtraFile(FileUploadEvent event) {
    String type = (String) event.getComponent().getAttributes().get("type");
    if ("argument".equals(type)) {
      String argname = (String) event.getComponent().getAttributes().get(
              "name");
      for (AdamInvocationArgument aia : args) {
        if (aia.getArg().getName().equals(argname)) {
        System.out.println("Found matching input, setting value");
          aia.setValue(event.getFile().getFileName());
        }
      }
    } else if ("option".equals(type)) {
      String optname = (String) event.getComponent().getAttributes().get(
              "name");
      for (AdamInvocationOption aio : opts) {
        if (aio.getOpt().getName().equals(optname)) {
          aio.setStringValue(event.getFile().getFileName());
        }
      }
    } else {
      throw new IllegalArgumentException("Illegal attribute value for type.");
    }
    System.out.println("AU------ARGS------\n"+args);
    System.out.println("AU------OPTS------\n"+opts);
  }

  @Override
  public String getPushChannel() {
    return "/" + sessionState.getActiveStudyname() + "/" + JobType.ADAM;
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
    } catch (IOException c) {
      logger.log(Level.SEVERE,
              "Failed to initialize ADAM staging folder for uploading.", c);
      MessagesController.addErrorMessage(
              "Failed to initialize ADAM controller. Running ADAM jobs will not work.");
    }
  }

  public void startJob() {
    System.out.println("SJ------ARGS------\n"+args);
    System.out.println("SJ------OPTS------\n"+opts);
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
            getMainFilePath(), Constants.ADAM_MAINCLASS);
    //Set some ADAM-specific property values
    builder.addSystemVariable("spark.serializer",
            "org.apache.spark.serializer.KryoSerializer");
    builder.addSystemVariable("spark.kryo.registrator",
            "org.bdgenomics.adam.serialization.ADAMKryoRegistrator");
    builder.addSystemVariable("spark.kryoserializer.buffer.mb", "4");
    builder.addSystemVariable("spark.kryo.referenceTracking", "true");
    builder.setExecutorMemoryGB(4);

    builder.setJobArgs(constructArgs());
    //TODO: Figure out how to run ADAM on YARN :D

    //Set the job name
    builder.setJobName(jobName);

    YarnRunner r;
    try{
      r = builder.getYarnRunner();
    }catch(IOException e){
      logger.log(Level.SEVERE,"Unable to create temp directory for logs. Aborting execution.",e);
      MessagesController.addErrorMessage("Failed to start Yarn client.");
      return;
    }
    
    SparkJob job = new SparkJob(history,r,fops);
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
      if (aia.getArg().isOutputPath()) {
        aia.setValue(getPathForString(aia.getValue()));
      }
    }
    for (AdamInvocationOption aio : opts) {
      if (aio.getOpt().isValuePath()) {
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

  private String constructArgs() {
    StringBuilder argbuilder = new StringBuilder();
    //Loop over arguments
    for (AdamInvocationArgument aia : args) {
      if (aia.getArg().isPath() && !aia.getArg().isOutputPath()) {
        //Is an input path: for now: local file
        //TODO: change if file from system
        argbuilder.append("file:").append(getFilePath(aia.getValue()));
        argbuilder.append(" ");
      } else {
        argbuilder.append(aia.getValue());
        argbuilder.append(" ");
      }
    }
    //Loop over options
    for (AdamInvocationOption aio : opts) {
      if (aio.getOpt().isFlag()) {
        //flag: just append the name of the flag
        if (aio.getBooleanValue()) {
          argbuilder.append(aio.getOpt().getCliVal());
          argbuilder.append(" ");
        }
      } else {
        //Not a flag: append the name of the option
        argbuilder.append(aio.getOpt().getCliVal()).append(" ");
        if (aio.getOpt().isValuePath() && aio.getOpt().isOutputPath()) {
          //input path: append its full path
          argbuilder.append("file:").append(getFilePath(aio.getStringValue()));
        } else {
          //output path or string or sthn: append option value
          argbuilder.append(aio.getStringValue());
        }
        argbuilder.append(" ");
      }
    }
    return argbuilder.toString();
  }

  public AdamCommand[] getAdamCommands() {
    return AdamCommand.values();
  }

  public void setSelectedCommand(AdamCommand ac) {
    this.selectedCommand = ac;
    args = new ArrayList<>(ac.getArguments().length);
    opts = new ArrayList<>(ac.getOptions().length);
    for (AdamArgument aa : ac.getArguments()) {
      args.add(new AdamInvocationArgument(aa));
    }
    for (AdamOption ao : ac.getOptions()) {
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
    System.out.println("Setting command list");
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

}
