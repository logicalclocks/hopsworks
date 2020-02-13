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

import io.hops.hopsworks.common.dao.AbstractFacade;
import io.hops.hopsworks.common.dao.user.UserFacade;
import io.hops.hopsworks.persistence.entity.user.security.ua.UserAccountStatus;

import java.util.HashSet;
import java.util.Set;

public class SpamUsersAdminLazyDataModel extends UsersLazyDataModel {
  
  public SpamUsersAdminLazyDataModel(UserFacade userFacade) {
    super(userFacade);
    Set<UserFacade.FilterBy> filter = new HashSet<>();
    UserFacade.FilterBy fby = new FilterBy(UserFacade.Filters.STATUS, UserAccountStatus.SPAM_ACCOUNT.name());
    filter.add(fby);
    long count = userFacade.countWithFilter(filter);
    this.setRowCount((int) count);
  }
  
  @Override
  public AbstractFacade.CollectionInfo getUsers(int first, int pageSize, Set<UserFacade.FilterBy> filter,
    Set<UserFacade.SortBy> sort) {
    UserFacade.FilterBy fby = new FilterBy(UserFacade.Filters.STATUS, UserAccountStatus.SPAM_ACCOUNT.name());
    filter.add(fby);
    return userFacade.findAll(first, pageSize, filter, sort);
  }
}
