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

import com.google.common.base.Strings;
import com.google.zxing.WriterException;
import io.hops.hopsworks.common.dao.project.ProjectFacade;
import io.hops.hopsworks.common.dao.project.team.ProjectTeamFacade;
import io.hops.hopsworks.persistence.entity.project.Project;
import io.hops.hopsworks.persistence.entity.user.BbcGroup;
import io.hops.hopsworks.common.dao.user.BbcGroupFacade;
import io.hops.hopsworks.common.dao.user.UserDTO;
import io.hops.hopsworks.common.dao.user.UserFacade;
import io.hops.hopsworks.persistence.entity.user.Users;
import io.hops.hopsworks.persistence.entity.user.security.audit.AccountAudit;
import io.hops.hopsworks.common.dao.user.security.audit.AccountAuditFacade;
import io.hops.hopsworks.persistence.entity.user.security.audit.RolesAudit;
import io.hops.hopsworks.common.dao.user.security.audit.RolesAuditFacade;
import io.hops.hopsworks.persistence.entity.user.security.ua.UserAccountStatus;
import io.hops.hopsworks.persistence.entity.user.security.ua.UserAccountType;
import io.hops.hopsworks.common.dao.user.security.ua.UserAccountsEmailMessages;
import io.hops.hopsworks.common.security.utils.Secret;
import io.hops.hopsworks.common.security.utils.SecurityUtils;
import io.hops.hopsworks.exceptions.ServiceException;
import io.hops.hopsworks.persistence.entity.user.security.ua.ValidationKeyType;
import io.hops.hopsworks.restutils.RESTCodes;
import io.hops.hopsworks.exceptions.UserException;
import io.hops.hopsworks.common.util.EmailBean;
import io.hops.hopsworks.common.util.QRCodeGenerator;
import io.hops.hopsworks.common.util.Settings;
import org.apache.commons.lang3.StringUtils;

import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.enterprise.inject.Any;
import javax.enterprise.inject.Instance;
import javax.inject.Inject;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.validation.ConstraintViolationException;
import java.io.IOException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;

@Stateless
//the operations in this method does not need any transaction
//the actual persisting will in the facades transaction.
@TransactionAttribute(TransactionAttributeType.NEVER)
public class UsersController {

  private static final Logger LOGGER = Logger.getLogger(UsersController.class.getName());
  @EJB
  private UserFacade userFacade;
  @EJB
  private AccountAuditFacade accountAuditFacade;
  @EJB
  private RolesAuditFacade rolesAuditFacade;
  @EJB
  private BbcGroupFacade bbcGroupFacade;
  @EJB
  private UserValidator userValidator;
  @EJB
  private EmailBean emailBean;
  @EJB
  private Settings settings;
  @EJB
  private AuthController authController;
  @EJB
  private SecurityUtils securityUtils;
  @EJB
  private UserStatusValidator userStatusValidator;
  @EJB
  private ProjectFacade projectFacade;
  @EJB
  private ProjectTeamFacade projectTeamFacade;
  @Inject
  @Any
  private Instance<UserAccountHandler> userAccountHandlers;

  public QrCode registerUser(UserDTO newUser, String validationKeyUrl) throws UserException {
    if (newUser.getEmail() != null && !newUser.getEmail().isEmpty()) {
      newUser.setEmail(newUser.getEmail().toLowerCase());
    }
    userValidator.isValidNewUser(newUser);
    // if setting is changed user should still be able to use her device
    if (!Strings.isNullOrEmpty(settings.getTwoFactorAuth()) &&
      Settings.TwoFactorMode.MANDATORY.getName().equals(settings.getTwoFactorAuth())) {
      newUser.setTwoFactor(true);
    }
    newUser.setMaxNumProjects(0);//should not allow setting max project
    Users user = createNewUser(newUser, UserAccountStatus.NEW_MOBILE_ACCOUNT, UserAccountType.M_ACCOUNT_TYPE);
    //to prevent sending email for test user emails
    QrCode qrCode;
    try {
      if (!newUser.isTestUser()) {
        // Notify user about the request if not test user.
        authController.sendEmailValidationKey(user, user.getValidationKey(), validationKeyUrl);
      }
      // Only register the user if i can send the email. To prevent fake emails
      userFacade.persist(user);
      
      // trigger user account handlers
      UserAccountHandler.runUserAccountCreateHandlers(userAccountHandlers, user);
      
      qrCode = getQRCode(user);
    } catch (MessagingException ex) {
      throw new UserException(RESTCodes.UserErrorCode.ACCOUNT_REGISTRATION_ERROR, Level.SEVERE,
        "user: " + newUser.getUsername(), ex.getMessage(), ex);
    }
    return qrCode;
  }
  
  public Users registerUser(UserDTO newUser, String role, UserAccountStatus accountStatus, UserAccountType accountType)
    throws UserException {
    if (!Strings.isNullOrEmpty(newUser.getEmail())) {
      newUser.setEmail(newUser.getEmail().toLowerCase());
    }
    userValidator.isValidEmail(newUser.getEmail());
    if (userFacade.findByEmail(newUser.getEmail()) != null) {
      throw new UserException(RESTCodes.UserErrorCode.USER_EXISTS, Level.FINE);
    }
    Users user = createNewUser(newUser, accountStatus, accountType);
    BbcGroup group = bbcGroupFacade.findByGroupName(role);
    if (group != null) {
      user.getBbcGroupCollection().add(group);
    }
    userFacade.persist(user);
  
    // trigger user account handlers
    UserAccountHandler.runUserAccountCreateHandlers(userAccountHandlers, user);
    
    return user;
  }
  
  public void addRole(String role, Integer id) throws UserException {
    Users p = userFacade.find(id);
    BbcGroup bbcGroup = bbcGroupFacade.findByGroupName(role);
    if (bbcGroup != null) {
      registerGroup(p, bbcGroup.getGid());
    } else {
      throw new UserException(RESTCodes.UserErrorCode.ROLE_NOT_FOUND, Level.FINE, "Role could not be granted.");
    }
  
    // trigger user account handlers
    UserAccountHandler.runUserAccountUpdateHandlers(userAccountHandlers, p);
  }
  
  public void removeRole(String role, Integer id) throws UserException {
    Users p = userFacade.find(id);
    BbcGroup bbcGroup = bbcGroupFacade.findByGroupName(role);
    if (bbcGroup != null && p.getBbcGroupCollection().contains(bbcGroup)) {
      userFacade.removeGroup(p.getEmail(), bbcGroup.getGid());// remove from table only
      p.getBbcGroupCollection().remove(bbcGroup);// remove from the user entity
    } else if (bbcGroup != null) {
      throw new UserException(RESTCodes.UserErrorCode.ROLE_NOT_FOUND, Level.FINE, "Role could not be granted.");
    }
  
    // trigger user account handlers
    UserAccountHandler.runUserAccountUpdateHandlers(userAccountHandlers, p);
  }
  
  public void sendConfirmationMail(Users user) throws ServiceException {
    try {
      //send confirmation email
      emailBean.sendEmail(user.getEmail(), Message.RecipientType.TO,
        UserAccountsEmailMessages.ACCOUNT_CONFIRMATION_SUBJECT,
        UserAccountsEmailMessages.accountActivatedMessage(user.getEmail()));
    } catch (MessagingException e) {
      throw new ServiceException(RESTCodes.ServiceErrorCode.EMAIL_SENDING_FAILURE, Level.SEVERE, null, e.getMessage(),
        e);
    }
  }
  
  public void sendRejectionEmail(Users user) throws ServiceException {
    try {
      emailBean.sendEmail(user.getEmail(), Message.RecipientType.TO, UserAccountsEmailMessages.ACCOUNT_REJECT,
        UserAccountsEmailMessages.accountRejectedMessage());
    } catch (MessagingException e) {
      throw new ServiceException(RESTCodes.ServiceErrorCode.EMAIL_SENDING_FAILURE, Level.SEVERE, null, e.getMessage(),
        e);
    }
  }
  
  public Users resendAccountVerificationEmail(Users user, String linkUrl) throws ServiceException {
    try {
      authController.sendNewValidationKey(user, linkUrl);
      return user;
    } catch (MessagingException e) {
      throw new ServiceException(RESTCodes.ServiceErrorCode.EMAIL_SENDING_FAILURE, Level.SEVERE, null, e.getMessage(),
        e);
    }
  }
  
  /**
   * Create a new user
   *
   * @param newUser
   * @param accountStatus
   * @param accountType
   * @return
   */
  public Users createNewUser(UserDTO newUser, UserAccountStatus accountStatus, UserAccountType accountType) {
    String otpSecret = securityUtils.calculateSecretKey();
    String activationKey = securityUtils.generateSecureRandomString();
    String uname = generateUsername(newUser.getEmail());
    List<BbcGroup> groups = new ArrayList<>();
    Secret secret = securityUtils.generateSecret(newUser.getChosenPassword());
    Timestamp now = new Timestamp(new Date().getTime());
    int maxNumProjects = newUser.getMaxNumProjects() > 0? newUser.getMaxNumProjects() : settings.getMaxNumProjPerUser();

    Users user = new Users(uname, secret.getSha256HexDigest(), newUser.getEmail(), newUser.getFirstName(),
        newUser.getLastName(), now, "-", accountStatus, otpSecret, activationKey,
        now, ValidationKeyType.EMAIL, accountType, now, maxNumProjects, newUser.isTwoFactor(),
        secret.getSalt(), newUser.getToursState());
    user.setBbcGroupCollection(groups);
    return user;
  }

  /**
   * Remote user
   * @param email
   * @param fname
   * @param lname
   * @param pwd
   * @param accStatus
   * @return
   */
  public Users createNewRemoteUser(String email, String fname, String lname, String pwd, UserAccountStatus accStatus) {
    String uname = generateUsername(email);
    List<BbcGroup> groups = new ArrayList<>();
    Secret secret = securityUtils.generateSecret(pwd);
    Users user = new Users(uname, secret.getSha256HexDigest(), email, fname, lname, new Timestamp(new Date().getTime()),
      "-", accStatus, UserAccountType.REMOTE_ACCOUNT_TYPE, new Timestamp(new Date().getTime()),
      settings.getMaxNumProjPerUser(), secret.getSalt());
    user.setBbcGroupCollection(groups);
    return user;
  }

  public void sendQRRecoveryEmail(String email, String password, String reqUrl)
    throws UserException, MessagingException {
    Users user = userFacade.findByEmail(email);
    if (!authController.checkUserPasswordAndStatus(user, password)) {
      throw new UserException(RESTCodes.UserErrorCode.INCORRECT_CREDENTIALS, Level.FINE);
    }
    if (!authController.isTwoFactorEnabled(user)) {
      throw new UserException(RESTCodes.UserErrorCode.TWO_FA_DISABLED, Level.FINE);
    }
    authController.sendNewRecoveryValidationKey(user, reqUrl, false);
  }
  
  public void sendPasswordRecoveryEmail(String email, String reqUrl) throws UserException, MessagingException {
    Users user = userFacade.findByEmail(email);
    if (user == null) {
      throw new UserException(RESTCodes.UserErrorCode.USER_WAS_NOT_FOUND, Level.FINE);
    }

    try {
      userStatusValidator.checkStatus(user.getStatus());
    } catch (UserException e) {
      //Needed to not map account exceptions to Unauthorized rest response.
      throw new UserException(RESTCodes.UserErrorCode.ACCOUNT_NOT_ACTIVE, Level.FINE, e.getErrorCode().getMessage());
    }
    authController.sendNewRecoveryValidationKey(user, reqUrl, true);
  }
  
  public byte[] recoverQRCodeByte(String key) throws UserException, MessagingException {
    Users user = sendQRCodeRecoverEmail(key);
    return resetQRCodeByte(user);
  }
  
  public QrCode recoverQRCode(String key) throws UserException, MessagingException {
    Users user = sendQRCodeRecoverEmail(key);
    return resetQRCode(user);
  }
  
  private Users sendQRCodeRecoverEmail(String key) throws UserException, MessagingException {
    Users user = authController.validateRecoveryKey(key, ValidationKeyType.QR_RESET);
    if (!authController.isTwoFactorEnabled(user)) {
      throw new UserException(RESTCodes.UserErrorCode.TWO_FA_DISABLED, Level.FINE);
    }
    authController.resetValidationKey(user);
    //Send verification
    String subject = UserAccountsEmailMessages.ACCOUNT_QR_RESET;
    String msg = UserAccountsEmailMessages.buildQRResetMessage();
    emailBean.sendEmail(user.getEmail(), Message.RecipientType.TO, subject, msg);
    return user;
  }

  public QrCode resetQRCode(Users user, String password) throws UserException {
    if (user == null) {
      throw new UserException(RESTCodes.UserErrorCode.USER_DOES_NOT_EXIST, Level.FINE);
    }
    if (!authController.validatePassword(user, password)) {
      throw new UserException(RESTCodes.UserErrorCode.PASSWORD_INCORRECT, Level.FINE);
    }
    if (!authController.isTwoFactorEnabled(user)) {
      throw new UserException(RESTCodes.UserErrorCode.TWO_FA_DISABLED, Level.FINE);
    }
    return resetQRCode(user);
  }
  
  private QrCode resetQRCode(Users user) throws UserException {
    updateUserSecret(user);
    return getQRCode(user);
  }
  
  private byte[] resetQRCodeByte(Users user) throws UserException {
    updateUserSecret(user);
    try {
      return QRCodeGenerator.getQRCodeBytes(user.getEmail(), Settings.ISSUER, user.getSecret());
    } catch (IOException | WriterException e) {
      throw new UserException(RESTCodes.UserErrorCode.FAILED_TO_GENERATE_QR_CODE, Level.FINE);
    }
  }
  
  private void updateUserSecret(Users user) {
    String random = securityUtils.calculateSecretKey();
    updateSecret(user, random);
  }
  
  /**
   * Use when a user change her/his password
   * @param user
   * @param oldPassword
   * @param newPassword
   * @param confirmedPassword
   * @throws UserException
   */
  public void changePassword(Users user, String oldPassword, String newPassword, String confirmedPassword)
    throws UserException {
    if (!authController.validatePassword(user, oldPassword)) {
      throw new UserException(RESTCodes.UserErrorCode.PASSWORD_INCORRECT, Level.FINE);
    }
    if (UserAccountStatus.TEMP_PASSWORD.equals(user.getStatus())){
      user.setStatus(UserAccountStatus.ACTIVATED_ACCOUNT);
    }
    changePassword(user, newPassword, confirmedPassword);
  }
  
  public void checkRecoveryKey(String key) throws UserException {
    authController.checkRecoveryKey(key);
  }
  
  public void validateKey(String key) throws UserException {
    authController.validateEmail(key);
  }
  
  /**
   * Use to reset password to a temporary random password
   * @param id
   * @return
   * @throws UserException
   * @throws MessagingException
   */
  public String resetPassword(Integer id, String initiator) throws UserException, MessagingException {
    Users user = userFacade.find(id);
    if (!user.getMode().equals(UserAccountType.M_ACCOUNT_TYPE)) {
      throw new UserException(RESTCodes.UserErrorCode.OPERATION_NOT_ALLOWED, Level.FINE, "Can not reset password of a" +
        " remote user");
    }
    String randomPwd = securityUtils.generateRandomString(UserValidator.TEMP_PASSWORD_LENGTH);
    user.setStatus(UserAccountStatus.TEMP_PASSWORD);
    changePasswordAsAdmin(user, randomPwd);
    String subject = UserAccountsEmailMessages.ACCOUNT_PASSWORD_RESET;
    String msg = UserAccountsEmailMessages.buildResetByAdminMessage(initiator);
    emailBean.sendEmail(user.getEmail(), Message.RecipientType.TO, subject, msg);
    return randomPwd;
  }
  
  /**
   * Use to reset password with a recovery key
   * @param key
   * @param newPassword
   * @param confirmedPassword
   * @throws UserException
   * @throws MessagingException
   */
  public void changePassword(String key, String newPassword, String confirmedPassword)
    throws UserException, MessagingException {
    userValidator.isValidPassword(newPassword, confirmedPassword);
    Users user = authController.validateRecoveryKey(key, ValidationKeyType.PASSWORD_RESET);
    changePassword(user, newPassword, confirmedPassword);
    authController.resetValidationKey(user);
    //Send verification
    String subject = UserAccountsEmailMessages.ACCOUNT_PASSWORD_RESET;
    String msg = UserAccountsEmailMessages.buildResetMessage();
    emailBean.sendEmail(user.getEmail(), Message.RecipientType.TO, subject, msg);
  }
  
  private void changePassword(Users user, String newPassword, String confirmedPassword) throws UserException {
    if (userValidator.isValidPassword(newPassword, confirmedPassword)) {
      try {
        Secret secret = securityUtils.generateSecret(newPassword);
        authController.changePassword(user, secret);
      } catch (Exception ex) {
        throw new UserException(RESTCodes.UserErrorCode.PASSWORD_RESET_UNSUCCESSFUL, Level.SEVERE, null,
          ex.getMessage(), ex);
      }
    }
  }
  
  private void changePasswordAsAdmin(Users user, String newPassword) throws UserException {
    try {
      Secret secret = securityUtils.generateSecret(newPassword);
      authController.changeUserPasswordAsAdmin(user, secret);
    } catch (Exception ex) {
      throw new UserException(RESTCodes.UserErrorCode.PASSWORD_RESET_UNSUCCESSFUL, Level.SEVERE, null,
        ex.getMessage(), ex);
    }
  }

  public Users updateProfile(Users user, String firstName, String lastName, Integer toursState)
    throws UserException {

    if (user == null) {
      throw new UserException(RESTCodes.UserErrorCode.USER_WAS_NOT_FOUND, Level.FINE);
    }

    if (firstName != null) {
      user.setFname(firstName);
    }
    if (lastName != null) {
      user.setLname(lastName);
    }
    if (toursState != null) {
      user.setToursState(toursState);
    }
    userFacade.update(user);
    return user;
  }

  public String generateUsername(String email) {
    if (email == null) {
      throw new IllegalArgumentException("Email is null");
    }

    String emailUsername = email.substring(0, email.lastIndexOf("@")).toLowerCase();

    // Remove all special chars from the string
    String baseUsername = emailUsername.replaceAll("[^a-z0-9]", "");

    if (baseUsername.length() <= Settings.USERNAME_LEN) {
      baseUsername = baseUsername + StringUtils.repeat( "0", Settings.USERNAME_LEN - baseUsername.length());
    } else {
      baseUsername = baseUsername.substring(0, Settings.USERNAME_LEN);
    }

    Users user = userFacade.findByUsername(baseUsername);
    if (user == null) {
      return baseUsername;
    }

    String testUname = "";
    String suffix = "";
    int count = 1;
    while (user != null && count < 1000) {
      suffix = String.valueOf(count);
      testUname = baseUsername.substring(0, (Settings.USERNAME_LEN - suffix.length()));
      user = userFacade.findByUsername(testUname + suffix);
      count++;
    }

    if (count == 1000) {
      throw new IllegalStateException("You cannot register with this email address. Pick another.");
    }
    return testUname + suffix;
  }

  public Optional<Users> findByUsername(String username) {
    Users user = userFacade.findByUsername(username);
    if(user != null) {
      return Optional.of(user);
    }
    return Optional.empty();
  }


  /**
   * Enables or disables two factor authentication.
   * The operation depends on the current status of the users two factor.
   * i.e enables if it was disabled or vice versa.
   *
   * @param user
   * @param password
   * @return qrCode if tow factor is enabled null if disabled.
   */
  public QrCode changeTwoFactor(Users user, String password) throws UserException {
    if (user == null) {
      throw new IllegalArgumentException("User was not provided.");
    }
    if (!authController.validatePassword(user, password)) {
      throw new UserException(RESTCodes.UserErrorCode.PASSWORD_INCORRECT, Level.FINE);
    }
    if (user.getTwoFactor()) {
      user.setTwoFactor(false);
      userFacade.update(user);
    } else {
      user.setTwoFactor(true);
      userFacade.update(user);
      return getQRCode(user);
    }
    return null;
  }

  /**
   * Returns the QR code for the user if two factor is enabled.
   *
   * @param user
   * @param password
   * @return null if two factor is disabled.
   */
  public QrCode getQRCode(Users user, String password) throws UserException {
    if (user == null) {
      throw new IllegalArgumentException("User was not provided");
    }
    if (!authController.validatePassword(user, password)) {
      throw new UserException(RESTCodes.UserErrorCode.PASSWORD_INCORRECT, Level.FINE);
    }
    if (!authController.isTwoFactorEnabled(user)) {
      throw new UserException(RESTCodes.UserErrorCode.TWO_FA_DISABLED, Level.FINE);
    }
    return getQRCode(user);
  }
  
  private QrCode getQRCode(Users user) throws UserException {
    try {
      byte[] img = QRCodeGenerator.getQRCodeBytes(user.getEmail(), Settings.ISSUER, user.getSecret());
      return new QrCode(img, user.getSecret(), Settings.ISSUER, user.getEmail());
    } catch (IOException | WriterException e) {
      throw new UserException(RESTCodes.UserErrorCode.FAILED_TO_GENERATE_QR_CODE, Level.FINE);
    }
  }

  public void registerGroup(Users uid, int gidNumber) {
    BbcGroup bbcGroup = bbcGroupFacade.find(gidNumber);
    uid.getBbcGroupCollection().add(bbcGroup);
    userFacade.update(uid);
  }

  public void changeAccountStatus(int id, String note, UserAccountStatus status) throws UserException {
    Users p = userFacade.find(id);
    if (p != null) {
      if (UserAccountStatus.ACTIVATED_ACCOUNT.equals(status)) {
        p.setFalseLogin(0);
        if (p.getBbcGroupCollection() == null || p.getBbcGroupCollection().isEmpty()) {
          BbcGroup group = bbcGroupFacade.findByGroupName("HOPS_USER");
          List<BbcGroup> groups = new ArrayList<>();
          groups.add(group);
          p.setBbcGroupCollection(groups);
        }
      }
      p.setNotes(note);
      p.setStatus(status);
      userFacade.update(p);
  
      // trigger user account handlers
      UserAccountHandler.runUserAccountUpdateHandlers(userAccountHandlers, p);
      
      try {
        emailBean.sendEmail(p.getEmail(), Message.RecipientType.TO, UserAccountsEmailMessages.ACCOUNT_STATUS_CHANGED,
          UserAccountsEmailMessages.accountStatusChangeMessage(status.getUserStatus()));
      } catch (MessagingException e) {
      
      }
    } else {
      throw new UserException(RESTCodes.UserErrorCode.USER_WAS_NOT_FOUND, Level.FINE);
    }
  }

  public void updateSecret(Users user, String sec) {
    user.setSecret(sec);
    userFacade.update(user);
  }

  public void increaseNumCreatedProjects(int id) {
    Users u = userFacade.find(id);
    u.setNumCreatedProjects(u.getNumCreatedProjects() + 1);
    u.setNumActiveProjects(u.getNumActiveProjects() + 1);
    userFacade.update(u);
  }

  public void decrementNumProjectsCreated(int id) {
    Users u = userFacade.find(id);
    int n = u.getNumCreatedProjects();
    if (n > 0) {
      u.setNumCreatedProjects(n - 1);
      userFacade.update(u);
    }
  }

  public void decrementNumActiveProjects(int id) {
    Users u = userFacade.find(id);
    int n = u.getNumActiveProjects();
    if (n > 0) {
      u.setNumActiveProjects(n - 1);
      userFacade.update(u);
    }
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

  public void updateMaxNumProjs(Integer id, int maxNumProjs) {
    Users user = userFacade.find(id);
    user.setMaxNumProjects(maxNumProjs);
    userFacade.update(user);
  }
  
  /**
   * Delete users. Will fail if the user is an initiator of an audit log.
   * @param u
   * @throws UserException
   */
  public void deleteUser(Users u) throws UserException {
    if (u != null) {
      //Should not delete user that is an Initiator in a RolesAudit
      List<RolesAudit> results = rolesAuditFacade.findByTarget(u);
      
      for (Iterator<RolesAudit> iterator = results.iterator(); iterator.hasNext();) {
        RolesAudit next = iterator.next();
        rolesAuditFacade.remove(next);
      }
  
      //Should not delete user that is an Initiator in an AccountAudit
      List<AccountAudit> resultsAA = accountAuditFacade.findByTarget(u);

      for (Iterator<AccountAudit> iterator = resultsAA.iterator(); iterator.hasNext();) {
        AccountAudit next = iterator.next();
        accountAuditFacade.remove(next);
      }
  
      // run delete handlers
      UserAccountHandler.runUserAccountDeleteHandlers(userAccountHandlers, u);
      
      try {
        userFacade.removeByEmail(u.getEmail());
      } catch (ConstraintViolationException cve) {
        throw new UserException(RESTCodes.UserErrorCode.ACCOUNT_DELETION_ERROR, Level.FINE, "User that initiated " +
          "audit log on another account can not be deleted.", cve.getMessage());
      }
    }
  }
  
  /**
   * Delete user that is not an owner or a member of a project
   * @param id
   * @throws UserException
   */
  public void deleteUser(Integer id) throws UserException {
    Users user = getUserById(id);
    List<Project> projects = projectFacade.findByUser(user);
    if (projects != null && !projects.isEmpty()) {
      throw new UserException(RESTCodes.UserErrorCode.ACCOUNT_DELETION_ERROR, Level.FINE, "Can not delete a user that" +
        " owns a project. Delete the project first and try again.");
    }
    if (projectTeamFacade.countByMember(user) > 0) {
      throw new UserException(RESTCodes.UserErrorCode.ACCOUNT_DELETION_ERROR, Level.FINE, "Can not delete a user that" +
        " is a member of a project. Remove the user from all projects and try again.");
    }
    deleteUser(user);
  }
  
  public Users getUserById(Integer id) throws UserException {
    if (id == null) {
      throw new IllegalArgumentException("id can not be null");
    }
    Users user = userFacade.find(id);
    if (user == null) {
      throw new UserException(RESTCodes.UserErrorCode.USER_WAS_NOT_FOUND, Level.FINE);
    }
    return user;
  }
}
