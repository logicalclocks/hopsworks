package io.hops.hopsworks.admin.project.privacy;

import io.hops.hopsworks.common.dao.project.ProjectPrivacyFacade;
import org.apache.hadoop.fs.Path;
import io.hops.hopsworks.common.dao.user.consent.ConsentStatus;
import java.io.BufferedOutputStream;
import java.io.Closeable;
import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.List;
import javax.ejb.Stateless;
import javax.faces.application.FacesMessage;
import javax.faces.context.ExternalContext;
import javax.faces.context.FacesContext;
import javax.servlet.http.HttpServletResponse;
import io.hops.hopsworks.common.dao.user.consent.Consents;

import java.util.logging.Logger;
import org.apache.hadoop.fs.FSDataInputStream;
import io.hops.hopsworks.common.hdfs.DistributedFileSystemOps;
import io.hops.hopsworks.common.util.Settings;
import javax.ejb.EJB;
import org.primefaces.context.RequestContext;
import org.primefaces.event.SelectEvent;

@Stateless
public class ProjectPrivacyManager {

  private static final Logger logger = Logger.getLogger(
          ProjectPrivacyManager.class.
          getName());

  @EJB
  private ProjectPrivacyFacade privacyFacade;

  private static final int DEFAULT_BUFFER_SIZE = 10240;

  public void onDateSelect(SelectEvent event) {
    FacesContext facesContext = FacesContext.getCurrentInstance();
    SimpleDateFormat format = new SimpleDateFormat("yyyy/MM/dd");
    facesContext.addMessage(null, new FacesMessage(FacesMessage.SEVERITY_INFO,
            "Date Selected", format.format(event.getObject())));
  }

  public void click() {
    RequestContext requestContext = RequestContext.getCurrentInstance();

    requestContext.update("form:display");
    requestContext.execute("PF('dlg').show()");
  }

  public boolean upload(Consents consent) {
    privacyFacade.upload(consent);
    return true;
  }

  public Consents getConsentById(int cid) throws ParseException {
    return privacyFacade.getConsentById(cid);
  }

  public Consents getConsentByName(String name) throws ParseException {
    return privacyFacade.getConsentByName(name);
  }

  public List<Consents> getAllConsets(int pid) {
    return privacyFacade.getAllConsets(pid);
  }

  public List<Consents> findAllNewConsets(ConsentStatus status) {
    return privacyFacade.findAllNewConsets(status);
  }

  public List<Consents> findAllConsents() {
    return privacyFacade.findAllConsents();
  }

  public boolean updateConsentStatus(Consents cons, ConsentStatus status) {
    return privacyFacade.updateConsentStatus(cons, status);
  }

  public void downloadPDF(Consents consent, DistributedFileSystemOps dfso)
          throws IOException {

    // Prepare.
    FacesContext facesContext = FacesContext.getCurrentInstance();
    ExternalContext externalContext = facesContext.getExternalContext();
    HttpServletResponse response = (HttpServletResponse) externalContext.
            getResponse();

    BufferedOutputStream output = null;

    String projectPath = "/" + Settings.DIR_ROOT + "/" + consent.getProject().
            getName();
    String consentsPath = projectPath + "/" + Settings.DIR_CONSENTS + "/"
            + consent.getProject().getInode().getInodePK().getName() + ".pdf";

    FSDataInputStream stream = null;
    try {
      stream = dfso.open(new Path(consentsPath));
      //response.header("Content-disposition", "attachment;");

      // Init servlet response.
      response.reset();
      response.setHeader("Content-Type", "application/pdf");
      response.setHeader("Content-Disposition", "attachment;filename="
              + consent.getProject().getInode().getInodePK().getName() + ".pdf");

      output = new BufferedOutputStream(response.getOutputStream());

      // Write file contents to response.
      byte[] buffer = new byte[DEFAULT_BUFFER_SIZE];
      int length;
      while ((length = stream.read(buffer)) > 0) {
        output.write(buffer, 0, length);
      }
      output.flush();
    } finally {
      close(output);
      if (stream != null) {
        stream.close();
      }
    }

    // Inform JSF that it doesn't need to handle response.
    // This is very important, otherwise you will get the following exception in the logs:
    // java.lang.IllegalStateException: Cannot forward after response has been committed.
    facesContext.responseComplete();
  }

  // Helpers (can be refactored to public utility class) ----------------------------------------
  private static void close(Closeable resource) {
    if (resource != null) {
      try {
        resource.close();
      } catch (IOException e) {
        // Do your thing with the exception. Print it, log it or mail it. It may be useful to 
        // know that this will generally only be thrown when the client aborted the download.
        e.printStackTrace();
      }
    }
  }

}
