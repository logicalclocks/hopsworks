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
import io.hops.hopsworks.common.dao.host.HostsFacade;
import org.primefaces.model.LazyDataModel;
import org.primefaces.model.SortOrder;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class HostsLazyDataModel extends LazyDataModel<Hosts> {
  private HostsFacade hostsFacade;
  
  public HostsLazyDataModel(HostsFacade hostsFacade) {
    this.hostsFacade = hostsFacade;
  }
  
  @Override
  public List<Hosts> load(int first, int pageSize, String sortField, SortOrder sortOrder, Map<String, Object> filters) {
    List<Hosts> allHosts = hostsFacade.findAllHosts();
    
    // Filter by hostname and health
    List<Hosts> filteredItems = allHosts.parallelStream()
        .filter(h -> {
          boolean hostnamePredicate = true;
          boolean healthPredicate = true;
          if (filters != null) {
            String hostnameFilter = (String) filters.get("hostname");
            if (hostnameFilter != null) {
              hostnamePredicate = h.getHostname().startsWith(hostnameFilter);
            }
            String healthFilter = (String) filters.get("health");
            if (healthFilter != null) {
              healthPredicate = h.getHealth().name().toLowerCase().startsWith(healthFilter.toLowerCase());
            }
          }
          return hostnamePredicate && healthPredicate;
        })
        .collect(Collectors.toList());
  
    if (sortField != null && !sortField.isEmpty()) {
      filteredItems.sort(new HostsLazySorter(sortField, sortOrder));
    }
    
    int filteredItemsSize = filteredItems.size();
    this.setRowCount(filteredItemsSize);
  
    if(filteredItemsSize > pageSize) {
      try {
        return filteredItems.subList(first, first + pageSize);
      }
      catch(IndexOutOfBoundsException e) {
        return filteredItems.subList(first, first + (filteredItemsSize % pageSize));
      }
    }
    else {
      return filteredItems;
    }
  }
  
  @Override
  public Hosts getRowData(String rowKey) {
    return hostsFacade.findByHostname(rowKey);
  }
}
