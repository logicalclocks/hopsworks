package io.hops.hopsworks.api.jobs.executions;

import io.hops.hopsworks.api.user.UserResource;
import io.hops.hopsworks.common.api.Expansions;
import io.hops.hopsworks.common.api.Resource;

public class ExecutionExpansions implements Expansions {
  private Resource resource;
  
  public ExecutionExpansions(String queryParam) {
    Resource.Name name;
    //Get name of resource
    if (queryParam.contains("(")) {
      name = Resource.Name.valueOf(queryParam.substring(0, queryParam.indexOf('(')).toUpperCase());
    } else {
      name = Resource.Name.valueOf(queryParam.toUpperCase());
    }
    
    switch (name) {
      case USER:
        resource = new UserResource(name, queryParam);
        break;
      default:
        break;
    }
  }
  
  @Override
  public Resource getResource() {
    return resource;
  }
  
  @Override
  public void setResource(Resource resource) {
    this.resource = resource;
  }
}

