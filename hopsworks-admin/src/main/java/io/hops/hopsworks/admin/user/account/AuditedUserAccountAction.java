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

import io.hops.hopsworks.common.dao.user.Users;
import io.hops.hopsworks.common.dao.user.security.ua.SecurityQuestion;
import io.hops.hopsworks.common.user.UsersController;
import io.hops.hopsworks.common.util.FormatUtils;
import io.hops.hopsworks.exceptions.UserException;

import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.mail.MessagingException;
import javax.servlet.http.HttpServletRequest;
import java.util.logging.Logger;

@Stateless
@TransactionAttribute(TransactionAttributeType.NEVER)
public class AuditedUserAccountAction {
  
  private static final Logger LOGGER = Logger.getLogger(AuditedUserAccountAction.class.getName());
  @EJB
  protected UsersController usersController;
  
  public void validateKey(String key, HttpServletRequest req) throws UserException {
    usersController.validateKey(key);
  }

  public void sendPasswordRecoveryEmail(String username, SecurityQuestion question, String answer,
    HttpServletRequest req) throws MessagingException, UserException {
    String reqUrl = FormatUtils.getUserURL(req);
    usersController.sendPasswordRecoveryEmail(username, question.getValue(), answer, reqUrl);
  }
  
  public void checkRecoveryKey(String key, HttpServletRequest req) throws UserException {
    usersController.checkRecoveryKey(key);
  }
  
  public void changePassword(String key, String passwd1, String passwd2, HttpServletRequest req)
    throws MessagingException, UserException {
    usersController.changePassword(key, passwd1, passwd2);
  }
  
  public void changePassword(Users user,
    String current, String passwd1, String passwd2, HttpServletRequest req)
    throws UserException {
    usersController.changePassword(user, current, passwd1, passwd2);
  }
  
  public byte[] recoverQRCodeByte(String key, HttpServletRequest req) throws UserException, MessagingException {
    return usersController.recoverQRCodeByte(key);
  }
  
  public void sendQRRecoveryEmail(String uname, String passwd, HttpServletRequest req)
    throws MessagingException, UserException {
    String reqUrl = FormatUtils.getUserURL(req);
    usersController.sendQRRecoveryEmail(uname, passwd, reqUrl);
  }
}
