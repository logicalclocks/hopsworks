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

package io.hops.hopsworks.common.featurestore.query.filter;

public class FilterLogic {
  private SqlFilterLogic type;
  private Filter leftFilter;
  private Filter rightFilter;
  private FilterLogic leftLogic;
  private FilterLogic rightLogic;
  
  public FilterLogic() {}
  
  public FilterLogic(SqlFilterLogic type) {
    this.type = type;
  }

  public FilterLogic(Filter leftFilter) {
    this.type = SqlFilterLogic.SINGLE;
    this.leftFilter = leftFilter;
  }

  public FilterLogic(SqlFilterLogic type, Filter leftFilter, Filter rightFilter) {
    this.type = type;
    this.leftFilter = leftFilter;
    this.rightFilter = rightFilter;
  }

  public FilterLogic(SqlFilterLogic type, Filter leftFilter, FilterLogic rightLogic) {
    this.type = type;
    this.leftFilter = leftFilter;
    this.rightLogic = rightLogic;
  }

  public FilterLogic(SqlFilterLogic type, FilterLogic leftLogic, Filter rightFilter) {
    this.type = type;
    this.leftLogic = leftLogic;
    this.rightFilter = rightFilter;
  }

  public FilterLogic(SqlFilterLogic type, FilterLogic leftLogic, FilterLogic rightLogic) {
    this.type = type;
    this.leftLogic = leftLogic;
    this.rightLogic = rightLogic;
  }

  public FilterLogic and(Filter other) {
    return new FilterLogic(SqlFilterLogic.AND, this, other);
  }

  public FilterLogic and(FilterLogic other) {
    return new FilterLogic(SqlFilterLogic.AND, this, other);
  }

  public SqlFilterLogic getType() {
    return type;
  }
  
  public void setType(SqlFilterLogic type) {
    this.type = type;
  }
  
  public Filter getLeftFilter() {
    return leftFilter;
  }
  
  public void setLeftFilter(Filter leftFilter) {
    this.leftFilter = leftFilter;
  }
  
  public Filter getRightFilter() {
    return rightFilter;
  }
  
  public void setRightFilter(Filter rightFilter) {
    this.rightFilter = rightFilter;
  }
  
  public FilterLogic getLeftLogic() {
    return leftLogic;
  }
  
  public void setLeftLogic(FilterLogic leftLogic) {
    this.leftLogic = leftLogic;
  }
  
  public FilterLogic getRightLogic() {
    return rightLogic;
  }
  
  public void setRightLogic(FilterLogic rightLogic) {
    this.rightLogic = rightLogic;
  }
}
