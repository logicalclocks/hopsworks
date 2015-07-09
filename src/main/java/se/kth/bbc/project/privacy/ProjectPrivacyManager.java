package se.kth.bbc.project.privacy;

/**
 *
 * @author Ali Gholami <gholami@pdc.kth.se>
 */
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
import se.kth.bbc.project.Project;
import se.kth.bbc.project.ProjectTeamFacade;
import se.kth.bbc.project.privacy.model.Consent;

@Stateless
public class ProjectPrivacyManager {

  @PersistenceContext(unitName = "kthfsPU")
  private EntityManager em;

  @EJB
  private ProjectTeamFacade stc;

  // Constants ----------------------------------------------------------------------------------
  private static final int DEFAULT_BUFFER_SIZE = 10240; // 10KB.

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

  public boolean upload(Consent consent) {
    em.persist(consent);
    return true;
  }

  /**
   * Get all the Consents for the given Project.
   * <p>
   * @param project
   * @return The Consent object, or null if none has been found.
   */
  public List<Consent> getAllConsentsByProject(Project project) {
    TypedQuery<Consent> q = em.createNamedQuery("Consent.findByProject",
            Consent.class);
    q.setParameter("project", project);
    return q.getResultList();
  }

  //TODO: this method is very badly designed. Not sure if it's supposed to count the number of consents, or ...?
  /**
   * Get the consent by its name.
   * <p>
   * @param name
   * @return The Consent object, or null if none has been found.
   * @throws ParseException
   */
  public Consent getConsentByName(String name) throws ParseException {
    TypedQuery<Consent> q = em.createNamedQuery("Consent.findByName",
            Consent.class);
    q.setParameter("name", name);
    List<Consent> consent = q.getResultList();
    if (consent.size() > 0) {
      return consent.get(0);
    }
    return null;

  }

  public Consent getActiveConsent(String projectName) {
    return (Consent) em.createQuery(
            "SELECT c FROM hopsworks_kthfs.Consent c WHERE c.status ='APPROVED' AND c.projectName = '"
            + projectName + "'").getSingleResult();
  }

  // Actions ------------------------------------------------------------------------------------
  public void downloadPDF(Consent consent) throws IOException {

    // Prepare.
    FacesContext facesContext = FacesContext.getCurrentInstance();
    ExternalContext externalContext = facesContext.getExternalContext();
    HttpServletResponse response = (HttpServletResponse) externalContext.
            getResponse();

    StreamedContent input = null;
    BufferedOutputStream output = null;

    try {
      // Open file.
      input = new DefaultStreamedContent(new ByteArrayInputStream(consent.
              getConsentForm()));

      // Init servlet response.
      response.reset();
      response.setHeader("Content-Type", "application/pdf");
      response.setHeader("Content-Length", String.valueOf(consent.
              getConsentForm().length));
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
}
