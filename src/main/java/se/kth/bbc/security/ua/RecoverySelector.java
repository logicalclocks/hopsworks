package se.kth.bbc.security.ua;

import com.google.zxing.WriterException;
import java.io.IOException;
import java.io.Serializable;
import java.io.UnsupportedEncodingException;
import java.security.NoSuchAlgorithmException;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.ejb.EJB;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.SessionScoped;
import javax.mail.MessagingException;
import org.primefaces.model.StreamedContent;
import se.kth.bbc.lims.MessagesController;
import se.kth.bbc.security.auth.AccountStatusErrorMessages;
import se.kth.bbc.security.auth.CustomAuthentication;
import se.kth.bbc.security.auth.QRCodeGenerator;
import se.kth.bbc.security.ua.model.User;

/**
 *
 * @author Ali Gholami <gholami@pdc.kth.se>
 */
@ManagedBean
@SessionScoped
public class RecoverySelector implements Serializable {

  private static final long serialVersionUID = 1L;

  @EJB
  private UserManager um;

  @EJB
  private EmailBean email;

  private User people;

  private String console;

  // Quick response code URL
  private String qrUrl = "Pass";
  private StreamedContent qrCode;

  //@ManagedProperty(value="#{recoverySelector.uname}")
  private String uname;
  private String tmpCode;
  private String passwd;

  private final int passwordLength = 6;

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

  public String sendQrCode() {

    people = um.getUserByEmail(this.uname);

    if (people == null) {
      MessagesController.addSecurityErrorMessage(
              AccountStatusErrorMessages.USER_NOT_FOUND);
      return "";
    }

    // Check the status to see if user is not blocked or deactivate
    if (people.getStatus() == PeopleAccountStatus.ACCOUNT_BLOCKED.getValue()) {
      MessagesController.addSecurityErrorMessage(
              AccountStatusErrorMessages.BLOCKED_ACCOUNT);
      return "";
    }

    if (people.getStatus() == PeopleAccountStatus.ACCOUNT_DEACTIVATED.getValue()) {
      MessagesController.addSecurityErrorMessage(
              AccountStatusErrorMessages.DEACTIVATED_ACCOUNT);
      return "";
    }

    if (people.getYubikeyUser() == PeopleAccountStatus.YUBIKEY_USER.getValue()) {
      MessagesController.addSecurityErrorMessage(
              AccountStatusErrorMessages.USER_NOT_FOUND);
      return "";
    }

    try {

      if (people.getPassword().equals(SecurityUtils.converToSHA256(passwd))) {

        // generate a randome secret of legth 6
        String random = SecurityUtils.getRandomString(passwordLength);
        um.updateSecret(people.getUid(), random);
        String message = UserAccountsEmailMessages.buildTempResetMessage(random);
        email.sendEmail(people.getEmail(),
                UserAccountsEmailMessages.ACCOUNT_PASSWORD_RESET, message);

        return "validate_code";
      } else {
        MessagesController.addSecurityErrorMessage(
                AccountStatusErrorMessages.INCCORCT_CREDENTIALS);

        return "";
      }
    } catch (NoSuchAlgorithmException | UnsupportedEncodingException |
            MessagingException ex) {
      Logger.getLogger(RecoverySelector.class.getName()).log(Level.SEVERE, null,
              ex);
    }

    return "";
  }

  public String validateTmpCode() {

    people = um.getUserByEmail(this.uname);

    if (people == null) {
      MessagesController.addSecurityErrorMessage(
              AccountStatusErrorMessages.USER_NOT_FOUND);
      return "";
    }

    // Check the status to see if user is not blocked or deactivate
    if (people.getStatus() == PeopleAccountStatus.ACCOUNT_BLOCKED.getValue()) {
      MessagesController.addSecurityErrorMessage(
              AccountStatusErrorMessages.BLOCKED_ACCOUNT);
      return "";
    }

    if (people.getStatus() == PeopleAccountStatus.ACCOUNT_DEACTIVATED.getValue()) {
      MessagesController.addSecurityErrorMessage(
              AccountStatusErrorMessages.DEACTIVATED_ACCOUNT);
      return "";
    }

    if (people.getSecret() == null ? tmpCode == null : people.getSecret().
            equals(this.tmpCode)) {

      try {
        String otpSecret = SecurityUtils.calculateSecretKey();

        um.updateSecret(people.getUid(), otpSecret);
        qrCode = QRCodeGenerator.getQRCode(people.getEmail(),
                CustomAuthentication.ISSUER, otpSecret);
        return "qrcode";

      } catch (IOException | WriterException ex) {
        Logger.getLogger(RecoverySelector.class.getName()).log(Level.SEVERE,
                null, ex);
      }

    } else {
      int val = people.getFalseLogin();
      um.increaseLockNum(people.getUid(), val + 1);
      if (val > 5) {
        um.changeAccountStatus(people.getUid(), "",
                PeopleAccountStatus.ACCOUNT_BLOCKED.getValue());
        try {
          email.sendEmail(people.getEmail(),
                  UserAccountsEmailMessages.ACCOUNT_BLOCKED__SUBJECT,
                  UserAccountsEmailMessages.accountBlockedMessage());
        } catch (MessagingException ex1) {
          Logger.getLogger(CustomAuthentication.class.getName()).log(
                  Level.SEVERE, null, ex1);
        }
      }

      MessagesController.addSecurityErrorMessage(
              AccountStatusErrorMessages.INCCORCT_TMP_PIN);

      return "";
    }
    return "";
  }

  public String sendYubiReq() {

    people = um.getUserByEmail(this.uname);

    if (people == null) {
      MessagesController.addSecurityErrorMessage(
              AccountStatusErrorMessages.USER_NOT_FOUND);

      return "";
    }

    if (people.getStatus() == PeopleAccountStatus.ACCOUNT_BLOCKED.getValue()) {
      MessagesController.addSecurityErrorMessage(
              AccountStatusErrorMessages.BLOCKED_ACCOUNT);

      return "";
    }

    if (people.getYubikeyUser() != PeopleAccountStatus.YUBIKEY_USER.getValue()) {
      MessagesController.addSecurityErrorMessage(
              AccountStatusErrorMessages.USER_NOT_FOUND);
      return "";
    }

    try {
      if (people.getPassword().equals(SecurityUtils.converToSHA256(passwd))) {

        String message = UserAccountsEmailMessages.buildYubikeyResetMessage();
        people.
                setStatus(PeopleAccountStatus.YUBIKEY_ACCOUNT_INACTIVE.
                        getValue());
        people.getYubikey().setStatus(PeopleAccountStatus.YUBIKEY_LOST.
                getValue());
        um.updatePeople(people);
        email.sendEmail(people.getEmail(),
                UserAccountsEmailMessages.ACCOUNT_REQUEST_SUBJECT, message);
        return "yubico";
      } else {

        int val = people.getFalseLogin();
        um.increaseLockNum(people.getUid(), val + 1);
        if (val > 5) {
          um.changeAccountStatus(people.getUid(), "",
                  PeopleAccountStatus.ACCOUNT_BLOCKED.getValue());
          try {
            email.sendEmail(people.getEmail(),
                    UserAccountsEmailMessages.ACCOUNT_BLOCKED__SUBJECT,
                    UserAccountsEmailMessages.accountBlockedMessage());
          } catch (MessagingException ex1) {
            Logger.getLogger(CustomAuthentication.class.getName()).log(
                    Level.SEVERE, null, ex1);
          }
        }

        MessagesController.addSecurityErrorMessage(
                AccountStatusErrorMessages.INCCORCT_CREDENTIALS);
        return "";
      }
    } catch (NoSuchAlgorithmException | UnsupportedEncodingException |
            MessagingException ex) {
      Logger.getLogger(RecoverySelector.class.getName()).log(Level.SEVERE, null,
              ex);
    }

    return "";

  }

}
