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

package io.hops.hopsworks.common.featurestore.featureview;

import io.hops.hopsworks.common.dao.AbstractFacade;

public class FeatureViewFilterBy implements AbstractFacade.FilterBy {

  private final FeatureViewFacade.Filters filter;
  private final String value;
  private final String param;

  public FeatureViewFilterBy(String field, String value, String param) {
    this.filter = FeatureViewFacade.Filters.valueOf(field.toUpperCase());
    this.value = value;
    this.param = param;
  }

  public FeatureViewFilterBy(String value) {
    if (value.contains(":")) {
      this.filter = FeatureViewFacade.Filters.valueOf(value.substring(0, value.indexOf(':')).toUpperCase());
      this.value = value.substring(value.indexOf(':') + 1);
      this.param = filter.getField();
    } else {
      this.filter = FeatureViewFacade.Filters.valueOf(value.toUpperCase());
      this.value = filter.getDefaultValue();
      this.param = filter.getField();

    }
  }

  @Override
  public String getParam() {
    return param;
  }

  @Override
  public String getValue() {
    return value;
  }

  @Override
  public String getSql() {
    return String.format(this.filter.getSql(), param);
  }

  @Override
  public String getField() {
    return this.filter.getField();
  }

  @Override
  public String toString() {
    return filter.getName();
  }
}
