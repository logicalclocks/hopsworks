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
package io.hops.hopsworks.api.python.environment;

import io.hops.hopsworks.common.api.ResourceRequest;
import io.swagger.annotations.ApiParam;
import java.util.HashSet;
import java.util.Set;
import javax.ws.rs.QueryParam;


public class EnvironmentExpansionBeanParam {
  @QueryParam("expand")
  @ApiParam(value = "ex. expand=commands", allowableValues = "commands, libraries, conflicts")
  private Set<EnvironmentExpansions> expansions;

  public EnvironmentExpansionBeanParam(@QueryParam("expand") Set<EnvironmentExpansions> expansions) {
    this.expansions = expansions;
  }
  
  public Set<EnvironmentExpansions> getExpansions() {
    return expansions;
  }
  
  public void setExpansions(Set<EnvironmentExpansions> expansions) {
    this.expansions = expansions;
  }
  
  public Set<ResourceRequest> getResources(){
    Set<ResourceRequest> expansions = new HashSet<>();
    for(EnvironmentExpansions environmentExpansions : this.expansions){
      if (environmentExpansions != null) {
        expansions.add(environmentExpansions.getResourceRequest());
      }
    }
    return expansions;
  }
}