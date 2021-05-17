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

package io.hops.hopsworks.api.alert.receiver;

import io.swagger.annotations.ApiParam;

import javax.ws.rs.QueryParam;
import java.util.LinkedHashSet;
import java.util.Set;

public class ReceiverBeanParam {
  
  @QueryParam("sort_by")
  @ApiParam(value = "ex. sort_by=name:asc,name:desc",
      allowableValues = "NAME:asc, NAME:desc")
  private String sortBy;
  private final Set<ReceiverSortBy> sortBySet;
  
  public ReceiverBeanParam(@QueryParam("sort_by") String sortBy) {
    this.sortBy = sortBy;
    this.sortBySet = getSortBy(sortBy);
  }
  
  private Set<ReceiverSortBy> getSortBy(String param) {
    if (param == null || param.isEmpty()) {
      return new LinkedHashSet<>();
    }
    String[] params = param.split(",");
    //Hash table and linked list implementation of the Set interface, with predictable iteration order
    Set<ReceiverSortBy> sortBys = new LinkedHashSet<>();
    ReceiverSortBy sort;
    for (String s : params) {
      sort = new ReceiverSortBy(s.trim());
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
  
  public Set<ReceiverSortBy> getSortBySet() {
    return sortBySet;
  }
  
}