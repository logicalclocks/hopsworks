package io.hops.hopsworks.common.user;

import javax.ejb.Stateless;
import javax.ws.rs.core.Response;
import io.hops.hopsworks.common.constants.auth.AccountStatusErrorMessages;
import io.hops.hopsworks.common.dao.user.security.ua.PeopleAccountStatus;
import io.hops.hopsworks.common.exception.AppException;

@Stateless
public class UserStatusValidator {

  public boolean checkStatus(int status) throws AppException {
    if (status == PeopleAccountStatus.NEW_MOBILE_ACCOUNT.getValue() || status
            == PeopleAccountStatus.NEW_YUBIKEY_ACCOUNT.getValue()) {
      throw new AppException(Response.Status.UNAUTHORIZED.getStatusCode(),
              AccountStatusErrorMessages.INACTIVE_ACCOUNT);
    }
    if (status == PeopleAccountStatus.BLOCKED_ACCOUNT.getValue()) {
      throw new AppException(Response.Status.UNAUTHORIZED.getStatusCode(),
              AccountStatusErrorMessages.BLOCKED_ACCOUNT);
    }
    if (status == PeopleAccountStatus.DEACTIVATED_ACCOUNT.getValue()) {
      throw new AppException(Response.Status.UNAUTHORIZED.getStatusCode(),
              AccountStatusErrorMessages.DEACTIVATED_ACCOUNT);
    }
    if (status == PeopleAccountStatus.LOST_MOBILE.getValue() || status
            == PeopleAccountStatus.LOST_YUBIKEY.getValue()) {
      throw new AppException(Response.Status.UNAUTHORIZED.getStatusCode(),
              AccountStatusErrorMessages.LOST_DEVICE);
    }
    return true;
  }
}
