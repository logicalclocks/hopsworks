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

import com.fasterxml.jackson.annotation.JsonTypeName;

import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement
@JsonTypeName("pullCommandConfiguration")
public class PullCommandConfiguration extends RepositoryActionCommandConfiguration {
  private String remoteName;
  private String branchName;
  private boolean force;

  public PullCommandConfiguration() {}

  public String getRemoteName() { return remoteName; }

  public void setRemoteName(String remoteName) { this.remoteName = remoteName; }

  public String getBranchName() { return branchName; }

  public void setBranchName(String branchName) { this.branchName = branchName; }

  public boolean isForce() { return force; }

  public void setForce(boolean force) { this.force = force; }
}
