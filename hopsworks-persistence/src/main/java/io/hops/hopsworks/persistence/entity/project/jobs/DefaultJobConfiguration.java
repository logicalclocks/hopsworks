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

package io.hops.hopsworks.persistence.entity.project.jobs;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.hops.hopsworks.persistence.entity.jobs.configuration.DefaultJobConfigurationPK;
import io.hops.hopsworks.persistence.entity.jobs.configuration.JobConfiguration;
import io.hops.hopsworks.persistence.entity.jobs.configuration.JobConfigurationConverter;
import io.hops.hopsworks.persistence.entity.jobs.configuration.JobType;
import io.hops.hopsworks.persistence.entity.project.Project;


import javax.persistence.Column;
import javax.persistence.Convert;
import javax.persistence.EmbeddedId;
import javax.persistence.Entity;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import javax.xml.bind.annotation.XmlRootElement;
import java.io.Serializable;
import java.util.Objects;

@Entity
@XmlRootElement
@Table(name = "default_job_configuration",
        catalog = "hopsworks",
        schema = "")
public class DefaultJobConfiguration implements Serializable {

  public DefaultJobConfiguration(){}

  public DefaultJobConfiguration(Project project, JobType jobType, JobConfiguration jobConfiguration) {
    this.setDefaultJobConfigurationPK(new DefaultJobConfigurationPK(project.getId(), jobType));
    this.setJobConfig(jobConfiguration);
  }

  private static final long serialVersionUID = 1L;
  @EmbeddedId
  private DefaultJobConfigurationPK defaultJobConfigurationPK;

  @JoinColumn(name = "project_id",
    referencedColumnName = "id",
    insertable = false,
    updatable = false)
  @ManyToOne(optional = false)
  private Project project;

  @Column(name = "config")
  @Convert(converter = JobConfigurationConverter.class)
  @SuppressFBWarnings(justification="Converter", value="SE_BAD_FIELD")
  private JobConfiguration jobConfig;

  public DefaultJobConfigurationPK getDefaultJobConfigurationPK() {
    return defaultJobConfigurationPK;
  }

  public void setDefaultJobConfigurationPK(DefaultJobConfigurationPK defaultJobConfigurationPK) {
    this.defaultJobConfigurationPK = defaultJobConfigurationPK;
  }

  public JobConfiguration getJobConfig() {
    return this.jobConfig;
  }

  public void setJobConfig(JobConfiguration jobConfig) {
    this.jobConfig = jobConfig;
  }
  
  @Override
  public int hashCode() {
    return Objects.hash(defaultJobConfigurationPK);
  }
  
  @Override
  public boolean equals(Object object) {
    if (!(object instanceof DefaultJobConfiguration)) {
      return false;
    }
    DefaultJobConfiguration other = (DefaultJobConfiguration) object;
    if (this.defaultJobConfigurationPK.equals(other.defaultJobConfigurationPK)) {
      return true;
    } else {
      return false;
    }
  }

  public Project getProject() {
    return project;
  }

  public void setProject(Project project) {
    this.project = project;
  }
}
