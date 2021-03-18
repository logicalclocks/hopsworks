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

package io.hops.hopsworks.api.featurestore.statistics;

import com.google.common.base.Strings;
import io.swagger.annotations.ApiParam;

import javax.ws.rs.QueryParam;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.stream.Collectors;

public class StatisticsBeanParam {
  @QueryParam("sort_by")
  @ApiParam(value = "ex. sort_by=commit_time:desc", allowableValues = "commit_time:asc,commit_time:desc")
  private String sortBy;
  private final Set<SortBy> sortBySet;

  @QueryParam("filter_by")
  @ApiParam(value = "ex. filter_by=commit_time_gt:2018-12-25T17:12:10", allowMultiple = true)
  private Set<FilterBy> filterSet;

  // TODO(Fabio): No Expansion but field selection - reflection
  @QueryParam("fields")
  @ApiParam(value = "ex. fields=content", allowableValues = "fields=content")
  private String fields;
  private Set<String> fieldSet = new HashSet<>();

  public StatisticsBeanParam(
      @QueryParam("sort_by") String sortBy,
      @QueryParam("filter_by") Set<FilterBy> filter,
      @QueryParam("fields") String fields) {
    this.sortBy = sortBy;
    this.sortBySet = getSortBy(sortBy);
    this.filterSet = filter;
    this.fields = fields;
    // TODO(fabio): check that the fields exists
    if (!Strings.isNullOrEmpty(fields)) {
      this.fieldSet.addAll(Arrays.stream(fields.split(",")).collect(Collectors.toSet()));
    }
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

  public Set<SortBy> getSortBySet() {
    return sortBySet;
  }

  public Set<FilterBy> getFilterSet() {
    return filterSet;
  }

  public Set<String> getFieldSet() {
    return fieldSet;
  }
}

