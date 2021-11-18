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

package io.hops.hopsworks.api.modelregistry.models;

import io.swagger.annotations.ApiParam;

import javax.ws.rs.BeanParam;
import javax.ws.rs.QueryParam;
import java.util.LinkedHashSet;
import java.util.Set;

public class ModelsBeanParam {
  @QueryParam("sort_by")
  @ApiParam(value = "ex. sort_by=name:desc or sort_by=accuracy:desc",
      allowableValues = "name:desc,name:asc")
  private String sortBy;
  private final Set<SortBy> sortBySet;
  @QueryParam("filter_by")
  @ApiParam(value = "ex. filter_by=name_eq:mnist&filter_by:version:1",
      allowableValues = "name_eq:mnist, name_like:mnist, version:1, id_eq:mnist_1, user:1234, user_project:1234",
      allowMultiple = true)
  private Set<FilterBy> filter;
  @BeanParam
  private ModelExpansionBeanParam expansions;

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

  public ModelsBeanParam(@QueryParam("filter_by") Set<FilterBy> filter, @QueryParam("sort_by") String sortBy) {
    this.filter = filter;
    this.sortBy = sortBy;
    sortBySet = getSortBy(sortBy);
  }

  public Set<FilterBy> getFilter() {
    return filter;
  }

  public void setFilter(Set<FilterBy> filter) {
    this.filter = filter;
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

  public ModelExpansionBeanParam getExpansions() {
    return expansions;
  }

  public void setExpansions(ModelExpansionBeanParam expansions) {
    this.expansions = expansions;
  }

}
