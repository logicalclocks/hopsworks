/*
 * This file is part of Hopsworks
 * Copyright (C) 2019, Logical Clocks AB. All rights reserved
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
 */

package io.hops.hopsworks.persistence.entity.jupyter.config;

import javax.persistence.Basic;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.persistence.Transient;
import javax.validation.constraints.Size;
import javax.xml.bind.annotation.XmlRootElement;
import java.io.Serializable;
import java.util.Set;

@Entity
@Table(name = "jupyter_git_config",
        catalog = "hopsworks")
@XmlRootElement
public class GitConfig implements Serializable {
  private static final long serialVersionUID = 1L;
  
  @Id
  @GeneratedValue(strategy = GenerationType.SEQUENCE)
  @Basic(optional = false)
  private Integer id;
  
  @Column(name = "remote_git_url",
          nullable = false)
  @Size(min = 1, max = 255)
  @Basic(optional = false)
  private String remoteGitURL;
  
  @Column(name = "api_key_name",
          nullable = false)
  @Size(min = 1, max = 125)
  @Basic(optional = false)
  private String apiKeyName;
  
  @Column(name = "base_branch")
  @Size(max = 125)
  private String baseBranch;
  
  @Column(name = "head_branch")
  @Size(max = 125)
  private String headBranch;
  
  @Column(name = "startup_auto_pull", nullable = false)
  private Boolean startupAutoPull = true;
  
  @Column(name = "shutdown_auto_push", nullable = false)
  private Boolean shutdownAutoPush = true;
  
  @Transient
  private Set<String> branches;
  
  public GitConfig() { }
  
  public Integer getId() {
    return id;
  }
  
  public void setId(Integer id) {
    this.id = id;
  }
  
  public String getRemoteGitURL() {
    return remoteGitURL;
  }
  
  public void setRemoteGitURL(String remoteGitURL) {
    this.remoteGitURL = remoteGitURL;
  }
  
  public String getApiKeyName() {
    return apiKeyName;
  }
  
  public void setApiKeyName(String apiKeyName) {
    this.apiKeyName = apiKeyName;
  }
  
  public Set<String> getBranches() {
    return branches;
  }
  
  public void setBranches(Set<String> branches) {
    this.branches = branches;
  }
  
  public String getBaseBranch() {
    return baseBranch;
  }
  
  public void setBaseBranch(String baseBranch) {
    this.baseBranch = baseBranch;
  }
  
  public String getHeadBranch() {
    return headBranch;
  }
  
  public void setHeadBranch(String headBranch) {
    this.headBranch = headBranch;
  }
  
  public Boolean getStartupAutoPull() {
    return startupAutoPull;
  }
  
  public void setStartupAutoPull(Boolean startupAutoPull) {
    this.startupAutoPull = startupAutoPull;
  }
  
  public Boolean getShutdownAutoPush() {
    return shutdownAutoPush;
  }
  
  public void setShutdownAutoPush(Boolean shutdownAutoPush) {
    this.shutdownAutoPush = shutdownAutoPush;
  }
}
