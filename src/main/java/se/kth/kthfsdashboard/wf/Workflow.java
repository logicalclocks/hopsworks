/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package se.kth.kthfsdashboard.wf;

import java.io.Serializable;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.Table;

/**
 *
 * @author Alberto Lorente Leal <albll@kth.se>
 */
@Entity
@Table(name="Workflows")
@NamedQueries({
    @NamedQuery(name = "Workflow.findAll", query = "SELECT c FROM Workflow c")
})

public class Workflow implements Serializable{
    
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;
    private String owner;
    private String workflowName;
    private String workflowDate;
    private String workflowTags;
    @Column(columnDefinition = "text")
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

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
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
