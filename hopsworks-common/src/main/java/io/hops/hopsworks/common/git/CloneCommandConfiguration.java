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

import io.hops.hopsworks.persistence.entity.git.config.GitProvider;

public class CloneCommandConfiguration {
  private String path;
  private String url;
  //GitHub or BitBucket or GitLab
  private GitProvider provider;
  private String branch;

  public CloneCommandConfiguration() {}

  public String getPath() { return path; }

  public void setPath(String path) { this.path = path; }

  public String getUrl() { return url; }

  public void setUrl(String url) { this.url = url; }

  public GitProvider getProvider() { return provider; }

  public void setProvider(GitProvider provider) { this.provider = provider; }

  public String getBranch() { return branch; }

  public void setBranch(String branch) { this.branch = branch; }
}
