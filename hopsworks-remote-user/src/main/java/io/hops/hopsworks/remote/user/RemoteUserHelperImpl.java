/*
 * Copyright (C) 2020, Logical Clocks AB. All rights reserved
 */
package io.hops.hopsworks.remote.user;

import io.hops.hopsworks.common.remote.RemoteUserDTO;
import io.hops.hopsworks.common.remote.RemoteUserHelper;
import io.hops.hopsworks.exceptions.GenericException;
import io.hops.hopsworks.exceptions.UserException;
import io.hops.hopsworks.persistence.entity.remote.user.RemoteUser;
import io.hops.hopsworks.persistence.entity.remote.user.RemoteUserType;
import io.hops.hopsworks.persistence.entity.user.security.ua.UserAccountStatus;

import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;

@RemoteAuthStereotype
@Stateless
@TransactionAttribute(TransactionAttributeType.NEVER)
public class RemoteUserHelperImpl implements RemoteUserHelper {
  
  @EJB
  private RemoteUserAuthController remoteUserAuthController;
  
  @Override
  public void createRemoteUser(RemoteUserDTO userDTO, String email, String givenName, String surname,
    RemoteUserType type, UserAccountStatus status) throws GenericException, UserException {
    remoteUserAuthController.createRemoteUser(userDTO, email, givenName, surname, type, status);
  }
  
  @Override
  public RemoteUser getRemoteUser(String uuid) throws UserException {
    return remoteUserAuthController.getRemoteUser(uuid);
  }
}
