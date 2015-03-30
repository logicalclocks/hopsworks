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
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import javax.ejb.EJB;
import javax.faces.application.FacesMessage;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.RequestScoped;
import javax.faces.context.FacesContext;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.TypedQuery;

import org.primefaces.context.RequestContext;
import org.primefaces.event.SelectEvent;
import se.kth.bbc.activity.ActivityController;
import se.kth.bbc.activity.ActivityDetail;
import se.kth.bbc.study.StudyTeam;
import se.kth.bbc.study.StudyTeamFacade;
import se.kth.bbc.study.privacy.model.Consent;

@ManagedBean
@RequestScoped
public class StudyPrivacyManager {

    @PersistenceContext(unitName = "kthfsPU")
    private EntityManager em;
    private Date date;

   @EJB
    private ActivityController activityController;

   @EJB
    private StudyTeamFacade stc;
   
    private List <ActivityDetail> ad;
     
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

    public void showConsent(String name) {

    }

    public Date getDate() throws ParseException {
        SimpleDateFormat format = new SimpleDateFormat("yyyy/MM/dd");

        this.date = format.parse(format.format(this.date));
        return this.date;
    }

    public void setDate(Date date) {
        this.date = date;
    }

    public boolean updateRetentionPeriod(String studyname) {

        TypedQuery<Consent> q = em.createNamedQuery("Consent.findByStudyName", Consent.class);
        q.setParameter("studyName", studyname);
        Consent consent = q.getSingleResult();
        consent.setDate(date);
        em.merge(consent);
        return true;

    }

    public Date getRetentionPeriod(String studyname) throws ParseException {

        TypedQuery<Consent> q = em.createNamedQuery("Consent.findByStudyName", Consent.class);
        q.setParameter("studyName", studyname);
        Consent consent = q.getSingleResult();
        this.date = consent.getRetentionPeriod();
        SimpleDateFormat format = new SimpleDateFormat("yyyy/MM/dd");

        return format.parse(format.format(this.date));

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
    
    public List <ActivityDetail> getAllActivities(String studyName){
        List<ActivityDetail> ad= activityController.activityDetailOnStudy(studyName);
        return ad;
    }
}
