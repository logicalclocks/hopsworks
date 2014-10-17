/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package se.kth.bbc.workflows;

import java.io.Serializable;
import java.util.Date;
import javax.persistence.Basic;
import javax.persistence.Column;
import javax.persistence.EmbeddedId;
import javax.persistence.Entity;
import javax.persistence.Lob;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import javax.xml.bind.annotation.XmlRootElement;

/**
 *
 * @author stig
 */
@Entity
@Table(name = "Workflows")
@XmlRootElement
@NamedQueries({
    @NamedQuery(name = "Workflows.findAll", query = "SELECT w FROM Workflow w"),
    @NamedQuery(name = "Workflows.findByTitle", query = "SELECT w FROM Workflow w WHERE w.workflowsPK.title = :title"),
    @NamedQuery(name = "Workflows.findByStudy", query = "SELECT w FROM Workflow w WHERE w.workflowsPK.study = :study"),
    @NamedQuery(name = "Workflows.findByCreationDate", query = "SELECT w FROM Workflow w WHERE w.creationDate = :creationDate")})
public class Workflow implements Serializable {
    private static final long serialVersionUID = 1L;
    @EmbeddedId
    protected WorkflowsPK workflowsPK;
    @Lob
    @Size(max = 65535)
    @Column(name = "description")
    private String description;
    @Basic(optional = false)
    @NotNull
    @Column(name = "creationDate")
    @Temporal(TemporalType.TIMESTAMP)
    private Date creationDate;
    @Basic(optional = false)
    @NotNull
    @Lob
    @Size(min = 1, max = 2147483647)
    @Column(name = "script")
    private String script;

    public Workflow() {
    }

    public Workflow(WorkflowsPK workflowsPK) {
        this.workflowsPK = workflowsPK;
    }

    public Workflow(WorkflowsPK workflowsPK, Date creationDate, String script) {
        this.workflowsPK = workflowsPK;
        this.creationDate = creationDate;
        this.script = script;
    }

    public Workflow(String title, String study) {
        this.workflowsPK = new WorkflowsPK(title, study);
    }

    public WorkflowsPK getWorkflowsPK() {
        return workflowsPK;
    }

    public void setWorkflowsPK(WorkflowsPK workflowsPK) {
        this.workflowsPK = workflowsPK;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Date getCreationDate() {
        return creationDate;
    }

    public void setCreationDate(Date creationDate) {
        this.creationDate = creationDate;
    }

    public String getScript() {
        return script;
    }

    public void setScript(String script) {
        this.script = script;
    }

    @Override
    public int hashCode() {
        int hash = 0;
        hash += (workflowsPK != null ? workflowsPK.hashCode() : 0);
        return hash;
    }

    @Override
    public boolean equals(Object object) {
        // TODO: Warning - this method won't work in the case the id fields are not set
        if (!(object instanceof Workflow)) {
            return false;
        }
        Workflow other = (Workflow) object;
        if ((this.workflowsPK == null && other.workflowsPK != null) || (this.workflowsPK != null && !this.workflowsPK.equals(other.workflowsPK))) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return "se.kth.bbc.workflows.Workflows[ workflowsPK=" + workflowsPK + " ]";
    }
    
    /**
     * Get the title of this record. Avoids going through the workflowsPK. 
     */
    public String getTitle(){
        return workflowsPK.getTitle();
    }
    
     /**
     * Get the study of this record. Avoids going through the workflowsPK. 
     */
    public String getStudy(){
        return workflowsPK.getStudy();
    }
    
}
