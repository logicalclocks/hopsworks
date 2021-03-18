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
package io.hops.hopsworks.api.provenance.app;

import io.swagger.annotations.ApiParam;

import javax.ws.rs.QueryParam;
import java.util.List;
import java.util.Set;

public class ProvAppBeanParams {
  @QueryParam("filter_by")
  @ApiParam(allowMultiple = true)
  private Set<String> filterBy;
  @QueryParam("sort_by")
  @ApiParam(allowMultiple = true)
  private List<String> sortBy;
  
  public ProvAppBeanParams(@QueryParam("filter_by") Set<String> filterBy,
    @QueryParam("sort_by") List<String> sortBy) {
    this.filterBy = filterBy;
    this.sortBy = sortBy;
  }
  
  public ProvAppBeanParams() {}
  
  public Set<String> getFilterBy() {
    return filterBy;
  }
  
  public void setFilterBy(Set<String> filterBy) {
    this.filterBy = filterBy;
  }
  
  public List<String> getSortBy() {
    return sortBy;
  }
  
  public void setSortBy(List<String> sortBy) {
    this.sortBy = sortBy;
  }
}
