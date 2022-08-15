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

package io.hops.hopsworks.common.tags;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.hops.hopsworks.exceptions.GenericException;
import io.hops.hopsworks.exceptions.SchematizedTagException;
import io.hops.hopsworks.persistence.entity.featurestore.tag.TagSchemas;

import java.util.Map;

public interface TagSchemasControllerIface {
  void create(String name, String schema) throws SchematizedTagException, GenericException;
  void delete(String name) throws GenericException;
  void delete(TagSchemas tag) throws GenericException;
  Map<String, String> getAll() throws GenericException;
  
  boolean schemaHasNestedTypes(String schema) throws SchematizedTagException, GenericException;
  boolean schemaHasAdditionalRules(String name, String schema, ObjectMapper objectMapper)
    throws SchematizedTagException, GenericException;
}
