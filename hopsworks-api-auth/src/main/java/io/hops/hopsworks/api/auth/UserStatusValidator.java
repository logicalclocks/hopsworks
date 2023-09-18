/*
 * This file is part of Hopsworks
 * Copyright (C) 2023, Hopsworks AB. All rights reserved
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

package io.hops.hopsworks.api.auth;

import io.hops.hopsworks.exceptions.UserException;
import io.hops.hopsworks.persistence.entity.user.security.ua.UserAccountStatus;
import io.hops.hopsworks.restutils.RESTCodes;

import javax.ejb.Stateless;
import java.util.logging.Level;

@Stateless
public class UserStatusValidator {

  public boolean checkStatus(UserAccountStatus status) throws UserException {
    if (status.equals(UserAccountStatus.NEW_MOBILE_ACCOUNT)) {
      throw new UserException(RESTCodes.UserErrorCode.ACCOUNT_INACTIVE, Level.FINE);
    }
    if (status.equals(UserAccountStatus.BLOCKED_ACCOUNT) || status.equals(UserAccountStatus.SPAM_ACCOUNT)) {
      throw new UserException(RESTCodes.UserErrorCode.ACCOUNT_BLOCKED, Level.FINE);
    }
    if (status.equals(UserAccountStatus.DEACTIVATED_ACCOUNT)) {
      throw new UserException(RESTCodes.UserErrorCode.ACCOUNT_DEACTIVATED, Level.FINE);
    }
    if (status.equals(UserAccountStatus.LOST_MOBILE)) {
      throw new UserException(RESTCodes.UserErrorCode.ACCOUNT_LOST_DEVICE, Level.FINE);
    }
    if (status.equals(UserAccountStatus.VERIFIED_ACCOUNT)) {
      throw new UserException(RESTCodes.UserErrorCode.ACCOUNT_NOT_APPROVED, Level.FINE);
    }
    return true;
  }

  public void checkNewUserStatus(UserAccountStatus status) throws UserException {
    switch (status) {
      case NEW_MOBILE_ACCOUNT:
        return;
      case VERIFIED_ACCOUNT:
      case ACTIVATED_ACCOUNT:
        throw new UserException(RESTCodes.UserErrorCode.ACCOUNT_ALREADY_VERIFIED, Level.FINE);
      case LOST_MOBILE:
        throw new UserException(RESTCodes.UserErrorCode.ACCOUNT_LOST_DEVICE, Level.FINE);
      case DEACTIVATED_ACCOUNT:
        throw new UserException(RESTCodes.UserErrorCode.ACCOUNT_DEACTIVATED, Level.FINE);
      default:
        throw new UserException(RESTCodes.UserErrorCode.ACCOUNT_BLOCKED, Level.FINE);
    }
  }
}
