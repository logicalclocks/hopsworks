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
package io.hops.hopsworks.api.activities;

import io.hops.hopsworks.common.dao.AbstractFacade;
import io.hops.hopsworks.common.dao.user.activity.ActivityFacade;

public class SortBy implements AbstractFacade.SortBy {

  private final ActivityFacade.Sorts sortBy;
  private final AbstractFacade.OrderBy param;

  public SortBy(String param) {
    String[] sortByParams = param.split(":");
    this.sortBy = ActivityFacade.Sorts.valueOf(sortByParams[0].toUpperCase());
    String order = sortByParams.length > 1 ? sortByParams[1].toUpperCase() : this.sortBy.getDefaultParam();
    this.param = AbstractFacade.OrderBy.valueOf(order);
  }

  @Override
  public String getValue() {
    return this.sortBy.getValue();
  }

  @Override
  public AbstractFacade.OrderBy getParam() {
    return this.param;
  }

  @Override
  public String getSql() {
    return this.sortBy.getSql();
  }

}
