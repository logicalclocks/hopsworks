package se.kth.bbc.jobs.jobhistory;

import se.kth.bbc.jobs.model.description.JobDescription;
import java.io.Serializable;
import java.util.Collection;
import java.util.Date;
import javax.persistence.Basic;
import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
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
import org.codehaus.jackson.annotate.JsonIgnore;
import se.kth.hopsworks.user.model.Users;

/**
 * An Execution is an instance of execution of a specific JobDescription.
 * <p>
 * @author stig
 */
@Entity
@Table(name = "hopsworks.executions")
@XmlRootElement
@NamedQueries({
  @NamedQuery(name = "Execution.findAll",
          query
          = "SELECT e FROM Execution e"),
  @NamedQuery(name = "Execution.findById",
          query
          = "SELECT e FROM Execution e WHERE e.id = :id"),
  @NamedQuery(name = "Execution.findBySubmissionTime",
          query
          = "SELECT e FROM Execution e WHERE e.submissionTime = :submissionTime"),
  @NamedQuery(name = "Execution.findByState",
          query
          = "SELECT e FROM Execution e WHERE e.state = :state"),
  @NamedQuery(name = "Execution.findByExecutionDuration",
          query
          = "SELECT e FROM Execution e WHERE e.executionDuration = :executionDuration"),
  @NamedQuery(name = "Execution.findByStdoutPath",
          query
          = "SELECT e FROM Execution e WHERE e.stdoutPath = :stdoutPath"),
  @NamedQuery(name = "Execution.findByStderrPath",
          query
          = "SELECT e FROM Execution e WHERE e.stderrPath = :stderrPath"),
  @NamedQuery(name = "Execution.findByAppId",
          query
          = "SELECT e FROM Execution e WHERE e.appId = :appId"),
  @NamedQuery(name = "Execution.findByProjectAndType",
          query
          = "SELECT e FROM Execution e WHERE e.job.type = :type AND e.job.project = :project ORDER BY e.submissionTime DESC"),
  @NamedQuery(name = "Execution.findByJob",
          query
          = "SELECT e FROM Execution e WHERE e.job = :job ORDER BY e.submissionTime DESC")})
public class Execution implements Serializable {

  private static final long serialVersionUID = 1L;

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Basic(optional = false)
  @Column(name = "id")
  private Integer id;

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
  @Enumerated(EnumType.STRING)
  private JobState state;

  @Column(name = "execution_duration")
  private long executionDuration;

  @Size(max = 255)
  @Column(name = "stdout_path")
  private String stdoutPath;

  @Size(max = 255)
  @Column(name = "stderr_path")
  private String stderrPath;

  @Size(max = 30)
  @Column(name = "app_id")
  private String appId;

  @JoinColumn(name = "job_id",
          referencedColumnName = "id")
  @ManyToOne(optional = false)
  private JobDescription job;

  @JoinColumn(name = "user",
          referencedColumnName = "email")
  @ManyToOne(optional = false)
  private Users user;

  @OneToMany(cascade = CascadeType.ALL,
          mappedBy = "execution")
  private Collection<JobOutputFile> jobOutputFileCollection;

  @OneToMany(cascade = CascadeType.ALL,
          mappedBy = "execution")
  private Collection<JobInputFile> jobInputFileCollection;

  public Execution() {
  }

  public Execution(JobState state, JobDescription job, Users user) {
    this(state, job, user, new Date());
  }

  public Execution(JobState state, JobDescription job, Users user, Date submissionTime) {
    this(state, job, user, submissionTime, null, null);
  }

  public Execution(JobState state, JobDescription job, Users user, String stdoutPath,
          String stderrPath) {
    this(state, job, user, new Date(), stdoutPath, stderrPath);
  }

  public Execution(JobState state, JobDescription job, Users user, Date submissionTime,
          String stdoutPath, String stderrPath) {
    this(state, job, user, submissionTime, stdoutPath, stderrPath, null);
  }

  public Execution(JobState state, JobDescription job, Users user, String stdoutPath,
          String stderrPath, Collection<JobInputFile> input) {
    this(state, job, user, new Date(), stdoutPath, stderrPath, input);
  }

  public Execution(Execution t) {
    this(t.state, t.job, t.user, t.submissionTime, t.stdoutPath, t.stderrPath,
            t.jobInputFileCollection);
    this.id = t.id;
  }

  public Execution(JobState state, JobDescription job, Users user, Date submissionTime,
          String stdoutPath, String stderrPath, Collection<JobInputFile> input) {
    this.submissionTime = submissionTime;
    this.state = state;
    this.stdoutPath = stdoutPath;
    this.stderrPath = stderrPath;
    this.job = job;
    this.user = user;
    this.jobInputFileCollection = input;
  }

  public Integer getId() {
    return id;
  }

  public void setId(Integer id) {
    this.id = id;
  }

  public Date getSubmissionTime() {
    return submissionTime;
  }

  public void setSubmissionTime(Date submissionTime) {
    this.submissionTime = submissionTime;
  }

  public JobState getState() {
    return state;
  }

  public void setState(JobState state) {
    this.state = state;
  }

  public long getExecutionDuration() {
    return executionDuration;
  }

  public void setExecutionDuration(long executionDuration) {
    this.executionDuration = executionDuration;
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

  public String getAppId() {
    return appId;
  }

  public void setAppId(String appId) {
    this.appId = appId;
  }

  public JobDescription getJob() {
    return job;
  }

  public void setJob(JobDescription job) {
    this.job = job;
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
    if (!(object instanceof Execution)) {
      return false;
    }
    Execution other = (Execution) object;
    if ((this.id == null && other.id != null) || (this.id != null && !this.id.
            equals(other.id))) {
      return false;
    }
    return true;
  }

  @Override
  public String toString() {
    return "Execution " + id + " of job " + job;
  }

  public Users getUser() {
    return user;
  }

  public void setUser(Users user) {
    this.user = user;
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

  @XmlTransient
  @JsonIgnore
  public Collection<JobInputFile> getJobInputFileCollection() {
    return jobInputFileCollection;
  }

  public void setJobInputFileCollection(
          Collection<JobInputFile> jobInputFileCollection) {
    this.jobInputFileCollection = jobInputFileCollection;
  }

}
