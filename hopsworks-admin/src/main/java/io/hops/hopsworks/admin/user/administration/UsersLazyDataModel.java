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
package io.hops.hopsworks.admin.user.administration;

import io.hops.hopsworks.common.api.ResourceRequest;
import io.hops.hopsworks.common.dao.AbstractFacade;
import io.hops.hopsworks.common.dao.user.UserFacade;
import io.hops.hopsworks.common.dao.user.Users;
import org.primefaces.model.LazyDataModel;
import org.primefaces.model.SortOrder;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;

public class UsersLazyDataModel extends LazyDataModel<Users> {
  
  private static final Logger LOGGER = Logger.getLogger(UsersAdministrationBean.class.getName());
  private UserFacade userFacade;
  
  public UsersLazyDataModel(UserFacade userFacade) {
    this.userFacade = userFacade;
    this.setRowCount((int) userFacade.count());
  }
  
  @Override
  public List<Users> load(int first, int pageSize, String sortField, SortOrder sortOrder, Map<String, Object> filters) {
    ResourceRequest resourceRequest = new ResourceRequest(ResourceRequest.Name.USERS);
    resourceRequest.setOffset(first);
    resourceRequest.setLimit(pageSize);
    resourceRequest.setFilter(getFilters(filters));
    Set<UserFacade.SortBy> sort = new HashSet<>();
    AbstractFacade.CollectionInfo collectionInfo =
      userFacade.findAll(first, pageSize, resourceRequest.getFilter(), sort);
    this.setRowCount(collectionInfo.getCount().intValue());
    return collectionInfo.getItems();
  }
  
  @Override
  public Users getRowData(String rowKey) {
    return userFacade.findByEmail(rowKey);
  }
  
  private Set<UserFacade.FilterBy> getFilters(Map<String, Object> filters) {
    Set<UserFacade.FilterBy> filterSet = new HashSet<>();
    for (String filter : filters.keySet()) {
      filterSet.add(new FilterBy(filter, (String) filters.get(filter)));
    }
    return filterSet;
  }
  
}
