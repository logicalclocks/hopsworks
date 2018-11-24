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
package io.hops.hopsworks.api.jobs.executions;

import io.hops.hopsworks.common.dao.jobhistory.ExecutionFacade;
import io.swagger.annotations.ApiParam;

import javax.ws.rs.QueryParam;
import java.util.LinkedHashSet;
import java.util.Set;

public class ExecutionsBeanParam {
  @QueryParam("sort_by")
  @ApiParam(value = "ex. sort_by=submission_time:desc,id:asc",
    allowableValues = "id:asc,id:desc,name:asc,name:desc,date_created:asc,date_created:desc")
  private String sortBy;
  private final Set<ExecutionFacade.SortBy> sortBySet;
  @QueryParam("filter_by")
  @ApiParam(value = "ex. filter_by=state:running,finished")
  private Set<ExecutionFacade.FilterBy> filter;
  
  public ExecutionsBeanParam(
    @QueryParam("sort_by") String sortBy,
    @QueryParam("filter_by") Set<ExecutionFacade.FilterBy> filter) {
    this.sortBy = sortBy;
    this.sortBySet = getSortBy(sortBy);
    this.filter = filter;
  }
  
  private Set<ExecutionFacade.SortBy> getSortBy(String param) {
    if (param == null || param.isEmpty()) {
      return null;
    }
    String[] params = param.split(",");
    //Hash table and linked list implementation of the Set interface, with predictable iteration order
    Set<ExecutionFacade.SortBy> sortBys = new LinkedHashSet<>();//make ordered
    ExecutionFacade.SortBy sort;
    for (String s : params) {
      sort = ExecutionFacade.SortBy.fromString(s.trim());
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
  
  public Set<ExecutionFacade.FilterBy> getFilter() {
    return filter;
  }
  
  public void setFilter(Set<ExecutionFacade.FilterBy> filter) {
    this.filter = filter;
  }
  
  public Set<ExecutionFacade.SortBy> getSortBySet() {
    return sortBySet;
  }
}
