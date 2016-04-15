/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package io.hops.kafka;

import java.util.Map;
import java.util.Set;
import javax.xml.bind.annotation.XmlRootElement;


@XmlRootElement
public class TopicDetailDTO {

  private String topic;
  
  private Map<Integer, Set<String>> partitionReplicas;
  
  private Map<Integer, String> partitionLeader;
  
  private Map<Integer, Set<String>>inSyncReplicas;
  
  
  public TopicDetailDTO() {
  }
  public TopicDetailDTO(String topic) {
    this.topic = topic;
  }

    public TopicDetailDTO(String topic, Map<Integer, Set<String>> partition,
            Map<Integer, String> patitionLeader,
            Map<Integer, Set<String>> inSyncReplicas) {
        this.topic = topic;
        this.partitionReplicas = partition;
        this.partitionLeader = patitionLeader;
        this.inSyncReplicas = inSyncReplicas;
    }

    public Map<Integer, Set<String>> getInSyncReplicas() {
        return inSyncReplicas;
    }

    public Map<Integer, Set<String>> getPartition() {
        return partitionReplicas;
    }
  

    public Map<Integer, String> getPartitionLeader() {
        return partitionLeader;
    }

    public void setInSyncReplicas(Map<Integer, Set<String>> inSyncReplicas) {
        this.inSyncReplicas = inSyncReplicas;
    }

    public void setPartition(Map<Integer, Set<String>> partitionReplicas) {
        this.partitionReplicas = partitionReplicas;
    }

    public void setPartitionLeader(Map<Integer, String> patitionLeader) {
        this.partitionLeader = patitionLeader;
    }

  
  public String getTopic() {
    return topic;
  }

  public void setTopic(String topic) {
    this.topic = topic;
  }
  
}
