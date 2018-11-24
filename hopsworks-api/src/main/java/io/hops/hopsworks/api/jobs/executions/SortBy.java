package io.hops.hopsworks.api.jobs.executions;

import io.hops.hopsworks.common.dao.AbstractFacade;
import io.hops.hopsworks.common.dao.jobhistory.ExecutionFacade;

public class SortBy implements AbstractFacade.SortBy {
  
  private final ExecutionFacade.Sorts sortBy;
  private final AbstractFacade.OrderBy param;
  
  public SortBy(String param) {
    String[] sortByParams = param.split(":");
    this.sortBy = ExecutionFacade.Sorts.valueOf(sortByParams[0].toUpperCase());
    String order = sortByParams.length > 1 ? sortByParams[1].toUpperCase() : this.sortBy.getDefaultParam();
    this.param = AbstractFacade.OrderBy.valueOf(order);
  }
  
  @Override
  public String getValue() {
    return this.sortBy.getValue();
  }
  
  @Override
  public AbstractFacade.OrderBy getParam() {
    return this.param;
  }
  
  @Override
  public String getSql() {
    return this.sortBy.getSql();
  }
}