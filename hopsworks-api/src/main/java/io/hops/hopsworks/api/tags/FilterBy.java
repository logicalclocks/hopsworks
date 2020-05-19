/*
 * Copyright (C) 2020, Logical Clocks AB. All rights reserved
 */
package io.hops.hopsworks.api.tags;

import io.hops.hopsworks.common.dao.AbstractFacade;
import io.hops.hopsworks.common.dao.featurestore.tag.FeatureStoreTagFacade;

public class FilterBy implements AbstractFacade.FilterBy {
  private final FeatureStoreTagFacade.Filters filter;
  private final String param;
  
  public FilterBy(String param) {
    if (param.contains(":")) {
      this.filter = FeatureStoreTagFacade.Filters.valueOf(param.substring(0, param.indexOf(':')).toUpperCase());
      this.param = param.substring(param.indexOf(':') + 1);
    } else {
      this.filter = FeatureStoreTagFacade.Filters.valueOf(param);
      this.param = this.filter.getDefaultParam();
    }
  }
  
  @Override
  public String getValue() {
    return this.filter.getValue();
  }
  
  @Override
  public String getParam() {
    return param;
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
    return filter.toString() + ":" + param;
  }
}
