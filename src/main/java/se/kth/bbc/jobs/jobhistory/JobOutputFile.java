package se.kth.bbc.jobs.jobhistory;

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
import javax.xml.bind.annotation.XmlTransient;
import org.codehaus.jackson.annotate.JsonIgnore;

/**
 *
 * @author stig
 */
@Entity
@Table(name = "hopsworks.job_output_files")
@XmlRootElement
@NamedQueries({
  @NamedQuery(name = "JobOutputFile.findAll",
          query
          = "SELECT j FROM JobOutputFile j"),
  @NamedQuery(name = "JobOutputFile.findByJobId",
          query
          = "SELECT j FROM JobOutputFile j WHERE j.jobOutputFilePK.jobId = :jobId"),
  @NamedQuery(name = "JobOutputFile.findByName",
          query
          = "SELECT j FROM JobOutputFile j WHERE j.jobOutputFilePK.name = :name"),
  @NamedQuery(name = "JobOutputFile.findByPath",
          query
          = "SELECT j FROM JobOutputFile j WHERE j.path = :path"),
  @NamedQuery(name = "JobOutputFile.findByNameAndJobId",
          query
          = "SELECT j FROM JobOutputFile j WHERE j.jobOutputFilePK.name = :name AND j.jobOutputFilePK.jobId = :jobId")})
public class JobOutputFile implements Serializable {

  private static final long serialVersionUID = 1L;

  @EmbeddedId
  protected JobOutputFilePK jobOutputFilePK;

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

  public JobOutputFile() {
  }

  public JobOutputFile(JobOutputFilePK jobOutputFilePK) {
    this.jobOutputFilePK = jobOutputFilePK;
  }

  public JobOutputFile(JobOutputFilePK jobOutputFilePK, String path) {
    this.jobOutputFilePK = jobOutputFilePK;
    this.path = path;
  }

  public JobOutputFile(long jobId, String name) {
    this.jobOutputFilePK = new JobOutputFilePK(jobId, name);
  }

  public JobOutputFilePK getJobOutputFilePK() {
    return jobOutputFilePK;
  }

  public void setJobOutputFilePK(JobOutputFilePK jobOutputFilePK) {
    this.jobOutputFilePK = jobOutputFilePK;
  }

  public String getPath() {
    return path;
  }

  public void setPath(String path) {
    this.path = path;
  }

  @XmlTransient
  @JsonIgnore
  public JobHistory getJobHistory() {
    return jobHistory;
  }

  public void setJobHistory(JobHistory jobHistory) {
    this.jobHistory = jobHistory;
  }

  @Override
  public int hashCode() {
    int hash = 0;
    hash += (jobOutputFilePK != null ? jobOutputFilePK.hashCode() : 0);
    return hash;
  }

  @Override
  public boolean equals(Object object) {
    // TODO: Warning - this method won't work in the case the id fields are not set
    if (!(object instanceof JobOutputFile)) {
      return false;
    }
    JobOutputFile other = (JobOutputFile) object;
    if ((this.jobOutputFilePK == null && other.jobOutputFilePK != null)
            || (this.jobOutputFilePK != null && !this.jobOutputFilePK.equals(
                    other.jobOutputFilePK))) {
      return false;
    }
    return true;
  }

  @Override
  public String toString() {
    return "se.kth.bbc.job.JobOutputFile[ jobOutputFilePK=" + jobOutputFilePK
            + " ]";
  }

}
