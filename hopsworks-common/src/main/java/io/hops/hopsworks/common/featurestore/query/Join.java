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

import io.hops.hopsworks.common.dao.featurestore.featuregroup.Featuregroup;
import io.hops.hopsworks.common.featurestore.FeaturestoreFacade;
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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class Join {
  private Featuregroup leftFeatureGroup;
  private String leftFgAs;
  private Featuregroup rightFeatureGroup;
  private String rightFgAs;

  private List<FeatureDTO> on;
  private List<FeatureDTO> leftOn;
  private List<FeatureDTO> rightOn;
  private JoinType joinType;

  // TODO(Fabio): this is not great, find a better solution
  private FeaturestoreFacade featurestoreFacade;

  public Join(FeaturestoreFacade featurestoreFacade, Featuregroup leftFeatureGroup, String leftFgAs) {
    this.featurestoreFacade = featurestoreFacade;
    this.leftFeatureGroup = leftFeatureGroup;
    this.leftFgAs = leftFgAs;
  }

  public Join(FeaturestoreFacade featurestoreFacade, Featuregroup leftFeatureGroup, String leftFgAs,
              Featuregroup rightFeatureGroup, String rightFgAs, List<FeatureDTO> on, JoinType joinType) {
    this.featurestoreFacade = featurestoreFacade;
    this.leftFeatureGroup = leftFeatureGroup;
    this.leftFgAs = leftFgAs;
    this.rightFeatureGroup = rightFeatureGroup;
    this.rightFgAs = rightFgAs;
    this.on = on;
    this.joinType = joinType;
  }

  public Join(FeaturestoreFacade featurestoreFacade, Featuregroup leftFeatureGroup, String leftFgAs,
              Featuregroup rightFeatureGroup, String rightFgAs, List<FeatureDTO> leftOn, List<FeatureDTO> rightOn,
              JoinType joinType) {
    this.featurestoreFacade = featurestoreFacade;
    this.leftFeatureGroup = leftFeatureGroup;
    this.leftFgAs = leftFgAs;
    this.rightFeatureGroup = rightFeatureGroup;
    this.rightFgAs = rightFgAs;
    this.leftOn = leftOn;
    this.rightOn = rightOn;
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

  public SqlNode getJoinNode() {
    if (rightFeatureGroup == null) {
      // Effectively no join
      return generateTableNode(leftFeatureGroup, leftFgAs);
    } else {
      SqlNode conditionNode = getCondition();
      return new SqlJoin(SqlParserPos.ZERO, generateTableNode(leftFeatureGroup, leftFgAs),
          SqlLiteral.createBoolean(false, SqlParserPos.ZERO),
          SqlLiteral.createSymbol(joinType, SqlParserPos.ZERO),
          generateTableNode(rightFeatureGroup, rightFgAs),
          SqlLiteral.createSymbol(JoinConditionType.ON, SqlParserPos.ZERO),
          conditionNode);
    }
  }

  private SqlNode generateTableNode(Featuregroup featuregroup, String as) {
    List<String> tableIdentifierStr = new ArrayList<>();
    tableIdentifierStr.add(featurestoreFacade.getHiveDbName(featuregroup.getFeaturestore().getHiveDbId()));
    tableIdentifierStr.add(featuregroup.getName() + "_" + featuregroup.getVersion());

    SqlNodeList asNodeList = new SqlNodeList(Arrays.asList(new SqlIdentifier(tableIdentifierStr, SqlParserPos.ZERO),
        new SqlIdentifier(as, SqlParserPos.ZERO)), SqlParserPos.ZERO);

    return SqlStdOperatorTable.AS.createCall(asNodeList);
  }

  private SqlNode getCondition() {
    if (on.size() > 1) {
      SqlNodeList conditionList = new SqlNodeList(SqlParserPos.ZERO);
      for (FeatureDTO f : on) {
        conditionList.add(generateEqualityCondition(leftFgAs, rightFgAs, f));
      }
      return  SqlStdOperatorTable.AND.createCall(conditionList);
    } else {
      return generateEqualityCondition(leftFgAs, rightFgAs, on.get(0));
    }
  }

  private SqlNode generateEqualityCondition(String leftFgAs, String rightFgAs, FeatureDTO on) {

    SqlNodeList leftHandside = new SqlNodeList(SqlParserPos.ZERO);
    leftHandside.add(new SqlIdentifier(leftFgAs, SqlParserPos.ZERO));
    leftHandside.add(new SqlIdentifier(on.getName(), SqlParserPos.ZERO));

    SqlNodeList rightHandside = new SqlNodeList(SqlParserPos.ZERO);
    rightHandside.add(new SqlIdentifier(rightFgAs, SqlParserPos.ZERO));
    rightHandside.add(new SqlIdentifier(on.getName(), SqlParserPos.ZERO));

    SqlNodeList equalityList = new SqlNodeList(SqlParserPos.ZERO);
    equalityList.add(leftHandside);
    equalityList.add(rightHandside);

    return SqlStdOperatorTable.EQUALS.createCall(equalityList);
  }
}
