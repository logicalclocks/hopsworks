package io.hops.hopsworks.api.experiments.results;

import io.swagger.annotations.ApiParam;

import javax.ws.rs.QueryParam;
import java.util.LinkedHashSet;
import java.util.Set;

public class ExperimentResultsBeanParam {
  @QueryParam("sort_by")
  @ApiParam(value = "ex. sort_by=metric:desc")
  private String sortBy;
  private final Set<SortBy> sortBySet;

  private Set<SortBy> getSortBy(String param) {
    if (param == null || param.isEmpty()) {
      return new LinkedHashSet<>();
    }
    String[] params = param.split(",");
    //Hash table and linked list implementation of the Set interface, with predictable iteration order
    Set<SortBy> sortBys = new LinkedHashSet<>();//make ordered
    SortBy sort;
    for (String s : params) {
      sort = new SortBy(s.trim());
      sortBys.add(sort);
    }
    return sortBys;
  }

  public ExperimentResultsBeanParam(@QueryParam("sort_by") String sortBy) {
    this.sortBy = sortBy;
    sortBySet = getSortBy(sortBy);
  }

  public String getSortBy() {
    return sortBy;
  }

  public void setSortBy(String sortBy) {
    this.sortBy = sortBy;
  }

  public Set<SortBy> getSortBySet() {
    return sortBySet;
  }
}
