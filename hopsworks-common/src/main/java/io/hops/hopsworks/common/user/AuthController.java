/*
 * Changes to this file committed after and not including commit-id: ccc0d2c5f9a5ac661e60e6eaf138de7889928b8b
 * are released under the following license:
 *
 * This file is part of Hopsworks
 * Copyright (C) 2018, Logical Clocks AB. All rights reserved
 *
 * Hopsworks is free software: you can redistribute it and/or modify it under the terms of
 * the GNU Affero General Public License as published by the Free Software Foundation,
 * either version 3 of the License, or (at your option) any later version.
 *
 * Hopsworks is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR
 * PURPOSE.  See the GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License along with this program.
 * If not, see <https://www.gnu.org/licenses/>.
 *
 * Changes to this file committed before and including commit-id: ccc0d2c5f9a5ac661e60e6eaf138de7889928b8b
 * are released under the following license:
 *
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
 */

package io.hops.hopsworks.common.user;

import io.hops.hopsworks.common.dao.certificates.CertsFacade;
import io.hops.hopsworks.common.dao.certificates.ProjectGenericUserCerts;
import io.hops.hopsworks.common.dao.certificates.UserCerts;
import io.hops.hopsworks.common.dao.project.Project;
import io.hops.hopsworks.common.dao.project.ProjectFacade;
import io.hops.hopsworks.common.dao.user.BbcGroup;
import io.hops.hopsworks.common.dao.user.BbcGroupFacade;
import io.hops.hopsworks.common.dao.user.UserFacade;
import io.hops.hopsworks.common.dao.user.Users;
import io.hops.hopsworks.common.dao.user.ldap.LdapUser;
import io.hops.hopsworks.common.dao.user.ldap.LdapUserFacade;
import io.hops.hopsworks.common.dao.user.security.audit.AccountAuditFacade;
import io.hops.hopsworks.common.dao.user.security.audit.AccountsAuditActions;
import io.hops.hopsworks.common.dao.user.security.audit.RolesAuditAction;
import io.hops.hopsworks.common.dao.user.security.audit.UserAuditActions;
import io.hops.hopsworks.common.dao.user.security.ua.SecurityQuestion;
import io.hops.hopsworks.common.dao.user.security.ua.SecurityUtils;
import io.hops.hopsworks.common.dao.user.security.ua.UserAccountStatus;
import io.hops.hopsworks.common.dao.user.security.ua.UserAccountType;
import io.hops.hopsworks.common.dao.user.security.ua.UserAccountsEmailMessages;
import io.hops.hopsworks.common.exception.RESTCodes;
import io.hops.hopsworks.common.exception.ServiceException;
import io.hops.hopsworks.common.exception.UserException;
import io.hops.hopsworks.common.security.CertificatesMgmService;
import io.hops.hopsworks.common.user.ldap.LdapRealm;
import io.hops.hopsworks.common.util.EmailBean;
import io.hops.hopsworks.common.util.HopsUtils;
import io.hops.hopsworks.common.util.Settings;
import org.apache.commons.codec.digest.DigestUtils;

import javax.ejb.EJB;
import javax.ejb.EJBException;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.naming.NamingException;
import javax.security.auth.login.LoginException;
import javax.servlet.http.HttpServletRequest;
import java.io.UnsupportedEncodingException;
import java.security.SecureRandom;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Date;
import java.util.List;
import java.util.Random;
import java.util.logging.Level;
import java.util.logging.Logger;

@Stateless
@TransactionAttribute(TransactionAttributeType.NEVER)
public class AuthController {

  private final static Logger LOGGER = Logger.getLogger(AuthController.class.getName());
  private final static int SALT_LENGTH = 64;
  private final static int RANDOM_PWD_LEN = 8;
  private final static Random RANDOM = new SecureRandom();

  @EJB
  private UserFacade userFacade;
  @EJB
  private BbcGroupFacade bbcGroupFacade;
  @EJB
  private UserStatusValidator userStatusValidator;
  @EJB
  private Settings settings;
  @EJB
  private AccountAuditFacade accountAuditFacade;
  @EJB
  private EmailBean emailBean;
  @EJB
  private CertsFacade userCertsFacade;
  @EJB
  private ProjectFacade projectFacade;
  @EJB
  private CertificatesMgmService certificatesMgmService;
  @EJB
  private LdapRealm ldapRealm;
  @EJB
  private LdapUserFacade ldapUserFacade;

  /**
   * Pre check for custom realm login.
   *
   * @param user
   * @param password
   * @param otp
   * @param req
   * @return
   */
  public String preCustomRealmLoginCheck(Users user, String password, String otp, HttpServletRequest req)
    throws UserException {
    if (user == null) {
      throw new IllegalArgumentException("User not set.");
    }
    if (user.getMode().equals(UserAccountType.LDAP_ACCOUNT_TYPE)) {
      throw new IllegalArgumentException("Can not login ldap user. Use LDAP login.");
    }
    if (isTwoFactorEnabled(user)) {
      if ((otp == null || otp.isEmpty()) && user.getMode().equals(UserAccountType.M_ACCOUNT_TYPE)) {
        if (checkPasswordAndStatus(user, password, req)) {
          throw new IllegalStateException("Second factor required.");
        }
      }
    }

    // Add padding if custom realm is disabled
    if (otp == null || otp.isEmpty() && user.getMode().equals(UserAccountType.M_ACCOUNT_TYPE)) {
      otp = Settings.MOBILE_OTP_PADDING;
    }
    String newPassword = getPasswordPlusSalt(password, user.getSalt());
    if (otp.length() == Settings.MOBILE_OTP_PADDING.length() && user.getMode().equals(UserAccountType.M_ACCOUNT_TYPE)) {
      newPassword = newPassword + otp;
    } else {
      throw new IllegalArgumentException("Could not recognize the account type. Report a bug.");
    }
    return newPassword;
  }

  public String preLdapLoginCheck(Users user, String password) {
    if (user == null) {
      throw new IllegalArgumentException("User not set.");
    }
    if (!user.getMode().equals(UserAccountType.LDAP_ACCOUNT_TYPE)) {
      throw new IllegalArgumentException("User is not registerd as ldap user.");
    }
    return getPasswordPlusSalt(password, user.getSalt()) + Settings.MOBILE_OTP_PADDING;
  }

  /**
   * Validates password and update account audit. Use validatePwd if ldap user.
   *
   * @param user
   * @param password
   * @param req
   * @return
   */
  public boolean validatePassword(Users user, String password, HttpServletRequest req) {
    if (user == null) {
      throw new IllegalArgumentException("User not set.");
    }
    if (user.getMode().equals(UserAccountType.LDAP_ACCOUNT_TYPE)) {
      throw new IllegalArgumentException("Operation not allowed for LDAP account.");
    }
    String userPwdHash = user.getPassword();
    String pwdHash = getPasswordHash(password, user.getSalt());
    if (!userPwdHash.equals(pwdHash)) {
      registerFalseLogin(user, req);
      LOGGER.log(Level.WARNING, "False login attempt by user: {0}", user.getEmail());
      return false;
    }
    resetFalseLogin(user);
    return true;
  }
  
  /**
   * Validate password works both for hopsworks and ldap user
   * @param user
   * @param password
   * @param req
   * @return
   */
  public boolean validatePwd(Users user, String password, HttpServletRequest req) {
    if (user == null) {
      throw new IllegalArgumentException("User not set.");
    }
    if (user.getMode().equals(UserAccountType.LDAP_ACCOUNT_TYPE)) {
      LdapUser ldapUser = ldapUserFacade.findByUsers(user);
      if (ldapUser == null) {
        return false;
      }
      try {
        ldapRealm.authenticateLdapUser(ldapUser, password);
        return true;
      } catch (LoginException ex) {
        LOGGER.log(Level.WARNING, "False login attempt by ldap user: {0}", user.getEmail());
        LOGGER.log(Level.WARNING, null, ex.getMessage());
        return false;
      } catch (EJBException | NamingException ee) {
        LOGGER.log(Level.WARNING, "Could not reach LDAP server. {0}", ee.getMessage());
        throw new IllegalStateException("Could not reach LDAP server.");
      }
    }
    String userPwdHash = user.getPassword();
    String pwdHash = getPasswordHash(password, user.getSalt());
    if (!userPwdHash.equals(pwdHash)) {
      registerFalseLogin(user, req);
      LOGGER.log(Level.WARNING, "False login attempt by user: {0}", user.getEmail());
      return false;
    }
    resetFalseLogin(user);
    return true;
  }

  /**
   * Validate security question and update false login attempts
   *
   * @param user
   * @param securityQ
   * @param securityAnswer
   * @param req
   * @return
   */
  public boolean validateSecurityQA(Users user, String securityQ, String securityAnswer, HttpServletRequest req) {
    if (user == null) {
      throw new IllegalArgumentException("User not set.");
    }
    if (user.getMode().equals(UserAccountType.LDAP_ACCOUNT_TYPE)) {
      throw new IllegalArgumentException("Operation not allowed for LDAP account.");
    }
    if (securityQ == null || securityQ.isEmpty() || securityAnswer == null || securityAnswer.isEmpty()) {
      return false;
    }
    if (!user.getSecurityQuestion().getValue().equalsIgnoreCase(securityQ)
        || !user.getSecurityAnswer().equals(DigestUtils.sha256Hex(securityAnswer.toLowerCase()))) {
      registerFalseLogin(user, req);
      LOGGER.log(Level.WARNING, "False Security Question attempt by user: {0}", user.getEmail());
      return false;
    }
    return true;
  }

  /**
   * Checks password and user status. Also updates false login attempts
   *
   * @param user
   * @param password
   * @param req
   * @return
   */
  public boolean checkPasswordAndStatus(Users user, String password, HttpServletRequest req) throws UserException {
    if (user == null) {
      throw new IllegalArgumentException("User not set.");
    }
    if (!validatePassword(user, password, req)) {
      return false;
    }
    return userStatusValidator.checkStatus(user.getStatus());
  }

  /**
   * Validates email validation key. Also updates false key validation attempts.
   *
   * @param key
   * @param req
   */
  public void validateKey(String key, HttpServletRequest req) throws UserException {
    if (key == null) {
      throw new IllegalArgumentException("the validation key should not be null");
    }
    if (key.length() <= Settings.USERNAME_LENGTH) {
      throw new IllegalArgumentException(
        "The validation key is invalid. Minimum length is: " + Settings.USERNAME_LENGTH);
    }
    String userName = key.substring(0, Settings.USERNAME_LENGTH);
    // get the 8 char username
    String secret = key.substring(Settings.USERNAME_LENGTH);
    Users user = userFacade.findByUsername(userName);
    if (user == null) {
      throw new UserException(RESTCodes.UserErrorCode.USER_WAS_NOT_FOUND, Level.FINE);
    }
    if (!secret.equals(user.getValidationKey())) {
      registerFalseKeyValidation(user, req);
      throw new UserException(RESTCodes.UserErrorCode.INCORRECT_VALIDATION_KEY, Level.FINE,
        "user: " + user.getUsername());
    }

    if (!user.getStatus().equals(UserAccountStatus.NEW_MOBILE_ACCOUNT)) {
      switch (user.getStatus()) {
        case VERIFIED_ACCOUNT:
          throw new UserException(RESTCodes.UserErrorCode.ACCOUNT_INACTIVE, Level.FINE);
        case ACTIVATED_ACCOUNT:
          throw new UserException(RESTCodes.UserErrorCode.ACCOUNT_ALREADY_VERIFIED, Level.FINE);
        default:
          throw new UserException(RESTCodes.UserErrorCode.ACCOUNT_BLOCKED, Level.FINE);
      }
    }

    user.setStatus(UserAccountStatus.VERIFIED_ACCOUNT);
    userFacade.update(user);
    accountAuditFacade.registerRoleChange(user, UserAccountStatus.VERIFIED_ACCOUNT.name(), RolesAuditAction.SUCCESS.
        name(), "Account verification", user, req);
  }

  /**
   * Sends new activation key to the given user.
   *
   * @param user
   * @param req
   * @throws MessagingException
   */
  public void sendNewValidationKey(Users user, HttpServletRequest req) throws MessagingException {
    if (user == null) {
      throw new IllegalArgumentException("User not set.");
    }
    String activationKey = SecurityUtils.getRandomPassword(RANDOM_PWD_LEN);
    emailBean.sendEmail(user.getEmail(), Message.RecipientType.TO, UserAccountsEmailMessages.ACCOUNT_REQUEST_SUBJECT,
        UserAccountsEmailMessages.buildMobileRequestMessageRest(settings.getVerificationEndpoint(), user.getUsername()
            + activationKey));
    user.setValidationKey(activationKey);
    userFacade.update(user);
  }

  /**
   * Reset password with random string and send email to user with the new password.
   *
   * @param user
   * @param req
   * @throws MessagingException
   * @throws Exception
   */
  public void resetPassword(Users user, HttpServletRequest req) throws ServiceException {
    if (user == null) {
      throw new IllegalArgumentException("User not set.");
    }
    if (userStatusValidator.isBlockedAccount(user)) {
      throw new IllegalStateException("User is blocked.");
    }
    String randomPassword = SecurityUtils.getRandomPassword(UserValidator.PASSWORD_MIN_LENGTH);
    String message = UserAccountsEmailMessages.buildTempResetMessage(randomPassword);
    try {
      emailBean.sendEmail(user.getEmail(), Message.RecipientType.TO, UserAccountsEmailMessages.ACCOUNT_PASSWORD_RESET,
        message);
    } catch (MessagingException ex){
      throw new ServiceException(RESTCodes.ServiceErrorCode.EMAIL_SENDING_FAILURE,
        Level.SEVERE, "user: " + user.getUsername(), ex.getMessage(), ex);
    }
    changePassword(user, randomPassword, req);
    resetFalseLogin(user);
    accountAuditFacade.registerAccountChange(user, AccountsAuditActions.RECOVERY.name(), UserAuditActions.SUCCESS.
        name(), "Password reset.", user, req);
  }

  /**
   * Test if two factor is enabled in the system and by the user
   *
   * @param user
   * @return
   */
  public boolean isTwoFactorEnabled(Users user) {
    String twoFactorAuth = settings.getTwoFactorAuth();
    String twoFactorExclude = settings.getTwoFactorExclude();
    String twoFactorMode = (twoFactorAuth != null ? twoFactorAuth : "");
    String excludes = (twoFactorExclude != null ? twoFactorExclude : null);
    String[] groups = (excludes != null && !excludes.isEmpty() ? excludes.split(";") : new String[]{});

    for (String group : groups) {
      if (isUserInRole(user, group)) {
        return false; //will allow anyone if one of the users groups are in the exclude list
      }
    }
    if (twoFactorMode.equals(Settings.TwoFactorMode.MANDATORY.getName())) {
      return true;
    } else if (twoFactorMode.equals(Settings.TwoFactorMode.OPTIONAL.getName()) && user.getTwoFactor()) {
      return true;
    }

    return false;
  }

  /**
   * Test if two factor is enabled
   *
   * @return
   */
  public boolean isTwoFactorEnabled() {
    String twoFactorAuth = settings.getTwoFactorAuth();
    String twoFactorMode = (twoFactorAuth != null ? twoFactorAuth : "");
    return twoFactorMode.equals(Settings.TwoFactorMode.MANDATORY.getName()) || twoFactorMode.equals(
        Settings.TwoFactorMode.OPTIONAL.getName());
  }

  /**
   * Hash password + salt
   *
   * @param password
   * @param salt
   * @return
   */
  public String getPasswordHash(String password, String salt) {
    return getHash(getPasswordPlusSalt(password, salt));
  }

  /**
   * Returns the hash of the value
   *
   * @param val
   * @return
   */
  public String getHash(String val) {
    return DigestUtils.sha256Hex(val);
  }

  /**
   * Change password to the given password. Will generate a new salt
   *
   * @param user
   * @param password
   * @param req
   * @throws Exception
   */
  @TransactionAttribute(TransactionAttributeType.REQUIRED)
  public void changePassword(Users user, String password, HttpServletRequest req) {
    String salt = generateSalt();
    String passwordWithSalt = getPasswordHash(password, salt);
    String oldPassword = user.getPassword();
    user.setPassword(passwordWithSalt);
    user.setSalt(salt);
    user.setPasswordChanged(new Timestamp(new Date().getTime()));
    userFacade.update(user);
    resetProjectCertPassword(user, oldPassword);
  }

  /**
   * Change security question and adds account audit for the operation.
   *
   * @param user
   * @param securityQuestion
   * @param securityAnswer
   * @param req
   */
  public void changeSecQA(Users user, String securityQuestion, String securityAnswer, HttpServletRequest req) {
    user.setSecurityQuestion(SecurityQuestion.getQuestion(securityQuestion));
    user.setSecurityAnswer(DigestUtils.sha256Hex(securityAnswer.toLowerCase()));
    userFacade.update(user);
    accountAuditFacade.registerAccountChange(user, AccountsAuditActions.SECQUESTION.name(),
        AccountsAuditActions.SUCCESS.name(), "Changed Security Question.", user, req);
  }

  /**
   * Concatenates password and salt
   *
   * @param password
   * @param salt
   * @return
   */
  public String getPasswordPlusSalt(String password, String salt) {
    return password + salt;
  }

  private void resetProjectCertPassword(Users p, String oldPass) {
    //For every project, change the certificate secret in the database
    //Get cert password by decrypting it with old password
    List<Project> projects = projectFacade.findAllMemberStudies(p);
    List<ProjectGenericUserCerts> pguCerts = null;
    try {
      for (Project project : projects) {
        UserCerts userCert = userCertsFacade.findUserCert(project.getName(), p.getUsername());
        String masterEncryptionPassword = certificatesMgmService.getMasterEncryptionPassword();
        String certPassword = HopsUtils.decrypt(oldPass, userCert.getUserKeyPwd(), masterEncryptionPassword);
        //Encrypt it with new password and store it in the db
        String newSecret = HopsUtils.encrypt(p.getPassword(), certPassword, masterEncryptionPassword);
        userCert.setUserKeyPwd(newSecret);
        userCertsFacade.update(userCert);

        //If user is owner of the project, update projectgenericuser certs as well
        if (project.getOwner().equals(p)) {
          if (pguCerts == null) {
            pguCerts = new ArrayList<>();
          }
          ProjectGenericUserCerts pguCert = userCertsFacade.findProjectGenericUserCerts(project.getName()
              + Settings.PROJECT_GENERIC_USER_SUFFIX);
          pguCerts.add(userCertsFacade.findProjectGenericUserCerts(project.getName()
              + Settings.PROJECT_GENERIC_USER_SUFFIX));
          String pguCertPassword = HopsUtils.decrypt(oldPass, pguCert.getCertificatePassword(),
              masterEncryptionPassword);
          //Encrypt it with new password and store it in the db
          String newPguSecret = HopsUtils.encrypt(p.getPassword(), pguCertPassword, masterEncryptionPassword);
          pguCert.setCertificatePassword(newPguSecret);
          userCertsFacade.updatePGUCert(pguCert);
        }
      }
    } catch (Exception ex) {
      LOGGER.log(Level.SEVERE, null, ex);
      throw new EJBException(ex);
    }

  }

  /**
   * Register failed login attempt.
   *
   * @param user
   * @param req
   */
  public void registerFalseLogin(Users user, HttpServletRequest req) {
    if (user != null) {
      int count = user.getFalseLogin() + 1;
      user.setFalseLogin(count);

      // block the user account if more than allowed false logins
      if (count > Settings.ALLOWED_FALSE_LOGINS) {
        user.setStatus(UserAccountStatus.BLOCKED_ACCOUNT);
        try {
          emailBean.sendEmail(user.getEmail(), Message.RecipientType.TO,
              UserAccountsEmailMessages.ACCOUNT_BLOCKED__SUBJECT, UserAccountsEmailMessages.accountBlockedMessage());
        } catch (MessagingException ex) {
          LOGGER.log(Level.SEVERE, "Failed to send email. ", ex);
        }
        accountAuditFacade.registerRoleChange(user, UserAccountStatus.SPAM_ACCOUNT.name(), RolesAuditAction.SUCCESS.
            name(), "False login retries:" + Integer.toString(count), user, req);
      }
      // notify user about the false attempts
      userFacade.update(user);
    }
  }

  /**
   * Registers failed email validation
   *
   * @param user
   * @param req
   */
  public void registerFalseKeyValidation(Users user, HttpServletRequest req) {
    if (user != null) {
      int count = user.getFalseLogin() + 1;
      user.setFalseLogin(count);

      // make the user spam account if more than allowed tries
      if (count > Settings.ACCOUNT_VALIDATION_TRIES) {
        user.setStatus(UserAccountStatus.SPAM_ACCOUNT);
      }
      userFacade.update(user);
      accountAuditFacade.registerRoleChange(user, UserAccountStatus.SPAM_ACCOUNT.name(), RolesAuditAction.SUCCESS.
          name(), "Wrong validation key retries: " + Integer.toString(count), user, req);
    }
  }

  /**
   * Set user online, resets false login attempts and register login audit info
   *
   * @param user
   * @param req
   */
  public void registerLogin(Users user, HttpServletRequest req) {
    resetFalseLogin(user);
    setUserOnlineStatus(user, Settings.IS_ONLINE);
    accountAuditFacade.registerLoginInfo(user, UserAuditActions.LOGIN.name(), UserAuditActions.SUCCESS.name(), req);
    LOGGER.log(Level.INFO, "Logged in user: {0}. ", user.getEmail());
  }

  /**
   * Set user offline and register login audit info
   *
   * @param user
   * @param req
   */
  public void registerLogout(Users user, HttpServletRequest req) {
    setUserOnlineStatus(user, Settings.IS_OFFLINE);
    accountAuditFacade.registerLoginInfo(user, UserAuditActions.LOGOUT.name(), UserAuditActions.SUCCESS.name(), req);
    LOGGER.log(Level.INFO, "Logged out user: {0}. ", user.getEmail());
  }

  /**
   * Register authentication failure and register login audit info
   *
   * @param user
   * @param req
   */
  public void registerAuthenticationFailure(Users user, HttpServletRequest req) {
    registerFalseLogin(user, req);
    accountAuditFacade.registerLoginInfo(user, UserAuditActions.LOGIN.name(), UserAuditActions.FAILED.name(), req);
    LOGGER.log(Level.INFO, "Authentication failure user: {0}. ", user.getEmail());
  }

  private void resetFalseLogin(Users user) {
    if (user != null) {
      user.setFalseLogin(0);
      userFacade.update(user);
    }
  }

  private void setUserOnlineStatus(Users user, int status) {
    if (user != null) {
      user.setIsonline(status);
      userFacade.update(user);
    }
  }

  private boolean isUserInRole(Users user, String groupName) {
    if (user == null || groupName == null) {
      return false;
    }
    BbcGroup group = bbcGroupFacade.findByGroupName(groupName);
    if (group == null) {
      return false;
    }
    return user.getBbcGroupCollection().contains(group);
  }

  /**
   * Generates a salt value with SALT_LENGTH and DIGEST
   *
   * @return
   */
  public String generateSalt() {
    byte[] bytes = new byte[SALT_LENGTH];
    RANDOM.nextBytes(bytes);
    byte[] encodedSalt = Base64.getEncoder().encode(bytes);
    String salt = "";
    try {
      salt = new String(encodedSalt, "UTF-8");
    } catch (UnsupportedEncodingException ex) {
      LOGGER.log(Level.SEVERE, "Generate salt encoding failed", ex);
    }
    return salt;
  }
}
