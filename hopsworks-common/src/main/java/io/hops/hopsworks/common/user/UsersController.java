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

import com.google.zxing.WriterException;
import io.hops.hopsworks.common.dao.user.BbcGroup;
import io.hops.hopsworks.common.dao.user.BbcGroupFacade;
import io.hops.hopsworks.common.dao.user.UserDTO;
import io.hops.hopsworks.common.dao.user.UserFacade;
import io.hops.hopsworks.common.dao.user.Users;
import io.hops.hopsworks.common.dao.user.security.Address;
import io.hops.hopsworks.common.dao.user.security.Organization;
import io.hops.hopsworks.common.dao.user.security.audit.AccountAudit;
import io.hops.hopsworks.common.dao.user.security.audit.AccountAuditFacade;
import io.hops.hopsworks.common.dao.user.security.audit.AccountsAuditActions;
import io.hops.hopsworks.common.dao.user.security.audit.RolesAudit;
import io.hops.hopsworks.common.dao.user.security.audit.RolesAuditAction;
import io.hops.hopsworks.common.dao.user.security.audit.RolesAuditFacade;
import io.hops.hopsworks.common.dao.user.security.audit.UserAuditActions;
import io.hops.hopsworks.common.dao.user.security.ua.SecurityQuestion;
import io.hops.hopsworks.common.dao.user.security.ua.SecurityUtils;
import io.hops.hopsworks.common.dao.user.security.ua.UserAccountStatus;
import io.hops.hopsworks.common.dao.user.security.ua.UserAccountType;
import io.hops.hopsworks.common.dao.user.security.ua.UserAccountsEmailMessages;
import io.hops.hopsworks.common.dao.user.sshkey.SshKeyDTO;
import io.hops.hopsworks.common.dao.user.sshkey.SshKeys;
import io.hops.hopsworks.common.dao.user.sshkey.SshKeysPK;
import io.hops.hopsworks.common.dao.user.sshkey.SshkeysFacade;
import io.hops.hopsworks.common.exception.RESTCodes;
import io.hops.hopsworks.common.exception.ServiceException;
import io.hops.hopsworks.common.exception.UserException;
import io.hops.hopsworks.common.util.EmailBean;
import io.hops.hopsworks.common.util.FormatUtils;
import io.hops.hopsworks.common.util.QRCodeGenerator;
import io.hops.hopsworks.common.util.Settings;
import org.apache.commons.lang3.StringUtils;

import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.mail.Message.RecipientType;
import javax.mail.MessagingException;
import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
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
  private SshkeysFacade sshKeysBean;
  @EJB
  private UserValidator userValidator;
  @EJB
  private EmailBean emailBean;
  @EJB
  private Settings settings;
  @EJB
  private AuthController authController;
  @EJB
  private AccountAuditFacade auditManager;

  // To send the user the QR code image
  private byte[] qrCode;

  public byte[] registerUser(UserDTO newUser, HttpServletRequest req) throws
    NoSuchAlgorithmException, UserException {
    userValidator.isValidNewUser(newUser);
    Users user = createNewUser(newUser, UserAccountStatus.NEW_MOBILE_ACCOUNT, UserAccountType.M_ACCOUNT_TYPE);
    addAddress(user);
    addOrg(user);
    //to privent sending email for test user emails
    try {
      if (!newUser.isTestUser()) {
        // Notify user about the request if not test user.
        emailBean.sendEmail(newUser.getEmail(), RecipientType.TO, UserAccountsEmailMessages.ACCOUNT_REQUEST_SUBJECT,
            UserAccountsEmailMessages.buildMobileRequestMessage(FormatUtils.getUserURL(req), user.getUsername() + user.
                getValidationKey()));
      }
      // Only register the user if i can send the email
      userFacade.persist(user);
      qrCode = QRCodeGenerator.getQRCodeBytes(newUser.getEmail(), Settings.ISSUER, user.getSecret());
      accountAuditFacade.registerAccountChange(user, AccountsAuditActions.REGISTRATION.name(),
          AccountsAuditActions.SUCCESS.name(), "", user, req);
      accountAuditFacade.registerAccountChange(user, AccountsAuditActions.QRCODE.name(),
          AccountsAuditActions.SUCCESS.name(), "", user, req);
    } catch (WriterException | MessagingException | IOException ex) {

      accountAuditFacade.registerAccountChange(user, AccountsAuditActions.REGISTRATION.name(),
          AccountsAuditActions.FAILED.name(), "", user, req);
      accountAuditFacade.registerAccountChange(user, AccountsAuditActions.QRCODE.name(),
          AccountsAuditActions.FAILED.name(), "", user, req);

      throw new UserException(RESTCodes.UserErrorCode.ACCOUNT_REGISTRATION_ERROR, Level.SEVERE,
        "user: " + newUser.getUsername(), ex.getMessage(), ex);
    }
    return qrCode;
  }
  
  @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
  public String activateUser(String role, Users user1, Users loggedInUser, HttpServletRequest httpServletRequest) {
    BbcGroup bbcGroup = bbcGroupFacade.findByGroupName(role);
    if (bbcGroup != null) {
      registerGroup(user1, bbcGroup.getGid());
      auditManager.registerRoleChange(loggedInUser,
          RolesAuditAction.ROLE_ADDED.name(),
          RolesAuditAction.SUCCESS.name(), bbcGroup.getGroupName(),
          user1, httpServletRequest);
    } else {
      auditManager.registerAccountChange(loggedInUser,
          UserAccountStatus.ACTIVATED_ACCOUNT.name(),
          RolesAuditAction.FAILED.name(), "Role could not be granted.",
          user1, httpServletRequest);
      return "Role could not be granted.";
    }
    
    try {
      updateStatus(user1, UserAccountStatus.ACTIVATED_ACCOUNT);
      auditManager.registerAccountChange(loggedInUser,
          UserAccountStatus.ACTIVATED_ACCOUNT.name(),
          UserAuditActions.SUCCESS.name(), "", user1, httpServletRequest);
    } catch (IllegalArgumentException ex) {
      auditManager.registerAccountChange(loggedInUser,
          UserAccountStatus.ACTIVATED_ACCOUNT.name(),
          RolesAuditAction.FAILED.name(), "User could not be activated.",
          user1, httpServletRequest);
      return "User could not be activated.";
    }
    
    return null;
  }
  
  /**
   * Create a new user
   *
   * @param newUser
   * @param accountStatus
   * @param accountType
   * @return
   * @throws NoSuchAlgorithmException
   */
  public Users createNewUser(UserDTO newUser, UserAccountStatus accountStatus, UserAccountType accountType)
    throws NoSuchAlgorithmException {
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
   *
   * @param email
   * @param fname
   * @param lname
   * @param pwd
   * @param title
   * @return
   */
  public Users createNewAgent(String email, String fname, String lname, String pwd, String title) {
    String uname = generateUsername(email);
    List<BbcGroup> groups = new ArrayList<>();
    String salt = authController.generateSalt();
    String password = authController.getPasswordHash(pwd, salt);
    Users user = new Users(uname, password, email, fname, lname, title, "-", UserAccountStatus.NEW_MOBILE_ACCOUNT,
        UserAccountType.M_ACCOUNT_TYPE, 0, salt);
    user.setBbcGroupCollection(groups);
    return user;
  }

  /**
   * Create ldap user
   *
   * @param email
   * @param fname
   * @param lname
   * @param pwd
   * @param accStatus
   * @return
   */
  public Users createNewLdapUser(String email, String fname, String lname, String pwd, UserAccountStatus accStatus) {
    String uname = generateUsername(email);
    List<BbcGroup> groups = new ArrayList<>();
    String salt = authController.generateSalt();
    String password = authController.getPasswordHash(pwd, salt);

    Users user = new Users(uname, password, email, fname, lname, "-", "-", accStatus,
        UserAccountType.LDAP_ACCOUNT_TYPE, settings.getMaxNumProjPerUser(), salt);
    user.setBbcGroupCollection(groups);
    addAddress(user);
    addOrg(user);
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

  public void recoverPassword(String email, String securityQuestion, String securityAnswer, HttpServletRequest req)
    throws UserException, ServiceException {
    if (userValidator.isValidEmail(email) && userValidator.isValidsecurityQA(securityQuestion, securityAnswer)) {
      Users user = userFacade.findByEmail(email);
      if (user == null) {
        throw new UserException(RESTCodes.UserErrorCode.USER_WAS_NOT_FOUND, Level.FINE);
      }
      if (!authController.validateSecurityQA(user, securityQuestion, securityAnswer, req)) {
        throw new UserException(RESTCodes.UserErrorCode.SEC_QA_INCORRECT, Level.FINE);
      }
      authController.resetPassword(user, req);
    }
  }

  public void changePassword(String email, String oldPassword, String newPassword, String confirmedPassword,
      HttpServletRequest req) throws UserException {
    Users user = userFacade.findByEmail(email);

    if (!authController.validatePassword(user, oldPassword, req)) {
      throw new UserException(RESTCodes.UserErrorCode.PASSWORD_INCORRECT, Level.FINE);
    }
    if (userValidator.isValidPassword(newPassword, confirmedPassword)) {
      try {
        authController.changePassword(user, newPassword, req);
      } catch (Exception ex) {
        throw new UserException(RESTCodes.UserErrorCode.PASSWORD_RESET_UNSUCCESSFUL, Level.SEVERE, null,
          ex.getMessage(), ex);
      }
      accountAuditFacade.registerAccountChange(user, AccountsAuditActions.PASSWORDCHANGE.name(),
          AccountsAuditActions.SUCCESS.name(), "Changed password.", user, req);
      if (user.getEmail().compareTo("admin@kth.se") == 0) {
        settings.setAdminPasswordChanged();
      }

    }
  }

  public void changeSecQA(String email, String oldPassword, String securityQuestion, String securityAnswer,
      HttpServletRequest req) throws UserException {
    Users user = userFacade.findByEmail(email);
    if (!authController.validatePassword(user, oldPassword, req)) {
      throw new UserException(RESTCodes.UserErrorCode.PASSWORD_INCORRECT, Level.FINE);
    }

    if (userValidator.isValidsecurityQA(securityQuestion, securityAnswer)) {
      authController.changeSecQA(user, securityQuestion, securityAnswer, req);
    }
  }

  public Users updateProfile(String email, String firstName, String lastName, String telephoneNum, Integer toursState,
      HttpServletRequest req) throws UserException {
    Users user = userFacade.findByEmail(email);

    if (user == null) {
      throw new UserException(RESTCodes.UserErrorCode.USER_WAS_NOT_FOUND, Level.FINE);
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
    accountAuditFacade.registerAccountChange(user, AccountsAuditActions.SECQUESTION.name(),
        AccountsAuditActions.SUCCESS.name(), "Update Profile Info", user,
        req);
    userFacade.update(user);
    return user;
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
    while (user != null && count < 100) {
      suffix = String.valueOf(count);
      testUname = baseUsername.substring(0, (Settings.USERNAME_LEN - suffix.length()));
      user = userFacade.findByUsername(testUname + suffix);
      count++;
    }

    if (count == 100) {
      throw new IllegalStateException(
          "You cannot register with this email address. Pick another.");
    }
    return testUname + suffix;
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
   */
  public byte[] changeTwoFactor(Users user, String password, HttpServletRequest req) throws UserException {
    if (user == null) {
      throw new IllegalArgumentException("User was not provided.");
    }
    if (!authController.validatePassword(user, password, req)) {
      accountAuditFacade.registerAccountChange(user, AccountsAuditActions.TWO_FACTOR.name(),
          AccountsAuditActions.FAILED.name(), "Incorrect password", user,
          req);
      throw new UserException(RESTCodes.UserErrorCode.PASSWORD_INCORRECT, Level.FINE);
    }
    byte[] qr_code = null;
    if (user.getTwoFactor()) {
      user.setTwoFactor(false);
      userFacade.update(user);
      accountAuditFacade.registerAccountChange(user, AccountsAuditActions.TWO_FACTOR.name(),
          AccountsAuditActions.SUCCESS.name(), "Disabled 2-factor", user,
          req);
    } else {
      try {
        user.setTwoFactor(true);
        userFacade.update(user);
        qr_code = QRCodeGenerator.getQRCodeBytes(user.getEmail(),
            Settings.ISSUER, user.getSecret());
        accountAuditFacade.registerAccountChange(user, AccountsAuditActions.TWO_FACTOR.name(),
            AccountsAuditActions.SUCCESS.name(), "Enabled 2-factor", user,
            req);
        accountAuditFacade.registerAccountChange(user, AccountsAuditActions.QRCODE.name(),
            AccountsAuditActions.SUCCESS.name(), "Enabled 2-factor", user,
            req);
      } catch (IOException | WriterException ex) {
        LOGGER.log(Level.SEVERE, null, ex);
        accountAuditFacade.registerAccountChange(user, AccountsAuditActions.TWO_FACTOR.name(),
            AccountsAuditActions.FAILED.name(), "Enabled 2-factor", user,
            req);
        accountAuditFacade.registerAccountChange(user, AccountsAuditActions.QRCODE.name(),
            AccountsAuditActions.FAILED.name(), "Enabled 2-factor", user,
            req);
        throw new UserException(RESTCodes.UserErrorCode.TWO_FA_ENABLE_ERROR, Level.SEVERE,
          "user: " + user.getUsername(), ex.getMessage(), ex);
      }
    }
    return qr_code;
  }

  /**
   * Returns the QR code for the user if two factor is enabled.
   *
   * @param user
   * @param password
   * @param req
   * @return null if two factor is disabled.
   */
  public byte[] getQRCode(Users user, String password, HttpServletRequest req) throws UserException {
    if (user == null) {
      throw new IllegalArgumentException("User was not provided");
    }
    if (!authController.validatePassword(user, password, req)) {
      throw new UserException(RESTCodes.UserErrorCode.PASSWORD_INCORRECT, Level.FINE);
    }
    byte[] qr_code = null;
    if (user.getTwoFactor()) {
      try {
        qr_code = QRCodeGenerator.getQRCodeBytes(user.getEmail(), Settings.ISSUER, user.getSecret());
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

  public void changeAccountStatus(int id, String note, UserAccountStatus status) {
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

  public void updateStatus(Users id, UserAccountStatus stat) {
    id.setStatus(stat);
    userFacade.update(id);
  }

  public void updateSecret(int id, String sec) {
    Users p = userFacade.find(id);
    p.setSecret(sec);
    userFacade.update(p);
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
