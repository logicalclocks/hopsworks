package se.kth.bbc.jobs.jobhistory;

import java.io.Serializable;
import javax.persistence.Basic;
import javax.persistence.Column;
import javax.persistence.Embeddable;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

@Embeddable
public class ExecutionsInputfilesPK implements Serializable {
    
  @Basic(optional = false)
  @NotNull
  @Column(name = "execution_id")
  private int executionId;
  
  @Basic(optional = false)
  @NotNull
  @Column(name = "inode_pid")
  private int inodePid;

  @Basic(optional = false)
  @NotNull
  @Size(min = 1,
          max = 255)
  @Column(name = "inode_name")
  private String name;

  public ExecutionsInputfilesPK() {
  }

  public ExecutionsInputfilesPK(int executionId, int inodePid, String name) {
    this.executionId = executionId;
    this.inodePid = inodePid;
    this.name = name;           
  }

    public int getExecutionId() {
        return executionId;
    }

    public void setExecutionId(int executionId) {
        this.executionId = executionId;
    }

    public int getInodePid() {
        return inodePid;
    }

    public void setInodePid(int inodePid) {
        this.inodePid = inodePid;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

  
   @Override
  public int hashCode() {
    int hash = 0;
    hash += executionId;
    hash += inodePid;
    hash += (name != null ? name.hashCode() : 0);
    return hash;
  }

  @Override
  public boolean equals(Object object) {
    if (!(object instanceof JobOutputFilePK)) {
      return false;
    }
    ExecutionsInputfilesPK other = (ExecutionsInputfilesPK) object;
    if (this.executionId != other.executionId) {
      return false;
    }
    if (this.inodePid != other.inodePid) {
      return false;
    }
    if ((this.name == null && other.name != null) || (this.name != null
            && !this.name.equals(other.name))) {
      return false;
    }
    return true;
  }

  @Override
  public String toString() {
    return "se.kth.bbc.jobs.history.ExecutionsInputfilesPK[ executionId=" + executionId + 
            ", inodePid="+ inodePid + 
            ", name="+ name +
            "]";
  }
}

