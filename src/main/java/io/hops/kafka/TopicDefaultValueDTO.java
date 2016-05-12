/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package io.hops.kafka;

import javax.xml.bind.annotation.XmlRootElement;

/**
 *
 * @author misdess
 */
@XmlRootElement
public class TopicDefaultValueDTO {
    
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
