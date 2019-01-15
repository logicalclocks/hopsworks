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

import io.swagger.annotations.ApiParam;
import java.util.LinkedHashSet;
import java.util.Set;
import javax.ws.rs.QueryParam;

public class UsersBeanParam {

  @QueryParam("sort_by")
  @ApiParam(value = "ex. sort_by=first_name:asc,last_name:desc",
      allowableValues = "first_name:asc,first_name:desc,last_name:asc,last_name:desc,date_created:asc,date_created:desc"
      + ",email:asc,email:desc")
  private String sortBy;
  private final Set<SortBy> sortBySet;
  @QueryParam("filter_by")
  @ApiParam(value = "ex. filter_by=role:hops_admin,hops_user&filter_by=status:2",
      allowableValues = "role:hops_admin,role_neq:hops_admin,role:hops_user,role_neq:hops_user,role:agent, "
      + "role_neq:agent, status=new_mobile_account, status=verified_account, status=activated_account, "
      + "status=deactivated_account, status=blocked_account, status=lost_mobile, status=spam_account, "
      + "status_gt=2, status_gt=2, status_lt=2, is_online=0, is_online=1, false_login=10, false_login_gt=20, "
      + "false_login_lt=20, user_name=a, user_first_name=b, user_last_name=c, user_email=d, user_like=e",
      allowMultiple = true)
  private Set<FilterBy> filter;

  public UsersBeanParam(
      @QueryParam("sort_by") String sortBy,
      @QueryParam("filter_by") Set<FilterBy> filter) {
    this.sortBy = sortBy;
    this.sortBySet = getSortBy(sortBy);
    this.filter = filter;
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

  public String getSortBy() {
    return sortBy;
  }

  public void setSortBy(String sortBy) {
    this.sortBy = sortBy;
  }

  public Set<FilterBy> getFilter() {
    return filter;
  }

  public void setFilter(Set<FilterBy> filter) {
    this.filter = filter;
  }

  public Set<SortBy> getSortBySet() {
    return sortBySet;
  }

}
