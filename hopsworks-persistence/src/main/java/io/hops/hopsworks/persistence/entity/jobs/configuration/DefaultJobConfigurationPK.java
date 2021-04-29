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

package io.hops.hopsworks.persistence.entity.jobs.configuration;

import javax.persistence.Basic;
import javax.persistence.Column;
import javax.persistence.Embeddable;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.validation.constraints.NotNull;
import java.io.Serializable;

@Embeddable
public class DefaultJobConfigurationPK implements Serializable {

  @Basic(optional = false)
  @NotNull
  @Column(name = "project_id")
  private int projectId;

  @Basic(optional = false)
  @NotNull
  @Column(name = "type")
  @Enumerated(EnumType.STRING)
  private JobType type;

  public DefaultJobConfigurationPK() {
  }

  public DefaultJobConfigurationPK(int projectId, JobType type) {
    this.projectId = projectId;
    this.type = type;
  }

  public int getProjectId() {
    return projectId;
  }

  public void setProjectId(int projectId) {
    this.projectId = projectId;
  }

  public JobType getType() {
    return type;
  }

  public void setType(JobType type) {
    this.type = type;
  }

  @Override
  public int hashCode() {
    return (getProjectId() + "_" + getType()).hashCode();
  }

  @Override
  public boolean equals(Object object) {
    if (!(object instanceof DefaultJobConfigurationPK)) {
      return false;
    }
    DefaultJobConfigurationPK other = (DefaultJobConfigurationPK) object;
    if (this.getProjectId() == other.getProjectId() && this.getType().name().equals(other.getType().name())) {
      return true;
    } else {
      return false;
    }
  }

  @Override
  public String toString() {
    return "DefaultJobConfigurationPK[ project=" +
      getProjectId() + ", type=" + getType() + " ]";
  }
}
