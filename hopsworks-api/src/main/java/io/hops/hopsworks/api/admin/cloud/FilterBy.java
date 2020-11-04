/*
 * Copyright (C) 2020, Logical Clocks AB. All rights reserved
 */
package io.hops.hopsworks.api.admin.cloud;

import io.hops.hopsworks.common.dao.AbstractFacade;
import io.hops.hopsworks.common.dao.cloud.CloudRoleMappingFacade;

public class FilterBy implements AbstractFacade.FilterBy {
  private final CloudRoleMappingFacade.Filters filter;
  private final String param;
  
  public FilterBy(String param) {
    String[] filterByParams = param.split(":", 2);
    this.filter = CloudRoleMappingFacade.Filters.valueOf(filterByParams[0].toUpperCase());
    String filterStr = filterByParams.length > 1 ? filterByParams[1] : this.filter.getDefaultParam();
    this.param = filterStr;
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
