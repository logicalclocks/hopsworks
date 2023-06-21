/*
 * Copyright (C) 2023, Hopsworks AB. All rights reserved
 */
package io.hops.hopsworks.remote.user.api.mapping;

import io.hops.hopsworks.common.dao.AbstractFacade;
import io.hops.hopsworks.common.dao.remote.group.RemoteGroupProjectMappingFacade;

public class FilterBy implements AbstractFacade.FilterBy {
  private final RemoteGroupProjectMappingFacade.Filters filter;
  private final String param;
  
  public FilterBy(String param) {
    String[] filterByParams = param.split(":", 2);
    this.filter = RemoteGroupProjectMappingFacade.Filters.valueOf(filterByParams[0].toUpperCase());
    this.param = filterByParams.length > 1 ? filterByParams[1] : this.filter.getDefaultParam();
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
