package se.kth.hopsworks.controller;

import com.google.zxing.WriterException;
import java.io.IOException;
import java.net.SocketException;
import java.sql.Timestamp;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.mail.MessagingException;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.core.Response;
import org.apache.commons.codec.digest.DigestUtils;
import se.kth.bbc.security.audit.AuditManager;
import se.kth.bbc.security.audit.AuditUtil;
import se.kth.bbc.security.audit.RolesAuditActions;
import se.kth.bbc.security.audit.UserAuditActions;
import se.kth.bbc.security.ua.EmailBean;
import se.kth.bbc.security.ua.SecurityQuestion;
import se.kth.bbc.security.ua.UserAccountsEmailMessages;
import se.kth.bbc.security.auth.AuthenticationConstants;
import se.kth.bbc.security.auth.QRCodeGenerator;
import se.kth.bbc.security.ua.BBCGroup;
import se.kth.bbc.security.ua.PeopleAccountStatus;
import se.kth.bbc.security.ua.SecurityUtils;
import se.kth.bbc.security.ua.UserManager;
import se.kth.bbc.security.ua.model.Address;
import se.kth.bbc.security.ua.model.Organization;
import se.kth.bbc.security.ua.model.Yubikey;
import se.kth.hopsworks.rest.AppException;
import se.kth.hopsworks.rest.AuthService;
import se.kth.hopsworks.user.model.*;
import se.kth.hopsworks.users.*;

@Stateless
//the operations in this method does not need any transaction
//the actual persisting will in the facades transaction.
@TransactionAttribute(TransactionAttributeType.NEVER)
public class UsersController {

  @EJB
  private UserFacade userBean;
  @EJB
  private SshkeysFacade sshKeysBean;
  @EJB
  private UserValidator userValidator;
  @EJB
  private EmailBean emailBean;
  @EJB
  private BbcGroupFacade groupBean;
  @EJB
  private AuditManager am;

  @EJB
  private UserManager mgr;
  // To send the user the QR code image
  private byte[] qrCode;

  public byte[] registerUser(UserDTO newUser, HttpServletRequest req) throws
          AppException, SocketException //      , IOException, UnsupportedEncodingException, WriterException, MessagingException 
  {
    if (userValidator.isValidEmail(newUser.getEmail())
            && userValidator.isValidPassword(newUser.getChosenPassword(),
                    newUser.getRepeatedPassword())
            && userValidator.isValidsecurityQA(newUser.getSecurityQuestion(),
                    newUser.getSecurityAnswer())) {
      if (newUser.getToS()) {
        throw new AppException(Response.Status.BAD_REQUEST.getStatusCode(),
                ResponseMessages.TOS_NOT_AGREED);
      }
      if (userBean.findByEmail(newUser.getEmail()) != null) {
        throw new AppException(Response.Status.BAD_REQUEST.getStatusCode(),
                ResponseMessages.USER_EXIST);
      }

      String otpSecret = SecurityUtils.calculateSecretKey();
      String activationKey = SecurityUtils.getRandomPassword(64);

      int uid = userBean.lastUserID() + 1;

      // String uname = LocalhostServices.getUsernameFromEmail(newUser.getEmail());
      String uname = AuthenticationConstants.USERNAME_PREFIX + uid;

      List<BbcGroup> groups = new ArrayList<>();

      // add the guest default role so if a user can still browse the platform
      groups.add(groupBean.findByGroupName(BBCGroup.BBC_GUEST.name()));

  
      Users user = new Users(uid);
      user.setUsername(uname);
      user.setEmail(newUser.getEmail());
      user.setFname(newUser.getFirstName());
      user.setLname(newUser.getLastName());
      user.setMobile(newUser.getTelephoneNum());
      user.setStatus(PeopleAccountStatus.ACCOUNT_VERIFICATION.getValue());
      user.setSecret(otpSecret);
      user.setOrcid("-");
      user.setMobile("-");
      user.setTitle("-");
      user.setMode(PeopleAccountStatus.MOBILE_USER.getValue());
      user.setValidationKey(activationKey);
      user.setActivated(new Timestamp(new Date().getTime()));
      user.setPasswordChanged(new Timestamp(new Date().getTime()));
      user.setSecurityQuestion(SecurityQuestion.getQuestion(newUser.
              getSecurityQuestion()));
      user.setPassword(DigestUtils.sha256Hex(newUser.getChosenPassword()));
      user.setSecurityAnswer(DigestUtils.sha256Hex(newUser.getSecurityAnswer().
              toLowerCase()));
      user.setBbcGroupCollection(groups);
      Address a = new Address();
      a.setUid(user);
      // default '-' in sql file did not add these values!
      a.setAddress1("-");
      a.setAddress2("-");
      a.setAddress3("-");
      a.setCity("-");
      a.setCountry("-");
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

      try {
        // Notify user about the request
        emailBean.sendEmail(newUser.getEmail(),
                UserAccountsEmailMessages.ACCOUNT_REQUEST_SUBJECT,
                UserAccountsEmailMessages.buildMobileRequestMessage(
                        //getApplicationUri()
                        AuditUtil.getUserURL(req), user.getUsername() + activationKey));
        // Only register the user if i can send the email
        userBean.persist(user);
        qrCode = QRCodeGenerator.getQRCodeBytes(newUser.getEmail(),
                AuthenticationConstants.ISSUER,
                otpSecret);
        am.registerLoginInfo(user, UserAuditActions.REGISTRATION.name(), UserAuditActions.SUCCESS.name(), req);
        am.registerLoginInfo(user, UserAuditActions.QRCODE.name(), UserAuditActions.SUCCESS.name(), req); 
        am.registerRoleChange(user, RolesAuditActions.ADDROLE.name(), RolesAuditActions.SUCCESS.name(), BBCGroup.BBC_GUEST.name(), user, req);
      } catch (WriterException | MessagingException | IOException ex) {
        
        am.registerLoginInfo(user, UserAuditActions.REGISTRATION.name(), UserAuditActions.FAILED.name(), req);
        am.registerLoginInfo(user, UserAuditActions.QRCODE.name(), UserAuditActions.FAILED.name(), req); 
        am.registerRoleChange(user, RolesAuditActions.ADDROLE.name(), RolesAuditActions.FAILED.name(), BBCGroup.BBC_GUEST.name(), user, req);

        throw new AppException(Response.Status.INTERNAL_SERVER_ERROR.
                getStatusCode(),
                "Cannot register now due to email service problems");
      } 
      return qrCode;
    }

    return null;
  }

  public boolean registerYubikeyUser(UserDTO newUser, HttpServletRequest req)
          throws AppException, SocketException //      , IOException, UnsupportedEncodingException, WriterException, MessagingException 
  {
    if (userValidator.isValidEmail(newUser.getEmail())
            && userValidator.isValidPassword(newUser.getChosenPassword(),
                    newUser.getRepeatedPassword())
            && userValidator.isValidsecurityQA(newUser.getSecurityQuestion(),
                    newUser.getSecurityAnswer())) {
      if (newUser.getToS()) {
        throw new AppException(Response.Status.BAD_REQUEST.getStatusCode(),
                ResponseMessages.TOS_NOT_AGREED);
      }
      if (userBean.findByEmail(newUser.getEmail()) != null) {
        throw new AppException(Response.Status.BAD_REQUEST.getStatusCode(),
                ResponseMessages.USER_EXIST);
      }

      String otpSecret = SecurityUtils.calculateSecretKey();
      String activationKey = SecurityUtils.getRandomPassword(64);

      int uid = userBean.lastUserID() + 1;

      // String uname = LocalhostServices.getUsernameFromEmail(newUser.getEmail());
      String uname = AuthenticationConstants.USERNAME_PREFIX + uid;
      List<BbcGroup> groups = new ArrayList<>();

      // add the guest default role so if a user can still browse the platform
      groups.add(groupBean.findByGroupName(BBCGroup.BBC_GUEST.name()));

      Users user = new Users(uid);
      user.setUsername(uname);
      user.setEmail(newUser.getEmail());
      user.setFname(newUser.getFirstName());
      user.setLname(newUser.getLastName());
      user.setMobile(newUser.getTelephoneNum());
      user.setStatus(PeopleAccountStatus.ACCOUNT_VERIFICATION.getValue());
      user.setSecret(otpSecret);
      user.setOrcid("-");
      user.setMobile("-");
      user.setTitle("-");
      user.setMode(PeopleAccountStatus.YUBIKEY_USER.getValue());
      user.setValidationKey(activationKey);
      user.setActivated(new Timestamp(new Date().getTime()));
      user.setPasswordChanged(new Timestamp(new Date().getTime()));
      user.setSecurityQuestion(SecurityQuestion.getQuestion(newUser.
              getSecurityQuestion()));
      user.setPassword(DigestUtils.sha256Hex(newUser.getChosenPassword()));
      user.setSecurityAnswer(DigestUtils.sha256Hex(newUser.getSecurityAnswer().
              toLowerCase()));
      user.setBbcGroupCollection(groups);

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
      yk.setStatus(PeopleAccountStatus.YUBIKEY_ACCOUNT_INACTIVE.getValue());
      user.setYubikey(yk);
      user.setOrganization(org);

      try {
        // Notify user about the request
        emailBean.sendEmail(newUser.getEmail(),
                UserAccountsEmailMessages.ACCOUNT_REQUEST_SUBJECT,
                UserAccountsEmailMessages.buildYubikeyRequestMessage(
                        //getApplicationUri()
                        AuditUtil.getBrowserInfo(req), user.getUsername() + activationKey));
        // only register the user if i can send the email to the user
        userBean.persist(user);
        am.registerLoginInfo(user, UserAuditActions.REGISTRATION.name(), UserAuditActions.SUCCESS.name(), req);
        am.registerRoleChange(user, RolesAuditActions.ADDROLE.name(), RolesAuditActions.SUCCESS.name(), BBCGroup.BBC_GUEST.name(), user, req);
      } catch (MessagingException | IOException ex) {
        
        am.registerLoginInfo(user, UserAuditActions.REGISTRATION.name(), UserAuditActions.FAILED.name(), req);
        am.registerRoleChange(user, RolesAuditActions.ADDROLE.name(), RolesAuditActions.FAILED.name(), BBCGroup.BBC_GUEST.name(), user, req);

        throw new AppException(Response.Status.INTERNAL_SERVER_ERROR.
                getStatusCode(),
                "Cannot register now due to email service problems");

      }
      return true;
    }

    return false;
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
          am.registerLoginInfo(user, UserAuditActions.RECOVERY.name(), UserAuditActions.FAILED.name(), req);
        } catch (MessagingException ex) {
          Logger.getLogger(UsersController.class.getName()).
                  log(Level.SEVERE, null, ex);
          try {
            am.registerLoginInfo(user, UserAuditActions.RECOVERY.name(), UserAuditActions.FAILED.name(), req);
          } catch (SocketException ex1) {
            Logger.getLogger(UsersController.class.getName()).
                    log(Level.SEVERE, null, ex1);
          }
        } catch (SocketException ex) {
          Logger.getLogger(UsersController.class.getName()).
                  log(Level.SEVERE, null, ex);
        }
   
        throw new AppException(Response.Status.BAD_REQUEST.getStatusCode(),
                ResponseMessages.SEC_QA_INCORRECT);
      }

      String randomPassword = SecurityUtils.getRandomPassword(
              UserValidator.PASSWORD_MIN_LENGTH);
      try {
        String message = UserAccountsEmailMessages.buildTempResetMessage(
                randomPassword);
        emailBean.sendEmail(email,
                UserAccountsEmailMessages.ACCOUNT_PASSWORD_RESET, message);
        user.setPassword(DigestUtils.sha256Hex(randomPassword));
        //user.setStatus(PeopleAccountStatus.ACCOUNT_PENDING.getValue());
        userBean.update(user);
        resetFalseLogin(user);
        am.registerLoginInfo(user, UserAuditActions.RECOVERY.name(), UserAuditActions.SUCCESS.name(), req);
      } catch (MessagingException ex) {
        Logger.getLogger(AuthService.class.getName()).log(Level.SEVERE,
                "Could not send email: ", ex);
        throw new AppException(Response.Status.BAD_REQUEST.getStatusCode(),
                ResponseMessages.EMAIL_SENDING_FAILURE);
      } catch (SocketException ex) {
        Logger.getLogger(UsersController.class.getName()).
                log(Level.SEVERE, null, ex);
      }
    }
  }

  public void changePassword(String email, String oldPassword,
          String newPassword, String confirmedPassword, HttpServletRequest req) throws AppException {
    Users user = userBean.findByEmail(email);

    if (user == null) {
      throw new AppException(Response.Status.NOT_FOUND.getStatusCode(),
              ResponseMessages.USER_WAS_NOT_FOUND);
    }
    if (!user.getPassword().equals(DigestUtils.sha256Hex(oldPassword))) {
      try {
        am.registerLoginInfo(user, UserAuditActions.PASSWORD.name(), UserAuditActions.FAILED.name(), req);
      } catch (SocketException ex) {
        Logger.getLogger(UsersController.class.getName()).
                log(Level.SEVERE, null, ex);
      }
      throw new AppException(Response.Status.BAD_REQUEST.getStatusCode(),
              ResponseMessages.PASSWORD_INCORRECT);
      
    }
    if (userValidator.isValidPassword(newPassword, confirmedPassword)) {
      user.setPassword(DigestUtils.sha256Hex(newPassword));
      userBean.update(user);
      try {
        am.registerLoginInfo(user, UserAuditActions.PASSWORD.name(), UserAuditActions.SUCCESS.name(), req);
      } catch (SocketException ex) {
        Logger.getLogger(UsersController.class.getName()).
                log(Level.SEVERE, null, ex);
      }
    }
  }

  public void changeSecQA(String email, String oldPassword,
          String securityQuestion, String securityAnswer, HttpServletRequest req) throws AppException {
    Users user = userBean.findByEmail(email);

    if (user == null) {
      throw new AppException(Response.Status.NOT_FOUND.getStatusCode(),
              ResponseMessages.USER_WAS_NOT_FOUND);
    }
    if (!user.getPassword().equals(DigestUtils.sha256Hex(oldPassword))) {
      try {
        am.registerLoginInfo(user, UserAuditActions.SECQUESTION.name(), UserAuditActions.FAILED.name(), req);
      } catch (SocketException ex) {
        Logger.getLogger(UsersController.class.getName()).
                log(Level.SEVERE, null, ex);
      }
      throw new AppException(Response.Status.BAD_REQUEST.getStatusCode(),
              ResponseMessages.PASSWORD_INCORRECT);
    }

    if (userValidator.isValidsecurityQA(securityQuestion, securityAnswer)) {
      user.setSecurityQuestion(SecurityQuestion.getQuestion(securityQuestion));
      user.
              setSecurityAnswer(DigestUtils.sha256Hex(securityAnswer.
                              toLowerCase()));
      userBean.update(user);
      try {
        am.registerLoginInfo(user, UserAuditActions.SECQUESTION.name(), UserAuditActions.SUCCESS.name(), req);
      } catch (SocketException ex) {
        Logger.getLogger(UsersController.class.getName()).
                log(Level.SEVERE, null, ex);
      }
    }
  }

  public UserDTO updateProfile(String email, String firstName, String lastName,
          String telephoneNum, HttpServletRequest req) throws AppException {
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
    try {
      am.registerLoginInfo(user, UserAuditActions.SECQUESTION.name(), UserAuditActions.SUCCESS.name(), req);
    } catch (SocketException ex) {
      Logger.getLogger(UsersController.class.getName()).log(Level.SEVERE, null,
              ex);
    }
    userBean.update(user);
    return new UserDTO(user);
  }

  public void registerFalseLogin(Users user) throws MessagingException {
    if (user != null) {
      int count = user.getFalseLogin() + 1;
      user.setFalseLogin(count);

      // block the user account if more than allowed false logins
      if (count > AuthenticationConstants.ALLOWED_FALSE_LOGINS) {
        user.setStatus(UserAccountStatus.ACCOUNT_BLOCKED.getValue());

        emailBean.sendEmail(user.getEmail(),
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


}
