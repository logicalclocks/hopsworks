/*
 * This file is part of Logical Clocks
 * Copyright (C) 2022, Logical Clocks AB. All rights reserved
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

package io.hops.hopsworks.common.featurestore.app;

public enum JobEntityType {
  
  TD("td"),
  FG("fg"),
  FV("fv"),
  EXTERNAL_FG("external_fg");
  
  public final String label;
  
  private JobEntityType(String label) {
    this.label = label;
  }
  
  @Override
  public String toString() {
    return this.label;
  }
}
