/*
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
 *
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
