/*
 * This file is part of Hopsworks
 * Copyright (C) 2020, Logical Clocks AB. All rights reserved
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

package io.hops.hopsworks.common.python.library;

import io.hops.hopsworks.persistence.entity.jupyter.config.GitBackend;

import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement
public class LibrarySpecification {

  private PackageSource packageSource;
  private String version;
  private String channelUrl;
  private String dependencyUrl;
  private String gitApiKey;
  private GitBackend gitBackend;

  public String getVersion() {
    return version;
  }

  public void setVersion(String version) {
    this.version = version;
  }

  public PackageSource getPackageSource() {
    return packageSource;
  }

  public void setPackageSource(PackageSource packageSource) {
    this.packageSource = packageSource;
  }

  public String getChannelUrl() {
    return channelUrl;
  }

  public void setChannelUrl(String channelUrl) {
    this.channelUrl = channelUrl;
  }

  public String getDependencyUrl() {
    return dependencyUrl;
  }

  public void setDependencyUrl(String dependencyUrl) {
    this.dependencyUrl = dependencyUrl;
  }

  public String getGitApiKey() {
    return gitApiKey;
  }

  public void setGitApiKey(String gitApiKey) {
    this.gitApiKey = gitApiKey;
  }

  public GitBackend getGitBackend() {
    return gitBackend;
  }

  public void setGitBackend(GitBackend gitBackend) {
    this.gitBackend = gitBackend;
  }
}
