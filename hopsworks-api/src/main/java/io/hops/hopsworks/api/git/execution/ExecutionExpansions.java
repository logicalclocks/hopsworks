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
package io.hops.hopsworks.api.git.execution;

import io.hops.hopsworks.api.git.repository.RepositoryResourceRequest;
import io.hops.hopsworks.api.user.UserResourceRequest;
import io.hops.hopsworks.common.api.Expansions;
import io.hops.hopsworks.common.api.ResourceRequest;

public class ExecutionExpansions implements Expansions {
  private ResourceRequest resourceRequest;
  
  public ExecutionExpansions(String queryParam) {
    ResourceRequest.Name name;
    //Get name of resource
    if (queryParam.contains("(")) {
      name = ResourceRequest.Name.valueOf(queryParam.substring(0, queryParam.indexOf('(')).toUpperCase());
    } else {
      name = ResourceRequest.Name.valueOf(queryParam.toUpperCase());
    }
    
    switch (name) {
      case USER:
        resourceRequest = new UserResourceRequest(name, queryParam);
        break;
      case REPOSITORY:
        resourceRequest = new RepositoryResourceRequest(name, queryParam);
        break;
      default:
        break;
    }
  }
  @Override
  public ResourceRequest getResourceRequest() {
    return resourceRequest;
  }
  
  @Override
  public void setResourceRequest(ResourceRequest resourceRequest) {
    this.resourceRequest = resourceRequest;
  }
}
