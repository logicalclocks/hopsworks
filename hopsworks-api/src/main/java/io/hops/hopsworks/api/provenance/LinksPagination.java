/*
 * This file is part of Hopsworks
 * Copyright (C) 2022, Hopsworks AB. All rights reserved
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
package io.hops.hopsworks.api.provenance;

import io.swagger.annotations.ApiParam;

import javax.ws.rs.DefaultValue;
import javax.ws.rs.QueryParam;

public class LinksPagination {

  @QueryParam("upstreamLvls")
  @ApiParam(
    name = "upstreamLvls",
    value = "-1 for all upstream levels, positive integer for the number of traversed levels",
    example = "1",
    defaultValue = "1",
    required = false)
  @DefaultValue("1")
  private Integer upstreamLvls;

  @QueryParam("downstreamLvls")
  @ApiParam(
    name = "downstreamLvls",
    value = "-1 for all downstream levels, positive integer for the number of traversed levels",
    example = "1",
    defaultValue = "1",
    required = false)
  @DefaultValue("1")
  private Integer downstreamLvls;

  public LinksPagination(
      @QueryParam("upstreamLvls") Integer upstreamLvls,
      @QueryParam("downstreamLvls") Integer downstreamLvls) {
    this.upstreamLvls = upstreamLvls;
    this.downstreamLvls = downstreamLvls;
  }

  public LinksPagination() {}
  
  public Integer getUpstreamLvls() {
    return upstreamLvls;
  }
  
  public void setUpstreamLvls(Integer upstreamLvls) {
    this.upstreamLvls = upstreamLvls;
  }
  
  public Integer getDownstreamLvls() {
    return downstreamLvls;
  }
  
  public void setDownstreamLvls(Integer downstreamLvls) {
    this.downstreamLvls = downstreamLvls;
  }
}
