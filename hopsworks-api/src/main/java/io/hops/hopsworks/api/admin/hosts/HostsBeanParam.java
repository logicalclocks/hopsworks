/*
 * This file is part of Hopsworks
 * Copyright (C) 2020, Logical Clocks AB. All rights reserved
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
package io.hops.hopsworks.api.admin.hosts;

import io.swagger.annotations.ApiParam;

import javax.ws.rs.QueryParam;
import java.util.LinkedHashSet;
import java.util.Set;


public class HostsBeanParam {
  
  @QueryParam("sort_by")
  @ApiParam(value = "ex. sort_by=id:desc,hostname:asc",
    allowableValues = "id:asc,id:desc,hostname:asc,hostname:desc,host_ip:asc,host_ip:desc," +
      "public_ip:asc,public_ip:desc,private_ip:asc,private_ip:desc,cores:asc,cores:desc,num_gpus:asc," +
      "num_gpus:desc,memory_capacity:asc,memory_capacity:desc")
  private String sortBy;
  private final Set<SortBy> sortBySet;
  @QueryParam("filter_by")
  @ApiParam(value = "ex. filter_by=hostname:hopsworks0&registered:true",
    allowableValues = "filter_by=hostname:hopsworks0.com,filter_by=host_ip:192.168.1.1," +
      "filter_by=public_ip:192.168.1.1,filter_by=private_ip:192.168.1.1,filter_by=registered:true," +
      "filter_by=conda_enabled:true",
    allowMultiple = true)
  private Set<FilterBy> filter;

  
  public HostsBeanParam(
    @QueryParam("sort_by")
      String sortBy,
    @QueryParam("filter_by")
      Set<FilterBy> filter) {
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
  
  @Override
  public String toString() {
    return "HostsBeanParam=[sortBy=" + sortBy + ", filter_by=" + filter + "]";
  }
}
