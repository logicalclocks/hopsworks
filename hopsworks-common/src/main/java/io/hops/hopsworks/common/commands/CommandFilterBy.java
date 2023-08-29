/*
 * This file is part of Hopsworks
 * Copyright (C) 2023, Hopsworks AB. All rights reserved
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
package io.hops.hopsworks.common.commands;

import io.hops.hopsworks.common.dao.AbstractFacade;
import io.hops.hopsworks.common.dao.commands.CommandFilter;

import java.util.Objects;

public class CommandFilterBy implements AbstractFacade.FilterBy {
  
  private final CommandFilter filter;
  private final String param;
  
  public CommandFilterBy(CommandFilter filter, String param) {
    this.filter = filter;
    this.param = param;
  }
  
  public CommandFilterBy(CommandFilter filter) {
    this(filter, null);
  }
  
  @Override
  public String getValue() {
    return filter.getValue();
  }
  
  @Override
  public String getParam() {
    return param == null ? filter.getDefaultParam() : param;
  }
  
  @Override
  public String getSql() {
    return filter.getSql();
  }
  
  @Override
  public String getField() {
    return filter.getField();
  }
  
  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof CommandFilterBy)) {
      return false;
    }
    CommandFilterBy that = (CommandFilterBy) o;
    return filter == that.filter &&
      Objects.equals(param, that.param);
  }
  
  @Override
  public int hashCode() {
    return Objects.hash(filter, param);
  }
}