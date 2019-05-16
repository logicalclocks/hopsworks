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
package io.hops.hopsworks.api.dataset;

import io.swagger.annotations.ApiParam;

import javax.ws.rs.BeanParam;
import javax.ws.rs.QueryParam;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Set;

public class DatasetBeanParam {
  
  @QueryParam("sort_by")
  @ApiParam(value = "ex. sort_by=id:asc,date_created:desc",
    allowableValues = "id:asc, id:desc")
  private String sortBy;
  private final Set<DatasetSortBy> sortBySet;
  private final Set<DatasetSharedWithSortBy> sharedWithSortBySet;
  @QueryParam("filter_by")
  @ApiParam(value =
    "ex. filter_by=id:1, filter_by=modification_time:date (allowed date formats yyyy-MM-dd'T'HH:mm:ss.SSSZ or " +
      "yyyy-MM-dd)",
    allowableValues = "id:1, id:2, id:3",
    allowMultiple = true)
  private Set<DatasetFilterBy> filter;
  private Set<DatasetSharedWithFilterBy> sharedWithFilter;
  @BeanParam
  private DatasetExpansionBeanParam expansions;
  
  public DatasetBeanParam(@QueryParam("sort_by") String sortBy, @QueryParam("filter_by") Set<DatasetFilterBy> filter,
    @BeanParam DatasetExpansionBeanParam expansions) {
    this.sortBy = sortBy;
    this.sortBySet = getSortBy(sortBy);
    this.sharedWithSortBySet = getSharedWithSortBy(sortBy);
    this.filter = filter;
    this.sharedWithFilter = getFilterBy(filter);
  }
  
  private Set<DatasetSharedWithFilterBy> getFilterBy(Set<DatasetFilterBy> filters) {
    Set<DatasetSharedWithFilterBy> sharedWithFilters = new HashSet<>();
    for (DatasetFilterBy filter : filters) {
      sharedWithFilters.add(new DatasetSharedWithFilterBy(filter));
    }
    return sharedWithFilters;
  }
  
  private Set<DatasetSharedWithSortBy> getSharedWithSortBy(String param) {
    if (param == null || param.isEmpty()) {
      return new LinkedHashSet<>();
    }
    String[] params = param.split(",");
    //Hash table and linked list implementation of the Set interface, with predictable iteration order
    Set<DatasetSharedWithSortBy> sortBys = new LinkedHashSet<>();
    DatasetSharedWithSortBy sort;
    for (String s : params) {
      sort = new DatasetSharedWithSortBy(s.trim());
      sortBys.add(sort);
    }
    return sortBys;
  }
  
  private Set<DatasetSortBy> getSortBy(String param) {
    if (param == null || param.isEmpty()) {
      return new LinkedHashSet<>();
    }
    String[] params = param.split(",");
    //Hash table and linked list implementation of the Set interface, with predictable iteration order
    Set<DatasetSortBy> sortBys = new LinkedHashSet<>();
    DatasetSortBy sort;
    for (String s : params) {
      sort = new DatasetSortBy(s.trim());
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
  
  public Set<DatasetFilterBy> getFilter() {
    return filter;
  }
  
  public void setFilter(Set<DatasetFilterBy> filter) {
    this.filter = filter;
  }
  
  public Set<DatasetSortBy> getSortBySet() {
    return sortBySet;
  }
  
  public Set<DatasetSharedWithSortBy> getSharedWithSortBySet() {
    return sharedWithSortBySet;
  }
  
  public Set<DatasetSharedWithFilterBy> getSharedWithFilter() {
    return sharedWithFilter;
  }
  
  public void setSharedWithFilter(Set<DatasetSharedWithFilterBy> sharedWithFilter) {
    this.sharedWithFilter = sharedWithFilter;
  }
  
  public DatasetExpansionBeanParam getExpansions() {
    return expansions;
  }
  
  public void setExpansions(DatasetExpansionBeanParam expansions) {
    this.expansions = expansions;
  }
}