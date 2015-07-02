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
import se.kth.bbc.activity.ActivityFacade;
import se.kth.bbc.fileoperations.FileOperations;
import se.kth.bbc.jobs.FileSelectionController;
import se.kth.bbc.jobs.JobMB;
import se.kth.bbc.jobs.jobhistory.JobHistory;
import se.kth.bbc.jobs.jobhistory.JobHistoryFacade;
import se.kth.bbc.lims.ClientSessionState;
import se.kth.bbc.lims.MessagesController;
import se.kth.bbc.lims.StagingManager;
import se.kth.bbc.lims.Utils;
import se.kth.hopsworks.controller.AdamController;

/**
 *
 * @author stig
 */
@ManagedBean
@ViewScoped
public final class AdamMB extends JobMB {

  private static final Logger logger = Logger.getLogger(AdamMB.class.
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
  private AdamController adamController;

  @EJB
  private ActivityFacade activityFacade;

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
              + sessionState.getActiveProjectname();
      super.setBasePath(path);
      super.setJobHistoryFacade(history);
      super.setFileOperations(fops);
      super.setFileSelector(fileSelectionController);
      super.setActivityFacade(activityFacade);
    } catch (IOException c) {
      logger.log(Level.SEVERE,
              "Failed to initialize ADAM staging folder for uploading.", c);
      MessagesController.addErrorMessage(
              "Failed to initialize ADAM controller. Running ADAM jobs will not work.");
    }
  }

  public void startJob() {
    //Format some path stuff.
    for (AdamInvocationArgument aia : args) {
      if (aia.getArg().isPath()) {
        String filepath = getFilePath(aia.getValue());
        if (!aia.getArg().isOutputPath() && !filepath.startsWith("hdfs:")) {
          filepath = "file:" + filepath;
        }
        aia.setValue(filepath);
      }
    }
    for (AdamInvocationOption aio : opts) {
      if (aio.getOpt().isValuePath()) {
        String filepath = getFilePath(aio.getStringValue());
        if (!aio.getOpt().isOutputPath() && !filepath.startsWith("hdfs:")) {
          filepath = "file:" + filepath;
        }
        aio.setStringValue(filepath);
      }
    }
    //Construct an AdamJobConfiguration object
    AdamJobConfiguration config = new AdamJobConfiguration(new AdamCommandDTO(
            selectedCommand, args.toArray(new AdamInvocationArgument[0]), opts.
            toArray(new AdamInvocationOption[0])));
    try {
      JobHistory jh = adamController.startJob(config, sessionState.
              getLoggedInUsername(), sessionState.getActiveProject().getId());
      setSelectedJob(jh);
      MessagesController.addInfoMessage("Job submitted!");
      resetArguments();
    } catch (IllegalStateException | IllegalArgumentException | IOException e) {
      MessagesController.addErrorMessage("Failed to start application master: "
              + e.getLocalizedMessage());
      logger.log(Level.WARNING, "Failed to start Adam job.", e);
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
    if (selectedCommand == null) {
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
