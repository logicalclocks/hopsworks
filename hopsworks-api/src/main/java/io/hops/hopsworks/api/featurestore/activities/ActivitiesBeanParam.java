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

package io.hops.hopsworks.api.featurestore.activities;

import com.google.common.base.Strings;
import io.swagger.annotations.ApiParam;

import javax.ws.rs.BeanParam;
import javax.ws.rs.QueryParam;
import java.util.LinkedHashSet;
import java.util.Set;

public class ActivitiesBeanParam {
  @QueryParam("sort_by")
  @ApiParam(value = "ex. sort_by=time:desc", allowableValues = "time:asc,time:desc")
  private String sortBy;
  private Set<SortBy> sortBySet;

  @QueryParam("filter_by")
  @ApiParam(value = "ex. filter_by=type:job", allowMultiple = true)
  private Set<FilterBy> filterBySet;

  @BeanParam
  private ExpansionBeanParam expansions;

  public ActivitiesBeanParam(@QueryParam("sort_by") String sortBy,
                             @QueryParam("filter_by") Set<FilterBy> filterBySet) {
    this.sortBy = sortBy;
    this.sortBySet = getSortBy(sortBy);
    this.filterBySet = filterBySet;
  }

  private Set<SortBy> getSortBy(String param) {
    Set<SortBy> sortBys = new LinkedHashSet<>();
    if (Strings.isNullOrEmpty(param)) {
      return sortBys;
    }

    String[] params = param.split(",");
    for (String s : params) {
      sortBys.add(new SortBy(s.trim()));
    }

    return sortBys;
  }

  public Set<FilterBy> getFilterBySet() {
    return filterBySet;
  }

  public void setFilterBySet(Set<FilterBy> filterBySet) {
    this.filterBySet = filterBySet;
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

  public void setSortBySet(Set<SortBy> sortBySet) {
    this.sortBySet = sortBySet;
  }

  public ExpansionBeanParam getExpansions() {
    return expansions;
  }

  public void setExpansions(ExpansionBeanParam expansions) {
    this.expansions = expansions;
  }
}
