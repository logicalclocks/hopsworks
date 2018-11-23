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
package io.hops.hopsworks.api.jobs;

import io.hops.hopsworks.common.dao.jobs.description.JobFacade;
import io.swagger.annotations.ApiParam;

import javax.ws.rs.QueryParam;
import java.util.LinkedHashSet;
import java.util.Set;

public class JobsBeanParam {
  
  @QueryParam("sort_by")
  @ApiParam(value = "ex. sort_by=date_created:desc,name:asc",
    allowableValues = "id:asc,id:desc,name:asc,name:desc,date_created:asc,date_created:desc")
  private String sortBy;
  private final Set<JobFacade.SortBy> sortBySet;
  @QueryParam("filter_by")
  @ApiParam(value = "ex. filter_by=type:spark,pyspark")
  private Set<JobFacade.FilterBy> filter;
//  @QueryParam("expand")
//  @ApiParam(value = "ex. expand=creator")
//  private Set<JobFacade.JobExpansion> expand;
  
  public JobsBeanParam(
    @QueryParam("sort_by") String sortBy,
    @QueryParam("filter_by") Set<JobFacade.FilterBy> filter,
    @QueryParam("expand") Set<JobFacade.JobExpansion> expand) {
    this.sortBy = sortBy;
    this.sortBySet = getSortBy(sortBy);
    this.filter = filter;
//    this.expand = expand;
  }
  
  private Set<JobFacade.SortBy> getSortBy(String param) {
    if (param == null || param.isEmpty()) {
      return null;
    }
    String[] params = param.split(",");
    //Hash table and linked list implementation of the Set interface, with predictable iteration order
    Set<JobFacade.SortBy> sortBys = new LinkedHashSet<>();//make ordered
    JobFacade.SortBy sort;
    for (String s : params) {
      sort = JobFacade.SortBy.fromString(s.trim());
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
  
  public Set<JobFacade.FilterBy> getFilter() {
    return filter;
  }
  
  public void setFilter(Set<JobFacade.FilterBy> filter) {
    this.filter = filter;
  }
  
  public Set<JobFacade.SortBy> getSortBySet() {
    return sortBySet;
  }
  
//  public Set<JobFacade.JobExpansion> getExpand() {
//    return expand;
//  }
//
//  public void setExpand(Set<JobFacade.JobExpansion> expand) {
//    this.expand = expand;
//  }
}
