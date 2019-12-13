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
package io.hops.hopsworks.api.experiments;

import io.hops.hopsworks.common.dao.AbstractFacade;

public class FilterBy implements AbstractFacade.FilterBy {

  private String param = null;
  private String value = null;

  public FilterBy(String param) {
    if(param.contains(":")) {
      String[] paramSplit = param.split(":");
      this.param = paramSplit[0];
      if(this.param.compareToIgnoreCase(ExperimentsBuilder.Filters.DATE_START_LT.name()) == 0 ||
          this.param.compareToIgnoreCase(ExperimentsBuilder.Filters.DATE_START_GT.name()) == 0) {
        int splitIndex = param.indexOf(":");
        this.value = param.substring(splitIndex+1, param.length());
      } else {
        this.value = paramSplit[1];
      }
    }
  }

  public String getParam() {
    return param;
  }

  @Override
  public String getSql() {
    return null;
  }

  @Override
  public String getField() {
    return null;
  }

  public String getValue() {
    return value;
  }
}
