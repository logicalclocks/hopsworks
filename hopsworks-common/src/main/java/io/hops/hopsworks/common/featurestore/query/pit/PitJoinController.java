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

package io.hops.hopsworks.common.featurestore.query.pit;

import com.google.common.base.Strings;
import io.hops.hopsworks.common.featurestore.query.ConstructorController;
import io.hops.hopsworks.common.featurestore.query.Feature;
import io.hops.hopsworks.common.featurestore.query.Query;
import io.hops.hopsworks.common.featurestore.query.QueryDTO;
import io.hops.hopsworks.persistence.entity.featurestore.featuregroup.Featuregroup;
import io.hops.hopsworks.persistence.entity.featurestore.trainingdataset.SqlCondition;
import io.hops.hopsworks.common.featurestore.query.filter.Filter;
import io.hops.hopsworks.common.featurestore.query.filter.FilterController;
import io.hops.hopsworks.common.featurestore.query.join.Join;
import io.hops.hopsworks.common.featurestore.query.join.JoinController;
import org.apache.calcite.sql.JoinConditionType;
import org.apache.calcite.sql.JoinType;
import org.apache.calcite.sql.SqlCall;
import org.apache.calcite.sql.SqlIdentifier;
import org.apache.calcite.sql.SqlJoin;
import org.apache.calcite.sql.SqlLiteral;
import org.apache.calcite.sql.SqlNode;
import org.apache.calcite.sql.SqlNodeList;
import org.apache.calcite.sql.SqlSelect;
import org.apache.calcite.sql.SqlWindow;
import org.apache.calcite.sql.SqlWith;
import org.apache.calcite.sql.fun.SqlStdOperatorTable;
import org.apache.calcite.sql.parser.SqlParserPos;

import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Stateless
@TransactionAttribute(TransactionAttributeType.NEVER)
public class PitJoinController {

  @EJB
  private ConstructorController constructorController;
  @EJB
  private FilterController filterController;
  @EJB
  private JoinController joinController;

  // name for the rank column added for the point-in-time join query
  private final static String PIT_JOIN_RANK = "pit_rank_hopsworks";
  private final static String ALL_FEATURES = "*";
  private final static String FG_SUBQUERY = "right_fg";
  // subqueries need an alias in hive, this is not actually used to refer to anything
  private final static String HIVE_ALIAS_PLACEHOLDER = "NA";
  private final static String HIVE_AS = " AS";
  private final static String PK_JOIN_PREFIX = "join_pk_";
  private final static String EVT_JOIN_PREFIX = "join_evt_";

  public PitJoinController() {
  }

  // for testing
  public PitJoinController(ConstructorController constructorController, FilterController filterController,
                           JoinController joinController) {
    this.constructorController = constructorController;
    this.filterController = filterController;
    this.joinController = joinController;
  }

  public boolean isPitEnabled(QueryDTO queryDTO) {
    if (queryDTO.getJoins() == null || queryDTO.getJoins().isEmpty()) {
      return false;
    }
    // validate if right feature group is event time enabled
    // for now we allow only pit joins with all feature groups event time enabled
    boolean eventTimeEnabled =
        queryDTO.getJoins().stream().allMatch(j -> j.getQuery().getLeftFeatureGroup().getEventTime() != null);

    return queryDTO.getLeftFeatureGroup().getEventTime() != null && eventTimeEnabled;
  }

  public boolean isPitEnabled(Query query) {
    if (query.getJoins() == null || query.getJoins().isEmpty()) {
      return false;
    }
    // validate if right feature group is event time enabled
    // for now we allow only pit joins with all feature groups event time enabled
    boolean eventTimeEnabled =
        query.getJoins().stream().allMatch(j -> j.getRightQuery().getFeaturegroup().getEventTime() != null);

    return query.getFeaturegroup().getEventTime() != null && eventTimeEnabled;
  }

  public List<SqlCall> generateSubQueries(Query baseQuery, Query query, boolean isTrainingDataset,
                                          List<Feature> outerQueryFilterFeatures,
                                          boolean pushdownOuterQueryFilter) {
    List<SqlCall> subQueries = new ArrayList<>();

    // we always re-select all primary key columns of the "label group" in order to be able to perform final join
    List<Feature> additionalPkFeatures = query.getAvailableFeatures().stream().filter(Feature::isPrimary)
        .map(f ->
            new Feature(f.getName(), f.getFgAlias(), f.getType(), f.isPrimary(), f.getDefaultValue(), PK_JOIN_PREFIX,
                f.getFeatureGroup()))
        .collect(Collectors.toList());
    additionalPkFeatures.add(new Feature(query.getFeaturegroup().getEventTime(), query.getAs(), (String) null,
        null, EVT_JOIN_PREFIX));
    additionalPkFeatures.forEach(f -> f.setFeatureGroup(query.getFeaturegroup()));

    if (outerQueryFilterFeatures != null && !pushdownOuterQueryFilter) {
      // We are in any other scenario, we need to make sure that the features are selected in the subquery
      // for the filter to be applied later

      // We do it here for the label feature group as it's reused across all the joins
      List<Feature> missingLeftJoinFeatures = outerQueryFilterFeatures.stream()
          .filter(f -> f.getFeatureGroup().equals(baseQuery.getFeaturegroup()))
          .filter(f -> !baseQuery.getFeatures().contains(f))
          .collect(Collectors.toList());
      baseQuery.getFeatures().addAll(missingLeftJoinFeatures);
    }

    // each join will result in one WITH subquery which joins a right feature group always with the same base feature
    // group
    for (Join join : query.getJoins()) {
      // add event time inequality join condition
      List<Feature> newLeftOn = constructorController.addEventTimeOn(join.getLeftOn(),
          join.getLeftQuery().getFeaturegroup(), join.getLeftQuery().getAs());
      List<Feature> newRightOn = constructorController.addEventTimeOn(join.getRightOn(),
          join.getRightQuery().getFeaturegroup(), join.getRightQuery().getAs());
      List<SqlCondition> newJoinOperator =
          constructorController.addEventTimeCondition(join.getJoinOperator(), SqlCondition.GREATER_THAN_OR_EQUAL);

      List<Feature> originalRightJoinFeatures = new ArrayList<>(join.getRightQuery().getFeatures());

      // Add filters if needed
      if (pushdownOuterQueryFilter) {
        // We are in scenario 3/4, so we can safely push down the filter on the left side of the join
        // which here is represented by the baseQuery object
        baseQuery.setFilter(query.getFilter());
      } else if (outerQueryFilterFeatures != null) {
        // We are in any other scenario, we need to make sure that the features are selected in the subquery
        // for the filter to be applied later

        // Here we need to ensure the features are selected only for the right side of the join
        // as the left side is the label feature group (baseQuery) which is already taken care above.
        List<Feature> missingRightJoinFeatures = outerQueryFilterFeatures.stream()
            .filter(f -> f.getFeatureGroup().equals(join.getRightQuery().getFeaturegroup()))
            .filter(f -> !join.getRightQuery().getFeatures().contains(f))
            .collect(Collectors.toList());
        join.getRightQuery().getFeatures().addAll(missingRightJoinFeatures);
      }

      // single right feature group
      List<Join> newJoins = Collections.singletonList(
          new Join(baseQuery, join.getRightQuery(), newLeftOn, newRightOn, join.getJoinType(), join.getPrefix(),
              newJoinOperator));
      baseQuery.setJoins(newJoins);

      // if it is a training dataset all features are collected in left outer most query, so here we drop all
      // features from the base query that don't belong to any of the two feature groups joined in this sub query
      // if it's a regular query, this is a no op
      if (isTrainingDataset) {
        baseQuery.setFeatures(dropIrrelevantSubqueryFeatures(query, join.getRightQuery()));
      }
      baseQuery.getFeatures().addAll(additionalPkFeatures);

      // first generate subquery and subsequently add rank over window
      SqlSelect subQuery = constructorController.generateSQL(baseQuery, false);

      // reset the feature list
      join.getRightQuery().setFeatures(originalRightJoinFeatures);

      // now add rank over window
      subQuery.getSelectList().add(rankOverAs(newLeftOn,
          new Feature(join.getRightQuery().getFeaturegroup().getEventTime(), join.getRightQuery().getAs(), false)));
      subQueries.add(SqlStdOperatorTable.AS.createCall(SqlParserPos.ZERO, subQuery,
          new SqlIdentifier(HIVE_ALIAS_PLACEHOLDER, SqlParserPos.ZERO)));

      // reset the base query features. It doesn't matter if we reset the missingLeftJoinFeatures as well
      // as later on, we'll use the first subquery to select the PIT Alias for all the filter features
      // from the label feature group
      baseQuery.setFeatures(new ArrayList<>(query.getFeatures()));
    }

    return subQueries;
  }

  private SqlNodeList partitionBy(List<Feature> partitionFeatures) {
    SqlNodeList partitionBy = new SqlNodeList(SqlParserPos.ZERO);
    partitionFeatures.forEach(joinFeature -> {
      partitionBy.add(new SqlIdentifier(Arrays.asList("`" + joinFeature.getFgAlias() + "`",
          "`" + joinFeature.getName() + "`"), SqlParserPos.ZERO));
    });
    return partitionBy;
  }

  private SqlNodeList orderByDesc(Feature feature) {
    return SqlNodeList.of(SqlStdOperatorTable.DESC.createCall(SqlParserPos.ZERO,
        new SqlIdentifier(Arrays.asList("`" + feature.getFgAlias(false) + "`",
            "`" + feature.getName() + "`"), SqlParserPos.ZERO)));
  }

  public SqlNode rankOverAs(List<Feature> partitionByFeatures, Feature orderByFeature) {
    SqlNode rank = SqlStdOperatorTable.RANK.createCall(SqlParserPos.ZERO);
    SqlNodeList partitionBy = partitionBy(partitionByFeatures);
    SqlNodeList orderList = orderByDesc(orderByFeature);
    SqlWindow win = SqlWindow.create(null, null, partitionBy, orderList, null, null,
        null, null, SqlParserPos.ZERO);
    SqlNode over = SqlStdOperatorTable.OVER.createCall(SqlParserPos.ZERO, rank, win);
    return SqlStdOperatorTable.AS.createCall(SqlParserPos.ZERO, over,
        new SqlIdentifier(PIT_JOIN_RANK, SqlParserPos.ZERO));
  }

  public List<SqlSelect> wrapSubQueries(List<SqlCall> sqlSelects) {
    List<SqlSelect> newSubQueries = new ArrayList<>();

    for (SqlCall select : sqlSelects) {
      SqlNode whereRank = filterController.generateFilterNode(new Filter(Arrays.asList(new Feature(PIT_JOIN_RANK, null,
          "int", null, null)), SqlCondition.EQUALS, "1"), false);
      SqlNodeList selectList = SqlNodeList.of(new SqlIdentifier(ALL_FEATURES, SqlParserPos.ZERO));
      newSubQueries.add(new SqlSelect(SqlParserPos.ZERO, null, selectList, select, whereRank, null,
          null, null, null, null, null, null));
    }
    return newSubQueries;
  }

  public SqlNode generateSQL(Query query, boolean isTrainingDataset) {
    // make a copy of base query to replace joins
    Query baseQuery = new Query(query.getFeatureStore(), query.getProject(), query.getFeaturegroup(), query.getAs(),
        new ArrayList<>(query.getFeatures()), query.getAvailableFeatures(), query.getHiveEngine(), null);

    // collect left outer most features
    List<Feature> finalSelectList = constructorController.collectFeatures(baseQuery);

    // collect all features involved into the outer query filter, if any
    /**
     * Each join query can have its own filters, i.e.:
     * (1) query = trans_fg.select_all() \
     *     .join(window_aggs_fg.select_all().filter(window_aggs_fg.trans_freq >= 0.0))
     *
     * If this is the case, nothing special needs to be done, the filters will be attached to the query object on the
     * right side of the join (i.e., join.getRightQuery()) and they will be properly included when calling
     * the generateSQL method.
     *
     * A different case would be to have the filter on the root query, i.e.:
     * (2) query = trans_fg.select_all() \
     *     .join(window_aggs_fg.select_all([some, feature])) \
     *     .filter(window_aggs_fg.trans_freq >= 0.0)
     *
     * In this case the query will be applied on the outer query (i.e., the query joining all the WITH subqueries).
     * however, when generating the subqueries, we need to make sure that the feature used in the filter is selected
     * in the subquery and later on dropped in the final projection of the outer query.
     *
     * One special case to the rule above is the following:
     * (3) query = trans_fg.select_all() \
     *     .join(window_aggs_fg.select_all()) \
     *     .filter(trans_fg.category == "Cash Withdrawal")
     *
     * which is equivalent to:
     * (4) query = trans_fg.select_all().filter(trans_fg.category == "Cash Withdrawal")
     * (minus the join)
     *
     * Examples 3 and 4 are equivalent to example 1, to distinguish whether or not the filter is applied only on the
     * label feature group, or on the entire query, we compare the featureGroupId of the features involved in the
     * filter and the id of the label feature group. If and only if all the features featureGroupId match the label
     * feature group id, then we push down the filter to the subquery.
     *
     * If there is a mix of ids, we revert back to scenario 2. This is to avoid breaking filters such as:
     * (5) trans_fg.category == "Cash Withdrawal" AND window_aggs_fg.trans_freq >= 0.0
     * which can potentially span multiple subqueries. In this case we keep our code simple and we let the
     * Spark/Hive/DuckDb query optimizer decide whether or not it's safe to break the WHERE condition and push it down
     * to the subqueries.
     */
    List<Feature> outerQueryFilterFeatures = null;
    boolean pushdownOuterQueryFilter = false;
    if (query.getFilter() != null) {
      outerQueryFilterFeatures = constructorController.collectFeaturesFromFilter(query.getFilter());

      pushdownOuterQueryFilter = outerQueryFilterFeatures.stream()
          .map(Feature::getFeatureGroup)
          .allMatch(filterFg -> filterFg.equals(query.getFeaturegroup()));
    }

    // generate subqueries for WITH
    List<SqlSelect> withSelects = wrapSubQueries(
        generateSubQueries(baseQuery, query, isTrainingDataset, outerQueryFilterFeatures, pushdownOuterQueryFilter));
    finalSelectList.forEach(f -> f.setPitFgAlias(FG_SUBQUERY + "0"));

    // list for "x0 as ..."
    SqlNodeList selectAsses = new SqlNodeList(SqlParserPos.ZERO);

    // joins for the body of the WITH statement, bringing together the final result
    List<Join> newJoins = new ArrayList<>();

    // fix all the pit aliases of the filter features of the label feature group if they have not been pushed
    // down to subquery
    setFilterFeaturesPitAlias(outerQueryFilterFeatures,
        pushdownOuterQueryFilter,
        baseQuery.getFeaturegroup(), FG_SUBQUERY + "0");

    // each sqlSelect represents one subquery corresponding to one join in the final WITH body
    for (int i = 0; i < withSelects.size(); i++) {
      selectAsses.add(SqlStdOperatorTable.AS.createCall(
          // even with HiveSQLDialect, Calcite is not actually adding the AS keyword, however Hive syntax makes it
          // mandatory when using "WITH xyz AS ()" therefore we need to add it manually as string here
          SqlNodeList.of(new SqlIdentifier(FG_SUBQUERY + i + HIVE_AS, SqlParserPos.ZERO), withSelects.get(i))));

      // each select corresponds to one join, collect features and update alias, drop event time features from "right"
      // feature groups
      String pitAlias = FG_SUBQUERY + i;
      if (isTrainingDataset) {
        // for training datasets all features are contained in final select list from beginning, set the correct
        // alias only only for the features corresponding to the feature group in the current join
        int finalI = i;
        finalSelectList.stream()
            .filter(f -> f.getFeatureGroup() == query.getJoins().get(finalI).getRightQuery().getFeaturegroup())
            .forEach(f -> f.setPitFgAlias(pitAlias));
      } else {
        List<Feature> features = constructorController.collectFeatures(query.getJoins().get(i).getRightQuery());
        features.forEach(f -> f.setPitFgAlias(pitAlias));
        finalSelectList.addAll(features);
      }

      setFilterFeaturesPitAlias(outerQueryFilterFeatures,
          pushdownOuterQueryFilter,
          query.getJoins().get(i).getRightQuery().getFeaturegroup(), pitAlias);

      // add event time inequality join condition
      List<Feature> primaryKey =
          baseQuery.getAvailableFeatures().stream().filter(Feature::isPrimary).collect(Collectors.toList());
      List<Feature> newLeftOn = constructorController.addEventTimeOn(primaryKey,
          baseQuery.getFeaturegroup(), baseQuery.getAs());
      renameJoinFeatures(newLeftOn);

      // equivalent copy, but needed to be able to set different alias
      List<Feature> newRightOn = constructorController.addEventTimeOn(primaryKey,
          baseQuery.getFeaturegroup(), baseQuery.getAs());
      renameJoinFeatures(newRightOn);

      List<SqlCondition> newJoinOperator = newLeftOn.stream().map(f -> SqlCondition.EQUALS)
          .collect(Collectors.toList());
      newLeftOn.forEach(f -> f.setPitFgAlias(FG_SUBQUERY + "0"));
      newRightOn.forEach(f -> f.setPitFgAlias(pitAlias));

      newJoins.add(new Join(null, null, newLeftOn, newRightOn, JoinType.INNER, null, newJoinOperator));
    }

    // sort features in last select
    if (isTrainingDataset) {
      finalSelectList = finalSelectList.stream()
          .sorted(Comparator.comparing(Feature::getIdx))
          .collect(Collectors.toList());
    }

    SqlNodeList selectList = new SqlNodeList(SqlParserPos.ZERO);
    for (Feature f : finalSelectList) {
      String featurePrefixed;
      if (!Strings.isNullOrEmpty(f.getPrefix())) {
        featurePrefixed = f.getPrefix() + f.getName();
      } else {
        featurePrefixed = f.getName();
      }
      selectList.add(
          SqlStdOperatorTable.AS.createCall(SqlParserPos.ZERO,
              new SqlIdentifier(
                  Arrays.asList("`" + f.getFgAlias(true) + "`", "`" + featurePrefixed + "`"), SqlParserPos.ZERO),
              new SqlIdentifier("`" + featurePrefixed + "`", SqlParserPos.ZERO)));
    }

    // if the base query does not have filters, but there are filters on the final query, add them to the body
    SqlNode filterNode = null;
    if (baseQuery.getFilter() == null && query.getFilter() != null) {
      filterNode = filterController.generateFilterLogicNode(query.getFilter(), false, true, true);
    }

    SqlSelect body = new SqlSelect(SqlParserPos.ZERO, null, selectList,
        buildWithJoin(newJoins, newJoins.size() - 1), filterNode, null, null, null, null, null, null, null);

    return new SqlWith(SqlParserPos.ZERO, selectAsses, body);
  }

  private void setFilterFeaturesPitAlias(List<Feature> filterFeatures,
                                         boolean pushdownOuterQueryFilter,
                                         Featuregroup featuregroup, String pitAlias) {
    if (!pushdownOuterQueryFilter && filterFeatures != null && !filterFeatures.isEmpty()) {
      filterFeatures.stream()
          .filter(f -> f.getFeatureGroup().getId().equals(featuregroup.getId()))
          .forEach(f -> f.setPitFgAlias(pitAlias));
    }
  }

  public boolean isTrainingDataset(List<Feature> selectList) {
    return selectList.stream().allMatch(f -> f.getIdx() != null && f.getFeatureGroup() != null);
  }

  public void renameJoinFeatures(List<Feature> joinFeatures) {
    joinFeatures.forEach(f -> {
      String prefixName = f.isPrimary() ? PK_JOIN_PREFIX + f.getName() : EVT_JOIN_PREFIX + f.getName();
      f.setName(prefixName);
    });
  }

  public List<Feature> dropIrrelevantSubqueryFeatures(Query query, Query rightQuery) {
    return query.getFeatures().stream()
        .filter(f -> f.getFeatureGroup() == query.getFeaturegroup()
            || f.getFeatureGroup() == rightQuery.getFeaturegroup())
        .collect(Collectors.toList());
  }

  private SqlNode buildWithJoin(List<Join> joins, int i) {
    if (i > 0) {
      return new SqlJoin(
          SqlParserPos.ZERO,
          buildWithJoin(joins, i - 1),
          SqlLiteral.createBoolean(false, SqlParserPos.ZERO),
          SqlLiteral.createSymbol(JoinType.INNER, SqlParserPos.ZERO),
          new SqlIdentifier(FG_SUBQUERY + i, SqlParserPos.ZERO),
          SqlLiteral.createSymbol(JoinConditionType.ON, SqlParserPos.ZERO),
          // set online=true because join conditions don't need to use defaults again
          joinController.getLeftRightCondition(FG_SUBQUERY + "0", FG_SUBQUERY + i,
              joins.get(i).getLeftOn(), joins.get(i).getRightOn(), joins.get(i).getJoinOperator(), true));
    } else {
      return new SqlIdentifier(FG_SUBQUERY + "0", SqlParserPos.ZERO);
    }
  }
}
