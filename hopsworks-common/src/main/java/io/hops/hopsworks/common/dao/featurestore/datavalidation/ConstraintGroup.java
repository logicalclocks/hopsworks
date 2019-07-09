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

public class ConstraintGroup {
  private String name;
  private String description;
  private ConstraintGroupLevel level;
  private List<Constraint> constraints;
  
  public ConstraintGroup() {}
  
  public String getName() {
    return name;
  }
  
  public void setName(String name) {
    this.name = name;
  }
  
  public String getDescription() {
    return description;
  }
  
  public void setDescription(String description) {
    this.description = description;
  }
  
  public ConstraintGroupLevel getLevel() {
    return level;
  }
  
  public void setLevel(ConstraintGroupLevel level) {
    this.level = level;
  }
  
  public List<Constraint> getConstraints() {
    return constraints;
  }
  
  public void setConstraints(List<Constraint> constraints) {
    this.constraints = constraints;
  }
}
