/*
 * This file is part of Hopsworks
 * Copyright (C) 2022, Hopsworks AB. All rights reserved
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

package io.hops.hopsworks.api.featurestore.datavalidationv2.results;

import io.swagger.annotations.ApiParam;

import javax.ws.rs.QueryParam;
import java.util.LinkedHashSet;
import java.util.Set;

public class ValidationResultBeanParam {
  @QueryParam("sort_by")
  @ApiParam(value = "ex. sort_by=created:desc",
    allowableValues = "created:asc,created:desc,")
  private String sortBy;
  private final Set<ValidationResultSortBy> sortBySet;

  @QueryParam("filter_by")
  @ApiParam(value = "ex. filter_by=created_lt:1610471222000",
    allowableValues = "filter_by=created_lt:1610471222000,filter_by=created_gt:1610471222000",
    allowMultiple = true)
  private Set<ValidationResultFilterBy> filter;

  public ValidationResultBeanParam(
    @QueryParam("sort_by")
      String sortBy,
    @QueryParam("filter_by")
      Set<ValidationResultFilterBy> filter) {
    this.sortBy = sortBy;
    this.sortBySet = getSortBy(sortBy);
    this.filter = filter;
  }

  private Set<ValidationResultSortBy> getSortBy(String param) {
    if (param == null || param.isEmpty()) {
      return new LinkedHashSet<>();
    }
    String[] params = param.split(",");
    //Hash table and linked list implementation of the Set interface, with predictable iteration order
    Set<ValidationResultSortBy> sortBys = new LinkedHashSet<>();//make ordered
    ValidationResultSortBy sort;
    for (String s : params) {
      sort = new ValidationResultSortBy(s.trim());
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

  public Set<ValidationResultSortBy> getSortBySet() {
    return sortBySet;
  }

  public Set<ValidationResultFilterBy> getFilter() {
    return filter;
  }

  public void setFilter(Set<ValidationResultFilterBy> filter) {
    this.filter = filter;
  }
}