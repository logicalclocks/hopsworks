/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package se.kth.kthfsdashboard.wf;

import java.io.Serializable;

/**
 *
 * @author Alberto Lorente Leal <albll@kth.se>
 */
public class Workflow implements Serializable{
    private String owner;
    private String workflowName;
    private String workflowDate;
    private String workflowTags;
    private String workflowMetadata;

    public Workflow() {
    }

    public Workflow(String owner, String workflowName, String workflowDate, String workflowTags, String Metadata) {
        this.owner = owner;
        this.workflowName = workflowName;
        this.workflowDate = workflowDate;
        this.workflowTags = workflowTags;
        this.workflowMetadata = Metadata;
    }

    public String getWorkflowTags() {
        return workflowTags;
    }

    public void setWorkflowTags(String workflowContents) {
        this.workflowTags = workflowContents;
    }

    public String getOwner() {
        return owner;
    }

    public void setOwner(String owner) {
        this.owner = owner;
    }

    public String getWorkflowName() {
        return workflowName;
    }

    public void setWorkflowName(String workflowName) {
        this.workflowName = workflowName;
    }

    public String getWorkflowDate() {
        return workflowDate;
    }

    public void setWorkflowDate(String workflowDate) {
        this.workflowDate = workflowDate;
    }

    public String getWorkflowMetadata() {
        return workflowMetadata;
    }

    public void setWorkflowMetadata(String workflowMetadata) {
        this.workflowMetadata = workflowMetadata;
    }
    
}
