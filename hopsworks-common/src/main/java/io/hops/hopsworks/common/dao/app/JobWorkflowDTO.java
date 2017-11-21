package io.hops.hopsworks.common.dao.app;

import java.util.List;
import javax.xml.bind.annotation.XmlRootElement;

/**
 *
 * <p>
 */
@XmlRootElement
public class JobWorkflowDTO extends KeystoreDTO {

  List<Integer> jobIds;

  public List<Integer> getJobIds() {
    return jobIds;
  }

  public void setJobIds(List<Integer> jobIds) {
    this.jobIds = jobIds;
  }

  @Override
  public String toString() {
    return "JobWorkflowDTO{" + "jobIds=" + jobIds + '}';
  }

}
