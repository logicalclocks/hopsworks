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
import org.primefaces.model.SortOrder;

public class SortBy implements AbstractFacade.SortBy {
  
  private final UserFacade.Sorts sortBy;
  private final AbstractFacade.OrderBy orderBy;
  
  public SortBy(String sortField, SortOrder sortOrder) {
    this.sortBy = getSortBy(sortField);
    this.orderBy = getOrderBy(sortOrder);
  }
  
  private UserFacade.Sorts getSortBy(String sortField) {
    switch (sortField) {
      case "fname":
        return UserFacade.Sorts.FIRST_NAME;
      case "lname":
        return UserFacade.Sorts.LAST_NAME;
      case "email":
        return UserFacade.Sorts.EMAIL;
      case "activated":
        return UserFacade.Sorts.DATE_CREATED;
      default:
        throw new UnsupportedOperationException();
    }
  }
  
  private AbstractFacade.OrderBy getOrderBy(SortOrder sortOrder) {
    switch (sortOrder) {
      case UNSORTED:
      case ASCENDING:
        return AbstractFacade.OrderBy.ASC;
      case DESCENDING:
        return AbstractFacade.OrderBy.DESC;
      default:
        throw new UnsupportedOperationException();
    }
  }
  
  @Override
  public String getValue() {
    return this.sortBy.getValue();
  }
  
  @Override
  public AbstractFacade.OrderBy getParam() {
    return this.orderBy;
  }
  
  @Override
  public String getSql() {
    return this.sortBy.getSql();
  }
}
