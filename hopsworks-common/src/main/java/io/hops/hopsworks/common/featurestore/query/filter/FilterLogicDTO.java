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

import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement
public class FilterLogicDTO {
  private SqlFilterLogic type;
  private FilterDTO leftFilter;
  private FilterDTO rightFilter;
  private FilterLogicDTO leftLogic;
  private FilterLogicDTO rightLogic;
  
  public FilterLogicDTO() {}
  
  public FilterLogicDTO(SqlFilterLogic type, FilterDTO leftFilter, FilterDTO rightFilter, FilterLogicDTO leftLogic,
                        FilterLogicDTO rightLogic) {
    this.type = type;
    this.leftFilter = leftFilter;
    this.rightFilter = rightFilter;
    this.leftLogic = leftLogic;
    this.rightLogic = rightLogic;
  }
  
  // for testing
  public FilterLogicDTO(SqlFilterLogic type) {
    this.type = type;
  }
  
  public SqlFilterLogic getType() {
    return type;
  }
  
  public void setType(SqlFilterLogic type) {
    this.type = type;
  }
  
  public FilterDTO getLeftFilter() {
    return leftFilter;
  }
  
  public void setLeftFilter(FilterDTO leftFilter) {
    this.leftFilter = leftFilter;
  }
  
  public FilterDTO getRightFilter() {
    return rightFilter;
  }
  
  public void setRightFilter(FilterDTO rightFilter) {
    this.rightFilter = rightFilter;
  }
  
  public FilterLogicDTO getLeftLogic() {
    return leftLogic;
  }
  
  public void setLeftLogic(FilterLogicDTO leftLogic) {
    this.leftLogic = leftLogic;
  }
  
  public FilterLogicDTO getRightLogic() {
    return rightLogic;
  }
  
  public void setRightLogic(FilterLogicDTO rightLogic) {
    this.rightLogic = rightLogic;
  }
}
