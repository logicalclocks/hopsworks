/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package se.kth.bbc.study.privacy;

/**
 *
 * @author Ali Gholami <gholami@pdc.kth.se>
 */
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.Closeable;
import java.io.File;
import java.io.FileInputStream;
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
import org.primefaces.model.UploadedFile;
import se.kth.bbc.activity.ActivityController;
import se.kth.bbc.activity.ActivityDetail;
import se.kth.bbc.security.ua.EmailBean;
import se.kth.bbc.security.ua.UserManager;
import se.kth.bbc.study.StudyTeam;
import se.kth.bbc.study.StudyTeamFacade;
import se.kth.bbc.study.privacy.model.Consent;

@Stateless
public class StudyPrivacyManager {

    @PersistenceContext(unitName = "hopsPU")
    private EntityManager em;

    @EJB
    private ActivityController activityController;

    @EJB
    private StudyTeamFacade stc;

    @EJB
    private UserManager mgr;

    @EJB
    private EmailBean emailBean;

    private List<ActivityDetail> ad;

    // Constants ----------------------------------------------------------------------------------
    private static final int DEFAULT_BUFFER_SIZE = 10240; // 10KB.


 
    
    protected EntityManager getEntityManager() {
        return em;
    }

    public void onDateSelect(SelectEvent event) {
        FacesContext facesContext = FacesContext.getCurrentInstance();
        SimpleDateFormat format = new SimpleDateFormat("yyyy/MM/dd");
        facesContext.addMessage(null, new FacesMessage(FacesMessage.SEVERITY_INFO, "Date Selected", format.format(event.getObject())));
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
    
    public String getConsentStatus(String studyname) throws ParseException {

        TypedQuery<Consent> q = em.createNamedQuery("Consent.findByStudyName", Consent.class);
        q.setParameter("studyName", studyname);
        Consent consent = q.getSingleResult();
        return consent.getStatus();
    }

    public String getConsentName(String studyname) throws ParseException {

        TypedQuery<Consent> q = em.createNamedQuery("Consent.findByStudyName", Consent.class);
        q.setParameter("studyName", studyname);
        Consent consent = q.getSingleResult();
        return consent.getName();
    }

    public String getRoles(String study, String username) throws ParseException {
        List<StudyTeam> list = stc.findCurrentRole(study, username);
        return list.get(0).getTeamRole();
    }

    public List<ActivityDetail> getAllActivities(String studyName) {
        List<ActivityDetail> ad = activityController.activityDetailOnStudy(studyName);
        return ad;
    }

     public Consent getActiveConsent(String studyName) {
        return (Consent) em.createQuery("SELECT c FROM Consent c WHERE c.active =1 AND c.studyName = '" + studyName + "'").getSingleResult();
        
    }

    public List<Consent> getAllConsets(String studyName) {
        TypedQuery<Consent> q = em.createNamedQuery("Consent.findByStudyName", Consent.class);
        q.setParameter("studyName", studyName);
        return q.getResultList();
    
    }
    
    // Actions ------------------------------------------------------------------------------------
    public void downloadPDF(String path) throws IOException {

        // Prepare.
        FacesContext facesContext = FacesContext.getCurrentInstance();
        ExternalContext externalContext = facesContext.getExternalContext();
        HttpServletResponse response = (HttpServletResponse) externalContext.getResponse();
        path = "/usr/biobankcloud/kthfs-dashboard/src/main/webapp/resources/images/users/sampleinformedconsent.pdf";
        File file = new File(path);
        BufferedInputStream input = null;
        BufferedOutputStream output = null;

        try {
            // Open file.
            input = new BufferedInputStream(new FileInputStream(file), DEFAULT_BUFFER_SIZE);

            // Init servlet response.
            response.reset();
            response.setHeader("Content-Type", "application/pdf");
            response.setHeader("Content-Length", String.valueOf(file.length()));
            output = new BufferedOutputStream(response.getOutputStream(), DEFAULT_BUFFER_SIZE);

            // Write file contents to response.
            byte[] buffer = new byte[DEFAULT_BUFFER_SIZE];
            int length;
            while ((length = input.read(buffer)) > 0) {
                output.write(buffer, 0, length);
            }

            // Finalize task.
            output.flush();
        } finally {
            // Gently close streams.
            close(output);
            close(input);
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
