package se.kth.hopsworks.controller;

import com.google.zxing.WriterException;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URISyntaxException;
import java.sql.Timestamp;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.faces.FacesException;
import javax.faces.context.ExternalContext;
import javax.faces.context.FacesContext;
import javax.mail.MessagingException;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.core.Response;
import org.apache.commons.codec.digest.DigestUtils;
import se.kth.bbc.security.ua.EmailBean;
import se.kth.bbc.security.ua.SecurityQuestion;
import se.kth.bbc.security.ua.UserAccountsEmailMessages;
import se.kth.bbc.security.audit.model.Userlogins;
import se.kth.bbc.security.auth.CustomAuthentication;
import se.kth.bbc.security.auth.QRCodeGenerator;
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
  private UserLoginsFacade userLoginsBean;

  @EJB
  private UserManager mgr;
    // To send the user the QR code image
  private byte[] qrCode;
  
    // BiobankCloud prefix username prefix
  private final String USERNAME_PREFIX = "meb";
  
  public byte[] registerUser(UserDTO newUser, String url, String ip, String os, String browser, String mac) throws AppException, IOException, UnsupportedEncodingException, WriterException, MessagingException {
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
      String activationKey = SecurityUtils.getRandomString(64);
      
      int uid = userBean.lastUserID() + 1;

     // String uname = LocalhostServices.getUsernameFromEmail(newUser.getEmail());

      String uname =  USERNAME_PREFIX + uid;
      List<BbcGroup> groups = new ArrayList<>();
      groups.add(groupBean.findByGroupName(BbcGroup.USER));

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
      user.setYubikeyUser(PeopleAccountStatus.MOBILE_USER.getValue());
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
      
      Organization org  = new Organization();
      org.setUid(user);
      org.setContactEmail("-");
      org.setContactPerson("-");
      org.setDepartment("-");
      org.setFax("-");
      org.setOrgName("-");
      org.setWebsite("-");
      org.setPhone("-");
      
      user.setOrganization(org);
      userBean.persist(user);
      
      qrCode = QRCodeGenerator.getQRCodeBytes(newUser.getEmail(), CustomAuthentication.ISSUER,
              otpSecret);
      // Notify user about the request
      emailBean.sendEmail(newUser.getEmail(),
              UserAccountsEmailMessages.ACCOUNT_REQUEST_SUBJECT,
              UserAccountsEmailMessages.buildMobileRequestMessage(
                      //getApplicationUri()
                      url
                      , user.getUsername() + activationKey));
      return qrCode;
    }
    
    return null;
  }
  
  
  public boolean registerYubikeyUser(UserDTO newUser, String url, String ip, String os, String browser, String mac) throws AppException, IOException, UnsupportedEncodingException, WriterException, MessagingException {
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
      String activationKey = SecurityUtils.getRandomString(64);
      
      int uid = userBean.lastUserID() + 1;

     // String uname = LocalhostServices.getUsernameFromEmail(newUser.getEmail());

      String uname =  USERNAME_PREFIX + uid;
      List<BbcGroup> groups = new ArrayList<>();
      groups.add(groupBean.findByGroupName(BbcGroup.USER));

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
      user.setYubikeyUser(PeopleAccountStatus.YUBIKEY_USER.getValue());
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
      a.setAddress1(newUser.getStreet());
      a.setAddress2("-");
      a.setAddress3(newUser.getDep());
      a.setCity(newUser.getCity());
      a.setCountry(newUser.getCountry());
      a.setPostalcode(newUser.getPostCode());
      a.setState("-");
      user.setAddress(a);
      
      Organization org  = new Organization();
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
      userBean.persist(user);
      
    
      // Notify user about the request
      emailBean.sendEmail(newUser.getEmail(),
              UserAccountsEmailMessages.ACCOUNT_REQUEST_SUBJECT,
              UserAccountsEmailMessages.buildYubikeyRequestMessage(
                      //getApplicationUri()
                      url
                      , user.getUsername() + activationKey));
      return true;
    }
    
    return false;
  }
  
  
  // fix this
    public String getApplicationUri() {
    try {
      FacesContext ctxt = FacesContext.getCurrentInstance();
      ExternalContext ext = ctxt.getExternalContext();
      URI uri = new URI(ext.getRequestScheme(),
              null, ext.getRequestServerName(), ext.getRequestServerPort(),
              ext.getRequestContextPath(), null, null);
      return uri.toASCIIString();
    } catch (URISyntaxException e) {
      throw new FacesException(e);
    }
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
        registerFalseLogin(user);
        registerLoginInfo(user, "Recovery", "Failure",req);
        throw new AppException(Response.Status.BAD_REQUEST.getStatusCode(),
            ResponseMessages.SEC_QA_INCORRECT);
      }

      String randomPassword = getRandomPassword(
          UserValidator.PASSWORD_MIN_LENGTH);
      try {
        String message = UserAccountsEmailMessages.buildTempResetMessage(
            randomPassword);
        emailBean.sendEmail(email,
            UserAccountsEmailMessages.ACCOUNT_PASSWORD_RESET, message);
        user.setPassword(DigestUtils.sha256Hex(randomPassword));
        userBean.update(user);
        resetFalseLogin(user);
        registerLoginInfo(user, "Recovery", "SUCCESS", req);
      } catch (MessagingException ex) {
        Logger.getLogger(AuthService.class.getName()).log(Level.SEVERE,
            "Could not send email: ", ex);
        throw new AppException(Response.Status.BAD_REQUEST.getStatusCode(),
            ResponseMessages.EMAIL_SENDING_FAILURE);
      }
    }
  }

  public void changePassword(String email, String oldPassword,
      String newPassword, String confirmedPassword) throws AppException {
    Users user = userBean.findByEmail(email);

    if (user == null) {
      throw new AppException(Response.Status.NOT_FOUND.getStatusCode(),
          ResponseMessages.USER_WAS_NOT_FOUND);
    }
    if (!user.getPassword().equals(DigestUtils.sha256Hex(oldPassword))) {
      throw new AppException(Response.Status.BAD_REQUEST.getStatusCode(),
          ResponseMessages.PASSWORD_INCORRECT);
    }
    if (userValidator.isValidPassword(newPassword, confirmedPassword)) {
      user.setPassword(DigestUtils.sha256Hex(newPassword));
      userBean.update(user);
    }
  }

  public void changeSecQA(String email, String oldPassword,
      String securityQuestion, String securityAnswer) throws AppException {
    Users user = userBean.findByEmail(email);

    if (user == null) {
      throw new AppException(Response.Status.NOT_FOUND.getStatusCode(),
          ResponseMessages.USER_WAS_NOT_FOUND);
    }
    if (!user.getPassword().equals(DigestUtils.sha256Hex(oldPassword))) {
      throw new AppException(Response.Status.BAD_REQUEST.getStatusCode(),
          ResponseMessages.PASSWORD_INCORRECT);
    }

    if (userValidator.isValidsecurityQA(securityQuestion, securityAnswer)) {
      user.setSecurityQuestion(SecurityQuestion.getQuestion(securityQuestion));
      user.setSecurityAnswer(DigestUtils.sha256Hex(securityAnswer. toLowerCase()));
      userBean.update(user);
    }
  }

  public UserDTO updateProfile(String email, String firstName, String lastName,
      String telephoneNum) throws AppException {
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

    userBean.update(user);
    return new UserDTO(user);
  }

  private String getRandomPassword(int length) {
    String randomStr = UUID.randomUUID().toString();
    while (randomStr.length() < length) {
      randomStr += UUID.randomUUID().toString();
    }
    return randomStr.substring(0, length);
  }

  public void registerLoginInfo(Users user, String action, String outcome,
      HttpServletRequest req) {
    String ip = req.getRemoteAddr();
    String userAgent = req.getHeader("User-Agent");
    String browser = null;
    Logger.getLogger(AuthService.class.getName()).log(Level.SEVERE,
        "User agent --->>> {0}", userAgent);
    if (userAgent.contains("MSIE")) {
      browser = "Internet Explorer";
    } else if (userAgent.contains("Firefox")) {
      browser = "Firefox";
    } else if (userAgent.contains("Chrome")) {
      browser = "Chrome";
    } else if (userAgent.contains("Opera")) {
      browser = "Opera";
    } else if (userAgent.contains("Safari")) {
      browser = "Safari";
    }
    Userlogins login = new Userlogins();
    login.setUid(user.getUid());
    login.setBrowser(browser);
    login.setIp(ip);
    login.setAction(action);
    login.setOutcome(outcome);
    login.setLoginDate(new Date());
    userLoginsBean.persist(login);
  }

  public void registerFalseLogin(Users user) {
    if (user != null) {
      int count = user.getFalseLogin() + 1;
      user.setFalseLogin(count);
      if (count > Users.ALLOWED_FALSE_LOGINS) {
        user.setStatus(UserAccountStatus.ACCOUNT_BLOCKED.getValue());
      }
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
