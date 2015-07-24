package se.kth.bbc.jobs.model.description;

import se.kth.bbc.jobs.model.configuration.JobConfiguration;
import se.kth.bbc.jobs.model.configuration.JobConfigurationConverter;
import java.io.Serializable;
import java.util.Collection;
import java.util.Date;
import javax.persistence.Basic;
import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Convert;
import javax.persistence.DiscriminatorColumn;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Inheritance;
import javax.persistence.InheritanceType;
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
import se.kth.bbc.jobs.jobhistory.Execution;
import se.kth.bbc.project.Project;
import se.kth.hopsworks.user.model.Users;

/**
 * Description of work to be executed. If the work is executed, this
 * results in an Execution. Every type of Job needs to subclass this Entity and
 * declare the @DiscriminatorValue annotation.
 * <p>
 * @author stig
 * @param <T>
 */
@Entity
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
@DiscriminatorColumn(name = "type")
@Table(name = "hopsworks.jobs")
@XmlRootElement
@NamedQueries({
  @NamedQuery(name = "JobDescription.findAll",
          query = "SELECT j FROM JobDescription j"),
  @NamedQuery(name = "JobDescription.findById",
          query
          = "SELECT j FROM JobDescription j WHERE j.id = :id"),
  @NamedQuery(name = "JobDescription.findByName",
          query
          = "SELECT j FROM JobDescription j WHERE j.name = :name"),
  @NamedQuery(name = "JobDescription.findByCreationTime",
          query
          = "SELECT j FROM JobDescription j WHERE j.creationTime = :creationTime"),
  @NamedQuery(name = "JobDescription.findByProject",
          query
          = "SELECT j FROM JobDescription j WHERE j.project = :project")})
public abstract class JobDescription<T extends JobConfiguration> implements
        Serializable {

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

  @Column(name = "json_config")
  @Convert(converter = JobConfigurationConverter.class)
  protected T jobConfig;

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
  private Collection<Execution> executionCollection;
  
  protected JobDescription(){};

  public JobDescription(T config, Project project,
          Users creator) {
    this(config, project, creator, new Date());
  }

  public JobDescription(T config, Project project,
          Users creator, Date creationTime) {
    this(config, project, creator, null, creationTime);
  }

  public JobDescription(T config, Project project,
          Users creator, String jobname) {
    this(config, project, creator, jobname, new Date());
  }

  protected JobDescription(T config, Project project,
          Users creator, String jobname, Date creationTime) {
    this.name = jobname;
    this.creationTime = creationTime;
    this.jobConfig = config;
    this.project = project;
    this.creator = creator;
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

  public T getJobConfig() {
    return jobConfig;
  }

  public void setJobConfig(T jobConfig) {
    this.jobConfig = jobConfig;
  }

  @JsonIgnore
  @XmlTransient
  public Collection<Execution> getExecutionCollection() {
    return executionCollection;
  }

  public void setExecutionCollection(
          Collection<Execution> executionCollection) {
    this.executionCollection = executionCollection;
  }

  @Override
  public final int hashCode() {
    int hash = 0;
    hash += (id != null ? id.hashCode() : 0);
    return hash;
  }

  @Override
  public final boolean equals(Object object) {
    // TODO: Warning - this method won't work in the case the id fields are not set
    if (!(object instanceof JobDescription)) {
      return false;
    }
    JobDescription other = (JobDescription) object;
    if ((this.id == null && other.id != null) || (this.id != null && !this.id.
            equals(other.id))) {
      return false;
    }
    return true;
  }

  @Override
  public String toString() {
    return "Job [" + name + ", " + id + "]";
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

}
