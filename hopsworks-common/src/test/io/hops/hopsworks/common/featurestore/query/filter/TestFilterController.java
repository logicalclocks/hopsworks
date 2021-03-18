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

package io.hops.hopsworks.common.featurestore.query.filter;

import io.hops.hopsworks.common.featurestore.feature.FeatureGroupFeatureDTO;
import io.hops.hopsworks.common.featurestore.query.ConstructorController;
import io.hops.hopsworks.common.featurestore.query.Feature;
import io.hops.hopsworks.common.featurestore.query.Join;
import io.hops.hopsworks.common.featurestore.query.Query;
import io.hops.hopsworks.exceptions.FeaturestoreException;
import io.hops.hopsworks.persistence.entity.featurestore.Featurestore;
import io.hops.hopsworks.persistence.entity.featurestore.featuregroup.Featuregroup;
import io.hops.hopsworks.persistence.entity.featurestore.featuregroup.cached.CachedFeaturegroup;
import io.hops.hopsworks.persistence.entity.featurestore.featuregroup.cached.TimeTravelFormat;
import io.hops.hopsworks.persistence.entity.project.Project;
import org.apache.calcite.sql.JoinType;
import org.apache.calcite.sql.SqlDialect;
import org.apache.calcite.sql.dialect.SparkSqlDialect;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TestFilterController {
  
  private ConstructorController constructorController;
  private FilterController filterController;
  
  Featuregroup fg1;
  Featuregroup fg2;
  Featuregroup fg3;
  
  List<Feature> fg1Features = new ArrayList<>();
  List<Feature> fg2Features = new ArrayList<>();
  List<Feature> fg3Features = new ArrayList<>();
  
  List<Feature> joinFeatures = new ArrayList<>();
  List<Feature> leftOn = new ArrayList<>();
  List<Feature> rightOn = new ArrayList<>();
  
  
  private Map<Integer, Featuregroup> fgLookup;
  private Map<Integer, List<Feature>> availableFeatureLookup;
  
  @Rule
  public ExpectedException thrown = ExpectedException.none();
  
  @Before
  public void setup() {
    constructorController = new ConstructorController();
    filterController = new FilterController(constructorController);
  
    Featurestore fs = new Featurestore();
    fs.setHiveDbId(1l);
    fs.setProject(new Project("test_proj"));
    CachedFeaturegroup cachedFeaturegroup = new CachedFeaturegroup();
    cachedFeaturegroup.setTimeTravelFormat(TimeTravelFormat.NONE);
    fg1 = new Featuregroup(1);
    fg1.setName("fg1");
    fg1.setVersion(1);
    fg1.setCachedFeaturegroup(cachedFeaturegroup);
    fg1.setFeaturestore(fs);
    fg2 = new Featuregroup(2);
    fg2.setName("fg2");
    fg2.setVersion(1);
    fg2.setCachedFeaturegroup(cachedFeaturegroup);
    fg2.setFeaturestore(fs);
    fg3 = new Featuregroup(3);
    fg3.setName("fg3");
    fg3.setVersion(1);
    fg3.setCachedFeaturegroup(cachedFeaturegroup);
    fg3.setFeaturestore(fs);

    fgLookup = new HashMap<>();
    fgLookup.put(fg1.getId(), fg1);
    fgLookup.put(fg2.getId(), fg2);
    fgLookup.put(fg3.getId(), fg3);
  
    fg1Features = new ArrayList<>();
    fg1Features.add(new Feature("fg1_pk", "fg1", "string",true, null));
    fg1Features.add(new Feature("fg1_ft", "fg1", "integer", false, null));
    fg1Features.add(new Feature("join", "fg1", "string",true, null));
  
    fg2Features = new ArrayList<>();
    fg2Features.add(new Feature("fg2_pk", "fg2", "string", true, null));
    fg2Features.add(new Feature("fg2_ft", "fg2", "double", false, "10.0"));
    fg3Features.add(new Feature("join", "fg2", "string",true, null));
  
    fg3Features = new ArrayList<>();
    fg3Features.add(new Feature("fg3_pk", "fg3", "string", true, null));
    fg3Features.add(new Feature("fg3_ft", "fg3", "string",false, "default"));
    fg3Features.add(new Feature("join", "fg3", "string",true, null));
  
    joinFeatures = new ArrayList<>();
    joinFeatures.add(fg1Features.get(2));
    leftOn = new ArrayList<>();
    leftOn.add(fg1Features.get(0));
    leftOn.add(fg1Features.get(2));
    rightOn = new ArrayList<>();
    rightOn.add(fg3Features.get(0));
    rightOn.add(fg3Features.get(2));
  
    availableFeatureLookup = new HashMap<>();
    availableFeatureLookup.put(fg1.getId(), fg1Features);
    availableFeatureLookup.put(fg2.getId(), fg2Features);
    availableFeatureLookup.put(fg3.getId(), fg3Features);
  }
  
  @Test
  public void testValidateFilterLogicDTO() throws Exception {
    // tests the validation of a FilterLogicDTO
    
    FeatureGroupFeatureDTO featureGroupFeatureDTO = new FeatureGroupFeatureDTO("fg1_pk", "string", "", true, "10");
    FilterDTO filter = new FilterDTO(featureGroupFeatureDTO, SqlFilterCondition.EQUALS, "10");
    FilterLogicDTO filterLogicDTO2 = new FilterLogicDTO(SqlFilterLogic.AND);
    
    // case 1
    FilterLogicDTO filterLogicDTO = new FilterLogicDTO(SqlFilterLogic.AND);
    filterLogicDTO.setLeftFilter(filter);
    filterLogicDTO.setLeftLogic(filterLogicDTO2);
    
    thrown.expect(FeaturestoreException.class);
    filterController.validateFilterLogicDTO(filterLogicDTO);
    
    // case 2
    filterLogicDTO = new FilterLogicDTO(SqlFilterLogic.AND);
    filterLogicDTO.setRightFilter(filter);
    filterLogicDTO.setRightLogic(filterLogicDTO2);
  
    thrown.expect(FeaturestoreException.class);
    filterController.validateFilterLogicDTO(filterLogicDTO);
  
    // case 3
    // single and left filter null
    filterLogicDTO = new FilterLogicDTO(SqlFilterLogic.SINGLE);
  
    thrown.expect(FeaturestoreException.class);
    filterController.validateFilterLogicDTO(filterLogicDTO);
  }
  
  @Test
  public void testConvertFilterLogic() throws Exception {
    // test successful conversion of FilterLogicDTO with one filter and one logic set
    
    FeatureGroupFeatureDTO featureGroupFeatureDTO =
      new FeatureGroupFeatureDTO("fg1_pk", "string", "");
    FilterDTO filter = new FilterDTO(featureGroupFeatureDTO, SqlFilterCondition.EQUALS, "10");
    FilterLogicDTO filterLogicDTO2 = new FilterLogicDTO(SqlFilterLogic.AND);
  
    FilterLogicDTO filterLogicDTO = new FilterLogicDTO(SqlFilterLogic.AND);
    filterLogicDTO.setLeftFilter(filter);
    filterLogicDTO.setRightLogic(filterLogicDTO2);
  
    FilterLogic result = filterController.convertFilterLogic(filterLogicDTO, fgLookup, availableFeatureLookup);
  
    Assert.assertEquals(result.getType(), SqlFilterLogic.AND);
    Assert.assertEquals(result.getRightLogic().getType(), SqlFilterLogic.AND);
    Assert.assertEquals(SqlFilterCondition.EQUALS, result.getLeftFilter().getCondition());
    Assert.assertEquals("10", result.getLeftFilter().getValue());
    Assert.assertEquals("fg1_pk", result.getLeftFilter().getFeature().getName());
    Assert.assertEquals("fg1", result.getLeftFilter().getFeature().getFgAlias());
    Assert.assertEquals("string", result.getLeftFilter().getFeature().getType());
    Assert.assertNull(result.getLeftFilter().getFeature().getDefaultValue());
  }
  
  @Test
  public void testConvertFilter() throws Exception {
    FeatureGroupFeatureDTO featureGroupFeatureDTO =
      new FeatureGroupFeatureDTO("fg2_ft", "double", "");
    FilterDTO filter = new FilterDTO(featureGroupFeatureDTO, SqlFilterCondition.EQUALS, "10");
    
    Filter result = filterController.convertFilter(filter, fgLookup, availableFeatureLookup);
  
    Assert.assertEquals(SqlFilterCondition.EQUALS, result.getCondition());
    Assert.assertEquals("10", result.getValue());
    Assert.assertEquals("fg2_ft", result.getFeature().getName());
    Assert.assertEquals("fg2", result.getFeature().getFgAlias());
    Assert.assertEquals("double", result.getFeature().getType());
    Assert.assertEquals("10.0",result.getFeature().getDefaultValue());
  }
  
  @Test
  public void testFindFilteredFeatureNoId() throws Exception {
    FeatureGroupFeatureDTO featureGroupFeatureDTO =
      new FeatureGroupFeatureDTO("fg2_ft", "double", "");
  
    Feature result = filterController.findFilteredFeature(featureGroupFeatureDTO, fgLookup, availableFeatureLookup);
  
    Assert.assertEquals("fg2_ft", result.getName());
    Assert.assertEquals("fg2", result.getFgAlias());
    Assert.assertEquals("double", result.getType());
    Assert.assertEquals("10.0",result.getDefaultValue());
  }
  
  @Test
  public void testFindFilteredFeatureWithId() throws Exception {
    FeatureGroupFeatureDTO featureGroupFeatureDTO =
      new FeatureGroupFeatureDTO("fg2_ft", "double", "", 2);
  
    Feature result = filterController.findFilteredFeature(featureGroupFeatureDTO, fgLookup, availableFeatureLookup);
  
    Assert.assertEquals("fg2_ft", result.getName());
    Assert.assertEquals("fg2", result.getFgAlias());
    Assert.assertEquals("double", result.getType());
    Assert.assertEquals("10.0",result.getDefaultValue());
  }
  
  @Test
  public void testFindFilteredFeatureNoIdException() throws Exception {
    FeatureGroupFeatureDTO featureGroupFeatureDTO =
      new FeatureGroupFeatureDTO("not_a_feature", "double", "");
  
    thrown.expect(FeaturestoreException.class);
    filterController.findFilteredFeature(featureGroupFeatureDTO, fgLookup, availableFeatureLookup);
  }
  
  @Test
  public void testFindFilteredFeatureWithIdException() throws Exception {
    FeatureGroupFeatureDTO featureGroupFeatureDTO =
      new FeatureGroupFeatureDTO("not_a_feature_of_fg2", "double", "", 2);
    
    thrown.expect(FeaturestoreException.class);
    filterController.findFilteredFeature(featureGroupFeatureDTO, fgLookup, availableFeatureLookup);
  }
  
  @Test
  public void testGenerateFilterLogicNodeSingle() throws Exception {
    FilterLogic filterLogic = new FilterLogic(SqlFilterLogic.SINGLE);
    Filter filter = new Filter(fg1Features.get(0), SqlFilterCondition.EQUALS, "abc");
    filterLogic.setLeftFilter(filter);
    
    String result = filterController.generateFilterLogicNode(filterLogic, false)
      .toSqlString(new SparkSqlDialect(SqlDialect.EMPTY_CONTEXT)).getSql();
    
    String expected = "`fg1`.`fg1_pk` = 'abc'";
    Assert.assertEquals(expected, result);
  }
  
  @Test
  public void testGenerateFilterLogicNodeCase1() throws Exception {
    FilterLogic filterLogic = new FilterLogic(SqlFilterLogic.AND);
    Filter leftFilter = new Filter(fg1Features.get(0), SqlFilterCondition.EQUALS, "abc");
    Filter rightFilter = new Filter(fg2Features.get(1), SqlFilterCondition.LESS_THAN_OR_EQUAL, "10");
  
    filterLogic.setLeftFilter(leftFilter);
    filterLogic.setRightFilter(rightFilter);
  
    String result =
      filterController.generateFilterLogicNode(filterLogic, false).toSqlString(new SparkSqlDialect(SqlDialect
      .EMPTY_CONTEXT)).getSql();
  
    String expected = "`fg1`.`fg1_pk` = 'abc' " +
      "AND CASE WHEN `fg2`.`fg2_ft` IS NULL THEN 10.0 ELSE `fg2`.`fg2_ft` END <= 10";
    Assert.assertEquals(expected, result);
  }
  
  @Test
  public void testGenerateFilterLogicNodeCase2() throws Exception {
    FilterLogic filterLogic = new FilterLogic(SqlFilterLogic.AND);
    Filter leftFilter = new Filter(fg1Features.get(0), SqlFilterCondition.EQUALS, "abc");
    Filter rightFilter = new Filter(fg2Features.get(1), SqlFilterCondition.LESS_THAN_OR_EQUAL, "10");
    FilterLogic rightLogic = new FilterLogic(SqlFilterLogic.OR);
    Filter middleFilter = new Filter(fg3Features.get(0), SqlFilterCondition.NOT_EQUALS, "abc");
    rightLogic.setLeftFilter(middleFilter);
    rightLogic.setRightFilter(rightFilter);
  
    filterLogic.setLeftFilter(leftFilter);
    filterLogic.setRightLogic(rightLogic);
  
    String result =
      filterController.generateFilterLogicNode(filterLogic, true).toSqlString(new SparkSqlDialect(SqlDialect
        .EMPTY_CONTEXT)).getSql();
  
    String expected = "`fg1`.`fg1_pk` = 'abc' AND (`fg3`.`fg3_pk` <> 'abc' OR `fg2`.`fg2_ft` <= 10)";
    Assert.assertEquals(expected, result);
  }
  
  @Test
  public void testGenerateFilterLogicNodeCase3() throws Exception {
    FilterLogic filterLogic = new FilterLogic(SqlFilterLogic.OR);
    Filter leftFilter = new Filter(fg1Features.get(0), SqlFilterCondition.EQUALS, "abc");
    Filter rightFilter = new Filter(fg2Features.get(1), SqlFilterCondition.LESS_THAN_OR_EQUAL, "10");
    FilterLogic leftLogic = new FilterLogic(SqlFilterLogic.AND);
    Filter middleFilter = new Filter(fg3Features.get(0), SqlFilterCondition.NOT_EQUALS, "abc");
    leftLogic.setLeftFilter(leftFilter);
    leftLogic.setRightFilter(middleFilter);
  
    filterLogic.setLeftLogic(leftLogic);
    filterLogic.setRightFilter(rightFilter);
  
    String result =
      filterController.generateFilterLogicNode(filterLogic, true).toSqlString(new SparkSqlDialect(SqlDialect
        .EMPTY_CONTEXT)).getSql();
  
    String expected = "`fg1`.`fg1_pk` = 'abc' AND `fg3`.`fg3_pk` <> 'abc' OR `fg2`.`fg2_ft` <= 10";
    Assert.assertEquals(expected, result);
  }
  
  @Test
  public void testGenerateFilterLogicNodeCase4() throws Exception {
    FilterLogic filterLogic = new FilterLogic(SqlFilterLogic.OR);
    Filter leftFilter = new Filter(fg1Features.get(0), SqlFilterCondition.EQUALS, "abc");
    Filter rightFilter = new Filter(fg2Features.get(1), SqlFilterCondition.LESS_THAN_OR_EQUAL, "10");
    FilterLogic leftLogic = new FilterLogic(SqlFilterLogic.AND);
    Filter middleFilter = new Filter(fg3Features.get(0), SqlFilterCondition.NOT_EQUALS, "abc");
    Filter middleFilter2 = new Filter(fg3Features.get(1), SqlFilterCondition.NOT_EQUALS, "abc");
    FilterLogic rightLogic = new FilterLogic(SqlFilterLogic.OR);
  
  
    leftLogic.setLeftFilter(leftFilter);
    leftLogic.setRightFilter(middleFilter);
    rightLogic.setLeftFilter(middleFilter2);
    rightLogic.setRightFilter(rightFilter);
  
    filterLogic.setLeftLogic(leftLogic);
    filterLogic.setRightLogic(rightLogic);
  
    String result =
      filterController.generateFilterLogicNode(filterLogic, false).toSqlString(new SparkSqlDialect(SqlDialect
        .EMPTY_CONTEXT)).getSql();
  
    String expected = "`fg1`.`fg1_pk` = 'abc' AND `fg3`.`fg3_pk` <> 'abc' " +
      "OR (CASE WHEN `fg3`.`fg3_ft` IS NULL THEN 'default' ELSE `fg3`.`fg3_ft` END <> 'abc' " +
        "OR CASE WHEN `fg2`.`fg2_ft` IS NULL THEN 10.0 ELSE `fg2`.`fg2_ft` END <= 10)";
    Assert.assertEquals(expected, result);
  }
  
  @Test
  public void testGenerateFilterNodeDefault() {
    Filter filter = new Filter(fg2Features.get(1), SqlFilterCondition.EQUALS, "1");
    
    String result = filterController.generateFilterNode(filter, false).toSqlString(new SparkSqlDialect(SqlDialect
      .EMPTY_CONTEXT)).getSql();
    
    String expected = "CASE WHEN `fg2`.`fg2_ft` IS NULL THEN 10.0 ELSE `fg2`.`fg2_ft` END = 1";
    Assert.assertEquals(expected, result);
  }
  
  @Test
  public void testGenerateFilterNodeOnline() {
    Filter filter = new Filter(fg2Features.get(1), SqlFilterCondition.EQUALS, "1");
  
    String result = filterController.generateFilterNode(filter, true).toSqlString(new SparkSqlDialect(SqlDialect
      .EMPTY_CONTEXT)).getSql();
  
    String expected = "`fg2`.`fg2_ft` = 1";
    Assert.assertEquals(expected, result);
  }
  
  @Test
  public void testGenerateFilterNodeStringType() {
    Filter filter = new Filter(fg1Features.get(0), SqlFilterCondition.NOT_EQUALS, "abc");
  
    String result = filterController.generateFilterNode(filter, false).toSqlString(new SparkSqlDialect(SqlDialect
      .EMPTY_CONTEXT)).getSql();
  
    String expected = "`fg1`.`fg1_pk` <> 'abc'";
    Assert.assertEquals(expected, result);
  }
  
  @Test
  public void testGenerateFilterNodeNumericType() {
    Filter filter = new Filter(fg2Features.get(1), SqlFilterCondition.NOT_EQUALS, "10");
  
    String result = filterController.generateFilterNode(filter, false).toSqlString(new SparkSqlDialect(SqlDialect
      .EMPTY_CONTEXT)).getSql();
  
    String expected = "CASE WHEN `fg2`.`fg2_ft` IS NULL THEN 10.0 ELSE `fg2`.`fg2_ft` END <> 10";
    Assert.assertEquals(expected, result);
  }
  
  @Test
  public void testBuildFilterNodeSingleJoin() throws Exception {
    Query leftQuery = new Query("fs1", "project_fs1", fg1, "fg0", fg1Features, fg1Features);
    Query secondQuery = new Query("fs1", "project_fs1", fg2, "fg1", fg2Features , fg2Features);
  
    FilterLogic firstFilter = new FilterLogic(SqlFilterLogic.AND);
    firstFilter.setLeftFilter(new Filter(fg1Features.get(1), SqlFilterCondition.EQUALS, "10"));
    FilterLogic rightLogic = new FilterLogic(SqlFilterLogic.OR);
    rightLogic.setLeftFilter(new Filter(fg3Features.get(1), SqlFilterCondition.EQUALS, "10"));
    rightLogic.setRightFilter(new Filter(fg3Features.get(2), SqlFilterCondition.EQUALS, "10"));
    firstFilter.setRightLogic(rightLogic);
    leftQuery.setFilter(firstFilter);
  
    FilterLogic secondFilter = new FilterLogic(SqlFilterLogic.SINGLE);
    secondFilter.setLeftFilter(new Filter(fg2Features.get(1), SqlFilterCondition.NOT_EQUALS, "10"));
    secondQuery.setFilter(secondFilter);
  
    Join join = new Join(leftQuery, secondQuery, joinFeatures, JoinType.INNER);
    leftQuery.setJoins(Arrays.asList(join));
  
    String result =
      filterController.buildFilterNode(leftQuery, leftQuery, leftQuery.getJoins().size() - 1, false)
        .toSqlString(new SparkSqlDialect(SqlDialect.EMPTY_CONTEXT)).getSql();
    String expected = "`fg1`.`fg1_ft` = 10 " +
      "AND (CASE WHEN `fg3`.`fg3_ft` IS NULL THEN 'default' ELSE `fg3`.`fg3_ft` END = '10' OR `fg3`.`join` = '10') " +
      "AND CASE WHEN `fg2`.`fg2_ft` IS NULL THEN 10.0 ELSE `fg2`.`fg2_ft` END <> 10";
  
    Assert.assertEquals(expected, result);
  }
  
  @Test
  public void testBuildFilterNodeTripleJoin() throws Exception {
    Query leftQuery = new Query("fs1", "project_fs1", fg1, "fg0", fg1Features, fg1Features);
    Query secondQuery = new Query("fs1", "project_fs1", fg2, "fg1", fg2Features , fg2Features);
    Query thirdQuery = new Query("fs1", "project_fs1", fg3,"fg2", fg3Features, fg3Features);
  
    FilterLogic firstFilter = new FilterLogic(SqlFilterLogic.AND);
    firstFilter.setLeftFilter(new Filter(fg1Features.get(1), SqlFilterCondition.EQUALS, "10"));
    FilterLogic rightLogic = new FilterLogic(SqlFilterLogic.OR);
    rightLogic.setLeftFilter(new Filter(fg3Features.get(1), SqlFilterCondition.EQUALS, "10"));
    rightLogic.setRightFilter(new Filter(fg3Features.get(2), SqlFilterCondition.EQUALS, "10"));
    firstFilter.setRightLogic(rightLogic);
    leftQuery.setFilter(firstFilter);
    
    FilterLogic secondFilter = new FilterLogic(SqlFilterLogic.SINGLE);
    secondFilter.setLeftFilter(new Filter(fg2Features.get(1), SqlFilterCondition.NOT_EQUALS, "10"));
    secondQuery.setFilter(secondFilter);
    
    Join join = new Join(leftQuery, secondQuery, joinFeatures, JoinType.INNER);
    Join secondJoin = new Join(leftQuery, thirdQuery, leftOn, rightOn, JoinType.INNER);
    leftQuery.setJoins(Arrays.asList(join, secondJoin));
    
    String result =
      filterController.buildFilterNode(leftQuery, leftQuery, leftQuery.getJoins().size() - 1, false)
        .toSqlString(new SparkSqlDialect(SqlDialect.EMPTY_CONTEXT)).getSql();
    String expected = "`fg1`.`fg1_ft` = 10 " +
      "AND (CASE WHEN `fg3`.`fg3_ft` IS NULL THEN 'default' ELSE `fg3`.`fg3_ft` END = '10' OR `fg3`.`join` = '10') " +
      "AND CASE WHEN `fg2`.`fg2_ft` IS NULL THEN 10.0 ELSE `fg2`.`fg2_ft` END <> 10";
    
    Assert.assertEquals(expected, result);
  }
}
