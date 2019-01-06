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

import io.swagger.annotations.ApiParam;

import javax.ws.rs.BeanParam;
import javax.ws.rs.QueryParam;
import java.util.LinkedHashSet;
import java.util.Set;

public class ExecutionsBeanParam {
  @QueryParam("sort_by")
  @ApiParam(value = "ex. sort_by=submissiontime:desc,id:asc",
    allowableValues = "id:asc,id:desc,name,submissiontime:asc,submissiontime:desc,state:asc,state:desc," +
      "finalstatus:asc,finalstatus:desc,appId:asc,appId:desc,progress:asc,progress:desc,duration:asc,duration:desc")
  private String sortBy;
  private final Set<SortBy> sortBySet;
  @QueryParam("filter_by")
  @ApiParam(value = "state and finalstatus accept also neq (not equals) ex. " +
    "filter_by=state:running&filter_by=state_neq:new&filter_by=submissiontime:2018-12-25T17:12:10.058",
    allowableValues = "state:initializing, state:initilization_failed, state:finished, state:running, " +
      "state:accepted, state:failed, state:killed, state:new, state:new_saving, state:submitted, " +
      "state:aggregating_logs, state:framework_failure, state:starting_app_master, state:app_master_start_failed," +
      "finalstatus:undefined, finalstatus:succeeded, finalstatus:failed, finalstatus:killed, " +
      "submissiontime:2018-12-25T17:12:10.058, submissiontime_gt:2018-12-25T17:12:10.058, " +
      "submissiontime_lt:2018-12-25T17:12:10.058")
  private Set<FilterBy> filter;
  @BeanParam
  private ExpansionBeanParam expansions;
  
  public ExecutionsBeanParam(
    @QueryParam("sort_by") String sortBy,
    @QueryParam("filter_by") Set<FilterBy> filter) {
    this.sortBy = sortBy;
    this.sortBySet = getSortBy(sortBy);
    this.filter = filter;
  }
  
  private Set<SortBy> getSortBy(String param) {
    if (param == null || param.isEmpty()) {
      return new LinkedHashSet<>();
    }
    String[] params = param.split(",");
    //Hash table and linked list implementation of the Set interface, with predictable iteration order
    Set<SortBy> sortBys = new LinkedHashSet<>();//make ordered
    SortBy sort;
    for (String s : params) {
      sort = new SortBy(s.trim());
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
  
  public Set<FilterBy> getFilter() {
    return filter;
  }
  
  public void setFilter(Set<FilterBy> filter) {
    this.filter = filter;
  }
  
  public Set<SortBy> getSortBySet() {
    return sortBySet;
  }
  
  public ExpansionBeanParam getExpansions() {
    return expansions;
  }
  
  public void setExpansions(ExpansionBeanParam expansions) {
    this.expansions = expansions;
  }
  
}
