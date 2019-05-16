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
package io.hops.hopsworks.api.dataset.inode;

import io.hops.hopsworks.common.api.ResourceRequest;

import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Set;

public class InodeResourceRequest extends ResourceRequest {
  
  public InodeResourceRequest(Name name, String queryParam) {
    super(name, queryParam);
    //Set sort_by
    Set<InodeFilterBy> filters = null;
    for (String queryProp : queryProps) {
      if (queryProp.startsWith("sort_by")) {
        String[] params = queryProp.substring(queryProp.indexOf('=') + 1).split(",");
        //Hash table and linked list implementation of the Set interface, with predictable iteration order
        Set<InodeSortBy> sortBys = new LinkedHashSet<>();//make ordered
        InodeSortBy sort;
        for (String s : params) {
          sort = new InodeSortBy(s.trim());
          sortBys.add(sort);
        }
        super.setSort(sortBys);
      } else if (queryProp.startsWith("filter_by")) {
        if (filters == null) {
          filters = new HashSet<>();
        }
        //Set filter_by
        InodeFilterBy filterBy = new InodeFilterBy(queryProp.substring(queryProp.indexOf('=') + 1));
        filters.add(filterBy);
      }
    }
    super.setFilter(filters);
  }
}