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

package io.hops.hopsworks.api.featurestore.activities;

import io.hops.hopsworks.common.dao.AbstractFacade;
import io.hops.hopsworks.common.featurestore.activity.FeaturestoreActivityFacade;

public class FilterBy implements AbstractFacade.FilterBy {

  private FeaturestoreActivityFacade.Filters filter;
  private String param;

  public FilterBy(String param) {
    if (param.contains(":")) {
      this.filter = FeaturestoreActivityFacade.Filters.valueOf(param.substring(0, param.indexOf(":")).toUpperCase());
      this.param = param.substring(param.indexOf(":") + 1);
    } else {
      this.filter = FeaturestoreActivityFacade.Filters.valueOf(param.toUpperCase());
      this.param = filter.getDefaultParam();
    }
  }

  @Override
  public String getValue() {
    return filter.getValue();
  }

  @Override
  public String getParam() {
    return param;
  }

  @Override
  public String getSql() {
    return filter.getSql();
  }

  @Override
  public String getField() {
    return filter.getField();
  }

  @Override
  public String toString() {
    return filter.toString();
  }
}
