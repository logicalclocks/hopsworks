/*
 * This file is part of Hopsworks
 * Copyright (C) 2018, Logical Clocks AB. All rights reserved
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
package io.hops.hopsworks.api.activities;

import io.hops.hopsworks.api.user.UserResource;
import io.hops.hopsworks.common.api.Expansions;
import io.hops.hopsworks.common.api.Resource;

public class ActivityExpansions implements Expansions {
  private Resource resource;
  
  public ActivityExpansions(String queryParam) {
    Resource.Name name;
    //Get name of resource
    if (queryParam.contains("(")) {
      name = Resource.Name.valueOf(queryParam.substring(0, queryParam.indexOf('(')).toUpperCase());
    } else {
      name = Resource.Name.valueOf(queryParam.toUpperCase());
    }
    
    switch (name) {
      case USER:
        resource = new UserResource(name, queryParam);
        break;
      default:
        break;
    }
  }
  @Override
  public Resource getResource() {
    return resource;
  }
  
  @Override
  public void setResource(Resource resource) {
    this.resource = resource;
  }
}
