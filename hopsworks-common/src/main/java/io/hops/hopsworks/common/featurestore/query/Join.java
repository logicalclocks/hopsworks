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

package io.hops.hopsworks.common.featurestore.query;

import io.hops.hopsworks.common.featurestore.feature.FeatureDTO;
import org.apache.calcite.sql.JoinType;
import org.apache.calcite.sql.SqlIdentifier;
import org.apache.calcite.sql.SqlNode;
import org.apache.calcite.sql.SqlNodeList;
import org.apache.calcite.sql.fun.SqlStdOperatorTable;
import org.apache.calcite.sql.parser.SqlParserPos;

import java.util.Arrays;
import java.util.List;

public class Join {
  private Query leftQuery;
  private Query rightQuery;

  private List<FeatureDTO> on;
  private List<FeatureDTO> leftOn;
  private List<FeatureDTO> rightOn;
  private JoinType joinType;

  public Join(Query leftQuery) {
    this.leftQuery = leftQuery;
  }

  public Join(Query leftQuery, Query rightQuery, List<FeatureDTO> on, JoinType joinType) {
    this.leftQuery = leftQuery;
    this.rightQuery = rightQuery;
    this.on = on;
    this.joinType = joinType;
  }

  public Join(Query leftQuery, Query rightQuery, List<FeatureDTO> leftOn,
              List<FeatureDTO> rightOn, JoinType joinType) {
    this.leftQuery = leftQuery;
    this.rightQuery = rightQuery;
    this.leftOn = leftOn;
    this.rightOn = rightOn;
    this.joinType = joinType;
  }

  public Query getLeftQuery() {
    return leftQuery;
  }

  public void setLeftQuery(Query leftQuery) {
    this.leftQuery = leftQuery;
  }

  public Query getRightQuery() {
    return rightQuery;
  }

  public void setRightQuery(Query rightQuery) {
    this.rightQuery = rightQuery;
  }

  public void setLeftOn(List<FeatureDTO> leftOn) {
    this.leftOn = leftOn;
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

  public List<FeatureDTO> getOn() {
    return on;
  }

  public void setOn(List<FeatureDTO> on) {
    this.on = on;
  }

  public List<FeatureDTO> getLeftOn() {
    return leftOn;
  }

  public List<FeatureDTO> getRightOn() {
    return rightOn;
  }

  /**
   * Generate the condition node for the join node. At this stage, primary keys joins are treated as `on` joins.
   * @return
   */
  public SqlNode getCondition() {
    if (on != null) {
      return getOnCondition();
    } else {
      return getLeftRightCondition();
    }
  }

  /**
   * Iterate over the on list to generate the on condition node
   * @return
   */
  private SqlNode getOnCondition() {
    if (on.size() > 1) {
      SqlNodeList conditionList = new SqlNodeList(SqlParserPos.ZERO);
      for (FeatureDTO f : on) {
        conditionList.add(generateEqualityCondition(leftQuery.getAs(), rightQuery.getAs(), f, f));
      }
      return  SqlStdOperatorTable.AND.createCall(conditionList);
    } else {
      return generateEqualityCondition(leftQuery.getAs(), rightQuery.getAs(), on.get(0), on.get(0));
    }
  }

  /**
   * Iterate over the leftOn and rightOn to generate the on condition node
   * @return
   */
  private SqlNode getLeftRightCondition()  {
    if (leftOn.size() > 1) {
      SqlNodeList conditionList = new SqlNodeList(SqlParserPos.ZERO);
      for (int i = 0; i < leftOn.size(); i++) {
        conditionList.add(generateEqualityCondition(leftQuery.getAs(), rightQuery.getAs(),
            leftOn.get(i), rightOn.get(i)));
      }
      return  SqlStdOperatorTable.AND.createCall(conditionList);
    } else {
      return generateEqualityCondition(leftQuery.getAs(), rightQuery.getAs(), leftOn.get(0), rightOn.get(0));
    }
  }

  /**
   * Generate equality node between 2 single feature. The feature name will have the fully qualified domain name.
   * fg_alias.ft_name
   * @param leftFgAs
   * @param rightFgAs
   * @param leftOn
   * @param rightOn
   * @return
   */
  private SqlNode generateEqualityCondition(String leftFgAs, String rightFgAs, FeatureDTO leftOn, FeatureDTO rightOn) {
    SqlIdentifier leftHandside = new SqlIdentifier(Arrays.asList(leftFgAs, leftOn.getName()), SqlParserPos.ZERO);
    SqlIdentifier rightHandside = new SqlIdentifier(Arrays.asList(rightFgAs, rightOn.getName()), SqlParserPos.ZERO);

    SqlNodeList equalityList = new SqlNodeList(SqlParserPos.ZERO);
    equalityList.add(leftHandside);
    equalityList.add(rightHandside);

    return SqlStdOperatorTable.EQUALS.createCall(equalityList);
  }
}
