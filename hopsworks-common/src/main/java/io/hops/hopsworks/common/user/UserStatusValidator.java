package io.hops.hopsworks.common.user;

import javax.ejb.Stateless;
import javax.ws.rs.core.Response;
import io.hops.hopsworks.common.constants.auth.AccountStatusErrorMessages;
import io.hops.hopsworks.common.dao.user.security.ua.PeopleAccountStatus;
import io.hops.hopsworks.common.exception.AppException;

@Stateless
public class UserStatusValidator {

  public boolean checkStatus(PeopleAccountStatus status) throws AppException {
    if (status.equals(PeopleAccountStatus.NEW_MOBILE_ACCOUNT) || 
        status.equals(PeopleAccountStatus.NEW_YUBIKEY_ACCOUNT)) {
      throw new AppException(Response.Status.UNAUTHORIZED.getStatusCode(),
              AccountStatusErrorMessages.INACTIVE_ACCOUNT);
    }
    if (status.equals(PeopleAccountStatus.BLOCKED_ACCOUNT)) {
      throw new AppException(Response.Status.UNAUTHORIZED.getStatusCode(),
              AccountStatusErrorMessages.BLOCKED_ACCOUNT);
    }
    if (status.equals(PeopleAccountStatus.DEACTIVATED_ACCOUNT)) {
      throw new AppException(Response.Status.UNAUTHORIZED.getStatusCode(),
              AccountStatusErrorMessages.DEACTIVATED_ACCOUNT);
    }
    if (status.equals(PeopleAccountStatus.LOST_MOBILE) || status.equals(PeopleAccountStatus.LOST_YUBIKEY)) {
      throw new AppException(Response.Status.UNAUTHORIZED.getStatusCode(),
              AccountStatusErrorMessages.LOST_DEVICE);
    }
    return true;
  }
}
