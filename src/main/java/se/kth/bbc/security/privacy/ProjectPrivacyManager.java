package se.kth.bbc.security.privacy;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.Path;

import io.hops.bbc.ConsentStatus;
import java.io.BufferedOutputStream;
import java.io.ByteArrayInputStream;
import java.io.Closeable;
import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.List;
import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.faces.application.FacesMessage;
import javax.faces.context.ExternalContext;
import javax.faces.context.FacesContext;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

import javax.persistence.TypedQuery;
import javax.servlet.http.HttpServletResponse;
import org.primefaces.context.RequestContext;
import org.primefaces.event.SelectEvent;
import org.primefaces.model.DefaultStreamedContent;
import org.primefaces.model.StreamedContent;
import io.hops.bbc.Consents;
import java.util.logging.Logger;
import se.kth.bbc.project.Project;
import se.kth.bbc.project.fb.InodeFacade;
import se.kth.hopsworks.util.Settings;

@Stateless
public class ProjectPrivacyManager {

  
  private static final Logger logger = Logger.getLogger(ProjectPrivacyManager.class.
          getName());

  @PersistenceContext(unitName = "kthfsPU")
  private EntityManager em;

  
  @EJB
  private InodeFacade inodeFacade;

  private static final int DEFAULT_BUFFER_SIZE = 10240;

  protected EntityManager getEntityManager() {
    return em;
  }

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
    em.persist(consent);
    return true;
  }

  public Consents getConsentByName(int cid) throws ParseException {

    TypedQuery<Consents> q = em.createNamedQuery("Consents.findById",
            Consents.class);
    q.setParameter("id", cid);
    List<Consents> consent = q.getResultList();
    if (consent.size() > 0) {
      return consent.get(0);
    }
    return null;

  }

  public List<Consents> getAllConsets(int pid) {
    TypedQuery<Consents> q = em.createNamedQuery("Consents.findByProjectId",
            Consents.class);
    q.setParameter("project.id", pid);

    return q.getResultList();

  }

  public List<Consents> findAllNewConsets(ConsentStatus status) {
    TypedQuery<Consents> q = em.createNamedQuery("Consents.findByStatus",
            Consents.class);
    q.setParameter("consentStatus", status);

    return q.getResultList();

  }

  public List<Consents> findAllConsents() {
    TypedQuery<Consents> q = em.createNamedQuery("Consents.findAll",
            Consents.class);
    return q.getResultList();
  }

  public boolean updateConsentStatus(Consents cons, ConsentStatus status) {

    if (cons != null) {
      cons.setConsentStatus(status);
      em.merge(cons);

      return true;
    }
    return false;
  }

  public void downloadPDF(Consents consent) throws IOException {

    // Prepare.
    FacesContext facesContext = FacesContext.getCurrentInstance();
    ExternalContext externalContext = facesContext.getExternalContext();
    HttpServletResponse response = (HttpServletResponse) externalContext.
            getResponse();

    StreamedContent input = null;
    BufferedOutputStream output = null;

    String projectPath = "/" + Settings.DIR_ROOT + "/" + consent.getProject().
            getName();
    String consentsPath = projectPath + "/" + Settings.DIR_CONSENTS;
    
    String path =  relativePath(inodeFacade.getPath(consent.getInode()), consent.getProject());
    
    //String path= inodeFacade.getPath(consent.getInode());

    try {

      Path path2 = Paths.get(path);
      byte[] data = Files.readAllBytes(path2);

      input = new DefaultStreamedContent(new ByteArrayInputStream(data));

      // Init servlet response.
      response.reset();
      response.setHeader("Content-Type", "application/pdf");
      response.setHeader("Content-Length",
              String.valueOf(data.length)
      );
      output = new BufferedOutputStream(response.getOutputStream(),
              DEFAULT_BUFFER_SIZE);

      // Write file contents to response.
      byte[] buffer = new byte[DEFAULT_BUFFER_SIZE];
      int length;
      while ((length = input.getStream().read(buffer)) > 0) {
        output.write(buffer, 0, length);
      }

      // Finalize task.
      output.flush();
    } finally {
      // Gently close streams.
      close(output);
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

  private String relativePath(String path, Project project) {
    logger.info("relative path for: " + path);
    return path.replace("/" + Settings.DIR_ROOT + "/" + project.getName() + "/", "");
  }

}
