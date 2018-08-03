/*
 * Copyright (C) 2013 - 2018, Logical Clocks AB and RISE SICS AB. All rights reserved
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this
 * software and associated documentation files (the "Software"), to deal in the Software
 * without restriction, including without limitation the rights to use, copy, modify, merge,
 * publish, distribute, sublicense, and/or sell copies of the Software, and to permit
 * persons to whom the Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or
 * substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS  OR IMPLIED, INCLUDING
 * BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL  THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM,
 * DAMAGES OR  OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 *
 */

package io.hops.hopsworks.admin.user.account;

import io.hops.hopsworks.admin.maintenance.MessagesController;
import io.hops.hopsworks.common.constants.auth.AccountStatusErrorMessages;
import io.hops.hopsworks.common.dao.user.UserFacade;
import io.hops.hopsworks.common.util.EmailBean;
import java.io.Serializable;
import java.util.logging.Logger;
import javax.annotation.Resource;
import javax.ejb.EJB;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.SessionScoped;
import javax.faces.context.FacesContext;
import javax.mail.Message.RecipientType;
import javax.mail.MessagingException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import javax.transaction.HeuristicMixedException;
import javax.transaction.HeuristicRollbackException;
import javax.transaction.NotSupportedException;
import javax.transaction.RollbackException;
import javax.transaction.SystemException;
import javax.transaction.UserTransaction;

import io.hops.hopsworks.common.dao.user.Users;
import io.hops.hopsworks.common.dao.user.security.audit.AccountsAuditActions;
import io.hops.hopsworks.common.dao.user.security.audit.AccountAuditFacade;
import io.hops.hopsworks.common.dao.user.security.audit.UserAuditActions;
import io.hops.hopsworks.common.dao.user.security.ua.UserAccountStatus;
import io.hops.hopsworks.common.dao.user.security.ua.SecurityQuestion;
import io.hops.hopsworks.common.dao.user.security.ua.SecurityUtils;
import io.hops.hopsworks.common.dao.user.security.ua.UserAccountsEmailMessages;
import io.hops.hopsworks.common.exception.AppException;
import io.hops.hopsworks.common.metadata.exception.ApplicationException;
import io.hops.hopsworks.common.user.AuthController;
import io.hops.hopsworks.common.user.UsersController;
import java.io.IOException;
import java.util.logging.Level;
import javax.servlet.ServletException;

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
  private AccountAuditFacade auditManager;
  @EJB
  protected UsersController usersController;
  @EJB
  private UserFacade userFacade;
  @EJB
  private EmailBean emailBean;
  @EJB
  private AuthController authController;

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

  public String sendTmpPassword() throws MessagingException, AppException {
    FacesContext ctx = FacesContext.getCurrentInstance();
    HttpServletRequest req = (HttpServletRequest) ctx.getExternalContext().getRequest();
    people = userFacade.findByEmail(this.username);

    if (people == null || people.getStatus().equals(UserAccountStatus.DEACTIVATED_ACCOUNT)) {
      return ("password_sent");
    }
    
    try {
      if (!authController.validateSecurityQA(people, people.getSecurityQuestion().getValue(), answer, req)) {
        String mess = UserAccountsEmailMessages.buildWrongAnswerMessage();
        emailBean.sendEmail(people.getEmail(), RecipientType.TO, UserAccountsEmailMessages.ACCOUNT_PASSWORD_RESET, 
            mess);
        return ("password_sent");
      }

      // generate a radndom password
      String random_password = SecurityUtils.getRandomPassword(passwordLength);

      String mess = UserAccountsEmailMessages.buildPasswordResetMessage(random_password);

      userTransaction.begin();
      // make the account pending until it will be reset by user upon first login
      // mgr.updateStatus(people, UserAccountStatus.ACCOUNT_PENDING.getValue());
      // update the status of user to active

      people.setStatus(UserAccountStatus.ACTIVATED_ACCOUNT);

      // reset the old password with a new one
      authController.changePassword(people, random_password, req);

      userTransaction.commit();

      auditManager.registerAccountChange(people, AccountsAuditActions.PASSWORDCHANGE.name(),
          AccountsAuditActions.SUCCESS.name(), "Temporary Password Sent.", people, req);
      // sned the new password to the user email
      emailBean.sendEmail(people.getEmail(), RecipientType.TO,
          UserAccountsEmailMessages.ACCOUNT_PASSWORD_RESET, mess);

    } catch (RollbackException | HeuristicMixedException |
        HeuristicRollbackException | SecurityException |
        IllegalStateException | SystemException | NotSupportedException ex) {
      MessagesController.addSecurityErrorMessage("Technical Error!");
      return ("");
    } catch (Exception ex) {
      logger.log(Level.SEVERE, null, ex);
      return "";
    }

    return ("password_sent");
  }

  /**
   * Change password through profile.
   * <p>
   * @return
   */
  public String changePassword() throws AppException {
    FacesContext ctx = FacesContext.getCurrentInstance();
    HttpServletRequest req = (HttpServletRequest) ctx.getExternalContext().getRequest();
    if (req.getRemoteUser() == null) {
      return ("welcome");
    }
    people = userFacade.findByEmail(req.getRemoteUser());

    if (people.getStatus().equals(UserAccountStatus.DEACTIVATED_ACCOUNT)) {
      MessagesController.addSecurityErrorMessage("Inactive Account");
      return "";
    }

    try {

      // Reset the old password with a new one
      authController.changePassword(people, passwd1, req);

      try {
        usersController.updateStatus(people, UserAccountStatus.ACTIVATED_ACCOUNT);
      } catch (ApplicationException ex) {
        logger.log(Level.SEVERE, null, ex);
        return "";
      }

      // Send email    
      String message = UserAccountsEmailMessages.buildResetMessage();
      emailBean.sendEmail(people.getEmail(), RecipientType.TO,
          UserAccountsEmailMessages.ACCOUNT_PASSWORD_RESET, message);

      auditManager.registerAccountChange(people, UserAccountsEmailMessages.ACCOUNT_PASSWORD_RESET,
          UserAuditActions.SUCCESS.name(), "", people, req);
      return ("password_changed");
    } catch (MessagingException e) {
      MessagesController.addSecurityErrorMessage("Technical Error!");

      auditManager.registerAccountChange(people, UserAccountsEmailMessages.ACCOUNT_PASSWORD_RESET,
          UserAuditActions.FAILED.name(), "", people, req);
      return ("");

    } catch (Exception ex) {
      logger.log(Level.SEVERE, null, ex);
      return "";
    }
  }

  /**
   * Change security question in through profile.
   *
   * @return
   */
  public String changeSecQuestion() throws AppException {
    FacesContext ctx = FacesContext.getCurrentInstance();
    HttpServletRequest req = (HttpServletRequest) ctx.getExternalContext().getRequest();

    if (req.getRemoteUser() == null) {
      return ("welcome");
    }

    people = userFacade.findByEmail(req.getRemoteUser());
    if (people == null) {
      FacesContext context = FacesContext.getCurrentInstance();
      HttpSession session = (HttpSession) context.getExternalContext().getSession(false);
      session.invalidate();
      auditManager.registerAccountChange(people, AccountsAuditActions.SECQUESTION.name(),
          AccountsAuditActions.FAILED.name(), "", people, req);
      return ("welcome");
    }
    if (this.answer.isEmpty() || this.answer == null || this.current == null || this.current.isEmpty()) {
      MessagesController.addSecurityErrorMessage("No valid answer!");
      auditManager.registerAccountChange(people, AccountsAuditActions.SECQUESTION.name(),
          AccountsAuditActions.FAILED.name(), "", people, req);
      return ("");
    }

    try {
      if (authController.checkPasswordAndStatus(people, this.current, req)) {
        // update the security question
        authController.changeSecQA(people, people.getSecurityQuestion().getValue(), answer, req);
        // send email    
        String message = UserAccountsEmailMessages.buildSecResetMessage();
        emailBean.sendEmail(people.getEmail(), RecipientType.TO,
            UserAccountsEmailMessages.ACCOUNT_PROFILE_UPDATE, message);
        return ("sec_question_changed");
      } else {
        MessagesController.addSecurityErrorMessage(AccountStatusErrorMessages.INCORRECT_CREDENTIALS);
        auditManager.registerAccountChange(people, AccountsAuditActions.SECQUESTION.name(),
            AccountsAuditActions.FAILED.name(), "", people, req);
        return "";
      }
    } catch (MessagingException e) {
      MessagesController.addSecurityErrorMessage("Technical Error!");
      return ("");
    } catch (AppException ex) {
      MessagesController.addSecurityErrorMessage(ex.getMessage());
      return ("");
    }

  }

  /**
   * Get the user security question.
   *
   * @return
   */
  public String findQuestion() throws AppException {

    people = userFacade.findByEmail(this.username);
    if (people == null) {
      this.question = SecurityQuestion.randomQuestion();
      return ("reset_password");
    }

    if (people.getStatus().equals(UserAccountStatus.DEACTIVATED_ACCOUNT)) {
      this.question = SecurityQuestion.randomQuestion();
      return ("reset_password");
    }

    this.question = people.getSecurityQuestion();

    return ("reset_password");
  }

  public String deactivatedProfile() throws AppException {
    FacesContext ctx = FacesContext.getCurrentInstance();
    HttpServletRequest req = (HttpServletRequest) ctx.getExternalContext().
        getRequest();

    if (req.getRemoteUser() == null) {
      return ("welcome");
    }

    people = userFacade.findByEmail(req.getRemoteUser());

    try {

      // check the deactivation reason length
      if (this.notes.length() < 5 || this.notes.length() > 500) {
        MessagesController.addSecurityErrorMessage(AccountStatusErrorMessages.INCORRECT_DEACTIVATION_LENGTH);

        auditManager.registerAccountChange(people, UserAccountStatus.DEACTIVATED_ACCOUNT.name(),
            UserAuditActions.FAILED.name(), "", people, req);
      }

      if (authController.checkPasswordAndStatus(people, this.current, req)) {

        // close the account
        usersController.changeAccountStatus(people.getUid(), this.notes,
            UserAccountStatus.DEACTIVATED_ACCOUNT);
        // send email    
        String message = UserAccountsEmailMessages.buildSecResetMessage();
        emailBean.sendEmail(people.getEmail(), RecipientType.TO,
            UserAccountsEmailMessages.ACCOUNT_DEACTIVATED, message);

        auditManager.registerAccountChange(people, UserAccountStatus.DEACTIVATED_ACCOUNT.name(),
            UserAuditActions.FAILED.name(), "", people, req);
      } else {
        MessagesController.addSecurityErrorMessage(AccountStatusErrorMessages.INCORRECT_PASSWORD);

        auditManager.registerAccountChange(people, UserAccountStatus.DEACTIVATED_ACCOUNT.name(),
            UserAuditActions.FAILED.name(), "", people, req);
        return "";
      }
    } catch (MessagingException e) {
      auditManager.registerAccountChange(people, UserAccountStatus.DEACTIVATED_ACCOUNT.name(),
          UserAuditActions.FAILED.name(), "", people, req);
    } catch (AppException ex) {
      logger.log(Level.SEVERE, null, ex);
    }
    return logout();
  }

  public String changeProfilePassword() throws AppException {
    FacesContext ctx = FacesContext.getCurrentInstance();
    HttpServletRequest req = (HttpServletRequest) ctx.getExternalContext().getRequest();

    if (req.getRemoteUser() == null) {
      return ("welcome");
    }

    people = userFacade.findByEmail(req.getRemoteUser());

    if (people == null) {
      FacesContext context = FacesContext.getCurrentInstance();
      HttpSession session = (HttpSession) context.getExternalContext().
          getSession(false);
      session.invalidate();
      return ("welcome");
    }

    // Check the status to see if user is not blocked or deactivate
    if (people.getStatus() == UserAccountStatus.BLOCKED_ACCOUNT) {
      MessagesController.addSecurityErrorMessage(AccountStatusErrorMessages.BLOCKED_ACCOUNT);
      auditManager.registerAccountChange(people, AccountsAuditActions.PASSWORD.name(),
          AccountsAuditActions.FAILED.name(), "", people, req);
      return "";
    }

    if (people.getStatus() == UserAccountStatus.DEACTIVATED_ACCOUNT) {
      MessagesController.addSecurityErrorMessage(AccountStatusErrorMessages.DEACTIVATED_ACCOUNT);
      auditManager.registerAccountChange(people, AccountsAuditActions.PASSWORD.name(),
          AccountsAuditActions.FAILED.name(), "", people, req);
      return "";
    }

    if (passwd1 == null || passwd2 == null) {
      MessagesController.addSecurityErrorMessage("No Password Entry!");
      return ("");
    }

    try {

      if (authController.checkPasswordAndStatus(people, this.current, req))  {

        // reset the old password with a new one
        authController.changePassword(people, passwd1, req);

        // send email    
        String message = UserAccountsEmailMessages.buildResetMessage();
        emailBean.sendEmail(people.getEmail(), RecipientType.TO,
            UserAccountsEmailMessages.ACCOUNT_CONFIRMATION_SUBJECT, message);

        auditManager.registerAccountChange(people, AccountsAuditActions.PASSWORD.name(),
            AccountsAuditActions.SUCCESS.name(), "", people, req);
        return ("profile_password_changed");
      } else {
        MessagesController.addSecurityErrorMessage(AccountStatusErrorMessages.INCORRECT_CREDENTIALS);
        auditManager.registerAccountChange(people, AccountsAuditActions.PASSWORD.name(),
            AccountsAuditActions.FAILED.name(), "", people, req);
        return "";
      }
    } catch (MessagingException ex) {
      MessagesController.addSecurityErrorMessage("Email Technical Error!");

      auditManager.registerAccountChange(people, AccountsAuditActions.PASSWORD.name(),
          AccountsAuditActions.FAILED.name(), "", people, req);
      return ("");
    } catch (Exception ex) {
      logger.log(Level.SEVERE, null, ex);
      return ("");
    }
  }

  public String logout() {

    // Logout user
    FacesContext context = FacesContext.getCurrentInstance();

    HttpServletRequest req = (HttpServletRequest) context.getExternalContext().
        getRequest();

    HttpSession session = (HttpSession) context.getExternalContext().getSession(
        false);

    if (req.getRemoteUser() == null) {
      return ("welcome");
    }

    try {
      people = userFacade.findByEmail(req.getRemoteUser());
      auditManager.registerLoginInfo(people, UserAuditActions.LOGOUT.toString(),
          UserAuditActions.SUCCESS.toString(), req);
      req.logout();
      session.invalidate();
      usersController.setOnline(people.getUid(), -1);
      context.getExternalContext().redirect("/hopsworks/#!/home");
    } catch (IOException | ServletException | AppException ex) {
      logger.log(Level.SEVERE, null, ex);
    }
    return ("welcome");
  }

  public String returnMenu() {
    HttpSession sess = (HttpSession)
        FacesContext.getCurrentInstance().getExternalContext().getSession(false);
    if (sess != null) {
      sess.invalidate();
    }
    return ("welcome");
  }
}
