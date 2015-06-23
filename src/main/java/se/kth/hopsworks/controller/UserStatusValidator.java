package se.kth.hopsworks.controller;

import javax.ejb.Stateless;
import javax.ws.rs.core.Response;
import se.kth.hopsworks.rest.AppException;
import se.kth.hopsworks.user.model.UserAccountStatus;

/**
 * @author André<amore@kth.se>
 * @author Ermias<ermiasg@kth.se>
 */
@Stateless
public class UserStatusValidator {

  public boolean checkStatus(int status) throws AppException {
    if (status == UserAccountStatus.ACCOUNT_INACTIVE.getValue()) {
      throw new AppException(Response.Status.BAD_REQUEST.getStatusCode(),
              ResponseMessages.ACCOUNT_REQUEST);
    }
    if (status == UserAccountStatus.ACCOUNT_BLOCKED.getValue()) {
      throw new AppException(Response.Status.BAD_REQUEST.getStatusCode(),
              ResponseMessages.ACCOUNT_BLOCKED);
    }
    if (status == UserAccountStatus.ACCOUNT_DEACTIVATED.getValue()) {
      throw new AppException(Response.Status.BAD_REQUEST.getStatusCode(),
              ResponseMessages.ACCOUNT_DEACTIVATED);
    }
    return true;
  }
}
