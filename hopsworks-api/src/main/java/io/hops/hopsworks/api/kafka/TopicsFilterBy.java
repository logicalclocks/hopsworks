package io.hops.hopsworks.api.kafka;

import io.hops.hopsworks.common.dao.AbstractFacade;
import io.hops.hopsworks.common.dao.kafka.KafkaFacade;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;

public class TopicsFilterBy implements AbstractFacade.FilterBy {
  
  private final KafkaFacade.TopicsFilters filter;
  private final String param;
  
  public TopicsFilterBy(String param) {
    if (param == null || param.isEmpty()) {
      throw new WebApplicationException("Filter by need to set a valid filter parameter", Response.Status.NOT_FOUND);
    }
    String[] filterByParams = param.split(":");
    this.filter = KafkaFacade.TopicsFilters.valueOf(filterByParams[0].toUpperCase());
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
