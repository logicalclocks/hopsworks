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
package io.hops.hopsworks.api.python.conflicts;

import io.hops.hopsworks.api.experiments.FilterBy;
import io.hops.hopsworks.common.api.ResourceRequest;

import java.util.LinkedHashSet;
import java.util.Set;

public class ConflictsResourceRequest extends ResourceRequest {

  public ConflictsResourceRequest(Name name, String queryParam) {
    super(name, queryParam);
    //Set filter_by
    for (String queryProp : queryProps) {
      if (queryProp.startsWith("filter_by")) {
        String[] params = queryProp.substring(queryProp.indexOf('=')+1).split(",");
        //Hash table and linked list implementation of the Set interface, with predictable iteration order
        Set<FilterBy> filterBys = new LinkedHashSet<>();//make ordered
        FilterBy filterBy;
        for (String s : params) {
          filterBy = new FilterBy(s.trim());
          filterBys.add(filterBy);
        }
        super.setFilter(filterBys);
      }
    }
  }
}
