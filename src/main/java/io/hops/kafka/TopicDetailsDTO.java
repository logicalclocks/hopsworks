/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package io.hops.kafka;

import java.util.List;
import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement
public class TopicDetailsDTO {

    private String name;

    private List<PartitionDetails> parrtitionDetails;

    public TopicDetailsDTO() {
    }

    public TopicDetailsDTO(String name) {
        this.name = name;
    }

    public TopicDetailsDTO(String topic, List<PartitionDetails> partitionDetails) {
        this.name = topic;
        this.parrtitionDetails = partitionDetails;
    }

    public String getName() {
        return name;
    }

    public List<PartitionDetails> getPartition() {
        return parrtitionDetails;
    }

    public void setName(String topic) {
        this.name = topic;
    }

    public void setPartition(List<PartitionDetails> partitionReplicas) {
        this.parrtitionDetails = partitionReplicas;
    }

}
