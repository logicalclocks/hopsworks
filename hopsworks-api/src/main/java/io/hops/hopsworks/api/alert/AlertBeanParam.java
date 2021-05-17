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
package io.hops.hopsworks.api.alert;

import io.swagger.annotations.ApiParam;

import javax.ws.rs.BeanParam;
import javax.ws.rs.QueryParam;
import java.util.LinkedHashSet;
import java.util.Set;

public class AlertBeanParam {
  
  @QueryParam("sort_by")
  @ApiParam(value = "ex. sort_by=updatedAt:asc,updatedAt:desc",
      allowableValues = "UPDATED_AT:asc, UPDATED_AT:desc, STARTS_AT:asc, STARTS_AT:desc, ENDS_AT:asc, ENDS_AT:desc")
  private String sortBy;
  @QueryParam("unprocessed")
  @ApiParam(defaultValue = "true", allowableValues = "true, false")
  private Boolean unprocessed;
  private final Set<AlertSortBy> sortBySet;
  @BeanParam
  private AlertFilterBy alertFilterBy;
  
  public AlertBeanParam(@QueryParam("sort_by") String sortBy, @BeanParam AlertFilterBy alertFilterBy,
      @QueryParam("unprocessed") Boolean unprocessed) {
    this.sortBy = sortBy;
    this.unprocessed = unprocessed;
    this.sortBySet = getSortBy(sortBy);
    this.alertFilterBy = alertFilterBy == null ? new AlertFilterBy(null, null, null,  null, null) : alertFilterBy;
  }
  
  private Set<AlertSortBy> getSortBy(String param) {
    if (param == null || param.isEmpty()) {
      return new LinkedHashSet<>();
    }
    String[] params = param.split(",");
    //Hash table and linked list implementation of the Set interface, with predictable iteration order
    Set<AlertSortBy> sortBys = new LinkedHashSet<>();//make ordered
    AlertSortBy sort;
    for (String s : params) {
      sort = new AlertSortBy(s.trim());
      sortBys.add(sort);
    }
    return sortBys;
  }
  
  public Set<AlertSortBy> getSortBySet() {
    return sortBySet;
  }
  
  public AlertFilterBy getAlertFilterBy() {
    return alertFilterBy;
  }
  
  public Boolean isUnprocessed() {
    return unprocessed;
  }
  
  public void setUnprocessed(Boolean unprocessed) {
    this.unprocessed = unprocessed;
  }
  
  @Override
  public String toString() {
    return "AlertBeanParam{" +
        "sortBy='" + sortBy + '\'' +
        ", sortBySet=" + sortBySet +
        ", alertFilterBy=" + alertFilterBy +
        '}';
  }
}