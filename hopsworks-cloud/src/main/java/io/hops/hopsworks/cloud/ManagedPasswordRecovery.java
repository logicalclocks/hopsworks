/*
 * Copyright (C) 2020, Logical Clocks AB. All rights reserved
 */
package io.hops.hopsworks.cloud;

import io.hops.hopsworks.common.user.AuthController;
import io.hops.hopsworks.common.user.PasswordRecovery;
import io.hops.hopsworks.exceptions.UserException;
import io.hops.hopsworks.persistence.entity.user.Users;
import io.hops.hopsworks.restutils.RESTCodes;

import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.mail.MessagingException;
import java.util.logging.Level;
import java.util.logging.Logger;

@ManagedStereotype
@Stateless
@TransactionAttribute(TransactionAttributeType.NEVER)
public class ManagedPasswordRecovery implements PasswordRecovery {

  private static final Logger LOG = Logger.getLogger(ManagedPasswordRecovery.class.getName());

  @EJB
  private AuthController authController;
  @EJB
  private CloudClient cloudClient;

  @Override
  public void validateSecurityQAndStatus(Users user, String securityQuestion, String security) throws UserException {
    // We don't check security questions on Cloud
    if (user == null) {
      throw new UserException(RESTCodes.UserErrorCode.USER_WAS_NOT_FOUND, Level.FINE);
    }
    authController.checkUserStatus(user, false);
  }

  @Override
  public void sendRecoveryNotification(Users user, String url, boolean isPassword,
          AuthController.CredentialsResetToken resetToken) throws MessagingException, UserException {
    cloudClient.notifyToSendEmail(user.getEmail(), user.getUsername(), resetToken);
  }
}
