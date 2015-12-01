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
import se.kth.bbc.fileoperations.FileOperations;
import se.kth.bbc.lims.ClientSessionState;
import se.kth.bbc.lims.MessagesController;
import se.kth.bbc.project.Project;
import se.kth.bbc.project.ProjectFacade;

@ManagedBean(name = "projectEthicalManager")
@SessionScoped
public class ProjectEthicalManager implements Serializable {

  private static final long serialVersionUID = 1L;

  @PersistenceContext(unitName = "kthfsPU")
  private EntityManager em;

  @EJB
  private ProjectPrivacyManager privacyManager;

  @EJB
  private FileOperations fops;
    
  @EJB
  private ProjectFacade studyController;

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
    this.allConsents = privacyManager.findAllConsents();
    return this.allConsents;
  }

  public List<Consents> getAllNewConsents() {
    this.allNewConsents = privacyManager.findAllNewConsets(
            ConsentStatus.PENDING);
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

  public void showConsent(Consents consName) {

    try {
      Consents consent = privacyManager.getConsentByName(consName.getId());
      privacyManager.downloadPDF(consent);
    } catch (ParseException | IOException ex) {
      MessagesController.addErrorMessage("Could not download the consent");
    }

  }
 public void approveProject(Project project) {

    if (project == null) {
      MessagesController.addErrorMessage("Error", "No project found!");
      return;
    }

    if (studyController.updateStudyStatus(project, ConsentStatus.APPROVED.name())) {
      MessagesController.addInfoMessage(project.getName() + " was approved.");
    } else {
      MessagesController.addErrorMessage(project.getName() + " was not approved!");

    }

  }

  public void rejectProject(Project project) {

    if (project == null) {
      MessagesController.addErrorMessage("Error", "No project found!");
      return;
    }

    if (studyController.updateStudyStatus(project, ConsentStatus.REJECTED.name())) {
      MessagesController.addInfoMessage(project.getName() + " was rejected.");
    
    } else {
      MessagesController.addErrorMessage(project.getName() + " was not rejected!");

    }

  }

  public void approveConsent(Consents cons) {

     if (cons == null) {
      MessagesController.addErrorMessage("Error", "No consent found!");
      return;
    }

    if (privacyManager.updateConsentStatus(cons, ConsentStatus.APPROVED)) {

      MessagesController.addInfoMessage(cons.getId() + " was approved.");

    } else {
      MessagesController.addErrorMessage(cons.getId() + " was not approved!");

    } 
  }

  public void rejectConsent(Consents cons) {

     if (cons == null) {
      MessagesController.addErrorMessage("Error", "No consent found!");
      return;
    }

    if (privacyManager.updateConsentStatus(cons, ConsentStatus.REJECTED)) {
      MessagesController.addErrorMessage(cons.getId() + " was not rejected!");

    } else {
      MessagesController.addInfoMessage(cons.getInode().getSymlink() + " was rejected.");
    }

  }

}