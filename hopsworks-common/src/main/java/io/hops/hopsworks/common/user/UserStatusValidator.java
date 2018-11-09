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

import io.hops.hopsworks.common.dao.user.Users;
import io.hops.hopsworks.common.dao.user.security.ua.UserAccountStatus;
import io.hops.hopsworks.common.exception.RESTCodes;
import io.hops.hopsworks.common.exception.UserException;

import javax.ejb.Stateless;
import java.util.logging.Level;

@Stateless
public class UserStatusValidator {

  public boolean checkStatus(UserAccountStatus status) throws UserException {
    if (status.equals(UserAccountStatus.NEW_MOBILE_ACCOUNT)) {
      throw new UserException(RESTCodes.UserErrorCode.ACCOUNT_INACTIVE, Level.FINE);
    }
    if (status.equals(UserAccountStatus.BLOCKED_ACCOUNT)) {
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

  public boolean isNewAccount(Users user) {
    if (user == null) {
      return false;
    }
    return user.getStatus().equals(UserAccountStatus.NEW_MOBILE_ACCOUNT);
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
