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

package io.hops.hopsworks.persistence.entity.featurestore.featuregroup.datavalidation;

import java.util.Arrays;

public class Rule {
  
  private Name name;
  private Level level;
  private Double min;
  private Double max;
  private String pattern;
  private ValueType acceptedType;
  private String[] legalValues;

  public Rule() {
  }
  
  public Name getName() {
    return name;
  }
  
  public void setName(Name name) {
    this.name = name;
  }

  public Level getLevel() {
    return level;
  }

  public void setLevel(Level level) {
    this.level = level;
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

  public String getPattern() {
    return pattern;
  }

  public void setPattern(String pattern) {
    this.pattern = pattern;
  }

  public ValueType getAcceptedType() {
    return acceptedType;
  }

  public void setAcceptedType(ValueType acceptedType) {
    this.acceptedType = acceptedType;
  }

  public String[] getLegalValues() {
    return legalValues;
  }

  public void setLegalValues(String[] legalValues) {
    this.legalValues = legalValues;
  }

  @Override
  public String toString() {
    return "Rule{" +
            "name=" + name +
            ", level=" + level +
            ", min=" + min +
            ", max=" + max +
            ", pattern='" + pattern + '\'' +
            ", acceptedType=" + acceptedType +
            ", legalValues=" + Arrays.toString(legalValues) +
            '}';
  }
}