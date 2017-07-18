package io.hops.hopsworks.api.jobs;

import org.influxdb.dto.QueryResult;

import javax.xml.bind.annotation.XmlRootElement;
import java.io.Serializable;

@XmlRootElement
public class InfluxDBResultDTO implements Serializable {

  private static final Long serialVersionUID = 1L;

  private String query;
  private QueryResult result;

  public InfluxDBResultDTO() {}

  public QueryResult getResult() {
    return result;
  }

  public void setResult(QueryResult result) {
    this.result = result;
  }

  public void setQuery(String query) {
    this.query = query;
  }

  public String getQuery() {
    return query;
  }
}
