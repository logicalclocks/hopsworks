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
package io.hops.hopsworks.api.python.library;

import io.swagger.annotations.ApiParam;
import java.util.LinkedHashSet;
import java.util.Set;
import javax.ws.rs.BeanParam;
import javax.ws.rs.QueryParam;

public class LibrariesBeanParam {

  @QueryParam("sort_by")
  @ApiParam(value = "ex. sort_by=ID:asc,dependency:desc",
      allowableValues = "id:asc, id:desc, dependency:asc, dependency:desc, status:asc, status:desc")
  private String sortBy;
  private final Set<SortBy> sortBySet;
  @QueryParam("filter_by")
  @ApiParam(value = "ex. filter_by=preinstalled:1",
      allowableValues = "preinstalled:1, preinstalled:0, status:new, status:success, status:ongoing, status:failed",
      allowMultiple = true)
  private Set<FilterBy> filter;
  @BeanParam
  private LibraryExpansionBeanParam expansions;

  public LibrariesBeanParam(
      @QueryParam("sort_by") String sortBy,
      @QueryParam("filter_by") Set<FilterBy> filter,
      @BeanParam LibraryExpansionBeanParam expansions) {
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

  public LibraryExpansionBeanParam getExpansions() {
    return expansions;
  }

  public void setExpansions(LibraryExpansionBeanParam expansions) {
    this.expansions = expansions;
  }
}
