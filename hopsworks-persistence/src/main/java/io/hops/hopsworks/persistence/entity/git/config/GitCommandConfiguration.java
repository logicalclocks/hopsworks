/*
 * This file is part of Hopsworks
 * Copyright (C) 2021, Logical Clocks AB. All rights reserved
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
package io.hops.hopsworks.persistence.entity.git.config;

import io.hops.hopsworks.persistence.entity.git.CommitterSignature;

import javax.xml.bind.annotation.XmlRootElement;
import java.io.Serializable;
import java.util.List;

@XmlRootElement
public class GitCommandConfiguration implements Serializable {
  private static final long serialVersionUID = 1L;

  private String remoteName;
  private String remoteUrl;
  private String branchName;
  private boolean force;
  private String commit;
  private String url;
  private GitProvider provider;
  private String message;
  private CommitterSignature committer;
  private boolean all;
  private List<String> files;
  private boolean checkout;
  private boolean deleteOnRemote;
  private GitCommandType commandType;
  private String path;
  private Boolean readOnly;

  public GitCommandConfiguration(){}

  public String getRemoteName() { return remoteName; }

  public void setRemoteName(String remoteName) { this.remoteName = remoteName; }

  public String getRemoteUrl() { return remoteUrl; }

  public void setRemoteUrl(String remoteUrl) { this.remoteUrl = remoteUrl; }

  public String getBranchName() { return branchName; }

  public void setBranchName(String branchName) { this.branchName = branchName; }

  public boolean isForce() { return force; }

  public void setForce(boolean force) { this.force = force; }

  public String getCommit() { return commit; }

  public void setCommit(String commit) { this.commit = commit; }

  public String getUrl() { return url; }

  public void setUrl(String url) { this.url = url; }

  public GitProvider getProvider() { return provider; }

  public void setProvider(GitProvider provider) { this.provider = provider; }

  public String getMessage() { return message; }

  public void setMessage(String message) { this.message = message; }

  public CommitterSignature getCommitter() { return committer; }

  public void setCommitter(CommitterSignature committer) { this.committer = committer; }

  public boolean isAll() { return all; }

  public void setAll(boolean all) { this.all = all; }

  public List<String> getFiles() { return files; }

  public void setFiles(List<String> files) { this.files = files; }

  public boolean isCheckout() { return checkout; }

  public void setCheckout(boolean checkout) { this.checkout = checkout; }

  public boolean isDeleteOnRemote() { return deleteOnRemote; }

  public void setDeleteOnRemote(boolean deleteOnRemote) { this.deleteOnRemote = deleteOnRemote; }

  public GitCommandType getCommandType() { return commandType; }

  public void setCommandType(GitCommandType commandType) { this.commandType = commandType; }

  public String getPath() { return path; }

  public void setPath(String path) { this.path = path; }

  public Boolean getReadOnly() { return readOnly; }

  public void setReadOnly(Boolean readOnly) { this.readOnly = readOnly; }
}
