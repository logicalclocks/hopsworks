package io.hops.hopsworks.common.models.tags;

/*
 * This file is part of Hopsworks
 * Copyright (C) 2022, Logical Clocks AB. All rights reserved
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

import io.hops.hopsworks.common.integrations.CommunityStereotype;
import io.hops.hopsworks.common.tags.AttachTagResult;
import io.hops.hopsworks.persistence.entity.project.Project;
import io.hops.hopsworks.persistence.entity.user.Users;

import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import java.util.Map;

@Stateless
@CommunityStereotype
@TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
public class ModelTagsController implements ModelTagControllerIface {

  @Override
  public String get(Project accessProject, Users user, String path, String name) {
    throw new IllegalArgumentException("API not supported in the community edition");
  }

  @Override
  public Map<String, String> getAll(Project accessProject, Users user, String path) {
    throw new IllegalArgumentException("API not supported in the community edition");
  }

  @Override
  public AttachTagResult upsert(Project accessProject, Users user, String path,
                                String name, String value) {
    throw new IllegalArgumentException("API not supported in the community edition");
  }

  @Override
  public AttachTagResult upsertAll(Project accessProject, Users user, String path,
                                Map<String, String> tags) {
    throw new IllegalArgumentException("API not supported in the community edition");
  }

  @Override
  public void delete(Project accessProject, Users user, String path, String name) {
    throw new IllegalArgumentException("API not supported in the community edition");
  }

  @Override
  public void deleteAll(Project accessProject, Users user, String path) {
    throw new IllegalArgumentException("API not supported in the community edition");
  }

}
