/*
 * Changes to this file committed after and not including commit-id: ccc0d2c5f9a5ac661e60e6eaf138de7889928b8b
 * are released under the following license:
 *
 * This file is part of Hopsworks
 * Copyright (C) 2018, Logical Clocks AB. All rights reserved
 *
 * Hopsworks is free software: you can redistribute it and/or modify it under the terms of
 * the GNU Affero General Public License as published by the Free Software Foundation,
 * either version 3 of the License, or (at your option) any later version.
 *
 * Hopsworks is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR
 * PURPOSE.  See the GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License along with this program.
 * If not, see <https://www.gnu.org/licenses/>.
 *
 * Changes to this file committed before and including commit-id: ccc0d2c5f9a5ac661e60e6eaf138de7889928b8b
 * are released under the following license:
 *
 * Copyright (C) 2013 - 2018, Logical Clocks AB and RISE SICS AB. All rights reserved
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this
 * software and associated documentation files (the "Software"), to deal in the Software
 * without restriction, including without limitation the rights to use, copy, modify, merge,
 * publish, distribute, sublicense, and/or sell copies of the Software, and to permit
 * persons to whom the Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or
 * substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS  OR IMPLIED, INCLUDING
 * BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL  THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM,
 * DAMAGES OR  OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */
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
      query = "SELECT j FROM JupyterSettings j")
  ,
    @NamedQuery(name = "JupyterSettings.findByProjectId",
      query
      = "SELECT j FROM JupyterSettings j WHERE j.jupyterSettingsPK.projectId = :projectId")
  ,
    @NamedQuery(name = "JupyterSettings.findByTeamMember",
      query
      = "SELECT j FROM JupyterSettings j WHERE j.jupyterSettingsPK.email = :email")
  ,
    @NamedQuery(name = "JupyterSettings.findByNumTfPs",
      query
      = "SELECT j FROM JupyterSettings j WHERE j.numTfPs = :numTfPs")
  ,
    @NamedQuery(name = "JupyterSettings.findByNumExecutorGpus",
      query
      = "SELECT j FROM JupyterSettings j WHERE j.numExecutorGpus = :numExecutorGpus")
  ,
    @NamedQuery(name = "JupyterSettings.findByNumMpiNp",
      query
      = "SELECT j FROM JupyterSettings j WHERE j.numMpiNp = :numMpiNp")
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
      = "SELECT j FROM JupyterSettings j WHERE j.secret = :secret")
  ,
    @NamedQuery(name = "JupyterSettings.findByLogLevel",
      query
      = "SELECT j FROM JupyterSettings j WHERE j.logLevel = :logLevel")
  ,
    @NamedQuery(name = "JupyterSettings.findByMode",
      query = "SELECT j FROM JupyterSettings j WHERE j.mode = :mode")
  ,
    @NamedQuery(name = "JupyterSettings.findByAdvanced",
      query
      = "SELECT j FROM JupyterSettings j WHERE j.advanced = :advanced")
  ,
    @NamedQuery(name = "JupyterSettings.findByArchives",
      query
      = "SELECT j FROM JupyterSettings j WHERE j.archives = :archives")
  ,
    @NamedQuery(name = "JupyterSettings.findByJars",
      query = "SELECT j FROM JupyterSettings j WHERE j.jars = :jars")
  ,
    @NamedQuery(name = "JupyterSettings.findByFiles",
      query = "SELECT j FROM JupyterSettings j WHERE j.files = :files")
  ,
    @NamedQuery(name = "JupyterSettings.findByPyFiles",
      query
      = "SELECT j FROM JupyterSettings j WHERE j.pyFiles = :pyFiles")
  ,
    @NamedQuery(name = "JupyterSettings.findBySparkParams",
      query
      = "SELECT j FROM JupyterSettings j WHERE j.sparkParams = :sparkParams")
  ,
    @NamedQuery(name = "JupyterSettings.findByUmask",
      query = "SELECT j FROM JupyterSettings j WHERE j.umask = :umask")})
public class JupyterSettings implements Serializable {

  private static final long serialVersionUID = 1L;
  @EmbeddedId
  protected JupyterSettingsPK jupyterSettingsPK;

  @Basic(optional = false)
  @Column(name = "num_tf_ps")
  private int numTfPs = 1;

  @Basic(optional = false)
  @Column(name = "num_tf_gpus")
  private int numExecutorGpus = 0;

  @Basic(optional = false)
  @Column(name = "num_mpi_np")
  private int numMpiNp = 1;

  @Basic(optional = false)
  @Column(name = "appmaster_cores")
  private int appmasterCores = 1;

  @Basic(optional = false)
  @Column(name = "appmaster_memory")
  private int appmasterMemory = 1024;

  @Basic(optional = false)
  @Column(name = "num_executors")
  private int numExecutors = 1;

  @Basic(optional = false)
  @Column(name = "num_executor_cores")
  private int numExecutorCores = 1;

  @Basic(optional = false)
  @Column(name = "executor_memory")
  private int executorMemory = 4096;

  @Basic(optional = false)
  @Column(name = "dynamic_initial_executors")
  private int dynamicInitialExecutors = 1;

  @Basic(optional = false)
  @Column(name = "dynamic_min_executors")
  private int dynamicMinExecutors = 1;

  @Basic(optional = false)
  @Column(name = "dynamic_max_executors")
  private int dynamicMaxExecutors = 100;

  @Basic(optional = false)
  @NotNull
  @Size(min = 1,
      max = 255)
  @Column(name = "secret")
  private String secret;

  @Size(max = 32)
  @Column(name = "log_level")
  private String logLevel = "INFO";

  @Basic(optional = false)
  @Column(name = "shutdown_level")
  private int shutdownLevel = 4;

  @Basic(optional = false)
  @NotNull
  @Size(min = 1,
      max = 32)
  @Column(name = "mode")
  private String mode = "dynamicspark";

  @Basic(optional = false)
  @Column(name = "advanced")
  private boolean advanced = false;

  @Basic(optional = false)
  @Size(min = 0,
      max = 1500)
  @Column(name = "archives")
  private String archives = "";

  @Basic(optional = false)
  @Size(min = 0,
      max = 1500)
  @Column(name = "jars")
  private String jars = "";

  @Basic(optional = false)
  @Size(min = 0,
      max = 1500)
  @Column(name = "files")
  private String files = "";

  @Basic(optional = false)
  @Size(min = 0,
      max = 1500)
  @Column(name = "py_files")
  private String pyFiles = "";

  @Basic(optional = false)
  @Size(min = 0,
      max = 6500)
  @Column(name = "spark_params")
  private String sparkParams = "";

  @Basic(optional = false)
  @Size(min = 3,
      max = 4)
  @Column(name = "umask")
  private String umask = "022";

  @Basic(optional = false)
  @NotNull
  @Column(name = "fault_tolerant")
  private boolean faultTolerant;

  @JoinColumn(name = "team_member",
      referencedColumnName = "email",
      insertable = false,
      updatable = false)
  @ManyToOne(optional = false)
  private Users users;

  @JoinColumn(name = "project_id",
      referencedColumnName = "id",
      insertable = false,
      updatable = false)
  @ManyToOne(optional = false)
  private Project project;

  @Transient
  private String privateDir = "";

  @Transient  
  private String baseDir = "/Jupyter/";

  @Transient
  private String distributionStrategy;

  public JupyterSettings() {
  }

  public JupyterSettings(JupyterSettingsPK jupyterSettingsPK) {
    this.jupyterSettingsPK = jupyterSettingsPK;
  }

  public JupyterSettings(JupyterSettingsPK jupyterSettingsPK, int numTfPs, int numExecutorGpus, int numMpiNp,
      int appmasterCores, int appmasterMemory, int numExecutors, int numExecutorCores, int executorMemory,
      int dynamicInitialExecutors, int dynamicMinExecutors, int dynamicMaxExecutors, String secret, String mode,
      boolean advanced, String archives, String jars, String files, String pyFiles, String sparkParams, String umask,
      boolean faultTolerant) {
    this.jupyterSettingsPK = jupyterSettingsPK;
    this.numTfPs = numTfPs;
    this.numExecutorGpus = numExecutorGpus;
    this.numMpiNp = numMpiNp;
    this.appmasterCores = appmasterCores;
    this.appmasterMemory = appmasterMemory;
    this.numExecutors = numExecutors;
    this.numExecutorCores = numExecutorCores;
    this.executorMemory = executorMemory;
    this.dynamicInitialExecutors = dynamicInitialExecutors;
    this.dynamicMinExecutors = dynamicMinExecutors;
    this.dynamicMaxExecutors = dynamicMaxExecutors;
    this.secret = secret;
    this.mode = mode;
    this.advanced = advanced;
    this.archives = archives;
    this.jars = jars;
    this.files = files;
    this.pyFiles = pyFiles;
    this.sparkParams = sparkParams;
    this.umask = umask;
    this.faultTolerant = faultTolerant;
  }

  public JupyterSettings(int projectId, String email) {
    this.jupyterSettingsPK = new JupyterSettingsPK(projectId, email);
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

  public int getNumExecutorGpus() {
    return numExecutorGpus;
  }

  public void setNumExecutorGpus(int numExecutorGpus) {
    this.numExecutorGpus = numExecutorGpus;
  }

  public int getNumMpiNp() {
    return numMpiNp;
  }

  public void setNumMpiNp(int numMpiNp) {
    this.numMpiNp = numMpiNp;
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

  public String getMode() {
    return mode;
  }

  public void setMode(String mode) {
    this.mode = mode;
  }

  public boolean getAdvanced() {
    return advanced;
  }

  public void setAdvanced(boolean advanced) {
    this.advanced = advanced;
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

  public String getSparkParams() {
    return sparkParams;
  }

  public void setSparkParams(String sparkParams) {
    this.sparkParams = sparkParams;
  }

  public String getUmask() {
    return umask;
  }

  public void setUmask(String umask) {
    this.umask = umask;
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
    if ((this.jupyterSettingsPK == null && other.jupyterSettingsPK != null) || (this.jupyterSettingsPK != null
        && !this.jupyterSettingsPK.equals(other.jupyterSettingsPK))) {
      return false;
    }
    return true;
  }

  @Override
  public String toString() {
    return "io.hops.hopsworks.common.dao.jupyter.JupyterSettings[ jupyterSettingsPK="
        + jupyterSettingsPK + " ]";
  }

  public String getBaseDir() {
    return baseDir;
  }

  public void setBaseDir(String baseDir) {
    this.baseDir = baseDir;
  }

  public String getPrivateDir() {
    return privateDir;
  }

  public void setPrivateDir(String privateDir) {
    this.privateDir = privateDir;
  }

  public int getShutdownLevel() {
    return shutdownLevel;
  }

  public void setShutdownLevel(int shutdownLevel) {
    this.shutdownLevel = shutdownLevel;
  }

  public boolean getFaultTolerant() {
    return faultTolerant;
  }

  public void setFaultTolerant(boolean faultTolerant) {
    this.faultTolerant = faultTolerant;
  }

  public String getDistributionStrategy() {
    return distributionStrategy;
  }

  public void setDistributionStrategy(String distributionStrategy) {
    this.distributionStrategy = distributionStrategy;
  }
}
