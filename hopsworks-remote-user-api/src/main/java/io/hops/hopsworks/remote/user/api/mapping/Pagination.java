/*
 * Copyright (C) 2023, Hopsworks AB. All rights reserved
 */
package io.hops.hopsworks.remote.user.api.mapping;

import io.swagger.annotations.ApiParam;

import javax.ws.rs.QueryParam;

public class Pagination {
  
  @QueryParam("offset")
  @ApiParam(required = false)
  private Integer offset;
  
  @QueryParam("limit")
  @ApiParam(required = false)
  private Integer limit;
  
  public Pagination(@QueryParam("offset") Integer offset, @QueryParam("limit") Integer limit) {
    this.offset = offset;
    this.limit = limit;
  }
  
  public Pagination() {
  }
  
  public Integer getOffset() {
    return offset;
  }
  
  public void setOffset(Integer offset) {
    this.offset = offset;
  }
  
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
