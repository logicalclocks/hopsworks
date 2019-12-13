package io.hops.hopsworks.api.experiments.tensorboard;

import io.hops.hopsworks.api.jobs.executions.FilterBy;
import io.hops.hopsworks.api.jobs.executions.SortBy;
import io.hops.hopsworks.common.api.ResourceRequest;

import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Set;

public class TensorBoardResourceRequest extends ResourceRequest {
  public TensorBoardResourceRequest(Name name, String queryParam) {
    super(name, queryParam);
    //Set sort_by
    Set<FilterBy> filters = null;
    for (String queryProp : queryProps) {
      if (queryProp.startsWith("sort_by")) {
        String[] params = queryProp.substring(queryProp.indexOf('=')+1).split(",");
        //Hash table and linked list implementation of the Set interface, with predictable iteration order
        Set<SortBy> sortBys = new LinkedHashSet<>();//make ordered
        SortBy sort;
        for (String s : params) {
          sort = new SortBy(s.trim());
          sortBys.add(sort);
        }
        super.setSort(sortBys);
      } else if (queryProp.startsWith("filter_by")) {
        if (filters == null) {
          filters = new HashSet<>();
        }
        //Set filter_by
        FilterBy filterBy = new FilterBy(queryProp.substring(queryProp.indexOf('=')+1));
        filters.add(filterBy);
      }
    }
    super.setFilter(filters);
  }
}
