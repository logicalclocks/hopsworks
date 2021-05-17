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

package io.hops.hopsworks.api.alert.silence;


import io.swagger.annotations.ApiParam;

import javax.ws.rs.QueryParam;
import java.util.HashSet;
import java.util.Set;

public class SilenceFilterBy {
  
  @QueryParam("createdBy")
  @ApiParam(value = "Filter by created by.")
  private String createdBy;
  @QueryParam("status")
  @ApiParam(allowableValues = "expired, active, pending")
  private String status;
  @QueryParam("filter")
  @ApiParam(value = "A list of matchers to filter silences by. ex. filter=jobId=\"12\"", allowMultiple = true)
  private Set<String> filter;
  
  public SilenceFilterBy(@QueryParam("createdBy") String createdBy, @QueryParam("status") String status,
      @QueryParam("filter") Set<String> filter) {
    this.createdBy = createdBy;
    this.status = status;
    this.filter = filter == null? new HashSet<>() : filter;
  }
  
  public String getCreatedBy() {
    return createdBy;
  }
  
  public void setCreatedBy(String createdBy) {
    this.createdBy = createdBy;
  }
  
  public String getStatus() {
    return status;
  }
  
  public void setStatus(String status) {
    this.status = status;
  }
  
  public Set<String> getFilter() {
    return filter;
  }
  
  public void setFilter(Set<String> filter) {
    this.filter = filter;
  }
}