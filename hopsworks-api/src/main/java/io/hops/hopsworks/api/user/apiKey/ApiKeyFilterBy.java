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
package io.hops.hopsworks.api.user.apiKey;

import io.hops.hopsworks.api.auth.key.ApiKeyFacade;
import io.hops.hopsworks.common.dao.AbstractFacade;

public class ApiKeyFilterBy implements AbstractFacade.FilterBy {
  
  private final ApiKeyFacade.Filters filter;
  private final String param;
  
  public ApiKeyFilterBy(String param) {
    int index = param.indexOf(':');
    if (index >= 0) {
      this.filter = ApiKeyFacade.Filters.valueOf(param.substring(0, index).toUpperCase());
      this.param = param.substring(index + 1).isEmpty() ? this.filter.getDefaultParam() : param.substring(index + 1);
    } else {
      this.filter = ApiKeyFacade.Filters.valueOf(param.toUpperCase());
      this.param = this.filter.getDefaultParam();
    }
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