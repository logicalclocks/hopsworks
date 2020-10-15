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

import io.hops.hopsworks.common.featurestore.FeaturestoreFacade;
import io.hops.hopsworks.common.featurestore.feature.FeatureGroupFeatureDTO;
import io.hops.hopsworks.common.featurestore.featuregroup.FeaturegroupController;
import io.hops.hopsworks.common.featurestore.featuregroup.FeaturegroupDTO;
import io.hops.hopsworks.common.featurestore.featuregroup.FeaturegroupFacade;
import io.hops.hopsworks.common.featurestore.online.OnlineFeaturestoreController;
import io.hops.hopsworks.exceptions.FeaturestoreException;
import io.hops.hopsworks.persistence.entity.featurestore.featuregroup.Featuregroup;
import io.hops.hopsworks.persistence.entity.project.Project;
import io.hops.hopsworks.persistence.entity.user.Users;
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
import org.apache.calcite.sql.fun.SqlCase;
import org.apache.calcite.sql.fun.SqlStdOperatorTable;
import org.apache.calcite.sql.parser.SqlParserPos;

import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
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
  @EJB
  private OnlineFeaturestoreController onlineFeaturestoreController;

  private final static String ALL_FEATURES = "*";

  public ConstructorController() { }

  // For testing
  protected ConstructorController(FeaturegroupController featuregroupController,
                                  FeaturestoreFacade featurestoreFacade,
                                  FeaturegroupFacade featuregroupFacade,
                                  OnlineFeaturestoreController onlineFeaturestoreController) {
    this.featuregroupController = featuregroupController;
    this.featurestoreFacade = featurestoreFacade;
    this.featuregroupFacade = featuregroupFacade;
    this.onlineFeaturestoreController = onlineFeaturestoreController;
  }

  public FsQueryDTO construct(QueryDTO queryDTO, Project project, Users user)
    throws FeaturestoreException {
    Query query = convertQueryDTO(queryDTO, 0, project, user);
    // Generate SQL
    return construct(query);
  }

  public FsQueryDTO construct(Query query) {
    FsQueryDTO fsQueryDTO = new FsQueryDTO();
    fsQueryDTO.setQuery(generateSQL(query, false));
    fsQueryDTO.setQueryOnline(generateSQL(query,true));

    return fsQueryDTO;
  }

  /**
   * Recursively convert the QueryDTO into the internal query representation
   * @param queryDTO
   * @param fgId each feature group will be aliased as fg[Integer] where [integer] is going to be an incremental id
   * @return
   */
  public Query convertQueryDTO(QueryDTO queryDTO, int fgId, Project project, Users user)
    throws FeaturestoreException {
    Featuregroup fg = validateFeaturegroupDTO(queryDTO.getLeftFeatureGroup());
    String fgAs = generateAs(fgId++);

    String featureStore = featurestoreFacade.getHiveDbName(fg.getFeaturestore().getHiveDbId());
    // used to build the online query - needs to respect the online db format name
    String projectName = onlineFeaturestoreController.getOnlineFeaturestoreDbName(fg.getFeaturestore().getProject());

    List<Feature> availableFeatures = featuregroupController.getFeatures(fg, project, user).stream()
        // Set the type as well, as the same code is used when parsing the query to generate a training dataset
        // in that case we would like to show the type of the feature in the UI.
        // it's easier and faster to return the training dataset schema if we store the type in the
        // training dataset features table.
        .map(f -> new Feature(f.getName(), fg.getName(), fgAs, f.getType(), f.getPrimary(), f.getDefaultValue()))
        .collect(Collectors.toList());

    List<Feature> requestedFeatures = validateFeatures(fg, queryDTO.getLeftFeatures(), availableFeatures);

    Query query = new Query(featureStore, projectName, fg, fgAs, requestedFeatures, availableFeatures);
    // If there are any joins, recursively convert the Join's QueryDTO into the internal Query representation
    if (queryDTO.getJoins() != null && !queryDTO.getJoins().isEmpty()) {
      query.setJoins(convertJoins(query, queryDTO.getJoins(), fgId, project, user));
      // remove duplicated join columns
      removeDuplicateColumns(query);
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
      return featuregroupFacade.findById(featuregroupDTO.getId())
          .orElseThrow(() -> new FeaturestoreException(RESTCodes.FeaturestoreErrorCode.FEATUREGROUP_NOT_FOUND,
              Level.FINE, "Could not find feature group with ID" + featuregroupDTO.getId()));
    }
  }

  /**
   * Given the list of features available in the feature group check that all the features the user
   * requests actually exists.
   *
   * Users are allowed to pass a list with a single feature named * to select all the features.
   * @param fg
   * @param requestedFeatures
   * @param availableFeatures
   * @return The list of feature objects that it's going to be used to generate the SQL string.
   */
  protected List<Feature> validateFeatures(Featuregroup fg, List<FeatureGroupFeatureDTO> requestedFeatures,
                                                 List<Feature> availableFeatures)
      throws FeaturestoreException {
    List<Feature> featureList = new ArrayList<>();

    if (requestedFeatures == null || requestedFeatures.isEmpty()) {
      throw new IllegalArgumentException("Invalid requested features");
    } else if (requestedFeatures.size() == 1 && requestedFeatures.get(0).getName().equals(ALL_FEATURES)) {
      // Users can specify * to request all the features in a specific feature group
      featureList.addAll(availableFeatures);
    } else {
      // Check that all the requested features are available in the list, based on the provided name
      for (FeatureGroupFeatureDTO requestedFeature : requestedFeatures) {
        featureList.add(availableFeatures.stream().filter(af -> af.getName().equals(requestedFeature.getName()))
            .findFirst()
            .orElseThrow(() -> new FeaturestoreException(RESTCodes.FeaturestoreErrorCode.FEATURE_DOES_NOT_EXIST,
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
  private List<Join> convertJoins(Query leftQuery, List<JoinDTO> joinDTOS, int fgId, Project project, Users user)
    throws FeaturestoreException {
    List<Join> joins = new ArrayList<>();
    for (JoinDTO joinDTO : joinDTOS) {
      if (joinDTO.getQuery() == null) {
        throw new IllegalArgumentException("Subquery not specified");
      }
      // Recursively convert the QueryDTO. Currently we don't support Joins of Joins
      Query rightQuery = convertQueryDTO(joinDTO.getQuery(), fgId++, project, user);

      if (joinDTO.getOn() != null && !joinDTO.getOn().isEmpty()) {
        List<Feature> leftOn = joinDTO.getOn().stream().map(f -> new Feature(f.getName())).collect(Collectors.toList());
        List<Feature> rightOn =
          joinDTO.getOn().stream().map(f -> new Feature(f.getName())).collect(Collectors.toList());

        joins.add(extractLeftRightOn(leftQuery, rightQuery, leftOn, rightOn, joinDTO.getType()));
      } else if (joinDTO.getLeftOn() != null && !joinDTO.getLeftOn().isEmpty()) {
        List<Feature> leftOn = joinDTO.getLeftOn().stream()
            .map(f -> new Feature(f.getName())).collect(Collectors.toList());
        List<Feature> rightOn = joinDTO.getRightOn().stream()
            .map(f -> new Feature(f.getName())).collect(Collectors.toList());

        joins.add(extractLeftRightOn(leftQuery, rightQuery, leftOn, rightOn, joinDTO.getType()));
      } else {
        // Only if right feature group is present, extract the primary keys for the join
        joins.add(extractPrimaryKeysJoin(leftQuery, rightQuery, joinDTO.getType()));
      }
    }

    return joins;
  }

  /**
   * If the user has specified the `leftOn` and `rightOn` make sure that both list have the same length, that the leftOn
   * features are present in the left feature group, the rightOn features in the right feature group.
   *
   * @param leftQuery
   * @param rightQuery
   * @param leftOn
   * @param rightOn
   * @param joinType
   * @return
   */
  public Join extractLeftRightOn(Query leftQuery, Query rightQuery, List<Feature> leftOn, List<Feature> rightOn,
                                    JoinType joinType)
      throws FeaturestoreException {
    // Make sure that they 2 list have the same length
    if (leftOn.size() != rightOn.size()) {
      throw new FeaturestoreException(RESTCodes.FeaturestoreErrorCode.LEFT_RIGHT_ON_DIFF_SIZES, Level.FINE);
    }

    // Check that all the left features exist in the left query
    for (Feature feature : leftOn) {
      checkFeatureExistsAndSetAttributes(leftQuery, feature);
    }

    // Check that all the right features exist in the right query
    for (Feature feature : rightOn) {
      checkFeatureExistsAndSetAttributes(rightQuery, feature);
    }

    return new Join(leftQuery, rightQuery, leftOn, rightOn, joinType);
  }

  /**
   * In case the user has not specified any joining key, the largest subset of matching primary key will be used
   * for the join. The name should match for the feature to be added in the subset
   * @param leftQuery
   * @param rightQuery
   * @param joinType
   * @return
   */
  protected Join extractPrimaryKeysJoin(Query leftQuery, Query rightQuery, JoinType joinType)
      throws FeaturestoreException {
    // Find subset of matching primary keys (same name) to be used as join condition
    List<Feature> joinFeatures = new ArrayList<>();
    leftQuery.getAvailableFeatures().stream().filter(Feature::isPrimary).forEach(lf -> {
      joinFeatures.addAll(rightQuery.getAvailableFeatures().stream()
          .filter(rf -> rf.getName().equals(lf.getName()) && rf.isPrimary())
          .collect(Collectors.toList()));
    });

    if (joinFeatures.isEmpty()) {
      throw new FeaturestoreException(RESTCodes.FeaturestoreErrorCode.NO_PK_JOINING_KEYS, Level.FINE,
          leftQuery.getFeaturegroup().getName() + " and: " + rightQuery.getFeaturegroup().getName());
    }

    return new Join(leftQuery, rightQuery, joinFeatures, joinType);
  }

  private void checkFeatureExistsAndSetAttributes(Query query, Feature feature) throws FeaturestoreException {
    Optional<Feature> availableFeature =
      query.getAvailableFeatures().stream().filter(f -> (f.getName().equals(feature.getName()))).findAny();
    if (!availableFeature.isPresent()) {
      throw new FeaturestoreException(RESTCodes.FeaturestoreErrorCode.FEATURE_DOES_NOT_EXIST, Level.FINE,
          "Could not find Join feature " + feature.getName() + " in feature group: "
              + query.getFeaturegroup().getName());
    } else {
      feature.setDefaultValue(availableFeature.get().getDefaultValue());
      feature.setType(availableFeature.get().getType());
      feature.setFgAlias(availableFeature.get().getFgAlias());
    }
  }

  /**
   * For Join on primary keys or On condition we should remove duplicated (same name) columns.
   * Spark refuses to write dataframes with duplicated column names.
   * @param query
   */
  private void removeDuplicateColumns(Query query) {
    for (Join join : query.getJoins()) {
      if (join.getRightOn() != null && !join.getRightOn().isEmpty()) {
        // No need to process leftOn/rightOn type of query. Those are expected to have different column names
        // on each side.
        continue;
      }

      List<Feature> rightFeatureList = join.getRightQuery().getFeatures();

      // Extract join feature names
      List<String> joinFeatureNames = join.getOn().stream().map(Feature::getName).collect(Collectors.toList());

      // Remove all features which are on the join condition. This means that they are also on the other side of the
      // query
      List<Feature> filteredRightFeatures =
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
  public String generateSQL(Query query, boolean online) {

    SqlNodeList selectList = new SqlNodeList(SqlParserPos.ZERO);
    for (Feature f : collectFeatures(query)) {
      // Build the select part. List of features selected by the user. Each feature will be fg_alias.fg_name
      // we should use the ` to avoid syntax errors on reserved keywords used as feature names (e.g. date)
      if (f.getDefaultValue() == null || online) {
        selectList.add(new SqlIdentifier(Arrays.asList("`" + f.getFgAlias() + "`", "`" + f.getName() + "`"),
          SqlParserPos.ZERO));
      } else {
        selectList.add(selectWithDefaultAs(f));
      }
    }

    SqlNode joinNode = null;
    if (query.getJoins() == null || query.getJoins().isEmpty()) {
      // If there are no joins just set `from featuregroup`
      joinNode = generateTableNode(query, online);
    } else {
      // If there are joins jgenerate the join list with the respective conditions
      joinNode = buildJoinNode(query, query.getJoins().size() - 1, online);
    }

    // Assemble the query
    SqlSelect select = new SqlSelect(SqlParserPos.ZERO, null, selectList, joinNode,
        null, null, null, null, null, null, null);
    return select.toSqlString(new SparkSqlDialect(SqlDialect.EMPTY_CONTEXT)).getSql();
  }

  public SqlNode caseWhenDefault(Feature feature) {
    SqlIdentifier featureIdentifier = new SqlIdentifier(Arrays.asList("`" + feature.getFgAlias() + "`",
      "`" + feature.getName() + "`"), SqlParserPos.ZERO);

    SqlNode featureIsNull = SqlStdOperatorTable.IS_NULL.createCall(
      SqlParserPos.ZERO, featureIdentifier);

    // most type can be implicitly converted from string, in question are complex types
    // https://cwiki.apache.org/confluence/display/Hive/LanguageManual+Types#LanguageManualTypes
    // -AllowedImplicitConversions
    SqlNode defaultValue;
    if (feature.getType().equalsIgnoreCase("string")) {
      defaultValue = SqlLiteral.createCharString(feature.getDefaultValue(), SqlParserPos.ZERO);
    } else {
      defaultValue = new SqlIdentifier(feature.getDefaultValue(), SqlParserPos.ZERO);
    }
    
    return new SqlCase(SqlParserPos.ZERO, null,
      // when
      new SqlNodeList(Arrays.asList(featureIsNull), SqlParserPos.ZERO),
      // then
      new SqlNodeList(Arrays.asList(defaultValue), SqlParserPos.ZERO),
      // else
      featureIdentifier);
  }

  public SqlNode selectWithDefaultAs(Feature feature) {
    return SqlStdOperatorTable.AS.createCall(new SqlNodeList(Arrays.asList(caseWhenDefault(feature),
      new SqlIdentifier("`" + feature.getName() + "`", SqlParserPos.ZERO)), SqlParserPos.ZERO));
  }

  /**
   * Recursively generate
   * @param query
   * @param i
   * @return
   */
  private SqlNode buildJoinNode(Query query, int i, boolean online) {
    if (i < 0) {
      // No more joins to read build the node for the query itself.
      return generateTableNode(query, online);
    } else {
      return new SqlJoin(SqlParserPos.ZERO, buildJoinNode(query, i-1, online),
          SqlLiteral.createBoolean(false, SqlParserPos.ZERO),
          SqlLiteral.createSymbol(query.getJoins().get(i).getJoinType(), SqlParserPos.ZERO),
          generateTableNode(query.getJoins().get(i).getRightQuery(), online),
          SqlLiteral.createSymbol(JoinConditionType.ON, SqlParserPos.ZERO),
          getCondition(query.getJoins().get(i), online));
    }

  }

  protected List<Feature> collectFeatures(Query query) {
    List<Feature> features = new ArrayList<>(query.getFeatures());
    if (query.getJoins() != null) {
      for (Join join : query.getJoins()) {
        if (join.getRightQuery() != null && join.getRightQuery().getFeatures() != null) {
          features.addAll(collectFeatures(join.getRightQuery()));
        }
      }
    }
    return features;
  }

  /**
   * Generate the table node. The object will contain the fully qualified name of a feature group:
   * featurestore_name.feature_group_name_feature_group_version [as] feature_group alias
   * @param query
   * @return
   */
  private SqlNode generateTableNode(Query query, boolean online) {
    List<String> tableIdentifierStr = new ArrayList<>();
    if (online) {
      tableIdentifierStr.add("`" + query.getProject() + "`");
    } else {
      tableIdentifierStr.add("`" + query.getFeatureStore() + "`");
    }

    tableIdentifierStr.add("`" + query.getFeaturegroup().getName() + "_" + query.getFeaturegroup().getVersion() + "`");

    SqlNodeList asNodeList = new SqlNodeList(Arrays.asList(new SqlIdentifier(tableIdentifierStr, SqlParserPos.ZERO),
        new SqlIdentifier("`" + query.getAs() + "`", SqlParserPos.ZERO)), SqlParserPos.ZERO);

    return SqlStdOperatorTable.AS.createCall(asNodeList);
  }
  
  
  /**
   * Generate the condition node for the join node. At this stage, primary keys joins are treated as `on` joins.
   * @return
   */
  public SqlNode getCondition(Join join, boolean online) {
    if (join.getOn() != null) {
      return getOnCondition(join, online);
    } else {
      return getLeftRightCondition(join, online);
    }
  }
  
  /**
   * Iterate over the on list to generate the on condition node
   * @return
   */
  private SqlNode getOnCondition(Join join, boolean online) {
    if (join.getOn().size() > 1) {
      SqlNodeList conditionList = new SqlNodeList(SqlParserPos.ZERO);
      for (Feature f : join.getOn()) {
        conditionList.add(generateEqualityCondition(join.getLeftQuery().getAs(), join.getRightQuery().getAs(), f, f,
          online));
      }
      return  SqlStdOperatorTable.AND.createCall(conditionList);
    } else {
      return generateEqualityCondition(join.getLeftQuery().getAs(), join.getRightQuery().getAs(), join.getOn().get(0),
        join.getOn().get(0), online);
    }
  }
  
  /**
   * Iterate over the leftOn and rightOn to generate the on condition node
   * @return
   */
  private SqlNode getLeftRightCondition(Join join, boolean online)  {
    if (join.getLeftOn().size() > 1) {
      SqlNodeList conditionList = new SqlNodeList(SqlParserPos.ZERO);
      for (int i = 0; i < join.getLeftOn().size(); i++) {
        conditionList.add(generateEqualityCondition(join.getLeftQuery().getAs(), join.getRightQuery().getAs(),
          join.getLeftOn().get(i), join.getRightOn().get(i), online));
      }
      return  SqlStdOperatorTable.AND.createCall(conditionList);
    } else {
      return generateEqualityCondition(join.getLeftQuery().getAs(), join.getRightQuery().getAs(),
        join.getLeftOn().get(0), join.getRightOn().get(0), online);
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
  private SqlNode generateEqualityCondition(String leftFgAs, String rightFgAs, Feature leftOn, Feature rightOn,
                                            boolean online ) {
    SqlNode leftHandside;
    SqlNode rightHandside;
    if (leftOn.getDefaultValue() == null || online) {
      leftHandside = new SqlIdentifier(Arrays.asList("`" + leftFgAs + "`", "`" + leftOn.getName() + "`"),
        SqlParserPos.ZERO);
    } else {
      leftHandside = caseWhenDefault(leftOn);
    }
    if (rightOn.getDefaultValue() == null || online) {
      rightHandside = new SqlIdentifier(Arrays.asList("`" + rightFgAs + "`", "`" + rightOn.getName() + "`"),
        SqlParserPos.ZERO);
    } else {
      rightHandside = caseWhenDefault(rightOn);
    }
    
    SqlNodeList equalityList = new SqlNodeList(SqlParserPos.ZERO);
    equalityList.add(leftHandside);
    equalityList.add(rightHandside);
    
    return SqlStdOperatorTable.EQUALS.createCall(equalityList);
  }
}
