/*
 * This file is part of HopsWorks
 *
 * Copyright (C) 2013 - 2018, Logical Clocks AB and RISE SICS AB. All rights reserved.
 *
 * HopsWorks is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * HopsWorks is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with HopsWorks.  If not, see <http://www.gnu.org/licenses/>.
 */

package io.hops.hopsworks.common.user;

import javax.ejb.Stateless;
import javax.ws.rs.core.Response;
import io.hops.hopsworks.common.constants.auth.AccountStatusErrorMessages;
import io.hops.hopsworks.common.dao.user.Users;
import io.hops.hopsworks.common.dao.user.security.ua.UserAccountStatus;
import io.hops.hopsworks.common.exception.AppException;

@Stateless
public class UserStatusValidator {

  public boolean checkStatus(UserAccountStatus status) throws AppException {
    if (status.equals(UserAccountStatus.NEW_MOBILE_ACCOUNT)) {
      throw new AppException(Response.Status.UNAUTHORIZED.getStatusCode(), AccountStatusErrorMessages.INACTIVE_ACCOUNT);
    }
    if (status.equals(UserAccountStatus.BLOCKED_ACCOUNT)) {
      throw new AppException(Response.Status.UNAUTHORIZED.getStatusCode(), AccountStatusErrorMessages.BLOCKED_ACCOUNT);
    }
    if (status.equals(UserAccountStatus.DEACTIVATED_ACCOUNT)) {
      throw new AppException(Response.Status.UNAUTHORIZED.getStatusCode(),
          AccountStatusErrorMessages.DEACTIVATED_ACCOUNT);
    }
    if (status.equals(UserAccountStatus.LOST_MOBILE)) {
      throw new AppException(Response.Status.UNAUTHORIZED.getStatusCode(), AccountStatusErrorMessages.LOST_DEVICE);
    }
    if (status.equals(UserAccountStatus.VERIFIED_ACCOUNT)) {
      throw new AppException(Response.Status.UNAUTHORIZED.getStatusCode(), 
          AccountStatusErrorMessages.UNAPPROVED_ACCOUNT);
    }
    return true;
  }

  public boolean isNewAccount(Users user) {
    if (user == null) {
      return false;
    }
    return user.getStatus().equals(UserAccountStatus.NEW_MOBILE_ACCOUNT) || user.getStatus().equals(
        UserAccountStatus.NEW_MOBILE_ACCOUNT);
  }

  public boolean isBlockedAccount(Users user) {
    if (user == null) {
      return false;
    }
    return user.getStatus().equals(UserAccountStatus.DEACTIVATED_ACCOUNT) || user.getStatus().equals(
        UserAccountStatus.BLOCKED_ACCOUNT) || user.getStatus().equals(UserAccountStatus.SPAM_ACCOUNT);
  }

  public boolean isLostDeviceAccount(Users user) {
    if (user == null) {
      return false;
    }
    return user.getStatus().equals(UserAccountStatus.LOST_MOBILE);
  }
}
