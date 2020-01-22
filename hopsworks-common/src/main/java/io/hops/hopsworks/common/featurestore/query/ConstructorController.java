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
import io.hops.hopsworks.common.featurestore.featuregroup.FeaturegroupController;
import io.hops.hopsworks.common.featurestore.featuregroup.FeaturegroupDTO;
import io.hops.hopsworks.common.featurestore.featuregroup.FeaturegroupFacade;
import io.hops.hopsworks.exceptions.FeaturestoreException;
import org.apache.calcite.sql.JoinConditionType;
import org.apache.calcite.sql.JoinType;
import org.apache.calcite.sql.SqlDialect;
import org.apache.calcite.sql.SqlIdentifier;
import org.apache.calcite.sql.SqlJoin;
import org.apache.calcite.sql.SqlLiteral;
import org.apache.calcite.sql.SqlNode;
import org.apache.calcite.sql.SqlNodeList;
import org.apache.calcite.sql.SqlSelect;
import org.apache.calcite.sql.dialect.SparkSqlDialect;
import org.apache.calcite.sql.fun.SqlStdOperatorTable;
import org.apache.calcite.sql.parser.SqlParserPos;

import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Stateless
@TransactionAttribute(TransactionAttributeType.NEVER)
public class ConstructorController {

  @EJB
  private FeaturegroupFacade featuregroupFacade;
  @EJB
  private FeaturestoreFacade featurestoreFacade;
  @EJB
  private FeaturegroupController featuregroupController;

  private final static String ALL_FEATURES = "*";

  public ConstructorController() { }

  // For testing
  protected ConstructorController(FeaturegroupController featuregroupController,
                                  FeaturestoreFacade featurestoreFacade,
                                  FeaturegroupFacade featuregroupFacade) {
    this.featuregroupController = featuregroupController;
    this.featurestoreFacade = featurestoreFacade;
    this.featuregroupFacade = featuregroupFacade;
  }

  public String construct(QueryDTO queryDTO) throws FeaturestoreException {
    Query query = convertQueryDTO(queryDTO, 0);
    // Generate SQL
    return generateSQL(query);
  }

  protected Query convertQueryDTO(QueryDTO queryDTO, int fgId) {
    Featuregroup fg = validateFeaturegroupDTO(queryDTO.getLeftFeatureGroup());
    String fgAs = generateAs(fgId++);

    String featureStore = featurestoreFacade.getHiveDbName(fg.getFeaturestore().getHiveDbId());
    List<FeatureDTO> availableFeatures = featuregroupController.getFeatures(fg);
    List<FeatureDTO> requestedFeatures = validateFeatures(fg, fgAs, queryDTO.getLeftFeatures(), availableFeatures);

    Query query = new Query(featureStore, fg, fgAs, requestedFeatures, availableFeatures);
    if (queryDTO.getJoins() != null && !queryDTO.getJoins().isEmpty()) {
      query.setJoins(convertJoins(query, queryDTO.getJoins(), fgId));
    }

    return query;
  }

  private String generateAs(int id) {
    return "fg" + id;
  }

  private Featuregroup validateFeaturegroupDTO(FeaturegroupDTO featuregroupDTO) {
    if (featuregroupDTO == null) {
      throw new IllegalArgumentException("Feature group not specified or the list of features is empty");
    } else {
      Featuregroup featuregroup = featuregroupFacade.findById(featuregroupDTO.getId());
      if (featuregroup == null) {
        throw new IllegalArgumentException("Could not find feature group with ID"
            + featuregroupDTO.getId());
      }
      return featuregroup;
    }
  }

  protected List<FeatureDTO> validateFeatures(Featuregroup fg, String as,
                                              List<FeatureDTO> requestedFeatures, List<FeatureDTO> availableFeatures) {
    List<FeatureDTO> featureList = new ArrayList<>();
    availableFeatures.forEach(f -> f.setFeaturegroup(as));

    if (requestedFeatures == null || requestedFeatures.isEmpty()) {
      throw new IllegalArgumentException("Invalid requested features");
    } else if (requestedFeatures.size() == 1 && requestedFeatures.get(0).getName().equals(ALL_FEATURES)) {
      // Users can specify * to request all the features in a specific feature group
      featureList.addAll(availableFeatures);
    } else {
      // Check that all the requested features are available in the list, based on the provided name
      for (FeatureDTO requestedFeature : requestedFeatures) {
        featureList.add(availableFeatures.stream().filter(af -> af.getName().equals(requestedFeature.getName()))
            .findFirst()
            .orElseThrow(() -> new IllegalArgumentException("Feature: " + requestedFeature.getName()
                    + " not found in feature group: " + fg.getName())));
      }
    }
    return featureList;
  }

  private List<Join> convertJoins(Query leftQuery, List<JoinDTO> joinDTOS, int fgId) {
    List<Join> joins = new ArrayList<>();
    for (JoinDTO joinDTO : joinDTOS) {
      if (joinDTO.getQuery() == null) {
        throw new IllegalArgumentException("Subquery not specified");
      }
      Query rightQuery = convertQueryDTO(joinDTO.getQuery(), fgId++);

      if (joinDTO.getOn() != null && !joinDTO.getOn().isEmpty()) {
        joins.add(extractOn(leftQuery, rightQuery, joinDTO.getOn(), joinDTO.getType()));
      } else if (joinDTO.getLeftOn() != null && !joinDTO.getLeftOn().isEmpty()) {
        joins.add(
            extractLeftRightOn(leftQuery, rightQuery, joinDTO.getLeftOn(), joinDTO.getRightOn(), joinDTO.getType()));
      } else {
        // Only if right feature group is present, extract the primary keys for the join
        joins.add(extractPrimaryKeysJoin(leftQuery, rightQuery, joinDTO.getType()));
      }
    }

    return joins;
  }

  // TODO(Fabio): investigate type compatibility
  protected Join extractOn(Query leftQuery, Query rightQuery, List<FeatureDTO> on, JoinType joinType) {
    // Make sure that the joining features are available on both feature groups and have the same type on both
    for (FeatureDTO joinFeature : on) {
      FeatureDTO leftFeature = leftQuery.getAvailableFeatures().stream()
          .filter(f -> f.getName().equals(joinFeature.getName()))
          .findFirst()
          .orElseThrow(() -> new IllegalArgumentException("Could not find Join feature: " + joinFeature.getName() +
              " in feature group: " + leftQuery.getFeaturegroup().getName()));
      // Make sure that the same feature (name and type) is available on the right side as well.
      if (rightQuery.getAvailableFeatures().stream()
          .noneMatch(f -> (f.getName().equals(joinFeature.getName()) && f.getType().equals(leftFeature.getType())))) {
        throw new IllegalArgumentException("Could not find Join feature " + joinFeature.getName() +
            " in feature group: " + rightQuery.getFeaturegroup().getName() + ", or it doesn't have the expected type");
      }
    }
    return new Join(leftQuery, rightQuery, on, joinType);
  }

  protected Join extractLeftRightOn(Query leftQuery, Query rightQuery,
                                    List<FeatureDTO> leftOn, List<FeatureDTO> rightOn, JoinType joinType) {
    // Make sure that they 2 list have the same length, and that the respective features have the same type
    if (leftOn.size() != rightOn.size()) {
      throw new IllegalArgumentException("LeftOn and RightOn have different sizes");
    }
    int i = 0;
    while (i < leftOn.size()) {
      String leftFeatureName = leftOn.get(i).getName();
      FeatureDTO leftFeature = leftQuery.getAvailableFeatures().stream()
          .filter(f -> f.getName().equals(leftFeatureName))
          .findFirst()
          .orElseThrow(() -> new IllegalArgumentException("Could not find Join feature: " + leftFeatureName +
              " in feature group: " + leftQuery.getFeaturegroup().getName()));

      String rightFeatureName = rightOn.get(i).getName();
      if (rightQuery.getAvailableFeatures().stream()
          .noneMatch(f -> (f.getName().equals(rightFeatureName) && f.getType().equals(leftFeature.getType())))) {
        throw new IllegalArgumentException("Could not find Join feature " + rightFeatureName +
            " in feature group: " + rightQuery.getFeaturegroup().getName() + ", or it doesn't have the expected type");
      }
      i++;
    }
    return new Join(leftQuery, rightQuery, leftOn, rightOn, joinType);
  }

  protected Join extractPrimaryKeysJoin(Query leftQuery, Query rightQuery, JoinType joinType) {
    // Find subset of matching primary keys (same name and type) to be used as join condition
    List<FeatureDTO> joinFeatures = new ArrayList<>();
    leftQuery.getAvailableFeatures().stream().filter(FeatureDTO::getPrimary).forEach(lf -> {
      joinFeatures.addAll(rightQuery.getAvailableFeatures().stream()
          .filter(rf -> rf.getName().equals(lf.getName()) && rf.getType().equals(lf.getType()) && rf.getPrimary())
          .collect(Collectors.toList()));
    });

    if (joinFeatures.isEmpty()) {
      throw new IllegalArgumentException("Could not find any matching feature to join: "
          + leftQuery.getFeaturegroup().getName() + " and: " + rightQuery.getFeaturegroup().getName());
    }

    return new Join(leftQuery, rightQuery, joinFeatures, joinType);
  }

  public String generateSQL(Query query) {
    SqlNodeList selectList = new SqlNodeList(SqlParserPos.ZERO);
    for (FeatureDTO f : collectFeatures(query)) {
      selectList.add(new SqlIdentifier(Arrays.asList(f.getFeaturegroup(), f.getName()), SqlParserPos.ZERO));
    }

    SqlNode joinNode = null;
    if (query.getJoins() == null || query.getJoins().isEmpty()) {
      joinNode = generateTableNode(query);
    } else {
      joinNode = buildJoinNode(query, query.getJoins().size() - 1);
    }

    SqlSelect select = new SqlSelect(SqlParserPos.ZERO, null, selectList, joinNode,
        null, null, null, null, null, null, null);
    return select.toSqlString(new SparkSqlDialect(SqlDialect.EMPTY_CONTEXT)).getSql();
  }

  private SqlNode buildJoinNode(Query query, int i) {
    if (i < 0) {
      // No more joins to read build the node for the query itself.
      return generateTableNode(query);
    } else {
      return new SqlJoin(SqlParserPos.ZERO, buildJoinNode(query, i-1),
          SqlLiteral.createBoolean(false, SqlParserPos.ZERO),
          SqlLiteral.createSymbol(query.getJoins().get(i).getJoinType(), SqlParserPos.ZERO),
          generateTableNode(query.getJoins().get(i).getRightQuery()),
          SqlLiteral.createSymbol(JoinConditionType.ON, SqlParserPos.ZERO),
          query.getJoins().get(i).getCondition());
    }

  }

  protected List<FeatureDTO> collectFeatures(Query query) {
    List<FeatureDTO> features = new ArrayList<>(query.getFeatures());
    if (query.getJoins() != null) {
      query.getJoins().forEach(join -> {
        features.addAll(collectFeatures(join.getRightQuery()));
      });
    }
    return features;
  }

  private SqlNode generateTableNode(Query query) {
    List<String> tableIdentifierStr = new ArrayList<>();
    tableIdentifierStr.add(query.getFeatureStore());
    tableIdentifierStr.add(query.getFeaturegroup().getName() + "_" + query.getFeaturegroup().getVersion());

    SqlNodeList asNodeList = new SqlNodeList(Arrays.asList(new SqlIdentifier(tableIdentifierStr, SqlParserPos.ZERO),
        new SqlIdentifier(query.getAs(), SqlParserPos.ZERO)), SqlParserPos.ZERO);

    return SqlStdOperatorTable.AS.createCall(asNodeList);
  }
}
