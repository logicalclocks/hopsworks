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

package io.hops.hopsworks.api.featurestore.activities;

import io.hops.hopsworks.common.api.ResourceRequest;
import io.swagger.annotations.ApiParam;

import javax.ws.rs.QueryParam;
import java.util.Set;
import java.util.stream.Collectors;

public class ExpansionBeanParam {
  @QueryParam("expand")
  @ApiParam(value = "ex. expand=executions",
      allowableValues = "expand=users,expand=executions,expand=jobs,expand=statistics,expand=commits")
  private Set<ActivityExpansions> expansions;

  public ExpansionBeanParam(@QueryParam("expand") Set<ActivityExpansions> expansions) {
    this.expansions = expansions;
  }

  public Set<ActivityExpansions> getExpansions() {
    return expansions;
  }

  public void setExpansions(Set<ActivityExpansions> expansions) {
    this.expansions = expansions;
  }

  public Set<ResourceRequest> getResources() {
    return expansions.stream().map(ActivityExpansions::getResourceRequest).collect(Collectors.toSet());
  }
}
