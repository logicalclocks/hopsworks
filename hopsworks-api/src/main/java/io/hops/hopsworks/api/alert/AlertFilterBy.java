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

package io.hops.hopsworks.api.alert;

import io.swagger.annotations.ApiParam;

import javax.ws.rs.QueryParam;
import java.util.HashSet;
import java.util.Set;

public class AlertFilterBy {
  @QueryParam("active")
  @ApiParam(defaultValue = "true", allowableValues = "true, false")
  private Boolean active;
  @QueryParam("silenced")
  @ApiParam(defaultValue = "true", allowableValues = "true, false")
  private Boolean silenced;
  @QueryParam("inhibited")
  @ApiParam(defaultValue = "true", allowableValues = "true, false")
  private Boolean inhibited;
  @QueryParam("filter")
  @ApiParam(value = "A list of matchers to filter alerts by. ex. filter=alertname=\"OutOfMemory,LowDiskSpace\"",
      allowMultiple = true)
  private Set<String> filter;
  @QueryParam("receiver")
  @ApiParam(value = "A regex matching receivers to filter alerts by", allowableValues = "true, false")
  private String receiver;

  public AlertFilterBy(@QueryParam("active") Boolean active, @QueryParam("silenced") Boolean silenced,
      @QueryParam("inhibited") Boolean inhibited, @QueryParam("filter") Set<String> filter,
      @QueryParam("receiver") String receiver) {
    this.active = active;
    this.silenced = silenced;
    this.inhibited = inhibited;
    this.filter = filter == null? new HashSet<>() : filter;
    this.receiver = receiver;
  }

  public Boolean isActive() {
    return active;
  }

  public void setActive(Boolean active) {
    this.active = active;
  }

  public Boolean isSilenced() {
    return silenced;
  }

  public void setSilenced(Boolean silenced) {
    this.silenced = silenced;
  }

  public Boolean isInhibited() {
    return inhibited;
  }

  public void setInhibited(Boolean inhibited) {
    this.inhibited = inhibited;
  }

  public Set<String> getFilter() {
    return filter;
  }

  public void setFilter(Set<String> filter) {
    this.filter = filter;
  }

  public String getReceiver() {
    return receiver;
  }

  public void setReceiver(String receiver) {
    this.receiver = receiver;
  }

  @Override
  public String toString() {
    return "AlertFilterBy{" +
        "active=" + active +
        ", silenced=" + silenced +
        ", inhibited=" + inhibited +
        ", filter=" + filter +
        ", receiver='" + receiver + '\'' +
        '}';
  }
}