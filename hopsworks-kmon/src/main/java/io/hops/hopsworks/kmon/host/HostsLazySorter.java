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

package io.hops.hopsworks.kmon.host;

import io.hops.hopsworks.common.dao.host.Hosts;
import org.primefaces.model.SortOrder;

import java.lang.reflect.Field;
import java.util.Comparator;

public class HostsLazySorter implements Comparator<Hosts> {
  
  private final String sortField;
  private final SortOrder sortOrder;
  
  public HostsLazySorter(String sortField, SortOrder sortOrder) {
    this.sortField = sortField;
    this.sortOrder = sortOrder;
  }
  
  public int compare(Hosts host0, Hosts host1) {
    try {
      Field compareField = Hosts.class.getDeclaredField(sortField);
      compareField.setAccessible(true);
      
      Object value1 = compareField.get(host0);
      Object value2 = compareField.get(host1);
      int value = ((Comparable) value1).compareTo(value2);
      return SortOrder.ASCENDING.equals(sortOrder) ? value : -1 * value;
    } catch (Exception ex) {
      throw new RuntimeException(ex);
    }
  }
}
