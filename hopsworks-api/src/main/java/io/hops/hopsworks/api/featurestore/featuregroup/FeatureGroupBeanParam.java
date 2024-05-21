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

package io.hops.hopsworks.api.featurestore.featuregroup;

import io.hops.hopsworks.api.util.Pagination;
import io.swagger.annotations.ApiParam;

import javax.ws.rs.BeanParam;
import javax.ws.rs.QueryParam;

import java.util.LinkedHashSet;
import java.util.Set;

public class FeatureGroupBeanParam {

  @QueryParam("filter_by")
  @ApiParam(value = "filter_by=latest_version",
      allowableValues = "filter_by=latest_version,filter_by=name:value,filter_by=version:value",
      allowMultiple = true)
  private Set<FilterBy> filter;

  @QueryParam("sort_by")
  @ApiParam(value = "sort_by=name,version",
      allowableValues = "sort_by=name,sort_by=version,sort_by=creation" +
          ",sort_by=name:asc,sort_by=version:asc,sort_by=creation:asc" +
          ",sort_by=name:desc,sort_by=version:desc,sort_by=creation:desc\n" +
          "sort by multiple fields is possible e.g sort_by=version,creation"
  )
  private String sortBy;

  private final Set<SortBy> parsedSortBy;

  @BeanParam
  private Pagination pagination;

  @BeanParam
  private FeaturegroupExpansionBeanParam expansion;

  public FeatureGroupBeanParam(
          @QueryParam("filter_by") Set<FilterBy> filter,
          @QueryParam("sort_by") String sortBy) {
    this.filter = filter;
    this.sortBy = sortBy;
    this.parsedSortBy = makeParsedSortBy(sortBy);
  }

  public Set<FilterBy> getFilters() {
    return filter;
  }

  public void setFilters(Set<FilterBy> filter) {
    this.filter = filter;
  }

  public String getSortBy() {
    return sortBy;
  }

  public void setSortBy(String sortBy) {
    this.sortBy = sortBy;
  }

  public Pagination getPagination() {
    return pagination;
  }

  public FeaturegroupExpansionBeanParam getExpansion() {
    return expansion;
  }

  public void setExpansion(FeaturegroupExpansionBeanParam expansion) {
    this.expansion = expansion;
  }

  public void setPagination(Pagination pagination) {
    this.pagination = pagination;
  }

  private Set<SortBy> makeParsedSortBy(String param) {
    if (param == null || param.isEmpty()) {
      return new LinkedHashSet<>();
    }
    String[] params = param.split(",");
    Set<SortBy> sortBys = new LinkedHashSet<>();//make ordered
    SortBy sort;
    for (String s : params) {
      sort = new SortBy(s.trim());
      sortBys.add(sort);
    }
    return sortBys;
  }

  public Set<SortBy> getParsedSortBy() {
    return parsedSortBy;
  }
}
