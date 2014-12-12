package se.kth.bbc.jobhistory;

import java.io.Serializable;
import javax.persistence.Basic;
import javax.persistence.Column;
import javax.persistence.EmbeddedId;
import javax.persistence.Entity;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.Table;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import javax.xml.bind.annotation.XmlRootElement;

/**
 *
 * @author stig
 */
@Entity
@Table(name = "job_input_files")
@XmlRootElement
@NamedQueries({
  @NamedQuery(name = "JobInputFile.findAll",
          query
          = "SELECT j FROM JobInputFile j"),
  @NamedQuery(name = "JobInputFile.findByJobId",
          query
          = "SELECT j FROM JobInputFile j WHERE j.jobInputFilePK.jobId = :jobId"),
  @NamedQuery(name = "JobInputFile.findByPath",
          query
          = "SELECT j FROM JobInputFile j WHERE j.path = :path"),
  @NamedQuery(name = "JobInputFile.findByName",
          query
          = "SELECT j FROM JobInputFile j WHERE j.jobInputFilePK.name = :name")})
public class JobInputFile implements Serializable {
  private static final long serialVersionUID = 1L;
  @EmbeddedId
  protected JobInputFilePK jobInputFilePK;
  @Basic(optional = false)
  @NotNull
  @Size(min = 1,
          max = 255)
  @Column(name = "path")
  private String path;
  @JoinColumn(name = "job_id",
          referencedColumnName = "id",
          insertable = false,
          updatable
          = false)
  @ManyToOne(optional = false)
  private JobHistory jobHistory;

  public JobInputFile() {
  }

  public JobInputFile(JobInputFilePK jobInputFilePK) {
    this.jobInputFilePK = jobInputFilePK;
  }

  public JobInputFile(JobInputFilePK jobInputFilePK, String path) {
    this.jobInputFilePK = jobInputFilePK;
    this.path = path;
  }

  public JobInputFile(long jobId, String name) {
    this.jobInputFilePK = new JobInputFilePK(jobId, name);
  }

  public JobInputFilePK getJobInputFilePK() {
    return jobInputFilePK;
  }

  public void setJobInputFilePK(JobInputFilePK jobInputFilePK) {
    this.jobInputFilePK = jobInputFilePK;
  }

  public String getPath() {
    return path;
  }

  public void setPath(String path) {
    this.path = path;
  }

  public JobHistory getJobHistory() {
    return jobHistory;
  }

  public void setJobHistory(JobHistory jobHistory) {
    this.jobHistory = jobHistory;
  }

  @Override
  public int hashCode() {
    int hash = 0;
    hash += (jobInputFilePK != null ? jobInputFilePK.hashCode() : 0);
    return hash;
  }

  @Override
  public boolean equals(Object object) {
    // TODO: Warning - this method won't work in the case the id fields are not set
    if (!(object instanceof JobInputFile)) {
      return false;
    }
    JobInputFile other = (JobInputFile) object;
    if ((this.jobInputFilePK == null && other.jobInputFilePK != null) ||
            (this.jobInputFilePK != null &&
            !this.jobInputFilePK.equals(other.jobInputFilePK))) {
      return false;
    }
    return true;
  }

  @Override
  public String toString() {
    return "se.kth.bbc.job.JobInputFile[ jobInputFilePK=" + jobInputFilePK + " ]";
  }
  
}
