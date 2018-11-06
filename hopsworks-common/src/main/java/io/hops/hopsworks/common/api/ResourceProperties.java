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

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Class that parses user provided uri query params and then is used to propagate this information to the resource
 * builders. An example of a request accepted by hopsworks api is
 * "offset=5&limit=10&sort_by=date&order_by=asc&expand=param1,param2(offset=0:limit=10)"
 */
public class ResourceProperties {
  
  private List<ResourceProperty> properties;
  
  public ResourceProperties(Name resourceName) {
    this(resourceName, null, null, null, null, null);
  }
  
  public ResourceProperties(Name resourceName, String expandParam) {
    this(resourceName, null, null, null, null, expandParam);
  }
  
  public ResourceProperties(Name resourceName, Integer offsetParam, Integer limitParam, SortBy sortByParam,
    OrderBy orderByParam, String expandParam) {
    properties = new ArrayList<>();
    //Resource that was requested
    ResourceProperty resource = new ResourceProperty()
      .setName(resourceName)
      .setOffset(offsetParam)
      .setLimit(limitParam)
      .setSortBy(sortByParam)
      .setOrderBy(orderByParam);
    
    properties.add(resource);
    
    //Parse expand param of the requested resource
    if (!Strings.isNullOrEmpty(expandParam)) {
      String[] propertiesToExpand = expandParam.split(",");
      for (String property : propertiesToExpand) {
        ResourceProperty resourceProperty;
        //Get offset and limit if any
        if (property.contains("(")) {
          resourceProperty = new ResourceProperty().setName(Name.valueOf(property.substring(0, property.indexOf('('))
            .toUpperCase()));
        } else {
          resourceProperty = new ResourceProperty().setName(Name.valueOf(property.toUpperCase()));
        }
        if (property.contains(":")) {
          resourceProperty
            .setOffset(Integer.parseInt(property.substring(property.indexOf('=') + 1,
              property.indexOf(':'))))
            .setLimit(Integer.parseInt(property.substring(property.lastIndexOf('=') + 1,
              property.indexOf(')'))));
        }
        properties.add(resourceProperty);
      }
    }
  }
  
  public List<ResourceProperty> getProperties() {
    return properties;
  }
  
  public ResourceProperty get(Name property) {
    if (properties != null) {
      for (ResourceProperty resourceProperty : properties) {
        if (resourceProperty.getName().equals(property)) {
          return resourceProperty;
        }
      }
    }
    return null;
  }
  
  
  public class ResourceProperty {
    private Name name;
    private Integer offset;
    private Integer limit;
    private SortBy sortBy;
    private OrderBy orderBy;
    
    public Name getName() {
      return name;
    }
    
    public ResourceProperty setName(Name name) {
      this.name = name;
      return this;
    }
    
    public Integer getOffset() {
      return offset;
    }
    
    public ResourceProperty setOffset(Integer offset) {
      this.offset = offset;
      return this;
    }
    
    public Integer getLimit() {
      return limit;
    }
    
    public ResourceProperty setLimit(Integer limit) {
      this.limit = limit;
      return this;
    }
    
    public SortBy getSortBy() {
      return sortBy;
    }
    
    public ResourceProperty setSortBy(SortBy sortBy) {
      this.sortBy = sortBy;
      return this;
    }
    
    public OrderBy getOrderBy() {
      return orderBy;
    }
    
    public ResourceProperty setOrderBy(OrderBy orderBy) {
      this.orderBy = orderBy;
      return this;
    }
    
    @Override
    public boolean equals(Object o) {
      if (this == o) {
        return true;
      }
      if (o == null || getClass() != o.getClass()) {
        return false;
      }
      ResourceProperty that = (ResourceProperty) o;
      return Objects.equals(name, that.name);
    }
    
    @Override
    public int hashCode() {
      
      return Objects.hash(name);
    }
    
    @Override
    public String toString() {
      return "ResourceProperty{" +
        "name='" + name + '\'' +
        ", offset=" + offset +
        ", limit=" + limit +
        ", sortBy='" + sortBy + '\'' +
        ", orderBy='" + orderBy + '\'' +
        '}';
    }
  }
  
  /**
   * Name of the resource requested by the user which needs to match the name of the resource in Hopsworks.
   */
  public enum Name {
    USERS,
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
  
  public enum SortBy {
    ID,
    NAME,
    DATE_SENT,
    DATE_CREATED
  }
  
  public enum OrderBy {
    ASC,
    DESC
  }
}

