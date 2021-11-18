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

package io.hops.hopsworks.api.modelregistry;

import io.hops.hopsworks.common.api.ResourceRequest;
import io.swagger.annotations.ApiParam;

import javax.ws.rs.QueryParam;
import java.util.HashSet;
import java.util.Set;

public class ModelRegistryExpansionBeanParam {
  @QueryParam("expand")
  @ApiParam(value = "ex. expand=models", allowableValues = "expand=models")
  private Set<ModelRegistryExpansions> expansions;

  public ModelRegistryExpansionBeanParam(@QueryParam("expand") Set<ModelRegistryExpansions> expansions) {
    this.expansions = expansions;
  }

  public Set<ModelRegistryExpansions> getExpansions() {
    return expansions;
  }

  public void setExpansions(Set<ModelRegistryExpansions> expansions) {
    this.expansions = expansions;
  }

  public Set<ResourceRequest> getResources(){
    Set<ResourceRequest> expansions = new HashSet<>();
    for(ModelRegistryExpansions modelExpansions : this.expansions){
      expansions.add(modelExpansions.getResourceRequest());
    }
    return expansions;
  }
}