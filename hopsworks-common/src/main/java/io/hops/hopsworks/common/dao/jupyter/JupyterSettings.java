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

import com.fasterxml.jackson.annotation.JsonInclude;
import io.hops.hopsworks.common.dao.jupyter.config.GitConfig;
import io.hops.hopsworks.common.dao.project.Project;
import io.hops.hopsworks.common.dao.user.Users;
import io.hops.hopsworks.common.jobs.configuration.JobConfiguration;
import io.hops.hopsworks.common.jupyter.JupyterConfigurationConverter;

import java.io.Serializable;
import javax.persistence.Basic;
import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Convert;
import javax.persistence.EmbeddedId;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.OneToOne;
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
    @NamedQuery(name = "JupyterSettings.findBySecret",
      query
      = "SELECT j FROM JupyterSettings j WHERE j.secret = :secret")
  ,
    @NamedQuery(name = "JupyterSettings.findByAdvanced",
      query
      = "SELECT j FROM JupyterSettings j WHERE j.advanced = :advanced")})
public class JupyterSettings implements Serializable {

  private static final long serialVersionUID = 1L;
  @EmbeddedId
  protected JupyterSettingsPK jupyterSettingsPK;

  @Basic(optional = false)
  @NotNull
  @Size(min = 1,
      max = 255)
  @Column(name = "secret")
  private String secret;

  @Basic(optional = false)
  @Column(name = "shutdown_level")
  private int shutdownLevel = 6;

  @Basic(optional = false)
  @Column(name = "advanced")
  private boolean advanced = false;

  @Basic(optional = false)
  @Column(name = "python_kernel")
  private boolean pythonKernel = true;

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

  @Basic(optional = false)
  @NotNull
  @Size(min = 1,
          max = 255)
  @Column(name = "base_dir")
  private String baseDir = "/Jupyter/";

  @Column(name = "job_config")
  @Convert(converter = JupyterConfigurationConverter.class)
  private JobConfiguration jobConfig;
  
  @Transient
  private String privateDir = "";

  @Transient
  private Boolean gitAvailable;
  
  @Column(name = "git_backend")
  private Boolean gitBackend = false;
  
  @OneToOne(fetch = FetchType.LAZY, cascade = CascadeType.ALL, orphanRemoval = true)
  @JoinColumn(name = "git_config_id", referencedColumnName = "id")
  @JsonInclude(JsonInclude.Include.NON_EMPTY)
  private GitConfig gitConfig;
  
  @Transient
  private JupyterMode mode;
  
  public JupyterSettings() {
  }

  public JupyterSettings(JupyterSettingsPK jupyterSettingsPK) {
    this.jupyterSettingsPK = jupyterSettingsPK;
  }

  public JupyterSettings(JupyterSettingsPK jupyterSettingsPK, String secret, boolean advanced) {
    this.jupyterSettingsPK = jupyterSettingsPK;
    this.secret = secret;
    this.setAdvanced(advanced);
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

  public String getSecret() {
    return secret;
  }

  public void setSecret(String secret) {
    this.secret = secret;
  }

  public boolean getAdvanced() {
    return isAdvanced();
  }

  public void setAdvanced(boolean advanced) {
    this.advanced = advanced;
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

  public boolean isAdvanced() {
    return advanced;
  }

  public JobConfiguration getJobConfig() {
    return jobConfig;
  }

  public void setJobConfig(JobConfiguration jobConfig) {
    this.jobConfig = jobConfig;
  }
  
  public Boolean isGitBackend() {
    return gitBackend;
  }
  
  public void setGitBackend(Boolean gitBackend) {
    this.gitBackend = gitBackend;
  }
  
  public Boolean getGitAvailable() {
    return gitAvailable;
  }
  
  public void setGitAvailable(Boolean gitAvailable) {
    this.gitAvailable = gitAvailable;
  }
  
  public GitConfig getGitConfig() {
    return gitConfig;
  }
  
  public void setGitConfig(GitConfig gitConfig) {
    this.gitConfig = gitConfig;
  }

  public boolean isPythonKernel() {
    return pythonKernel;
  }

  public void setPythonKernel(boolean pythonKernel) {
    this.pythonKernel = pythonKernel;
  }
  
  public JupyterMode getMode() {
    return mode;
  }
  
  public void setMode(JupyterMode mode) {
    this.mode = mode;
  }
}
