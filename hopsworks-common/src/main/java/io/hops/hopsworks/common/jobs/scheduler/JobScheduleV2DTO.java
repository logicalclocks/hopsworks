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

package io.hops.hopsworks.common.jobs.scheduler;

import io.hops.hopsworks.common.api.RestDTO;
import io.hops.hopsworks.common.util.InstantAdapter;

import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;
import java.time.Instant;

public class JobScheduleV2DTO extends RestDTO<JobScheduleV2DTO> {
  private Integer id;
  @XmlJavaTypeAdapter(InstantAdapter.class)
  private Instant startDateTime;
  @XmlJavaTypeAdapter(InstantAdapter.class)
  private Instant endDateTime;
  private Boolean enabled;
  private String cronExpression;
  @XmlJavaTypeAdapter(InstantAdapter.class)
  private Instant nextExecutionDateTime;

  public JobScheduleV2DTO() {}

  public Integer getId() {
    return id;
  }

  public void setId(Integer id) {
    this.id = id;
  }

  public Instant getStartDateTime() {
    return startDateTime;
  }

  public void setStartDateTime(Instant startDateTime) {
    this.startDateTime = startDateTime;
  }

  public Instant getEndDateTime() {
    return endDateTime;
  }

  public void setEndDateTime(Instant endDateTime) {
    this.endDateTime = endDateTime;
  }

  public Boolean getEnabled() {
    return enabled;
  }

  public void setEnabled(Boolean enabled) {
    this.enabled = enabled;
  }

  public String getCronExpression() {
    return cronExpression;
  }

  public void setCronExpression(String cronExpression) {
    this.cronExpression = cronExpression;
  }

  public Instant getNextExecutionDateTime() {
    return nextExecutionDateTime;
  }

  public void setNextExecutionDateTime(Instant nextExecutionDateTime) {
    this.nextExecutionDateTime = nextExecutionDateTime;
  }
}
