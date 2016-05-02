/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package io.hops.kafka;

/**
 *
 * @author misdess
 */
public class TopicDefaultValueDTO {
    
    private String numOfReplicas;
    
    private String numOfPartitions;

    public TopicDefaultValueDTO(String numOfReplicas, String numOfPartitions) {
        this.numOfReplicas = numOfReplicas;
        this.numOfPartitions = numOfPartitions;
    }

    public String getNumOfPartitions() {
        return numOfPartitions;
    }

    public String getNumOfReplicas() {
        return numOfReplicas;
    }

    public void setNumOfPartitions(String numOfPartitions) {
        this.numOfPartitions = numOfPartitions;
    }

    public void setNumOfReplicas(String numOfReplicas) {
        this.numOfReplicas = numOfReplicas;
    }
}
