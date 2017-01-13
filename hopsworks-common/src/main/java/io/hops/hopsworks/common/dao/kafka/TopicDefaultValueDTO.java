package io.hops.hopsworks.common.dao.kafka;

import java.io.Serializable;
import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement
public class TopicDefaultValueDTO implements Serializable {

  private String numOfReplicas;

  private String numOfPartitions;

  private String maxNumOfReplicas;

  public TopicDefaultValueDTO() {
  }

  public TopicDefaultValueDTO(String numOfReplicas, String numOfPartitions,
          String maxNumOfReplicas) {
    this.numOfReplicas = numOfReplicas;
    this.numOfPartitions = numOfPartitions;
    this.maxNumOfReplicas = maxNumOfReplicas;
  }

  public String getNumOfPartitions() {
    return numOfPartitions;
  }

  public String getNumOfReplicas() {
    return numOfReplicas;
  }

  public String getMaxNumOfReplicas() {
    return maxNumOfReplicas;
  }

  public void setNumOfPartitions(String numOfPartitions) {
    this.numOfPartitions = numOfPartitions;
  }

  public void setNumOfReplicas(String numOfReplicas) {
    this.numOfReplicas = numOfReplicas;
  }

  public void setMaxNumOfReplicas(String maxNumOfReplicas) {
    this.maxNumOfReplicas = maxNumOfReplicas;
  }
}
