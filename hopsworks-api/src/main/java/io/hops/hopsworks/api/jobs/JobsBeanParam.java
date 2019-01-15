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

import io.swagger.annotations.ApiParam;

import javax.ws.rs.BeanParam;
import javax.ws.rs.QueryParam;
import java.util.LinkedHashSet;
import java.util.Set;

public class JobsBeanParam {
  
  @QueryParam("sort_by")
  @ApiParam(value = "ex. sort_by=date_created:desc,name:asc",
    allowableValues = "id:asc,id:desc,name:asc,name:desc,date_created:asc,date_created:desc,jobtype:asc," +
      "jobtype:desc,creator:asc,creator:desc,creator_firstname:asc,creator_firstname:desc," +
      "creator_lastname:asc,creator_lastname:desc,state:asc,state:desc,finalstatus:asc,finalstatus:desc,progress:asc," +
      "progress:desc,submissiontime:asc,submissiontime:desc,duration:asc,duration:desc")
  private String sortBy;
  private final Set<SortBy> sortBySet;
  @QueryParam("filter_by")
  @ApiParam(value = "ex. filter_by=jobtype:spark&filter_by=date_created_gt:2018-12-25T17:12:10",
    allowableValues = "jobtype:spark, jobtype:pyspark, jobtype:flink,jobtype_neq:spark, jobtype_neq:pyspark, " +
      "jobtype_neq:flink,filter_by=date_created:2018-12-25T17:12:10.058, " +
      "filter_by=date_created_gt:2018-12-25T17:12:10.058, filter_by=date_created_lt:2018-12-25T17:12:10.058, " +
      "filter_by=name:myetl, filter_by=creator:john, filter_by=latest_execution:finished",
    allowMultiple = true)
  private Set<FilterBy> filter;
  @BeanParam
  private ExpansionBeanParam expansions;
  
  public JobsBeanParam(
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
