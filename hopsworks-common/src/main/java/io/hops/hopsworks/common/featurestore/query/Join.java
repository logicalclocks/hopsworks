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

package io.hops.hopsworks.common.featurestore.query;

import io.hops.hopsworks.common.featurestore.feature.FeatureDTO;
import org.apache.calcite.sql.JoinConditionType;
import org.apache.calcite.sql.JoinType;
import org.apache.calcite.sql.SqlIdentifier;
import org.apache.calcite.sql.SqlJoin;
import org.apache.calcite.sql.SqlLiteral;
import org.apache.calcite.sql.SqlNode;
import org.apache.calcite.sql.SqlNodeList;
import org.apache.calcite.sql.fun.SqlStdOperatorTable;
import org.apache.calcite.sql.parser.SqlParserPos;

import java.util.Arrays;
import java.util.List;

public class Join {
  private String leftFeaturegroup;
  private String rightFeaturegroup;
  private List<FeatureDTO> on;
  private List<FeatureDTO> leftOn;
  private List<FeatureDTO> rightOn;
  private JoinType joinType;

  public Join(String leftFeaturegroup, String rightFeaturegroup, List<FeatureDTO> on, JoinType joinType) {
    this.leftFeaturegroup = leftFeaturegroup;
    this.rightFeaturegroup = rightFeaturegroup;
    this.on = on;
    this.joinType = joinType;
  }

  public Join(String leftFeaturegroup, String rightFeaturegroup, List<FeatureDTO> leftOn,
              List<FeatureDTO> rightOn, JoinType joinType) {
    this.leftFeaturegroup = leftFeaturegroup;
    this.rightFeaturegroup = rightFeaturegroup;
    this.leftOn = leftOn;
    this.rightOn = rightOn;
    this.joinType = joinType;
  }

  public String getLeftFeaturegroup() {
    return leftFeaturegroup;
  }

  public void setLeftFeaturegroup(String leftFeaturegroup) {
    this.leftFeaturegroup = leftFeaturegroup;
  }

  public String getRightFeaturegroup() {
    return rightFeaturegroup;
  }

  public void setRightFeaturegroup(String rightFeaturegroup) {
    this.rightFeaturegroup = rightFeaturegroup;
  }

  public List<FeatureDTO> getOn() {
    return on;
  }

  public void setOn(List<FeatureDTO> on) {
    this.on = on;
  }

  public List<FeatureDTO> getLeftOn() {
    return leftOn;
  }

  public void setLeftOn(List<FeatureDTO> leftOn) {
    this.leftOn = leftOn;
  }

  public List<FeatureDTO> getRightOn() {
    return rightOn;
  }

  public void setRightOn(List<FeatureDTO> rightOn) {
    this.rightOn = rightOn;
  }

  public JoinType getJoinType() {
    return joinType;
  }

  public void setJoinType(JoinType joinType) {
    this.joinType = joinType;
  }

  public SqlNode getJoinNode() {
    SqlNode conditionNode = getCondition();
    return new SqlJoin(SqlParserPos.ZERO, new SqlIdentifier(leftFeaturegroup, SqlParserPos.ZERO),
        SqlLiteral.createBoolean(false, SqlParserPos.ZERO),
        SqlLiteral.createSymbol(joinType, SqlParserPos.ZERO),
        new SqlIdentifier(rightFeaturegroup, SqlParserPos.ZERO),
        SqlLiteral.createSymbol(JoinConditionType.ON, SqlParserPos.ZERO),
        conditionNode);
  }

  private SqlNode getCondition() {
    if (on.size() > 1) {
      SqlNodeList conditionList = new SqlNodeList(SqlParserPos.ZERO);
      for (FeatureDTO f : on) {
        conditionList.add(generateEqualityCondition(leftFeaturegroup, rightFeaturegroup, f));
      }
      return  SqlStdOperatorTable.AND.createCall(conditionList);
    } else {
      return generateEqualityCondition(leftFeaturegroup, rightFeaturegroup, on.get(0));
    }
  }

  private SqlNode generateEqualityCondition(String leftName, String rightName, FeatureDTO on) {
    SqlNodeList equalityList = new SqlNodeList(SqlParserPos.ZERO);
    equalityList.add(new SqlIdentifier(Arrays.asList(leftName, on.getName()), SqlParserPos.ZERO));
    equalityList.add(new SqlIdentifier(Arrays.asList(rightName, on.getName()), SqlParserPos.ZERO));

    return SqlStdOperatorTable.EQUALS.createCall(equalityList);
  }
}
