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
package io.hops.hopsworks.common.remote.group.mapping;

import io.hops.hopsworks.common.integrations.NullRemoteAuthStereotype;
import io.hops.hopsworks.exceptions.UserException;
import io.hops.hopsworks.persistence.entity.remote.user.RemoteUser;

import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;

@NullRemoteAuthStereotype
@Stateless
@TransactionAttribute(TransactionAttributeType.NEVER)
public class NullRemoteGroupMappingHelper implements RemoteGroupMappingHelper {
  @Override
  public boolean isRemoteGroupMappingAvailable() {
    return false;
  }
  
  @Override
  public void syncMapping(RemoteUser remoteUser) throws UserException {
    throw new UnsupportedOperationException("Remote auth not supported.");
  }
  
  @Override
  public void syncMappingAsync() {
    throw new UnsupportedOperationException("Remote auth not supported.");
  }
  
  @Override
  public void removeFromAllProjects(RemoteUser remoteUser) {
    throw new UnsupportedOperationException("Remote auth not supported.");
  }
}
