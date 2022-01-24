/*
 * This file is part of Hopsworks
 * Copyright (C) 2021, Logical Clocks AB. All rights reserved
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

package io.hops.hopsworks.common.user;

import io.hops.hopsworks.exceptions.UserException;
import io.hops.hopsworks.persistence.entity.user.Users;
import io.hops.hopsworks.restutils.RESTCodes;

import javax.enterprise.inject.Instance;
import java.util.logging.Level;

public interface UserAccountHandler {
  void create(Users user) throws Exception;
  void update(Users user) throws Exception;
  void remove(Users user) throws Exception;
  
  String getClassName();
  
  static void runUserAccountCreateHandlers(Instance<UserAccountHandler> userAccountHandlers, Users user)
      throws UserException {
    for (UserAccountHandler handler : userAccountHandlers) {
      try {
        handler.create(user);
      } catch (Exception e) {
        String msg = "user: "+ user.getUsername() + ", handler: "+ handler.getClassName();
        throw new UserException(RESTCodes.UserErrorCode.USER_ACCOUNT_HANDLER_CREATE_ERROR, Level.SEVERE,
          e.getMessage(), msg, e);
      }
    }
  }
  
  static void runUserAccountUpdateHandlers(Instance<UserAccountHandler> userAccountHandlers, Users user)
      throws UserException {
    for (UserAccountHandler handler : userAccountHandlers) {
      try {
        handler.update(user);
      } catch (Exception e) {
        String msg = "user: "+ user.getUsername() + ", handler: "+ handler.getClassName();
        throw new UserException(RESTCodes.UserErrorCode.USER_ACCOUNT_HANDLER_UPDATE_ERROR, Level.SEVERE,
          e.getMessage(), msg, e);
      }
    }
  }
  
  static void runUserAccountDeleteHandlers(Instance<UserAccountHandler> userAccountHandlers, Users user)
      throws UserException {
    for (UserAccountHandler handler : userAccountHandlers) {
      try {
        handler.remove(user);
      } catch (Exception e) {
        throw new UserException(RESTCodes.UserErrorCode.USER_ACCOUNT_HANDLER_REMOVE_ERROR, Level.SEVERE,
          e.getMessage(),
          "user: " + user.getUsername() + ", status: " + user.getStatusName() + ", handler: " + handler.getClassName(),
          e);
      }
    }
  }
}

