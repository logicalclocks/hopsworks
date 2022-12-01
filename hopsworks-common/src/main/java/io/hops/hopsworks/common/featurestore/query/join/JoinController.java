/*
 * This file is part of Hopsworks
 * Copyright (C) 2021, Logical Clocks AB. All rights reserved
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

package io.hops.hopsworks.common.featurestore.query.join;

import io.hops.hopsworks.common.featurestore.query.ConstructorController;
import io.hops.hopsworks.common.featurestore.query.Feature;
import io.hops.hopsworks.common.featurestore.query.Query;
import io.hops.hopsworks.persistence.entity.featurestore.trainingdataset.SqlCondition;
import org.apache.calcite.sql.JoinConditionType;
import org.apache.calcite.sql.SqlIdentifier;
import org.apache.calcite.sql.SqlJoin;
import org.apache.calcite.sql.SqlLiteral;
import org.apache.calcite.sql.SqlNode;
import org.apache.calcite.sql.SqlNodeList;
import org.apache.calcite.sql.fun.SqlStdOperatorTable;
import org.apache.calcite.sql.parser.SqlParserPos;

import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import java.util.Arrays;
import java.util.List;

@Stateless
@TransactionAttribute(TransactionAttributeType.NEVER)
public class JoinController {
  
  @EJB
  private ConstructorController constructorController;
  
  public JoinController() { }
  
  // for testing
  public JoinController(ConstructorController constructorController) {
    this.constructorController = constructorController;
  }
  
  /**
   * Recursively generate
   * @param query
   * @param i
   * @return
   */
  public SqlNode buildJoinNode(Query query, int i, boolean online) {
    if (i < 0) {
      // No more joins to read build the node for the query itself.
      return constructorController.generateTableNode(query, online);
    } else {
      return new SqlJoin(SqlParserPos.ZERO, buildJoinNode(query, i-1, online),
        SqlLiteral.createBoolean(false, SqlParserPos.ZERO),
        SqlLiteral.createSymbol(query.getJoins().get(i).getJoinType(), SqlParserPos.ZERO),
        constructorController.generateTableNode(query.getJoins().get(i).getRightQuery(), online),
        SqlLiteral.createSymbol(JoinConditionType.ON, SqlParserPos.ZERO),
        getLeftRightCondition(query.getJoins().get(i), online));
    }
  }
  
  /**
   * Iterate over the leftOn and rightOn to generate the on condition node
   * @return
   */
  public SqlNode getLeftRightCondition(Join join, boolean online) {
    return getLeftRightCondition(join.getLeftQuery().getAs(), join.getRightQuery().getAs(), join.getLeftOn(),
      join.getRightOn(), join.getJoinOperator(), online);
  }
  
  public SqlNode getLeftRightCondition(String leftAs, String rightAs, List<Feature> leftOn, List<Feature> rightOn,
                                       List<SqlCondition> joinOperator, boolean online)  {
    if (leftOn.size() == 1) {
      return generateCondition(leftAs, rightAs, leftOn.get(0), rightOn.get(0), online, joinOperator.get(0));
    }
    
    SqlNodeList conditionList = new SqlNodeList(SqlParserPos.ZERO);
    for (int i = 0; i < leftOn.size(); i++) {
      conditionList = compactEquality(conditionList);
      conditionList.add(generateCondition(leftAs, rightAs, leftOn.get(i), rightOn.get(i), online, joinOperator.get(i)));
    }
    return  SqlStdOperatorTable.AND.createCall(conditionList);
  }
  
  // if there are already two elements in the list, then compact the list
  // using the equality operator
  private SqlNodeList compactEquality(SqlNodeList conditionList) {
    if (conditionList.size() < 2) {
      return conditionList;
    }
    
    SqlNodeList compactedList = new SqlNodeList(SqlParserPos.ZERO);
    compactedList.add(SqlStdOperatorTable.AND.createCall(conditionList));
    return compactedList;
  }
  
  private SqlNode generateCondition(String leftFgAs, String rightFgAs, Feature leftOn, Feature rightOn,
                                    boolean online, SqlCondition sqlBinaryOperator) {
    SqlNode leftHandside;
    SqlNode rightHandside;
    if (leftOn.getDefaultValue() == null) {
      leftHandside = new SqlIdentifier(Arrays.asList("`" + leftFgAs + "`", "`" + leftOn.getName() + "`"),
        SqlParserPos.ZERO);
    } else {
      leftHandside = constructorController.caseWhenDefault(leftOn);
    }
    if (rightOn.getDefaultValue() == null) {
      rightHandside = new SqlIdentifier(Arrays.asList("`" + rightFgAs + "`", "`" + rightOn.getName() + "`"),
        SqlParserPos.ZERO);
    } else {
      rightHandside = constructorController.caseWhenDefault(rightOn);
    }
    
    SqlNodeList equalityList = new SqlNodeList(SqlParserPos.ZERO);
    equalityList.add(leftHandside);
    equalityList.add(rightHandside);
    
    return sqlBinaryOperator.operator.createCall(equalityList);
  }
  
}
