/*
 * Copyright (C) 2020, Logical Clocks AB. All rights reserved
 */
package io.hops.hopsworks.common.user;

import io.hops.hopsworks.common.dao.user.UserFacade;
import io.hops.hopsworks.common.security.utils.SecurityUtils;
import io.hops.hopsworks.common.util.CloudClient;
import io.hops.hopsworks.exceptions.UserException;
import io.hops.hopsworks.persistence.entity.user.Users;
import io.hops.hopsworks.persistence.entity.user.security.ua.UserAccountType;
import io.hops.hopsworks.persistence.entity.user.security.ua.ValidationKeyType;
import io.hops.hopsworks.restutils.RESTCodes;

import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;

@Stateless
@TransactionAttribute(TransactionAttributeType.NEVER)
public class CloudUserController {
  
  @EJB
  private UserFacade userFacade;
  @EJB
  private CloudClient cloudClient;
  @EJB
  private SecurityUtils securityUtils;
  @EJB
  private AuthController authController;
  
  public void sendPasswordRecoveryEmailForCloud(String email, String reqUrl) throws
      UserException {
    Users user = userFacade.findByEmail(email);
    sendNewRecoveryValidationKeyForCloud(user, reqUrl, true);
  }
  
  private void sendNewRecoveryValidationKeyForCloud(Users user, String url,
      boolean isPassword) throws UserException {
    if (user == null) {
      throw new IllegalArgumentException("User not set.");
    }
    if (UserAccountType.REMOTE_ACCOUNT_TYPE.equals(user.getMode())) {
      throw new UserException(RESTCodes.UserErrorCode.USER_WAS_NOT_FOUND, Level.FINE);
    }
    authController.checkUserStatus(user, false);
    
    String resetToken;
    long validForHour = TimeUnit.HOURS.toMillis(SecurityUtils.RESET_LINK_VALID_FOR_HOUR);
    //resend the same token exp date > 5min
    if (user.getValidationKey() != null && user.getValidationKeyType() != null && user.getValidationKeyUpdated() != null
        && user.getValidationKeyType().equals(isPassword ? ValidationKeyType.PASSWORD : ValidationKeyType.QR_RESET) &&
        authController.diffMillis(user.getValidationKeyUpdated()) > TimeUnit.MINUTES.toMillis(5)) {
      resetToken = user.getValidationKey();
      validForHour = authController.diffMillis(user.getValidationKeyUpdated());
    } else {
      resetToken = securityUtils.generateSecureRandomString();
      authController.setValidationKey(user, resetToken, isPassword ?
          ValidationKeyType.PASSWORD : ValidationKeyType.QR_RESET);
    }
  
    cloudClient.notifyToSendEmail(user.getEmail(), user.getUsername(),
        resetToken, validForHour);
  }
}
