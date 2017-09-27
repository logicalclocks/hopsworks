package io.hops.hopsworks.common.dao.jupyter;

import io.hops.hopsworks.common.dao.project.Project;
import io.hops.hopsworks.common.dao.user.Users;
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
import javax.persistence.Transient;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import javax.xml.bind.annotation.XmlRootElement;

@Entity
@Table(name = "jupyter_settings",
        catalog = "hopsworks",
        schema = "")
@XmlRootElement
@NamedQueries({
  @NamedQuery(name = "JupyterSettings.findAll",
          query
          = "SELECT j FROM JupyterSettings j")
  ,
  @NamedQuery(name = "JupyterSettings.findByProjectId",
          query
          = "SELECT j FROM JupyterSettings j WHERE j.jupyterSettingsPK.projectId = :projectId")
  ,
  @NamedQuery(name = "JupyterSettings.findByTeamMember",
          query
          = "SELECT j FROM JupyterSettings j WHERE j.jupyterSettingsPK.teamMember = :teamMember")
  ,
  @NamedQuery(name = "JupyterSettings.findByNumTfPs",
          query
          = "SELECT j FROM JupyterSettings j WHERE j.numTfPs = :numTfPs")
  ,
  @NamedQuery(name = "JupyterSettings.findByNumTfGpus",
          query
          = "SELECT j FROM JupyterSettings j WHERE j.numTfGpus = :numTfGpus")
  ,
  @NamedQuery(name = "JupyterSettings.findByAppmasterCores",
          query
          = "SELECT j FROM JupyterSettings j WHERE j.appmasterCores = :appmasterCores")
  ,
  @NamedQuery(name = "JupyterSettings.findByAppmasterMemory",
          query
          = "SELECT j FROM JupyterSettings j WHERE j.appmasterMemory = :appmasterMemory")
  ,
  @NamedQuery(name = "JupyterSettings.findByNumExecutors",
          query
          = "SELECT j FROM JupyterSettings j WHERE j.numExecutors = :numExecutors")
  ,
  @NamedQuery(name = "JupyterSettings.findByNumExecutorCores",
          query
          = "SELECT j FROM JupyterSettings j WHERE j.numExecutorCores = :numExecutorCores")
  ,
  @NamedQuery(name = "JupyterSettings.findByExecutorMemory",
          query
          = "SELECT j FROM JupyterSettings j WHERE j.executorMemory = :executorMemory")
  ,
  @NamedQuery(name = "JupyterSettings.findByDynamicInitialExecutors",
          query
          = "SELECT j FROM JupyterSettings j WHERE j.dynamicInitialExecutors = :dynamicInitialExecutors")
  ,
  @NamedQuery(name = "JupyterSettings.findByDynamicMinExecutors",
          query
          = "SELECT j FROM JupyterSettings j WHERE j.dynamicMinExecutors = :dynamicMinExecutors")
  ,
  @NamedQuery(name = "JupyterSettings.findByDynamicMaxExecutors",
          query
          = "SELECT j FROM JupyterSettings j WHERE j.dynamicMaxExecutors = :dynamicMaxExecutors")
  ,
  @NamedQuery(name = "JupyterSettings.findBySecret",
          query
          = "SELECT j FROM JupyterSettings j WHERE j.secret = :secret")})
public class JupyterSettings implements Serializable {

  private static final long serialVersionUID = 1L;
  @EmbeddedId
  protected JupyterSettingsPK jupyterSettingsPK;
  @Basic(optional = false)
  @NotNull
  @Column(name = "num_tf_ps")
  private int numTfPs = 0;
  @Basic(optional = false)
  @NotNull
  @Column(name = "num_tf_gpus")
  private int numTfGpus = 0;
  @Basic(optional = false)
  @NotNull
  @Column(name = "appmaster_cores")
  private int appmasterCores = 1;
  @Basic(optional = false)
  @NotNull
  @Column(name = "appmaster_memory")
  private int appmasterMemory = 1024;
  @Basic(optional = false)
  @NotNull
  @Column(name = "num_executors")
  private int numExecutors = 1;
  @Basic(optional = false)
  @NotNull
  @Column(name = "num_executor_cores")
  private int numExecutorCores = 1;
  @Basic(optional = false)
  @NotNull
  @Column(name = "executor_memory")
  private int executorMemory = 1024;
  @Basic(optional = false)
  @NotNull
  @Column(name = "dynamic_initial_executors")
  private int dynamicInitialExecutors = 1;
  @Basic(optional = false)
  @NotNull
  @Column(name = "dynamic_min_executors")
  private int dynamicMinExecutors = 1;
  @Basic(optional = false)
  @NotNull
  @Column(name = "dynamic_max_executors")
  private int dynamicMaxExecutors = 100;
  @Basic(optional = false)
  @NotNull
  @Size(min = 3,
          max = 32)
  @Column(name = "mode")
  private String mode = "dynamicSpark";

  @Basic(optional = false)
  @NotNull
  @Column(name = "advanced")
  private boolean advanced = false;

  @Basic(optional = false)
  @NotNull
  @Size(min = 0,
          max = 255)
  @Column(name = "secret")
  private String secret;

  @Basic(optional = true)
  @Size(min = 3,
          max = 15)
  @Column(name = "log_level")
  private String logLevel = "INFO";

  @Basic(optional = false)
  @NotNull
  @Size(min = 0,
          max = 2500)
  @Column(name = "archives")
  private String archives = "";

  @Basic(optional = false)
  @NotNull
  @Size(min = 0,
          max = 2500)
  @Column(name = "jars")
  private String jars = "";

  @Basic(optional = false)
  @NotNull
  @Size(min = 0,
          max = 2500)
  @Column(name = "files")
  private String files = "";

  @Basic(optional = false)
  @NotNull
  @Size(min = 0,
          max = 2500)
  @Column(name = "py_files")
  private String pyFiles = "";

  @Transient
  private String privateDir = "";

  @Transient
  private String baseDir = "/Jupyter/";
  
  @JoinColumn(name = "team_member",
          referencedColumnName = "email",
          insertable
          = false,
          updatable = false)
  @ManyToOne(optional = false)
  private Users users;
  @JoinColumn(name = "project_id",
          referencedColumnName = "id",
          insertable = false,
          updatable
          = false)
  @ManyToOne(optional = false)
  private Project project;

  public JupyterSettings() {
  }

  public JupyterSettings(JupyterSettingsPK jupyterSettingsPK, String secret,
          String framework) {
    this.jupyterSettingsPK = jupyterSettingsPK;
    this.secret = secret;
    this.mode = framework;
  }

  public JupyterSettings(int projectId, String teamMember) {
    this.jupyterSettingsPK = new JupyterSettingsPK(projectId, teamMember);
  }

  public JupyterSettingsPK getJupyterSettingsPK() {
    return jupyterSettingsPK;
  }

  public void setJupyterSettingsPK(JupyterSettingsPK jupyterSettingsPK) {
    this.jupyterSettingsPK = jupyterSettingsPK;
  }

  public int getNumTfPs() {
    return numTfPs;
  }

  public void setNumTfPs(int numTfPs) {
    this.numTfPs = numTfPs;
  }

  public int getNumTfGpus() {
    return numTfGpus;
  }

  public void setNumTfGpus(int numTfGpus) {
    this.numTfGpus = numTfGpus;
  }

  public int getAppmasterCores() {
    return appmasterCores;
  }

  public void setAppmasterCores(int appmasterCores) {
    this.appmasterCores = appmasterCores;
  }

  public int getAppmasterMemory() {
    return appmasterMemory;
  }

  public void setAppmasterMemory(int appmasterMemory) {
    this.appmasterMemory = appmasterMemory;
  }

  public int getNumExecutors() {
    return numExecutors;
  }

  public void setNumExecutors(int numExecutors) {
    this.numExecutors = numExecutors;
  }

  public int getNumExecutorCores() {
    return numExecutorCores;
  }

  public void setNumExecutorCores(int numExecutorCores) {
    this.numExecutorCores = numExecutorCores;
  }

  public int getExecutorMemory() {
    return executorMemory;
  }

  public void setExecutorMemory(int executorMemory) {
    this.executorMemory = executorMemory;
  }

  public int getDynamicInitialExecutors() {
    return dynamicInitialExecutors;
  }

  public void setDynamicInitialExecutors(int dynamicInitialExecutors) {
    this.dynamicInitialExecutors = dynamicInitialExecutors;
  }

  public int getDynamicMinExecutors() {
    return dynamicMinExecutors;
  }

  public void setDynamicMinExecutors(int dynamicMinExecutors) {
    this.dynamicMinExecutors = dynamicMinExecutors;
  }

  public int getDynamicMaxExecutors() {
    return dynamicMaxExecutors;
  }

  public void setDynamicMaxExecutors(int dynamicMaxExecutors) {
    this.dynamicMaxExecutors = dynamicMaxExecutors;
  }

  public String getSecret() {
    return secret;
  }

  public void setSecret(String secret) {
    this.secret = secret;
  }

  public String getLogLevel() {
    return logLevel;
  }

  public void setLogLevel(String logLevel) {
    this.logLevel = logLevel;
  }

  public Users getUsers() {
    return users;
  }

  public void setUsers(Users users) {
    this.users = users;
  }

  public Project getProject() {
    return project;
  }

  public void setProject(Project project) {
    this.project = project;
  }

  public String getArchives() {
    return archives;
  }

  public void setArchives(String archives) {
    this.archives = archives;
  }

  public String getFiles() {
    return files;
  }

  public void setFiles(String files) {
    this.files = files;
  }

  public String getJars() {
    return jars;
  }

  public void setJars(String jars) {
    this.jars = jars;
  }

  public String getPyFiles() {
    return pyFiles;
  }

  public void setPyFiles(String pyFiles) {
    this.pyFiles = pyFiles;
  }

  public String getMode() {
    return mode;
  }

  public void setMode(String mode) {
    this.mode = mode;
  }

  public boolean isAdvanced() {
    return advanced;
  }

  public void setAdvanced(boolean advanced) {
    this.advanced = advanced;
  }

  public String getPrivateDir() {
    return privateDir;
  }

  public void setPrivateDir(String privateDir) {
    this.privateDir = privateDir;
  }

  public String getBaseDir() {
    return baseDir;
  }

  public void setBaseDir(String baseDir) {
    this.baseDir = baseDir;
  }

  
  @Override
  public int hashCode() {
    int hash = 0;
    hash += (jupyterSettingsPK != null ? jupyterSettingsPK.hashCode() : 0);
    return hash;
  }

  @Override
  public boolean equals(Object object) {
    // TODO: Warning - this method won't work in the case the id fields are not set
    if (!(object instanceof JupyterSettings)) {
      return false;
    }
    JupyterSettings other = (JupyterSettings) object;
    if ((this.jupyterSettingsPK == null && other.jupyterSettingsPK != null)
            || (this.jupyterSettingsPK != null && !this.jupyterSettingsPK.
                    equals(other.jupyterSettingsPK))) {
      return false;
    }
    return true;
  }

  @Override
  public String toString() {
    return "io.hops.hopsworks.common.dao.jupyter.JupyterSettings[ jupyterSettingsPK="
            + jupyterSettingsPK + " ]";
  }

}
