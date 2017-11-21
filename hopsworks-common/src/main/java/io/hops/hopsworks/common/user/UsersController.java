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
import io.hops.hopsworks.common.dao.user.security.audit.AccountAuditFacade;
import io.hops.hopsworks.common.dao.user.security.ua.SecurityQuestion;
import io.hops.hopsworks.common.dao.user.security.ua.UserAccountsEmailMessages;
import io.hops.hopsworks.common.constants.auth.AuthenticationConstants;
import io.hops.hopsworks.common.constants.message.ResponseMessages;
import io.hops.hopsworks.common.dao.user.BbcGroupFacade;
import io.hops.hopsworks.common.dao.user.security.ua.PeopleAccountStatus;
import io.hops.hopsworks.common.dao.user.security.ua.SecurityUtils;
import io.hops.hopsworks.common.dao.user.security.Address;
import io.hops.hopsworks.common.dao.user.security.Organization;
import io.hops.hopsworks.common.dao.user.security.Yubikey;
import io.hops.hopsworks.common.dao.user.security.audit.AccountAudit;
import io.hops.hopsworks.common.dao.user.security.audit.RolesAudit;
import io.hops.hopsworks.common.dao.user.security.audit.RolesAuditFacade;
import io.hops.hopsworks.common.dao.user.security.ua.PeopleAccountType;
import io.hops.hopsworks.common.exception.AppException;
import io.hops.hopsworks.common.metadata.exception.ApplicationException;
import io.hops.hopsworks.common.util.AuditUtil;
import io.hops.hopsworks.common.util.EmailBean;
import io.hops.hopsworks.common.util.QRCodeGenerator;
import io.hops.hopsworks.common.util.Settings;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import javax.persistence.TransactionRequiredException;

@Stateless
//the operations in this method does not need any transaction
//the actual persisting will in the facades transaction.
@TransactionAttribute(TransactionAttributeType.NEVER)
public class UsersController {

  private final static Logger LOGGER = Logger.getLogger(UsersController.class.getName());
  @EJB
  private UserFacade userFacade;
  @EJB
  private AccountAuditFacade accountAuditFacade;
  @EJB
  private RolesAuditFacade rolesAuditFacade;
  @EJB
  private BbcGroupFacade bbcGroupFacade;
  @EJB
  private SshkeysFacade sshKeysBean;
  @EJB
  private UserValidator userValidator;
  @EJB
  private EmailBean emailBean;
  @EJB
  private AccountAuditFacade am;
  @EJB
  private Settings settings;
  @EJB
  private AuthController authController;

  // To send the user the QR code image
  private byte[] qrCode;

  public byte[] registerUser(UserDTO newUser, HttpServletRequest req) throws AppException, SocketException, 
      NoSuchAlgorithmException {
    userValidator.isValidNewUser(newUser);
    Users user = createNewUser(newUser, PeopleAccountStatus.NEW_MOBILE_ACCOUNT, PeopleAccountType.M_ACCOUNT_TYPE);
    addAddress(user);
    addOrg(user);
    //to privent sending email for test user emails
    try {
      if (!newUser.isTestUser()) {
        // Notify user about the request if not test user.
        emailBean.sendEmail(newUser.getEmail(), RecipientType.TO, UserAccountsEmailMessages.ACCOUNT_REQUEST_SUBJECT,
            UserAccountsEmailMessages.buildMobileRequestMessage(AuditUtil.getUserURL(req), user.getUsername() + user.
                getValidationKey()));
      }
      // Only register the user if i can send the email
      userFacade.persist(user);
      qrCode = QRCodeGenerator.getQRCodeBytes(newUser.getEmail(),AuthenticationConstants.ISSUER, user.getSecret());
      am.registerAccountChange(user, AccountsAuditActions.REGISTRATION.name(),
          AccountsAuditActions.SUCCESS.name(), "", user, req);
      am.registerAccountChange(user, AccountsAuditActions.QRCODE.name(),
          AccountsAuditActions.SUCCESS.name(), "", user, req);
    } catch (WriterException | MessagingException | IOException ex) {

      am.registerAccountChange(user, AccountsAuditActions.REGISTRATION.name(),
          AccountsAuditActions.FAILED.name(), "", user, req);
      am.registerAccountChange(user, AccountsAuditActions.QRCODE.name(),
          AccountsAuditActions.FAILED.name(), "", user, req);

      throw new AppException(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode(),
          "Cannot register now due to email service problems");
    }
    return qrCode;
  }

  public boolean registerYubikeyUser(UserDTO newUser, HttpServletRequest req)
      throws AppException, SocketException, NoSuchAlgorithmException {

    userValidator.isValidNewUser(newUser);
    Users user = createNewUser(newUser, PeopleAccountStatus.NEW_YUBIKEY_ACCOUNT, PeopleAccountType.Y_ACCOUNT_TYPE);
    addAddress(user);
    addOrg(user);

    Yubikey yk = new Yubikey();
    yk.setUid(user);
    yk.setStatus(PeopleAccountStatus.NEW_YUBIKEY_ACCOUNT);
    user.setYubikey(yk);

    try {
      // Notify user about the request
      emailBean.sendEmail(newUser.getEmail(), RecipientType.TO, UserAccountsEmailMessages.ACCOUNT_REQUEST_SUBJECT,
          UserAccountsEmailMessages.buildYubikeyRequestMessage(AuditUtil.getUserURL(req), user.getUsername() + user.
              getValidationKey()));
      // only register the user if i can send the email to the user
      userFacade.persist(user);
      am.registerAccountChange(user, AccountsAuditActions.REGISTRATION.name(),
          AccountsAuditActions.SUCCESS.name(), "", user, req);
    } catch (MessagingException ex) {

      am.registerAccountChange(user, AccountsAuditActions.REGISTRATION.name(),
          AccountsAuditActions.FAILED.name(), "", user, req);

      throw new AppException(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode(),
          "Cannot register now due to email service problems");

    }
    return true;
  }

  /**
   * Create a new user
   *
   * @param newUser
   * @param accountStatus
   * @param accountType
   * @return
   * @throws AppException
   * @throws NoSuchAlgorithmException
   */
  public Users createNewUser(UserDTO newUser, PeopleAccountStatus accountStatus, PeopleAccountType accountType) throws
      AppException, NoSuchAlgorithmException {
    String otpSecret = SecurityUtils.calculateSecretKey();
    String activationKey = SecurityUtils.getRandomPassword(64);
    String uname = generateUsername(newUser.getEmail());
    List<BbcGroup> groups = new ArrayList<>();
    String salt = authController.generateSalt();
    String password = authController.getPasswordHash(newUser.getChosenPassword(), salt);

    Users user = new Users(uname, password,
        newUser.getEmail(), newUser.getFirstName(), newUser.getLastName(),
        new Timestamp(new Date().getTime()), "-", "-", accountStatus, otpSecret, activationKey,
        SecurityQuestion.getQuestion(newUser.getSecurityQuestion()),
        authController.getHash(newUser.getSecurityAnswer().toLowerCase()),
        accountType, new Timestamp(new Date().getTime()), newUser.getTelephoneNum(),
        settings.getMaxNumProjPerUser(), newUser.isTwoFactor(), salt, newUser.getToursState());
    user.setBbcGroupCollection(groups);
    return user;
  }
  
  /**
   * Creates new agent user with only the not null values set
   * @param email
   * @param fname
   * @param lname
   * @param pwd
   * @param title
   * @return
   * @throws AppException
   * @throws NoSuchAlgorithmException 
   */
  public Users createNewAgent(String email, String fname, String lname, String pwd, String title) throws
      AppException, NoSuchAlgorithmException {
    String uname = generateUsername(email);
    List<BbcGroup> groups = new ArrayList<>();
    String salt = authController.generateSalt();
    String password = authController.getPasswordHash(pwd, salt);

    Users user = new Users(uname, password, email, fname, lname, title, PeopleAccountStatus.NEW_MOBILE_ACCOUNT,
        PeopleAccountType.M_ACCOUNT_TYPE, 0, salt);
    user.setBbcGroupCollection(groups);
    return user;
  }

  public void addAddress(Users user) {
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
  }

  public void addOrg(Users user) {
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
  }

  public void recoverPassword(String email, String securityQuestion,
      String securityAnswer, HttpServletRequest req) throws AppException, MessagingException, Exception {
    if (userValidator.isValidEmail(email) && userValidator.isValidsecurityQA(securityQuestion, securityAnswer)) {
      Users user = userFacade.findByEmail(email);
      if (user == null) {
        throw new AppException(Response.Status.NOT_FOUND.getStatusCode(),
            ResponseMessages.USER_DOES_NOT_EXIST);
      }
      authController.validateSecurityQA(user, securityQuestion, securityAnswer, req);
      authController.resetPassword(user, req);
    }
  }

  public void changePassword(String email, String oldPassword, String newPassword, String confirmedPassword,
      HttpServletRequest req) throws AppException, MessagingException {
    Users user = userFacade.findByEmail(email);

    if (user == null) {
      throw new AppException(Response.Status.NOT_FOUND.getStatusCode(), ResponseMessages.USER_WAS_NOT_FOUND);
    }
    if (!authController.validatePassword(user, oldPassword, req)) {
      throw new AppException(Response.Status.BAD_REQUEST.getStatusCode(), ResponseMessages.PASSWORD_INCORRECT);
    }
    if (userValidator.isValidPassword(newPassword, confirmedPassword)) {
      try {
        authController.changePassword(user, newPassword, req);
      } catch (Exception ex) {
        LOGGER.log(Level.SEVERE, "Error while changing password: ", ex);
        throw new AppException(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode(),
            ResponseMessages.PASSWORD_RESET_UNSUCCESSFUL);
      }
      am.registerAccountChange(user, AccountsAuditActions.PASSWORD.name(), AccountsAuditActions.SUCCESS.name(), "",
          user, req);
    }
  }

  public void changeSecQA(String email, String oldPassword, String securityQuestion, String securityAnswer,
      HttpServletRequest req) throws AppException, MessagingException {
    Users user = userFacade.findByEmail(email);

    if (user == null) {
      throw new AppException(Response.Status.NOT_FOUND.getStatusCode(), ResponseMessages.USER_WAS_NOT_FOUND);
    }
    if (!authController.validatePassword(user, oldPassword, req)) {
      throw new AppException(Response.Status.BAD_REQUEST.getStatusCode(), ResponseMessages.PASSWORD_INCORRECT);
    }

    if (userValidator.isValidsecurityQA(securityQuestion, securityAnswer)) {
      authController.changeSecQA(user, securityQuestion, securityAnswer, req);
    }
  }

  public UserDTO updateProfile(String email, String firstName, String lastName,
      String telephoneNum, Integer toursState, HttpServletRequest req)
      throws AppException {
    Users user = userFacade.findByEmail(email);

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

    userFacade.update(user);
    return new UserDTO(user);
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
    Users user = userFacade.findByUsername(uname);
    String suffix = "";
    if (user == null) {
      return uname;
    }

    String testUname = "";
    while (user != null && count < 100) {
      suffix = count.toString();
      testUname = uname.substring(0, (Settings.USERNAME_LEN - suffix.length()));
      user = userFacade.findByUsername(testUname + suffix);
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
      userFacade.update(user);
      am.registerAccountChange(user, AccountsAuditActions.TWO_FACTOR.name(),
          AccountsAuditActions.SUCCESS.name(), "Disabled 2-factor", user,
          req);
    } else {
      try {
        user.setTwoFactor(true);
        userFacade.update(user);
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

  public void registerGroup(Users uid, int gidNumber) {
    BbcGroup bbcGroup = bbcGroupFacade.find(gidNumber);
    uid.getBbcGroupCollection().add(bbcGroup);
    userFacade.update(uid);
  }

  /**
   * Register an address for new mobile users.
   *
   * @param user
   * @return
   */
  public void registerAddress(Users user) {
    Address add = new Address();
    add.setAddress1("-");
    add.setAddress2("-");
    add.setAddress3("-");
    add.setState("-");
    add.setCity("-");
    add.setCountry("-");
    add.setPostalcode("-");
    user.setAddress(add);
    userFacade.persist(user);
  }

  public void increaseLockNum(int id, int val) {
    Users p = userFacade.find(id);
    if (p != null) {
      p.setFalseLogin(val);
      userFacade.update(p);
    }

  }

  public void setOnline(int id, int val) {
    Users p = userFacade.find(id);
    p.setIsonline(val);
    userFacade.update(p);
  }

  public void resetLock(int id) {
    Users p = userFacade.find(id);
    p.setFalseLogin(0);
    userFacade.update(p);

  }

  public void changeAccountStatus(int id, String note, PeopleAccountStatus status) {
    Users p = userFacade.find(id);
    if (p != null) {
      p.setNotes(note);
      p.setStatus(status);
      userFacade.update(p);
    }

  }

  public void resetKey(int id) {
    Users p = userFacade.find(id);
    p.setValidationKey(SecurityUtils.getRandomPassword(64));
    userFacade.update(p);

  }

  public void resetSecQuestion(int id, SecurityQuestion question, String ans) {
    Users p = userFacade.find(id);
    p.setSecurityQuestion(question);
    p.setSecurityAnswer(ans);
    userFacade.update(p);

  }

  public void updateStatus(Users id, PeopleAccountStatus stat) throws ApplicationException {
    id.setStatus(stat);
    try {
      userFacade.update(id);
    } catch (TransactionRequiredException ex) {
      throw new ApplicationException("Need a transaction to update the user status");
    }
  }

  public void updateSecret(int id, String sec) {
    Users p = userFacade.find(id);
    p.setSecret(sec);
    userFacade.update(p);
  }

  public void increaseNumCreatedProjects(int id) {
    Users u = userFacade.find(id);
    u.setNumCreatedProjects(u.getNumCreatedProjects() + 1);
    userFacade.update(u);
  }

  public boolean isUsernameTaken(String username) {
    return (userFacade.findByEmail(username) != null);
  }

  public boolean isUserInRole(Users user, String groupName) {
    if (user == null || groupName == null) {
      return false;
    }
    BbcGroup group = bbcGroupFacade.findByGroupName(groupName);
    if (group == null) {
      return false;
    }
    return user.getBbcGroupCollection().contains(group);
  }

  public List<String> getUserRoles(Users p) {
    Collection<BbcGroup> groupList = p.getBbcGroupCollection();
    List<String> list = new ArrayList<>();
    for (BbcGroup g : groupList) {
      list.add(g.getGroupName());
    }
    return list;
  }

  public void updateMaxNumProjs(Users id, int maxNumProjs) {
    id.setMaxNumProjects(maxNumProjs);
    userFacade.update(id);
  }

  public void deleteUser(Users u) {
    if (u != null) {
      List<RolesAudit> results1 = rolesAuditFacade.findByInitiator(u);
      List<RolesAudit> results2 = rolesAuditFacade.findByTarget(u);
      results1.addAll(results2);
      for (Iterator<RolesAudit> iterator = results1.iterator(); iterator.hasNext();) {
        RolesAudit next = iterator.next();
        rolesAuditFacade.remove(next);
      }

      List<AccountAudit> resultsAA1 = accountAuditFacade.findByInitiator(u);
      List<AccountAudit> resultsAA2 = accountAuditFacade.findByTarget(u);
      resultsAA1.addAll(resultsAA2);

      for (Iterator<AccountAudit> iterator = resultsAA1.iterator(); iterator.hasNext();) {
        AccountAudit next = iterator.next();
        accountAuditFacade.remove(next);
      }
      userFacade.removeByEmail(u.getEmail());
    }
  }

}
