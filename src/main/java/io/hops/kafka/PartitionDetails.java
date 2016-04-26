/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package io.hops.kafka;

import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author misdess
 */
public class PartitionDetails {

    private int id;
    private String paritionLeader;
    private List<String> partitionReplicas = new ArrayList<>();
    private List<String> inSyncReplicas = new ArrayList<>();

    public PartitionDetails() {
    }

    public PartitionDetails(int id, String paritionLeader,
            List<String> partitionReplicas, List<String> inSyncReplicas) {
        this.id = id;
        this.paritionLeader = paritionLeader;
        this.partitionReplicas = partitionReplicas;
        this.inSyncReplicas = inSyncReplicas;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getParitionLeader() {
        return paritionLeader;
    }

    public void setParitionLeader(String paritionLeader) {
        this.paritionLeader = paritionLeader;
    }

    public List<String> getPartitionReplicas() {
        return partitionReplicas;
    }

    public void setPartitionReplicas(List<String> partitionReplicas) {
        this.partitionReplicas = partitionReplicas;
    }

    public List<String> getInSyncReplicas() {
        return inSyncReplicas;
    }

    public void setInSyncReplicas(List<String> inSyncReplicas) {
        this.inSyncReplicas = inSyncReplicas;
    }

}
