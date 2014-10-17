/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package se.kth.hop.wf;

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
@Table(name="WorkflowsOld")
@NamedQueries({
    @NamedQuery(name = "WorkflowOld.findAll", query = "SELECT c FROM WorkflowOld c")
})

public class WorkflowOld implements Serializable{

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;
    private String owner;
    private String workflowName;
    private String workflowDate;
    private String workflowTags;
    @Column(columnDefinition = "text")
    private String workflowMetadata;

    public WorkflowOld() {
    }

    public WorkflowOld(String owner, String workflowName, String workflowDate, String workflowTags, String Metadata) {
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

    @Override
    public int hashCode() {
        int hash = 5;
        hash = 67 * hash + (this.owner != null ? this.owner.hashCode() : 0);
        hash = 67 * hash + (this.workflowName != null ? this.workflowName.hashCode() : 0);
        hash = 67 * hash + (this.workflowDate != null ? this.workflowDate.hashCode() : 0);
        hash = 67 * hash + (this.workflowTags != null ? this.workflowTags.hashCode() : 0);
        hash = 67 * hash + (this.workflowMetadata != null ? this.workflowMetadata.hashCode() : 0);
        return hash;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final WorkflowOld other = (WorkflowOld) obj;
        if ((this.owner == null) ? (other.owner != null) : !this.owner.equals(other.owner)) {
            return false;
        }
        if ((this.workflowName == null) ? (other.workflowName != null) : !this.workflowName.equals(other.workflowName)) {
            return false;
        }
        if ((this.workflowDate == null) ? (other.workflowDate != null) : !this.workflowDate.equals(other.workflowDate)) {
            return false;
        }
        if ((this.workflowTags == null) ? (other.workflowTags != null) : !this.workflowTags.equals(other.workflowTags)) {
            return false;
        }
        if ((this.workflowMetadata == null) ? (other.workflowMetadata != null) : !this.workflowMetadata.equals(other.workflowMetadata)) {
            return false;
        }
        return true;
    }
    
    
}
