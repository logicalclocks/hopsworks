package io.hops.hopsworks.common.remote;

import io.hops.hopsworks.common.integrations.NullRemoteAuthStereotype;
import io.hops.hopsworks.exceptions.GenericException;
import io.hops.hopsworks.exceptions.UserException;
import io.hops.hopsworks.persistence.entity.remote.user.RemoteUser;
import io.hops.hopsworks.persistence.entity.remote.user.RemoteUserType;
import io.hops.hopsworks.persistence.entity.user.security.ua.UserAccountStatus;

import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;

@NullRemoteAuthStereotype
@Stateless
@TransactionAttribute(TransactionAttributeType.NEVER)
public class NullRemoteUserHelper implements RemoteUserHelper {
  
  @Override
  public void createRemoteUser(RemoteUserDTO userDTO, String email, String givenName, String surname,
    RemoteUserType type, UserAccountStatus status) throws GenericException, UserException {
    throw new UnsupportedOperationException("Remote auth not supported.");
  }
  
  @Override
  public RemoteUser getRemoteUser(String uuid) throws UserException {
    return null;
  }
}
