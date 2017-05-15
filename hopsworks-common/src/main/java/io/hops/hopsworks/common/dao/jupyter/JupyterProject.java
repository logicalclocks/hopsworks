package io.hops.hopsworks.common.dao.jupyter;

import io.hops.hopsworks.common.dao.project.Project;
import java.io.Serializable;
import java.time.Instant;
import java.util.Date;
import javax.persistence.Basic;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import javax.xml.bind.annotation.XmlRootElement;

@Entity
@Table(name = "jupyter_project",
        catalog = "hopsworks",
        schema = "")
@XmlRootElement
@NamedQueries({
  @NamedQuery(name = "JupyterProject.findAll",
          query
          = "SELECT j FROM JupyterProject j"),
  @NamedQuery(name = "JupyterProject.findByPort",
          query
          = "SELECT j FROM JupyterProject j WHERE j.port = :port"),
  @NamedQuery(name = "JupyterProject.findByHdfsUserId",
          query
          = "SELECT j FROM JupyterProject j WHERE j.hdfsUserId = :hdfsUserId"),
  @NamedQuery(name = "JupyterProject.findByCreated",
          query
          = "SELECT j FROM JupyterProject j WHERE j.created = :created"),
  @NamedQuery(name = "JupyterProject.findByHostIp",
          query
          = "SELECT j FROM JupyterProject j WHERE j.hostIp = :hostIp"),
  @NamedQuery(name = "JupyterProject.findByToken",
          query
          = "SELECT j FROM JupyterProject j WHERE j.token = :token"),
  @NamedQuery(name = "JupyterProject.findByPid",
          query
          = "SELECT j FROM JupyterProject j WHERE j.pid = :pid")})
public class JupyterProject implements Serializable {

  private static final long serialVersionUID = 1L;
  @Id
  @Basic(optional = false)
  @NotNull
  @Column(name = "port")
  private Integer port;
  @Basic(optional = false)
  @NotNull
  @Column(name = "created")
  @Temporal(TemporalType.TIMESTAMP)
  private Date created;
  @Basic(optional = false)
  @NotNull
  @Column(name = "last_accessed")
  @Temporal(TemporalType.TIMESTAMP)
  private Date lastAccessed;
  @Basic(optional = false)
  @NotNull
  @Size(min = 1,
          max = 255)
  @Column(name = "host_ip")
  private String hostIp;
  @Basic(optional = false)
  @NotNull
  @Size(min = 20,
          max = 64)
  @Column(name = "secret")
  private String secret;
  @Basic(optional = false)
  @NotNull
  @Size(min = 1,
          max = 255)
  @Column(name = "token")
  private String token;
  @Basic(optional = false)
  @NotNull
  @Column(name = "pid")
  private Long pid;
  @JoinColumn(name = "project_id",
          referencedColumnName = "id")
  @ManyToOne(optional = false)
  private Project projectId;
  @Basic(optional = false)
  @NotNull
  @Column(name = "hdfs_user_id")
  private int hdfsUserId;

  @Basic(optional = false)
  @NotNull
  @Column(name = "driver_cores")
  private Integer driverCores;

  @Basic(optional = false)
  @NotNull
  @Column(name = "num_executors")
  private Integer numExecutors;

  @Basic(optional = false)
  @NotNull
  @Column(name = "executor_cores")
  private Integer executorCores;
  @Basic(optional = false)
  @NotNull
  @Size(min = 2,
          max = 32)
  @Column(name = "driver_memory")
  private String driverMemory;
  @Basic(optional = false)
  @NotNull
  @Size(min = 2,
          max = 32)
  @Column(name = "executor_memory")
  private String executorMemory;
  @Basic(optional = true)
  @Column(name = "gpus")
  private Integer gpus;

  @Basic(optional = false)
  @NotNull
  @Size(min = 0,
          max = 3000)
  @Column(name = "archives")
  private String archives;

  @Basic(optional = false)
  @NotNull
  @Size(min = 0,
          max = 3000)
  @Column(name = "jars")
  private String jars;

  @Basic(optional = false)
  @NotNull
  @Size(min = 0,
          max = 3000)
  @Column(name = "files")
  private String files;

  @Basic(optional = false)
  @NotNull
  @Size(min = 0,
          max = 3000)
  @Column(name = "pyFiles")
  private String pyFiles;

  public JupyterProject() {
  }

  public JupyterProject(Project project, String secret, Integer port,
          int hdfsUserId, String hostIp, String token, Long pid, int driverCores,
          String driverMemory, int numExecutors, int executorCores,
          String executorMemory, int gpus, String archives, String jars,
          String files, String pyFiles
  ) {
    this.projectId = project;
    this.secret = secret;
    this.port = port;
    this.hdfsUserId = hdfsUserId;
    this.created = Date.from(Instant.now());
    this.lastAccessed = Date.from(Instant.now());
    this.hostIp = hostIp;
    this.token = token;
    this.pid = pid;
    this.driverCores = driverCores;
    this.driverMemory = driverMemory;
    this.numExecutors = numExecutors;
    this.executorCores = executorCores;
    this.executorMemory = driverMemory;
    this.gpus = gpus;
    this.archives = archives;
    this.jars = jars;
    this.files = files;
    this.pyFiles = pyFiles;
  }

  public String getSecret() {
    return secret;
  }

  public void setSecret(String secret) {
    this.secret = secret;
  }
  
  public Integer getPort() {
    return port;
  }

  public void setPort(Integer port) {
    this.port = port;
  }

  public int getHdfsUserId() {
    return hdfsUserId;
  }

  public void setHdfsUserId(int hdfsUserId) {
    this.hdfsUserId = hdfsUserId;
  }

  public Date getCreated() {
    return created;
  }

  public void setCreated(Date created) {
    this.created = created;
  }

  public Date getLastAccessed() {
    return lastAccessed;
  }

  public void setLastAccessed(Date lastAccessed) {
    this.lastAccessed = lastAccessed;
  }

  public String getHostIp() {
    return hostIp;
  }

  public void setHostIp(String hostIp) {
    this.hostIp = hostIp;
  }

  public String getToken() {
    return token;
  }

  public void setToken(String token) {
    this.token = token;
  }

  public Long getPid() {
    return pid;
  }

  public void setPid(Long pid) {
    this.pid = pid;
  }

  public Project getProjectId() {
    return projectId;
  }

  public void setProjectId(Project projectId) {
    this.projectId = projectId;
  }

  public Integer getDriverCores() {
    return driverCores;
  }

  public void setDriverCores(Integer driverCores) {
    this.driverCores = driverCores;
  }

  public String getDriverMemory() {
    return driverMemory;
  }

  public void setDriverMemory(String driverMemory) {
    this.driverMemory = driverMemory;
  }

  public Integer getNumExecutors() {
    return numExecutors;
  }

  public void setNumExecutors(Integer numExecutors) {
    this.numExecutors = numExecutors;
  }

  public Integer getExecutorCores() {
    return executorCores;
  }

  public void setExecutorCores(Integer executorCores) {
    this.executorCores = executorCores;
  }

  public String getExecutorMemory() {
    return executorMemory;
  }

  public void setExecutorMemory(String executorMemory) {
    this.executorMemory = executorMemory;
  }

  public Integer getGpus() {
    return gpus;
  }

  public void setGpus(Integer gpus) {
    this.gpus = gpus;
  }

  public String getArchives() {
    return archives;
  }

  public void setArchives(String archives) {
    this.archives = archives;
  }

  public String getJars() {
    return jars;
  }

  public void setJars(String jars) {
    this.jars = jars;
  }

  public String getFiles() {
    return files;
  }

  public void setFiles(String files) {
    this.files = files;
  }

  public String getPyFiles() {
    return pyFiles;
  }

  public void setPyFiles(String pyFiles) {
    this.pyFiles = pyFiles;
  }

  @Override
  public int hashCode() {
    int hash = 0;
    hash += (port != null ? port.hashCode() : 0);
    return hash;
  }

  @Override
  public boolean equals(Object object) {
    if (!(object instanceof JupyterProject)) {
      return false;
    }
    JupyterProject other = (JupyterProject) object;
    if ((this.port == null && other.port != null) || (this.port != null
            && !this.port.equals(other.port))) {
      return false;
    }
    return true;
  }

  @Override
  public String toString() {
    return "io.hops.hopsworks.common.dao.jupyter.JupyterProject[ port=" + port
            + " ]";
  }

}
