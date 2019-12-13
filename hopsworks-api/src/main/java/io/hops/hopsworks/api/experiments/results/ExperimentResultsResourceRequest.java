package io.hops.hopsworks.api.experiments.results;

import io.hops.hopsworks.common.api.ResourceRequest;

import java.util.LinkedHashSet;
import java.util.Set;

public class ExperimentResultsResourceRequest extends ResourceRequest {
  public ExperimentResultsResourceRequest(ResourceRequest.Name name, String queryParam) {
    super(name, queryParam);
    //Set sort_by
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
      }
    }
  }
}
