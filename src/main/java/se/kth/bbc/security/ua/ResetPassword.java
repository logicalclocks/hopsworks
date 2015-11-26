  /*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package se.kth.bbc.security.ua;

import java.io.Serializable;
import java.io.UnsupportedEncodingException;
import java.net.SocketException;
import java.security.NoSuchAlgorithmException;
import java.util.logging.Logger;
import javax.annotation.Resource;
import javax.ejb.EJB;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.ManagedProperty;
import javax.faces.bean.SessionScoped;
import javax.faces.context.FacesContext;
import javax.mail.MessagingException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import javax.transaction.HeuristicMixedException;
import javax.transaction.HeuristicRollbackException;
import javax.transaction.NotSupportedException;
import javax.transaction.RollbackException;
import javax.transaction.SystemException;
import javax.transaction.UserTransaction;
import se.kth.bbc.lims.ClientSessionState;
import se.kth.bbc.lims.MessagesController;
import se.kth.bbc.security.audit.AccountsAuditActions;
import se.kth.bbc.security.audit.AuditManager;
import se.kth.bbc.security.audit.AuditUtil;
import se.kth.bbc.security.audit.LoginsAuditActions;
import se.kth.bbc.security.auth.AccountStatusErrorMessages;
import se.kth.hopsworks.user.model.Users;

/**
 *
 * @author Ali Gholami <gholami@pdc.kth.se>
 */
@ManagedBean
@SessionScoped
public class ResetPassword implements Serializable {

  private static final long serialVersionUID = 1L;

  private static final Logger logger = Logger.getLogger(ResetPassword.class.
          getName());

  private String username;
  private String passwd1;
  private String passwd2;
  private String current;
  private SecurityQuestion question;
  private Users people;

  private String answer;

  private String notes;

  private final int passwordLength = 6;

  @EJB
  private AuditManager auditManager;

  @ManagedProperty(value = "#{clientSessionState}")
  private ClientSessionState sessionState;

  @EJB
  private UserManager mgr;

  @EJB
  private EmailBean emailBean;

  @Resource
  private UserTransaction userTransaction;

  public String getNotes() {
    return notes;
  }

  public void setNotes(String notes) {
    this.notes = notes;
  }

  public SecurityQuestion[] getQuestions() {
    return SecurityQuestion.values();
  }

  public SecurityQuestion getQuestion() {
    return question;
  }

  public void setQuestion(SecurityQuestion question) {
    this.question = question;
  }

  public void setSessionState(ClientSessionState sessionState) {
    this.sessionState = sessionState;
  }

  public String getCurrent() {
    return current;
  }

  public void setCurrent(String current) {
    this.current = current;
  }

  public Users getPeople() {
    return people;
  }

  public void setPeople(Users people) {
    this.people = people;
  }

  public String getAnswer() {
    return answer;
  }

  public void setAnswer(String answer) {
    this.answer = answer;
  }

  public String getUsername() {
    return username;
  }

  public void setUsername(String username) {
    this.username = username;
  }

  public String getPasswd1() {
    return passwd1;
  }

  public void setPasswd1(String passwd1) {
    this.passwd1 = passwd1;
  }

  public String getPasswd2() {
    return passwd2;
  }

  public void setPasswd2(String passwd2) {
    this.passwd2 = passwd2;
  }

  public String sendTmpPassword() throws MessagingException {

    people = mgr.getUserByEmail(this.username);

    try {

      if (!SecurityUtils.converToSHA256(answer).equals(people.
              getSecurityAnswer())) {

        MessagesController.addSecurityErrorMessage(
                AccountStatusErrorMessages.INVALID_SEQ_ANSWER);

        // Lock the account if n tmies wrong answer  
        int val = people.getFalseLogin();
        mgr.increaseLockNum(people.getUid(), val + 1);
        if (val > Users.ALLOWED_FALSE_LOGINS) {
          mgr.changeAccountStatus(people.getUid(), "",
                  PeopleAccountStatus.ACCOUNT_DEACTIVATED.getValue());
          return ("welcome");
        }
        return "";
      }

      // generate a radndom password
      String random_password = SecurityUtils.getRandomString(passwordLength);

    
     String mess = UserAccountsEmailMessages.buildPasswordResetMessage(
              random_password);
    
      userTransaction.begin();
      // make the account pending until it will be reset by user upon first login
      // mgr.updateStatus(people, PeopleAccountStatus.ACCOUNT_PENDING.getValue());
        // update the status of user to active
      people.setStatus(PeopleAccountStatus.ACCOUNT_ACTIVE.getValue());
      
      // reset the old password with a new one
      mgr.resetPassword(people, SecurityUtils.converToSHA256(random_password));

      userTransaction.commit();

      // sned the new password to the user email
      emailBean.sendEmail(people.getEmail(),
              UserAccountsEmailMessages.ACCOUNT_PASSWORD_RESET, mess);

    } catch (UnsupportedEncodingException | NoSuchAlgorithmException ex) {
      MessagesController.addSecurityErrorMessage("Technical Error!");
      return ("");
    } catch (RollbackException | HeuristicMixedException |
            HeuristicRollbackException | SecurityException |
            IllegalStateException | SystemException | NotSupportedException ex) {
      MessagesController.addSecurityErrorMessage("Technical Error!");
      return ("");
    }

    return ("password_sent");
  }

  /**
   * Change password through profile.
   * <p>
   * @return
   */
  public String changePassword() throws SocketException {
    FacesContext ctx = FacesContext.getCurrentInstance();
    HttpServletRequest req = (HttpServletRequest) ctx.getExternalContext().
            getRequest();

    if (req.getRemoteUser() == null) {
      return ("welcome");
    }

    people = mgr.getUserByEmail(req.getRemoteUser());

    if (people == null) {
      FacesContext context = FacesContext.getCurrentInstance();
      HttpSession session = (HttpSession) context.getExternalContext().
              getSession(false);
      session.invalidate();
      auditManager.registerAccountChange(people,
              AccountsAuditActions.PASSWORDCHANGE.getValue(),
              AuditUtil.getIPAddress(), AuditUtil.getBrowserInfo(), AuditUtil.
              getOSInfo(), AuditUtil.getMacAddress(AuditUtil.getIPAddress()),
              "FAIL", "PASSWORD CHANGE");

      return ("welcome");
    }

    if (people.getStatus() == PeopleAccountStatus.ACCOUNT_DEACTIVATED.getValue()) {
      MessagesController.addSecurityErrorMessage("Inactive Account");
      return "";
    }

    try {

      // Reset the old password with a new one
      mgr.resetPassword(people, SecurityUtils.converToSHA256(passwd1));

      mgr.updateStatus(people, PeopleAccountStatus.ACCOUNT_ACTIVE.getValue());

      // Send email    
      String message = UserAccountsEmailMessages.buildResetMessage();
      emailBean.sendEmail(people.getEmail(),
              UserAccountsEmailMessages.ACCOUNT_PASSWORD_RESET, message);

      auditManager.registerAccountChange(people,
              AccountsAuditActions.PASSWORDCHANGE.getValue(),
              AuditUtil.getIPAddress(), AuditUtil.getBrowserInfo(), AuditUtil.
              getOSInfo(), AuditUtil.getMacAddress(AuditUtil.getIPAddress()),
              "SUCCESS", "PASSWORD CHANGE");

      // logout user
      FacesContext context = FacesContext.getCurrentInstance();
      HttpSession session = (HttpSession) context.getExternalContext().
              getSession(false);
      session.invalidate();

      return ("password_changed");
    } catch (NoSuchAlgorithmException | UnsupportedEncodingException |
            MessagingException ex) {
      MessagesController.addSecurityErrorMessage("Technical Error!");

      auditManager.registerAccountChange(people,
              AccountsAuditActions.PASSWORDCHANGE.getValue(),
              AuditUtil.getIPAddress(), AuditUtil.getBrowserInfo(), AuditUtil.
              getOSInfo(), AuditUtil.getMacAddress(AuditUtil.getIPAddress()),
              "FAIL", "PASSWORD CHANGE");
      return ("");

    }
  }

  /**
   * Change security question in through profile.
   *
   * @return
   */
  public String changeSecQuestion() throws SocketException {
    FacesContext ctx = FacesContext.getCurrentInstance();
    HttpServletRequest req = (HttpServletRequest) ctx.getExternalContext().
            getRequest();

    if (req.getRemoteUser() == null) {
      return ("welcome");
    }

    people = mgr.getUserByEmail(req.getRemoteUser());

    if (this.answer.isEmpty() || this.answer == null || this.current == null
            || this.current.isEmpty()) {
      MessagesController.addSecurityErrorMessage("No valid answer!");

      auditManager.registerAccountChange(people,
              AccountsAuditActions.SECQUESTIONCHANGE.getValue(),
              AuditUtil.getIPAddress(), AuditUtil.getBrowserInfo(), AuditUtil.
              getOSInfo(), AuditUtil.getMacAddress(AuditUtil.getIPAddress()),
              "FAIL", "CHNAGED SEC QUESTION");

      return ("");
    }

    if (people == null) {
      FacesContext context = FacesContext.getCurrentInstance();
      HttpSession session = (HttpSession) context.getExternalContext().
              getSession(false);
      session.invalidate();

      auditManager.registerAccountChange(people,
              AccountsAuditActions.SECQUESTIONCHANGE.getValue(),
              AuditUtil.getIPAddress(), AuditUtil.getBrowserInfo(), AuditUtil.
              getOSInfo(), AuditUtil.getMacAddress(AuditUtil.getIPAddress()),
              "FAIL", "CHNAGED SEC QUESTION");
      return ("welcome");
    }

    // Check the status to see if user is not blocked or deactivate
    if (people.getStatus() == PeopleAccountStatus.ACCOUNT_BLOCKED.getValue()) {
      MessagesController.addSecurityErrorMessage(
              AccountStatusErrorMessages.BLOCKED_ACCOUNT);

      auditManager.registerAccountChange(people,
              AccountsAuditActions.SECQUESTIONCHANGE.getValue(),
              AuditUtil.getIPAddress(), AuditUtil.getBrowserInfo(), AuditUtil.
              getOSInfo(), AuditUtil.getMacAddress(AuditUtil.getIPAddress()),
              "FAIL", "CHNAGED SEC QUESTION");

      return "";
    }

    if (people.getStatus() == PeopleAccountStatus.ACCOUNT_DEACTIVATED.getValue()) {
      MessagesController.addSecurityErrorMessage(
              AccountStatusErrorMessages.DEACTIVATED_ACCOUNT);
      auditManager.registerAccountChange(people,
              AccountsAuditActions.SECQUESTIONCHANGE.getValue(),
              AuditUtil.getIPAddress(), AuditUtil.getBrowserInfo(), AuditUtil.
              getOSInfo(), AuditUtil.getMacAddress(AuditUtil.getIPAddress()),
              "FAIL", "CHNAGED SEC QUESTION");

      return "";
    }

    try {
      if (SecurityUtils.converToSHA256(this.current).
              equals(people.getPassword())) {

        // update the security question
        mgr.resetSecQuestion(people.getUid(), question, SecurityUtils.
                converToSHA256(this.answer));

        // send email    
        String message = UserAccountsEmailMessages.buildSecResetMessage();
        emailBean.sendEmail(people.getEmail(),
                UserAccountsEmailMessages.ACCOUNT_PROFILE_UPDATE, message);

        auditManager.registerAccountChange(people,
                AccountsAuditActions.SECQUESTIONCHANGE.getValue(),
                AuditUtil.getIPAddress(), AuditUtil.getBrowserInfo(), AuditUtil.
                getOSInfo(), AuditUtil.getMacAddress(AuditUtil.getIPAddress()),
                "SUCCESS", "CHNAGED SEC QUESTION");

        return ("sec_question_changed");
      } else {
        MessagesController.addSecurityErrorMessage(
                AccountStatusErrorMessages.INCCORCT_CREDENTIALS);

        auditManager.registerAccountChange(people,
                AccountsAuditActions.SECQUESTIONCHANGE.getValue(),
                AuditUtil.getIPAddress(), AuditUtil.getBrowserInfo(), AuditUtil.
                getOSInfo(), AuditUtil.getMacAddress(AuditUtil.getIPAddress()),
                "FAIL", "CHNAGED SEC QUESTION");

        return "";
      }
    } catch (NoSuchAlgorithmException | UnsupportedEncodingException |
            MessagingException ex) {
      MessagesController.addSecurityErrorMessage("Technical Error!");
      return ("");
    }

  }

  /**
   * Get the user security question.
   *
   * @return
   */
  public String findQuestion() {

    people = mgr.getUserByEmail(this.username);
    if (people == null) {
      MessagesController.addSecurityErrorMessage(
              AccountStatusErrorMessages.USER_NOT_FOUND);
      return "";
    }

    if (people.getStatus() == PeopleAccountStatus.ACCOUNT_DEACTIVATED.getValue()) {
      MessagesController.addSecurityErrorMessage(
              AccountStatusErrorMessages.DEACTIVATED_ACCOUNT);
      return "";
    }

    this.question = people.getSecurityQuestion();

    return ("reset_password");
  }

  public String deactivatedProfile() throws SocketException {
    FacesContext ctx = FacesContext.getCurrentInstance();
    HttpServletRequest req = (HttpServletRequest) ctx.getExternalContext().
            getRequest();

    if (req.getRemoteUser() == null) {
      return ("welcome");
    }

    people = mgr.getUserByEmail(req.getRemoteUser());

    try {

      // check the deactivation reason length
      if (this.notes.length() < 5 || this.notes.length() > 500) {
        MessagesController.addSecurityErrorMessage(
                AccountStatusErrorMessages.INCCORCT_DEACTIVATION_LENGTH);

        auditManager.registerAccountChange(people,
                AccountsAuditActions.USERMANAGEMENT.getValue(),
                AuditUtil.getIPAddress(), AuditUtil.getBrowserInfo(), AuditUtil.
                getOSInfo(), AuditUtil.getMacAddress(AuditUtil.getIPAddress()),
                "FAIL", "ACCOUNT DEACTIVATION");

      }

      if (SecurityUtils.converToSHA256(this.current).
              equals(people.getPassword())) {

        // close the account
        mgr.changeAccountStatus(people.getUid(), this.notes,
                PeopleAccountStatus.ACCOUNT_DEACTIVATED.getValue());
        // send email    
        String message = UserAccountsEmailMessages.buildSecResetMessage();
        emailBean.sendEmail(people.getEmail(),
                UserAccountsEmailMessages.ACCOUNT_DEACTIVATED, message);

        auditManager.registerAccountChange(people,
                AccountsAuditActions.USERMANAGEMENT.getValue(),
                AuditUtil.getIPAddress(), AuditUtil.getBrowserInfo(), AuditUtil.
                getOSInfo(), AuditUtil.getMacAddress(AuditUtil.getIPAddress()),
                "SUCCESS", "ACCOUNT DEACTIVATION");
      } else {
        MessagesController.addSecurityErrorMessage(
                AccountStatusErrorMessages.INCCORCT_PASSWORD);

        auditManager.registerAccountChange(people,
                AccountsAuditActions.USERMANAGEMENT.getValue(),
                AuditUtil.getIPAddress(), AuditUtil.getBrowserInfo(), AuditUtil.
                getOSInfo(), AuditUtil.getMacAddress(AuditUtil.getIPAddress()),
                "FAIL", "ACCOUNT DEACTIVATION");
        return "";
      }
    } catch (NoSuchAlgorithmException | UnsupportedEncodingException |
            MessagingException ex) {

      auditManager.registerAccountChange(people,
              AccountsAuditActions.USERMANAGEMENT.getValue(),
              AuditUtil.getIPAddress(), AuditUtil.getBrowserInfo(), AuditUtil.
              getOSInfo(), AuditUtil.getMacAddress(AuditUtil.getIPAddress()),
              "FAIL", "ACCOUNT DEACTIVATION");
    }
    return logout();
  }

  public String changeProfilePassword() throws SocketException {
    FacesContext ctx = FacesContext.getCurrentInstance();
    HttpServletRequest req = (HttpServletRequest) ctx.getExternalContext().
            getRequest();

    if (req.getRemoteUser() == null) {
      return ("welcome");
    }

    people = mgr.getUserByEmail(req.getRemoteUser());

    if (people == null) {
      FacesContext context = FacesContext.getCurrentInstance();
      HttpSession session = (HttpSession) context.getExternalContext().
              getSession(false);
      session.invalidate();
      return ("welcome");
    }

    // Check the status to see if user is not blocked or deactivate
    if (people.getStatus() == PeopleAccountStatus.ACCOUNT_BLOCKED.getValue()) {
      MessagesController.addSecurityErrorMessage(
              AccountStatusErrorMessages.BLOCKED_ACCOUNT);

      auditManager.registerAccountChange(people,
              AccountsAuditActions.PASSWORDCHANGE.getValue(),
              AuditUtil.getIPAddress(), AuditUtil.getBrowserInfo(), AuditUtil.
              getOSInfo(), AuditUtil.getMacAddress(AuditUtil.getIPAddress()),
              "FAIL", "PASSWORD RESET");

      return "";
    }

    if (people.getStatus() == PeopleAccountStatus.ACCOUNT_DEACTIVATED.getValue()) {
      MessagesController.addSecurityErrorMessage(
              AccountStatusErrorMessages.DEACTIVATED_ACCOUNT);

      auditManager.registerAccountChange(people,
              AccountsAuditActions.PASSWORDCHANGE.getValue(),
              AuditUtil.getIPAddress(), AuditUtil.getBrowserInfo(), AuditUtil.
              getOSInfo(), AuditUtil.getMacAddress(AuditUtil.getIPAddress()),
              "FAIL", "PASSWORD RESET");

      return "";
    }

    if (passwd1 == null || passwd2 == null) {
      MessagesController.addSecurityErrorMessage("No Password Entry!");
      return ("");
    }

    try {

      if (SecurityUtils.converToSHA256(current).equals(people.getPassword())) {

        // reset the old password with a new one
        mgr.resetPassword(people, SecurityUtils.converToSHA256(passwd1));

        // send email    
        String message = UserAccountsEmailMessages.buildResetMessage();
        emailBean.sendEmail(people.getEmail(),
                UserAccountsEmailMessages.ACCOUNT_CONFIRMATION_SUBJECT, message);

        auditManager.registerAccountChange(people,
                AccountsAuditActions.PASSWORDCHANGE.getValue(),
                AuditUtil.getIPAddress(), AuditUtil.getBrowserInfo(), AuditUtil.
                getOSInfo(), AuditUtil.getMacAddress(AuditUtil.getIPAddress()),
                "SUCCESS", "PASSWORD RESET");

        return ("profile_password_changed");
      } else {
        MessagesController.addSecurityErrorMessage(
                AccountStatusErrorMessages.INCCORCT_CREDENTIALS);

        auditManager.registerAccountChange(people,
                AccountsAuditActions.PASSWORDCHANGE.getValue(),
                AuditUtil.getIPAddress(), AuditUtil.getBrowserInfo(), AuditUtil.
                getOSInfo(), AuditUtil.getMacAddress(AuditUtil.getIPAddress()),
                "FAIL", "PASSWORD RESET");

        return "";
      }
    } catch (NoSuchAlgorithmException | UnsupportedEncodingException |
            MessagingException ex) {
      MessagesController.addSecurityErrorMessage("Email Technical Error!");

      auditManager.registerAccountChange(people,
              AccountsAuditActions.PASSWORDCHANGE.getValue(),
              AuditUtil.getIPAddress(), AuditUtil.getBrowserInfo(), AuditUtil.
              getOSInfo(), AuditUtil.getMacAddress(AuditUtil.getIPAddress()),
              "FAIL", "PASSWORD RESET");

      return ("");
    }
  }

  public String logout() throws SocketException {

    // Logout user
    FacesContext context = FacesContext.getCurrentInstance();

    HttpServletRequest req = (HttpServletRequest) context.getExternalContext().
            getRequest();

    HttpSession session = (HttpSession) context.getExternalContext().getSession(
            false);

    if (req.getRemoteUser() == null) {
      return ("welcome");
    }

    people = mgr.getUserByEmail(req.getRemoteUser());

    String ip = AuditUtil.getIPAddress();
    String browser = AuditUtil.getBrowserInfo();
    String os = AuditUtil.getOSInfo();
    String macAddress = AuditUtil.getMacAddress(ip);

    auditManager.registerLoginInfo(people, LoginsAuditActions.LOGOUT.getValue(),
            ip, browser, os, macAddress, "SUCCESS");

    session.invalidate();

    mgr.setOnline(people.getUid(), -1);
    return ("welcome");
  }

}
