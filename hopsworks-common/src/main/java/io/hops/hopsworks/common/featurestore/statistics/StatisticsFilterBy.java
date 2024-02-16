/*
 * This file is part of Hopsworks
 * Copyright (C) 2024, Hopsworks AB. All rights reserved
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

package io.hops.hopsworks.common.featurestore.statistics;

import io.hops.hopsworks.common.dao.AbstractFacade;

import java.util.Objects;

public class StatisticsFilterBy implements AbstractFacade.FilterBy {
  
  private final StatisticsFilters.Filters filter;
  private final String param;
  
  // used by QueryParam to instantiate StatisticsFilterBy objects
  public StatisticsFilterBy(String param) {
    if (param.startsWith("COMMIT_TIME")) {  // backwards compatibility
      param = param.replace("COMMIT_TIME", "COMPUTATION_TIME");
    }
    if (param.contains(":")) {
      String[] filterSplit = param.split(":");
      this.filter = StatisticsFilters.Filters.valueOf(filterSplit[0].toUpperCase());
      this.param = filterSplit[1];
    } else {
      this.filter = StatisticsFilters.Filters.valueOf(param);
      this.param = this.filter.getDefaultParam();
    }
  }
  
  public StatisticsFilterBy(StatisticsFilters.Filters filter, String param) {
    this.filter = filter;
    this.param = param != null ? param : filter.getDefaultParam();
  }

  @Override
  public String getValue() {
    return this.filter.getValue();
  }

  @Override
  public String getParam() {
    return this.param;
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
  public int hashCode() {
    return Objects.hash(filter, param);
  }
  
  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    StatisticsFilterBy that = (StatisticsFilterBy) o;
    if (!Objects.equals(filter.getValue(), that.filter.getValue())) {
      return false;
    }
    if (!Objects.equals(param, that.param)) {
      return false;
    }
    return true;
  }
  
  @Override
  public String toString() {
    return this.getValue() + ":" + this.getParam();
  }
}
