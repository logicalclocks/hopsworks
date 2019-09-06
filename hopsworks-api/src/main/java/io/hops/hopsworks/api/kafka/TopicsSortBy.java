package io.hops.hopsworks.api.kafka;

import io.hops.hopsworks.common.dao.AbstractFacade;
import io.hops.hopsworks.common.dao.kafka.KafkaFacade;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;

public class TopicsSortBy implements AbstractFacade.SortBy {
  private final KafkaFacade.TopicSorts sortBy;
  private final AbstractFacade.OrderBy param;
  
  public TopicsSortBy(String param) {
    String[] sortByParams = param.split(":");
    String sort = "";
    try {
      sort = sortByParams[0].toUpperCase();
      this.sortBy = KafkaFacade.TopicSorts.valueOf(sort);
    } catch (IllegalArgumentException iae) {
      throw new WebApplicationException("Sort by need to set a valid sort parameter, but found: " + sort,
        Response.Status.NOT_FOUND);
    }
    String order = "";
    try {
      order = sortByParams.length > 1 ? sortByParams[1].toUpperCase() : this.sortBy.getDefaultParam();
      this.param = AbstractFacade.OrderBy.valueOf(order);
    } catch (IllegalArgumentException iae) {
      throw new WebApplicationException("Sort by " + sort + " need to set a valid order(asc|desc), but found: " + order
        , Response.Status.NOT_FOUND);
    }
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
