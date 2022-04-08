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
import io.hops.hopsworks.common.featurestore.query.filter.Filter;
import io.hops.hopsworks.common.featurestore.query.filter.FilterController;
import io.hops.hopsworks.common.featurestore.query.join.Join;
import io.hops.hopsworks.common.featurestore.query.join.JoinController;
import io.hops.hopsworks.persistence.entity.featurestore.featuregroup.Featuregroup;
import io.hops.hopsworks.persistence.entity.featurestore.trainingdataset.SqlCondition;
import org.apache.calcite.sql.JoinConditionType;
import org.apache.calcite.sql.JoinType;
import org.apache.calcite.sql.SqlCall;
import org.apache.calcite.sql.SqlFunction;
import org.apache.calcite.sql.SqlFunctionCategory;
import org.apache.calcite.sql.SqlIdentifier;
import org.apache.calcite.sql.SqlJoin;
import org.apache.calcite.sql.SqlKind;
import org.apache.calcite.sql.SqlLiteral;
import org.apache.calcite.sql.SqlNode;
import org.apache.calcite.sql.SqlNodeList;
import org.apache.calcite.sql.SqlOperator;
import org.apache.calcite.sql.SqlSelect;
import org.apache.calcite.sql.SqlWindow;
import org.apache.calcite.sql.SqlWith;
import org.apache.calcite.sql.fun.SqlStdOperatorTable;
import org.apache.calcite.sql.parser.SqlParserPos;
import org.apache.calcite.sql.type.OperandTypes;
import org.apache.calcite.sql.type.ReturnTypes;

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
    boolean optimizedPit) {
    List<SqlCall> subQueries = new ArrayList<>();
    
    // we always re-select all primary key columns of the "label group" in order to be able to perform final join
    List<Feature> additionalPkFeatures = query.getAvailableFeatures().stream().filter(Feature::isPrimary)
      .map(f ->
        new Feature(f.getName(), f.getFgAlias(), f.getType(), f.isPrimary(), f.getDefaultValue(), PK_JOIN_PREFIX))
      .collect(Collectors.toList());
    additionalPkFeatures.add(new Feature(query.getFeaturegroup().getEventTime(), query.getAs(), (String) null,
      null, EVT_JOIN_PREFIX));
    additionalPkFeatures.forEach(f -> f.setFeatureGroup(query.getFeaturegroup()));
    // baseQuery.getFeatures().addAll(additionalPkFeatures);
    
    // each join will result in one WITH subquery which joins a right feature group always with the same base feature
    // group
    for (Join join : query.getJoins()) {
      // add event time inequality join condition
      List<Feature> newLeftOn = addEventTimeOn(join.getLeftOn(), baseQuery.getFeaturegroup(), baseQuery.getAs());
      List<Feature> newRightOn = addEventTimeOn(join.getRightOn(), join.getRightQuery().getFeaturegroup(),
        join.getRightQuery().getAs());
      
      List<SqlCondition> newJoinOperator =
        addEventTimeCondition(join.getJoinOperator(), optimizedPit ? SqlCondition.PIT :
          SqlCondition.GREATER_THAN_OR_EQUAL);
      
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
      
      if (!optimizedPit) {
        // now add rank over window
        subQuery.getSelectList().add(rankOverAs(newLeftOn,
          new Feature(join.getRightQuery().getFeaturegroup().getEventTime(), join.getRightQuery().getAs(), false)));
      }
      subQueries.add(SqlStdOperatorTable.AS.createCall(SqlParserPos.ZERO, subQuery,
        new SqlIdentifier(HIVE_ALIAS_PLACEHOLDER, SqlParserPos.ZERO)));
      
      baseQuery.setFeatures(new ArrayList<>(query.getFeatures()));
    }
    
    // reset base query joins and features
    //baseQuery.setJoins(null);
    //baseQuery.setFeatures(query.getFeatures());
    
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
  
  public List<SqlSelect> wrapOptimizedSubQueries(List<SqlCall> sqlSelects) {
    List<SqlSelect> newSubQueries = new ArrayList<>();
    for (SqlCall select : sqlSelects) {
      SqlNodeList selectList = SqlNodeList.of(new SqlIdentifier(ALL_FEATURES, SqlParserPos.ZERO));
      newSubQueries.add(new SqlSelect(SqlParserPos.ZERO, null, selectList, select, null, null,
        null, null, null, null, null, null));
    }
    return newSubQueries;
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
  
  public SqlNode generateSQL(Query query, boolean isTrainingDataset, boolean optimizedPit) {
    // make a copy of base query to replace joins
    Query baseQuery = new Query(query.getFeatureStore(), query.getProject(), query.getFeaturegroup(), query.getAs(),
      new ArrayList<>(query.getFeatures()), query.getAvailableFeatures(), query.getHiveEngine(), query.getFilter());
    
    // collect left outer most features
    List<Feature> finalSelectList = constructorController.collectFeatures(baseQuery);
    
    // generate subqueries
    List<SqlCall> subQueries = generateSubQueries(baseQuery, query, isTrainingDataset, optimizedPit);
    // generate select statement for subqueries with WITH
    List<SqlSelect> withSelects = optimizedPit ? wrapOptimizedSubQueries(subQueries) : wrapSubQueries(subQueries);
    finalSelectList.forEach(f -> f.setPitFgAlias(FG_SUBQUERY + "0"));
    
    // list for "x0 as ..."
    SqlNodeList selectAsses = new SqlNodeList(SqlParserPos.ZERO);
    
    // joins for the body of the WITH statement, bringing together the final result
    List<Join> newJoins = new ArrayList<>();
    
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
        // alias only for the features corresponding to the feature group in the current join
        int finalI = i;
        finalSelectList.stream()
          .filter(f -> f.getFeatureGroup() == query.getJoins().get(finalI).getRightQuery().getFeaturegroup())
          .forEach(f -> f.setPitFgAlias(pitAlias));
      } else {
        List<Feature> features = constructorController.collectFeatures(query.getJoins().get(i).getRightQuery());
        features.forEach(f -> f.setPitFgAlias(pitAlias));
        finalSelectList.addAll(features);
      }
      // add event time inequality join condition
      List<Feature> primaryKey =
        baseQuery.getAvailableFeatures().stream().filter(Feature::isPrimary).collect(Collectors.toList());
      List<Feature> newLeftOn = addEventTimeOn(primaryKey, baseQuery.getFeaturegroup(), baseQuery.getAs());
      renameJoinFeatures(newLeftOn);
      
      // equivalent copy, but needed to be able to set different alias
      List<Feature> newRightOn = addEventTimeOn(primaryKey, baseQuery.getFeaturegroup(), baseQuery.getAs());
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
      selectList.add(new SqlIdentifier(
        Arrays.asList("`" + f.getFgAlias(true) + "`", "`" + featurePrefixed + "`"), SqlParserPos.ZERO));
    }
    SqlSelect body = new SqlSelect(SqlParserPos.ZERO, null, selectList,
      buildWithJoin(newJoins, newJoins.size() - 1), null, null, null, null, null, null, null, null);
    
    return new SqlWith(SqlParserPos.ZERO, selectAsses, body);
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
