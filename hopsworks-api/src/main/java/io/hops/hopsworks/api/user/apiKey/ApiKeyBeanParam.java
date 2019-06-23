/*
 * This file is part of Hopsworks
 * Copyright (C) 2019, Logical Clocks AB. All rights reserved
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
package io.hops.hopsworks.api.user.apiKey;

import io.swagger.annotations.ApiParam;

import javax.ws.rs.QueryParam;
import java.util.LinkedHashSet;
import java.util.Set;

public class ApiKeyBeanParam {
  
  @QueryParam("sort_by")
  @ApiParam(value = "ex. sort_by=Name:asc,created:desc,modified:desc",
    allowableValues = "name:asc, name:desc, created:asc, created:desc, modified:asc, modified:desc")
  private String sortBy;
  private final Set<ApiKeySortBy> sortBySet;
  @QueryParam("filter_by")
  @ApiParam(value = "ex. filter_by=name:key, created:2019-01-01T00:00:00.000, created_lt:2019-01-01-00:00:00, " +
    "created_gt:2019-01-01-00:00:00, modified:2019-01-01-00:00:00, modified_lt:2019-01-01-00:00:00, " +
    "modified_gt:2019-01-01-00:00:00",
    allowMultiple = true)
  private Set<ApiKeyFilterBy> filter;
  
  public ApiKeyBeanParam(@QueryParam("sort_by") String sortBy, @QueryParam("filter_by") Set<ApiKeyFilterBy> filter) {
    this.sortBy = sortBy;
    this.sortBySet = getSortBy(sortBy);
    this.filter = filter;
    
  }
  
  private Set<ApiKeySortBy> getSortBy(String param) {
    if (param == null || param.isEmpty()) {
      return new LinkedHashSet<>();
    }
    String[] params = param.split(",");
    //Hash table and linked list implementation of the Set interface, with predictable iteration order
    Set<ApiKeySortBy> sortBys = new LinkedHashSet<>();
    ApiKeySortBy sort;
    for (String s : params) {
      sort = new ApiKeySortBy(s.trim());
      sortBys.add(sort);
    }
    return sortBys;
  }
  
  public String getSortBy() {
    return sortBy;
  }
  
  public void setSortBy(String sortBy) {
    this.sortBy = sortBy;
  }
  
  public Set<ApiKeyFilterBy> getFilter() {
    return filter;
  }
  
  public void setFilter(Set<ApiKeyFilterBy> filter) {
    this.filter = filter;
  }
  
  public Set<ApiKeySortBy> getSortBySet() {
    return sortBySet;
  }
  
}