/*
 * This file is part of Hopsworks
 * Copyright (C) 2023, Hopsworks AB. All rights reserved
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
package io.hops.hopsworks.api.python.environment.history;

import io.swagger.annotations.ApiParam;

import javax.ws.rs.BeanParam;
import javax.ws.rs.QueryParam;
import java.util.LinkedHashSet;
import java.util.Set;

public class EnvironmentHistoryBeanParam {
  @QueryParam("sort_by")
  @ApiParam(value = "ex. sort_by=id:desc", allowableValues = "id:asc,id:desc")
  private String sortBy;

  private final Set<SortBy> sortBySet;

  @QueryParam("filter_by")
  @ApiParam(value = "ex. filter_by=library:myetl",
      allowableValues = "filter_by=library:myetl,date_from:2018-12-25T17:12:10.058,date_to:2018-12-25T17:12:10.058",
      allowMultiple = true)
  private Set<FilterBy> filter;

  @BeanParam
  private EnvironmentHistoryExpansionsBeanParam expansions;

  public EnvironmentHistoryBeanParam( @QueryParam("sort_by") String sortBy,
                              @QueryParam("filter_by") Set<FilterBy> filter) {
    this.sortBy = sortBy;
    this.filter = filter;
    this.sortBySet = getSortBy(sortBy);
  }

  private Set<SortBy> getSortBy(String param) {
    if (param == null || param.isEmpty()) {
      return new LinkedHashSet<>();
    }
    String[] params = param.split(",");
    Set<SortBy> sortBys = new LinkedHashSet<>();
    SortBy sort;
    for (String s : params) {
      sort = new SortBy(s.trim());
      sortBys.add(sort);
    }
    return sortBys;
  }

  public String getSortBy() { return sortBy; }

  public void setSortBy(String sortBy) { this.sortBy = sortBy; }

  public Set<FilterBy> getFilter() { return filter; }

  public void setFilter(Set<FilterBy> filter) { this.filter = filter; }

  public Set<SortBy> getSortBySet() { return sortBySet; }

  public EnvironmentHistoryExpansionsBeanParam getExpansions() { return expansions; }

  public void setExpansions(EnvironmentHistoryExpansionsBeanParam expansions) { this.expansions = expansions; }
}
