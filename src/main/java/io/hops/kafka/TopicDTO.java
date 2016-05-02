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
public class TopicDTO {

    private String name;

    private Integer numOfReplicas;
    
    private Integer numOfPartitions;

    // TODO - put in all the name details here
    public TopicDTO() {
    }

    public TopicDTO(String name) {
        this.name = name;
    }

    public TopicDTO(String name, Integer numOfReplicas, Integer numOfPartitions) {
        this.name = name;
        this.numOfReplicas = numOfReplicas;
        this.numOfPartitions = numOfPartitions;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Integer getNumOfPartitions() {
        return numOfPartitions;
    }

    public Integer getNumOfReplicas() {
        return numOfReplicas;
    }

    public void setNumOfPartitions(Integer numOfPartitions) {
        this.numOfPartitions = numOfPartitions;
    }

    public void setNumOfReplicas(Integer numOfReplicas) {
        this.numOfReplicas = numOfReplicas;
    }

}
