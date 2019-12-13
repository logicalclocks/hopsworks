package io.hops.hopsworks.api.experiments;

import io.hops.hopsworks.api.experiments.results.ExperimentResultsResourceRequest;
import io.hops.hopsworks.api.experiments.tensorboard.TensorBoardResourceRequest;
import io.hops.hopsworks.common.api.Expansions;
import io.hops.hopsworks.common.api.ResourceRequest;

public class ExperimentExpansions implements Expansions {
  private ResourceRequest resourceRequest;

  public ExperimentExpansions(String queryParam) {
    ResourceRequest.Name name;
    //Get name of resource
    if (queryParam.contains("(")) {
      name = ResourceRequest.Name.valueOf(queryParam.substring(0, queryParam.indexOf('(')).toUpperCase());
    } else {
      name = ResourceRequest.Name.valueOf(queryParam.toUpperCase());
    }
    switch (name) {
      case RESULTS:
        resourceRequest = new ExperimentResultsResourceRequest(name, queryParam);
        break;
      case TENSORBOARD:
        resourceRequest = new TensorBoardResourceRequest(name, queryParam);
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