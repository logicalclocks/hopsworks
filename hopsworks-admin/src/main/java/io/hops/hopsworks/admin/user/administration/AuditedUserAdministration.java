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

import io.hops.hopsworks.audit.auditor.AuditType;
import io.hops.hopsworks.audit.auditor.annotation.AuditTarget;
import io.hops.hopsworks.audit.auditor.annotation.Audited;
import io.hops.hopsworks.audit.auditor.annotation.AuditedList;
import io.hops.hopsworks.audit.helper.AuditAction;
import io.hops.hopsworks.audit.helper.UserIdentifier;
import io.hops.hopsworks.audit.logger.LogLevel;
import io.hops.hopsworks.audit.logger.annotation.Logged;
import io.hops.hopsworks.common.dao.user.UserFacade;
import io.hops.hopsworks.common.dao.user.Users;
import io.hops.hopsworks.common.dao.user.security.ua.UserAccountStatus;
import io.hops.hopsworks.common.user.AuthController;
import io.hops.hopsworks.common.user.UsersController;
import io.hops.hopsworks.common.util.FormatUtils;
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
@Logged(logLevel = LogLevel.WARNING)
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
  
  @AuditedList({@Audited(type = AuditType.ACCOUNT_AUDIT, action = AuditAction.ACTIVATED_ACCOUNT, message = "Activated" +
    " account"),
    @Audited(type = AuditType.ROLE_AUDIT, action = AuditAction.ROLE_ADDED, message = "Added role")})
  public void activateUser(@AuditTarget(UserIdentifier.USERS) Users user, HttpServletRequest httpServletRequest)
    throws UserException {//httpServletRequest needed for logging
    usersController.activateUser(user);
  }
  
  @AuditedList({@Audited(type = AuditType.ACCOUNT_AUDIT, action = AuditAction.ROLE_ADDED, message = "Add role"),
    @Audited(type = AuditType.ROLE_AUDIT, action = AuditAction.ROLE_ADDED, message = "Add role")})
  public void addRole(@AuditTarget(UserIdentifier.USERS) Users user, String role, HttpServletRequest httpServletRequest)
    throws UserException {//httpServletRequest needed for logging
    usersController.addRole(role, user);
  }
  
  @AuditedList({@Audited(type = AuditType.ACCOUNT_AUDIT, action = AuditAction.ROLE_REMOVED, message = "Removed role"),
    @Audited(type = AuditType.ROLE_AUDIT, action = AuditAction.ROLE_REMOVED, message = "Removed role")})
  public void removeRole(@AuditTarget(UserIdentifier.USERS) Users user, String role,
    HttpServletRequest httpServletRequest) throws UserException {//httpServletRequest needed for logging
    usersController.removeRole(role, user);
  }
  
  @Audited(type = AuditType.ACCOUNT_AUDIT, action = AuditAction.CHANGED_STATUS, message = "Status change")
  public void changeStatus(@AuditTarget(UserIdentifier.USERS) Users user, UserAccountStatus accountStatus,
    HttpServletRequest httpServletRequest) throws UserException {//httpServletRequest needed for logging
    usersController.changeAccountStatus(user.getUid(), accountStatus.getUserStatus(), accountStatus);
  }
  
  @Audited(type = AuditType.ACCOUNT_AUDIT, action = AuditAction.REGISTRATION, message = "New validation key")
  public void resendAccountVerificationEmail(@AuditTarget(UserIdentifier.USERS) Users user, HttpServletRequest request)
    throws MessagingException {
    String linkUrl = FormatUtils.getUserURL(request) + settings.getEmailVerificationEndpoint();
    authController.sendNewValidationKey(user, linkUrl);
  }
  
  @Audited(type = AuditType.ACCOUNT_AUDIT, action = AuditAction.PASSWORD_CHANGE, message = "Admin reset password")
  public String resetPassword(@AuditTarget(UserIdentifier.USERS) Users user, HttpServletRequest request)
    throws MessagingException, UserException {//httpServletRequest needed for logging
    return usersController.resetPassword(user, request.getRemoteUser());
  }
  
  @Audited(type = AuditType.ACCOUNT_AUDIT, action = AuditAction.PROFILE_UPDATE, message = "Set max project")
  public void setMaxProject(@AuditTarget(UserIdentifier.USERS) Users user, int num, HttpServletRequest request) {
    //httpServletRequest needed for logging
    usersController.updateMaxNumProjs(user, num);
  }
  
  @Audited(type = AuditType.ACCOUNT_AUDIT, action = AuditAction.PROFILE_UPDATE, message = "User updated profile")
  public void updateProfile(@AuditTarget(UserIdentifier.USERS) Users user, HttpServletRequest request) {
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
