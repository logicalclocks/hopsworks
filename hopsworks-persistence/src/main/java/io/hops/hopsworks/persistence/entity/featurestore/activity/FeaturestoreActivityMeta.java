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

package io.hops.hopsworks.persistence.entity.featurestore.activity;

public enum FeaturestoreActivityMeta {
  // Don't change the order
  FG_CREATED("Feature group was created"),
  FG_ALTERED("Feature group was altered"),
  ONLINE_ENABLED("Feature group available online"),
  ONLINE_DISABLED("Feature group not available online"),
  TD_CREATED("The training dataset was created");

  private String value;

  FeaturestoreActivityMeta(String value) {
    this.value = value;
  }

  public String getValue() {
    return value;
  }
}
