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
package io.hops.hopsworks.admin.user.account;

import io.hops.hopsworks.audit.auditor.AuditType;
import io.hops.hopsworks.audit.auditor.annotation.AuditTarget;
import io.hops.hopsworks.audit.auditor.annotation.Audited;
import io.hops.hopsworks.audit.helper.AuditAction;
import io.hops.hopsworks.audit.helper.UserIdentifier;
import io.hops.hopsworks.audit.logger.annotation.Caller;
import io.hops.hopsworks.audit.logger.annotation.Logged;
import io.hops.hopsworks.audit.logger.annotation.Secret;
import io.hops.hopsworks.common.user.UsersController;
import io.hops.hopsworks.exceptions.UserException;
import io.hops.hopsworks.persistence.entity.user.Users;
import io.hops.hopsworks.persistence.entity.util.FormatUtils;

import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.mail.MessagingException;
import javax.servlet.http.HttpServletRequest;
import java.util.logging.Logger;

@Stateless
@Logged
@TransactionAttribute(TransactionAttributeType.NEVER)
public class AuditedUserAccountAction {
  
  private static final Logger LOGGER = Logger.getLogger(AuditedUserAccountAction.class.getName());
  @EJB
  protected UsersController usersController;

  @Audited(type = AuditType.ACCOUNT_AUDIT, action = AuditAction.VERIFIED_ACCOUNT, message = "Account email " +
    "verification")
  public void validateKey(@Caller(UserIdentifier.KEY) @AuditTarget(UserIdentifier.KEY) String key,
    HttpServletRequest req) throws UserException {
    usersController.validateKey(key);
  }
  
  @Audited(type = AuditType.ACCOUNT_AUDIT, action = AuditAction.PASSWORD, message = "User requested password recovery")
  public void sendPasswordRecoveryEmail(@Caller(UserIdentifier.USERNAME) @AuditTarget(UserIdentifier.USERNAME)
                                              String username, HttpServletRequest req)
    throws MessagingException, UserException {
    String reqUrl = FormatUtils.getUserURL(req);
    usersController.sendPasswordRecoveryEmail(username, reqUrl);
  }
  
  @Audited(type = AuditType.ACCOUNT_AUDIT, action = AuditAction.PASSWORD, message = "User verified password recovery " +
    "key")
  public void checkRecoveryKey(@Caller(UserIdentifier.KEY) @AuditTarget(UserIdentifier.KEY) String key,
    HttpServletRequest req) throws UserException {
    usersController.checkRecoveryKey(key);
  }
  
  @Audited(type = AuditType.ACCOUNT_AUDIT, action = AuditAction.PASSWORD_CHANGE, message = "User recovered password")
  public void changePassword(@Caller(UserIdentifier.KEY) @AuditTarget(UserIdentifier.KEY) String key,
    @Secret String passwd1, @Secret String passwd2, HttpServletRequest req) throws MessagingException, UserException {
    usersController.changePassword(key, passwd1, passwd2);
  }
  
  @Audited(type = AuditType.ACCOUNT_AUDIT, action = AuditAction.PASSWORD_CHANGE, message = "User changed password")
  public void changePassword(@Caller(UserIdentifier.USERS) @AuditTarget(UserIdentifier.USERS) Users user,
    @Secret String current, @Secret String passwd1, @Secret  String passwd2, HttpServletRequest req)
    throws UserException {
    usersController.changePassword(user, current, passwd1, passwd2);
  }
  
  @Audited(type = AuditType.ACCOUNT_AUDIT, action = AuditAction.QR_CODE, message = "User recovered QR code")
  public byte[] recoverQRCodeByte(@Caller(UserIdentifier.KEY) @AuditTarget(UserIdentifier.KEY) String key,
    HttpServletRequest req) throws UserException, MessagingException {
    return usersController.recoverQRCodeByte(key);
  }
  
  @Audited(type = AuditType.ACCOUNT_AUDIT, action = AuditAction.QR_CODE, message = "User requested QR code recovery")
  public void sendQRRecoveryEmail(@Caller(UserIdentifier.USERNAME) @AuditTarget(UserIdentifier.USERNAME) String uname,
    @Secret String passwd, HttpServletRequest req) throws MessagingException, UserException {
    String reqUrl = FormatUtils.getUserURL(req);
    usersController.sendQRRecoveryEmail(uname, passwd, reqUrl);
  }
}
