/*
 * This file is part of Hopsworks
 * Copyright (C) 2020, Logical Clocks AB. All rights reserved
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
package io.hops.hopsworks.api.provenance.ops;

import io.hops.hopsworks.common.provenance.ops.ProvOpsReturnType;
import io.swagger.annotations.ApiParam;

import javax.ws.rs.DefaultValue;
import javax.ws.rs.QueryParam;
import java.util.List;
import java.util.Set;

public class ProvOpsBeanParams {
  @QueryParam("filter_by")
  @ApiParam(value = "ex. filter_by=FILE_NAME:file1, filter_by=PROJECT_I_ID:id1, filter_by=FILE_I_ID:id1, " +
    "filter_by=FILE_NAME:file1, filter_by=FILE_NAME_LIKE:fil, " +
    "filter_by=USER_ID:user1, filter_by=APP_ID:app1," +
    "filter_by=TIMESTAMP:10030042, " +
    "filter_by=TIMESTAMP_LT:10030042, filter_by=TIMESTAMP_GT:10010042" +
    "filter_by=TIMESTAMP_LTE:10030042, filter_by=TIMESTAMP_GTE:10010042," +
    "filter_by=FILE_OPERATION:CREATE/DELETE/ACCESS_DATA/MODIFY_DATA",
    allowMultiple = true)
  private Set<String> fileOpsFilterBy;
  
  @QueryParam("sort_by")
  @ApiParam(value = "sort_by=PROJECT_I_ID:asc, sort_by=FILE_I_ID:desc, sort_by=FILE_NAME:asc, " +
      "sort_by=FILE_OPERATION:asc, sort_by=USER_ID:asc, sort_by=APP_ID:asc,sort_by=TIMESTAMP:asc",
    allowMultiple = true)
  private List<String> fileOpsSortBy;
  
  @QueryParam("prov_expand")
  @ApiParam(value = "ex. prov_expand=APP_STATE")
  private Set<String> provExpansions;
  
  @QueryParam("app_filter_by")
  @ApiParam(value = "app_filter_by=APP_ID:appId, app_filter_by=APP_STATE:state",
    allowMultiple = true)
  private Set<String> appExpansionParams;
  
  @QueryParam("aggregations")
  @ApiParam(value = "aggregations=FILES_IN, FILES_LEAST_ACTIVE_BY_LAST_ACCESSED, " +
      "PROJECTS_LEAST_ACTIVE_BY_LAST_ACCESSED",
    allowMultiple = true)
  private Set<String> aggregations;
  
  @QueryParam("return_type")
  @DefaultValue("LIST")
  private ProvOpsReturnType returnType;
  
  public ProvOpsBeanParams(
    @QueryParam("filter_by") Set<String> fileOpsFilterBy,
    @QueryParam("sort_by") List<String> fileOpsSortBy,
    @QueryParam("expand") Set<String> expansions,
    @QueryParam("app_filter_by") Set<String> appExpansionParams,
    @QueryParam("aggregations") Set<String> aggregations,
    @QueryParam("return_type") @DefaultValue("LIST") ProvOpsReturnType returnType) {
    
    this.fileOpsFilterBy = fileOpsFilterBy;
    this.fileOpsSortBy = fileOpsSortBy;
    this.provExpansions = expansions;
    this.appExpansionParams = appExpansionParams;
    this.aggregations = aggregations;
    this.returnType = returnType;
  }
  
  public ProvOpsBeanParams() {}
  
  public Set<String> getFileOpsFilterBy() {
    return fileOpsFilterBy;
  }
  
  public void setFileOpsFilterBy(Set<String> fileOpsFilterBy) {
    this.fileOpsFilterBy = fileOpsFilterBy;
  }
  
  public List<String> getFileOpsSortBy() {
    return fileOpsSortBy;
  }
  
  public void setFileOpsSortBy(List<String> fileOpsSortBy) {
    this.fileOpsSortBy = fileOpsSortBy;
  }
  
  public Set<String> getProvExpansions() {
    return provExpansions;
  }
  
  public void setProvExpansions(Set<String> provExpansions) {
    this.provExpansions = provExpansions;
  }
  
  public Set<String> getAppExpansionParams() {
    return appExpansionParams;
  }
  
  public void setAppExpansionParams(Set<String> appExpansionParams) {
    this.appExpansionParams = appExpansionParams;
  }
  
  public Set<String> getAggregations() {
    return aggregations;
  }
  
  public void setAggregations(Set<String> aggregations) {
    this.aggregations = aggregations;
  }
  
  public ProvOpsReturnType getReturnType() {
    return returnType;
  }
  
  public void setReturnType(ProvOpsReturnType returnType) {
    this.returnType = returnType;
  }
}
