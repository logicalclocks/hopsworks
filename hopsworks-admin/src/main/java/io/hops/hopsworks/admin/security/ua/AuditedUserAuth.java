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
package io.hops.hopsworks.admin.security.ua;

import io.hops.hopsworks.common.dao.user.Users;
import io.hops.hopsworks.common.user.AuthController;
import io.hops.hopsworks.exceptions.UserException;

import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import java.util.logging.Logger;

@Stateless
@TransactionAttribute(TransactionAttributeType.NEVER)
public class AuditedUserAuth {
  
  private static final Logger LOGGER = Logger.getLogger(AuditedUserAuth.class.getName());
  @EJB
  private AuthController authController;
  
  public void login(Users user, String password, String otp, HttpServletRequest req) throws UserException,
    ServletException {
    String passwordWithSaltPlusOtp = authController.preCustomRealmLoginCheck(user, password, otp);
    req.login(user.getEmail(), passwordWithSaltPlusOtp);
    authController.registerLogin(user, req);
  }
  
  public void logout(Users user, HttpServletRequest req) throws ServletException {
    req.getSession().invalidate();
    req.logout();
    if (user != null) {
      authController.registerLogout(user);
    }
  }
}
