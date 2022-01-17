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
package io.hops.hopsworks.common.featurestore.query.filter;

import java.util.Objects;

public class FilterValue {

  private Integer featureGroupId;
  private String featureGroupAlias;
  private String value;

  public FilterValue(String value) {
    this.value = value;
  }

  public FilterValue(Integer featureGroupId, String featureGroupAlias, String value) {
    this.featureGroupId = featureGroupId;
    this.featureGroupAlias = featureGroupAlias;
    this.value = value;
  }

  public String getValue() {
    return value;
  }

  public void setValue(String value) {
    this.value = value;
  }

  public boolean isFeatureValue() {
    return featureGroupId != null || featureGroupAlias != null;
  }

  public Integer getFeatureGroupId() {
    return featureGroupId;
  }

  public String makeSqlValue() {
    return featureGroupAlias ==  null ? value : String.format("`%s`.`%s`", featureGroupAlias, value);
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    FilterValue that = (FilterValue) o;
    return Objects.equals(featureGroupId, that.featureGroupId) &&
        Objects.equals(featureGroupAlias, that.featureGroupAlias) &&
        Objects.equals(value, that.value);
  }

  @Override
  public int hashCode() {
    return Objects.hash(featureGroupId, featureGroupAlias, value);
  }
}
