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

package io.hops.hopsworks.api.alert.silence;

import io.swagger.annotations.ApiParam;

import javax.ws.rs.BeanParam;
import javax.ws.rs.QueryParam;
import java.util.LinkedHashSet;
import java.util.Set;

public class SilenceBeanParam {

  @QueryParam("sort_by")
  @ApiParam(value = "ex. sort_by=updatedAt:asc,updatedAt:desc",
      allowableValues = "CREATED_BY:asc, CREATED_BY:desc, UPDATED_AT:asc, UPDATED_AT:desc, STARTS_AT:asc, " +
          "STARTS_AT:desc, ENDS_AT:asc," +
          " ENDS_AT:desc")
  private String sortBy;
  private final Set<SilenceSortBy> sortBySet;
  @BeanParam
  private SilenceFilterBy silenceFilterBy;

  public SilenceBeanParam(@QueryParam("sort_by") String sortBy, @BeanParam SilenceFilterBy silenceFilterBy) {
    this.sortBy = sortBy;
    this.sortBySet = getSortBy(sortBy);
    this.silenceFilterBy = silenceFilterBy == null ? new SilenceFilterBy(null, null, null) : silenceFilterBy;
  }

  private Set<SilenceSortBy> getSortBy(String param) {
    if (param == null || param.isEmpty()) {
      return new LinkedHashSet<>();
    }
    String[] params = param.split(",");
    //Hash table and linked list implementation of the Set interface, with predictable iteration order
    Set<SilenceSortBy> sortBys = new LinkedHashSet<>();//make ordered
    SilenceSortBy sort;
    for (String s : params) {
      sort = new SilenceSortBy(s.trim());
      sortBys.add(sort);
    }
    return sortBys;
  }

  public Set<SilenceSortBy> getSortBySet() {
    return sortBySet;
  }

  public SilenceFilterBy getSilenceFilterBy() {
    return silenceFilterBy;
  }
}