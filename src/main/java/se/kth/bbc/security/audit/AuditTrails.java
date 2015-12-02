/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package se.kth.bbc.security.audit;

import java.io.Serializable;
import java.util.Date;
import java.util.List;
import javax.ejb.EJB;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.ViewScoped;
import se.kth.bbc.activity.ActivityController;
import se.kth.bbc.activity.ActivityDetail;
import se.kth.bbc.lims.MessagesController;
import se.kth.bbc.security.audit.model.AccountAudit;
import se.kth.bbc.security.audit.model.RolesAudit;
import se.kth.bbc.security.audit.model.Userlogins;
import se.kth.bbc.security.ua.UserManager;
import se.kth.hopsworks.user.model.Users;
 
@ManagedBean
@ViewScoped

public class AuditTrails implements Serializable {

  private static final long serialVersionUID = 1L;

  @EJB
  private UserManager userManager;

  @EJB
  private AuditManager auditManager;

  @EJB
  private ActivityController activityController;

  private String username;

  private Date from;

  private Date to;

  private AccountsAuditActions selectedAccountsAuditAction;

  private RolesAuditActions selectdeRolesAuditAction;

  private StudyAuditActions selectedStudyAuditAction;

  private LoginAuditActions selectedLoginsAuditAction;

  private List<Userlogins> userLogins;

  private List<RolesAudit> roleAudit;

  private List<AccountAudit> accountAudit;

  private List<ActivityDetail> ad;

  public String getUsername() {
    return username;
  }

  public void setUsername(String username) {
    this.username = username;
  }

  public Date getFrom() {
    return from;
  }

  public void setFrom(Date from) {
    this.from = from;
  }

  public Date getTo() {
    return to;
  }

  public void setTo(Date to) {
    this.to = to;
  }

  public RolesAuditActions[] getAuditActions() {
    return RolesAuditActions.values();
  }

  public List<Userlogins> getUserLogins() {
    return userLogins;
  }

  public void setUserLogins(List<Userlogins> userLogins) {
    this.userLogins = userLogins;
  }

  public List<RolesAudit> getRoleAudit() {
    return roleAudit;
  }

  public void setRoleAudit(List<RolesAudit> roleAudit) {
    this.roleAudit = roleAudit;
  }

  public List<AccountAudit> getAccountAudit() {
    return accountAudit;
  }

  public void setAccountAudit(List<AccountAudit> accountAudit) {
    this.accountAudit = accountAudit;
  }

  public AccountsAuditActions[] getAccountsAuditActions() {
    return AccountsAuditActions.values();
  }

  public RolesAuditActions[] getRolesAuditActions() {
    return RolesAuditActions.values();
  }

  public LoginAuditActions[] getLoginsAuditActions() {
    return LoginAuditActions.values();
  }

  public StudyAuditActions[] getStudyAuditActions() {
    return StudyAuditActions.values();
  }

  public AccountsAuditActions getSelectedAccountsAuditAction() {
    return selectedAccountsAuditAction;
  }

  public void setSelectedAccountsAuditAction(
          AccountsAuditActions selectedAccountsAuditAction) {
    this.selectedAccountsAuditAction = selectedAccountsAuditAction;
  }

  public RolesAuditActions getSelectdeRolesAuditAction() {
    return selectdeRolesAuditAction;
  }

  public void setSelectdeRolesAuditAction(
          RolesAuditActions selectdeRolesAuditAction) {
    this.selectdeRolesAuditAction = selectdeRolesAuditAction;
  }

  public StudyAuditActions getSelectedStudyAuditAction() {
    return selectedStudyAuditAction;
  }

  public void setSelectedStudyAuditAction(
          StudyAuditActions selectedStudyAuditAction) {
    this.selectedStudyAuditAction = selectedStudyAuditAction;
  }

  public LoginAuditActions getSelectedLoginsAuditAction() {
    return selectedLoginsAuditAction;
  }

  public void setSelectedLoginsAuditAction(
          LoginAuditActions selectedLoginsAuditAction) {
    this.selectedLoginsAuditAction = selectedLoginsAuditAction;
  }

  public List<ActivityDetail> getAd() {
    return ad;
  }

  public void setAd(List<ActivityDetail> ad) {
    this.ad = ad;
  }

  /**
   * Generate audit report for account modifications.
   * <p>
   * @param username
   * @param from
   * @param to
   * @param action
   * @return
   */
  public List<AccountAudit> getAccoutnAudit(String username, Date from, Date to,
          String action) {
    Users u = userManager.getUserByEmail(username);

    if (u == null) {
      return auditManager.getAccountAudit(convertTosqlDate(from),
              convertTosqlDate(to), action);
    } else {
      return auditManager.getAccountAudit(u.getUid(), convertTosqlDate(from),
              convertTosqlDate(to), action);
    }
  }

  /**
   *
   * @param studyName
   * @param from
   * @param to
   * @return
   */
  public List<ActivityDetail> getStudyAudit(String studyName, Date from, Date to) {
    return activityController.activityDetailOnStudyAudit(studyName,
            convertTosqlDate(from), convertTosqlDate(to));
  }

  /**
   * Generate audit report for role entitlement.
   * <p>
   * @param username
   * @param from
   * @param to
   * @param action
   * @return
   */
  public List<RolesAudit> getRoleAudit(String username, Date from, Date to,
          String action) {
    Users u = userManager.getUserByEmail(username);

    if (u == null) {
      return auditManager.getRoletAudit(convertTosqlDate(from),
              convertTosqlDate(to), action);
    } else {
      return auditManager.getRoletAudit(u.getUid(), convertTosqlDate(from),
              convertTosqlDate(to), action);
    }
  }

  /**
   *
   * @param username
   * @param from
   * @param to
   * @param action
   * @return
   */
  public List<Userlogins> getUserLogins(String username, Date from, Date to,
          String action) {
    Users u = userManager.getUserByEmail(username);
    if (u == null) {
      return auditManager.getUsersLoginsFromTo(convertTosqlDate(from),
              convertTosqlDate(to), action);
    } else {
      return auditManager.
              getUserLoginsFromTo(u.getUid(), convertTosqlDate(from),
                      convertTosqlDate(to), action);
    }
  }

  /**
   * Dispatch the audit events and get the relevant audit trails.
   * <p>
   * @param action
   */
  public void processLoginAuditRequest(LoginAuditActions action) {

    if (action.getValue().equals(LoginAuditActions.REGISTRATION.getValue())) {
      userLogins = getUserLogins(username, from, to, action.getValue());
    } else if (action.getValue().equals(LoginAuditActions.LOGIN.
            getValue()) || action.getValue().equals(LoginAuditActions.LOGOUT.
                    getValue())) {
      userLogins = getUserLogins(username, from, to, action.getValue());
    }else if(action.getValue().equals(LoginAuditActions.ALL.getValue())){
         userLogins = getUserLogins(username, from, to, action.getValue());
    } else {
      MessagesController.addSecurityErrorMessage("Audit action not supported.");
    }
  }

  /**
   * Dispatch the audit events and get the relevant audit trails.
   * <p>
   * @param action
   */
  public void processAccountAuditRequest(AccountsAuditActions action) {

    if (action.getValue().equals(AccountsAuditActions.PASSWORDCHANGE.getValue())) {
      accountAudit = getAccoutnAudit(username, from, to, action.getValue());
    } else if (action.getValue().equals(AccountsAuditActions.LOSTDEVICE.
            getValue())) {
      accountAudit = getAccoutnAudit(username, from, to, action.getValue());
    } else if (action.getValue().equals(AccountsAuditActions.PROFILEUPDATE.
            getValue())) {
      accountAudit = getAccoutnAudit(username, from, to, action.getValue());
    } else if (action.getValue().equals(AccountsAuditActions.SECQUESTIONCHANGE.
            getValue())) {
      accountAudit = getAccoutnAudit(username, from, to, action.getValue());
    } else if (action.getValue().equals(AccountsAuditActions.PROFILEUPDATE.
            getValue())) {
      accountAudit = getAccoutnAudit(username, from, to, action.getValue());
    } else if (action.getValue().equals(AccountsAuditActions.USERMANAGEMENT.
            getValue())) {
      accountAudit = getAccoutnAudit(username, from, to, action.getValue());
    } else {
      MessagesController.addSecurityErrorMessage("Audit action not supported.");
    }
  }

  /**
   * Generate audit report for role entitlement.
   * <p>
   * @param action
   */
  public void processRoleAuditRequest(RolesAuditActions action) {

    if (action.getValue().equals(RolesAuditActions.ADDROME.getValue())) {
      roleAudit = getRoleAudit(username, from, to, action.getValue());
    } else if (action.getValue().equals(RolesAuditActions.REMOVEROLE.getValue())) {
      roleAudit = getRoleAudit(username, from, to, action.getValue());
    } else if (action.getValue().equals(RolesAuditActions.ALLROLEASSIGNMENTS.
            getValue())) {
      roleAudit = getRoleAudit(username, from, to, action.getValue());
    } else {
      MessagesController.addSecurityErrorMessage("Audit action not supported.");
    }
  }

  /**
   * Generate audit report for studies.
   * <p>
   * @param action
   */
  public void processStudyAuditRequest(StudyAuditActions action) {

    if (action.getValue().equals(StudyAuditActions.AUDITTRAILS.getValue())) {
      ad = activityController.activityDetailOnStudyAudit(username,
              convertTosqlDate(from), convertTosqlDate(to));
    } else {
      MessagesController.addSecurityErrorMessage("Audit action not supported.");
    }
  }

  /**
   * Convert the GUI date to SQL format.
   * <p>
   * @param calendarDate
   * @return
   */
  public java.sql.Date convertTosqlDate(java.util.Date calendarDate) {
    return new java.sql.Date(calendarDate.getTime());
  }
}
