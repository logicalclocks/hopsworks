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
    @NamedQuery(name = "Workflow.findAll", query = "SELECT w FROM Workflow w"),
    @NamedQuery(name = "Workflow.findByTitle", query = "SELECT w FROM Workflow w WHERE w.workflowPK.title = :title"),
    @NamedQuery(name = "Workflow.findByStudy", query = "SELECT w FROM Workflow w WHERE w.workflowPK.study = :study"),
    @NamedQuery(name = "Workflow.findByCreationDate", query = "SELECT w FROM Workflow w WHERE w.creationDate = :creationDate"),
    @NamedQuery(name = "Workflow.findByPublicFile", query = "SELECT w FROM Workflow w WHERE w.publicFile = :publicFile"),
    @NamedQuery(name = "Workflow.findByCreator", query = "SELECT w FROM Workflow w WHERE w.creator = :creator"),
    @NamedQuery(name = "Workflow.findByStudyOrPublic", query = "SELECT w FROM Workflow w WHERE w.workflowPK.study = :study OR w.publicFile=TRUE")})
public class Workflow implements Serializable {
    private static final long serialVersionUID = 1L;
    @EmbeddedId
    protected WorkflowPK workflowPK;
    @Lob
    @Size(max = 65535)
    private String description;
    @Basic(optional = false)
    @NotNull
    @Temporal(TemporalType.TIMESTAMP)
    private Date creationDate;
    @Basic(optional = false)
    @NotNull
    private boolean publicFile;
    @Basic(optional = false)
    @NotNull
    @Size(min = 1, max = 255)
    private String creator;


    public Workflow() {
    }

    public Workflow(WorkflowPK workflowPK) {
        this.workflowPK = workflowPK;
    }

    public Workflow(WorkflowPK workflowPK, Date creationDate, boolean publicFile, String creator) {
        this.workflowPK = workflowPK;
        this.creationDate = creationDate;
        this.publicFile = publicFile;
        this.creator = creator;
    }

    public Workflow(String title, String study) {
        this.workflowPK = new WorkflowPK(title, study);
    }

    public WorkflowPK getWorkflowPK() {
        return workflowPK;
    }

    public void setWorkflowPK(WorkflowPK workflowPK) {
        this.workflowPK = workflowPK;
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

    public boolean getPublicFile() {
        return publicFile;
    }

    public void setPublicFile(boolean publicFile) {
        this.publicFile = publicFile;
    }

    public String getCreator() {
        return creator;
    }

    public void setCreator(String creator) {
        this.creator = creator;
    }

    @Override
    public int hashCode() {
        int hash = 0;
        hash += (workflowPK != null ? workflowPK.hashCode() : 0);
        return hash;
    }

    @Override
    public boolean equals(Object object) {
        // TODO: Warning - this method won't work in the case the id fields are not set
        if (!(object instanceof Workflow)) {
            return false;
        }
        Workflow other = (Workflow) object;
        if ((this.workflowPK == null && other.workflowPK != null) || (this.workflowPK != null && !this.workflowPK.equals(other.workflowPK))) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return "se.kth.bbc.workflows.Workflow[ workflowPK=" + workflowPK + " ]";
    }
    
}
