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

package io.hops.hopsworks.common.serving;

import io.hops.hopsworks.exceptions.ApiKeyException;
import io.hops.hopsworks.persistence.entity.user.Users;

import java.util.Map;

/**
 * Interface for managing serving configuration. Different type of serving
 * config e.g (localhost or Kubernetes) should implement this interface.
 */
public interface ServingConfig {
 
  default Map<String, String> getEnvVars(Users user, boolean includeSecrets) throws ApiKeyException {
    return null;
  }
  
  String getClassName();
}
