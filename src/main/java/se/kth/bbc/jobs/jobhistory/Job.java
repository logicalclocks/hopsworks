package se.kth.bbc.jobs.jobhistory;

import java.io.Serializable;
import java.util.Collection;
import java.util.Date;
import javax.persistence.Basic;
import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Convert;
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
import se.kth.bbc.jobs.yarn.YarnJobConfiguration;
import se.kth.bbc.project.Project;
import se.kth.hopsworks.user.model.Users;

/**
 * A job is a description of work to be executed. If the work is executed, this
 * results in an Execution.
 * <p>
 * @author stig
 */
@Entity
@Table(name = "hopsworks.jobs")
@XmlRootElement
@NamedQueries({
  @NamedQuery(name = "Job.findAll",
          query = "SELECT j FROM Job j"),
  @NamedQuery(name = "Job.findById",
          query
          = "SELECT j FROM Job j WHERE j.id = :id"),
  @NamedQuery(name = "Job.findByName",
          query
          = "SELECT j FROM Job j WHERE j.name = :name"),
  @NamedQuery(name = "Job.findByCreationTime",
          query
          = "SELECT j FROM Job j WHERE j.creationTime = :creationTime"),
  @NamedQuery(name = "Job.findByType",
          query
          = "SELECT j FROM Job j WHERE j.type = :type"),
  @NamedQuery(name = "Job.findByProjectAndType",
          query
          = "SELECT j FROM Job j WHERE j.type = :type AND j.project = :project ORDER BY j.creationTime DESC")})
public class Job implements Serializable {

  private static final long serialVersionUID = 1L;

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Basic(optional = false)
  @Column(name = "id")
  private Integer id;

  @Size(max = 128)
  @Column(name = "name")
  private String name;

  @Basic(optional = false)
  @NotNull
  @Column(name = "creation_time")
  @Temporal(TemporalType.TIMESTAMP)
  private Date creationTime;

  @NotNull
  @Size(max = 128)
  @Column(name = "type")
  @Enumerated(EnumType.STRING)
  private JobType type;

  @Column(name = "json_config")
  @Convert(converter = YarnJobConfigurationConverter.class)
  private YarnJobConfiguration jobConfig;

  @JoinColumn(name = "project_id",
          referencedColumnName = "id")
  @ManyToOne(optional = false)
  private Project project;

  @JoinColumn(name = "creator",
          referencedColumnName = "email")
  @ManyToOne(optional = false)
  private Users creator;

  @OneToMany(cascade = CascadeType.ALL,
          mappedBy = "job")
  private Collection<JobExecutionFile> jobExecutionFileCollection;

  @OneToMany(cascade = CascadeType.ALL,
          mappedBy = "job")
  private Collection<Execution> executionCollection;

  public Job() {
  }

  public Job(JobType type, YarnJobConfiguration config, Project project,
          Users creator) {
    this(type, config, project, creator, new Date());
  }

  public Job(JobType type, YarnJobConfiguration config, Project project,
          Users creator, Date creationTime) {
    this(type, config, project, creator, null, creationTime);
  }

  public Job(JobType type, YarnJobConfiguration config, Project project,
          Users creator, String jobname, Date creationTime) {
    this(type, config, project, creator, jobname, null, creationTime);
  }

  public Job(JobType type, YarnJobConfiguration config, Project project,
          Users creator, String jobname) {
    this(type, config, project, creator, jobname, new Date());
  }

  public Job(JobType type, YarnJobConfiguration config, Project project,
          Users creator, String jobname, Collection<JobExecutionFile> execFiles) {
    this(type, config, project, creator, jobname, execFiles, new Date());
  }

  public Job(JobType type, YarnJobConfiguration config, Project project,
          Users creator, String jobname, Collection<JobExecutionFile> execFiles,
          Date creationTime) {
    this.name = jobname;
    this.creationTime = creationTime;
    this.type = type;
    this.jobConfig = config;
    this.project = project;
    this.creator = creator;
    this.jobExecutionFileCollection = execFiles;
  }

  public Integer getId() {
    return id;
  }

  public void setId(Integer id) {
    this.id = id;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public Date getCreationTime() {
    return creationTime;
  }

  public void setCreationTime(Date creationTime) {
    this.creationTime = creationTime;
  }

  public JobType getType() {
    return type;
  }

  public void setType(JobType type) {
    this.type = type;
  }

  public YarnJobConfiguration getJobConfig() {
    return jobConfig;
  }

  public void setJobConfig(YarnJobConfiguration config) {
    this.jobConfig = config;
  }

  @XmlTransient
  @JsonIgnore
  public Collection<Execution> getExecutionCollection() {
    return executionCollection;
  }

  public void setExecutionCollection(Collection<Execution> executionCollection) {
    this.executionCollection = executionCollection;
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
    if (!(object instanceof Job)) {
      return false;
    }
    Job other = (Job) object;
    if ((this.id == null && other.id != null) || (this.id != null && !this.id.
            equals(other.id))) {
      return false;
    }
    return true;
  }

  @Override
  public String toString() {
    return "Job [" + name + ", " + type + ", " + id + "]";
  }

  public Project getProject() {
    return project;
  }

  public void setProject(Project project) {
    this.project = project;
  }

  public Users getCreator() {
    return creator;
  }

  public void setCreator(Users creator) {
    this.creator = creator;
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

}
