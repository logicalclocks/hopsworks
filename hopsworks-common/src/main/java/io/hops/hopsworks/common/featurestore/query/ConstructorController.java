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

import com.logicalclocks.servicediscoverclient.exceptions.ServiceDiscoveryException;
import io.hops.hopsworks.common.featurestore.FeaturestoreController;
import io.hops.hopsworks.common.featurestore.featuregroup.FeaturegroupController;
import io.hops.hopsworks.common.featurestore.featuregroup.cached.CachedFeaturegroupController;
import io.hops.hopsworks.common.featurestore.featuregroup.cached.CachedFeaturegroupDTO;
import io.hops.hopsworks.common.featurestore.featuregroup.ondemand.OnDemandFeaturegroupDTO;
import io.hops.hopsworks.common.featurestore.featuregroup.stream.StreamFeatureGroupDTO;
import io.hops.hopsworks.common.featurestore.query.filter.FilterController;
import io.hops.hopsworks.common.featurestore.query.filter.FilterLogic;
import io.hops.hopsworks.common.featurestore.query.join.Join;
import io.hops.hopsworks.common.featurestore.query.join.JoinController;
import io.hops.hopsworks.common.featurestore.query.pit.PitJoinController;
import io.hops.hopsworks.common.featurestore.storageconnectors.FeaturestoreStorageConnectorController;
import io.hops.hopsworks.common.featurestore.storageconnectors.FeaturestoreStorageConnectorDTO;
import io.hops.hopsworks.common.featurestore.utils.FeaturestoreUtils;
import io.hops.hopsworks.exceptions.FeaturestoreException;
import io.hops.hopsworks.exceptions.ServiceException;
import io.hops.hopsworks.persistence.entity.featurestore.featuregroup.Featuregroup;
import io.hops.hopsworks.persistence.entity.featurestore.featuregroup.FeaturegroupType;
import io.hops.hopsworks.persistence.entity.featurestore.featuregroup.cached.TimeTravelFormat;
import io.hops.hopsworks.persistence.entity.featurestore.trainingdataset.SqlCondition;
import io.hops.hopsworks.persistence.entity.project.Project;
import io.hops.hopsworks.persistence.entity.user.Users;
import io.hops.hopsworks.restutils.RESTCodes;
import org.apache.calcite.sql.SqlDialect;
import org.apache.calcite.sql.SqlIdentifier;
import org.apache.calcite.sql.SqlLiteral;
import org.apache.calcite.sql.SqlNode;
import org.apache.calcite.sql.SqlNodeList;
import org.apache.calcite.sql.SqlSelect;
import org.apache.calcite.sql.JoinType;
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
import java.util.Comparator;
import java.util.List;
import java.util.logging.Level;
import java.util.stream.Collectors;

@Stateless
@TransactionAttribute(TransactionAttributeType.NEVER)
public class ConstructorController {

  @EJB
  private FeaturegroupController featuregroupController;

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
  @EJB
  private FeaturestoreController featurestoreController;

  public ConstructorController() {
  }

  // For testing
  public ConstructorController(FeaturegroupController featuregroupController,
      CachedFeaturegroupController cachedFeaturegroupController,
      FilterController filterController,
      JoinController joinController) {
    this.featuregroupController = featuregroupController;
    this.cachedFeaturegroupController = cachedFeaturegroupController;
    this.filterController = filterController;
    this.joinController = joinController;
  }

  public FsQueryDTO construct(Query query, boolean pitEnabled, Project project, Users user)
      throws FeaturestoreException, ServiceException {
    return construct(query, pitEnabled, false, project, user);
  }

  public FsQueryDTO construct(Query query, boolean pitEnabled, boolean isTrainingDataset, Project project, Users user)
      throws FeaturestoreException, ServiceException {
    FsQueryDTO fsQueryDTO = new FsQueryDTO();

    if (query.getDeletedFeatureGroups() != null && !query.getDeletedFeatureGroups().isEmpty()) {
      fsQueryDTO.setQuery(String.format("Parent feature groups of the following features are not available anymore: " +
        "%s", String.join(", ", query.getDeletedFeatureGroups())));
      return fsQueryDTO;
    }

    fsQueryDTO.setQuery(makeOfflineQuery(query));
    fsQueryDTO.setHudiCachedFeatureGroups(getHudiAliases(query));
    fsQueryDTO.setOnDemandFeatureGroups(getOnDemandAliases(user, project, query));
    fsQueryDTO.setQueryOnline(
      generateSQL(query, true).toSqlString(new SparkSqlDialect(SqlDialect.EMPTY_CONTEXT)).getSql());

    if (pitEnabled) {
      fsQueryDTO.setPitQuery(makePitQuery(query, isTrainingDataset));
      fsQueryDTO.setPitQueryAsof(makePitQueryAsof(query, isTrainingDataset));
    }

    return fsQueryDTO;
  }

  String makeOfflineQuery(Query query) {
    SqlDialect offlineSqlDialect = query.getHiveEngine() ? new HiveSqlDialect(SqlDialect.EMPTY_CONTEXT) :
        new SparkSqlDialect(SqlDialect.EMPTY_CONTEXT);
    return generateSQL(query, false).toSqlString(offlineSqlDialect).getSql();
  }

  String makePitQuery(Query query, boolean isTrainingDataset) {
    SqlNode pitQuery = pitJoinController.generateSQL(query, isTrainingDataset);
    return query.getHiveEngine() ? pitQuery.toSqlString(new HiveSqlDialect(SqlDialect.EMPTY_CONTEXT)).getSql() :
        pitQuery.toSqlString(new SparkSqlDialect(SqlDialect.EMPTY_CONTEXT)).getSql();
  }

  public String makePitQueryAsof(Query query, boolean isTrainingDataset) {
    SqlDialect offlineSqlDialect = query.getHiveEngine() ? new HiveSqlDialect(SqlDialect.EMPTY_CONTEXT) :
            new SparkSqlDialect(SqlDialect.EMPTY_CONTEXT);
    return generateSQL(query, false, true, isTrainingDataset).toSqlString(offlineSqlDialect).getSql();
  }

  /**
   * Generate the SQL string. The backend will return a string to the client which is the SQL query to execute.
   *
   * @param query
   * @return
   */
  public SqlSelect generateSQL(Query query, boolean online) {
    return this.generateSQL(query, online, false, false);
  }

    /**
     * Generate the SQL string. The backend will return a string to the client which is the SQL query to execute.
     *
     * @param query
     * @return
     */
  public SqlSelect generateSQL(Query query, boolean online, boolean pitQuery, boolean isTrainingDataset) {

    List<Feature> finalSelectList = collectFeatures(query);
    // sort features in last select
    if (isTrainingDataset) {
      finalSelectList = finalSelectList.stream()
              .sorted(Comparator.comparing(Feature::getIdx))
              .collect(Collectors.toList());
    }

    SqlNodeList selectList = new SqlNodeList(SqlParserPos.ZERO);
    for (Feature f : finalSelectList) {
      // Build the select part. List of features selected by the user. Each feature will be fg_alias.fg_name
      // we should use the ` to avoid syntax errors on reserved keywords used as feature names (e.g. date)
      if (f.getDefaultValue() == null) {
        selectList.add(getWithOrWithoutAs(f, true,true));
      } else {
        selectList.add(selectWithDefaultAs(f, true));
      }
    }



    if (pitQuery) {
      List<Join> newJoins = new ArrayList<>();
      for (Join join : query.getJoins()) {
        JoinType joinType = join.getJoinType();
        if (joinType == JoinType.INNER) {
          joinType = JoinType.INNER_ASOF;
        } else if (joinType == JoinType.LEFT) {
          joinType = JoinType.LEFT_ASOF;
        } else if (joinType == JoinType.RIGHT) {
          joinType = JoinType.RIGHT_ASOF;
        } else if (joinType == JoinType.FULL) {
          joinType = JoinType.FULL_ASOF;
        }
        // add event time inequality join condition
        List<Feature> newLeftOn = this.addEventTimeOn(join.getLeftOn(), query.getFeaturegroup(), query.getAs());
        List<Feature> newRightOn = this.addEventTimeOn(join.getRightOn(), join.getRightQuery().getFeaturegroup(),
                join.getRightQuery().getAs());
        List<SqlCondition> newJoinOperator =
                this.addEventTimeCondition(join.getJoinOperator(), SqlCondition.GREATER_THAN_OR_EQUAL);

        // single right feature group
        newJoins.add(new Join(query, join.getRightQuery(), newLeftOn, newRightOn, joinType, join.getPrefix(),
                        newJoinOperator));

      }
      // make a copy of original query to replace joins
      query = new Query(query.getFeatureStore(), query.getProject(), query.getFeaturegroup(), query.getAs(),
              new ArrayList<>(query.getFeatures()), query.getAvailableFeatures(), query.getHiveEngine(),
              query.getFilter());
      query.setJoins(newJoins);
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
      filterNode = filterController.buildFilterNode(query, query, query.getJoins().size() - 1, online);
    }

    SqlNodeList orderByList = null;
    if (query.getOrderByFeatures() != null && !query.getOrderByFeatures().isEmpty()) {
      orderByList = new SqlNodeList(SqlParserPos.ZERO);
      for (Feature f : query.getOrderByFeatures()) {
        // Build the order by part. List of features selected by the user. Each feature will be fg_alias.fg_name
        // we should use the ` to avoid syntax errors on reserved keywords used as feature names (e.g. date)
        // if feature gas prefix don't add it here as it will not work for order by.
        if (f.getDefaultValue() == null) {
          orderByList.add(getWithOrWithoutAs(f, false,false));
        } else {
          orderByList.add(selectWithDefaultAs(f, false));
        }
      }
    }

    // Assemble the query
    return new SqlSelect(SqlParserPos.ZERO, null, selectList, joinNode,
        filterNode, null, null, null, orderByList, null, null, null);
  }

  public List<Feature> addEventTimeOn(List<Feature> on, Featuregroup featureGroup, String fgAlias) {
    // make copy of features since otherwise it leads to problems when setting aliases later on
    List<Feature> newOn = on.stream().map(f -> new Feature(f.getName(), f.getFgAlias(), f.isPrimary()))
            .collect(Collectors.toList());
    newOn.add(new Feature(featureGroup.getEventTime(), fgAlias));
    return newOn;
  }

  public List<SqlCondition> addEventTimeCondition(List<SqlCondition> joinCondition, SqlCondition operator) {
    List<SqlCondition> newJoinCondition = new ArrayList<>(joinCondition);
    newJoinCondition.add(operator);
    return newJoinCondition;
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

  public SqlNode selectWithDefaultAs(Feature feature, boolean withPrefix) {
    String featureName = feature.getName();
    if (feature.getPrefix() != null && withPrefix) {
      featureName = feature.getPrefix() + featureName;
    }
    return SqlStdOperatorTable.AS.createCall(new SqlNodeList(Arrays.asList(caseWhenDefault(feature),
        new SqlIdentifier("`" + featureName + "`", SqlParserPos.ZERO)), SqlParserPos.ZERO));
  }

  public List<Feature> collectFeatures(Query query) {
    List<Feature> features = new ArrayList<>(query.getFeatures());
    if (query.getJoins() != null) {
      for (Join join : query.getJoins()) {
        if (join.getRightQuery() != null && join.getRightQuery().getFeatures() != null) {
          // add prefix
          if (join.getPrefix() != null) {
            for (Feature f : join.getRightQuery().getFeatures()) {
              f.setPrefix(join.getPrefix());
            }
          }

          features.addAll(collectFeatures(join.getRightQuery()));
        }
      }
    }
    // Remove hudi spec metadata features if any
    if (query.getFeaturegroup().getFeaturegroupType() == FeaturegroupType.STREAM_FEATURE_GROUP ||
      (query.getFeaturegroup().getFeaturegroupType() == FeaturegroupType.CACHED_FEATURE_GROUP &&
        query.getFeaturegroup().getCachedFeaturegroup().getTimeTravelFormat() == TimeTravelFormat.HUDI)) {
      features = cachedFeaturegroupController.dropHudiSpecFeatures(features);
    }
    return features;
  }

  public List<Feature> collectFeaturesFromFilter(FilterLogic filter) {
    return this.collectFeaturesFromFilter(filter, null);
  }

  public List<Feature> collectFeaturesFromFilter(FilterLogic filter, Query query) {
    List<Feature> features = new ArrayList<>();
    collectFeatureFromFilter(filter, features, query);
    return features;
  }

  private void collectFeatureFromFilter(FilterLogic filter, List<Feature> features, Query query) {
    if(filter.getLeftFilter() != null) {
      features.addAll(filter.getLeftFilter().getFeatures().stream().filter(f ->
        (query == null || f.getFeatureGroup().equals(query.getFeaturegroup()))).collect(Collectors.toList()));
    }
    if(filter.getRightFilter() != null) {
      features.addAll(filter.getRightFilter().getFeatures().stream().filter(f ->
        (query == null || f.getFeatureGroup().equals(query.getFeaturegroup()))).collect(Collectors.toList()));
    }
    if(filter.getLeftLogic() != null) {
      collectFeatureFromFilter(filter.getLeftLogic(), features, query);
    }
    if(filter.getRightLogic() != null) {
      collectFeatureFromFilter(filter.getRightLogic(), features, query);
    }
  }

  private SqlNode generateCachedTableNode(Query query, boolean online) {
    List<String> tableIdentifierStr = new ArrayList<>();
    if (online) {
      tableIdentifierStr.add("`" + query.getProject() + "`");
      tableIdentifierStr.add("`" + query.getFeaturegroup().getName() + "_" + query.getFeaturegroup().getVersion()
          + "`");
    } else if (query.getHiveEngine() || !isHudiTimeTravelFeatureGroup(query)) {
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

  private SqlNode generateOnDemandTableNode(Query query, boolean online) {
    if (online) {
      List<String> tableIdentifierStr = new ArrayList<>();
      tableIdentifierStr.add("`" + query.getProject() + "`");
      tableIdentifierStr.add("`" + query.getFeaturegroup().getName() + "_" + query.getFeaturegroup().getVersion()
        + "`");
      SqlNodeList asNodeList = new SqlNodeList(Arrays.asList(new SqlIdentifier(tableIdentifierStr, SqlParserPos.ZERO),
        new SqlIdentifier("`" + query.getAs() + "`", SqlParserPos.ZERO)), SqlParserPos.ZERO);
      return SqlStdOperatorTable.AS.createCall(asNodeList);
    }
    return new SqlIdentifier("`" + query.getAs() + "`", SqlParserPos.ZERO);
  }

  /**
   * Generate the table node. The object will contain the fully qualified name of a feature group:
   * featurestore_name.feature_group_name_feature_group_version [as] feature_group alias
   *
   * @param query
   * @return
   */
  public SqlNode generateTableNode(Query query, boolean online) {
    if (query.getFeaturegroup().getFeaturegroupType() != FeaturegroupType.ON_DEMAND_FEATURE_GROUP) {
      return generateCachedTableNode(query, online);
    } else {
      return generateOnDemandTableNode(query, online);
    }
  }

  public List<HudiFeatureGroupAliasDTO> getHudiAliases(Query query) throws ServiceException {
    List<HudiFeatureGroupAliasDTO> aliases = new ArrayList<>();
    Featuregroup featuregroup = query.getFeaturegroup();
    if (featuregroup.getFeaturegroupType() == FeaturegroupType.CACHED_FEATURE_GROUP &&
            isHudiTimeTravelFeatureGroup(query)) {
      CachedFeaturegroupDTO featuregroupDTO = new CachedFeaturegroupDTO(featuregroup);
      String featurestoreName =
              featurestoreController.getOfflineFeaturestoreDbName(featuregroup.getFeaturestore());
      featuregroupDTO.setFeaturestoreName(featurestoreName);
      featuregroupDTO.setLocation(featurestoreUtils.resolveLocation(
              featuregroupController.getFeatureGroupLocation(featuregroup)));
      aliases.add(new HudiFeatureGroupAliasDTO(query.getAs(), featuregroupDTO,
          query.getLeftFeatureGroupStartTimestamp(), query.getLeftFeatureGroupEndTimestamp()));
    } else if (featuregroup.getFeaturegroupType() == FeaturegroupType.STREAM_FEATURE_GROUP &&
            isHudiTimeTravelFeatureGroup(query)) {
      StreamFeatureGroupDTO featuregroupDTO = new StreamFeatureGroupDTO(featuregroup);
      String featurestoreName =
              featurestoreController.getOfflineFeaturestoreDbName(featuregroup.getFeaturestore());
      featuregroupDTO.setFeaturestoreName(featurestoreName);
      featuregroupDTO.setLocation(featurestoreUtils.resolveLocation(
              featuregroupController.getFeatureGroupLocation(featuregroup)));
      aliases.add(new HudiFeatureGroupAliasDTO(query.getAs(), featuregroupDTO,
          query.getLeftFeatureGroupStartTimestamp(), query.getLeftFeatureGroupEndTimestamp()));
    }

    if (query.getJoins() != null && !query.getJoins().isEmpty()) {
      for (Join join : query.getJoins()) {
        aliases.addAll(getHudiAliases(join.getRightQuery()));
      }
    }

    return aliases;
  }

  private boolean isHudiTimeTravelFeatureGroup(Query query) {
    Boolean isHudiFg = (query.getFeaturegroup().getFeaturegroupType() == FeaturegroupType.STREAM_FEATURE_GROUP ||
            (query.getFeaturegroup().getFeaturegroupType() == FeaturegroupType.CACHED_FEATURE_GROUP &&
                    query.getFeaturegroup().getCachedFeaturegroup().getTimeTravelFormat() == TimeTravelFormat.HUDI));
    Boolean hasTimeTravel = (query.getLeftFeatureGroupStartTimestamp() != null &&
            query.getLeftFeatureGroupStartTimestamp() != 0) ||
            query.getLeftFeatureGroupEndTimestamp() != null;
    return isHudiFg && hasTimeTravel;
  }

  // TODO(Fabio): does it make sense to this in the same pass as where we generate the table nodes?
  // or does the code becomes even more complicated?
  public List<OnDemandFeatureGroupAliasDTO> getOnDemandAliases(Users user, Project project, Query query)
      throws FeaturestoreException, ServiceException {
    List<OnDemandFeatureGroupAliasDTO> aliases = new ArrayList<>();

    if (query.getFeaturegroup().getFeaturegroupType() == FeaturegroupType.ON_DEMAND_FEATURE_GROUP) {
      FeaturestoreStorageConnectorDTO storageConnectorDTO = null;
      if (!query.getFeaturegroup().getOnDemandFeaturegroup().isSpine()) {
        storageConnectorDTO =
          storageConnectorController.convertToConnectorDTO(user, project,
            query.getFeaturegroup().getOnDemandFeaturegroup().getFeaturestoreConnector());
      }
      OnDemandFeaturegroupDTO onDemandFeaturegroupDTO =
          new OnDemandFeaturegroupDTO(query.getFeaturegroup(), storageConnectorDTO);
      try {
        String path = featuregroupController.getFeatureGroupLocation(query.getFeaturegroup());
        onDemandFeaturegroupDTO.setLocation(featurestoreUtils.prependNameNode(path));
      } catch (ServiceDiscoveryException e) {
        throw new ServiceException(RESTCodes.ServiceErrorCode.SERVICE_NOT_FOUND, Level.SEVERE);
      }

      aliases.add(new OnDemandFeatureGroupAliasDTO(query.getAs(), onDemandFeaturegroupDTO));
    }

    if (query.getJoins() != null && !query.getJoins().isEmpty()) {
      for (Join join : query.getJoins()) {
        aliases.addAll(getOnDemandAliases(user, project, join.getRightQuery()));
      }
    }

    return aliases;
  }

  protected SqlNode getWithOrWithoutAs(Feature feature, boolean withAs, boolean withPrefix) {
    if (!withAs) {
      return new SqlIdentifier(Arrays.asList("`" + feature.getFgAlias() + "`", "`" + feature.getName() + "`"),
        SqlParserPos.ZERO);
    }
    String featureName = feature.getName();
    if (feature.getPrefix() != null && withPrefix) {
      featureName = feature.getPrefix() + featureName;
    }
    return SqlStdOperatorTable.AS.createCall(SqlParserPos.ZERO,
        new SqlIdentifier(Arrays.asList("`" + feature.getFgAlias() + "`", "`" + feature.getName() + "`"),
            SqlParserPos.ZERO),
        new SqlIdentifier("`" + featureName + "`", SqlParserPos.ZERO));
  }
}
