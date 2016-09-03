/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package io.hops.tf;

import io.hops.kafka.*;
import java.io.Serializable;
import java.util.List;
import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement
public class TfDetailsDTO implements Serializable{

    private String name;

    private List<PartitionDetailsDTO> parrtitionDetails;

    public TfDetailsDTO() {
    }

    public TfDetailsDTO(String name) {
        this.name = name;
    }

    public TfDetailsDTO(String topic, List<PartitionDetailsDTO> partitionDetails) {
        this.name = topic;
        this.parrtitionDetails = partitionDetails;
    }

    public String getName() {
        return name;
    }

    public List<PartitionDetailsDTO> getPartition() {
        return parrtitionDetails;
    }

    public void setName(String topic) {
        this.name = topic;
    }

    public void setPartition(List<PartitionDetailsDTO> partitionReplicas) {
        this.parrtitionDetails = partitionReplicas;
    }

}
