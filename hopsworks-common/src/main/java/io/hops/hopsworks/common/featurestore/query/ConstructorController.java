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

import com.google.common.base.Strings;
import io.hops.hopsworks.common.featurestore.FeaturestoreFacade;
import io.hops.hopsworks.common.featurestore.feature.FeatureGroupFeatureDTO;
import io.hops.hopsworks.common.featurestore.featuregroup.FeaturegroupController;
import io.hops.hopsworks.common.featurestore.featuregroup.FeaturegroupDTO;
import io.hops.hopsworks.common.featurestore.featuregroup.FeaturegroupFacade;
import io.hops.hopsworks.common.featurestore.featuregroup.cached.CachedFeaturegroupController;
import io.hops.hopsworks.common.featurestore.featuregroup.cached.CachedFeaturegroupDTO;
import io.hops.hopsworks.common.featurestore.featuregroup.cached.FeatureGroupCommitController;
import io.hops.hopsworks.common.featurestore.featuregroup.ondemand.OnDemandFeaturegroupDTO;
import io.hops.hopsworks.common.featurestore.online.OnlineFeaturestoreController;
import io.hops.hopsworks.common.featurestore.query.filter.FilterController;
import io.hops.hopsworks.common.featurestore.query.join.Join;
import io.hops.hopsworks.common.featurestore.query.join.JoinController;
import io.hops.hopsworks.common.featurestore.query.join.JoinDTO;
import io.hops.hopsworks.common.featurestore.query.pit.PitJoinController;
import io.hops.hopsworks.common.featurestore.storageconnectors.FeaturestoreStorageConnectorController;
import io.hops.hopsworks.common.featurestore.storageconnectors.FeaturestoreStorageConnectorDTO;
import io.hops.hopsworks.common.featurestore.utils.FeaturestoreUtils;
import io.hops.hopsworks.exceptions.FeaturestoreException;
import io.hops.hopsworks.exceptions.ServiceException;
import io.hops.hopsworks.persistence.entity.featurestore.featuregroup.Featuregroup;
import io.hops.hopsworks.persistence.entity.featurestore.featuregroup.FeaturegroupType;
import io.hops.hopsworks.persistence.entity.featurestore.featuregroup.cached.FeatureGroupCommit;
import io.hops.hopsworks.persistence.entity.featurestore.featuregroup.cached.TimeTravelFormat;
import io.hops.hopsworks.persistence.entity.project.Project;
import io.hops.hopsworks.persistence.entity.user.Users;
import io.hops.hopsworks.restutils.RESTCodes;
import org.apache.calcite.sql.JoinType;
import org.apache.calcite.sql.SqlDialect;
import org.apache.calcite.sql.SqlIdentifier;
import org.apache.calcite.sql.SqlLiteral;
import org.apache.calcite.sql.SqlNode;
import org.apache.calcite.sql.SqlNodeList;
import org.apache.calcite.sql.SqlSelect;
import org.apache.calcite.sql.dialect.HiveSqlDialect;
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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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
  @EJB
  private FeatureGroupCommitController featureGroupCommitCommitController;
  @EJB
  private CachedFeaturegroupController cachedFeaturegroupController;
  @EJB
  private FeaturestoreUtils featurestoreUtils;
  @EJB
  private FeaturestoreStorageConnectorController storageConnectorController;
  @EJB
  private FilterController filterController;
  @EJB
  private JoinController joinController;
  @EJB
  private PitJoinController pitJoinController;

  private final static String ALL_FEATURES = "*";

  public ConstructorController() { }

  // For testing
  public ConstructorController(FeaturegroupController featuregroupController,
                               FeaturestoreFacade featurestoreFacade,
                               FeaturegroupFacade featuregroupFacade,
                               OnlineFeaturestoreController onlineFeaturestoreController,
                               CachedFeaturegroupController cachedFeaturegroupController,
                               FilterController filterController,
                               JoinController joinController) {
    this.featuregroupController = featuregroupController;
    this.featurestoreFacade = featurestoreFacade;
    this.featuregroupFacade = featuregroupFacade;
    this.onlineFeaturestoreController = onlineFeaturestoreController;
    this.cachedFeaturegroupController = cachedFeaturegroupController;
    this.filterController = filterController;
    this.joinController = joinController;
  }

  public FsQueryDTO construct(QueryDTO queryDTO, Project project, Users user)
    throws FeaturestoreException, ServiceException {
    boolean pitEnabled = pitJoinController.isPitEnabled(queryDTO);

    // construct lookup tables once for all involved feature groups
    // all maps have the feature group id as key
    Map<Integer, String> fgAliasLookup = new HashMap<>();
    Map<Integer, Featuregroup> fgLookup = new HashMap<>();
    Map<Integer, List<Feature>> availableFeatureLookup = new HashMap<>();
    
    populateFgLookupTables(queryDTO, 0, fgAliasLookup, fgLookup, availableFeatureLookup, project, user, null);
    
    Query query = convertQueryDTO(queryDTO, fgAliasLookup, fgLookup, availableFeatureLookup, pitEnabled);

    return construct(query, pitEnabled, false, project, user);
  }

  public FsQueryDTO construct(Query query, boolean pitEnabled, boolean isTrainingDataset, Project project, Users user)
      throws FeaturestoreException, ServiceException {
    FsQueryDTO fsQueryDTO = new FsQueryDTO();
    fsQueryDTO.setQuery(
      generateSQL(query, false).toSqlString(new SparkSqlDialect(SqlDialect.EMPTY_CONTEXT)).getSql());
    fsQueryDTO.setHudiCachedFeatureGroups(getHudiAliases(query, new ArrayList<>(), project, user));
    fsQueryDTO.setOnDemandFeatureGroups(getOnDemandAliases(user, project, query, new ArrayList<>()));

    // if on-demand feature groups are involved in the query, we don't support online queries
    if (fsQueryDTO.getOnDemandFeatureGroups().isEmpty()) {
      fsQueryDTO.setQueryOnline(
        generateSQL(query, true).toSqlString(new SparkSqlDialect(SqlDialect.EMPTY_CONTEXT)).getSql());
    }

    if (pitEnabled) {
      SqlNode pitQuery = pitJoinController.generateSQL(query, isTrainingDataset);
      String pitString;
      if (query.getHiveEngine()) {
        pitString = pitQuery.toSqlString(new SparkSqlDialect(SqlDialect.EMPTY_CONTEXT)).getSql();
  
      } else {
        pitString = pitQuery.toSqlString(new HiveSqlDialect(SqlDialect.EMPTY_CONTEXT)).getSql();
      }
      fsQueryDTO.setPitQuery(pitString);
    }

    return fsQueryDTO;
  }

  /**
   * Recursively convert the QueryDTO into the internal query representation
   * @param queryDTO
   * @return
   */
  public Query convertQueryDTO(QueryDTO queryDTO, Map<Integer, String> fgAliasLookup,
                               Map<Integer, Featuregroup> fgLookup, Map<Integer, List<Feature>> availableFeatureLookup,
                               boolean pitEnabled)
      throws FeaturestoreException {
    Integer fgId = queryDTO.getLeftFeatureGroup().getId();
    Featuregroup fg = fgLookup.get(fgId);

    String featureStore = featurestoreFacade.getHiveDbName(fg.getFeaturestore().getHiveDbId());
    // used to build the online query - needs to respect the online db format name
    String projectName = onlineFeaturestoreController.getOnlineFeaturestoreDbName(fg.getFeaturestore().getProject());

    List<Feature> requestedFeatures = validateFeatures(fg, queryDTO.getLeftFeatures(),
      availableFeatureLookup.get(fgId));

    Query query = new Query(featureStore, projectName, fg, fgAliasLookup.get(fgId), requestedFeatures,
      availableFeatureLookup.get(fgId), queryDTO.getHiveEngine());

    if (fg.getCachedFeaturegroup() != null &&
        fg.getCachedFeaturegroup().getTimeTravelFormat() == TimeTravelFormat.HUDI){
      // if hudi and end hive engine, only possible to get latest snapshot else raise exception
      if (queryDTO.getHiveEngine() && (queryDTO.getLeftFeatureGroupEndTime() != null
          || queryDTO.getJoins().stream().anyMatch(join -> join.getQuery().getLeftFeatureGroupEndTime() != null))) {
        throw new IllegalArgumentException("Hive engine on Python environments does not support incremental or " +
          "snapshot queries. Read feature group without timestamp to retrieve latest snapshot or switch to " +
          "environment with Spark Engine.");
      }
      
      // If the feature group is hudi, validate and configure start and end commit id/timestamp
      FeatureGroupCommit endCommit =
          featureGroupCommitCommitController.findCommitByDate(fg, queryDTO.getLeftFeatureGroupEndTime());
      query.setLeftFeatureGroupEndTimestamp(endCommit.getCommittedOn());
      query.setLeftFeatureGroupEndCommitId(endCommit.getFeatureGroupCommitPK().getCommitId());

      if ((queryDTO.getJoins() == null || queryDTO.getJoins().isEmpty())
          && queryDTO.getLeftFeatureGroupStartTime() != null){
        Long exactStartCommitTimestamp = featureGroupCommitCommitController.findCommitByDate(
            query.getFeaturegroup(), queryDTO.getLeftFeatureGroupStartTime()).getCommittedOn();
        query.setLeftFeatureGroupStartTimestamp(exactStartCommitTimestamp);
      } else if (queryDTO.getJoins() != null && queryDTO.getLeftFeatureGroupStartTime() != null) {
        throw new IllegalArgumentException("For incremental queries start time must be provided and "
            + "join statements are not allowed");
      }
    }

    // If there are any joins, recursively convert the Join's QueryDTO into the internal Query representation
    if (queryDTO.getJoins() != null && !queryDTO.getJoins().isEmpty()) {
      query.setJoins(
              convertJoins(query, queryDTO.getJoins(), fgAliasLookup, fgLookup, availableFeatureLookup, pitEnabled));
      // remove duplicated join columns
      removeDuplicateColumns(query, pitEnabled);
    }
    
    // If there are any filters, recursively convert the
    if (queryDTO.getFilter() != null) {
      query.setFilter(filterController.convertFilterLogic(queryDTO.getFilter(), fgLookup, availableFeatureLookup));
    }

    return query;
  }
  
  public int populateFgLookupTables(QueryDTO queryDTO, int fgId, Map<Integer, String> fgAliasLookup,
                                    Map<Integer, Featuregroup> fgLookup,
                                    Map<Integer, List<Feature>> availableFeatureLookup, Project project,
                                    Users user, String prefix)
      throws FeaturestoreException {
    // go into depth first
    if (queryDTO.getJoins() != null && !queryDTO.getJoins().isEmpty()) {
      for (JoinDTO join : queryDTO.getJoins()) {
        fgId = populateFgLookupTables(join.getQuery(), fgId, fgAliasLookup, fgLookup, availableFeatureLookup, project,
          user, join.getPrefix());
        fgId++;
      }
    }

    Featuregroup fg = validateFeaturegroupDTO(queryDTO.getLeftFeatureGroup());
    fgLookup.put(fg.getId(), fg);
    fgAliasLookup.put(fg.getId(), generateAs(fgId));
  
    List<Feature> availableFeatures = featuregroupController.getFeatures(fg, project, user).stream()
      // Set the type as well, as the same code is used when parsing the query to generate a training dataset
      // in that case we would like to show the type of the feature in the UI.
      // it's easier and faster to return the training dataset schema if we store the type in the
      // training dataset features table.
      .map(f -> new Feature(
        f.getName(), fgAliasLookup.get(fg.getId()), f.getType(), f.getPrimary(), f.getDefaultValue(), prefix))
      .collect(Collectors.toList());
    availableFeatureLookup.put(fg.getId(), availableFeatures);

    return fgId;
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
   * @return
   */
  private List<Join> convertJoins(Query leftQuery, List<JoinDTO> joinDTOS, Map<Integer, String> fgAliasLookup,
                                  Map<Integer, Featuregroup> fgLookup,
                                  Map<Integer, List<Feature>> availableFeatureLookup,
                                  boolean pitEnabled)
      throws FeaturestoreException {
    List<Join> joins = new ArrayList<>();
    for (JoinDTO joinDTO : joinDTOS) {
      if (joinDTO.getQuery() == null) {
        throw new IllegalArgumentException("Subquery not specified");
      }
      // Recursively convert the QueryDTO. Currently we don't support Joins of Joins
      Query rightQuery = convertQueryDTO(
              joinDTO.getQuery(), fgAliasLookup, fgLookup, availableFeatureLookup, pitEnabled);

      if (joinDTO.getOn() != null && !joinDTO.getOn().isEmpty()) {
        List<Feature> leftOn = joinDTO.getOn().stream().map(f -> new Feature(f.getName())).collect(Collectors.toList());
        List<Feature> rightOn =
          joinDTO.getOn().stream().map(f -> new Feature(f.getName())).collect(Collectors.toList());

        joins.add(extractLeftRightOn(leftQuery, rightQuery, leftOn, rightOn, joinDTO.getType(), joinDTO.getPrefix()));
      } else if (joinDTO.getLeftOn() != null && !joinDTO.getLeftOn().isEmpty()) {
        List<Feature> leftOn = joinDTO.getLeftOn().stream()
            .map(f -> new Feature(f.getName())).collect(Collectors.toList());
        List<Feature> rightOn = joinDTO.getRightOn().stream()
            .map(f -> new Feature(f.getName())).collect(Collectors.toList());

        joins.add(extractLeftRightOn(leftQuery, rightQuery, leftOn, rightOn, joinDTO.getType(), joinDTO.getPrefix()));
      } else {
        // Only if right feature group is present, extract the primary keys for the join
        joins.add(extractPrimaryKeysJoin(leftQuery, rightQuery, joinDTO.getType(), joinDTO.getPrefix()));
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
                                    JoinType joinType, String prefix)
      throws FeaturestoreException {
    // Make sure that they 2 list have the same length
    if (leftOn.size() != rightOn.size()) {
      throw new FeaturestoreException(RESTCodes.FeaturestoreErrorCode.LEFT_RIGHT_ON_DIFF_SIZES, Level.FINE);
    }
    
    // note currently we don't support letting the user specify different operators than EQUALS for joins
    // this is here for completeness and to have the possibility later to allow for it
    List<SqlCondition> joinOperator = leftOn.stream().map(f -> SqlCondition.EQUALS).collect(Collectors.toList());
    
    // make sure that join operator list has same length as on list
    if (joinOperator.size() != leftOn.size()) {
      throw new FeaturestoreException(RESTCodes.FeaturestoreErrorCode.JOIN_OPERATOR_MISMATCH, Level.FINE);
    }

    // Check that all the left features exist in the left query
    for (Feature feature : leftOn) {
      checkFeatureExistsAndSetAttributes(leftQuery, feature);
    }

    // Check that all the right features exist in the right query
    for (Feature feature : rightOn) {
      checkFeatureExistsAndSetAttributes(rightQuery, feature);
    }

    return new Join(leftQuery, rightQuery, leftOn, rightOn, joinType, prefix, joinOperator);
  }

  /**
   * In case the user has not specified any joining key, the largest subset of matching primary key will be used
   * for the join. The name should match for the feature to be added in the subset
   * @param leftQuery
   * @param rightQuery
   * @param joinType
   * @return
   */
  protected Join extractPrimaryKeysJoin(Query leftQuery, Query rightQuery, JoinType joinType, String prefix)
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

    // primary key join is always an EQUALS join since user does not specify anything
    List<SqlCondition> joinOperator = joinFeatures.stream().map(f -> SqlCondition.EQUALS).collect(Collectors.toList());

    return new Join(leftQuery, rightQuery, joinFeatures, joinFeatures, joinType, prefix, joinOperator);
  }

  private void checkFeatureExistsAndSetAttributes(Query query, Feature feature)
      throws FeaturestoreException {
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
  void removeDuplicateColumns(Query query, boolean pitEnabled) {
    for (Join join : query.getJoins()) {

      // Extract left join feature names and drop all features on right side with same name
      List<String> leftJoinFeatureNames = join.getLeftOn().stream().map(Feature::getName).collect(Collectors.toList());
  
      // Remove all features which are on the join condition and are not already present in the left side of the join
      List<Feature> filteredRightFeatures = new ArrayList<>();
      for (Feature rightFeature : join.getRightQuery().getFeatures()) {
        if (leftJoinFeatureNames.contains(rightFeature.getName()) &&
            join.getLeftQuery().getFeatures().stream().anyMatch(lf -> lf.getName().equals(
              Strings.isNullOrEmpty(rightFeature.getPrefix()) ? rightFeature.getName() :
                rightFeature.getPrefix() + rightFeature.getName())
            )) {
          // The feature is part of the joining condition and it's also part of the features list in the left query
          // no need to pass it here.
          continue;
        }

        filteredRightFeatures.add(rightFeature);
      }

      // drop event time from right side if PIT join
      if (pitEnabled) {
        filteredRightFeatures = filteredRightFeatures.stream()
                .filter(f -> !f.getName().equals(join.getRightQuery().getFeaturegroup().getEventTime()))
                .collect(Collectors.toList());
      }

      // replace the features for the right query
      join.getRightQuery().setFeatures(filteredRightFeatures);
    }
  }

  /**
   * Generate the SQL string. The backend will return a string to the client which is the SQL query to execute.
   * @param query
   * @return
   */
  public SqlSelect generateSQL(Query query, boolean online) {

    SqlNodeList selectList = new SqlNodeList(SqlParserPos.ZERO);
    for (Feature f : collectFeatures(query)) {
      // Build the select part. List of features selected by the user. Each feature will be fg_alias.fg_name
      // we should use the ` to avoid syntax errors on reserved keywords used as feature names (e.g. date)
      if (f.getDefaultValue() == null || online) {
        selectList.add(getWithOrWithoutPrefix(f));
      } else {
        selectList.add(selectWithDefaultAs(f));
      }
    }

    SqlNode joinNode = null;
    if (query.getJoins() == null || query.getJoins().isEmpty()) {
      // If there are no joins just set `from featuregroup`
      joinNode = generateTableNode(query, online);
    } else {
      // If there are joins generate the join list with the respective conditions
      joinNode = joinController.buildJoinNode(query, query.getJoins().size() - 1, online);
    }
    
    SqlNode filterNode = null;
    if (query.getJoins() == null || query.getJoins().isEmpty()) {
      // no joins, don't look for other filters
      if (query.getFilter() != null) {
        filterNode = filterController.generateFilterLogicNode(query.getFilter(), online);
      }
    } else {
      filterNode = filterController.buildFilterNode(query, query,query.getJoins().size() - 1, online);
    }

    // Assemble the query
    return new SqlSelect(SqlParserPos.ZERO, null, selectList, joinNode,
        filterNode, null, null, null, null, null, null);
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

  public List<Feature> collectFeatures(Query query) {
    List<Feature> features = new ArrayList<>(query.getFeatures());
    if (query.getJoins() != null) {
      for (Join join : query.getJoins()) {
        if (join.getRightQuery() != null && join.getRightQuery().getFeatures() != null) {
          // add prefix
          if (join.getPrefix() != null){
            for (Feature f: join.getRightQuery().getFeatures()){
              f.setPrefix(join.getPrefix());
            }
          }

          features.addAll(collectFeatures(join.getRightQuery()));
        }
      }
    }
    // Remove hudi spec metadata features if any
    if (query.getFeaturegroup().getFeaturegroupType() == FeaturegroupType.CACHED_FEATURE_GROUP &&
        query.getFeaturegroup().getCachedFeaturegroup().getTimeTravelFormat() == TimeTravelFormat.HUDI){
      features = cachedFeaturegroupController.dropHudiSpecFeatures(features);
    }
    return features;
  }

  private SqlNode generateCachedTableNode(Query query, boolean online) {
    List<String> tableIdentifierStr = new ArrayList<>();
    if (online) {
      tableIdentifierStr.add("`" + query.getProject() + "`");
      tableIdentifierStr.add("`" + query.getFeaturegroup().getName() + "_" + query.getFeaturegroup().getVersion()
          + "`");
    } else if (query.getFeaturegroup().getCachedFeaturegroup().getTimeTravelFormat() != TimeTravelFormat.HUDI
        || query.getHiveEngine()) {
      tableIdentifierStr.add("`" + query.getFeatureStore() + "`");
      tableIdentifierStr.add("`" + query.getFeaturegroup().getName() + "_" + query.getFeaturegroup().getVersion()
          + "`");
    } else {
      tableIdentifierStr.add("`" + query.getAs() + "`");
    }

    SqlNodeList asNodeList = new SqlNodeList(Arrays.asList(new SqlIdentifier(tableIdentifierStr, SqlParserPos.ZERO),
        new SqlIdentifier("`" + query.getAs() + "`", SqlParserPos.ZERO)), SqlParserPos.ZERO);

    return SqlStdOperatorTable.AS.createCall(asNodeList);
  }

  private SqlNode generateOnDemandTableNode(Query query) {
    return new SqlIdentifier("`" + query.getAs() + "`", SqlParserPos.ZERO);
  }

  /**
   * Generate the table node. The object will contain the fully qualified name of a feature group:
   * featurestore_name.feature_group_name_feature_group_version [as] feature_group alias
   * @param query
   * @return
   */
  public SqlNode generateTableNode(Query query, boolean online) {
    if (query.getFeaturegroup().getFeaturegroupType() == FeaturegroupType.CACHED_FEATURE_GROUP) {
      return generateCachedTableNode(query, online);
    } else {
      return generateOnDemandTableNode(query);
    }
  }

  public List<HudiFeatureGroupAliasDTO> getHudiAliases(Query query, List<HudiFeatureGroupAliasDTO> aliases,
                                                        Project project, Users user)
      throws FeaturestoreException, ServiceException {
    if (query.getFeaturegroup().getFeaturegroupType() == FeaturegroupType.CACHED_FEATURE_GROUP &&
        query.getFeaturegroup().getCachedFeaturegroup().getTimeTravelFormat() == TimeTravelFormat.HUDI) {
      CachedFeaturegroupDTO featuregroupDTO = new CachedFeaturegroupDTO(query.getFeaturegroup());
      Featuregroup featuregroup = query.getFeaturegroup();
      List<FeatureGroupFeatureDTO> featureGroupFeatureDTOS = cachedFeaturegroupController.getFeaturesDTO(
        featuregroup, project, user);
      featuregroupDTO.setFeatures(featureGroupFeatureDTOS);

      featuregroupDTO.setLocation(featurestoreUtils.resolveLocationURI(
        featuregroup.getCachedFeaturegroup().getHiveTbls().getSdId().getLocation()));

      if (query.getLeftFeatureGroupStartTimestamp() == null) {
        aliases.add(new HudiFeatureGroupAliasDTO(query.getAs(), featuregroupDTO,
            query.getLeftFeatureGroupEndTimestamp()));
      } else {
        aliases.add(new HudiFeatureGroupAliasDTO(query.getAs(), featuregroupDTO,
            query.getLeftFeatureGroupStartTimestamp(), query.getLeftFeatureGroupEndTimestamp()));
      }
    }

    if (query.getJoins() != null && !query.getJoins().isEmpty()) {
      for (Join join : query.getJoins()) {
        getHudiAliases(join.getRightQuery(), aliases, project, user);
      }
    }

    return aliases;
  }

  // TODO(Fabio): does it make sense to this in the same pass as where we generate the table nodes?
  // or does the code becomes even more complicated?
  public List<OnDemandFeatureGroupAliasDTO> getOnDemandAliases(Users user, Project project, Query query,
                                                                List<OnDemandFeatureGroupAliasDTO> aliases)
      throws FeaturestoreException {

    if (query.getFeaturegroup().getFeaturegroupType() == FeaturegroupType.ON_DEMAND_FEATURE_GROUP) {
      FeaturestoreStorageConnectorDTO featurestoreStorageConnectorDTO =
          storageConnectorController.convertToConnectorDTO(user, project,
              query.getFeaturegroup().getOnDemandFeaturegroup().getFeaturestoreConnector());
      OnDemandFeaturegroupDTO onDemandFeaturegroupDTO =
          new OnDemandFeaturegroupDTO(query.getFeaturegroup(), featurestoreStorageConnectorDTO);

      aliases.add(new OnDemandFeatureGroupAliasDTO(query.getAs(), onDemandFeaturegroupDTO));
    }

    if (query.getJoins() != null && !query.getJoins().isEmpty()) {
      for (Join join : query.getJoins()) {
        getOnDemandAliases(user, project, join.getRightQuery(), aliases);
      }
    }

    return aliases;
  }

  public SqlNode getWithOrWithoutPrefix(Feature feature){
    if (feature.getPrefix()!=null){
      return SqlStdOperatorTable.AS.createCall(SqlParserPos.ZERO,
          new SqlIdentifier(Arrays.asList("`" + feature.getFgAlias() + "`", "`" + feature.getName() + "`"),
              SqlParserPos.ZERO),
          new SqlIdentifier("`" +  feature.getPrefix() + feature.getName() + "`", SqlParserPos.ZERO));
    } else {
      return new SqlIdentifier(Arrays.asList("`" + feature.getFgAlias() + "`", "`" + feature.getName() + "`"),
          SqlParserPos.ZERO);
    }
  }
}
