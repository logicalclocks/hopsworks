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

package io.hops.hopsworks.common.featurestore.featuregroup.pit;

import io.hops.hopsworks.common.featurestore.featuregroup.FeaturegroupController;
import io.hops.hopsworks.common.featurestore.featuregroup.FeaturegroupDTO;
import io.hops.hopsworks.common.featurestore.featuregroup.cached.CachedFeaturegroupController;
import io.hops.hopsworks.common.featurestore.query.ConstructorController;
import io.hops.hopsworks.common.featurestore.query.Feature;
import io.hops.hopsworks.common.featurestore.query.Query;
import io.hops.hopsworks.common.featurestore.query.QueryDTO;
import io.hops.hopsworks.persistence.entity.featurestore.trainingdataset.SqlCondition;
import io.hops.hopsworks.common.featurestore.query.filter.Filter;
import io.hops.hopsworks.common.featurestore.query.filter.FilterController;
import io.hops.hopsworks.common.featurestore.query.filter.FilterLogic;
import io.hops.hopsworks.common.featurestore.query.join.Join;
import io.hops.hopsworks.common.featurestore.query.join.JoinController;
import io.hops.hopsworks.common.featurestore.query.join.JoinDTO;
import io.hops.hopsworks.common.featurestore.query.pit.PitJoinController;
import io.hops.hopsworks.persistence.entity.featurestore.Featurestore;
import io.hops.hopsworks.persistence.entity.featurestore.featuregroup.Featuregroup;
import io.hops.hopsworks.persistence.entity.featurestore.featuregroup.cached.CachedFeaturegroup;
import io.hops.hopsworks.persistence.entity.featurestore.featuregroup.cached.TimeTravelFormat;
import io.hops.hopsworks.persistence.entity.project.Project;
import org.apache.calcite.sql.JoinType;
import org.apache.calcite.sql.SqlCall;
import org.apache.calcite.sql.SqlDialect;
import org.apache.calcite.sql.SqlNode;
import org.apache.calcite.sql.SqlSelect;
import org.apache.calcite.sql.dialect.SparkSqlDialect;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class TestPitJoinController {
  
  private Featurestore fs;
  private CachedFeaturegroup cachedFeaturegroup;
  private Featuregroup fgLeft;
  private Featuregroup fgRight;
  private Featuregroup fgRight1;
  
  private PitJoinController pitJoinController;
  
  @Before
  public void setup() {
    System.setProperty("line.separator", "\n");

    fs = new Featurestore();
    fs.setProject(new Project("test_proj"));
    cachedFeaturegroup = new CachedFeaturegroup();
    cachedFeaturegroup.setTimeTravelFormat(TimeTravelFormat.NONE);
  
    fgLeft = new Featuregroup(1);
    fgLeft.setEventTime("ts");
    fgLeft.setName("fg0");
    fgLeft.setVersion(1);
    fgLeft.setCachedFeaturegroup(cachedFeaturegroup);
    fgLeft.setFeaturestore(fs);

    fgRight = new Featuregroup(2);
    fgRight.setEventTime("ts");
    fgRight.setName("fg1");
    fgRight.setVersion(1);
    fgRight.setCachedFeaturegroup(cachedFeaturegroup);
    fgRight.setFeaturestore(fs);
  
    fgRight1 = new Featuregroup(3);
    fgRight1.setEventTime("ts");
    fgRight1.setName("fg2");
    fgRight1.setVersion(1);
    fgRight1.setCachedFeaturegroup(cachedFeaturegroup);
    fgRight1.setFeaturestore(fs);
    
    FeaturegroupController featuregroupController = Mockito.mock(FeaturegroupController.class);
    CachedFeaturegroupController cachedFeaturegroupController = Mockito.mock(CachedFeaturegroupController.class);
    FilterController filterController = new FilterController(new ConstructorController());
    JoinController joinController = new JoinController(new ConstructorController());
  
    ConstructorController constructorController = new ConstructorController(
        featuregroupController, cachedFeaturegroupController, filterController, joinController);
    
    pitJoinController = new PitJoinController(constructorController, filterController, joinController);
  }
  
  @Test
  public void testIsPitEnabledTrueQueryDTO() {
    FeaturegroupDTO fg1 = new FeaturegroupDTO("ts");
    FeaturegroupDTO fg2 = new FeaturegroupDTO("ts");
    
    JoinDTO join1 = new JoinDTO(new QueryDTO(fg1, null), null, null);
    JoinDTO join2 = new JoinDTO(new QueryDTO(fg2, null), null, null);
  
    List<JoinDTO> joins = Arrays.asList(join1, join2);
    QueryDTO leftQuery = new QueryDTO(new FeaturegroupDTO("ts"), new ArrayList<>(), joins);
  
    Assert.assertEquals(true, pitJoinController.isPitEnabled(leftQuery));
  }
  
  @Test
  public void testIsPitEnabledFalseQueryDTO() {
    FeaturegroupDTO fg1 = new FeaturegroupDTO("ts");
    
    // not event time enabled fg, therefore should be pitEnabled should be false
    FeaturegroupDTO fg2 = new FeaturegroupDTO();
  
    JoinDTO join1 = new JoinDTO(new QueryDTO(fg1, null), null, null);
    JoinDTO join2 = new JoinDTO(new QueryDTO(fg2, null), null, null);
  
    List<JoinDTO> joins = Arrays.asList(join1, join2);
    QueryDTO leftQuery = new QueryDTO(new FeaturegroupDTO("ts"), new ArrayList<>(), joins);
  
    Assert.assertEquals(false, pitJoinController.isPitEnabled(leftQuery));
  }
  
  @Test
  public void testIsPitEnabledTrueQuery() {
    Featuregroup fg1 = new Featuregroup(1);
    fg1.setEventTime("ts");
    Featuregroup fg2 = new Featuregroup(2);
    fg2.setEventTime("ts");
  
    Query right1 = new Query("fs", fg1);
    Query right2 = new Query("fs", fg2);
    
    Query leftQuery = new Query("fs", fg1);
  
    Join join1 = new Join(leftQuery, right1, null, null, null, null, null);
    Join join2 = new Join(leftQuery, right2, null, null, null, null, null);
    List<Join> joins = Arrays.asList(join1, join2);
    leftQuery.setJoins(joins);
    
    Assert.assertEquals(true, pitJoinController.isPitEnabled(leftQuery));
  }
  
  @Test
  public void testIsPitEnabledFalseQuery() {
    Featuregroup fg1 = new Featuregroup(1);
    fg1.setEventTime("ts");
    Featuregroup fg2 = new Featuregroup(2);

    Query right1 = new Query("fs", fg1);
    Query right2 = new Query("fs", fg2);
  
    Query leftQuery = new Query("fs", fg1);
  
    Join join1 = new Join(leftQuery, right1, null, null, null, null, null);
    Join join2 = new Join(leftQuery, right2, null, null, null, null, null);
    List<Join> joins = Arrays.asList(join1, join2);
    leftQuery.setJoins(joins);
  
    Assert.assertEquals(false, pitJoinController.isPitEnabled(leftQuery));
  }
  
  @Test
  public void testDropIrrelevantSubqueryFeatures() {
    Featuregroup fg1 = new Featuregroup(1);
    Featuregroup fg2 = new Featuregroup(2);
    Featuregroup fg3 = new Featuregroup(3);
  
    Query leftQuery = new Query("fs", fg1);
    List<Feature> selectedFeatures = Arrays.asList(new Feature(fg1), new Feature(fg2), new Feature(fg1),
      new Feature(fg3));
    leftQuery.setFeatures(selectedFeatures);
    
    Query rightQuery = new Query("fs", fg3);
    
    List<Feature> filteredSelect = pitJoinController.dropIrrelevantSubqueryFeatures(leftQuery, rightQuery);
    Assert.assertEquals(3, filteredSelect.size());
  }
  
  @Test
  public void testRankOverAs() {
    List<Feature> partitionFeatures = new ArrayList<>();
    partitionFeatures.add(new Feature("pk1", "fg1"));
    partitionFeatures.add(new Feature("pk2", "fg1"));
    partitionFeatures.add(new Feature("ts", "fg1"));
  
    Feature orderByFeature = new Feature("ts", "fg2");
    
    SqlNode rank = pitJoinController.rankOverAs(partitionFeatures, orderByFeature);
    String expected = "RANK() OVER (PARTITION BY `fg1`.`pk1`, `fg1`.`pk2`, `fg1`.`ts` ORDER BY `fg2`.`ts` DESC) pit_rank_hopsworks";
    Assert.assertEquals(expected, rank.toSqlString(new SparkSqlDialect(SqlDialect.EMPTY_CONTEXT)).getSql());
  }
  
  @Test
  public void testGenerateSubQueriesSingle() {
    List<Feature> leftFeatures = new ArrayList<>();
    leftFeatures.add(new Feature("pk1", "fg0", fgLeft, true));
    leftFeatures.add(new Feature("pk2", "fg0", fgLeft));
    leftFeatures.add(new Feature("ts", "fg0", fgLeft));
    leftFeatures.add(new Feature("label", "fg0", fgLeft));
    
    List<Feature> rightFeatures = new ArrayList<>();
    rightFeatures.add(new Feature("pk1", "fg1", fgRight));
    rightFeatures.add(new Feature("pk2", "fg1", fgRight));
    rightFeatures.add(new Feature("ts", "fg1", fgRight));
    rightFeatures.add(new Feature("ft1", "fg1", fgRight));
  
    List<Feature> leftOn = Arrays.asList(new Feature("pk1", "fg0", fgLeft), new Feature("pk2", "fg0", fgLeft));
    List<Feature> rightOn = Arrays.asList(new Feature("pk1", "fg1", fgRight), new Feature("pk2", "fg1", fgRight));
    List<SqlCondition> joinOperator = Arrays.asList(SqlCondition.EQUALS, SqlCondition.EQUALS);
  
    Query query = new Query("fs", "project", fgLeft, "fg0", leftFeatures, leftFeatures, false, null);
    Query right = new Query("fs", "project", fgRight, "fg1", rightFeatures, rightFeatures, false, null);
    
    Join join = new Join(query, right, leftOn, rightOn, JoinType.INNER, null, joinOperator);
    query.setJoins(Collections.singletonList(join));
  
    Query baseQuery = new Query(query.getFeatureStore(), query.getProject(), query.getFeaturegroup(), query.getAs(),
     query.getFeatures(), query.getAvailableFeatures(), query.getHiveEngine(), query.getFilter());
    
    List<SqlCall> result = pitJoinController.generateSubQueries(baseQuery, query, false);
    String expected = "(SELECT `fg0`.`pk1` `pk1`, `fg0`.`pk2` `pk2`, `fg0`.`ts` `ts`, `fg0`.`label` `label`, `fg0`.`pk1` `join_pk_pk1`, `fg0`.`ts` `join_evt_ts`, `fg1`.`pk1` `pk1`, `fg1`.`pk2` `pk2`, `fg1`.`ts` `ts`, `fg1`.`ft1` `ft1`, " +
                              "RANK() OVER (PARTITION BY `fg0`.`pk1`, `fg0`.`pk2`, `fg0`.`ts` ORDER BY `fg1`.`ts` DESC) pit_rank_hopsworks\n" +
                        "FROM `fs`.`fg0_1` `fg0`\n" +
                  "INNER JOIN `fs`.`fg1_1` `fg1` ON `fg0`.`pk1` = `fg1`.`pk1` AND `fg0`.`pk2` = `fg1`.`pk2` AND `fg0`.`ts` >= `fg1`.`ts`) NA";
    Assert.assertEquals(1, result.size());
    Assert.assertEquals(expected, result.get(0).toSqlString(new SparkSqlDialect(SqlDialect.EMPTY_CONTEXT)).getSql());
  }
  
  @Test
  public void testWrapSubQueries() {
    List<Feature> leftFeatures = new ArrayList<>();
    leftFeatures.add(new Feature("pk1", "fg0", fgLeft, true));
    leftFeatures.add(new Feature("pk2", "fg0", fgLeft));
    leftFeatures.add(new Feature("ts", "fg0", fgLeft));
    leftFeatures.add(new Feature("label", "fg0", fgLeft));

    List<Feature> rightFeatures = new ArrayList<>();
    rightFeatures.add(new Feature("pk1", "fg1", fgRight));
    rightFeatures.add(new Feature("pk2", "fg1", fgRight));
    rightFeatures.add(new Feature("ts", "fg1", fgRight));
    rightFeatures.add(new Feature("ft1", "fg1", fgRight));

    List<Feature> leftOn = Arrays.asList(new Feature("pk1", "fg0", fgLeft), new Feature("pk2", "fg0", fgLeft));
    List<Feature> rightOn = Arrays.asList(new Feature("pk1", "fg1", fgRight), new Feature("pk2", "fg1", fgRight));
    List<SqlCondition> joinOperator = Arrays.asList(SqlCondition.EQUALS, SqlCondition.EQUALS);

    Query query = new Query("fs", "project", fgLeft, "fg0", leftFeatures, leftFeatures, false, null);
    Query right = new Query("fs", "project", fgRight, "fg1", rightFeatures, rightFeatures, false, null);

    Join join = new Join(query, right, leftOn, rightOn, JoinType.INNER, null, joinOperator);
    query.setJoins(Collections.singletonList(join));

    Query baseQuery = new Query(query.getFeatureStore(), query.getProject(), query.getFeaturegroup(), query.getAs(),
      query.getFeatures(), query.getAvailableFeatures(), query.getHiveEngine(), query.getFilter());

    List<SqlSelect> result = pitJoinController.wrapSubQueries(pitJoinController.generateSubQueries(baseQuery, query,
      false));
    String expected = "SELECT *\n" +
                        "FROM (SELECT `fg0`.`pk1` `pk1`, `fg0`.`pk2` `pk2`, `fg0`.`ts` `ts`, `fg0`.`label` `label`, `fg0`.`pk1` `join_pk_pk1`, `fg0`.`ts` `join_evt_ts`, `fg1`.`pk1` `pk1`, `fg1`.`pk2` `pk2`, `fg1`.`ts` `ts`, `fg1`.`ft1` `ft1`, " +
                                       "RANK() OVER (PARTITION BY `fg0`.`pk1`, `fg0`.`pk2`, `fg0`.`ts` ORDER BY `fg1`.`ts` DESC) pit_rank_hopsworks\n" +
                                "FROM `fs`.`fg0_1` `fg0`\n" +
                          "INNER JOIN `fs`.`fg1_1` `fg1` ON `fg0`.`pk1` = `fg1`.`pk1` AND `fg0`.`pk2` = `fg1`.`pk2` AND `fg0`.`ts` >= `fg1`.`ts`) NA\n" +
                       "WHERE `pit_rank_hopsworks` = 1";
    Assert.assertEquals(1, result.size());
    Assert.assertEquals(expected, result.get(0).toSqlString(new SparkSqlDialect(SqlDialect.EMPTY_CONTEXT)).getSql());
  }
  
  @Test
  public void testGenerateSql() {
    List<Feature> leftFeatures = new ArrayList<>();
    leftFeatures.add(new Feature("pk1", "fg0", fgLeft, true));
    leftFeatures.add(new Feature("pk2", "fg0", fgLeft));
    leftFeatures.add(new Feature("ts", "fg0", fgLeft));
    leftFeatures.add(new Feature("label", "fg0", fgLeft));

    List<Feature> rightFeatures = new ArrayList<>();
    rightFeatures.add(new Feature("pk1", "fg1", fgRight));
    rightFeatures.add(new Feature("pk2", "fg1", fgRight));
    rightFeatures.add(new Feature("ts", "fg1", fgRight));
    rightFeatures.add(new Feature("ft1", "fg1", fgRight));

    List<Feature> rightFeatures1 = new ArrayList<>();
    rightFeatures1.add(new Feature("pk1", "fg2", fgRight1));
    rightFeatures1.add(new Feature("ts", "fg2", fgRight1));
    rightFeatures1.add(new Feature("ft1", "fg2", fgRight1));
  
    List<Feature> leftOn = Arrays.asList(new Feature("pk1", "fg0", fgLeft), new Feature("pk2", "fg0", fgLeft));
    List<Feature> rightOn = Arrays.asList(new Feature("pk1", "fg1", fgRight), new Feature("pk2", "fg1", fgRight));
  
    // join on different pks
    List<Feature> leftOn1 = Collections.singletonList(new Feature("pk1", "fg0", fgLeft));
    List<Feature> rightOn1 = Collections.singletonList(new Feature("pk1", "fg2", fgRight1));

    List<SqlCondition> joinOperator = Arrays.asList(SqlCondition.EQUALS, SqlCondition.EQUALS);
    List<SqlCondition> joinOperator1 = Collections.singletonList(SqlCondition.EQUALS);
    
    Query query = new Query("fs", "project", fgLeft, "fg0", leftFeatures, leftFeatures, false, null);
    Query right = new Query("fs", "project", fgRight, "fg1", rightFeatures, rightFeatures, false, null);
    Query right1 = new Query("fs", "project", fgRight1, "fg2", rightFeatures1, rightFeatures1, false, null);
  
    Join join = new Join(query, right, leftOn, rightOn, JoinType.INNER, null, joinOperator);
    Join join1 = new Join(query, right1, leftOn1, rightOn1, JoinType.INNER, null, joinOperator1);
  
    query.setJoins(Arrays.asList(join, join1));
    
    String result =
      pitJoinController.generateSQL(query, false).toSqlString(new SparkSqlDialect(SqlDialect.EMPTY_CONTEXT)).getSql();
    String expected = "WITH right_fg0 AS (SELECT *\n" +
                                        "FROM (SELECT `fg0`.`pk1` `pk1`, `fg0`.`pk2` `pk2`, `fg0`.`ts` `ts`, `fg0`.`label` `label`, `fg0`.`pk1` `join_pk_pk1`, `fg0`.`ts` `join_evt_ts`, `fg1`.`pk1` `pk1`, `fg1`.`pk2` `pk2`, `fg1`.`ts` `ts`, `fg1`.`ft1` `ft1`, " +
                                                      "RANK() OVER (PARTITION BY `fg0`.`pk1`, `fg0`.`pk2`, `fg0`.`ts` ORDER BY `fg1`.`ts` DESC) pit_rank_hopsworks\n" +
                                                "FROM `fs`.`fg0_1` `fg0`\n" +
                                          "INNER JOIN `fs`.`fg1_1` `fg1` ON `fg0`.`pk1` = `fg1`.`pk1` AND `fg0`.`pk2` = `fg1`.`pk2` AND `fg0`.`ts` >= `fg1`.`ts`) NA\n" +
                                       "WHERE `pit_rank_hopsworks` = 1), " +
                           "right_fg1 AS (SELECT *\n" +
                                        "FROM (SELECT `fg0`.`pk1` `pk1`, `fg0`.`pk2` `pk2`, `fg0`.`ts` `ts`, `fg0`.`label` `label`, `fg0`.`pk1` `join_pk_pk1`, `fg0`.`ts` `join_evt_ts`, `fg2`.`pk1` `pk1`, `fg2`.`ts` `ts`, `fg2`.`ft1` `ft1`, " +
                                                      "RANK() OVER (PARTITION BY `fg0`.`pk1`, `fg0`.`ts` ORDER BY `fg2`.`ts` DESC) pit_rank_hopsworks\n" +
                                                "FROM `fs`.`fg0_1` `fg0`\n" +
                                          "INNER JOIN `fs`.`fg2_1` `fg2` ON `fg0`.`pk1` = `fg2`.`pk1` AND `fg0`.`ts` >= `fg2`.`ts`) NA\n" +
                                       "WHERE `pit_rank_hopsworks` = 1) (" +
                      "SELECT `right_fg0`.`pk1` `pk1`, `right_fg0`.`pk2` `pk2`, `right_fg0`.`ts` `ts`, `right_fg0`.`label` `label`, `right_fg0`.`pk1` `pk1`, `right_fg0`.`pk2` `pk2`, `right_fg0`.`ts` `ts`, `right_fg0`.`ft1` `ft1`, `right_fg1`.`pk1` `pk1`, `right_fg1`.`ts` `ts`, `right_fg1`.`ft1` `ft1`\n" +
                        "FROM right_fg0\n" +
                  "INNER JOIN right_fg1 ON `right_fg0`.`join_pk_pk1` = `right_fg1`.`join_pk_pk1` AND `right_fg0`.`join_evt_ts` = `right_fg1`.`join_evt_ts`)";
    Assert.assertEquals(expected, result);
  }
  
  @Test
  public void testGenerateSql_hiveEngine() {
    List<Feature> leftFeatures = new ArrayList<>();
    leftFeatures.add(new Feature("pk1", "fg0", fgLeft, true));
    leftFeatures.add(new Feature("pk2", "fg0", fgLeft));
    leftFeatures.add(new Feature("ts", "fg0", fgLeft));
    leftFeatures.add(new Feature("label", "fg0", fgLeft));
    
    List<Feature> rightFeatures = new ArrayList<>();
    rightFeatures.add(new Feature("pk1", "fg1", fgRight));
    rightFeatures.add(new Feature("pk2", "fg1", fgRight));
    rightFeatures.add(new Feature("ts", "fg1", fgRight));
    rightFeatures.add(new Feature("ft1", "fg1", fgRight));
    
    List<Feature> rightFeatures1 = new ArrayList<>();
    rightFeatures1.add(new Feature("pk1", "fg2", fgRight1));
    rightFeatures1.add(new Feature("ts", "fg2", fgRight1));
    rightFeatures1.add(new Feature("ft1", "fg2", fgRight1));
    
    List<Feature> leftOn = Arrays.asList(new Feature("pk1", "fg0", fgLeft), new Feature("pk2", "fg0", fgLeft));
    List<Feature> rightOn = Arrays.asList(new Feature("pk1", "fg1", fgRight), new Feature("pk2", "fg1", fgRight));
    
    // join on different pks
    List<Feature> leftOn1 = Collections.singletonList(new Feature("pk1", "fg0", fgLeft));
    List<Feature> rightOn1 = Collections.singletonList(new Feature("pk1", "fg2", fgRight1));
    
    List<SqlCondition> joinOperator = Arrays.asList(SqlCondition.EQUALS, SqlCondition.EQUALS);
    List<SqlCondition> joinOperator1 = Collections.singletonList(SqlCondition.EQUALS);
    
    Query query = new Query("fs", "project", fgLeft, "fg0", leftFeatures, leftFeatures, true, null);
    Query right = new Query("fs", "project", fgRight, "fg1", rightFeatures, rightFeatures, true, null);
    Query right1 = new Query("fs", "project", fgRight1, "fg2", rightFeatures1, rightFeatures1, true, null);
    
    Join join = new Join(query, right, leftOn, rightOn, JoinType.INNER, null, joinOperator);
    Join join1 = new Join(query, right1, leftOn1, rightOn1, JoinType.INNER, null, joinOperator1);
    
    query.setJoins(Arrays.asList(join, join1));
    
    String result =
      pitJoinController.generateSQL(query, false).toSqlString(new SparkSqlDialect(SqlDialect.EMPTY_CONTEXT)).getSql();
    String expected = "WITH right_fg0 AS (SELECT *\n" +
      "FROM (SELECT `fg0`.`pk1` `pk1`, `fg0`.`pk2` `pk2`, `fg0`.`ts` `ts`, `fg0`.`label` `label`, `fg0`.`pk1` `join_pk_pk1`, `fg0`.`ts` `join_evt_ts`, `fg1`.`pk1` `pk1`, `fg1`.`pk2` `pk2`, `fg1`.`ts` `ts`, `fg1`.`ft1` `ft1`, " +
      "RANK() OVER (PARTITION BY `fg0`.`pk1`, `fg0`.`pk2`, `fg0`.`ts` ORDER BY `fg1`.`ts` DESC) pit_rank_hopsworks\n" +
      "FROM `fs`.`fg0_1` `fg0`\n" +
      "INNER JOIN `fs`.`fg1_1` `fg1` ON `fg0`.`pk1` = `fg1`.`pk1` AND `fg0`.`pk2` = `fg1`.`pk2` AND `fg0`.`ts` >= `fg1`.`ts`) NA\n" +
      "WHERE `pit_rank_hopsworks` = 1), " +
      "right_fg1 AS (SELECT *\n" +
      "FROM (SELECT `fg0`.`pk1` `pk1`, `fg0`.`pk2` `pk2`, `fg0`.`ts` `ts`, `fg0`.`label` `label`, `fg0`.`pk1` `join_pk_pk1`, `fg0`.`ts` `join_evt_ts`, `fg2`.`pk1` `pk1`, `fg2`.`ts` `ts`, `fg2`.`ft1` `ft1`, " +
      "RANK() OVER (PARTITION BY `fg0`.`pk1`, `fg0`.`ts` ORDER BY `fg2`.`ts` DESC) pit_rank_hopsworks\n" +
      "FROM `fs`.`fg0_1` `fg0`\n" +
      "INNER JOIN `fs`.`fg2_1` `fg2` ON `fg0`.`pk1` = `fg2`.`pk1` AND `fg0`.`ts` >= `fg2`.`ts`) NA\n" +
      "WHERE `pit_rank_hopsworks` = 1) (" +
      "SELECT `right_fg0`.`pk1` `pk1`, `right_fg0`.`pk2` `pk2`, `right_fg0`.`ts` `ts`, `right_fg0`.`label` `label`, `right_fg0`.`pk1` `pk1`, `right_fg0`.`pk2` `pk2`, `right_fg0`.`ts` `ts`, `right_fg0`.`ft1` `ft1`, `right_fg1`.`pk1` `pk1`, `right_fg1`.`ts` `ts`, `right_fg1`.`ft1` `ft1`\n" +
      "FROM right_fg0\n" +
      "INNER JOIN right_fg1 ON `right_fg0`.`join_pk_pk1` = `right_fg1`.`join_pk_pk1` AND `right_fg0`.`join_evt_ts` = `right_fg1`.`join_evt_ts`)";
    Assert.assertEquals(expected, result);
  }

  @Test
  public void testGenerateSqlTrainingDataset() {
    List<Feature> leftFeatures = new ArrayList<>();
    leftFeatures.add(new Feature("pk1", "fg0", fgLeft, true,1));
    leftFeatures.add(new Feature("pk2", "fg0", fgLeft, false,2));
    leftFeatures.add(new Feature("ts", "fg0", fgLeft, false,3));
    leftFeatures.add(new Feature("label", "fg0", fgLeft, false,4));

    // add all features to left query as we do for training datasets
    leftFeatures.add(new Feature("ft1", "fg1", fgRight, false,5));
    leftFeatures.add(new Feature("ft2", "fg2", fgRight1, false,6));
  
    List<Feature> leftOn = Arrays.asList(new Feature("pk1", "fg0", fgLeft), new Feature("pk2", "fg0", fgLeft));
    List<Feature> rightOn = Arrays.asList(new Feature("pk1", "fg1", fgRight), new Feature("pk2", "fg1", fgRight));
  
    // join on different pks
    List<Feature> leftOn1 = Collections.singletonList(new Feature("pk1", "fg0", fgLeft));
    List<Feature> rightOn1 = Collections.singletonList(new Feature("pk1", "fg2", fgRight1));
  
    List<SqlCondition> joinOperator = Arrays.asList(SqlCondition.EQUALS, SqlCondition.EQUALS);
    List<SqlCondition> joinOperator1 = Collections.singletonList(SqlCondition.EQUALS);
    
    Query query = new Query("fs", "project", fgLeft, "fg0", leftFeatures, leftFeatures, false, null);
    Query right = new Query("fs", "project", fgRight, "fg1", new ArrayList<>(), new ArrayList<>(), false, null);
    Query right1 = new Query("fs", "project", fgRight1, "fg2", new ArrayList<>(), new ArrayList<>(), false, null);
  
    Join join = new Join(query, right, leftOn, rightOn, JoinType.INNER, null, joinOperator);
    Join join1 = new Join(query, right1, leftOn1, rightOn1, JoinType.INNER, null, joinOperator1);
  
    query.setJoins(Arrays.asList(join, join1));
  
    String result =
      pitJoinController.generateSQL(query, true).toSqlString(new SparkSqlDialect(SqlDialect.EMPTY_CONTEXT)).getSql();
    String expected = "WITH right_fg0 AS (SELECT *\n" +
                                        "FROM (SELECT `fg0`.`pk1` `pk1`, `fg0`.`pk2` `pk2`, `fg0`.`ts` `ts`, `fg0`" +
      ".`label` `label`, " +
      "`fg1`" +
      ".`ft1` `ft1`, `fg0`.`pk1` `join_pk_pk1`, `fg0`.`ts` `join_evt_ts`, " +
                                                      "RANK() OVER (PARTITION BY `fg0`.`pk1`, `fg0`.`pk2`, `fg0`.`ts` ORDER BY `fg1`.`ts` DESC) pit_rank_hopsworks\n" +
                                                "FROM `fs`.`fg0_1` `fg0`\n" +
                                          "INNER JOIN `fs`.`fg1_1` `fg1` ON `fg0`.`pk1` = `fg1`.`pk1` AND `fg0`.`pk2` = `fg1`.`pk2` AND `fg0`.`ts` >= `fg1`.`ts`) NA\n" +
                                       "WHERE `pit_rank_hopsworks` = 1), " +
                           "right_fg1 AS (SELECT *\n" +
                                        "FROM (SELECT `fg0`.`pk1` `pk1`, `fg0`.`pk2` `pk2`, `fg0`.`ts` `ts`, `fg0`.`label` `label`, `fg2`.`ft2` `ft2`, `fg0`.`pk1` `join_pk_pk1`, `fg0`.`ts` `join_evt_ts`, " +
                                                      "RANK() OVER (PARTITION BY `fg0`.`pk1`, `fg0`.`ts` ORDER BY `fg2`.`ts` DESC) pit_rank_hopsworks\n" +
                                                "FROM `fs`.`fg0_1` `fg0`\n" +
                                          "INNER JOIN `fs`.`fg2_1` `fg2` ON `fg0`.`pk1` = `fg2`.`pk1` AND `fg0`.`ts` >= `fg2`.`ts`) NA\n" +
                                       "WHERE `pit_rank_hopsworks` = 1) (" +
                      "SELECT `right_fg0`.`pk1` `pk1`, `right_fg0`.`pk2` `pk2`, `right_fg0`.`ts` `ts`, `right_fg0`.`label` `label`, `right_fg0`.`ft1` `ft1`, `right_fg1`.`ft2` `ft2`\n" +
                        "FROM right_fg0\n" +
                  "INNER JOIN right_fg1 ON `right_fg0`.`join_pk_pk1` = `right_fg1`.`join_pk_pk1` AND `right_fg0`.`join_evt_ts` = `right_fg1`.`join_evt_ts`)";
    Assert.assertEquals(expected, result);
  }
  
  @Test
  public void testGenerateSqlTrainingDatasetWrongFeatureOrder() {
    List<Feature> leftFeatures = new ArrayList<>();
    leftFeatures.add(new Feature("pk1", "fg0", fgLeft, true,1));
    leftFeatures.add(new Feature("pk2", "fg0", fgLeft, false,2));
    leftFeatures.add(new Feature("ts", "fg0", fgLeft, false,3));
    leftFeatures.add(new Feature("label", "fg0", fgLeft, false,4));
  
    // note wrong order
    leftFeatures.add(new Feature("ft1", "fg1", fgRight, false,6));
    leftFeatures.add(new Feature("ft2", "fg2", fgRight1, false,5));
  
    List<Feature> leftOn = Arrays.asList(new Feature("pk1", "fg0", fgLeft), new Feature("pk2", "fg0", fgLeft));
    List<Feature> rightOn = Arrays.asList(new Feature("pk1", "fg1", fgRight), new Feature("pk2", "fg1", fgRight));
  
    // join on different pks
    List<Feature> leftOn1 = Collections.singletonList(new Feature("pk1", "fg0", fgLeft));
    List<Feature> rightOn1 = Collections.singletonList(new Feature("pk1", "fg2", fgRight1));
  
    List<SqlCondition> joinOperator = Arrays.asList(SqlCondition.EQUALS, SqlCondition.EQUALS);
    List<SqlCondition> joinOperator1 = Collections.singletonList(SqlCondition.EQUALS);
  
    Query query = new Query("fs", "project", fgLeft, "fg0", leftFeatures, leftFeatures, false, null);
    Query right = new Query("fs", "project", fgRight, "fg1", new ArrayList<>(), new ArrayList<>(), false, null);
    Query right1 = new Query("fs", "project", fgRight1, "fg2", new ArrayList<>(), new ArrayList<>(), false, null);
  
    Join join = new Join(query, right, leftOn, rightOn, JoinType.INNER, null, joinOperator);
    Join join1 = new Join(query, right1, leftOn1, rightOn1, JoinType.INNER, null, joinOperator1);
  
    query.setJoins(Arrays.asList(join, join1));
  
    String result =
      pitJoinController.generateSQL(query, true).toSqlString(new SparkSqlDialect(SqlDialect.EMPTY_CONTEXT)).getSql();
    String expected = "WITH right_fg0 AS (SELECT *\n" +
                                        "FROM (SELECT `fg0`.`pk1` `pk1`, `fg0`.`pk2` `pk2`, `fg0`.`ts` `ts`, `fg0`.`label` `label`, `fg1`.`ft1` `ft1`, `fg0`.`pk1` `join_pk_pk1`, `fg0`.`ts` `join_evt_ts`, " +
                                                      "RANK() OVER (PARTITION BY `fg0`.`pk1`, `fg0`.`pk2`, `fg0`.`ts` ORDER BY `fg1`.`ts` DESC) pit_rank_hopsworks\n" +
                                                "FROM `fs`.`fg0_1` `fg0`\n" +
                                          "INNER JOIN `fs`.`fg1_1` `fg1` ON `fg0`.`pk1` = `fg1`.`pk1` AND `fg0`.`pk2` = `fg1`.`pk2` AND `fg0`.`ts` >= `fg1`.`ts`) NA\n" +
                                       "WHERE `pit_rank_hopsworks` = 1), " +
                           "right_fg1 AS (SELECT *\n" +
                                        "FROM (SELECT `fg0`.`pk1` `pk1`, `fg0`.`pk2` `pk2`, `fg0`.`ts` `ts`, `fg0`.`label` `label`, `fg2`.`ft2` `ft2`, `fg0`.`pk1` `join_pk_pk1`, `fg0`.`ts` `join_evt_ts`, " +
                                                      "RANK() OVER (PARTITION BY `fg0`.`pk1`, `fg0`.`ts` ORDER BY `fg2`.`ts` DESC) pit_rank_hopsworks\n" +
                                                "FROM `fs`.`fg0_1` `fg0`\n" +
                                          "INNER JOIN `fs`.`fg2_1` `fg2` ON `fg0`.`pk1` = `fg2`.`pk1` AND `fg0`.`ts` >= `fg2`.`ts`) NA\n" +
                                       "WHERE `pit_rank_hopsworks` = 1) (" +
                      "SELECT `right_fg0`.`pk1` `pk1`, `right_fg0`.`pk2` `pk2`, `right_fg0`.`ts` `ts`, `right_fg0`.`label` `label`, `right_fg1`.`ft2` `ft2`, `right_fg0`.`ft1` `ft1`\n" +
                        "FROM right_fg0\n" +
                  "INNER JOIN right_fg1 ON `right_fg0`.`join_pk_pk1` = `right_fg1`.`join_pk_pk1` AND `right_fg0`.`join_evt_ts` = `right_fg1`.`join_evt_ts`)";
    Assert.assertEquals(expected, result);
  }

  @Test
  public void testGenerateSqlPrefix() {
    List<Feature> leftFeatures = new ArrayList<>();
    leftFeatures.add(new Feature("pk1", "fg0", fgLeft, true));
    leftFeatures.add(new Feature("pk2", "fg0", fgLeft));
    leftFeatures.add(new Feature("ts", "fg0", fgLeft));
    leftFeatures.add(new Feature("label", "fg0", fgLeft));
  
    List<Feature> rightFeatures = new ArrayList<>();
    rightFeatures.add(new Feature("pk1", "fg1", fgRight));
    rightFeatures.add(new Feature("pk2", "fg1", fgRight));
    rightFeatures.add(new Feature("ts", "fg1", fgRight));
    rightFeatures.add(new Feature("ft1", "fg1", fgRight));
  
    List<Feature> rightFeatures1 = new ArrayList<>();
    rightFeatures1.add(new Feature("pk1", "fg2", fgRight1));
    rightFeatures1.add(new Feature("ts", "fg2", fgRight1));
    rightFeatures1.add(new Feature("ft1", "fg2", fgRight1));
  
    List<Feature> leftOn = Arrays.asList(new Feature("pk1", "fg0", fgLeft), new Feature("pk2", "fg0", fgLeft));
    List<Feature> rightOn = Arrays.asList(new Feature("pk1", "fg1", fgRight), new Feature("pk2", "fg1", fgRight));
  
    // join on different pks
    List<Feature> leftOn1 = Collections.singletonList(new Feature("pk1", "fg0", fgLeft));
    List<Feature> rightOn1 = Collections.singletonList(new Feature("pk1", "fg2", fgRight1));
  
    List<SqlCondition> joinOperator = Arrays.asList(SqlCondition.EQUALS, SqlCondition.EQUALS);
    List<SqlCondition> joinOperator1 = Collections.singletonList(SqlCondition.EQUALS);
  
    Query query = new Query("fs", "project", fgLeft, "fg0", leftFeatures, leftFeatures, false, null);
    Query right = new Query("fs", "project", fgRight, "fg1", rightFeatures, rightFeatures, false, null);
    Query right1 = new Query("fs", "project", fgRight, "fg2", rightFeatures1, rightFeatures1, false, null);
  
    Join join = new Join(query, right, leftOn, rightOn, JoinType.INNER, null, joinOperator);
    Join join1 = new Join(query, right1, leftOn1, rightOn1, JoinType.INNER, "R_", joinOperator1);
  
    query.setJoins(Arrays.asList(join, join1));
  
    String result =
      pitJoinController.generateSQL(query, false).toSqlString(new SparkSqlDialect(SqlDialect.EMPTY_CONTEXT)).getSql();
    String expected = "WITH right_fg0 AS (SELECT *\n" +
                                        "FROM (SELECT `fg0`.`pk1` `pk1`, `fg0`.`pk2` `pk2`, `fg0`.`ts` `ts`, `fg0`.`label` `label`, `fg0`.`pk1` `join_pk_pk1`, `fg0`.`ts` `join_evt_ts`, `fg1`.`pk1` `pk1`, `fg1`.`pk2` `pk2`, `fg1`.`ts` `ts`, `fg1`.`ft1` `ft1`, " +
                                                      "RANK() OVER (PARTITION BY `fg0`.`pk1`, `fg0`.`pk2`, `fg0`.`ts` ORDER BY `fg1`.`ts` DESC) pit_rank_hopsworks\n" +
                                                "FROM `fs`.`fg0_1` `fg0`\n" +
                                          "INNER JOIN `fs`.`fg1_1` `fg1` ON `fg0`.`pk1` = `fg1`.`pk1` AND `fg0`.`pk2` = `fg1`.`pk2` AND `fg0`.`ts` >= `fg1`.`ts`) NA\n" +
                                       "WHERE `pit_rank_hopsworks` = 1), " +
                           "right_fg1 AS (SELECT *\n" +
                                        "FROM (SELECT `fg0`.`pk1` `pk1`, `fg0`.`pk2` `pk2`, `fg0`.`ts` `ts`, `fg0`.`label` `label`, `fg0`.`pk1` `join_pk_pk1`, `fg0`.`ts` `join_evt_ts`, `fg2`.`pk1` `R_pk1`, `fg2`.`ts` `R_ts`, `fg2`.`ft1` `R_ft1`, " +
                                                      "RANK() OVER (PARTITION BY `fg0`.`pk1`, `fg0`.`ts` ORDER BY `fg2`.`ts` DESC) pit_rank_hopsworks\n" +
                                                "FROM `fs`.`fg0_1` `fg0`\n" +
                                          "INNER JOIN `fs`.`fg1_1` `fg2` ON `fg0`.`pk1` = `fg2`.`pk1` AND `fg0`.`ts` >= `fg2`.`ts`) NA\n" +
                                       "WHERE `pit_rank_hopsworks` = 1) (" +
                      "SELECT `right_fg0`.`pk1` `pk1`, `right_fg0`.`pk2` `pk2`, `right_fg0`.`ts` `ts`, `right_fg0`.`label` `label`, `right_fg0`.`pk1` `pk1`, `right_fg0`.`pk2` `pk2`, `right_fg0`.`ts` `ts`, `right_fg0`.`ft1` `ft1`, `right_fg1`.`R_pk1` `R_pk1`, `right_fg1`.`R_ts` `R_ts`, `right_fg1`.`R_ft1` `R_ft1`\n" +
                        "FROM right_fg0\n" +
                  "INNER JOIN right_fg1 ON `right_fg0`.`join_pk_pk1` = `right_fg1`.`join_pk_pk1` AND `right_fg0`.`join_evt_ts` = `right_fg1`.`join_evt_ts`)";
    Assert.assertEquals(expected, result);
  }

  @Test
  public void testGenerateSqlWithRightFilter() {
    List<Feature> leftFeatures = new ArrayList<>();
    leftFeatures.add(new Feature("pk1", "fg0", fgLeft, true));
    leftFeatures.add(new Feature("pk2", "fg0", fgLeft));
    leftFeatures.add(new Feature("ts", "fg0", fgLeft));
    leftFeatures.add(new Feature("label", "fg0", fgLeft));
  
    List<Feature> rightFeatures = new ArrayList<>();
    Feature filterFeature = new Feature("ft1", "fg1", fgRight, "int", null);
    rightFeatures.add(new Feature("pk1", "fg1", fgRight));
    rightFeatures.add(new Feature("pk2", "fg1", fgRight));
    rightFeatures.add(new Feature("ts", "fg1", fgRight));
    rightFeatures.add(filterFeature);
  
    List<Feature> rightFeatures1 = new ArrayList<>();
    rightFeatures1.add(new Feature("pk1", "fg2", fgRight1));
    rightFeatures1.add(new Feature("ts", "fg2", fgRight1));
    rightFeatures1.add(new Feature("ft1", "fg2", fgRight1));
  
    List<Feature> leftOn = Arrays.asList(new Feature("pk1", "fg0", fgLeft), new Feature("pk2", "fg0", fgLeft));
    List<Feature> rightOn = Arrays.asList(new Feature("pk1", "fg1", fgRight), new Feature("pk2", "fg1", fgRight));
  
    // join on different pks
    List<Feature> leftOn1 = Collections.singletonList(new Feature("pk1", "fg0", fgLeft));
    List<Feature> rightOn1 = Collections.singletonList(new Feature("pk1", "fg2", fgRight1));
  
    List<SqlCondition> joinOperator = Arrays.asList(SqlCondition.EQUALS, SqlCondition.EQUALS);
    List<SqlCondition> joinOperator1 = Collections.singletonList(SqlCondition.EQUALS);
  
    FilterLogic filter = new FilterLogic(new Filter(Arrays.asList(filterFeature), SqlCondition.EQUALS, "1"));
  
    Query query = new Query("fs", "project", fgLeft, "fg0", leftFeatures, leftFeatures, false, filter);
    Query right = new Query("fs", "project", fgRight, "fg1", rightFeatures, rightFeatures, false, null);
    Query right1 = new Query("fs", "project", fgRight1, "fg2", rightFeatures1, rightFeatures1, false, null);
  
    Join join = new Join(query, right, leftOn, rightOn, JoinType.INNER, null, joinOperator);
    Join join1 = new Join(query, right1, leftOn1, rightOn1, JoinType.INNER, "R_", joinOperator1);
  
    query.setJoins(Arrays.asList(join, join1));
  
    String result =
      pitJoinController.generateSQL(query, false).toSqlString(new SparkSqlDialect(SqlDialect.EMPTY_CONTEXT)).getSql();
    String expected = "WITH right_fg0 AS (SELECT *\n" +
                                        "FROM (SELECT `fg0`.`pk1` `pk1`, `fg0`.`pk2` `pk2`, `fg0`.`ts` `ts`, `fg0`.`label` `label`, `fg0`.`pk1` `join_pk_pk1`, `fg0`.`ts` `join_evt_ts`, `fg1`.`pk1` `pk1`, `fg1`.`pk2` `pk2`, `fg1`.`ts` `ts`, `fg1`.`ft1` `ft1`, " +
                                                      "RANK() OVER (PARTITION BY `fg0`.`pk1`, `fg0`.`pk2`, `fg0`.`ts` ORDER BY `fg1`.`ts` DESC) pit_rank_hopsworks\n" +
                                                "FROM `fs`.`fg0_1` `fg0`\n" +
                                          "INNER JOIN `fs`.`fg1_1` `fg1` ON `fg0`.`pk1` = `fg1`.`pk1` AND `fg0`.`pk2` = `fg1`.`pk2` AND `fg0`.`ts` >= `fg1`.`ts`) NA\n" +
                                       "WHERE `pit_rank_hopsworks` = 1), " +
                           "right_fg1 AS (SELECT *\n" +
                                        "FROM (SELECT `fg0`.`pk1` `pk1`, `fg0`.`pk2` `pk2`, `fg0`.`ts` `ts`, `fg0`.`label` `label`, `fg0`.`pk1` `join_pk_pk1`, `fg0`.`ts` `join_evt_ts`, `fg2`.`pk1` `R_pk1`, `fg2`.`ts` `R_ts`, `fg2`.`ft1` `R_ft1`, " +
                                                      "RANK() OVER (PARTITION BY `fg0`.`pk1`, `fg0`.`ts` ORDER BY `fg2`.`ts` DESC) pit_rank_hopsworks\n" +
                                                "FROM `fs`.`fg0_1` `fg0`\n" +
                                          "INNER JOIN `fs`.`fg2_1` `fg2` ON `fg0`.`pk1` = `fg2`.`pk1` AND `fg0`.`ts` >= `fg2`.`ts`) NA\n" +
                                       "WHERE `pit_rank_hopsworks` = 1) (" +
                      "SELECT `right_fg0`.`pk1` `pk1`, `right_fg0`.`pk2` `pk2`, `right_fg0`.`ts` `ts`, `right_fg0`.`label` `label`, `right_fg0`.`pk1` `pk1`, `right_fg0`.`pk2` `pk2`, `right_fg0`.`ts` `ts`, `right_fg0`.`ft1` `ft1`, `right_fg1`.`R_pk1` `R_pk1`, `right_fg1`.`R_ts` `R_ts`, `right_fg1`.`R_ft1` `R_ft1`\n" +
                        "FROM right_fg0\n" +
                  "INNER JOIN right_fg1 ON `right_fg0`.`join_pk_pk1` = `right_fg1`.`join_pk_pk1` AND `right_fg0`.`join_evt_ts` = `right_fg1`.`join_evt_ts`\n"+
        "WHERE `right_fg0`.`ft1` = 1)";
    Assert.assertEquals(expected, result);
  }

  @Test
  public void testGenerateSqlWithRightFilterNotSelected() {
    List<Feature> leftFeatures = new ArrayList<>();
    leftFeatures.add(new Feature("pk1", "fg0", fgLeft, true));
    leftFeatures.add(new Feature("pk2", "fg0", fgLeft));
    leftFeatures.add(new Feature("ts", "fg0", fgLeft));
    leftFeatures.add(new Feature("label", "fg0", fgLeft));

    List<Feature> rightFeatures = new ArrayList<>();
    Feature filterFeature = new Feature("ft1", "fg1", fgRight, "int", null);
    rightFeatures.add(new Feature("pk1", "fg1", fgRight));
    rightFeatures.add(new Feature("pk2", "fg1", fgRight));
    rightFeatures.add(new Feature("ts", "fg1", fgRight));

    List<Feature> rightFeatures1 = new ArrayList<>();
    rightFeatures1.add(new Feature("pk1", "fg2", fgRight1));
    rightFeatures1.add(new Feature("ts", "fg2", fgRight1));
    rightFeatures1.add(new Feature("ft1", "fg2", fgRight1));

    List<Feature> leftOn = Arrays.asList(new Feature("pk1", "fg0", fgLeft), new Feature("pk2", "fg0", fgLeft));
    List<Feature> rightOn = Arrays.asList(new Feature("pk1", "fg1", fgRight), new Feature("pk2", "fg1", fgRight));

    // join on different pks
    List<Feature> leftOn1 = Collections.singletonList(new Feature("pk1", "fg0", fgLeft));
    List<Feature> rightOn1 = Collections.singletonList(new Feature("pk1", "fg2", fgRight1));

    List<SqlCondition> joinOperator = Arrays.asList(SqlCondition.EQUALS, SqlCondition.EQUALS);
    List<SqlCondition> joinOperator1 = Collections.singletonList(SqlCondition.EQUALS);

    FilterLogic filter = new FilterLogic(new Filter(Arrays.asList(filterFeature), SqlCondition.EQUALS, "1"));

    Query query = new Query("fs", "project", fgLeft, "fg0", leftFeatures, leftFeatures, false, filter);
    Query right = new Query("fs", "project", fgRight, "fg1", rightFeatures, rightFeatures, false, null);
    Query right1 = new Query("fs", "project", fgRight1, "fg2", rightFeatures1, rightFeatures1, false, null);

    Join join = new Join(query, right, leftOn, rightOn, JoinType.INNER, null, joinOperator);
    Join join1 = new Join(query, right1, leftOn1, rightOn1, JoinType.INNER, "R_", joinOperator1);

    query.setJoins(Arrays.asList(join, join1));

    String result =
            pitJoinController.generateSQL(query, false).toSqlString(new SparkSqlDialect(SqlDialect.EMPTY_CONTEXT)).getSql();
    String expected = "WITH right_fg0 AS (SELECT *\n" +
            "FROM (SELECT `fg0`.`pk1` `pk1`, `fg0`.`pk2` `pk2`, `fg0`.`ts` `ts`, `fg0`.`label` `label`, `fg0`.`pk1` `join_pk_pk1`, `fg0`.`ts` `join_evt_ts`, `fg1`.`pk1` `pk1`, `fg1`.`pk2` `pk2`, `fg1`.`ts` `ts`, `fg1`.`ft1` `ft1`, " +
            "RANK() OVER (PARTITION BY `fg0`.`pk1`, `fg0`.`pk2`, `fg0`.`ts` ORDER BY `fg1`.`ts` DESC) pit_rank_hopsworks\n" +
            "FROM `fs`.`fg0_1` `fg0`\n" +
            "INNER JOIN `fs`.`fg1_1` `fg1` ON `fg0`.`pk1` = `fg1`.`pk1` AND `fg0`.`pk2` = `fg1`.`pk2` AND `fg0`.`ts` >= `fg1`.`ts`) NA\n" +
            "WHERE `pit_rank_hopsworks` = 1), " +
            "right_fg1 AS (SELECT *\n" +
            "FROM (SELECT `fg0`.`pk1` `pk1`, `fg0`.`pk2` `pk2`, `fg0`.`ts` `ts`, `fg0`.`label` `label`, `fg0`.`pk1` `join_pk_pk1`, `fg0`.`ts` `join_evt_ts`, `fg2`.`pk1` `R_pk1`, `fg2`.`ts` `R_ts`, `fg2`.`ft1` `R_ft1`, " +
            "RANK() OVER (PARTITION BY `fg0`.`pk1`, `fg0`.`ts` ORDER BY `fg2`.`ts` DESC) pit_rank_hopsworks\n" +
            "FROM `fs`.`fg0_1` `fg0`\n" +
            "INNER JOIN `fs`.`fg2_1` `fg2` ON `fg0`.`pk1` = `fg2`.`pk1` AND `fg0`.`ts` >= `fg2`.`ts`) NA\n" +
            "WHERE `pit_rank_hopsworks` = 1) (" +
            "SELECT `right_fg0`.`pk1` `pk1`, `right_fg0`.`pk2` `pk2`, `right_fg0`.`ts` `ts`, `right_fg0`.`label` `label`, `right_fg0`.`pk1` `pk1`, `right_fg0`.`pk2` `pk2`, `right_fg0`.`ts` `ts`, `right_fg1`.`R_pk1` `R_pk1`, `right_fg1`.`R_ts` `R_ts`, `right_fg1`.`R_ft1` `R_ft1`\n" +
            "FROM right_fg0\n" +
            "INNER JOIN right_fg1 ON `right_fg0`.`join_pk_pk1` = `right_fg1`.`join_pk_pk1` AND `right_fg0`.`join_evt_ts` = `right_fg1`.`join_evt_ts`\n"+
            "WHERE `right_fg0`.`ft1` = 1)";
    Assert.assertEquals(expected, result);
  }

  @Test
  public void testGenerateSqlWithRightFilterInner() {
    List<Feature> leftFeatures = new ArrayList<>();
    leftFeatures.add(new Feature("pk1", "fg0", fgLeft, true));
    leftFeatures.add(new Feature("pk2", "fg0", fgLeft));
    leftFeatures.add(new Feature("ts", "fg0", fgLeft));
    leftFeatures.add(new Feature("label", "fg0", fgLeft));

    List<Feature> rightFeatures = new ArrayList<>();
    Feature filterFeature = new Feature("ft1", "fg1", fgRight, "int", null);
    rightFeatures.add(new Feature("pk1", "fg1", fgRight));
    rightFeatures.add(new Feature("pk2", "fg1", fgRight));
    rightFeatures.add(new Feature("ts", "fg1", fgRight));
    rightFeatures.add(filterFeature);

    List<Feature> rightFeatures1 = new ArrayList<>();
    rightFeatures1.add(new Feature("pk1", "fg2", fgRight1));
    rightFeatures1.add(new Feature("ts", "fg2", fgRight1));
    rightFeatures1.add(new Feature("ft1", "fg2", fgRight1));

    List<Feature> leftOn = Arrays.asList(new Feature("pk1", "fg0", fgLeft), new Feature("pk2", "fg0", fgLeft));
    List<Feature> rightOn = Arrays.asList(new Feature("pk1", "fg1", fgRight), new Feature("pk2", "fg1", fgRight));

    // join on different pks
    List<Feature> leftOn1 = Collections.singletonList(new Feature("pk1", "fg0", fgLeft));
    List<Feature> rightOn1 = Collections.singletonList(new Feature("pk1", "fg2", fgRight1));

    List<SqlCondition> joinOperator = Arrays.asList(SqlCondition.EQUALS, SqlCondition.EQUALS);
    List<SqlCondition> joinOperator1 = Collections.singletonList(SqlCondition.EQUALS);

    FilterLogic filter = new FilterLogic(new Filter(Arrays.asList(filterFeature), SqlCondition.EQUALS, "1"));

    Query query = new Query("fs", "project", fgLeft, "fg0", leftFeatures, leftFeatures, false, null);
    Query right = new Query("fs", "project", fgRight, "fg1", rightFeatures, rightFeatures, false, filter);
    Query right1 = new Query("fs", "project", fgRight1, "fg2", rightFeatures1, rightFeatures1, false, null);

    Join join = new Join(query, right, leftOn, rightOn, JoinType.INNER, null, joinOperator);
    Join join1 = new Join(query, right1, leftOn1, rightOn1, JoinType.INNER, "R_", joinOperator1);

    query.setJoins(Arrays.asList(join, join1));

    String result =
            pitJoinController.generateSQL(query, false).toSqlString(new SparkSqlDialect(SqlDialect.EMPTY_CONTEXT)).getSql();
    String expected = "WITH right_fg0 AS (SELECT *\n" +
            "FROM (SELECT `fg0`.`pk1` `pk1`, `fg0`.`pk2` `pk2`, `fg0`.`ts` `ts`, `fg0`.`label` `label`, `fg0`.`pk1` `join_pk_pk1`, `fg0`.`ts` `join_evt_ts`, `fg1`.`pk1` `pk1`, `fg1`.`pk2` `pk2`, `fg1`.`ts` `ts`, `fg1`.`ft1` `ft1`, " +
            "RANK() OVER (PARTITION BY `fg0`.`pk1`, `fg0`.`pk2`, `fg0`.`ts` ORDER BY `fg1`.`ts` DESC) pit_rank_hopsworks\n" +
            "FROM `fs`.`fg0_1` `fg0`\n" +
            "INNER JOIN `fs`.`fg1_1` `fg1` ON `fg0`.`pk1` = `fg1`.`pk1` AND `fg0`.`pk2` = `fg1`.`pk2` AND `fg0`.`ts` >= `fg1`.`ts`\n" +
            "WHERE `fg1`.`ft1` = 1) NA\n" +
            "WHERE `pit_rank_hopsworks` = 1), " +
            "right_fg1 AS (SELECT *\n" +
            "FROM (SELECT `fg0`.`pk1` `pk1`, `fg0`.`pk2` `pk2`, `fg0`.`ts` `ts`, `fg0`.`label` `label`, `fg0`.`pk1` `join_pk_pk1`, `fg0`.`ts` `join_evt_ts`, `fg2`.`pk1` `R_pk1`, `fg2`.`ts` `R_ts`, `fg2`.`ft1` `R_ft1`, " +
            "RANK() OVER (PARTITION BY `fg0`.`pk1`, `fg0`.`ts` ORDER BY `fg2`.`ts` DESC) pit_rank_hopsworks\n" +
            "FROM `fs`.`fg0_1` `fg0`\n" +
            "INNER JOIN `fs`.`fg2_1` `fg2` ON `fg0`.`pk1` = `fg2`.`pk1` AND `fg0`.`ts` >= `fg2`.`ts`) NA\n" +
            "WHERE `pit_rank_hopsworks` = 1) (" +
            "SELECT `right_fg0`.`pk1` `pk1`, `right_fg0`.`pk2` `pk2`, `right_fg0`.`ts` `ts`, `right_fg0`.`label` `label`, `right_fg0`.`pk1` `pk1`, `right_fg0`.`pk2` `pk2`, `right_fg0`.`ts` `ts`, `right_fg0`.`ft1` `ft1`, `right_fg1`.`R_pk1` `R_pk1`, `right_fg1`.`R_ts` `R_ts`, `right_fg1`.`R_ft1` `R_ft1`\n" +
            "FROM right_fg0\n" +
            "INNER JOIN right_fg1 ON `right_fg0`.`join_pk_pk1` = `right_fg1`.`join_pk_pk1` AND `right_fg0`.`join_evt_ts` = `right_fg1`.`join_evt_ts`)";
    Assert.assertEquals(expected, result);
  }

  @Test
  public void testGenerateSqlWithLeftFilter() {
    List<Feature> leftFeatures = new ArrayList<>();
    Feature filterFeature = new Feature("label", "fg0", fgLeft, "int", null);
    leftFeatures.add(new Feature("pk1", "fg0", fgLeft, true));
    leftFeatures.add(new Feature("pk2", "fg0", fgLeft));
    leftFeatures.add(new Feature("ts", "fg0", fgLeft));
    leftFeatures.add(filterFeature);

    List<Feature> rightFeatures = new ArrayList<>();
    rightFeatures.add(new Feature("pk1", "fg1", fgRight));
    rightFeatures.add(new Feature("pk2", "fg1", fgRight));
    rightFeatures.add(new Feature("ts", "fg1", fgRight));
    rightFeatures.add(new Feature("ft1", "fg1", fgRight));

    List<Feature> rightFeatures1 = new ArrayList<>();
    rightFeatures1.add(new Feature("pk1", "fg2", fgRight1));
    rightFeatures1.add(new Feature("ts", "fg2", fgRight1));
    rightFeatures1.add(new Feature("ft1", "fg2", fgRight1));

    List<Feature> leftOn = Arrays.asList(new Feature("pk1", "fg0", fgLeft), new Feature("pk2", "fg0", fgLeft));
    List<Feature> rightOn = Arrays.asList(new Feature("pk1", "fg1", fgRight), new Feature("pk2", "fg1", fgRight));

    // join on different pks
    List<Feature> leftOn1 = Collections.singletonList(new Feature("pk1", "fg0", fgLeft));
    List<Feature> rightOn1 = Collections.singletonList(new Feature("pk1", "fg2", fgRight1));

    List<SqlCondition> joinOperator = Arrays.asList(SqlCondition.EQUALS, SqlCondition.EQUALS);
    List<SqlCondition> joinOperator1 = Collections.singletonList(SqlCondition.EQUALS);

    FilterLogic filter = new FilterLogic(new Filter(Arrays.asList(filterFeature), SqlCondition.EQUALS, "1"));

    Query query = new Query("fs", "project", fgLeft, "fg0", leftFeatures, leftFeatures, false, filter);
    Query right = new Query("fs", "project", fgRight, "fg1", rightFeatures, rightFeatures, false, null);
    Query right1 = new Query("fs", "project", fgRight1, "fg2", rightFeatures1, rightFeatures1, false, null);

    Join join = new Join(query, right, leftOn, rightOn, JoinType.INNER, null, joinOperator);
    Join join1 = new Join(query, right1, leftOn1, rightOn1, JoinType.INNER, "R_", joinOperator1);

    query.setJoins(Arrays.asList(join, join1));

    String result =
            pitJoinController.generateSQL(query, false).toSqlString(new SparkSqlDialect(SqlDialect.EMPTY_CONTEXT)).getSql();
    String expected = "WITH right_fg0 AS (SELECT *\n" +
            "FROM (SELECT `fg0`.`pk1` `pk1`, `fg0`.`pk2` `pk2`, `fg0`.`ts` `ts`, `fg0`.`label` `label`, `fg0`.`pk1` `join_pk_pk1`, `fg0`.`ts` `join_evt_ts`, `fg1`.`pk1` `pk1`, `fg1`.`pk2` `pk2`, `fg1`.`ts` `ts`, `fg1`.`ft1` `ft1`, " +
            "RANK() OVER (PARTITION BY `fg0`.`pk1`, `fg0`.`pk2`, `fg0`.`ts` ORDER BY `fg1`.`ts` DESC) pit_rank_hopsworks\n" +
            "FROM `fs`.`fg0_1` `fg0`\n" +
            "INNER JOIN `fs`.`fg1_1` `fg1` ON `fg0`.`pk1` = `fg1`.`pk1` AND `fg0`.`pk2` = `fg1`.`pk2` AND `fg0`.`ts` >= `fg1`.`ts`\n" +
            "WHERE `fg0`.`label` = 1) NA\n" +
            "WHERE `pit_rank_hopsworks` = 1), " +
            "right_fg1 AS (SELECT *\n" +
            "FROM (SELECT `fg0`.`pk1` `pk1`, `fg0`.`pk2` `pk2`, `fg0`.`ts` `ts`, `fg0`.`label` `label`, `fg0`.`pk1` `join_pk_pk1`, `fg0`.`ts` `join_evt_ts`, `fg2`.`pk1` `R_pk1`, `fg2`.`ts` `R_ts`, `fg2`.`ft1` `R_ft1`, " +
            "RANK() OVER (PARTITION BY `fg0`.`pk1`, `fg0`.`ts` ORDER BY `fg2`.`ts` DESC) pit_rank_hopsworks\n" +
            "FROM `fs`.`fg0_1` `fg0`\n" +
            "INNER JOIN `fs`.`fg2_1` `fg2` ON `fg0`.`pk1` = `fg2`.`pk1` AND `fg0`.`ts` >= `fg2`.`ts`\n" +
            "WHERE `fg0`.`label` = 1) NA\n" +
            "WHERE `pit_rank_hopsworks` = 1) (" +
            "SELECT `right_fg0`.`pk1` `pk1`, `right_fg0`.`pk2` `pk2`, `right_fg0`.`ts` `ts`, `right_fg0`.`label` `label`, `right_fg0`.`pk1` `pk1`, `right_fg0`.`pk2` `pk2`, `right_fg0`.`ts` `ts`, `right_fg0`.`ft1` `ft1`, `right_fg1`.`R_pk1` `R_pk1`, `right_fg1`.`R_ts` `R_ts`, `right_fg1`.`R_ft1` `R_ft1`\n" +
            "FROM right_fg0\n" +
            "INNER JOIN right_fg1 ON `right_fg0`.`join_pk_pk1` = `right_fg1`.`join_pk_pk1` AND `right_fg0`.`join_evt_ts` = `right_fg1`.`join_evt_ts`)";
    Assert.assertEquals(expected, result);
  }

  @Test
  public void testGenerateSqlWithDefault() {
    List<Feature> leftFeatures = new ArrayList<>();
    leftFeatures.add(new Feature("pk1", "fg0", fgLeft, true));
    leftFeatures.add(new Feature("pk2", "fg0", fgLeft));
    leftFeatures.add(new Feature("ts", "fg0", fgLeft));
    leftFeatures.add(new Feature("label", "fg0", fgLeft));
    
    List<Feature> rightFeatures = new ArrayList<>();
    rightFeatures.add(new Feature("pk1", "fg1", fgRight));
    rightFeatures.add(new Feature("pk2", "fg1", fgRight));
    rightFeatures.add(new Feature("ts", "fg1", fgRight));
    rightFeatures.add(new Feature("ft1", "fg1", fgRight, "string", "abc"));
    
    List<Feature> rightFeatures1 = new ArrayList<>();
    rightFeatures1.add(new Feature("pk1", "fg2", fgRight1));
    rightFeatures1.add(new Feature("ts", "fg2", fgRight1));
    rightFeatures1.add(new Feature("ft1", "fg2", fgRight1));
    
    List<Feature> leftOn = Arrays.asList(new Feature("pk1", "fg0", fgLeft), new Feature("pk2", "fg0", fgLeft));
    List<Feature> rightOn = Arrays.asList(new Feature("pk1", "fg1", fgRight), new Feature("pk2", "fg1", fgRight));
    
    // join on different pks
    List<Feature> leftOn1 = Collections.singletonList(new Feature("pk1", "fg0", fgLeft));
    List<Feature> rightOn1 = Collections.singletonList(new Feature("pk1", "fg2", fgRight1));
    
    List<SqlCondition> joinOperator = Arrays.asList(SqlCondition.EQUALS, SqlCondition.EQUALS);
    List<SqlCondition> joinOperator1 = Collections.singletonList(SqlCondition.EQUALS);
    
    Query query = new Query("fs", "project", fgLeft, "fg0", leftFeatures, leftFeatures, false, null);
    Query right = new Query("fs", "project", fgRight, "fg1", rightFeatures, rightFeatures, false, null);
    Query right1 = new Query("fs", "project", fgRight, "fg2", rightFeatures1, rightFeatures1, false, null);
    
    Join join = new Join(query, right, leftOn, rightOn, JoinType.INNER, null, joinOperator);
    Join join1 = new Join(query, right1, leftOn1, rightOn1, JoinType.INNER, null, joinOperator1);
    
    query.setJoins(Arrays.asList(join, join1));
    
    String result =
      pitJoinController.generateSQL(query, false).toSqlString(new SparkSqlDialect(SqlDialect.EMPTY_CONTEXT)).getSql();
    String expected = "WITH right_fg0 AS (SELECT *\n" +
                                        "FROM (SELECT `fg0`.`pk1` `pk1`, `fg0`.`pk2` `pk2`, `fg0`.`ts` `ts`, `fg0`.`label` `label`, `fg0`.`pk1` `join_pk_pk1`, `fg0`.`ts` `join_evt_ts`, `fg1`.`pk1` `pk1`, `fg1`.`pk2` `pk2`, `fg1`.`ts` `ts`, " +
                                                      "CASE WHEN `fg1`.`ft1` IS NULL THEN 'abc' ELSE `fg1`.`ft1` END `ft1`, " +
                                                      "RANK() OVER (PARTITION BY `fg0`.`pk1`, `fg0`.`pk2`, `fg0`.`ts` ORDER BY `fg1`.`ts` DESC) pit_rank_hopsworks\n" +
                                                "FROM `fs`.`fg0_1` `fg0`\n" +
                                          "INNER JOIN `fs`.`fg1_1` `fg1` ON `fg0`.`pk1` = `fg1`.`pk1` AND `fg0`.`pk2` = `fg1`.`pk2` AND `fg0`.`ts` >= `fg1`.`ts`) NA\n" +
                                       "WHERE `pit_rank_hopsworks` = 1), " +
                           "right_fg1 AS (SELECT *\n" +
                                        "FROM (SELECT `fg0`.`pk1` `pk1`, `fg0`.`pk2` `pk2`, `fg0`.`ts` `ts`, `fg0`.`label` `label`, `fg0`.`pk1` `join_pk_pk1`, `fg0`.`ts` `join_evt_ts`, `fg2`.`pk1` `pk1`, `fg2`.`ts` `ts`, `fg2`.`ft1` `ft1`, " +
                                                      "RANK() OVER (PARTITION BY `fg0`.`pk1`, `fg0`.`ts` ORDER BY `fg2`.`ts` DESC) pit_rank_hopsworks\n" +
                                                "FROM `fs`.`fg0_1` `fg0`\n" +
                                          "INNER JOIN `fs`.`fg1_1` `fg2` ON `fg0`.`pk1` = `fg2`.`pk1` AND `fg0`.`ts` >= `fg2`.`ts`) NA\n" +
                                       "WHERE `pit_rank_hopsworks` = 1) (" +
                      "SELECT `right_fg0`.`pk1` `pk1`, `right_fg0`.`pk2` `pk2`, `right_fg0`.`ts` `ts`, `right_fg0`.`label` `label`, `right_fg0`.`pk1` `pk1`, `right_fg0`.`pk2` `pk2`, `right_fg0`.`ts` `ts`, `right_fg0`.`ft1` `ft1`, `right_fg1`.`pk1` `pk1`, `right_fg1`.`ts` `ts`, `right_fg1`.`ft1` `ft1`\n" +
                        "FROM right_fg0\n" +
                  "INNER JOIN right_fg1 ON `right_fg0`.`join_pk_pk1` = `right_fg1`.`join_pk_pk1` AND `right_fg0`.`join_evt_ts` = `right_fg1`.`join_evt_ts`)";
    Assert.assertEquals(expected, result);
  }
  
  @Test
  public void testGenerateSqlNoPKSelected() {
    List<Feature> leftFeaturesSelected = new ArrayList<>();
    leftFeaturesSelected.add(new Feature("label", "fg0", fgLeft));
    
    List<Feature> leftFeaturesAvailable = new ArrayList<>();
    leftFeaturesAvailable.add(new Feature("pk1", "fg0", fgLeft, true));
    leftFeaturesAvailable.add(new Feature("pk2", "fg0", fgLeft));
    leftFeaturesAvailable.add(new Feature("ts", "fg0", fgLeft));
    leftFeaturesAvailable.add(new Feature("label", "fg0", fgLeft));
    
    List<Feature> rightFeatures = new ArrayList<>();
    rightFeatures.add(new Feature("pk1", "fg1", fgRight));
    rightFeatures.add(new Feature("pk2", "fg1", fgRight));
    rightFeatures.add(new Feature("ts", "fg1", fgRight));
    rightFeatures.add(new Feature("ft1", "fg1", fgRight));
    
    List<Feature> rightFeatures1 = new ArrayList<>();
    rightFeatures1.add(new Feature("pk1", "fg2", fgRight1));
    rightFeatures1.add(new Feature("ts", "fg2", fgRight1));
    rightFeatures1.add(new Feature("ft1", "fg2", fgRight1));
    
    List<Feature> leftOn = Arrays.asList(new Feature("pk1", "fg0", fgLeft), new Feature("pk2", "fg0", fgLeft));
    List<Feature> rightOn = Arrays.asList(new Feature("pk1", "fg1", fgRight), new Feature("pk2", "fg1", fgRight));
    
    // join on different pks
    List<Feature> leftOn1 = Collections.singletonList(new Feature("pk1", "fg0", fgLeft));
    List<Feature> rightOn1 = Collections.singletonList(new Feature("pk1", "fg2", fgRight1));
    
    List<SqlCondition> joinOperator = Arrays.asList(SqlCondition.EQUALS, SqlCondition.EQUALS);
    List<SqlCondition> joinOperator1 = Collections.singletonList(SqlCondition.EQUALS);
    
    Query query = new Query("fs", "project", fgLeft, "fg0", leftFeaturesSelected, leftFeaturesAvailable, false, null);
    Query right = new Query("fs", "project", fgRight, "fg1", rightFeatures, rightFeatures, false, null);
    Query right1 = new Query("fs", "project", fgRight1, "fg2", rightFeatures1, rightFeatures1, false, null);
    
    Join join = new Join(query, right, leftOn, rightOn, JoinType.INNER, null, joinOperator);
    Join join1 = new Join(query, right1, leftOn1, rightOn1, JoinType.INNER, null, joinOperator1);
    
    query.setJoins(Arrays.asList(join, join1));
    
    String result =
      pitJoinController.generateSQL(query, false).toSqlString(new SparkSqlDialect(SqlDialect.EMPTY_CONTEXT)).getSql();
    String expected = "WITH right_fg0 AS (SELECT *\n" +
                                        "FROM (SELECT `fg0`.`label` `label`, `fg0`.`pk1` `join_pk_pk1`, `fg0`.`ts` `join_evt_ts`, `fg1`.`pk1` `pk1`, `fg1`.`pk2` `pk2`, `fg1`.`ts` `ts`, `fg1`.`ft1` `ft1`, " +
                                                      "RANK() OVER (PARTITION BY `fg0`.`pk1`, `fg0`.`pk2`, `fg0`.`ts` ORDER BY `fg1`.`ts` DESC) pit_rank_hopsworks\n" +
                                                "FROM `fs`.`fg0_1` `fg0`\n" +
                                          "INNER JOIN `fs`.`fg1_1` `fg1` ON `fg0`.`pk1` = `fg1`.`pk1` AND `fg0`.`pk2` = `fg1`.`pk2` AND `fg0`.`ts` >= `fg1`.`ts`) NA\n" +
                                       "WHERE `pit_rank_hopsworks` = 1), " +
                           "right_fg1 AS (SELECT *\n" +
                                         "FROM (SELECT `fg0`.`label` `label`, `fg0`.`pk1` `join_pk_pk1`, `fg0`.`ts` `join_evt_ts`, `fg2`.`pk1` `pk1`, `fg2`.`ts` `ts`, `fg2`.`ft1` `ft1`, " +
                                                       "RANK() OVER (PARTITION BY `fg0`.`pk1`, `fg0`.`ts` ORDER BY `fg2`.`ts` DESC) pit_rank_hopsworks\n" +
                                                 "FROM `fs`.`fg0_1` `fg0`\n" +
                                           "INNER JOIN `fs`.`fg2_1` `fg2` ON `fg0`.`pk1` = `fg2`.`pk1` AND `fg0`.`ts` >= `fg2`.`ts`) NA\n" +
                                        "WHERE `pit_rank_hopsworks` = 1) (" +
                        "SELECT `right_fg0`.`label` `label`, `right_fg0`.`pk1` `pk1`, `right_fg0`.`pk2` `pk2`, `right_fg0`.`ts` `ts`, `right_fg0`.`ft1` `ft1`, `right_fg1`.`pk1` `pk1`, `right_fg1`.`ts` `ts`, `right_fg1`.`ft1` `ft1`\n" +
                          "FROM right_fg0\n" +
                         "INNER JOIN right_fg1 ON `right_fg0`.`join_pk_pk1` = `right_fg1`.`join_pk_pk1` AND `right_fg0`.`join_evt_ts` = `right_fg1`.`join_evt_ts`)";
    Assert.assertEquals(expected, result);
  }

  @Test
  public void testGenerateSqlTrainingDatasetWithJoinKeysDropped() {
    List<Feature> leftFeaturesSelected = new ArrayList<>();
    leftFeaturesSelected.add(new Feature("label", "fg0", fgLeft,false, 1));
  
    List<Feature> leftFeaturesAvailable = new ArrayList<>();
    leftFeaturesAvailable.add(new Feature("pk1", "fg0", fgLeft, true));
    leftFeaturesAvailable.add(new Feature("pk2", "fg0", fgLeft));
    leftFeaturesAvailable.add(new Feature("ts", "fg0", fgLeft));
    leftFeaturesAvailable.add(new Feature("label", "fg0", fgLeft));
  
    // add all features to left query as we do for training datasets
    leftFeaturesSelected.add(new Feature("ft1", "fg1", fgRight, false,2));
    leftFeaturesSelected.add(new Feature("ft2", "fg2", fgRight1, false,3));
    
    List<Feature> leftOn = Arrays.asList(new Feature("pk1", "fg0", fgLeft), new Feature("pk2", "fg0", fgLeft));
    List<Feature> rightOn = Arrays.asList(new Feature("pk1", "fg1", fgRight), new Feature("pk2", "fg1", fgRight));
  
    List<Feature> rightAvailableFeatures = new ArrayList<>();
    rightAvailableFeatures.add(new Feature("pk1", "fg1", fgRight));
    rightAvailableFeatures.add(new Feature("pk2", "fg1", fgRight));
    rightAvailableFeatures.add(new Feature("ts", "fg1", fgRight));
    rightAvailableFeatures.add(new Feature("ft1", "fg1", fgRight));
  
    List<Feature> rightAvailableFeatures1 = new ArrayList<>();
    rightAvailableFeatures1.add(new Feature("pk1", "fg2", fgRight1));
    rightAvailableFeatures1.add(new Feature("ts", "fg2", fgRight1));
    rightAvailableFeatures1.add(new Feature("ft2", "fg2", fgRight1));
    
    // join on different pks
    List<Feature> leftOn1 = Collections.singletonList(new Feature("pk1", "fg0", fgLeft));
    List<Feature> rightOn1 = Collections.singletonList(new Feature("pk1", "fg2", fgRight1));
    
    List<SqlCondition> joinOperator = Arrays.asList(SqlCondition.EQUALS, SqlCondition.EQUALS);
    List<SqlCondition> joinOperator1 = Collections.singletonList(SqlCondition.EQUALS);
    
    Query query = new Query("fs", "project", fgLeft, "fg0", leftFeaturesSelected, leftFeaturesAvailable, false, null);
    Query right = new Query("fs", "project", fgRight, "fg1", new ArrayList<>(), rightAvailableFeatures, false, null);
    Query right1 = new Query("fs", "project", fgRight1, "fg2", new ArrayList<>(), rightAvailableFeatures1, false, null);
    
    Join join = new Join(query, right, leftOn, rightOn, JoinType.INNER, null, joinOperator);
    Join join1 = new Join(query, right1, leftOn1, rightOn1, JoinType.INNER, null, joinOperator1);
    
    query.setJoins(Arrays.asList(join, join1));
    
    String result =
      pitJoinController.generateSQL(query, true).toSqlString(new SparkSqlDialect(SqlDialect.EMPTY_CONTEXT)).getSql();
    String expected = "WITH right_fg0 AS (SELECT *\n" +
                                        "FROM (SELECT `fg0`.`label` `label`, `fg1`.`ft1` `ft1`, `fg0`.`pk1` `join_pk_pk1`, `fg0`.`ts` `join_evt_ts`, " +
                                                      "RANK() OVER (PARTITION BY `fg0`.`pk1`, `fg0`.`pk2`, `fg0`.`ts` ORDER BY `fg1`.`ts` DESC) pit_rank_hopsworks\n" +
                                                "FROM `fs`.`fg0_1` `fg0`\n" +
                                          "INNER JOIN `fs`.`fg1_1` `fg1` ON `fg0`.`pk1` = `fg1`.`pk1` AND `fg0`.`pk2` = `fg1`.`pk2` AND `fg0`.`ts` >= `fg1`.`ts`) NA\n" +
                                       "WHERE `pit_rank_hopsworks` = 1), " +
                           "right_fg1 AS (SELECT *\n" +
                                        "FROM (SELECT `fg0`.`label` `label`, `fg2`.`ft2` `ft2`, `fg0`.`pk1` `join_pk_pk1`, `fg0`.`ts` `join_evt_ts`, " +
                                                      "RANK() OVER (PARTITION BY `fg0`.`pk1`, `fg0`.`ts` ORDER BY `fg2`.`ts` DESC) pit_rank_hopsworks\n" +
                                                "FROM `fs`.`fg0_1` `fg0`\n" +
                                          "INNER JOIN `fs`.`fg2_1` `fg2` ON `fg0`.`pk1` = `fg2`.`pk1` AND `fg0`.`ts` >= `fg2`.`ts`) NA\n" +
                                       "WHERE `pit_rank_hopsworks` = 1) (" +
                        "SELECT `right_fg0`.`label` `label`, `right_fg0`.`ft1` `ft1`, `right_fg1`.`ft2` `ft2`\n" +
                          "FROM right_fg0\n" +
                    "INNER JOIN right_fg1 ON `right_fg0`.`join_pk_pk1` = `right_fg1`.`join_pk_pk1` AND `right_fg0`.`join_evt_ts` = `right_fg1`.`join_evt_ts`)";
    Assert.assertEquals(expected, result);
  }
  
}
