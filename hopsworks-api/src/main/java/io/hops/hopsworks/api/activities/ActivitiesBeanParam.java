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
package io.hops.hopsworks.api.activities;

import io.hops.hopsworks.common.dao.user.activity.ActivityFacade;
import io.swagger.annotations.ApiParam;
import java.util.LinkedHashSet;
import java.util.Set;
import javax.ws.rs.QueryParam;

public class ActivitiesBeanParam {

  @QueryParam("sort_by")
  @ApiParam(required = false, 
      value = "ex. sort_by=ID:asc,DATE_CREATED:desc",
      allowableValues = "ID:asc, ID:desc, DATE_CREATED:asc, DATE_CREATED:desc, FLAG:asc, FLAG:desc")
  private String sortBy;
  private final Set<ActivityFacade.SortBy> sortBySet;
  @QueryParam("filter_by")
  @ApiParam(required = false,
      value = "ex. filter_by=FLAG:DATASET",
      allowMultiple = true)
  private Set<ActivityFacade.FilterBy> filter;

  public ActivitiesBeanParam(
      @QueryParam("sort_by") String sortBy, 
      @QueryParam("filter_by") Set<ActivityFacade.FilterBy> filter) {
    this.sortBy = sortBy;
    this.sortBySet = getSortBy(sortBy);
    this.filter = filter;

  }

  private Set<ActivityFacade.SortBy> getSortBy(String param) {
    if (param == null || param.isEmpty()) {
      return null;
    }
    String[] params = param.split(",");
    //Hash table and linked list implementation of the Set interface, with predictable iteration order
    Set<ActivityFacade.SortBy> sortBys = new LinkedHashSet<>();//make orderd
    ActivityFacade.SortBy sort;
    for (String s : params) {
      sort = ActivityFacade.SortBy.fromString(s.trim());
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

  public Set<ActivityFacade.FilterBy> getFilter() {
    return filter;
  }

  public void setFilter(Set<ActivityFacade.FilterBy> filter) {
    this.filter = filter;
  }

  public Set<ActivityFacade.SortBy> getSortBySet() {
    return sortBySet;
  }

}
