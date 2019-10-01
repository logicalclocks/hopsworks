package io.hops.hopsworks.api.kafka;

import javax.ws.rs.QueryParam;
import java.util.LinkedHashSet;
import java.util.Set;

public class TopicsBeanParam {
  
  private String sortBy;
  private final Set<TopicsSortBy> sortBySet;
  private Set<TopicsFilterBy> filter;
  
  public TopicsBeanParam(
    @QueryParam("sort_by") String sortBy,
    @QueryParam("filter_by") Set<TopicsFilterBy> filter) {
    this.sortBy = sortBy;
    this.sortBySet = getSortBy(sortBy);
    this.filter = filter;
  }
  
  private Set<TopicsSortBy> getSortBy(String param) {
    if (param == null || param.isEmpty()) {
      return new LinkedHashSet<>();
    }
    String[] params = param.split(",");
    //Hash table and linked list implementation of the Set interface, with predictable iteration order
    Set<TopicsSortBy> sortBys = new LinkedHashSet<>();//make ordered
    TopicsSortBy sort;
    for (String s : params) {
      sort = new TopicsSortBy(s.trim());
      sortBys.add(sort);
    }
    return sortBys;
  }
  
  public String getSortBy() {
    return sortBy;
  }
  
  public void setSortBy(String sortBy) {
    this.sortBy = sortBy;
  }
  
  public Set<TopicsFilterBy> getFilter() {
    return filter;
  }
  
  public void setFilter(Set<TopicsFilterBy> filter) {
    this.filter = filter;
  }
  
  public Set<TopicsSortBy> getSortBySet() {
    return sortBySet;
  }
  
  @Override
  public String toString() {
    return "TopicsBeanParam=[sort_by="+sortBy+", filter_by="+filter+"]";
  }
}
