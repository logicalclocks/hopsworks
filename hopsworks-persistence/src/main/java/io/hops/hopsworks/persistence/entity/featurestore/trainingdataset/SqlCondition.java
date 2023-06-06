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

package io.hops.hopsworks.persistence.entity.featurestore.trainingdataset;

import org.apache.calcite.sql.fun.SqlStdOperatorTable;

public enum SqlCondition {
  LESS_THAN(SqlStdOperatorTable.LESS_THAN),
  GREATER_THAN(SqlStdOperatorTable.GREATER_THAN),
  LESS_THAN_OR_EQUAL(SqlStdOperatorTable.LESS_THAN_OR_EQUAL),
  GREATER_THAN_OR_EQUAL(SqlStdOperatorTable.GREATER_THAN_OR_EQUAL),
  EQUALS(SqlStdOperatorTable.EQUALS),
  NOT_EQUALS(SqlStdOperatorTable.NOT_EQUALS),
  IN(SqlStdOperatorTable.IN),
  LIKE(SqlStdOperatorTable.LIKE),
  IS(SqlStdOperatorTable.IS_NULL);
  
  
  public final org.apache.calcite.sql.SqlOperator operator;
  
  SqlCondition(org.apache.calcite.sql.SqlOperator operator) {
    this.operator = operator;
  }
  
}
