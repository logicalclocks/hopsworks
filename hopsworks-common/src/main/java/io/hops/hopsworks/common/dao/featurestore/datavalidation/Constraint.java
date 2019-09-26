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

package io.hops.hopsworks.common.dao.featurestore.datavalidation;

import java.util.List;

public class Constraint {
  private String name;
  private String hint;
  private Double min;
  private Double max;
  private List<String> columns;
  
  public Constraint() {}
  
  public String getName() {
    return name;
  }
  
  public void setName(String name) {
    this.name = name;
  }
  
  public String getHint() {
    return hint;
  }
  
  public void setHint(String hint) {
    this.hint = hint;
  }
  
  public Double getMin() {
    return min;
  }
  
  public void setMin(Double min) {
    this.min = min;
  }
  
  public Double getMax() {
    return max;
  }
  
  public void setMax(Double max) {
    this.max = max;
  }
  
  public List<String> getColumns() {
    return columns;
  }
  
  public void setColumns(List<String> columns) {
    this.columns = columns;
  }
}
