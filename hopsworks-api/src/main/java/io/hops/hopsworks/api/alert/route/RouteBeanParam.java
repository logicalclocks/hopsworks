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

package io.hops.hopsworks.api.alert.route;

import io.swagger.annotations.ApiParam;

import javax.ws.rs.QueryParam;
import java.util.LinkedHashSet;
import java.util.Set;

public class RouteBeanParam {

  @QueryParam("sort_by")
  @ApiParam(value = "ex. sort_by=receiver:asc,receiver:desc",
      allowableValues = "RECEIVER:asc, RECEIVER:desc")
  private String sortBy;
  private final Set<RouteSortBy> sortBySet;

  public RouteBeanParam(@QueryParam("sort_by") String sortBy) {
    this.sortBy = sortBy;
    this.sortBySet = getSortBy(sortBy);
  }

  private Set<RouteSortBy> getSortBy(String param) {
    if (param == null || param.isEmpty()) {
      return new LinkedHashSet<>();
    }
    String[] params = param.split(",");
    //Hash table and linked list implementation of the Set interface, with predictable iteration order
    Set<RouteSortBy> sortBys = new LinkedHashSet<>();
    RouteSortBy sort;
    for (String s : params) {
      sort = new RouteSortBy(s.trim());
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

  public Set<RouteSortBy> getSortBySet() {
    return sortBySet;
  }

}