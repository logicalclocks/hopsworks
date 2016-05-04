/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package io.hops.kafka;

import java.util.ArrayList;
import java.util.List;
import javax.xml.bind.annotation.XmlRootElement;

/**
 *
 * @author misdess
 */

@XmlRootElement
public class PartitionDetailsDTO {

    private int id;
    private String leader;
    private List<String> replicas = new ArrayList<>();
    private List<String> inSyncReplicas = new ArrayList<>();

    public PartitionDetailsDTO() {
    }

    public PartitionDetailsDTO(int id, String paritionLeader,
            List<String> replicas, List<String> inSyncReplicas) {
        this.id = id;
        this.leader = paritionLeader;
        this.replicas = replicas;
        this.inSyncReplicas = inSyncReplicas;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getLeader() {
        return leader;
    }

    public void setLeader(String leader) {
        this.leader = leader;
    }

    public List<String> getReplicas() {
        return replicas;
    }

    public void setReplicas(List<String> partitionReplicas) {
        this.replicas = partitionReplicas;
    }

    public List<String> getInSyncReplicas() {
        return inSyncReplicas;
    }

    public void setInSyncReplicas(List<String> inSyncReplicas) {
        this.inSyncReplicas = inSyncReplicas;
    }

}
