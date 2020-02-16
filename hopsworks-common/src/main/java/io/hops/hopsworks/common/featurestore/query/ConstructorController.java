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

import io.hops.hopsworks.common.dao.featurestore.featuregroup.Featuregroup;
import io.hops.hopsworks.common.featurestore.FeaturestoreFacade;
import io.hops.hopsworks.common.featurestore.feature.FeatureDTO;
import io.hops.hopsworks.common.featurestore.featuregroup.FeaturegroupController;
import io.hops.hopsworks.common.featurestore.featuregroup.FeaturegroupDTO;
import io.hops.hopsworks.common.featurestore.featuregroup.FeaturegroupFacade;
import io.hops.hopsworks.exceptions.FeaturestoreException;
import io.hops.hopsworks.restutils.RESTCodes;
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
import java.util.logging.Level;
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

  /**
   * Recursively convert the QueryDTO into the internal query representation
   * @param queryDTO
   * @param fgId each feature group will be aliased as fg[Integer] where [integer] is going to be an incremental id
   * @return
   */
  protected Query convertQueryDTO(QueryDTO queryDTO, int fgId) throws FeaturestoreException {
    Featuregroup fg = validateFeaturegroupDTO(queryDTO.getLeftFeatureGroup());
    String fgAs = generateAs(fgId++);

    String featureStore = featurestoreFacade.getHiveDbName(fg.getFeaturestore().getHiveDbId());
    List<FeatureDTO> availableFeatures = featuregroupController.getFeatures(fg);
    List<FeatureDTO> requestedFeatures = validateFeatures(fg, fgAs, queryDTO.getLeftFeatures(), availableFeatures);

    Query query = new Query(featureStore, fg, fgAs, requestedFeatures, availableFeatures);
    // If there are any join, recursively conver the Join's QueryDTO into the internal Query representation
    if (queryDTO.getJoins() != null && !queryDTO.getJoins().isEmpty()) {
      query.setJoins(convertJoins(query, queryDTO.getJoins(), fgId));
    }

    return query;
  }

  private String generateAs(int id) {
    return "fg" + id;
  }

  /**
   * Validate FeatureGroupDTO to make sure it exists. Authorization is done at the storage layer by HopsFS when
   * actually executing the query.
   * @param featuregroupDTO
   * @return
   */
  private Featuregroup validateFeaturegroupDTO(FeaturegroupDTO featuregroupDTO) throws FeaturestoreException {
    if (featuregroupDTO == null) {
      throw new IllegalArgumentException("Feature group not specified");
    } else {
      Featuregroup featuregroup = featuregroupFacade.findById(featuregroupDTO.getId());
      if (featuregroup == null) {
        throw new FeaturestoreException(RESTCodes.FeaturestoreErrorCode.FEATUREGROUP_NOT_FOUND, Level.FINE,
            "Could not find feature group with ID" + featuregroupDTO.getId());
      }
      return featuregroup;
    }
  }

  /**
   * Given the list of features available in the feature group check that all the features the user
   * requests actually exists.
   *
   * Users are allowed to pass a list with a single feature named * to select all the features.
   * @param fg
   * @param as
   * @param requestedFeatures
   * @param availableFeatures
   * @return The list of feature objects that it's going to be used to generate the SQL string.
   */
  protected List<FeatureDTO> validateFeatures(Featuregroup fg, String as,
                                              List<FeatureDTO> requestedFeatures, List<FeatureDTO> availableFeatures)
      throws FeaturestoreException {
    List<FeatureDTO> featureList = new ArrayList<>();
    // This list will be used when generating the SQL string.
    // Set the feature group alias here so that it can be used later
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
            .orElseThrow(() -> new FeaturestoreException(RESTCodes.FeaturestoreErrorCode.FEATURE_NOT_EXISTING,
                Level.FINE,
                "Feature: " + requestedFeature.getName() + " not found in feature group: " + fg.getName())));
      }
    }
    return featureList;
  }

  /**
   * Convert the JoinDTOs into the internal representation of the Join object.
   * The returned list will already contain the correct set of joining keys
   * @param leftQuery
   * @param joinDTOS
   * @param fgId
   * @return
   */
  private List<Join> convertJoins(Query leftQuery, List<JoinDTO> joinDTOS, int fgId) throws FeaturestoreException {
    List<Join> joins = new ArrayList<>();
    for (JoinDTO joinDTO : joinDTOS) {
      if (joinDTO.getQuery() == null) {
        throw new IllegalArgumentException("Subquery not specified");
      }
      // Recursively convert the QueryDTO. Currently we don't support Joins of Joins
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

  /**
   * If the user has specified the `on` field in the JoinDTO check that the features in it are present on both feature
   * groups (name check) and they have the same types on both of them.
   * @param leftQuery
   * @param rightQuery
   * @param on
   * @param joinType
   * @return
   */
  // TODO(Fabio): investigate type compatibility
  protected Join extractOn(Query leftQuery, Query rightQuery, List<FeatureDTO> on, JoinType joinType)
      throws FeaturestoreException {
    for (FeatureDTO joinFeature : on) {
      FeatureDTO leftFeature = leftQuery.getAvailableFeatures().stream()
          .filter(f -> f.getName().equals(joinFeature.getName()))
          .findFirst()
          .orElseThrow(() -> new FeaturestoreException(RESTCodes.FeaturestoreErrorCode.FEATURE_NOT_EXISTING,
              Level.FINE,
              "Could not find Join feature: " + joinFeature.getName() + " in feature group: "
                  + leftQuery.getFeaturegroup().getName()));
      // Make sure that the same feature (name and type) is available on the right side as well.
      if (rightQuery.getAvailableFeatures().stream()
          .noneMatch(f -> (f.getName().equals(joinFeature.getName()) && f.getType().equals(leftFeature.getType())))) {
        throw new FeaturestoreException(RESTCodes.FeaturestoreErrorCode.FEATURE_NOT_EXISTING, Level.FINE,
            "Could not find Join feature " + joinFeature.getName() + " in feature group: "
                + rightQuery.getFeaturegroup().getName() + ", or it doesn't have the expected type");
      }
    }
    return new Join(leftQuery, rightQuery, on, joinType);
  }

  /**
   * If the user has specified the `leftOn` and `rightOn` make sure that both list have the same length, that the leftOn
   * features are present in the left feature group, the rightOn features in the right feature group. Finally make sure
   * that their type match - first feature in the leftOn should have the same type as the first one on the
   * rightOn and so on.
   * @param leftQuery
   * @param rightQuery
   * @param leftOn
   * @param rightOn
   * @param joinType
   * @return
   */
  protected Join extractLeftRightOn(Query leftQuery, Query rightQuery,
                                    List<FeatureDTO> leftOn, List<FeatureDTO> rightOn, JoinType joinType)
      throws FeaturestoreException {
    // Make sure that they 2 list have the same length, and that the respective features have the same type
    if (leftOn.size() != rightOn.size()) {
      throw new FeaturestoreException(RESTCodes.FeaturestoreErrorCode.LEFT_RIGHT_ON_DIFF_SIZES, Level.FINE);
    }
    int i = 0;
    while (i < leftOn.size()) {
      String leftFeatureName = leftOn.get(i).getName();
      // Find the left feature in the left FeatureGroup.
      FeatureDTO leftFeature = leftQuery.getAvailableFeatures().stream()
          .filter(f -> f.getName().equals(leftFeatureName))
          .findFirst()
          .orElseThrow(() -> new FeaturestoreException(RESTCodes.FeaturestoreErrorCode.FEATURE_NOT_EXISTING,
              Level.FINE,
              "Could not find Join feature: " + leftFeatureName + " in feature group: "
                  + leftQuery.getFeaturegroup().getName()));

      String rightFeatureName = rightOn.get(i).getName();
      // Make sure that the rightOn feature at the same position (i) exists and it has the same type of the left one.
      if (rightQuery.getAvailableFeatures().stream()
          .noneMatch(f -> (f.getName().equals(rightFeatureName) && f.getType().equals(leftFeature.getType())))) {
        throw new FeaturestoreException(RESTCodes.FeaturestoreErrorCode.FEATURE_NOT_EXISTING, Level.FINE,
            "Could not find Join feature " + rightFeatureName + " in feature group: "
                + rightQuery.getFeaturegroup().getName() + ", or it doesn't have the expected type");
      }
      i++;
    }
    return new Join(leftQuery, rightQuery, leftOn, rightOn, joinType);
  }

  /**
   * In case the user has not specified any joining key, the largest subset of matching primary key will be used
   * for the join. Both name and type should match for a feature to be added to the subset.
   * @param leftQuery
   * @param rightQuery
   * @param joinType
   * @return
   */
  protected Join extractPrimaryKeysJoin(Query leftQuery, Query rightQuery, JoinType joinType)
      throws FeaturestoreException {
    // Find subset of matching primary keys (same name and type) to be used as join condition
    List<FeatureDTO> joinFeatures = new ArrayList<>();
    leftQuery.getAvailableFeatures().stream().filter(FeatureDTO::getPrimary).forEach(lf -> {
      joinFeatures.addAll(rightQuery.getAvailableFeatures().stream()
          .filter(rf -> rf.getName().equals(lf.getName()) && rf.getType().equals(lf.getType()) && rf.getPrimary())
          .collect(Collectors.toList()));
    });

    if (joinFeatures.isEmpty()) {
      throw new FeaturestoreException(RESTCodes.FeaturestoreErrorCode.NO_PK_JOINING_KEYS, Level.FINE,
          leftQuery.getFeaturegroup().getName() + " and: " + rightQuery.getFeaturegroup().getName());
    }

    return new Join(leftQuery, rightQuery, joinFeatures, joinType);
  }

  /**
   * For Join on primary keys or On condition we should remove duplicated (same name) columns.
   * Spark refuses to write dataframes with duplicated column names.
   * @param query
   */
  public void removeDuplicateColumns(Query query) {
    for (Join join : query.getJoins()) {
      if (join.getRightOn() != null && !join.getRightOn().isEmpty()) {
        // No need to process leftOn/rightOn type of query. Those are expected to have different column names
        // on each side.
        continue;
      }

      List<FeatureDTO> rightFeatureList = join.getRightQuery().getFeatures();

      // Extract join feature names
      List<String> joinFeatureNames = join.getOn().stream().map(FeatureDTO::getName).collect(Collectors.toList());

      // Remove all features which are on the join condition. This means that they are also on the other side of the
      // query
      List<FeatureDTO> filteredRightFeatures =
          rightFeatureList.stream().filter(f -> !joinFeatureNames.contains(f.getName())).collect(Collectors.toList());

      // replace the features for the right query
      join.getRightQuery().setFeatures(filteredRightFeatures);
    }
  }

  /**
   * Generate the SQL string. The backend will return a string to the client which is the SQL query to execute.
   * @param query
   * @return
   */
  public String generateSQL(Query query) {
    // remove duplicated join columns
    if (query.getJoins() != null) {
      removeDuplicateColumns(query);
    }

    SqlNodeList selectList = new SqlNodeList(SqlParserPos.ZERO);
    for (FeatureDTO f : collectFeatures(query)) {
      // Build the select part. List of features selected by the user. Each feature will be fg_alias.fg_name
      selectList.add(new SqlIdentifier(Arrays.asList(f.getFeaturegroup(), f.getName()), SqlParserPos.ZERO));
    }

    SqlNode joinNode = null;
    if (query.getJoins() == null || query.getJoins().isEmpty()) {
      // If there are no joins just set `from featuregroup`
      joinNode = generateTableNode(query);
    } else {
      // If there are joins generate the join list with the respective conditions
      joinNode = buildJoinNode(query, query.getJoins().size() - 1);
    }

    // Assemble the query
    SqlSelect select = new SqlSelect(SqlParserPos.ZERO, null, selectList, joinNode,
        null, null, null, null, null, null, null);
    return select.toSqlString(new SparkSqlDialect(SqlDialect.EMPTY_CONTEXT)).getSql();
  }

  /**
   * Recursively generate
   * @param query
   * @param i
   * @return
   */
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
      query.getJoins().forEach(join -> features.addAll(collectFeatures(join.getRightQuery())));
    }
    return features;
  }

  /**
   * Generate the table node. The object will contain the fully qualified name of a feature group:
   * featurestore_name.feature_group_name_feature_group_version [as] feature_group alias
   * @param query
   * @return
   */
  private SqlNode generateTableNode(Query query) {
    List<String> tableIdentifierStr = new ArrayList<>();
    tableIdentifierStr.add(query.getFeatureStore());
    tableIdentifierStr.add(query.getFeaturegroup().getName() + "_" + query.getFeaturegroup().getVersion());

    SqlNodeList asNodeList = new SqlNodeList(Arrays.asList(new SqlIdentifier(tableIdentifierStr, SqlParserPos.ZERO),
        new SqlIdentifier(query.getAs(), SqlParserPos.ZERO)), SqlParserPos.ZERO);

    return SqlStdOperatorTable.AS.createCall(asNodeList);
  }
}
