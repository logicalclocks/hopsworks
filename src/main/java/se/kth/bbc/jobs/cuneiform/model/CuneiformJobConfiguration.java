package se.kth.bbc.jobs.cuneiform.model;

import javax.xml.bind.annotation.XmlRootElement;
import se.kth.bbc.jobs.yarn.YarnJobConfiguration;

/**
 * A wrapper around a WorkflowDTO and a YarnJobConfiguration for easy
 * communication with the client.
 * <p>
 * @author stig
 */
@XmlRootElement
public class CuneiformJobConfiguration extends YarnJobConfiguration{

  private WorkflowDTO wf;
  
  public CuneiformJobConfiguration() {
  }

  public CuneiformJobConfiguration(WorkflowDTO wf) {
    this.wf = wf;
  }

  public WorkflowDTO getWf() {
    return wf;
  }

  public void setWf(WorkflowDTO wf) {
    this.wf = wf;
  }

}
