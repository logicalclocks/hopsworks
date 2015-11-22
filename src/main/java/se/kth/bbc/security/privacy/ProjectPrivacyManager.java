package se.kth.bbc.security.privacy;

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
import javax.persistence.Query;
import javax.persistence.TypedQuery;
import javax.servlet.http.HttpServletResponse;
import org.primefaces.context.RequestContext;
import org.primefaces.event.SelectEvent;
import org.primefaces.model.DefaultStreamedContent;
import org.primefaces.model.StreamedContent;
import se.kth.bbc.activity.ActivityController;
import se.kth.bbc.activity.ActivityDetail;
import se.kth.bbc.security.ua.EmailBean;
import se.kth.bbc.security.ua.UserManager;
import io.hops.bbc.Consents;
import se.kth.hopsworks.rest.AppException;

@Stateless
public class ProjectPrivacyManager {

  @PersistenceContext(unitName = "kthfsPU")
  private EntityManager em;

  @EJB
  private ActivityController activityController;


  @EJB
  private UserManager mgr;

  @EJB
  private EmailBean emailBean;

  private List<ActivityDetail> ad;

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

  public Consents getConsentByStudyName(String studyname) throws ParseException {

    TypedQuery<Consents> q = em.createNamedQuery("Consents.findByStudyName",
            Consents.class);
    q.setParameter("studyName", studyname);
    List<Consents> consent = q.getResultList();
    if (consent.size() > 0) {
      return consent.get(0);
    }
    return null;

  }

  public Consents getConsentByName(String name) throws ParseException {

    TypedQuery<Consents> q = em.createNamedQuery("Consents.findByName",
            Consents.class);
    q.setParameter("name", name);
    List<Consents> consent = q.getResultList();
    if (consent.size() > 0) {
      return consent.get(0);
    }
    return null;

  }


  public Consents getActiveConsent(String studyName) {
    return (Consents) em.createQuery(
            "SELECT c FROM Consents c WHERE c.ethical_status ='"
            + ConsentStatus.APPROVED.name()+ "' AND c.studyName = '"
            + studyName + "'").getSingleResult();

  }

  public List<Consents> getAllConsets(String studyName) {
    TypedQuery<Consents> q = em.createNamedQuery("Consents.findByStudyName",
            Consents.class);
    q.setParameter("studyName", studyName);

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

    try {
      // Open file.
      input = new DefaultStreamedContent(
              //new ByteArrayInputStream(consent.              getConsentForm())
      );

      // Init servlet response.
      response.reset();
      response.setHeader("Content-Type", "application/pdf");
      //response.setHeader("Content-Length",               String.valueOf(consent.getConsentForm().length)              );
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