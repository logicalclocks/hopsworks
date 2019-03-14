/*
 * This file is part of Hopsworks
 * Copyright (C) 2019, Logical Clocks AB. All rights reserved
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
package io.hops.hopsworks.admin.user.administration;

import io.hops.hopsworks.common.dao.AbstractFacade;
import io.hops.hopsworks.common.dao.user.UserFacade;

public class FilterBy implements AbstractFacade.FilterBy {
  
  private final UserFacade.Filters filter;
  private final String param;
  
  public FilterBy(String filter, String filterBy) {
    this.filter = toFilters(filter);
    String filterStr = filterBy != null && !filterBy.isEmpty() ? filterBy.toUpperCase() :
      this.filter.getDefaultParam();
    this.param = filterStr;
  }
  
  
  private UserFacade.Filters toFilters(String filter) {
    switch (filter) {
      case "fname":
        return UserFacade.Filters.USER_FIRST_NAME;
      case "lname":
        return UserFacade.Filters.USER_LAST_NAME;
      case "email":
        return UserFacade.Filters.USER_EMAIL;
      case "mode":
        return UserFacade.Filters.TYPE;
      case "status":
        return UserFacade.Filters.STATUS;
      case "isonline":
        return UserFacade.Filters.IS_ONLINE;
      default:
        throw new UnsupportedOperationException();
    }
  }
  
  @Override
  public String getParam() {
    return param;
  }
  
  @Override
  public String getValue() {
    return this.filter.getValue();
  }
  
  @Override
  public String getSql() {
    return this.filter.getSql();
  }
  
  @Override
  public String getField() {
    return this.filter.getField();
  }
  
  @Override
  public String toString() {
    return filter.toString();
  }
  
}
