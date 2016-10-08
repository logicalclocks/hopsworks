/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package io.hops.kafka;

import java.io.Serializable;
import javax.xml.bind.annotation.XmlRootElement;

/**
 *
 * @author misdess
 */
@XmlRootElement
public class TopicDTO implements Serializable {

    private String name;

    private Integer numOfReplicas;
    
    private Integer numOfPartitions;

    private String schemaName;
    
    private int schemaVersion;
    
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

    public TopicDTO(String name, String schemaName, int schemaVersion) {
        this.name = name;
        this.schemaName = schemaName;
        this.schemaVersion = schemaVersion;
    }
    
    public TopicDTO(String name, Integer numOfReplicas, Integer numOfPartitions, String schemaName, int schemaVersion) {
        this.name = name;
        this.numOfReplicas = numOfReplicas;
        this.numOfPartitions = numOfPartitions;
        this.schemaName = schemaName;
        this.schemaVersion = schemaVersion;
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

    public String getSchemaName() {
        return schemaName;
    }

    public int getSchemaVersion() {
        return schemaVersion;
    }

    public void setSchemaName(String schemaName) {
        this.schemaName = schemaName;
    }

    public void setSchemaVersion(int schemaVersion) {
        this.schemaVersion = schemaVersion;
    }
}
