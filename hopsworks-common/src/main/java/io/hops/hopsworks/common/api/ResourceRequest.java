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

import com.google.common.base.Strings;
import io.hops.hopsworks.common.dao.AbstractFacade;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class ResourceRequest {
  private ResourceRequest.Name name;
  private Integer offset;
  private Integer limit;
  private Set<? extends AbstractFacade.SortBy> sort;
  private Set<? extends AbstractFacade.FilterBy> filter;
  private Set<ResourceRequest> expansions;
  
  //Only for internal use by child classes
  protected List<String> queryProps;
  
  public ResourceRequest(Name name) {
    this(name, null);
  }
  
  public ResourceRequest(Name name, String queryParam) {
    this.name = name;
    this.queryProps = new ArrayList<>();
    if (!Strings.isNullOrEmpty(queryParam) && queryParam.contains("(")) {
      String[] queryPropsArr = queryParam.substring(queryParam.indexOf('(') + 1, queryParam.lastIndexOf(')')).split(
        ";");
      for (String queryProp : queryPropsArr) {
        String queryName = queryProp.substring(0, queryProp.indexOf('='));
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
  
  public Set<ResourceRequest> getExpansions() {
    return expansions;
  }
  
  public void setExpansions(Set<ResourceRequest> expansions) {
    this.expansions = expansions;
  }
  
  public boolean contains(Name name) {
    if(this.name == name){
      return true;
    }
    if(expansions != null && !expansions.isEmpty()) {
      for (ResourceRequest expansion : expansions) {
        if (expansion.name == name) {
          return true;
        }
      }
    }
    return false;
  }
  
  public ResourceRequest get(Name name) {
    if(expansions != null && !expansions.isEmpty()) {
      for (ResourceRequest expansion : expansions) {
        if (expansion.name == name) {
          return expansion;
        }
      }
    }
    return null;
  }
  
  /**
   * Name of the resource requested by the user which needs to match the name of the resource in Hopsworks.
   */
  public enum Name {
    USERS,
    USER, //user as it appears in ExecutionDTO
    CREATOR,//user as it appears in JobDTO
    PROJECT,
    JOBS,
    KAFKA,
    TOPICS,
    ACL,
    SHARED,
    EXECUTIONS,
    DATASETS,
    REQUESTS,
    INODES,
    MESSAGES,
    ACTIVITIES,
    PYTHON,
    ENVIRONMENTS,
    COMMANDS,
    MACHINETYPES,
    LIBRARIES,
    DATASETREQUESTS,
    APIKEY,
    TEMPLATE;
    
    public static Name fromString(String name) {
      return valueOf(name.toUpperCase());
    }
    
    @Override
    public String toString() {
      return this.name().toLowerCase();
    }
  }
  
}

