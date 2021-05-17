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
package io.hops.hopsworks.api.featurestore.datavalidation.alert;

import io.swagger.annotations.ApiParam;

import javax.ws.rs.QueryParam;
import java.util.LinkedHashSet;
import java.util.Set;

public class FeatureGroupAlertBeanParam {
  
  @QueryParam("sort_by")
  @ApiParam(value = "ex. sort_by=ID:asc,date_created:desc")
  private String sortBy;
  private final Set<FeatureGroupAlertSortBy> sortBySet;
  @QueryParam("filter_by")
  @ApiParam(value = "ex. filter_by=id:1", allowMultiple = true)
  private Set<FeatureGroupAlertFilterBy> filter;
  
  public FeatureGroupAlertBeanParam(
      @QueryParam("sort_by") String sortBy,
      @QueryParam("filter_by") Set<FeatureGroupAlertFilterBy> filter) {
    this.sortBy = sortBy;
    this.sortBySet = getSortBy(sortBy);
    this.filter = filter;
    
  }
  
  private Set<FeatureGroupAlertSortBy> getSortBy(String param) {
    if (param == null || param.isEmpty()) {
      return new LinkedHashSet<>();
    }
    String[] params = param.split(",");
    //Hash table and linked list implementation of the Set interface, with predictable iteration order
    Set<FeatureGroupAlertSortBy> sortBys = new LinkedHashSet<>();
    FeatureGroupAlertSortBy sort;
    for (String s : params) {
      sort = new FeatureGroupAlertSortBy(s.trim());
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
  
  public Set<FeatureGroupAlertFilterBy> getFilter() {
    return filter;
  }
  
  public void setFilter(Set<FeatureGroupAlertFilterBy> filter) {
    this.filter = filter;
  }
  
  public Set<FeatureGroupAlertSortBy> getSortBySet() {
    return sortBySet;
  }
  
}