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

package io.hops.hopsworks.common.dao;

import java.util.HashSet;
import java.util.Set;

public class QueryParam {

  private Integer offset;
  private Integer limit;
  private Set<AbstractFacade.FilterBy> filters = new HashSet<>();
  private Set<AbstractFacade.SortBy> sorts = new HashSet<>();

  public QueryParam() {
  }

  public QueryParam(Integer offset, Integer limit,
      Set<AbstractFacade.FilterBy> filters,
      Set<AbstractFacade.SortBy> sorts) {
    this.offset = offset;
    this.limit = limit;
    this.filters = filters;
    this.sorts = sorts;
  }

  public Integer getOffset() {
    return offset;
  }

  public Integer getLimit() {
    return limit;
  }

  public Set<AbstractFacade.FilterBy> getFilters() {
    return filters;
  }

  public Set<AbstractFacade.SortBy> getSorts() {
    return sorts;
  }
}
