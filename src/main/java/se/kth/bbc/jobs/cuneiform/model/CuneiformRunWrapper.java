package se.kth.bbc.jobs.cuneiform.model;

import javax.xml.bind.annotation.XmlRootElement;
import se.kth.bbc.jobs.cuneiform.model.WorkflowDTO;
import se.kth.bbc.jobs.yarn.YarnJobConfiguration;

/**
 * A wrapper around a WorkflowDTO and a YarnJobConfiguration for easy
 * communication with the client.
 * <p>
 * @author stig
 */
@XmlRootElement
public class CuneiformRunWrapper {

  private YarnJobConfiguration yarnConfig;
  private WorkflowDTO wf;

  public CuneiformRunWrapper(YarnJobConfiguration yjc, WorkflowDTO wf) {
    this.yarnConfig = yjc;
    this.wf = wf;
  }

  public CuneiformRunWrapper() {
  }

  public YarnJobConfiguration getYarnConfig() {
    return yarnConfig;
  }

  public void setYarnConfig(YarnJobConfiguration yarnConfig) {
    this.yarnConfig = yarnConfig;
  }

  public WorkflowDTO getWf() {
    return wf;
  }

  public void setWf(WorkflowDTO wf) {
    this.wf = wf;
  }

}
