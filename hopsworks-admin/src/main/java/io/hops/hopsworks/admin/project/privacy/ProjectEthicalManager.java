package io.hops.hopsworks.admin.project.privacy;

import io.hops.hopsworks.admin.lims.ClientSessionState;
import io.hops.hopsworks.admin.lims.MessagesController;
import io.hops.hopsworks.common.dao.user.consent.ConsentStatus;
import io.hops.hopsworks.common.dao.user.consent.Consents;
import java.io.IOException;
import java.io.Serializable;
import java.text.ParseException;
import java.util.List;
import javax.ejb.EJB;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.ManagedProperty;
import javax.faces.bean.SessionScoped;
import io.hops.hopsworks.common.dao.project.Project;
import io.hops.hopsworks.common.dao.project.ProjectFacade;
import io.hops.hopsworks.common.dao.user.security.audit.AuditManager;
import io.hops.hopsworks.common.hdfs.DistributedFileSystemOps;

@ManagedBean(name = "projectEthicalManager")
@SessionScoped
public class ProjectEthicalManager implements Serializable {

  private static final long serialVersionUID = 1L;

  @EJB
  private ProjectPrivacyManager privacyManager;
  @EJB
  private ProjectFacade projectController;

  @EJB
  AuditManager am;

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

  public List<Project> getAllStudies() {
    this.allStudies = projectController.findAll();
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
    this.allExpiredStudies = projectController.findAllExpiredStudies();
    return this.allExpiredStudies;
  }

  public void setAllExpiredStudies(List<Project> allExpiredStudies) {
    this.allExpiredStudies = allExpiredStudies;
  }

  public void setSessionState(ClientSessionState sessionState) {
    this.sessionState = sessionState;
  }

  public void showConsent(Consents consName, DistributedFileSystemOps dfso) {

    try {
      Consents consent = privacyManager.getConsentById(consName.getId());
      privacyManager.downloadPDF(consent, dfso);
    } catch (ParseException | IOException ex) {
      MessagesController.addErrorMessage("Could not download the consent");
    }

  }

  public void showConsent(String consName, DistributedFileSystemOps dfso) {

    try {
      Consents consent = privacyManager.getConsentByName(consName);
      privacyManager.downloadPDF(consent, dfso);
    } catch (ParseException | IOException ex) {
      MessagesController.addErrorMessage("Could not download the consent");
    }

  }

  public void approveProject(Project project) {

    if (project == null) {
      MessagesController.addErrorMessage("Error", "No project found!");
      return;
    }

    if (projectController.updateStudyStatus(project, ConsentStatus.APPROVED.
            name())) {
      MessagesController.addInfoMessage(project.getName() + " was approved.");
    } else {
      MessagesController.addErrorMessage(project.getName()
              + " was not approved!");

    }

  }

  public void rejectProject(Project project) {

    if (project == null) {
      MessagesController.addErrorMessage("Error", "No project found!");
      return;
    }

    if (projectController.updateStudyStatus(project, ConsentStatus.REJECTED.
            name())) {
      MessagesController.addInfoMessage(project.getName() + " was rejected.");

    } else {
      MessagesController.addErrorMessage(project.getName()
              + " was not rejected!");

    }

  }

  public void approveConsent(Consents cons) {

    if (cons == null) {
      MessagesController.addErrorMessage("Error", "No consent found!");
      return;
    }

    if (privacyManager.updateConsentStatus(cons, ConsentStatus.APPROVED)) {
      am.registerConsentInfo(sessionState.getLoggedInUser(),
              ConsentStatus.APPROVED.name(), "SUCCESS", cons, sessionState.
              getRequest());
      MessagesController.addInfoMessage(cons.getId() + " was approved.");

    } else {
      am.registerConsentInfo(sessionState.getLoggedInUser(),
              ConsentStatus.APPROVED.name(), "FAILED", cons, sessionState.
              getRequest());
      MessagesController.addErrorMessage(cons.getId() + " was not approved!");

    }
  }

  public void rejectConsent(Consents cons) {

    if (cons == null) {
      MessagesController.addErrorMessage("Error", "No consent found!");
      return;
    }

    if (privacyManager.updateConsentStatus(cons, ConsentStatus.REJECTED)) {
      am.registerConsentInfo(sessionState.getLoggedInUser(),
              ConsentStatus.REJECTED.name(), "FAILED", cons, sessionState.
              getRequest());
      MessagesController.addErrorMessage(cons.getId() + " was not rejected!");

    } else {
      am.registerConsentInfo(sessionState.getLoggedInUser(),
              ConsentStatus.REJECTED.name(), "SUCCESS", cons, sessionState.
              getRequest());
      MessagesController.addInfoMessage(cons.getInode().getSymlink()
              + " was rejected.");
    }

  }

}
