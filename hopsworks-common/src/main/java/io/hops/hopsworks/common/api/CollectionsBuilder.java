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
package io.hops.hopsworks.common.api;

import io.hops.hopsworks.common.dao.AbstractFacade;
import io.hops.hopsworks.persistence.entity.project.Project;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public abstract class CollectionsBuilder<T> {
  
  protected abstract List<T> getAll(Project project);
  
  protected abstract List<T> filterTopics(List<T> list, AbstractFacade.FilterBy filterBy);
  
  protected abstract List<T> sortTopics(List<T> list, Set<? extends AbstractFacade.SortBy> sortBySet);
  
  public List<T> buildItems(Project project, ResourceRequest resourceRequest) {
    List<T> list = getAll(project);
  
    for (AbstractFacade.FilterBy tFilter : resourceRequest.getFilter()) {
      list = filterTopics(list, tFilter);
    }
  
    list = sortTopics(list, resourceRequest.getSort());
  
    list = paginate(list, resourceRequest.getLimit(), resourceRequest.getOffset());
  
    return list;
  }
  
  private List<T> paginate(List<T> list, Integer limit, Integer offset) {
    Stream<T> stream = list.stream();
    if (offset != null && offset > 0) {
      stream = stream.skip(offset);
    }
    if (limit != null && limit > 0) {
      stream = stream.limit(limit);
    }
    return stream.collect(Collectors.toList());
  }
}
