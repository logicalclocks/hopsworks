package se.kth.bbc.jobs.jobhistory;

import java.io.Serializable;
import javax.persistence.EmbeddedId;
import javax.persistence.Entity;
import javax.persistence.JoinColumn;
import javax.persistence.JoinColumns;
import javax.persistence.ManyToOne;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.OneToOne;
import javax.persistence.Table;
import javax.xml.bind.annotation.XmlRootElement;
import se.kth.bbc.project.fb.Inode;

@Entity
@Table(name = "executions_inputfiles", catalog = "hopsworks", schema = "")
@XmlRootElement
@NamedQueries({
    @NamedQuery(name = "ExecutionsInputfiles.findAll", query = "SELECT e FROM ExecutionsInputfiles e"),
    @NamedQuery(name = "ExecutionsInputfiles.findByExecutionId", query = "SELECT e FROM ExecutionsInputfiles e WHERE e.execution.id = :id"),
    @NamedQuery(name = "ExecutionsInputfiles.findByInodePid", query = "SELECT e FROM ExecutionsInputfiles e WHERE e.inode.inodePK.parentId = :parent_id"),
    @NamedQuery(name = "ExecutionsInputfiles.findByInodeName", query = "SELECT e FROM ExecutionsInputfiles e WHERE e.inode.inodePK.name = :name")})

public class ExecutionsInputfiles implements Serializable {
    
    private static final long serialVersionUID = 1L;
    
    @EmbeddedId
    protected ExecutionsInputfilesPK executionsInputfilesPK;
    
    @JoinColumn(name = "execution_id",
          referencedColumnName = "id",
          insertable = false,
          updatable
          = false)
    @OneToOne(optional = false)
    private Execution execution;
    
    @JoinColumns({
    @JoinColumn(name = "inode_pid",
            referencedColumnName = "parent_id",
            insertable = false,
            updatable = false),
    @JoinColumn(name = "inode_name",
            referencedColumnName = "name",
            insertable = false,
            updatable = false)
    })
    @ManyToOne(optional = false)
    private Inode inode;

    
    public ExecutionsInputfiles() {
    }

    public ExecutionsInputfiles(ExecutionsInputfilesPK executionsInputfilesPK) {
        this.executionsInputfilesPK = executionsInputfilesPK;
    }

    public ExecutionsInputfiles(int executionId, int inodeId, String name) {
        this.executionsInputfilesPK = new ExecutionsInputfilesPK(executionId, inodeId, name);
    }

    public ExecutionsInputfilesPK getExecutionsInputfilesPK() {
        return executionsInputfilesPK;
    }

    public void setExecutionsInputfilesPK(ExecutionsInputfilesPK executionsInputfilesPK) {
        this.executionsInputfilesPK = executionsInputfilesPK;
    }

    @Override
    public int hashCode() {
        int hash = 0;
        hash += (executionsInputfilesPK != null ? executionsInputfilesPK.hashCode() : 0);
        return hash;
    }

    @Override
    public boolean equals(Object object) {
        // TODO: Warning - this method won't work in the case the id fields are not set
        if (!(object instanceof ExecutionsInputfiles)) {
            return false;
        }
        ExecutionsInputfiles other = (ExecutionsInputfiles) object;
        if ((this.executionsInputfilesPK == null && other.executionsInputfilesPK != null) || (this.executionsInputfilesPK != null && !this.executionsInputfilesPK.equals(other.executionsInputfilesPK))) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return "se.kth.bbc.jobs.jobhistory.ExecutionsInputfiles[ executionsInputfilesPK=" + executionsInputfilesPK + " ]";
    }
    
}
