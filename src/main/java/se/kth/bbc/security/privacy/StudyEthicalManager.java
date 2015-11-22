package se.kth.bbc.security.privacy;

import io.hops.bbc.ConsentStatus;
import io.hops.bbc.Consents;
import java.io.IOException;
import java.io.Serializable;
import java.text.ParseException;
import java.util.List;
import javax.ejb.EJB;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.ManagedProperty;
import javax.faces.bean.SessionScoped;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import se.kth.bbc.activity.ActivityFacade;
import se.kth.bbc.lims.ClientSessionState;
import se.kth.bbc.project.Project;
import se.kth.bbc.project.ProjectFacade;

@ManagedBean(name = "studyEthicalManager")
@SessionScoped
public class StudyEthicalManager implements Serializable {

  private static final long serialVersionUID = 1L;

  @PersistenceContext(unitName = "kthfsPU")
  private EntityManager em;

  @EJB
  private StudyPrivacyManager privacyManager;

  @EJB
  private ProjectFacade studyController;

  @EJB
  private ActivityFacade activityFacade;

  @ManagedProperty(value = "#{clientSessionState}")
  private ClientSessionState sessionState;

  public static final String APPROVE_STUDY = " approved study ";
  public static final String REJECT_STUDY = " rejected study ";

  public static final String APPROVE_CONSENT = " approveed consent info ";
  public static final String EXPIRED_STUDY = " removed expired study ";

  public static final String REJECT_CONSENT = " rejected consent info ";

  private List<Project> allStudies;

  private List<Project> allExpiredStudies;

  private List<Consents> allNewConsents;

  private List<Consents> allConsents;

  protected EntityManager getEntityManager() {
    return em;
  }

  public List<Project> getAllStudies() {
    this.allStudies = studyController.findAll();
    return this.allStudies;
  }

  public List<Consents> getAllConsents() {
    this.allConsents = privacyManager.findAllConsets(1);
    return this.allConsents;
  }

  public List<Consents> getAllNewConsents() {
    this.allNewConsents = privacyManager.findAllNewConsets(
            ConsentStatus.PENDING.name());
    return this.allNewConsents;
  }

  public List<Project> getAllExpiredStudies() {
    this.allExpiredStudies = studyController.findAllExpiredStudies();
    return this.allExpiredStudies;
  }

  public void setAllExpiredStudies(List<Project> allExpiredStudies) {
    this.allExpiredStudies = allExpiredStudies;
  }

  public void setSessionState(ClientSessionState sessionState) {
    this.sessionState = sessionState;
  }

  public void showConsent(String consName) {

    try {
      Consents consent = privacyManager.getConsentByName(consName);
      privacyManager.downloadPDF(consent);
    } catch (ParseException | IOException ex) {

    }

  }

  public void approveStudy(Project study) {

  }

  public void rejectStudy(Project study) {

   
  }

  public void approveConsent(Consents cons) {

  
  }

  public void rejectConsent(Consents cons) {

   

  }

}