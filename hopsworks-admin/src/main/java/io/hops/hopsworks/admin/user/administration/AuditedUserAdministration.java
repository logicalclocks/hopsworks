/*
 * This file is part of Hopsworks
 * Copyright (C) 2020, Logical Clocks AB. All rights reserved
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
 */
package io.hops.hopsworks.admin.user.administration;

import io.hops.hopsworks.common.dao.user.UserFacade;
import io.hops.hopsworks.common.dao.user.Users;
import io.hops.hopsworks.common.dao.user.security.audit.AccountAuditFacade;
import io.hops.hopsworks.common.dao.user.security.ua.UserAccountStatus;
import io.hops.hopsworks.common.user.AuthController;
import io.hops.hopsworks.common.user.UsersController;
import io.hops.hopsworks.common.util.FormatUtils;
import io.hops.hopsworks.common.util.HttpUtil;
import io.hops.hopsworks.common.util.Settings;
import io.hops.hopsworks.exceptions.UserException;
import io.hops.hopsworks.restutils.RESTCodes;

import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.mail.MessagingException;
import javax.servlet.http.HttpServletRequest;
import java.util.logging.Level;

@Stateless
@TransactionAttribute(TransactionAttributeType.NEVER)
public class AuditedUserAdministration {
  
  @EJB
  private UserFacade userFacade;
  @EJB
  protected UsersController usersController;
  @EJB
  private AuthController authController;
  @EJB
  private Settings settings;
  @EJB
  private AccountAuditFacade accountAuditFacade;
  
  public void activateUser(Users user, HttpServletRequest httpServletRequest)
    throws UserException {//httpServletRequest needed for logging
    usersController.activateUser(user);
  }
  
  public void addRole(Users user, String role, HttpServletRequest httpServletRequest)
    throws UserException {//httpServletRequest needed for logging
    usersController.addRole(role, user);
  }
  
  public void removeRole(Users user, String role,
    HttpServletRequest httpServletRequest) throws UserException {//httpServletRequest needed for logging
    usersController.removeRole(role, user);
  }
  
  public void changeStatus(Users user, UserAccountStatus accountStatus,
    HttpServletRequest httpServletRequest) throws UserException {//httpServletRequest needed for logging
    usersController.changeAccountStatus(user.getUid(), accountStatus.getUserStatus(), accountStatus);
  }
  
  public void resendAccountVerificationEmail(Users user, HttpServletRequest request)
    throws MessagingException {
    String linkUrl = FormatUtils.getUserURL(request) + settings.getEmailVerificationEndpoint();
    authController.sendNewValidationKey(user, linkUrl);
  }
  
  public String resetPassword(Users user, HttpServletRequest request)
    throws MessagingException, UserException {//httpServletRequest needed for logging
    Users init = userFacade.findByEmail(request.getRemoteUser());
    String remoteHost = HttpUtil.extractRemoteHostIp(request);
    String userAgent = HttpUtil.extractUserAgent(request);
    String pwd;
    try {
      pwd = usersController.resetPassword(user, request.getRemoteUser());
      accountAuditFacade.registerAccountChange(init, "PASSWORD CHANGE", "SUCCESS", "Admin reset password", user,
        remoteHost, userAgent);
    } catch (UserException ue) {
      accountAuditFacade.registerAccountChange(init, "PASSWORD CHANGE", "FAILED", "Admin reset password", user,
        remoteHost, userAgent);
      throw ue;
    }
    return pwd;
  }
  
  public void setMaxProject(Users user, int num, HttpServletRequest request) {
    //httpServletRequest needed for logging
    usersController.updateMaxNumProjs(user, num);
  }
  
  public void updateProfile(Users user, HttpServletRequest request) {
    //httpServletRequest needed for logging
    userFacade.update(user);
  }
  
  //Can not be audited b/c target user will be deleted, but will be logged.
  public void deleteSpamUser(Users user, HttpServletRequest request) throws UserException { //httpServletRequest
    // needed for logging
    if (UserAccountStatus.SPAM_ACCOUNT.equals(user.getStatus())) {
      usersController.deleteUser(user);
    } else {
      throw new UserException(RESTCodes.UserErrorCode.ACCOUNT_DELETION_ERROR, Level.FINE, "Not a spam account");
    }
  }
}
