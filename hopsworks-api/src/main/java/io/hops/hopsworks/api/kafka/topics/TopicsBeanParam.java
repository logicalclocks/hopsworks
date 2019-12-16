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
package io.hops.hopsworks.api.kafka.topics;

import io.swagger.annotations.ApiParam;

import javax.ws.rs.QueryParam;
import java.util.LinkedHashSet;
import java.util.Set;

public class TopicsBeanParam {
  
  @QueryParam("sort_by")
  @ApiParam(value = "ex. sort_by=name:asc,schema_name:desc",
    allowableValues = "name:asc,name:desc,schema_name:asc,schema_name:desc")
  private String sortBy;
  private final Set<TopicsSortBy> sortBySet;
  @QueryParam("filter_by")
  @ApiParam(value = "ex. filter_by=shared:true",
    allowableValues = "filter_by=shared:true,filter_by=shared:false",
    allowMultiple = true)
  private Set<TopicsFilterBy> filter;
  
  public TopicsBeanParam(
    @QueryParam("sort_by") String sortBy,
    @QueryParam("filter_by") Set<TopicsFilterBy> filter) {
    this.sortBy = sortBy;
    this.sortBySet = getSortBy(sortBy);
    this.filter = filter;
  }
  
  private Set<TopicsSortBy> getSortBy(String param) {
    if (param == null || param.isEmpty()) {
      return new LinkedHashSet<>();
    }
    String[] params = param.split(",");
    //Hash table and linked list implementation of the Set interface, with predictable iteration order
    Set<TopicsSortBy> sortBys = new LinkedHashSet<>();//make ordered
    TopicsSortBy sort;
    for (String s : params) {
      sort = new TopicsSortBy(s.trim());
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
  
  public Set<TopicsFilterBy> getFilter() {
    return filter;
  }
  
  public void setFilter(Set<TopicsFilterBy> filter) {
    this.filter = filter;
  }
  
  public Set<TopicsSortBy> getSortBySet() {
    return sortBySet;
  }
  
  @Override
  public String toString() {
    return "TopicsBeanParam=[sort_by="+sortBy+", filter_by="+filter+"]";
  }
}
