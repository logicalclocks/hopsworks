package io.hops.hopsworks.api.jobs.executions;

import io.hops.hopsworks.common.dao.AbstractFacade;
import io.hops.hopsworks.common.dao.jobhistory.ExecutionFacade;

public class FilterBy implements AbstractFacade.FilterBy {
  
  private final ExecutionFacade.Filters filter;
  private final String param;
  
  public FilterBy(String param) {
    String[] filterByParams = param.split(":");
    this.filter = ExecutionFacade.Filters.valueOf(filterByParams[0].toUpperCase());
    String filterStr = filterByParams.length > 1 ? filterByParams[1].toUpperCase() : this.filter.getDefaultParam();
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
