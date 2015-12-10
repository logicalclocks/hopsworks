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
import se.kth.bbc.activity.Activity;
import se.kth.bbc.activity.ActivityFacade;
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
  private ActivityFacade activityController;

  private String username;

  private Date from;

  private Date to;

  private AccountsAuditActions selectedAccountsAuditAction;

  private RolesAuditActions selectdeRolesAuditAction;

  private StudyAuditActions selectedStudyAuditAction;

  private UserAuditActions selectedLoginsAuditAction;

  private List<Userlogins> userLogins;

  private List<RolesAudit> roleAudit;

  private List<AccountAudit> accountAudit;

  private List<Activity> ad;

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

  public UserAuditActions[] getLoginsAuditActions() {
    return UserAuditActions.values();
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

  public UserAuditActions getSelectedLoginsAuditAction() {
    return selectedLoginsAuditAction;
  }

  public void setSelectedLoginsAuditAction(
          UserAuditActions selectedLoginsAuditAction) {
    this.selectedLoginsAuditAction = selectedLoginsAuditAction;
  }

  public List<Activity> getAd() {
    return ad;
  }

  public void setAd(List<Activity> ad) {
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
    } else if (action.equals(RolesAuditActions.SUCCESS.name()) || action.equals(
            RolesAuditActions.FAILED.name())) {
      return auditManager.getRoletAuditOutcome(from, to, action);
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
    } else if (action.equals(UserAuditActions.SUCCESS.name()) || action.equals(
            UserAuditActions.FAILED.name())
            || action.equals(UserAuditActions.ABORTED.name())) {
      return auditManager.getUserLoginsOutcome(u.getUid(),
              convertTosqlDate(from),
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
  public void processLoginAuditRequest(UserAuditActions action) {

    if (action.getValue().equals(UserAuditActions.REGISTRATION.getValue())) {
      userLogins = getUserLogins(username, from, to, action.getValue());
    } else if (action.getValue().equals(UserAuditActions.LOGIN.
            getValue()) || action.getValue().equals(UserAuditActions.LOGOUT.
                    getValue())) {
      userLogins = getUserLogins(username, from, to, action.getValue());
    } else if (action.getValue().equals(UserAuditActions.SUCCESS.
            getValue()) || action.getValue().equals(UserAuditActions.FAILED.
                    getValue())
            || action.getValue().equals(UserAuditActions.ABORTED.
                    getValue())) {
      userLogins = getUserLogins(username, from, to, action.getValue());
    } else if (action.getValue().equals(UserAuditActions.QRCODE.
            getValue()) || action.getValue().equals(UserAuditActions.RECOVERY.
                    getValue())) {
      userLogins = getUserLogins(username, from, to, action.getValue());
    } else if (action.getValue().equals(UserAuditActions.ALL.getValue())) {
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
    } else if (action.getValue().equals(AccountsAuditActions.ALL.
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

    if (action.getValue().equals(RolesAuditActions.ADDROLE.getValue())) {
      roleAudit = getRoleAudit(username, from, to, action.getValue());
    } else if (action.getValue().equals(RolesAuditActions.REMOVEROLE.getValue())) {
      roleAudit = getRoleAudit(username, from, to, action.getValue());
    } else if (action.getValue().equals(RolesAuditActions.ALLROLEASSIGNMENTS.
            getValue())) {
      roleAudit = getRoleAudit(username, from, to, action.getValue());
    } else if (action.getValue().equals(RolesAuditActions.SUCCESS) || action.
            getValue().equals(RolesAuditActions.FAILED)) {
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
