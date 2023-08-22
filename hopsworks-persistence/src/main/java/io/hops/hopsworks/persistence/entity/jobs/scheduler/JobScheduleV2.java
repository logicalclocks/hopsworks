/*
 * This file is part of Hopsworks
 * Copyright (C) 2023, Hopsworks AB. All rights reserved
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

package io.hops.hopsworks.persistence.entity.jobs.scheduler;

import io.hops.hopsworks.persistence.entity.jobs.description.Jobs;

import javax.persistence.Basic;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.OneToOne;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;
import javax.validation.constraints.NotNull;
import javax.xml.bind.annotation.XmlRootElement;
import java.io.Serializable;
import java.time.Instant;
import java.util.Date;
import java.util.Objects;

@Entity
@Table(name = "job_schedule", catalog = "hopsworks")
@XmlRootElement
@NamedQueries({
    @NamedQuery(name = "JobSchedule.getAll",
        query = "SELECT j FROM JobScheduleV2 j"),
    @NamedQuery(name = "JobSchedule.getByJobId",
        query = "SELECT j FROM JobScheduleV2 j WHERE j.job.id = :jobId"),
    @NamedQuery(name = "JobSchedule.getById",
        query = "SELECT j FROM JobScheduleV2 j WHERE j.id = :id"),
    @NamedQuery(name = "JobSchedule.getActiveWithCurrentExecutionTime",
        query = "SELECT j FROM JobScheduleV2 j WHERE j.enabled = true "
            + "AND j.nextExecutionDateTime <= :currentDateTime"),
  })
public class JobScheduleV2 implements Serializable {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Basic(optional = false)
  @Column(name = "id")
  private Integer id;

  @OneToOne(optional = false)
  @JoinColumn(name = "job_id", referencedColumnName = "id", updatable = false)
  private Jobs job;

  @Column(name = "start_date_time")
  @Basic(optional = false)
  @Temporal(TemporalType.TIMESTAMP)
  private Date startDateTime;

  @Column(name = "end_date_time")
  @Temporal(TemporalType.TIMESTAMP)
  @Basic(optional = true)
  private Date endDateTime;

  @Basic(optional = false)
  @NotNull
  @Column(name = "enabled")
  private Boolean enabled;

  @Basic(optional = false)
  @Column(name = "cron_expression")
  @NotNull
  private String cronExpression;

  @Basic(optional = true)
  @Column(name = "next_execution_date_time")
  @Temporal(TemporalType.TIMESTAMP)
  private Date nextExecutionDateTime;

  public JobScheduleV2() {}

  public JobScheduleV2(Integer id, Jobs job, Instant startDateTime, Instant endDateTime,
                       Boolean enabled, String cronExpression, Instant nextExecutionDateTime) {
    this.id = id;
    this.job = job;
    this.startDateTime = startDateTime != null ? Date.from(startDateTime) : null;
    this.endDateTime = endDateTime != null ? Date.from(endDateTime) : null;
    this.enabled = enabled;
    this.cronExpression = cronExpression;
    this.nextExecutionDateTime = nextExecutionDateTime != null ? Date.from(nextExecutionDateTime) : null;
  }

  public Integer getId() {
    return id;
  }

  public void setId(Integer id) {
    this.id = id;
  }

  public Jobs getJob() {
    return job;
  }

  public void setJob(Jobs job) {
    this.job = job;
  }

  public Instant getStartDateTime() {
    return startDateTime != null ? startDateTime.toInstant() : null;
  }

  public void setStartDateTime(Instant startDateTime) {
    this.startDateTime = startDateTime != null ? Date.from(startDateTime) : null;
  }

  public Instant getEndDateTime() {
    return endDateTime != null ? endDateTime.toInstant() : null;
  }

  public void setEndDateTime(Instant endDateTime) {
    this.endDateTime = endDateTime != null ? Date.from(endDateTime) : null;
  }

  public Boolean getEnabled() {
    return enabled;
  }

  public void setEnabled(Boolean enable) {
    this.enabled = enable;
  }

  public String getCronExpression() {
    return cronExpression;
  }

  public void setCronExpression(String cronExpression) {
    this.cronExpression = cronExpression;
  }

  public Instant getNextExecutionDateTime() {
    return nextExecutionDateTime != null ? nextExecutionDateTime.toInstant() : null;
  }

  public void setNextExecutionDateTime(Instant nextExecutionDateTime) {
    this.nextExecutionDateTime = nextExecutionDateTime != null ? Date.from(nextExecutionDateTime) : null;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    JobScheduleV2 that = (JobScheduleV2) o;
    return Objects.equals(id, that.id) &&
      Objects.equals(job, that.job) &&
      Objects.equals(startDateTime, that.startDateTime) &&
      Objects.equals(endDateTime, that.endDateTime) &&
      Objects.equals(enabled, that.enabled) &&
      Objects.equals(cronExpression, that.cronExpression) &&
      Objects.equals(nextExecutionDateTime, that.nextExecutionDateTime);
  }

  @Override
  public int hashCode() {
    return Objects.hash(id, job, startDateTime, endDateTime, enabled, cronExpression, nextExecutionDateTime);
  }
}
