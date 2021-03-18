/*
 * This file is part of Hopsworks
 * Copyright (C) 2018, Logical Clocks AB. All rights reserved
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
package io.hops.hopsworks.api.util;

import io.hops.hopsworks.common.provenance.core.PaginationParams;
import io.swagger.annotations.ApiParam;
import javax.ws.rs.QueryParam;

public class Pagination implements PaginationParams {

  @QueryParam("offset")
  @ApiParam(required = false)
  private Integer offset;

  @QueryParam("limit")
  @ApiParam(required = false)
  private Integer limit;

  public Pagination(
      @QueryParam("offset") Integer offset,
      @QueryParam("limit") Integer limit) {
    this.offset = offset;
    this.limit = limit;
  }

  public Pagination() {}
  
  @Override
  public Integer getOffset() {
    return offset;
  }

  public void setOffset(Integer offset) {
    this.offset = offset;
  }
  
  @Override
  public Integer getLimit() {
    return limit;
  }

  public void setLimit(Integer limit) {
    this.limit = limit;
  }

  @Override
  public String toString() {
    return "Pagination{" + "offset=" + offset + ", limit=" + limit + '}';
  }

}
