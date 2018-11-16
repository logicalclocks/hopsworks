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
import java.util.LinkedHashSet;
import java.util.Set;
import javax.ws.rs.QueryParam;

public class ActivitiesBeanParam {

  private final Set<ActivityFacade.SortBy> sort;
  private final Set<ActivityFacade.FilterBy> filter;

  public ActivitiesBeanParam(
      @QueryParam("sort_by") String sortBy,
      @QueryParam("filter_by") Set<ActivityFacade.FilterBy> filter) {
    this.sort = getSortBy(sortBy);
    this.filter = filter;

  }

  private Set<ActivityFacade.SortBy> getSortBy(String param) {
    if (param == null || param.isEmpty()) {
      return null;
    }
    String[] params = param.split(",");
    //Hash table and linked list implementation of the Set interface, with predictable iteration order
    Set<ActivityFacade.SortBy> sortBys = new LinkedHashSet<>();//make orderd
    ActivityFacade.SortBy sortBy;
    for (String s : params) {
      sortBy = ActivityFacade.SortBy.fromString(s.trim());
      sortBys.add(sortBy);
    }
    return sortBys;
  }

  public Set<ActivityFacade.SortBy> getSort() {
    return sort;
  }

  public Set<ActivityFacade.FilterBy> getFilter() {
    return filter;
  }

}
