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
import io.hops.hopsworks.persistence.entity.certificates.UserCerts;
import io.hops.hopsworks.persistence.entity.project.Project;
import io.hops.hopsworks.common.dao.project.ProjectFacade;
import io.hops.hopsworks.persistence.entity.user.BbcGroup;
import io.hops.hopsworks.common.dao.user.BbcGroupFacade;
import io.hops.hopsworks.common.dao.user.UserFacade;
import io.hops.hopsworks.persistence.entity.user.Users;
import io.hops.hopsworks.common.dao.user.security.audit.AccountAuditFacade;
import io.hops.hopsworks.persistence.entity.user.security.ua.UserAccountStatus;
import io.hops.hopsworks.persistence.entity.user.security.ua.UserAccountType;
import io.hops.hopsworks.common.dao.user.security.ua.UserAccountsEmailMessages;
import io.hops.hopsworks.common.security.utils.Secret;
import io.hops.hopsworks.common.security.utils.SecurityUtils;
import io.hops.hopsworks.common.util.HttpUtil;
import io.hops.hopsworks.persistence.entity.user.security.ua.ValidationKeyType;
import io.hops.hopsworks.restutils.RESTCodes;
import io.hops.hopsworks.exceptions.UserException;
import io.hops.hopsworks.common.security.CertificatesMgmService;
import io.hops.hopsworks.common.util.EmailBean;
import io.hops.hopsworks.common.util.HopsUtils;
import io.hops.hopsworks.common.util.Settings;

import javax.ejb.EJB;
import javax.ejb.EJBException;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.inject.Inject;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.servlet.http.HttpServletRequest;
import java.sql.Timestamp;
import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

@Stateless
@TransactionAttribute(TransactionAttributeType.NEVER)
public class AuthController {

  private final static Logger LOGGER = Logger.getLogger(AuthController.class.getName());

  @EJB
  private UserFacade userFacade;
  @EJB
  private BbcGroupFacade bbcGroupFacade;
  @EJB
  private UserStatusValidator userStatusValidator;
  @EJB
  private Settings settings;
  @EJB
  private EmailBean emailBean;
  @EJB
  private CertsFacade userCertsFacade;
  @EJB
  private ProjectFacade projectFacade;
  @EJB
  private CertificatesMgmService certificatesMgmService;
  @EJB
  private SecurityUtils securityUtils;
  @EJB
  private AccountAuditFacade accountAuditFacade;
  @Inject
  private PasswordRecovery passwordRecovery;

  private void validateUser(Users user) {
    if (user == null) {
      throw new IllegalArgumentException("User not set.");
    }
    if (!user.getMode().equals(UserAccountType.M_ACCOUNT_TYPE)) {
      throw new IllegalArgumentException("Can not login user with account type: " + user.getMode().toString());
    }
  }

  /**
   * Pre check for custom realm login.
   *
   * @param user
   * @param password
   * @param otp
   * @return
   * @throws UserException
   */
  public String preCustomRealmLoginCheck(Users user, String password, String otp) throws UserException {
    validateUser(user);
    if (isTwoFactorEnabled(user)) {
      if ((otp == null || otp.isEmpty()) && user.getMode().equals(UserAccountType.M_ACCOUNT_TYPE)) {
        if (checkPasswordAndStatus(user, password)) {
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

  /**
   * Validates password and update account audit.
   *
   * @param user
   * @param password
   * @return
   */
  public boolean validatePassword(Users user, String password) {
    validateUser(user);
    String userPwdHash = user.getPassword();
    Secret secret = new Secret(password, user.getSalt());
    if (!userPwdHash.equals(secret.getSha256HexDigest())) {
      registerFalseLogin(user);
      LOGGER.log(Level.FINEST, "False login attempt by user: {0}", user.getEmail());
      return false;
    }
    resetFalseLogin(user);
    return true;
  }

  /**
   * Checks password and user status. Also updates false login attempts.
   * throws UserException with rest code Unauthorized
   * @param user
   * @param password
   * @return
   * @throws UserException
   */
  public boolean checkPasswordAndStatus(Users user, String password) throws UserException {
    if (user == null) {
      throw new IllegalArgumentException("User not set.");
    }
    userStatusValidator.checkStatus(user.getStatus());
    return validatePassword(user, password);
  }
  
  /**
   * Checks password and user status. Also updates false login attempts.
   * throws UserException with rest code Bad Request
   * @param user
   * @param password
   * @return
   * @throws UserException
   */
  public boolean checkUserPasswordAndStatus(Users user, String password) throws UserException {
    checkUserStatus(user, false);
    return validatePassword(user, password);
  }
  
  private Users getUserFromKey(String key) {
    if (key == null) {
      throw new IllegalArgumentException("Validation key not supplied.");
    }
    if (key.length() <= Settings.USERNAME_LENGTH) {
      throw new IllegalArgumentException("Unrecognized validation key.");
    }
    String userName = key.substring(0, Settings.USERNAME_LENGTH);
    return userFacade.findByUsername(userName);
  }
  
  private void validate(Users user, String key) throws UserException {
    // get the 8 char username
    String secret = key.substring(Settings.USERNAME_LENGTH);
    if (!secret.equals(user.getValidationKey())) {
      registerFalseKeyValidation(user);
      throw new UserException(RESTCodes.UserErrorCode.INCORRECT_VALIDATION_KEY, Level.FINE);
    }
    if (diffMillis(user.getValidationKeyUpdated()) <  TimeUnit.SECONDS.toMillis(5)) {
      resetValidationKey(user);
      throw new UserException(RESTCodes.UserErrorCode.INCORRECT_VALIDATION_KEY, Level.FINE);
    }
    resetFalseLogin(user);
  }

  /**
   * Validates email validation key. Also updates false key validation attempts.
   *
   * @param key
   * @throws UserException
   */
  public void validateEmail(String key) throws UserException {
    Users user = getUserFromKey(key);
    checkUserStatusAndKey(user, ValidationKeyType.EMAIL, true);
    validate(user, key);
    user.setStatus(UserAccountStatus.VERIFIED_ACCOUNT);
    user.setActivated(new Timestamp(new Date().getTime()));
    resetValidationKey(user); //reset and update
  }
  
  /**
   * Check if the key exists and is valid. Will fail if the key is already set to reset.
   * Only password keys can be checked.
   * @param key
   * @throws UserException
   */
  public void checkRecoveryKey(String key) throws UserException {
    Users user = getUserFromKey(key);
    checkUserStatusAndKey(user, ValidationKeyType.PASSWORD, false); // only password keys can be checked
    validate(user, key);
    user.setValidationKeyType(ValidationKeyType.PASSWORD_RESET);
    userFacade.update(user);
  }
  
  /**
   * Check if the key exists and is valid before removing it. Will fail if the key is not of the given type
   * @param key
   * @return
   * @throws UserException
   */
  public Users validateRecoveryKey(String key, ValidationKeyType type)
    throws UserException {
    Users user = getUserFromKey(key);
    checkUserStatusAndKey(user, type, false);
    validate(user, key);
    return user;
  }
  
  public void resetValidationKey(Users user) {
    user.setValidationKey(null);
    user.setValidationKeyUpdated(null);
    user.setValidationKeyType(null);
    userFacade.update(user);
  }
  
  public void setValidationKey(Users user, String resetToken, ValidationKeyType type) {
    user.setValidationKey(resetToken);
    user.setValidationKeyUpdated(new Timestamp(new Date().getTime()));
    user.setValidationKeyType(type);
    userFacade.update(user);
  }
  
  private void checkUserStatusAndKey(Users user, ValidationKeyType type, boolean newUser) throws UserException {
    checkUserStatus(user, newUser);
    if (user.getValidationKeyType() == null || !type.equals(user.getValidationKeyType())) {
      throw new UserException(RESTCodes.UserErrorCode.INCORRECT_VALIDATION_KEY, Level.FINE);
    }
  }
  
  public void checkUserStatus(Users user, boolean newUser) throws UserException {
    if (user == null) {
      throw new UserException(RESTCodes.UserErrorCode.USER_WAS_NOT_FOUND, Level.FINE);
    }
    if (newUser) {
      userStatusValidator.checkNewUserStatus(user.getStatus());
    } else {
      try {
        userStatusValidator.checkStatus(user.getStatus());
      } catch (UserException e) {
        //Needed to not map account exceptions to Unauthorized rest response.
        throw new UserException(RESTCodes.UserErrorCode.ACCOUNT_NOT_ACTIVE, Level.FINE, e.getErrorCode().getMessage());
      }
    }
  }
  
  /**
   *
   * @param user
   * @param url
   * @param isPassword
   * @throws MessagingException
   */
  public void sendNewRecoveryValidationKey(Users user, String url, boolean isPassword)
    throws MessagingException, UserException {
    CredentialsResetToken resetToken = generateResetToken(user, isPassword);
    passwordRecovery.sendRecoveryNotification(user, url, isPassword, resetToken);
  }

  private static final long RESET_LINK_IN_HOURS = TimeUnit.HOURS.toMillis(SecurityUtils.RESET_LINK_VALID_FOR_HOUR);
  public CredentialsResetToken generateResetToken(Users user, boolean isPassword) throws UserException {
    if (user == null) {
      throw new IllegalArgumentException("User not set.");
    }
    if (UserAccountType.REMOTE_ACCOUNT_TYPE.equals(user.getMode())) {
      throw new UserException(RESTCodes.UserErrorCode.USER_WAS_NOT_FOUND, Level.FINE);
    }
    //resend the same token exp date > 5min
    if (user.getValidationKey() != null && user.getValidationKeyType() != null && user.getValidationKeyUpdated() != null
            && user.getValidationKeyType().equals(isPassword ? ValidationKeyType.PASSWORD : ValidationKeyType.QR_RESET)
            && diffMillis(user.getValidationKeyUpdated()) > TimeUnit.MINUTES.toMillis(5)) {
      return CredentialsResetToken.of(user.getValidationKey(), diffMillis(user.getValidationKeyUpdated()));
    }
    String resetToken = securityUtils.generateSecureRandomString();
    setValidationKey(user, resetToken, isPassword ? ValidationKeyType.PASSWORD : ValidationKeyType.QR_RESET);
    return CredentialsResetToken.of(resetToken, RESET_LINK_IN_HOURS);
  }

  public long diffMillis(Date date) {
    if (date == null) {
      return -1;
    }
    Date now = new Date();
    long validForMs = TimeUnit.HOURS.toMillis(SecurityUtils.RESET_LINK_VALID_FOR_HOUR);
    long diff = now.getTime() - date.getTime();
    long diffMs = validForMs - diff;
    return diffMs;
  }

  /**
   * Sends new recovery key email.
   *
   * @param user
   * @param req
   * @throws MessagingException
   */
  public void sendNewValidationKey(Users user, String linkUrl) throws MessagingException {
    if (user == null) {
      throw new IllegalArgumentException("User not set.");
    }
    String activationKey = securityUtils.generateSecureRandomString();
    sendEmailValidationKey(user, activationKey, linkUrl);
    setValidationKey(user, activationKey, ValidationKeyType.EMAIL);
  }
  
  /**
   *
   * @param user
   * @param activationKey
   * @param linkUrl of the validation key
   * @throws MessagingException
   */
  public void sendEmailValidationKey(Users user, String activationKey, String linkUrl)
    throws MessagingException {
    long validForHour = diffMillis(user.getValidationKeyUpdated());
    String subject = UserAccountsEmailMessages.ACCOUNT_REQUEST_SUBJECT;
    String msg = UserAccountsEmailMessages.buildMobileRequestMessageRest(linkUrl, user.getUsername()
        + securityUtils.urlEncode(activationKey), validForHour);
    emailBean.sendEmail(user.getEmail(), Message.RecipientType.TO, subject, msg);
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
   * Change user password. Will generate a new salt
   *
   * @param user
   * @param secret
   */
  @TransactionAttribute(TransactionAttributeType.REQUIRED)
  public void changePassword(Users user, Secret secret) {
    String oldPassword = user.getPassword();
    user.setPassword(secret.getSha256HexDigest());
    user.setSalt(secret.getSalt());
    user.setPasswordChanged(new Timestamp(new Date().getTime()));
    userFacade.update(user);
    resetProjectCertPassword(user, oldPassword);
  }
  
  @TransactionAttribute(TransactionAttributeType.REQUIRED)
  public void changeUserPasswordAsAdmin(Users user, Secret secret) {
    changePassword(user, secret);
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
    try {
      for (Project project : projects) {
        UserCerts userCert = userCertsFacade.findUserCert(project.getName(), p.getUsername());
        String masterEncryptionPassword = certificatesMgmService.getMasterEncryptionPassword();
        String certPassword = HopsUtils.decrypt(oldPass, userCert.getUserKeyPwd(), masterEncryptionPassword);
        //Encrypt it with new password and store it in the db
        String newSecret = HopsUtils.encrypt(p.getPassword(), certPassword, masterEncryptionPassword);
        userCert.setUserKeyPwd(newSecret);
        userCertsFacade.update(userCert);
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
   */
  public void registerFalseLogin(Users user) {
    if (user != null) {
      int count = user.getFalseLogin() + 1;
      user.setFalseLogin(count);

      int allowedFalseLogins = isUserAgent(user)? Settings.ALLOWED_AGENT_FALSE_LOGINS : Settings.ALLOWED_FALSE_LOGINS;
      // block the user account if more than allowed false logins
      if (count > allowedFalseLogins) {
        user.setStatus(UserAccountStatus.BLOCKED_ACCOUNT);
        try {
          emailBean.sendEmail(user.getEmail(), Message.RecipientType.TO,
              UserAccountsEmailMessages.ACCOUNT_BLOCKED__SUBJECT, UserAccountsEmailMessages.accountBlockedMessage());
        } catch (MessagingException ex) {
          LOGGER.log(Level.SEVERE, "Failed to send email. ", ex);
        }
      }
      // notify user about the false attempts
      userFacade.update(user);
    }
  }
  
  private boolean isUserAgent(Users user) {
    return isUserInRole(user,"AGENT");
  }

  /**
   * Registers failed email validation
   *
   * @param user
   */
  public void registerFalseKeyValidation(Users user) {
    if (user != null) {
      int count = user.getFalseLogin() + 1;
      user.setFalseLogin(count);

      // make the user spam account if more than allowed tries
      if (count > Settings.ACCOUNT_VALIDATION_TRIES) {
        user.setStatus(UserAccountStatus.SPAM_ACCOUNT);
      }
      userFacade.update(user);
    }
  }

  /**
   * Set user online, resets false login attempts and register login audit info
   *
   * @param user
   */
  public void registerLogin(Users user) {
    resetFalseLogin(user);
    setUserOnlineStatus(user, Settings.IS_ONLINE);
    LOGGER.log(Level.FINEST, "Logged in user: {0}. ", user.getEmail());
  }
  
  public void registerLogin(Users user, HttpServletRequest req) {
    String remoteHost = HttpUtil.extractRemoteHostIp(req);
    String userAgent = HttpUtil.extractUserAgent(req);
    registerLogin(user);
    accountAuditFacade.registerLoginInfo(user, "LOGIN", "SUCCESS", remoteHost, userAgent);
  }

  /**
   * Set user offline and register login audit info
   *
   * @param user
   */
  public void registerLogout(Users user) {
    setUserOnlineStatus(user, Settings.IS_OFFLINE);
    LOGGER.log(Level.FINEST, "Logged out user: {0}. ", user.getEmail());
  }

  /**
   * Register authentication failure and register login audit info
   *
   * @param user
   */
  public void registerAuthenticationFailure(Users user) {
    registerFalseLogin(user);
    LOGGER.log(Level.FINEST, "Authentication failure user: {0}. ", user.getEmail());
  }

  public void resetFalseLogin(Users user) {
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

  public static class CredentialsResetToken {
    private final String token;
    private final long validity;

    public static CredentialsResetToken of(String token, long validity) {
      return new CredentialsResetToken(token, validity);
    }

    private CredentialsResetToken(String token, long validity) {
      this.token = token;
      this.validity = validity;
    }

    public String getToken() {
      return token;
    }

    public long getValidity() {
      return validity;
    }
  }
}