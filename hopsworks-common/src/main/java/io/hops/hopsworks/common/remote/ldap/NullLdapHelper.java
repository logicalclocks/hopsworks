/*
 * This file is part of Hopsworks
 * Copyright (C) 2020, Logical Clocks AB. All rights reserved
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
package io.hops.hopsworks.common.remote.ldap;

import io.hops.hopsworks.common.integrations.NullRemoteAuthStereotype;
import io.hops.hopsworks.common.remote.RemoteUserDTO;
import io.hops.hopsworks.common.remote.RemoteUsersDTO;
import io.hops.hopsworks.exceptions.UserException;
import io.hops.hopsworks.persistence.entity.remote.group.RemoteGroupProjectMapping;

import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import java.util.List;

@NullRemoteAuthStereotype
@Stateless
@TransactionAttribute(TransactionAttributeType.NEVER)
public class NullLdapHelper implements LdapHelper{
  @Override
  public boolean isLdapAvailable() {
    return false;
  }
  
  @Override
  public List<String> getLDAPGroups(String groupQuery) {
    return null;
  }
  
  @Override
  public RemoteUsersDTO getMembers(List<String> target) {
    return null;
  }
  
  @Override
  public void addNewGroupProjectMapping(RemoteGroupProjectMapping remoteGroupProjectMapping) {
    throw new UnsupportedOperationException("Remote auth not supported.");
  }
  
  @Override
  public RemoteUserDTO getRemoteUserByUuid(String uuid) throws UserException {
    throw new UnsupportedOperationException("Remote auth not supported.");
  }
}
