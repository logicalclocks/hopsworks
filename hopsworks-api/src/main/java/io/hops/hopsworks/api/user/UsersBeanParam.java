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
package io.hops.hopsworks.api.user;

import io.hops.hopsworks.common.dao.user.UserFacade;
import java.util.LinkedHashSet;
import java.util.Set;
import javax.ws.rs.QueryParam;

public class UsersBeanParam {

  private final Set<UserFacade.SortBy> sort;
  private final Set<UserFacade.FilterBy> filter;

  public UsersBeanParam(
      @QueryParam("sort_by") String sortBy,
      @QueryParam("filter_by") Set<UserFacade.FilterBy> filter) {
    this.sort = getSortBy(sortBy);
    this.filter = filter;
  }

  private Set<UserFacade.SortBy> getSortBy(String param) {
    if (param == null || param.isEmpty()) {
      return null;
    }
    String[] params = param.split(",");
    //Hash table and linked list implementation of the Set interface, with predictable iteration order
    Set<UserFacade.SortBy> sortBys = new LinkedHashSet<>();//make orderd
    UserFacade.SortBy sortBy;
    for (String s : params) {
      sortBy = UserFacade.SortBy.fromString(s.trim());
      sortBys.add(sortBy);
    }
    return sortBys;
  }

  public Set<UserFacade.SortBy> getSort() {
    return sort;
  }

  public Set<UserFacade.FilterBy> getFilter() {
    return filter;
  }

}
