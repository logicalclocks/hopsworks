package io.hops.hopsworks.api.admin.hosts;

import io.hops.hopsworks.common.dao.AbstractFacade;
import io.hops.hopsworks.common.dao.host.HostsFacade;

public class FilterBy implements AbstractFacade.FilterBy {
  
  private final HostsFacade.Filters filter;
  private final String param;
  
  public FilterBy(String param) {
    String[] filterByParams = param.split(":");
    this.filter = HostsFacade.Filters.valueOf(filterByParams[0].toUpperCase());
    this.param = (filterByParams.length > 1) ? filterByParams[1] : this.filter.getDefaultParam();
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
