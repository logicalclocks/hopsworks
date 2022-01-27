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
package io.hops.hopsworks.common.git;

import com.google.common.base.Strings;
import io.hops.hopsworks.exceptions.GitOpException;
import io.hops.hopsworks.persistence.entity.git.CommitterSignature;
import io.hops.hopsworks.persistence.entity.git.config.GitCommandConfiguration;
import io.hops.hopsworks.persistence.entity.git.config.GitCommandType;
import io.hops.hopsworks.persistence.entity.git.config.GitProvider;
import io.hops.hopsworks.restutils.RESTCodes;

import java.util.List;
import java.util.logging.Level;

public class GitCommandConfigurationBuilder {
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

  public GitCommandConfigurationBuilder setRemoteName(String remoteName) {
    this.remoteName = remoteName;
    return this;
  }

  public GitCommandConfigurationBuilder setRemoteUrl(String remoteUrl) {
    this.remoteUrl = remoteUrl;
    return this;
  }

  public GitCommandConfigurationBuilder setBranchName(String branchName) {
    this.branchName = branchName;
    return this;
  }

  public GitCommandConfigurationBuilder setForce(boolean force) {
    this.force = force;
    return this;
  }

  public GitCommandConfigurationBuilder setCommit(String commit) {
    this.commit = commit;
    return this;
  }

  public GitCommandConfigurationBuilder setUrl(String url) {
    this.url = url;
    return this;
  }

  public GitCommandConfigurationBuilder setProvider(GitProvider provider) {
    this.provider = provider;
    return this;
  }

  public GitCommandConfigurationBuilder setMessage(String message) {
    this.message = message;
    return this;
  }

  public GitCommandConfigurationBuilder setCommitter(CommitterSignature committer) {
    this.committer = committer;
    return this;
  }

  public GitCommandConfigurationBuilder setAll(boolean all) {
    this.all = all;
    return this;
  }

  public GitCommandConfigurationBuilder setFiles(List<String> files) {
    this.files = files;
    return this;
  }

  public GitCommandConfigurationBuilder setCheckout(boolean checkout) {
    this.checkout = checkout;
    return this;
  }

  public GitCommandConfigurationBuilder setDeleteOnRemote(boolean deleteOnRemote) {
    this.deleteOnRemote = deleteOnRemote;
    return this;
  }

  public GitCommandConfigurationBuilder setCommandType(GitCommandType commandType) {
    this.commandType = commandType;
    return this;
  }

  public GitCommandConfigurationBuilder setPath(String path) {
    this.path = path;
    return this;
  }

  public GitCommandConfiguration build() throws GitOpException {
    GitCommandConfiguration commandConfiguration = new GitCommandConfiguration();
    commandConfiguration.setRemoteName(this.remoteName);
    commandConfiguration.setRemoteUrl(this.remoteUrl);
    commandConfiguration.setBranchName(this.branchName);
    commandConfiguration.setForce(this.force);
    commandConfiguration.setCommit(this.commit);
    commandConfiguration.setUrl(this.url);
    commandConfiguration.setProvider(this.provider);
    commandConfiguration.setMessage(this.message);
    commandConfiguration.setCommitter(this.committer);
    commandConfiguration.setAll(this.all);
    commandConfiguration.setFiles(this.files);
    commandConfiguration.setCheckout(this.checkout);
    commandConfiguration.setDeleteOnRemote(this.deleteOnRemote);
    commandConfiguration.setCommandType(this.commandType);
    commandConfiguration.setPath(this.path);

    if (Strings.isNullOrEmpty(this.path) || this.commandType == null) {
      throw new GitOpException(RESTCodes.GitOpErrorCode.INVALID_GIT_COMMAND_CONFIGURATION, Level.FINE, "Path and " +
          "command type in command configuration cannot be null");
    }
    return commandConfiguration;
  }
}
