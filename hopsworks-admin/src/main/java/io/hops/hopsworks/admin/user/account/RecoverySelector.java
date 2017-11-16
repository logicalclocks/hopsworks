package io.hops.hopsworks.admin.user.account;

import com.google.zxing.WriterException;
import io.hops.hopsworks.admin.lims.MessagesController;
import io.hops.hopsworks.common.constants.auth.AccountStatusErrorMessages;
import io.hops.hopsworks.common.constants.auth.AuthenticationConstants;
import io.hops.hopsworks.common.dao.user.UserFacade;
import io.hops.hopsworks.common.util.EmailBean;
import java.io.IOException;
import java.io.Serializable;
import java.net.SocketException;
import java.security.NoSuchAlgorithmException;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.ejb.EJB;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.SessionScoped;
import javax.faces.context.FacesContext;
import javax.mail.Message.RecipientType;
import javax.mail.MessagingException;
import javax.servlet.http.HttpSession;
import org.apache.commons.codec.digest.DigestUtils;
import org.primefaces.model.StreamedContent;
import io.hops.hopsworks.common.dao.user.Users;
import io.hops.hopsworks.common.dao.user.security.audit.AccountsAuditActions;
import io.hops.hopsworks.common.dao.user.security.audit.AccountAuditFacade;
import io.hops.hopsworks.common.dao.user.security.ua.PeopleAccountStatus;
import io.hops.hopsworks.common.dao.user.security.ua.PeopleAccountType;
import io.hops.hopsworks.common.dao.user.security.ua.SecurityUtils;
import io.hops.hopsworks.common.dao.user.security.ua.UserAccountsEmailMessages;
import io.hops.hopsworks.common.user.UsersController;
import io.hops.hopsworks.common.util.QRCodeGenerator;
import org.primefaces.model.DefaultStreamedContent;

@ManagedBean
@SessionScoped
public class RecoverySelector implements Serializable {

  private static final long serialVersionUID = 1L;

  @EJB
  protected UsersController usersController;
  @EJB
  private UserFacade userFacade;

  @EJB
  private EmailBean email;

  @EJB
  private AccountAuditFacade am;

  private Users people;

  private String console;

  // Quick response code URL
  private String qrUrl = "Pass";
  private StreamedContent qrCode = null;

  private String uname;
  private String tmpCode;
  private String passwd;

  private final int passwordLength = AuthenticationConstants.PASSWORD_MIN_LENGTH;

  private int qrEnabled = -1;

  public int getQrEnabled() {
    return qrEnabled;
  }

  public void setQrEnabled(int qrEnabled) {
    this.qrEnabled = qrEnabled;
  }

  public String getQrUrl() {
    return qrUrl;
  }

  public void setQrUrl(String qrUrl) {
    this.qrUrl = qrUrl;
  }

  public StreamedContent getQrCode() {
    return qrCode;
  }

  public void setQrCode(StreamedContent qrCode) {
    this.qrCode = qrCode;
  }

  public String getUname() {
    return uname;
  }

  public void setUname(String uname) {
    this.uname = uname;
  }

  public String getTmpCode() {
    return tmpCode;
  }

  public void setTmpCode(String tmpCode) {
    this.tmpCode = tmpCode;
  }

  public String getPasswd() {
    return passwd;
  }

  public void setPasswd(String passwd) {
    this.passwd = passwd;
  }

  public String getConsole() {
    return console;
  }

  public void setConsole(String console) {
    this.console = console;
  }

  public String redirect() {

    if (console.equals("Password")) {
      return "sec_question";
    }

    if (console.equals("Mobile")) {
      return "mobile_recovery";
    }

    if (console.equals("Yubikey")) {
      return "yubikey_recovery";
    }

    return "";
  }

  /**
   * Register lost mobile lost device.
   * <p>
   * @return
   * @throws SocketException
   */
  public String sendQrCode() throws SocketException {

    people = userFacade.findByEmail(this.uname);

    try {

      if (people != null && people.getPassword().equals(DigestUtils.sha256Hex(passwd))) {

        // Check the status to see if user is not blocked or deactivate
        if (people.getStatus().equals(PeopleAccountStatus.BLOCKED_ACCOUNT)) {
          MessagesController.addSecurityErrorMessage(
              AccountStatusErrorMessages.BLOCKED_ACCOUNT);

          am.registerAccountChange(people, AccountsAuditActions.RECOVERY.name(),
              AccountsAuditActions.FAILED.name(), "", people);

          return "";
        }

        if (people.getStatus().equals(PeopleAccountStatus.DEACTIVATED_ACCOUNT)) {
          MessagesController.addSecurityErrorMessage(
              AccountStatusErrorMessages.DEACTIVATED_ACCOUNT);
          am.registerAccountChange(people, AccountsAuditActions.RECOVERY.name(),
              AccountsAuditActions.FAILED.name(), "", people);
          return "";
        }

        if (people.getMode().equals(PeopleAccountType.Y_ACCOUNT_TYPE)) {
          MessagesController.addSecurityErrorMessage(
              AccountStatusErrorMessages.YUBIKEY);
          am.registerAccountChange(people, AccountsAuditActions.RECOVERY.name(),
              AccountsAuditActions.FAILED.name(), "", people);

          return "";
        }

        // generate a randome secret of legth 6
        String random = SecurityUtils.getRandomPassword(passwordLength);
        usersController.updateSecret(people.getUid(), random);
        String message = UserAccountsEmailMessages.buildTempResetMessage(random);
        email.sendEmail(people.getEmail(), RecipientType.TO,
            UserAccountsEmailMessages.ACCOUNT_PASSWORD_RESET, message);

        am.registerAccountChange(people, AccountsAuditActions.RECOVERY.name(),
            AccountsAuditActions.SUCCESS.name(), "Reset QR code.", people);

        return "validate_code";
      } else {
        MessagesController.addSecurityErrorMessage(
            AccountStatusErrorMessages.INCCORCT_CREDENTIALS);
        if (people != null) {
          am.registerAccountChange(people, AccountsAuditActions.RECOVERY.name(),
              AccountsAuditActions.FAILED.name(), "", people);
        }
        return "";
      }
    } catch (MessagingException ex) {
      am.registerAccountChange(people, AccountsAuditActions.RECOVERY.name(),
          AccountsAuditActions.FAILED.name(), "", people);

    }

    MessagesController.addSecurityErrorMessage(
        AccountStatusErrorMessages.INTERNAL_ERROR);
    return "";
  }

  /**
   * Validate the temp code sent to user to reset the account.
   * <p>
   * @return
   */
  public String validateTmpCode() {
    qrCode = null;
    people = userFacade.findByEmail(this.uname);

    if (people == null) {
      MessagesController.addSecurityErrorMessage(
              AccountStatusErrorMessages.USER_NOT_FOUND);
      return "";
    }

    // Check the status to see if user is not blocked or deactivate
    if (people.getStatus().equals(PeopleAccountStatus.BLOCKED_ACCOUNT)) {
      MessagesController.addSecurityErrorMessage(
              AccountStatusErrorMessages.BLOCKED_ACCOUNT);
      return "";
    }

    if (people.getStatus().equals(PeopleAccountStatus.DEACTIVATED_ACCOUNT)) {
      MessagesController.addSecurityErrorMessage(
              AccountStatusErrorMessages.DEACTIVATED_ACCOUNT);
      return "";
    }

    if (people.getSecret() == null ? tmpCode == null : people.getSecret().
            equals(this.tmpCode)) {

      try {
        String otpSecret = SecurityUtils.calculateSecretKey();

        usersController.updateSecret(people.getUid(), otpSecret);
        qrCode = new DefaultStreamedContent(QRCodeGenerator.getQRCode(people.
                getEmail(),
                AuthenticationConstants.ISSUER, otpSecret), "image/png");

        qrEnabled = 1;
        am.registerAccountChange(people, AccountsAuditActions.QRCODE.name(),
                AccountsAuditActions.SUCCESS.name(), "QR code reset.", people);

        return "qrcode";

      } catch (IOException | WriterException | NoSuchAlgorithmException ex) {
        Logger.getLogger(RecoverySelector.class.getName()).log(Level.SEVERE,
                null, ex);
      }

    } else {
      int val = people.getFalseLogin();
      usersController.increaseLockNum(people.getUid(), val + 1);
      if (val > AuthenticationConstants.ALLOWED_FALSE_LOGINS) {
        usersController.changeAccountStatus(people.getUid(), "",
                PeopleAccountStatus.BLOCKED_ACCOUNT);
        try {
          am.registerAccountChange(people, AccountsAuditActions.RECOVERY.name(),
                  AccountsAuditActions.SUCCESS.name(),
                  "Account bloecked due to many false attempts.", people);

          email.sendEmail(people.getEmail(), RecipientType.TO,
                  UserAccountsEmailMessages.ACCOUNT_BLOCKED__SUBJECT,
                  UserAccountsEmailMessages.accountBlockedMessage());
        } catch (MessagingException ex1) {

        }
      }

      MessagesController.addSecurityErrorMessage(
              AccountStatusErrorMessages.INCCORCT_TMP_PIN);

      return "";
    }
    return "";
  }

  /**
   * Register lost Yubikey device.
   * <p>
   * @return
   * @throws java.net.SocketException
   */
  public String sendYubiReq() throws SocketException {

    people = userFacade.findByEmail(this.uname);

    if (people == null) {
      MessagesController.addSecurityErrorMessage(
              AccountStatusErrorMessages.USER_NOT_FOUND);

      return "";
    }

    if (people.getStatus().equals(PeopleAccountStatus.BLOCKED_ACCOUNT)) {
      MessagesController.addSecurityErrorMessage(
              AccountStatusErrorMessages.BLOCKED_ACCOUNT);
      am.registerAccountChange(people, AccountsAuditActions.RECOVERY.name(),
              AccountsAuditActions.FAILED.name(), "", people);
      return "";
    }

    if (!people.getMode().equals(PeopleAccountType.Y_ACCOUNT_TYPE)) {
      MessagesController.addSecurityErrorMessage(
              AccountStatusErrorMessages.USER_NOT_FOUND);

      am.registerAccountChange(people, AccountsAuditActions.RECOVERY.name(),
              AccountsAuditActions.FAILED.name(), "", people);

      return "";
    }

    try {
      if (people.getPassword().equals(DigestUtils.sha256Hex(passwd))) {

        String message = UserAccountsEmailMessages.buildYubikeyResetMessage();
        people.
                setStatus(PeopleAccountStatus.NEW_YUBIKEY_ACCOUNT);
        people.getYubikey().setStatus(PeopleAccountStatus.LOST_YUBIKEY);
        userFacade.update(people);
        email.sendEmail(people.getEmail(), RecipientType.TO,
                UserAccountsEmailMessages.DEVICE_LOST_SUBJECT, message);

        am.registerAccountChange(people, AccountsAuditActions.RECOVERY.name(),
                AccountsAuditActions.SUCCESS.name(), "", people);
        return "yubico_reset";
      } else {

        int val = people.getFalseLogin();
        usersController.increaseLockNum(people.getUid(), val + 1);
        if (val > AuthenticationConstants.ALLOWED_FALSE_LOGINS) {
          usersController.changeAccountStatus(people.getUid(), "",
                  PeopleAccountStatus.BLOCKED_ACCOUNT);
          am.registerAccountChange(people, AccountsAuditActions.RECOVERY.name(),
                  AccountsAuditActions.SUCCESS.name(),
                  "Account bloecked due to many false attempts.", people);

          try {
            email.sendEmail(people.getEmail(), RecipientType.TO,
                    UserAccountsEmailMessages.ACCOUNT_BLOCKED__SUBJECT,
                    UserAccountsEmailMessages.accountBlockedMessage());
          } catch (MessagingException ex1) {

            am.registerAccountChange(people, AccountsAuditActions.RECOVERY.
                    name(),
                    AccountsAuditActions.FAILED.name(), "", people);

          }
        }

        MessagesController.addSecurityErrorMessage(
                AccountStatusErrorMessages.INCCORCT_CREDENTIALS);

        am.registerAccountChange(people, AccountsAuditActions.RECOVERY.name(),
                AccountsAuditActions.FAILED.name(), "", people);

        return "";
      }
    } catch (MessagingException ex) {
      am.registerAccountChange(people, AccountsAuditActions.RECOVERY.name(),
              AccountsAuditActions.FAILED.name(), "", people);
    }
    return "";
  }

  public String returnMenu() {

    FacesContext ctx = FacesContext.getCurrentInstance();
    HttpSession sess = null;
    if (ctx != null) {
      sess = (HttpSession) ctx.getExternalContext().getSession(false);
    }

    if (sess != null) {
      sess.invalidate();
    }
    qrCode = null;
    return ("welcome");

  }
}
