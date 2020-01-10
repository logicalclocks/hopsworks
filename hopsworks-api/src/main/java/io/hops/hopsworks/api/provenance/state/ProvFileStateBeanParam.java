/*
 * This file is part of Hopsworks
 * Copyright (C) 2019, Logical Clocks AB. All rights reserved
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
package io.hops.hopsworks.api.provenance.state;

import io.hops.hopsworks.api.provenance.ProjectProvenanceResource;
import io.swagger.annotations.ApiParam;

import javax.ws.rs.DefaultValue;
import javax.ws.rs.QueryParam;
import java.util.List;
import java.util.Set;

public class ProvFileStateBeanParam {
  @QueryParam("filter_by")
  @ApiParam(value = "ex. filter_by=FILE_NAME:file1",
    allowableValues = "filter_by=PROJECT_I_ID:id1, filter_by=FILE_I_ID:123, " +
      "filter_by=FILE_NAME:file1, filter_by=FILE_NAME_LIKE:fil, " +
      "filter_by=USER_ID:user1, filter_by=APP_ID:app1, " +
      "filter_by=CREATE_TIMESTAMP:10030042, " +
      "filter_by=CREATE_TIMESTAMP_LT:10030042, filter_by=CREATE_TIMESTAMP_GT:10010042," +
      "filter_by=CREATE_TIMESTAMP_LTE:10030042, filter_by=CREATE_TIMESTAMP_GTE:10010042, " +
      "filter_by=ML_TYPE:EXPERIMENT, filter_by=ML_ID:id",
    allowMultiple = true)
  private Set<String> fileStateParams;
  
  @QueryParam("sort_by")
  @ApiParam(value = "ex. sort_by=CREATE_TIMESTAMP:asc",
    allowableValues = "sort_by=PROJECT_I_ID:asc, sort_by=FILE_I_ID:asc, sort_by=FILE_NAME:asc, " +
      "sort_by=USER_ID:asc, sort_by=APP_ID:asc, sort_by=CREATE_TIMESTAMP:asc, " +
      "sort_by=ML_TYPE:asc, sort_by=ML_ID:asc",
    allowMultiple = true)
  private List<String> fileStateSortBy;
  
  @QueryParam("xattr_filter_by")
  @ApiParam(value = "ex. xattr_filter_by=name:val",
    allowableValues = "xattr_filter_by=details:model",
    allowMultiple = true)
  private Set<String> exactXAttrParams;
  
  @QueryParam("xattr_like")
  @ApiParam(value = "ex. xattr_like=name:val",
    allowableValues = "xattr_like=details:model",
    allowMultiple = true)
  private Set<String> likeXAttrParams;
  
  @QueryParam("filter_by_has_xattr")
  @ApiParam(value = "ex. filter_by_has_xattr=xattr1",
    allowableValues = "filter_by_has_xattr=xattr1",
    allowMultiple = true)
  private Set<String> filterByHasXAttrs;
  
  @QueryParam("xattr_sort_by")
  @ApiParam(value = "ex. xattr_sort_by=xattr1:asc",
    allowMultiple = true)
  private List<String> xattrSortBy;
  
  @QueryParam("expand")
  @ApiParam(value = "ex. expand=APP_STATE&expand=DIR_TREE", allowableValues = "expand=appState&expand=DIR_TREE")
  private Set<String> expansions;
  
  @QueryParam("app_filter_by")
  @ApiParam(value = "ex. app_filter_by=APP_STATE:state",
    allowableValues = "app_filter_by=APP_ID:appId, app_filter_by=APP_STATE:state",
    allowMultiple = true)
  private Set<String> appStateParams;
  
  @QueryParam("return_type")
  @DefaultValue("LIST")
  private ProjectProvenanceResource.FileStructReturnType returnType;
  
  public ProvFileStateBeanParam(
    @QueryParam("filter_by") Set<String> fileStateParams,
    @QueryParam("sort_by") List<String> fileStateSortBy,
    @QueryParam("xattr_filter_by") Set<String> xAttrParams,
    @QueryParam("xattr_like") Set<String> xAttrLikeParams,
    @QueryParam("filter_by_has_xattr") Set<String> filterByHasXAttrs,
    @QueryParam("xattr_sort_by") List<String> xattrSortBy,
    @QueryParam("expand") Set<String> expansions,
    @QueryParam("exp_filter_by") Set<String> appStateParams,
    @QueryParam("return_type") @DefaultValue("LIST") ProjectProvenanceResource.FileStructReturnType returnType) {
    this.fileStateParams = fileStateParams;
    this.fileStateSortBy = fileStateSortBy;
    this.exactXAttrParams = xAttrParams;
    this.likeXAttrParams = xAttrLikeParams;
    this.filterByHasXAttrs = filterByHasXAttrs;
    this.xattrSortBy = xattrSortBy;
    this.expansions = expansions;
    this.appStateParams = appStateParams;
    this.returnType = returnType;
  }
  
  public Set<String> getFileStateFilterBy() {
    return fileStateParams;
  }
  
  public void setFileStateParams(Set<String> fileStateParams) {
    this.fileStateParams = fileStateParams;
  }
  
  public List<String> getFileStateSortBy() {
    return fileStateSortBy;
  }
  
  public void setFileStateSortBy(List<String> fileStateSortBy) {
    this.fileStateSortBy = fileStateSortBy;
  }
  
  public Set<String> getExactXAttrParams() {
    return exactXAttrParams;
  }
  
  public void setExactXAttrParams(Set<String> exactXAttrParams) {
    this.exactXAttrParams = exactXAttrParams;
  }
  
  public Set<String> getLikeXAttrParams() {
    return likeXAttrParams;
  }
  
  public void setLikeXAttrParams(Set<String> likeXAttrParams) {
    this.likeXAttrParams = likeXAttrParams;
  }
  
  public Set<String> getFilterByHasXAttrs() {
    return filterByHasXAttrs;
  }
  
  public void setFilterByHasXAttrs(Set<String> filterByHasXAttrs) {
    this.filterByHasXAttrs = filterByHasXAttrs;
  }
  
  public List<String> getXattrSortBy() {
    return xattrSortBy;
  }
  
  public void setXattrSortBy(List<String> xattrSortBy) {
    this.xattrSortBy = xattrSortBy;
  }
  
  public Set<String> getExpansions() {
    return expansions;
  }
  
  public void setExpansions(Set<String> expansions) {
    this.expansions = expansions;
  }
  
  public Set<String> getAppExpansionParams() {
    return appStateParams;
  }
  
  public void setAppStateParams(Set<String> appStateParams) {
    this.appStateParams = appStateParams;
  }
  
  public ProjectProvenanceResource.FileStructReturnType getReturnType() {
    return returnType;
  }
  
  public void setReturnType(ProjectProvenanceResource.FileStructReturnType returnType) {
    this.returnType = returnType;
  }
  
  @Override
  public String toString() {
    return "ProvFileStateBeanParam{"
      + "file state filter by:" + fileStateParams.toString()
      + "file state sort by:" + fileStateSortBy.toString()
      + "filter by - exact xattr:" + exactXAttrParams.toString()
      + "filter by - like xattr:" + likeXAttrParams.toString()
      + "filter by - has xattr:" + filterByHasXAttrs.toString()
      + "xattr sort by" + xattrSortBy.toString()
      + "expansions:" + expansions
      + "app state:" + appStateParams.toString()
      + "return type:" + returnType
      + '}';
  }
}
