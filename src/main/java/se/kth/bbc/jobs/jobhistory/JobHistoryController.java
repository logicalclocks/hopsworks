package se.kth.bbc.jobs.jobhistory;

import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.ejb.EJB;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.ManagedProperty;
import javax.faces.bean.RequestScoped;
import org.primefaces.model.DefaultStreamedContent;
import org.primefaces.model.StreamedContent;
import se.kth.bbc.fileoperations.FileOperations;
import se.kth.bbc.jobs.cuneiform.CuneiformController;
import se.kth.bbc.lims.ClientSessionState;
import se.kth.bbc.lims.MessagesController;
import se.kth.bbc.lims.Utils;

/**
 *
 * @author stig
 */
@ManagedBean(name = "jobHistoryController")
@RequestScoped
public class JobHistoryController implements Serializable {

  private static final Logger logger = Logger.getLogger(
          JobHistoryController.class.getName());

  @EJB
  private JobHistoryFacade history;

  @EJB
  private FileOperations fops;

  @ManagedProperty(value = "#{clientSessionState}")
  private ClientSessionState sessionState;

  // for loading specific history 
  @ManagedProperty(value = "#{cuneiformController}")
  private CuneiformController cfCont;

  public void setSessionState(ClientSessionState sessionState) {
    this.sessionState = sessionState;
  }

  public void setCfCont(CuneiformController cfCont) {
    this.cfCont = cfCont;
  }

  public List<JobHistory> getHistoryForType(JobType type) {
    return history.findForStudyByType(sessionState.getActiveStudyname(), type);
  }

  public StreamedContent downloadFile(String path) {
    String filename = Utils.getFileName(path);
    try {
      InputStream is = fops.getInputStream(path);
      StreamedContent sc = new DefaultStreamedContent(is, Utils.getMimeType(
              filename),
              filename);
      logger.log(Level.FINE, "File was downloaded from HDFS path: {0}",
              path);
      return sc;
    } catch (IOException ex) {
      logger.log(Level.SEVERE, "Failed to download file at path: " + path, ex);
      MessagesController.addErrorMessage("Download failed.");
    }
    return null;
  }

  public void selectJob(JobHistory job) {
    switch (job.getType()) {
      case CUNEIFORM:
        cfCont.selectJob(job);
        break;
    }
  }

  public final boolean isDir(String path) {
    return fops.isDir(path);
  }
}
