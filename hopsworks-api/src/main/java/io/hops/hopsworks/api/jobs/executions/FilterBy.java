package io.hops.hopsworks.api.jobs.executions;

import io.hops.hopsworks.common.dao.AbstractFacade;
import io.hops.hopsworks.common.dao.jobhistory.ExecutionFacade;

public class FilterBy implements AbstractFacade.FilterBy {
  
  private final ExecutionFacade.Filters filter;
  private final String param;
  
  public FilterBy(String param) {
    if (param.contains(":")) {
      this.filter = ExecutionFacade.Filters.valueOf(param.substring(0, param.indexOf(':')).toUpperCase());
      this.param = param.substring(param.indexOf(':') + 1).toUpperCase();
    } else {
      this.filter = ExecutionFacade.Filters.valueOf(param);
      this.param = this.filter.getDefaultParam();
    }
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
