/*
 * Copyright (C) 2020, Logical Clocks AB. All rights reserved
 */
package io.hops.hopsworks.api.admin.cloud;

import io.swagger.annotations.ApiParam;

import javax.ws.rs.QueryParam;
import java.util.LinkedHashSet;
import java.util.Set;

public class CloudRoleMappingBeanParam {
  @QueryParam("sort_by")
  @ApiParam(value = "ex. sort_by=id:asc,project_role:desc",
    allowableValues = "id:asc,id:desc,project_id:asc,project_id:desc,project_role:asc,project_role:desc"
      + ",cloud_role:asc,cloud_role:desc")
  private String sortBy;
  private final Set<SortBy> sortBySet;
  @QueryParam("filter_by")
  @ApiParam(value = "ex. filter_by=id:1&filter_by=project_id:2", allowMultiple = true)
  private Set<FilterBy> filter;
  
  public CloudRoleMappingBeanParam(
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
  
}
