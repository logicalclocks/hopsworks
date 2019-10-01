package io.hops.hopsworks.api.kafka;

import io.hops.hopsworks.common.dao.AbstractFacade;
import io.hops.hopsworks.common.dao.kafka.KafkaFacade;

public class TopicsFilterBy implements AbstractFacade.FilterBy {
  
  private final KafkaFacade.TopicsFilters filter;
  private final String param;
  
  public TopicsFilterBy(String param) {
    String[] filterByParams = param.split(":");
    this.filter = KafkaFacade.TopicsFilters.valueOf(filterByParams[0].toUpperCase());
    this.param = (filterByParams.length > 1) ? filterByParams[1].toUpperCase() : this.filter.getDefaultParam();
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
