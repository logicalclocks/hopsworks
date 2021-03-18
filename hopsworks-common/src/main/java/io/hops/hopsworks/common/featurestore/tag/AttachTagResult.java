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
package io.hops.hopsworks.common.featurestore.tag;

import java.util.Map;

public class AttachTagResult {
  private boolean created;
  private Map<String, String> items;
  
  public AttachTagResult(Map<String, String> items, boolean created) {
    this.created = created;
    this.items = items;
  }
  
  public boolean isCreated() {
    return created;
  }
  
  public void setCreated(boolean created) {
    this.created = created;
  }
  
  public Map<String, String> getItems() {
    return items;
  }
  
  public void setItems(Map<String, String> items) {
    this.items = items;
  }
}
