package io.hops.hopsworks.common.user;

import io.hops.hopsworks.common.dao.user.BbcGroup;
import io.hops.hopsworks.common.dao.user.sshkey.SshKeysPK;
import io.hops.hopsworks.common.dao.user.sshkey.SshKeys;
import io.hops.hopsworks.common.dao.user.Users;
import io.hops.hopsworks.common.dao.user.sshkey.SshkeysFacade;
import io.hops.hopsworks.common.dao.user.UserDTO;
import io.hops.hopsworks.common.dao.user.UserFacade;
import io.hops.hopsworks.common.dao.user.sshkey.SshKeyDTO;
import com.google.zxing.WriterException;
import java.io.IOException;
import java.net.SocketException;
import java.security.NoSuchAlgorithmException;
import java.sql.Timestamp;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.mail.Message.RecipientType;
import javax.mail.MessagingException;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.core.Response;
import org.apache.commons.codec.digest.DigestUtils;
import io.hops.hopsworks.common.dao.user.security.audit.AccountsAuditActions;
import io.hops.hopsworks.common.dao.user.security.audit.AuditManager;
import io.hops.hopsworks.common.dao.user.security.audit.UserAuditActions;
import io.hops.hopsworks.common.dao.user.security.ua.SecurityQuestion;
import io.hops.hopsworks.common.dao.user.security.ua.UserAccountsEmailMessages;
import io.hops.hopsworks.common.constants.auth.AuthenticationConstants;
import io.hops.hopsworks.common.constants.message.ResponseMessages;
import io.hops.hopsworks.common.dao.user.security.ua.PeopleAccountStatus;
import io.hops.hopsworks.common.dao.user.security.ua.SecurityUtils;
import io.hops.hopsworks.common.dao.user.security.Address;
import io.hops.hopsworks.common.dao.user.security.Organization;
import io.hops.hopsworks.common.dao.user.security.Yubikey;
import io.hops.hopsworks.common.dao.user.security.ua.PeopleAccountType;
import io.hops.hopsworks.common.exception.AppException;
import io.hops.hopsworks.common.util.AuditUtil;
import io.hops.hopsworks.common.util.EmailBean;
import io.hops.hopsworks.common.util.QRCodeGenerator;
import io.hops.hopsworks.common.util.Settings;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Stateless
//the operations in this method does not need any transaction
//the actual persisting will in the facades transaction.
@TransactionAttribute(TransactionAttributeType.NEVER)
public class UsersController {

  private final static Logger LOGGER = Logger.getLogger(UsersController.class.
          getName());
  @EJB
  private UserFacade userBean;
  @EJB
  private SshkeysFacade sshKeysBean;
  @EJB
  private UserValidator userValidator;
  @EJB
  private EmailBean emailBean;
  @EJB
  private AuditManager am;
  @EJB
  private Settings settings;

  // To send the user the QR code image
  private byte[] qrCode;

  public byte[] registerUser(UserDTO newUser, HttpServletRequest req) throws
          AppException, SocketException, NoSuchAlgorithmException {

    userValidator.isValidNewUser(newUser);

    String otpSecret = SecurityUtils.calculateSecretKey();
    String activationKey = SecurityUtils.getRandomPassword(64);

    String uname = generateUsername(newUser.getEmail());

    List<BbcGroup> groups = new ArrayList<>();

    Users user = new Users();
    user.setUsername(uname);
    user.setEmail(newUser.getEmail());
    user.setFname(newUser.getFirstName());
    user.setLname(newUser.getLastName());
    user.setMobile(newUser.getTelephoneNum());
    user.setStatus(PeopleAccountStatus.NEW_MOBILE_ACCOUNT);
    user.setSecret(otpSecret);
    user.setTwoFactor(newUser.isTwoFactor());
    user.setToursState(newUser.getToursState());
    user.setOrcid("-");
    user.setMobile(newUser.getTelephoneNum());
    user.setTitle("-");
    user.setMode(PeopleAccountType.M_ACCOUNT_TYPE);
    user.setValidationKey(activationKey);
    user.setActivated(new Timestamp(new Date().getTime()));
    user.setPasswordChanged(new Timestamp(new Date().getTime()));
    user.setSecurityQuestion(SecurityQuestion.getQuestion(newUser.
            getSecurityQuestion()));
    user.setPassword(DigestUtils.sha256Hex(newUser.getChosenPassword()));
    user.setSecurityAnswer(DigestUtils.sha256Hex(newUser.getSecurityAnswer().
            toLowerCase()));
    user.setBbcGroupCollection(groups);
    user.setMaxNumProjects(settings.getMaxNumProjPerUser());
    Address a = new Address();
    a.setUid(user);
    // default '-' in sql file did not add these values!
    a.setAddress1("-");
    a.setAddress2("-");
    a.setAddress3("-");
    a.setCity("Stockholm");
    a.setCountry("SE");
    a.setPostalcode("-");
    a.setState("-");
    user.setAddress(a);

    Organization org = new Organization();
    org.setUid(user);
    org.setContactEmail("-");
    org.setContactPerson("-");
    org.setDepartment("-");
    org.setFax("-");
    org.setOrgName("-");
    org.setWebsite("-");
    org.setPhone("-");

    user.setOrganization(org);
    //to privent sending email for test user emails
    try {
      if (!newUser.isTestUser()) {
        // Notify user about the request if not test user.
        emailBean.sendEmail(newUser.getEmail(), RecipientType.TO,
                UserAccountsEmailMessages.ACCOUNT_REQUEST_SUBJECT,
                UserAccountsEmailMessages.buildMobileRequestMessage(
                        AuditUtil.getUserURL(req), user.getUsername()
                        + activationKey));
      }
      // Only register the user if i can send the email
      userBean.persist(user);
      qrCode = QRCodeGenerator.getQRCodeBytes(newUser.getEmail(),
              AuthenticationConstants.ISSUER,
              otpSecret);
      am.registerAccountChange(user, AccountsAuditActions.REGISTRATION.name(),
              AccountsAuditActions.SUCCESS.name(), "", user, req);
      am.registerAccountChange(user, AccountsAuditActions.QRCODE.name(),
              AccountsAuditActions.SUCCESS.name(), "", user, req);
    } catch (WriterException | MessagingException | IOException ex) {

      am.registerAccountChange(user, AccountsAuditActions.REGISTRATION.name(),
              AccountsAuditActions.FAILED.name(), "", user, req);
      am.registerAccountChange(user, AccountsAuditActions.QRCODE.name(),
              AccountsAuditActions.FAILED.name(), "", user, req);

      throw new AppException(Response.Status.INTERNAL_SERVER_ERROR.
              getStatusCode(),
              "Cannot register now due to email service problems");
    }
    return qrCode;
  }

  public boolean registerYubikeyUser(UserDTO newUser, HttpServletRequest req)
          throws AppException, SocketException, NoSuchAlgorithmException {

    userValidator.isValidNewUser(newUser);

    String otpSecret = SecurityUtils.calculateSecretKey();
    String activationKey = SecurityUtils.getRandomPassword(64);

    String uname = generateUsername(newUser.getEmail());
    List<BbcGroup> groups = new ArrayList<>();

    Users user = new Users();
    user.setUsername(uname);
    user.setEmail(newUser.getEmail());
    user.setFname(newUser.getFirstName());
    user.setLname(newUser.getLastName());
    user.setMobile(newUser.getTelephoneNum());
    user.setStatus(PeopleAccountStatus.NEW_YUBIKEY_ACCOUNT);
    user.setSecret(otpSecret);
    user.setTwoFactor(newUser.isTwoFactor());
    user.setOrcid("-");
    user.setMobile(newUser.getTelephoneNum());
    user.setTitle("-");
    user.setMode(PeopleAccountType.Y_ACCOUNT_TYPE);
    user.setValidationKey(activationKey);
    user.setActivated(new Timestamp(new Date().getTime()));
    user.setPasswordChanged(new Timestamp(new Date().getTime()));
    user.setSecurityQuestion(SecurityQuestion.getQuestion(newUser.
            getSecurityQuestion()));
    user.setPassword(DigestUtils.sha256Hex(newUser.getChosenPassword()));
    user.setSecurityAnswer(DigestUtils.sha256Hex(newUser.getSecurityAnswer().
            toLowerCase()));
    user.setBbcGroupCollection(groups);
    user.setMaxNumProjects(settings.getMaxNumProjPerUser());

    Address a = new Address();
    a.setUid(user);
    // default '-' in sql file did not add these values!
    a.setAddress1("-");
    a.setAddress2(newUser.getStreet());
    a.setAddress3("-");
    a.setCity(newUser.getCity());
    a.setCountry(newUser.getCountry());
    a.setPostalcode(newUser.getPostCode());
    a.setState("-");
    user.setAddress(a);

    Organization org = new Organization();
    org.setUid(user);
    org.setContactEmail("-");
    org.setContactPerson("-");
    org.setDepartment(newUser.getDep());
    org.setFax("-");
    org.setOrgName(newUser.getOrgName());
    org.setWebsite("-");
    org.setPhone("-");

    Yubikey yk = new Yubikey();
    yk.setUid(user);
    yk.setStatus(PeopleAccountStatus.NEW_YUBIKEY_ACCOUNT);
    user.setYubikey(yk);
    user.setOrganization(org);

    try {
      // Notify user about the request
      emailBean.sendEmail(newUser.getEmail(), RecipientType.TO,
              UserAccountsEmailMessages.ACCOUNT_REQUEST_SUBJECT,
              UserAccountsEmailMessages.buildYubikeyRequestMessage(
                      AuditUtil.getUserURL(req), user.getUsername()
                      + activationKey));
      // only register the user if i can send the email to the user
      userBean.persist(user);
      am.registerAccountChange(user, AccountsAuditActions.REGISTRATION.name(),
              AccountsAuditActions.SUCCESS.name(), "", user, req);
    } catch (MessagingException ex) {

      am.registerAccountChange(user, AccountsAuditActions.REGISTRATION.name(),
              AccountsAuditActions.FAILED.name(), "", user, req);

      throw new AppException(Response.Status.INTERNAL_SERVER_ERROR.
              getStatusCode(),
              "Cannot register now due to email service problems");

    }
    return true;
  }

  public void recoverPassword(String email, String securityQuestion,
          String securityAnswer, HttpServletRequest req) throws AppException {
    if (userValidator.isValidEmail(email) && userValidator.isValidsecurityQA(
            securityQuestion, securityAnswer)) {

      Users user = userBean.findByEmail(email);
      if (user == null) {
        throw new AppException(Response.Status.NOT_FOUND.getStatusCode(),
                ResponseMessages.USER_DOES_NOT_EXIST);
      }
      if (!user.getSecurityQuestion().getValue().equalsIgnoreCase(
              securityQuestion)
              || !user.getSecurityAnswer().equals(DigestUtils.sha256Hex(
                      securityAnswer.toLowerCase()))) {
        try {
          registerFalseLogin(user);
          am.registerAccountChange(user, AccountsAuditActions.RECOVERY.name(),
                  UserAuditActions.FAILED.name(), "", user, req);

        } catch (MessagingException ex) {
          Logger.getLogger(UsersController.class
                  .getName()).
                  log(Level.SEVERE, null, ex);
          am.registerAccountChange(user, AccountsAuditActions.RECOVERY.name(),
                  UserAuditActions.FAILED.name(), "", user, req);

          throw new AppException(Response.Status.BAD_REQUEST.getStatusCode(),
                  ResponseMessages.SEC_QA_INCORRECT);
        }

        String randomPassword = SecurityUtils.getRandomPassword(
                UserValidator.PASSWORD_MIN_LENGTH);
        try {
          String message = UserAccountsEmailMessages.buildTempResetMessage(
                  randomPassword);
          emailBean.sendEmail(email, RecipientType.TO,
                  UserAccountsEmailMessages.ACCOUNT_PASSWORD_RESET, message);
          user.setPassword(DigestUtils.sha256Hex(randomPassword));
          //user.setStatus(PeopleAccountStatus.ACCOUNT_PENDING.getValue());
          userBean.update(user);
          resetFalseLogin(user);
          am.registerAccountChange(user, AccountsAuditActions.RECOVERY.name(),
                  UserAuditActions.SUCCESS.name(), "", user, req);

        } catch (MessagingException ex) {
          LOGGER.log(Level.SEVERE, "Could not send email: ", ex);
          throw new AppException(Response.Status.BAD_REQUEST.getStatusCode(),
                  ResponseMessages.EMAIL_SENDING_FAILURE);

        }
      }
    }
  }

  public void changePassword(String email, String oldPassword,
          String newPassword, String confirmedPassword, HttpServletRequest req)
          throws AppException {
    Users user = userBean.findByEmail(email);

    if (user == null) {
      throw new AppException(Response.Status.NOT_FOUND.getStatusCode(),
              ResponseMessages.USER_WAS_NOT_FOUND);
    }
    if (!user.getPassword().equals(DigestUtils.sha256Hex(oldPassword))) {

      am.registerAccountChange(user, AccountsAuditActions.PASSWORD.name(),
              AccountsAuditActions.FAILED.name(), "", user, req);
      throw new AppException(Response.Status.BAD_REQUEST.getStatusCode(),
              ResponseMessages.PASSWORD_INCORRECT);

    }
    if (userValidator.isValidPassword(newPassword, confirmedPassword)) {
      user.setPassword(DigestUtils.sha256Hex(newPassword));
      userBean.update(user);

      am.registerAccountChange(user, AccountsAuditActions.PASSWORD.name(),
              AccountsAuditActions.SUCCESS.name(), "", user, req);
    }
  }

  public void changeSecQA(String email, String oldPassword,
          String securityQuestion, String securityAnswer, HttpServletRequest req)
          throws AppException {
    Users user = userBean.findByEmail(email);

    if (user == null) {
      throw new AppException(Response.Status.NOT_FOUND.getStatusCode(),
              ResponseMessages.USER_WAS_NOT_FOUND);
    }
    if (!user.getPassword().equals(DigestUtils.sha256Hex(oldPassword))) {
      am.registerAccountChange(user, AccountsAuditActions.SECQUESTION.name(),
              AccountsAuditActions.FAILED.name(), "", user, req);
      throw new AppException(Response.Status.BAD_REQUEST.getStatusCode(),
              ResponseMessages.PASSWORD_INCORRECT);
    }

    if (userValidator.isValidsecurityQA(securityQuestion, securityAnswer)) {
      user.setSecurityQuestion(SecurityQuestion.getQuestion(securityQuestion));
      user.
              setSecurityAnswer(DigestUtils.sha256Hex(securityAnswer.
                      toLowerCase()));
      userBean.update(user);
      am.registerAccountChange(user, AccountsAuditActions.SECQUESTION.name(),
              AccountsAuditActions.SUCCESS.name(),
              "Changed Security Question to: " + securityQuestion, user, req);
    }
  }

  public UserDTO updateProfile(String email, String firstName, String lastName,
          String telephoneNum, Integer toursState, HttpServletRequest req)
      throws AppException {
    Users user = userBean.findByEmail(email);

    if (user == null) {
      throw new AppException(Response.Status.NOT_FOUND.getStatusCode(),
              ResponseMessages.USER_WAS_NOT_FOUND);
    }
    if (firstName != null) {
      user.setFname(firstName);
    }
    if (lastName != null) {
      user.setLname(lastName);
    }
    if (telephoneNum != null) {
      user.setMobile(telephoneNum);
    }
    if (toursState != null) {
      user.setToursState(toursState);
    }
    am.registerAccountChange(user, AccountsAuditActions.SECQUESTION.name(),
            AccountsAuditActions.SUCCESS.name(), "Update Profile Info", user,
            req);

    userBean.update(user);
    return new UserDTO(user);
  }

  public void registerFalseLogin(Users user) throws MessagingException {
    if (user != null) {
      int count = user.getFalseLogin() + 1;
      user.setFalseLogin(count);

      // block the user account if more than allowed false logins
      if (count > AuthenticationConstants.ALLOWED_FALSE_LOGINS) {
        user.setStatus(PeopleAccountStatus.BLOCKED_ACCOUNT);

        emailBean.sendEmail(user.getEmail(), RecipientType.TO,
                UserAccountsEmailMessages.ACCOUNT_BLOCKED__SUBJECT,
                UserAccountsEmailMessages.accountBlockedMessage());

      }
      // notify user about the false attempts
      userBean.update(user);
    }
  }

  public void resetFalseLogin(Users user) {
    if (user != null) {
      user.setFalseLogin(0);
      userBean.update(user);
    }
  }

  public void setUserIsOnline(Users user, int status) {
    if (user != null) {
      user.setIsonline(status);
      userBean.update(user);
    }
  }

  public SshKeyDTO addSshKey(int id, String name, String sshKey) {
    SshKeys key = new SshKeys();
    key.setSshKeysPK(new SshKeysPK(id, name));
    key.setPublicKey(sshKey);
    sshKeysBean.persist(key);
    return new SshKeyDTO(key);
  }

  public void removeSshKey(int id, String name) {
    sshKeysBean.removeByIdName(id, name);
  }

  public List<SshKeyDTO> getSshKeys(int id) {
    List<SshKeys> keys = sshKeysBean.findAllById(id);
    List<SshKeyDTO> dtos = new ArrayList<>();
    for (SshKeys sshKey : keys) {
      dtos.add(new SshKeyDTO(sshKey));
    }
    return dtos;
  }

  public String generateUsername(String email) {
    Integer count = 0;
    String uname = getUsernameFromEmail(email);
    Users user = userBean.findByUsername(uname);
    String suffix = "";
    if (user == null) {
      return uname;
    }

    String testUname = "";
    while (user != null && count < 100) {
      suffix = count.toString();
      testUname = uname.substring(0, (Settings.USERNAME_LEN - suffix.length()));
      user = userBean.findByUsername(testUname + suffix);
      count++;
    }
    if (count == 100) {
      throw new IllegalStateException(
              "You cannot register with this email address. Pick another.");
    }
    return testUname + suffix;
  }

  private String getUsernameFromEmail(String email) {
    String username = email.substring(0, email.lastIndexOf("@"));
    if (username.contains(".")) {
      username = username.replace(".", "_");
    }
    if (username.contains("__")) {
      username = username.replace("__", "_");
    }
    if (username.length() > Settings.USERNAME_LEN) {
      username = username.substring(0, Settings.USERNAME_LEN);
    }
    if (username.length() < Settings.USERNAME_LEN) {
      while (username.length() < Settings.USERNAME_LEN) {
        username += "0";
      }
    }
    return username;
  }

  /**
   * Enables or disables two factor authentication.
   * The operation depends on the current status of the users two factor.
   * i.e enables if it was disabled or vice versa.
   *
   * @param user
   * @param password
   * @param req
   * @return qrCode if tow factor is enabled null if disabled.
   * @throws AppException
   */
  public byte[] changeTwoFactor(Users user, String password,
          HttpServletRequest req) throws AppException {
    if (user == null) {
      throw new AppException(Response.Status.NOT_FOUND.getStatusCode(),
              ResponseMessages.USER_WAS_NOT_FOUND);
    }
    if (!user.getPassword().equals(DigestUtils.sha256Hex(password))) {
      am.registerAccountChange(user, AccountsAuditActions.TWO_FACTOR.name(),
              AccountsAuditActions.FAILED.name(), "Incorrect password", user,
              req);
      throw new AppException(Response.Status.BAD_REQUEST.getStatusCode(),
              ResponseMessages.PASSWORD_INCORRECT);
    }
    byte[] qr_code = null;
    if (user.getTwoFactor()) {
      user.setTwoFactor(false);
      userBean.update(user);
      am.registerAccountChange(user, AccountsAuditActions.TWO_FACTOR.name(),
              AccountsAuditActions.SUCCESS.name(), "Disabled 2-factor", user,
              req);
    } else {
      try {
        user.setTwoFactor(true);
        userBean.update(user);
        qr_code = QRCodeGenerator.getQRCodeBytes(user.getEmail(),
                AuthenticationConstants.ISSUER, user.getSecret());
        am.registerAccountChange(user, AccountsAuditActions.TWO_FACTOR.name(),
                AccountsAuditActions.SUCCESS.name(), "Enabled 2-factor", user,
                req);
        am.registerAccountChange(user, AccountsAuditActions.QRCODE.name(),
                AccountsAuditActions.SUCCESS.name(), "Enabled 2-factor", user,
                req);
      } catch (IOException | WriterException ex) {
        LOGGER.log(Level.SEVERE, null, ex);
        am.registerAccountChange(user, AccountsAuditActions.TWO_FACTOR.name(),
                AccountsAuditActions.FAILED.name(), "Enabled 2-factor", user,
                req);
        am.registerAccountChange(user, AccountsAuditActions.QRCODE.name(),
                AccountsAuditActions.FAILED.name(), "Enabled 2-factor", user,
                req);
        throw new AppException(Response.Status.INTERNAL_SERVER_ERROR.
                getStatusCode(),
                "Cannot enable 2-factor authentication.");
      }
    }
    return qr_code;
  }

  /**
   * Returns the QR code for the user if two factor is enabled.
   *
   * @param user
   * @param password
   * @return null if two factor is disabled.
   * @throws AppException
   */
  public byte[] getQRCode(Users user, String password) throws AppException {
    byte[] qr_code = null;
    if (!user.getPassword().equals(DigestUtils.sha256Hex(password))) {
      throw new AppException(Response.Status.BAD_REQUEST.getStatusCode(),
              ResponseMessages.PASSWORD_INCORRECT);
    }
    if (user.getTwoFactor()) {
      try {
        qr_code = QRCodeGenerator.getQRCodeBytes(user.getEmail(),
                AuthenticationConstants.ISSUER, user.getSecret());
      } catch (IOException | WriterException ex) {
        LOGGER.log(Level.SEVERE, null, ex);
      }
    }
    return qr_code;
  }

}
