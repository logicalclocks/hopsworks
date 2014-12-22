package se.kth.bbc.jobs.jobhistory;

import java.io.Serializable;
import java.math.BigInteger;
import java.util.Collection;
import java.util.Date;
import javax.persistence.Basic;
import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.OneToMany;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlTransient;
import org.apache.hadoop.yarn.api.records.YarnApplicationState;
import org.codehaus.jackson.annotate.JsonIgnore;
import se.kth.bbc.study.TrackStudy;
import se.kth.kthfsdashboard.user.Username;

/**
 *
 * @author stig
 */
@Entity
@Table(name = "jobhistory")
@XmlRootElement
@NamedQueries({
  @NamedQuery(name = "JobHistory.findAll",
          query
          = "SELECT j FROM JobHistory j"),
  @NamedQuery(name = "JobHistory.findById",
          query
          = "SELECT j FROM JobHistory j WHERE j.id = :id"),
  @NamedQuery(name = "JobHistory.findByName",
          query
          = "SELECT j FROM JobHistory j WHERE j.name = :name"),
  @NamedQuery(name = "JobHistory.findBySubmissionTime",
          query
          = "SELECT j FROM JobHistory j WHERE j.submissionTime = :submissionTime"),
  @NamedQuery(name = "JobHistory.findByState",
          query
          = "SELECT j FROM JobHistory j WHERE j.state = :state"),
  @NamedQuery(name = "JobHistory.findByExecutionDuration",
          query
          = "SELECT j FROM JobHistory j WHERE j.executionDuration = :executionDuration"),
  @NamedQuery(name = "JobHistory.findByArgs",
          query
          = "SELECT j FROM JobHistory j WHERE j.args = :args"),
  @NamedQuery(name = "JobHistory.findByStdoutPath",
          query
          = "SELECT j FROM JobHistory j WHERE j.stdoutPath = :stdoutPath"),
  @NamedQuery(name = "JobHistory.findByStderrPath",
          query
          = "SELECT j FROM JobHistory j WHERE j.stderrPath = :stderrPath"),
  @NamedQuery(name = "JobHistory.findByType",
          query
          = "SELECT j FROM JobHistory j WHERE j.type = :type"),
  @NamedQuery(name = "JobHistory.findByStudyAndType",
          query
          = "SELECT j FROM JobHistory j WHERE j.type = :type AND j.study.name = :studyname ORDER BY j.submissionTime DESC")})
public class JobHistory implements Serializable {

  public static final String STATE_FINISHED = YarnApplicationState.FINISHED.toString();
  public static final String STATE_RUNNING = YarnApplicationState.RUNNING.toString();
  public static final String STATE_ACCEPTED = YarnApplicationState.ACCEPTED.toString();
  public static final String STATE_FAILED = YarnApplicationState.FAILED.toString();
  public static final String STATE_KILLED = YarnApplicationState.KILLED.toString();
  public static final String STATE_NEW = YarnApplicationState.NEW.toString();
  public static final String STATE_NEW_SAVING = YarnApplicationState.NEW_SAVING.toString();
  public static final String STATE_SUBMITTED = YarnApplicationState.SUBMITTED.toString();
  public static final String STATE_FRAMEWORK_FAILURE = "Framework Failure";
  

  private static final long serialVersionUID = 1L;
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Basic(optional = false)
  @Column(name = "id")
  private Long id;
  @Size(max = 128)
  @Column(name = "name")
  private String name;
  @Basic(optional = false)
  @NotNull
  @Column(name = "submission_time")
  @Temporal(TemporalType.TIMESTAMP)
  private Date submissionTime;
  @Basic(optional = false)
  @NotNull
  @Size(min = 1,
          max = 128)
  @Column(name = "state")
  private String state;
  @Column(name = "execution_duration")
  private BigInteger executionDuration;
  @Size(max = 255)
  @Column(name = "args")
  private String args;
  @Size(max = 255)
  @Column(name = "stdout_path")
  private String stdoutPath;
  @Size(max = 255)
  @Column(name = "stderr_path")
  private String stderrPath;
  @Size(max = 128)
  @Column(name = "type")
  private String type;
  @OneToMany(cascade = CascadeType.ALL,
          mappedBy = "jobHistory")
  private Collection<JobOutputFile> jobOutputFileCollection;
  @JoinColumn(name = "user",
          referencedColumnName = "EMAIL")
  @ManyToOne(optional = false)
  private Username user;
  @JoinColumn(name = "study",
          referencedColumnName = "name")
  @ManyToOne(optional = false)
  private TrackStudy study;
  @OneToMany(cascade = CascadeType.ALL,
          mappedBy = "jobHistory")
  private Collection<JobInputFile> jobInputFileCollection;
  @OneToMany(cascade = CascadeType.ALL,
          mappedBy = "jobHistory")
  private Collection<JobExecutionFile> jobExecutionFileCollection;

  public JobHistory() {
  }

  public JobHistory(Date submissionTime, String state) {
    this.submissionTime = submissionTime;
    this.state = state;
  }

  public Long getId() {
    return id;
  }

  public void setId(Long id) {
    this.id = id;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public Date getSubmissionTime() {
    return submissionTime;
  }

  public void setSubmissionTime(Date submissionTime) {
    this.submissionTime = submissionTime;
  }

  public String getState() {
    return state;
  }

  public void setState(String state) {
    this.state = state;
  }

  public BigInteger getExecutionDuration() {
    return executionDuration;
  }

  public void setExecutionDuration(BigInteger executionDuration) {
    this.executionDuration = executionDuration;
  }

  public String getArgs() {
    return args;
  }

  public void setArgs(String args) {
    this.args = args;
  }

  public String getStdoutPath() {
    return stdoutPath;
  }

  public void setStdoutPath(String stdoutPath) {
    this.stdoutPath = stdoutPath;
  }

  public String getStderrPath() {
    return stderrPath;
  }

  public void setStderrPath(String stderrPath) {
    this.stderrPath = stderrPath;
  }

  public String getType() {
    return type;
  }

  public void setType(String type) {
    this.type = type;
  }

  @XmlTransient
  @JsonIgnore
  public Collection<JobOutputFile> getJobOutputFileCollection() {
    return jobOutputFileCollection;
  }

  public void setJobOutputFileCollection(
          Collection<JobOutputFile> jobOutputFileCollection) {
    this.jobOutputFileCollection = jobOutputFileCollection;
  }

  public Username getUser() {
    return user;
  }

  public void setUser(Username user) {
    this.user = user;
  }

  public TrackStudy getStudy() {
    return study;
  }

  public void setStudy(TrackStudy study) {
    this.study = study;
  }

  @XmlTransient
  @JsonIgnore
  public Collection<JobInputFile> getJobInputFileCollection() {
    return jobInputFileCollection;
  }

  public void setJobInputFileCollection(
          Collection<JobInputFile> jobInputFileCollection) {
    this.jobInputFileCollection = jobInputFileCollection;
  }

  @XmlTransient
  @JsonIgnore
  public Collection<JobExecutionFile> getJobExecutionFileCollection() {
    return jobExecutionFileCollection;
  }

  public void setJobExecutionFileCollection(
          Collection<JobExecutionFile> jobExecutionFileCollection) {
    this.jobExecutionFileCollection = jobExecutionFileCollection;
  }

  @Override
  public int hashCode() {
    int hash = 0;
    hash += (id != null ? id.hashCode() : 0);
    return hash;
  }

  @Override
  public boolean equals(Object object) {
    // TODO: Warning - this method won't work in the case the id fields are not set
    if (!(object instanceof JobHistory)) {
      return false;
    }
    JobHistory other = (JobHistory) object;
    if ((this.id == null && other.id != null) || (this.id != null && !this.id.
            equals(other.id))) {
      return false;
    }
    return true;
  }

  @Override
  public String toString() {
    return "se.kth.bbc.job.JobHistory[ id=" + id + " ]";
  }

  public boolean isFinished() {
    return STATE_FINISHED.equals(this.state);
  }

}
