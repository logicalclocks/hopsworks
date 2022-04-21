/*
 * This file is part of Hopsworks
 * Copyright (C) 2021, Logical Clocks AB. All rights reserved
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
package io.hops.hopsworks.common.tags;

import io.hops.hopsworks.common.dataset.util.DatasetPath;
import io.hops.hopsworks.common.integrations.CommunityStereotype;
import io.hops.hopsworks.persistence.entity.user.Users;

import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import java.util.Map;

@Stateless
@CommunityStereotype
@TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
public class TagController implements TagControllerIface {
  @Override
  public String get(Users user, DatasetPath path, String name) {
    throw new IllegalArgumentException("API not supported in the community edition");
  }
  
  @Override
  public Map<String, String> getAll(Users user, DatasetPath path) {
    throw new IllegalArgumentException("API not supported in the community edition");
  }
  
  @Override
  public AttachTagResult upsert(Users user, DatasetPath path, String name, String value) {
    throw new IllegalArgumentException("API not supported in the community edition");
  }
  
  @Override
  public AttachTagResult upsertAll(Users user, DatasetPath path, Map<String, String> newTags) {
    throw new IllegalArgumentException("API not supported in the community edition");
  }
  
  @Override
  public void delete(Users user, DatasetPath path, String name) {
    throw new IllegalArgumentException("API not supported in the community edition");
  }
  
  @Override
  public void deleteAll(Users user, DatasetPath path) {
    throw new IllegalArgumentException("API not supported in the community edition");
  }
}
