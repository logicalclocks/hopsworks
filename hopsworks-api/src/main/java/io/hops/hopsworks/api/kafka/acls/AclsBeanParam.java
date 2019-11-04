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

package io.hops.hopsworks.api.kafka.acls;

import javax.ws.rs.QueryParam;
import java.util.LinkedHashSet;
import java.util.Set;

public class AclsBeanParam {
  
  private String sortBy;
  private final Set<AclsSortBy> sortBySet;
  private Set<AclsFilterBy> filter;
  
  public AclsBeanParam(
    @QueryParam("sort_by") String sortBy,
    @QueryParam("filter_by") Set<AclsFilterBy> filter) {
    this.sortBy = sortBy;
    this.sortBySet = getSortBy(sortBy);
    this.filter = filter;
  }
  
  private Set<AclsSortBy> getSortBy(String param) {
    if (param == null || param.isEmpty()) {
      return new LinkedHashSet<>();
    }
    String[] params = param.split(",");
    //Hash table and linked list implementation of the Set interface, with predictable iteration order
    Set<AclsSortBy> sortBys = new LinkedHashSet<>();//make ordered
    AclsSortBy sort;
    for (String s : params) {
      sort = new AclsSortBy(s.trim());
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
  
  public Set<AclsSortBy> getSortBySet() {
    return sortBySet;
  }
  
  public Set<AclsFilterBy> getFilter() {
    return filter;
  }
  
  public void setFilter(Set<AclsFilterBy> filter) {
    this.filter = filter;
  }
  
  @Override
  public String toString() {
    return "AclsBeanParam=[sort_by="+sortBy+", filter_by="+filter+"]";
  }
}
