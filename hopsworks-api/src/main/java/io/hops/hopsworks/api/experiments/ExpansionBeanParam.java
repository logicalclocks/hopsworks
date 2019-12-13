package io.hops.hopsworks.api.experiments;

import io.hops.hopsworks.common.api.ResourceRequest;
import io.swagger.annotations.ApiParam;

import javax.ws.rs.QueryParam;
import java.util.HashSet;
import java.util.Set;

public class ExpansionBeanParam {
  @QueryParam("expand")
  @ApiParam(value = "ex. expand=tensorboard", allowableValues = "expand=tensorboard, expand=results, expand=provenance")
  private Set<ExperimentExpansions> expansions;

  public ExpansionBeanParam(@QueryParam("expand") Set<ExperimentExpansions> expansions) {
    this.expansions = expansions;
  }

  public Set<ExperimentExpansions> getExpansions() {
    return expansions;
  }

  public void setExpansions(Set<ExperimentExpansions> expansions) {
    this.expansions = expansions;
  }

  public Set<ResourceRequest> getResources(){
    Set<ResourceRequest> expansions = new HashSet<>();
    for(ExperimentExpansions experimentExpansions : this.expansions){
      expansions.add(experimentExpansions.getResourceRequest());
    }
    return expansions;
  }
}