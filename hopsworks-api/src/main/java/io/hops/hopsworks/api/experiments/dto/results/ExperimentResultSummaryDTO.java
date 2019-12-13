package io.hops.hopsworks.api.experiments.dto.results;

import io.hops.hopsworks.common.api.RestDTO;

import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement
public class ExperimentResultSummaryDTO extends RestDTO<ExperimentResultSummaryDTO> {
  private ExperimentResultsDTO[] combinations;

  public ExperimentResultSummaryDTO() {
    //Needed for JAXB
  }

  public ExperimentResultsDTO[] getCombinations() {
    return combinations;
  }

  public void setCombinations(ExperimentResultsDTO[] combinations) {
    this.combinations = combinations;
  }
}
