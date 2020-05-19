/*
 * Copyright (C) 2020, Logical Clocks AB. All rights reserved
 */
package io.hops.hopsworks.api.tags;

import io.swagger.annotations.ApiParam;

import javax.ws.rs.QueryParam;
import java.util.LinkedHashSet;
import java.util.Set;

public class TagsBeanParam {
  
  @QueryParam("sort_by")
  @ApiParam(value = "ex. sort_by=id:desc,type:asc",
    allowableValues = "id:asc,id:desc,value:asc,value:desc,type:asc,type:desc,")
  private String sortBy;
  private final Set<SortBy> sortBySet;
  @QueryParam("filter_by")
  @ApiParam(value = "ex. filter_by=name:foo", allowMultiple = true)
  private Set<FilterBy> filter;

  
  public TagsBeanParam(@QueryParam("sort_by") String sortBy, @QueryParam("filter_by") Set<FilterBy> filter) {
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
  
  public Set<SortBy> getSortBySet() {
    return sortBySet;
  }
  
  public Set<FilterBy> getFilter() {
    return filter;
  }
  
  public void setFilter(Set<FilterBy> filter) {
    this.filter = filter;
  }
}
