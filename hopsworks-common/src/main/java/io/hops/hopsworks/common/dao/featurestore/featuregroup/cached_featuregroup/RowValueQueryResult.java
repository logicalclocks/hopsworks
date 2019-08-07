/*
 * This file is part of Hopsworks
 * Copyright (C) 2018, Logical Clocks AB. All rights reserved
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

package io.hops.hopsworks.common.dao.featurestore.featuregroup.cached_featuregroup;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;
import java.util.List;

/**
 * DTO containing a row query result in String format
 * using jaxb.
 */
@XmlRootElement
@XmlType(propOrder = {"columns"})
public class RowValueQueryResult {

  private List<ColumnValueQueryResult> columns;

  public RowValueQueryResult(){}

  public RowValueQueryResult(List<ColumnValueQueryResult> columns) {
    this.columns = columns;
  }

  @XmlElement
  public List<ColumnValueQueryResult> getColumns() {
    return columns;
  }
}
