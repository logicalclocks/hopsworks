package io.hops.hopsworks.api.jobs.executions;

import io.hops.hopsworks.api.user.UserResourceRequest;
import io.hops.hopsworks.common.api.Expansions;
import io.hops.hopsworks.common.api.ResourceRequest;

public class ExecutionExpansions implements Expansions {
  private ResourceRequest resourceRequest;
  
  public ExecutionExpansions(String queryParam) {
    ResourceRequest.Name name;
    //Get name of resource
    if (queryParam.contains("(")) {
      name = ResourceRequest.Name.valueOf(queryParam.substring(0, queryParam.indexOf('(')).toUpperCase());
    } else {
      name = ResourceRequest.Name.valueOf(queryParam.toUpperCase());
    }
    
    switch (name) {
      case USER:
        resourceRequest = new UserResourceRequest(name, queryParam);
        break;
      default:
        break;
    }
  }
  
  @Override
  public ResourceRequest getResourceRequest() {
    return resourceRequest;
  }
  
  @Override
  public void setResourceRequest(ResourceRequest resourceRequest) {
    this.resourceRequest = resourceRequest;
  }
}

