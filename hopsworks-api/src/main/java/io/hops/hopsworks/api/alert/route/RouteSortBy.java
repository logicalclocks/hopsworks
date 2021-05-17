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

package io.hops.hopsworks.api.alert.route;

import io.hops.hopsworks.common.dao.AbstractFacade;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;

public class RouteSortBy {
  
  public enum SortBys {
    RECEIVER("RECEIVER", "ASC");
    
    private SortBys(String value, String defaultOrder) {
      this.value = value;
      this.defaultOrder = defaultOrder;
    }
    
    private final String value;
    private final String defaultOrder;
    
    public String getValue() {
      return value;
    }
    
    public String getDefaultOrder() {
      return defaultOrder;
    }
    
    @Override
    public String toString() {
      return value;
    }
  }
  
  private final SortBys sortBy;
  private final AbstractFacade.OrderBy param;
  
  public RouteSortBy(String param) {
    String[] sortByParams = param.split(":");
    String sort = "";
    try {
      sort = sortByParams[0].toUpperCase();
      this.sortBy = SortBys.valueOf(sort);
    } catch (IllegalArgumentException iae) {
      throw new WebApplicationException("Sort by need to set a valid sort parameter, but found: " + sort,
          Response.Status.NOT_FOUND);
    }
    String order = "";
    try {
      order = sortByParams.length > 1 ? sortByParams[1].toUpperCase() : this.sortBy.getDefaultOrder();
      this.param = AbstractFacade.OrderBy.valueOf(order);
    } catch (IllegalArgumentException iae) {
      throw new WebApplicationException("Sort by " + sort + " need to set a valid order(asc|desc), but found: " + order
          , Response.Status.NOT_FOUND);
    }
  }
  
  public SortBys getSortBy() {
    return sortBy;
  }
  
  public String getValue() {
    return this.sortBy.getValue();
  }
  
  public AbstractFacade.OrderBy getParam() {
    return this.param;
  }
}