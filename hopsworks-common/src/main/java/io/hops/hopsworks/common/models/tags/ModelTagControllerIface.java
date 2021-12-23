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

import io.hops.hopsworks.common.tags.AttachTagResult;
import io.hops.hopsworks.exceptions.DatasetException;
import io.hops.hopsworks.exceptions.SchematizedTagException;
import io.hops.hopsworks.exceptions.MetadataException;
import io.hops.hopsworks.persistence.entity.project.Project;
import io.hops.hopsworks.persistence.entity.user.Users;

import java.util.Map;

public interface ModelTagControllerIface {

  String get(Project accessProject, Users user, String path, String name)
    throws DatasetException, MetadataException, SchematizedTagException;

  Map<String, String> getAll(Project accessProject, Users user, String path)
    throws DatasetException, MetadataException;

  AttachTagResult upsert(Project accessProject, Users user, String path,
                       String name, String value)
    throws MetadataException, SchematizedTagException;

  AttachTagResult upsertAll(Project accessProject, Users user, String path,
                       Map<String, String> tags)
    throws MetadataException, SchematizedTagException;

  void delete(Project accessProject, Users user, String path, String name)
      throws DatasetException, MetadataException;

  void deleteAll(Project accessProject, Users user, String path)
    throws DatasetException, MetadataException;

}
