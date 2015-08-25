package se.kth.hopsworks.controller;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;
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
import se.kth.bbc.security.ua.EmailBean;
import se.kth.bbc.security.ua.SecurityQuestion;
import se.kth.bbc.security.ua.UserAccountsEmailMessages;
import se.kth.bbc.security.ua.model.Userlogins;
import se.kth.hopsworks.rest.AppException;
import se.kth.hopsworks.rest.AuthService;
import se.kth.hopsworks.user.model.BbcGroup;
import se.kth.hopsworks.user.model.UserAccountStatus;
import se.kth.hopsworks.user.model.Users;
import se.kth.hopsworks.users.BbcGroupFacade;
import se.kth.hopsworks.users.UserDTO;
import se.kth.hopsworks.users.UserFacade;
import se.kth.hopsworks.users.UserLoginsFacade;

/**
 * @author André<amore@kth.se>
 * @author Ermias<ermiasg@kth.se>
 */
@Stateless
//the operations in this method does not need any transaction
//the acctual persisting will in the facades transaction.
@TransactionAttribute(TransactionAttributeType.NEVER)
public class UsersController {

  @EJB
  private UserFacade userBean;
  @EJB
  private UserValidator userValidator;
  @EJB
  private EmailBean emailBean;
  @EJB
  private BbcGroupFacade groupBean;
  @EJB
  private UserLoginsFacade userLoginsBean;

  public void registerUser(UserDTO newUser) throws AppException {
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

      int uid = userBean.lastUserID() + 1;
      String uname = Users.USERNAME_PREFIX + uid;
      List<BbcGroup> groups = new ArrayList<>();
      groups.add(groupBean.findByGroupName(BbcGroup.USER));

      Users user = new Users(uid);
      user.setUsername(uname);
      user.setEmail(newUser.getEmail());
      user.setFname(newUser.getFirstName());
      user.setLname(newUser.getLastName());
      user.setMobile(newUser.getTelephoneNum());
      user.setStatus(UserAccountStatus.ACCOUNT_INACTIVE.getValue());
      user.setSecurityQuestion(SecurityQuestion.getQuestion(newUser.
              getSecurityQuestion()));
      user.setPassword(DigestUtils.sha256Hex(newUser.getChosenPassword()));
      user.setSecurityAnswer(DigestUtils.sha256Hex(newUser.getSecurityAnswer().
              toLowerCase()));
      user.setBbcGroupCollection(groups);

      userBean.persist(user);
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
        registerLoginInfo(user, "False recovery", req);
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
        registerLoginInfo(user, "Successful recovery", req);
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
      user.
              setSecurityAnswer(DigestUtils.sha256Hex(securityAnswer.
                              toLowerCase()));
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

  public void registerLoginInfo(Users user, String action,
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

}
