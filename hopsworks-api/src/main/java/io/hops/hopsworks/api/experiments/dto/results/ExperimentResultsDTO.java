package io.hops.hopsworks.api.experiments.dto.results;

import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement
public class ExperimentResultsDTO {

  public ExperimentResultsDTO() {
    //Needed for JAXB
  }

  private ExperimentResult parameters;

  private ExperimentResult outputs;

  public ExperimentResult getParameters() {
    return parameters;
  }

  public void setParameters(ExperimentResult parameters) {
    this.parameters = parameters;
  }

  public ExperimentResult getOutputs() {
    return outputs;
  }

  public void setOutputs(ExperimentResult outputs) {
    this.outputs = outputs;
  }
}