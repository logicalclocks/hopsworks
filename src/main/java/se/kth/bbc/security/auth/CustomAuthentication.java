package se.kth.bbc.security.auth;

import java.io.Serializable;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.ejb.EJB;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.RequestScoped;
import javax.faces.context.FacesContext;
import javax.mail.MessagingException;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import se.kth.bbc.lims.MessagesController;
import se.kth.bbc.security.audit.AuditManager;
import se.kth.bbc.security.audit.AuditUtil;
import se.kth.bbc.security.audit.LoginsAuditActions;
import se.kth.bbc.security.ua.BBCGroup;
import se.kth.bbc.security.ua.EmailBean;
import se.kth.bbc.security.ua.PeopleAccountStatus;
import se.kth.bbc.security.ua.UserAccountsEmailMessages;
import se.kth.bbc.security.ua.UserManager;
import se.kth.hopsworks.meta.exception.ApplicationException;
import se.kth.hopsworks.user.model.Users;

/**
 *
 * @author Ali Gholami <gholami@pdc.kth.se>
 */
@ManagedBean
@RequestScoped
public class CustomAuthentication implements Serializable {

  private static final long serialVersionUID = 1L;

  // Issuer of the QrCode
  public static final String ISSUER = "BiobankCloud";

  // To distinguish Yubikey users
  private final String YUBIKEY_USER_MARKER = "YUBIKEY_USER_MARKER";

  // For disabled OTP auth mode
  private final String YUBIKEY_OTP_PADDING
          = "EaS5ksRVErn2jiOmSQy5LM2X7LgWAZWfWYKQoPavbrhN";

  // For padding when password field is empty
  private final String MOBILE_OTP_PADDING = "123456";

  @EJB
  private UserManager mgr;

  @EJB
  private EmailBean emailBean;

  @EJB
  private AuditManager am;

  private String username;
  private String password;
  private String otpCode;
  private Users user;
  private int userid;

  public String getUsername() {
    return username;
  }

  public void setUsername(String username) {
    this.username = username;
  }

  public String getPassword() {
    return password;
  }

  public void setPassword(String password) {
    this.password = password;
  }

  public String getOtpCode() {
    return otpCode;
  }

  public void setOtpCode(String otpCode) {
    this.otpCode = otpCode;
  }


  /**
   * Authenticate the users using two factor mobile authentication.
   *
   * @return
   * @throws java.net.UnknownHostException
   * @throws java.net.SocketException
   */
  public String login() throws UnknownHostException, SocketException, ApplicationException {

    FacesContext ctx = FacesContext.getCurrentInstance();
    HttpServletRequest req = (HttpServletRequest) ctx.getExternalContext().
            getRequest();

    /*
     * Log out from the existing logged in user
     */
    if (req.getRemoteUser() != null) {
      return logout();
    }

    user = mgr.getUserByEmail(username);

    // Add padding if custom realm is disabled
    if (this.otpCode == null || this.otpCode.isEmpty()) {
      this.otpCode = MOBILE_OTP_PADDING;
    }

    // Return if username is wrong
    if (user == null) {
      MessagesController.addMessageToGrowl(
              AccountStatusErrorMessages.USER_NOT_FOUND);
      return ("");
    }

    // Retrun if user is not Mobile user     
    if (user.getMode() == PeopleAccountStatus.YUBIKEY_USER.getValue()) {
      MessagesController.addMessageToGrowl(
              AccountStatusErrorMessages.USER_NOT_FOUND);
      return ("");

    }
    // Return if user not activated
    if (user.getStatus() == PeopleAccountStatus.MOBILE_ACCOUNT_INACTIVE.
            getValue()) {
      MessagesController.addMessageToGrowl(
              AccountStatusErrorMessages.INACTIVE_ACCOUNT);
      return ("");
    }

    // Return if uses is bloked
    if (user.getStatus() == PeopleAccountStatus.ACCOUNT_BLOCKED.getValue()) {
      // Inform the use about the blocked account
      MessagesController.addMessageToGrowl(
              AccountStatusErrorMessages.BLOCKED_ACCOUNT);
      return ("");
    }

    // Return if user is deactivated
    if (user.getStatus() == PeopleAccountStatus.ACCOUNT_DEACTIVATED.getValue()) {
      // Inform the use about the account is deactivated
      MessagesController.addMessageToGrowl(
              AccountStatusErrorMessages.DEACTIVATED_ACCOUNT);
      return ("");
    }

    userid = user.getUid();

    try {
      // Concatenate the static password with the otp due to limitations of passing two passwords to glassfish
      req.login(this.username, this.password + this.otpCode);
      // Reset the lock for failed accounts
      mgr.resetLock(userid);
      // Set the onlne flag
      mgr.setOnline(userid, 1);

      registerLoginInfo(user, LoginsAuditActions.LOGIN.getValue(),
              "SUCCESS");

    } catch (ServletException ex) {
      // If more than five times block the account
      int val = user.getFalseLogin();
      mgr.increaseLockNum(userid, val + 1);

      registerLoginInfo(user, LoginsAuditActions.LOGIN.getValue(),
              "FAIL");

      if (val > 5) {
        mgr.changeAccountStatus(userid, "", PeopleAccountStatus.ACCOUNT_BLOCKED.
                getValue());
        try {
          emailBean.sendEmail(user.getEmail(),
                  UserAccountsEmailMessages.ACCOUNT_BLOCKED__SUBJECT,
                  UserAccountsEmailMessages.accountBlockedMessage());
        } catch (MessagingException ex1) {
          Logger.getLogger(CustomAuthentication.class.getName()).log(
                  Level.SEVERE, null, ex1);
          throw new ApplicationException("Could not register you now due to a failed service. Tray again later.");
        }

      }

      // Inform the use about invalid credentials
      MessagesController.addMessageToGrowl(
              AccountStatusErrorMessages.INCCORCT_CREDENTIALS);
      return ("");
    }

    // Reset the password after first login
    if (user.getStatus() == PeopleAccountStatus.ACCOUNT_PENDING.getValue()) {
      return ("reset");
    }

    return redirectUser(user);
  }

  public String yubikeyLogin() throws UnknownHostException, SocketException {

    FacesContext ctx = FacesContext.getCurrentInstance();
    HttpServletRequest req = (HttpServletRequest) ctx.getExternalContext().
            getRequest();

    /*
     * Log out from the existing logged in user
     */
    if (req.getRemoteUser() != null) {
      return logout();

    }

    user = mgr.getUserByEmail(username);

    // Return if username is wrong
    if (user == null) {
      MessagesController.addMessageToGrowl(
              AccountStatusErrorMessages.USER_NOT_FOUND);
      return ("");
    }

    // Retrun if user is not Yubikey user     
    if (user.getMode() != PeopleAccountStatus.YUBIKEY_USER.getValue()) {
      MessagesController.addMessageToGrowl(
              AccountStatusErrorMessages.USER_NOT_FOUND);
      return ("");
    }

    // Return if user not activated
    if (user.getStatus() == PeopleAccountStatus.YUBIKEY_ACCOUNT_INACTIVE.
            getValue()) {
      MessagesController.addMessageToGrowl(
              AccountStatusErrorMessages.INACTIVE_ACCOUNT);
      return ("");
    }

    // Return if user is bloked
    if (user.getStatus() == PeopleAccountStatus.ACCOUNT_BLOCKED.getValue()) {
      // Inform the use about the blocked account
      MessagesController.addMessageToGrowl(
              AccountStatusErrorMessages.BLOCKED_ACCOUNT);
      return ("");
    }

    // Return if uses is deactivated
    if (user.getStatus() == PeopleAccountStatus.ACCOUNT_DEACTIVATED.getValue()) {
      // Inform the use about the deactivated account
      MessagesController.addMessageToGrowl(
              AccountStatusErrorMessages.DEACTIVATED_ACCOUNT);
      return ("");
    }

    userid = user.getUid();

    // Add padding if custom realm is disabled
    if (this.otpCode == null || this.otpCode.isEmpty()) {
      this.otpCode = YUBIKEY_OTP_PADDING;
    }

    registerLoginInfo(user, LoginsAuditActions.LOGIN.getValue(),
            "SUCCESS");

    try {
      // Concatenate the static password with the otp due to limitations of passing two passwords to glassfish
      req.login(this.username, this.password + this.otpCode
              + this.YUBIKEY_USER_MARKER);
      // Reset the lock for failed accounts
      mgr.resetLock(userid);
      // Set the onlne flag
      mgr.setOnline(userid, 1);

    } catch (ServletException ex) {
      // If more than five times block the account
      int val = user.getFalseLogin();
      mgr.increaseLockNum(userid, val + 1);
      registerLoginInfo(user, LoginsAuditActions.LOGIN.getValue(),
              "FAIL");
      if (val > 5) {
        mgr.changeAccountStatus(userid, "", PeopleAccountStatus.ACCOUNT_BLOCKED.
                getValue());
        try {
          emailBean.sendEmail(user.getEmail(),
                  UserAccountsEmailMessages.ACCOUNT_BLOCKED__SUBJECT,
                  UserAccountsEmailMessages.accountBlockedMessage());
        } catch (MessagingException ex1) {
          Logger.getLogger(CustomAuthentication.class.getName()).log(
                  Level.SEVERE, null, ex1);
          return ("");
        }
      }

      // Inform the use about invalid credentials
      MessagesController.addMessageToGrowl(
              AccountStatusErrorMessages.INCCORCT_CREDENTIALS);
      return ("");
    }

    // Reset the password after first login
    if (user.getStatus() == PeopleAccountStatus.ACCOUNT_PENDING.getValue()) {
      return ("reset");
    }

    return redirectUser(user);
  }

  public String logout() throws SocketException {

    FacesContext ctx = FacesContext.getCurrentInstance();
    HttpSession sess = (HttpSession) ctx.getExternalContext().getSession(false);

    if (null != sess) {
      sess.invalidate();
    }

    String ip = AuditUtil.getIPAddress();
    String browser = AuditUtil.getBrowserInfo();
    String os = AuditUtil.getOSInfo();
    String macAddress = AuditUtil.getMacAddress(ip);

    am.registerLoginInfo(user, LoginsAuditActions.LOGOUT.getValue(), ip,
                    browser, os, macAddress, "SUCCESS");

    mgr.setOnline(userid, -1);

    return ("welcome");
  }

  public void registerLoginInfo(Users p, String action, String outcome) throws
          UnknownHostException, SocketException {

    String ip = AuditUtil.getIPAddress();
    String browser = AuditUtil.getBrowserInfo();
    String os = AuditUtil.getOSInfo();
    String macAddress = AuditUtil.getMacAddress(ip);

    am.registerLoginInfo(p, action, ip, browser, os, macAddress, outcome);

  }

  public boolean isInAdminRole(Users user) {
    return mgr.findGroups(user.getUid()).contains(BBCGroup.SYS_ADMIN.name());
  }

  public boolean isInDataProviderRole(Users user) {
    return mgr.findGroups(user.getUid()).contains(BBCGroup.BBC_ADMIN.name());
  }

  public boolean isInAuditorRole(Users user) {
    return mgr.findGroups(user.getUid()).contains(BBCGroup.AUDITOR.name());
  }

  public boolean isInResearcherRole(Users user) {
    return mgr.findGroups(user.getUid()).
            contains(BBCGroup.BBC_RESEARCHER.name());
  }

  public String redirectUser(Users user) {

    if (isInAdminRole(user)) {
      return "adminIndex";
    } else if (isInAuditorRole(user)) {
      return "adminAuditIndex";
    } else if (isInDataProviderRole(user)) {
      return "indexPage";
    } else if (isInResearcherRole(user)) {
      return "indexPage";
    }

    return "indexPage";
  }

}
