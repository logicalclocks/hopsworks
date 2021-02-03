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

package io.hops.hopsworks.common.featurestore.tag;

import io.hops.hopsworks.exceptions.FeatureStoreTagException;
import io.hops.hopsworks.persistence.entity.featurestore.tag.FeatureStoreTag;

import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import java.util.Map;

@TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
public interface FeatureStoreTagSchemaControllerIface {
  default void create(String name, String schema) throws FeatureStoreTagException {
    throw new IllegalArgumentException("API not supported in the community edition");
  }
  default void delete(String name) {
    throw new IllegalArgumentException("API not supported in the community edition");
  }
  default void delete(FeatureStoreTag tag) {
    throw new IllegalArgumentException("API not supported in the community edition");
  }
  default Map<String, String> getAll() {
    throw new IllegalArgumentException("API not supported in the community edition");
  }
}
