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
package io.hops.hopsworks.api.dataset;

import io.hops.hopsworks.common.dao.AbstractFacade;
import io.hops.hopsworks.common.dao.dataset.DatasetSharedWithFacade;

public class DatasetSharedWithFilterBy implements AbstractFacade.FilterBy {
  
  private final DatasetSharedWithFacade.Filters filter;
  private final String param;
  
  public DatasetSharedWithFilterBy(DatasetFilterBy filterBy) {
    this.filter = DatasetSharedWithFacade.Filters.valueOf(filterBy.getValue());
    this.param = filterBy.getParam();
  }
  
  @Override
  public String getParam() {
    return param;
  }
  
  @Override
  public String getValue() {
    return this.filter.getValue();
  }
  
  @Override
  public String getSql() {
    return this.filter.getSql();
  }
  
  @Override
  public String getField() {
    return this.filter.getField();
  }
  
  @Override
  public String toString() {
    return filter.toString();
  }
  
}