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
package io.hops.hopsworks.common.api;

import io.hops.hopsworks.common.dao.AbstractFacade;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class Resource {
  private Resource.Name name;
  private Integer offset;
  private Integer limit;
  private Set<? extends AbstractFacade.SortBy> sort;
  private Set<? extends AbstractFacade.FilterBy> filter;
  
  //Only for internal use by child classes
  protected List<String> queryProps;
  
  public Resource(Name name, String queryParam) {
    this.name = name;
    if (queryParam.contains("(")) {
      String[] queryPropsArr = queryParam.substring(queryParam.indexOf('(') + 1, queryParam.lastIndexOf(')')).split(
        ";");
      this.queryProps = new ArrayList<>();
      for (String queryProp : queryPropsArr) {
        String queryName = queryProp.substring(0, queryProp.indexOf('=') + 1);
        String queryVal = queryProp.substring(queryProp.indexOf('=') + 1);
        //Set offset
        switch (queryName) {
          case "offset":
            this.offset = Integer.parseInt(queryVal);
            break;
          case "limit":
            this.limit = Integer.parseInt(queryVal);
            break;
          default:
            this.queryProps.add(queryProp);
            break;
        }
      }
    }
  }
  
  public Name getName() {
    return name;
  }
  
  public void setName(Name name) {
    this.name = name;
  }
  
  public Integer getOffset() {
    return offset;
  }
  
  public void setOffset(Integer offset) {
    this.offset = offset;
  }
  
  public Integer getLimit() {
    return limit;
  }
  
  public void setLimit(Integer limit) {
    this.limit = limit;
  }
  
  public Set<? extends AbstractFacade.SortBy> getSort() {
    return sort;
  }
  
  public void setSort(Set<? extends AbstractFacade.SortBy> sort) {
    this.sort = sort;
  }
  
  public Set<? extends AbstractFacade.FilterBy> getFilter() {
    return filter;
  }
  
  public void setFilter(Set<? extends AbstractFacade.FilterBy> filter) {
    this.filter = filter;
  }
  
  /**
   * Name of the resource requested by the user which needs to match the name of the resource in Hopsworks.
   */
  public enum Name {
    USERS,
    USER, //user as it appears in ExecutionDTO
    CREATOR,//user as it appears in JobDTO
    PROJECTS,
    JOBS,
    EXECUTIONS,
    DATASETS,
    REQUESTS,
    INODES,
    MESSAGES,
    ACTIVITIES,
    DATASETREQUESTS;
    
    public static Name fromString(String name) {
      return valueOf(name.toUpperCase());
    }
    
    @Override
    public String toString() {
      return this.name().toLowerCase();
    }
  }
  
}

