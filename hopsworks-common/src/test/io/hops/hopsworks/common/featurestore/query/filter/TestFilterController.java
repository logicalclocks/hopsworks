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

import com.fasterxml.jackson.databind.ObjectMapper;
import io.hops.hopsworks.common.featurestore.feature.FeatureGroupFeatureDTO;
import io.hops.hopsworks.common.featurestore.query.ConstructorController;
import io.hops.hopsworks.common.featurestore.query.Feature;
import io.hops.hopsworks.common.featurestore.query.Query;
import io.hops.hopsworks.persistence.entity.featurestore.trainingdataset.SqlCondition;
import io.hops.hopsworks.common.featurestore.query.join.Join;
import io.hops.hopsworks.exceptions.FeaturestoreException;
import io.hops.hopsworks.persistence.entity.featurestore.Featurestore;
import io.hops.hopsworks.persistence.entity.featurestore.featuregroup.Featuregroup;
import io.hops.hopsworks.persistence.entity.featurestore.featuregroup.cached.CachedFeaturegroup;
import io.hops.hopsworks.persistence.entity.featurestore.featuregroup.cached.TimeTravelFormat;
import io.hops.hopsworks.persistence.entity.featurestore.trainingdataset.SqlFilterLogic;
import io.hops.hopsworks.persistence.entity.project.Project;
import org.apache.calcite.sql.JoinType;
import org.apache.calcite.sql.SqlDialect;
import org.apache.calcite.sql.SqlNode;
import org.apache.calcite.sql.dialect.HiveSqlDialect;
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
  private ObjectMapper objectMapper = new ObjectMapper();
  
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
    fg1Features.add(new Feature("fg1_pk", "fg1", "string",true, null, "prefix2_", fg1));
    fg1Features.add(new Feature("fg1_ft", "fg1", "integer", false, null, "prefix2_", fg1));
    fg1Features.add(new Feature("join", "fg1", "string",true, null, "prefix2_", fg1));
  
    fg2Features = new ArrayList<>();
    fg2Features.add(new Feature("fg2_pk", "fg2", "string", true, null, "prefix2_"));
    fg2Features.add(new Feature("fg2_ft", "fg2", "double", false, "10.0", "prefix2_"));
    fg3Features.add(new Feature("join", "fg2", "string",true, null, "prefix2_"));
  
    fg3Features = new ArrayList<>();
    fg3Features.add(new Feature("fg3_pk", "fg3", "string", true, null, "prefix3_"));
    fg3Features.add(new Feature("fg3_ft", "fg3", "string",false, "default", "prefix3_"));
    fg3Features.add(new Feature("join", "fg3", "string",true, null, "prefix3_"));

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
    FilterDTO filter = new FilterDTO(featureGroupFeatureDTO, SqlCondition.EQUALS, "10");
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
    FilterDTO filter = new FilterDTO(featureGroupFeatureDTO, SqlCondition.EQUALS, "10");
    FilterLogicDTO filterLogicDTO2 = new FilterLogicDTO(SqlFilterLogic.AND);
  
    FilterLogicDTO filterLogicDTO = new FilterLogicDTO(SqlFilterLogic.AND);
    filterLogicDTO.setLeftFilter(filter);
    filterLogicDTO.setRightLogic(filterLogicDTO2);
  
    FilterLogic result = filterController.convertFilterLogic(filterLogicDTO, availableFeatureLookup);
    Feature feature = result.getLeftFilter().getFeatures().get(0);
  
    Assert.assertEquals(result.getType(), SqlFilterLogic.AND);
    Assert.assertEquals(result.getRightLogic().getType(), SqlFilterLogic.AND);
    Assert.assertEquals(SqlCondition.EQUALS, result.getLeftFilter().getCondition());
    Assert.assertEquals("10", result.getLeftFilter().getValue().getValue());
    Assert.assertEquals("fg1_pk", feature.getName());
    Assert.assertEquals("fg1", feature.getFgAlias(false));
    Assert.assertEquals("string", feature.getType());
    Assert.assertNull(feature.getDefaultValue());
  }
  
  @Test
  public void testConvertFilter() throws Exception {
    FeatureGroupFeatureDTO featureGroupFeatureDTO =
      new FeatureGroupFeatureDTO("fg2_ft", "double", "");
    FilterDTO filter = new FilterDTO(featureGroupFeatureDTO, SqlCondition.EQUALS, "10");
    
    Filter result = filterController.convertFilter(filter, availableFeatureLookup);
    Feature feature = result.getFeatures().get(0);
  
    Assert.assertEquals(SqlCondition.EQUALS, result.getCondition());
    Assert.assertEquals("10", result.getValue().getValue());
    Assert.assertEquals("fg2_ft", feature.getName());
    Assert.assertEquals("fg2", feature.getFgAlias(false));
    Assert.assertEquals("double", feature.getType());
    Assert.assertEquals("10.0", feature.getDefaultValue());
  }
  
  @Test
  public void testConvertFilterValue_feature() throws Exception {
    FeatureGroupFeatureDTO value =
        new FeatureGroupFeatureDTO("fg1_ft", "integer", null, null, 1);

    FilterValue result = filterController.convertFilterValue(objectMapper.writeValueAsString(value),
        availableFeatureLookup);
    
    Assert.assertEquals("`fg1`.`fg1_ft`", result.makeSqlValue());
  }
  
  @Test
  public void testConvertFilterValue_nonFeature() throws Exception {
    FilterValue resultOfIntegerValue = filterController.convertFilterValue("123",
        availableFeatureLookup);
    FilterValue resultOfStringValue = filterController.convertFilterValue("abc",
        availableFeatureLookup);
    FilterValue resultOfArrayValue = filterController.convertFilterValue("[1, 2, 3]",
        availableFeatureLookup);
    
    Assert.assertEquals("123", resultOfIntegerValue.makeSqlValue());
    Assert.assertEquals("abc", resultOfStringValue.makeSqlValue());
    Assert.assertEquals("[1, 2, 3]", resultOfArrayValue.makeSqlValue());
  }
  
  @Test
  public void testFindFilteredFeatureNoId() throws Exception {
    FeatureGroupFeatureDTO featureGroupFeatureDTO =
      new FeatureGroupFeatureDTO("fg2_ft", "double", "");
  
    Feature result = filterController.findFilteredFeature(featureGroupFeatureDTO, availableFeatureLookup);
  
    Assert.assertEquals("fg2_ft", result.getName());
    Assert.assertEquals("fg2", result.getFgAlias(false));
    Assert.assertEquals("double", result.getType());
    Assert.assertEquals("10.0",result.getDefaultValue());
  }
  
  @Test
  public void testFindFilteredFeatureWithId() throws Exception {
    FeatureGroupFeatureDTO featureGroupFeatureDTO =
      new FeatureGroupFeatureDTO("fg2_ft", "double", "", 2, true);
  
    Feature result = filterController.findFilteredFeature(featureGroupFeatureDTO, availableFeatureLookup);
  
    Assert.assertEquals("fg2_ft", result.getName());
    Assert.assertEquals("fg2", result.getFgAlias(false));
    Assert.assertEquals("double", result.getType());
    Assert.assertEquals("10.0",result.getDefaultValue());
  }
  
  @Test
  public void testFindFilteredFeatureNoIdException() throws Exception {
    FeatureGroupFeatureDTO featureGroupFeatureDTO =
      new FeatureGroupFeatureDTO("not_a_feature", "double", "");
  
    thrown.expect(FeaturestoreException.class);
    filterController.findFilteredFeature(featureGroupFeatureDTO, availableFeatureLookup);
  }
  
  @Test
  public void testFindFilteredFeatureWithIdException() throws Exception {
    FeatureGroupFeatureDTO featureGroupFeatureDTO =
      new FeatureGroupFeatureDTO("not_a_feature_of_fg2", "double", "", 2, true);
    
    thrown.expect(FeaturestoreException.class);
    filterController.findFilteredFeature(featureGroupFeatureDTO, availableFeatureLookup);
  }

  @Test
  public void testFindFilteredFeatureWithIdNotAvailableException() throws Exception {
    FeatureGroupFeatureDTO featureGroupFeatureDTO =
        new FeatureGroupFeatureDTO("not_a_feature_of_fg2", "double", "", 3, true);

    thrown.expect(FeaturestoreException.class);
    filterController.findFilteredFeature(featureGroupFeatureDTO, availableFeatureLookup);
  }
  
  @Test
  public void testGenerateFilterLogicNodeSingle() throws Exception {
    FilterLogic filterLogic = new FilterLogic(SqlFilterLogic.SINGLE);
    Filter filter = new Filter(Arrays.asList(fg1Features.get(0)), SqlCondition.EQUALS, "abc");
    filterLogic.setLeftFilter(filter);
    
    String result = filterController.generateFilterLogicNode(filterLogic, false)
      .toSqlString(new SparkSqlDialect(SqlDialect.EMPTY_CONTEXT)).getSql();
    
    String expected = "`fg1`.`fg1_pk` = 'abc'";
    Assert.assertEquals(expected, result);
  }
  
  @Test
  public void testGenerateFilterLogicNodeCase1() throws Exception {
    FilterLogic filterLogic = new FilterLogic(SqlFilterLogic.AND);
    Filter leftFilter = new Filter(Arrays.asList(fg1Features.get(0)), SqlCondition.EQUALS, "abc");
    Filter rightFilter = new Filter(Arrays.asList(fg2Features.get(1)), SqlCondition.LESS_THAN_OR_EQUAL, "10");
  
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
    Filter leftFilter = new Filter(Arrays.asList(fg1Features.get(0)), SqlCondition.EQUALS, "abc");
    Filter rightFilter = new Filter(Arrays.asList(fg2Features.get(1)), SqlCondition.LESS_THAN_OR_EQUAL, "10");
    FilterLogic rightLogic = new FilterLogic(SqlFilterLogic.OR);
    Filter middleFilter = new Filter(Arrays.asList(fg3Features.get(0)), SqlCondition.NOT_EQUALS, "abc");
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
    Filter leftFilter = new Filter(Arrays.asList(fg1Features.get(0)), SqlCondition.EQUALS, "abc");
    Filter rightFilter = new Filter(Arrays.asList(fg2Features.get(1)), SqlCondition.LESS_THAN_OR_EQUAL, "10");
    FilterLogic leftLogic = new FilterLogic(SqlFilterLogic.AND);
    Filter middleFilter = new Filter(Arrays.asList(fg3Features.get(0)), SqlCondition.NOT_EQUALS, "abc");
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
    Filter leftFilter = new Filter(Arrays.asList(fg1Features.get(0)), SqlCondition.EQUALS, "abc");
    Filter rightFilter = new Filter(Arrays.asList(fg2Features.get(1)), SqlCondition.LESS_THAN_OR_EQUAL, "10");
    FilterLogic leftLogic = new FilterLogic(SqlFilterLogic.AND);
    Filter middleFilter = new Filter(Arrays.asList(fg3Features.get(0)), SqlCondition.NOT_EQUALS, "abc");
    Filter middleFilter2 = new Filter(Arrays.asList(fg3Features.get(1)), SqlCondition.NOT_EQUALS, "abc");
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
  public void testGenerateFilterLogicNodeInOnline() {
    FilterLogic filter = new FilterLogic(new Filter(fg2Features, SqlCondition.IN, "?"));
    String result = filterController.generateFilterLogicNode(filter, true).toSqlString(new SparkSqlDialect(SqlDialect
      .EMPTY_CONTEXT)).getSql();
    System.out.println(result);
    String expected = "(`fg2`.`fg2_pk`, `fg2`.`fg2_ft`) IN ?";
    Assert.assertEquals(expected, result);
  }

  @Test
  public void testGenerateFilterLogicNodeSingleIN() throws Exception {
    FilterLogic filterLogic = new FilterLogic(SqlFilterLogic.SINGLE);
    Filter filter = new Filter(Arrays.asList(fg1Features.get(0)), SqlCondition.IN, "[\"ab\", \"cd\"]");
    filterLogic.setLeftFilter(filter);
    
    String result = filterController.generateFilterLogicNode(filterLogic, false)
      .toSqlString(new SparkSqlDialect(SqlDialect.EMPTY_CONTEXT)).getSql();
    
    String expected = "`fg1`.`fg1_pk` IN ('ab', 'cd')";
    Assert.assertEquals(expected, result);
  }

  @Test
  public void testGenerateFilterLogicNodeSingleLIKE() throws Exception {
    Filter filter = new Filter(Arrays.asList(fg1Features.get(0)), SqlCondition.LIKE, "%abc%");

    String result = filterController.generateFilterNode(filter, false).toSqlString(new SparkSqlDialect(SqlDialect
        .EMPTY_CONTEXT)).getSql();

    String expected = "`fg1`.`fg1_pk` LIKE '%abc%'";
    Assert.assertEquals(expected, result);
  }
  
  @Test
  public void testGenerateFilterNodeDefault() {
    Filter filter = new Filter(Arrays.asList(fg2Features.get(1)), SqlCondition.EQUALS, "1");
    
    String result = filterController.generateFilterNode(filter, false).toSqlString(new SparkSqlDialect(SqlDialect
      .EMPTY_CONTEXT)).getSql();
    
    String expected = "CASE WHEN `fg2`.`fg2_ft` IS NULL THEN 10.0 ELSE `fg2`.`fg2_ft` END = 1";
    Assert.assertEquals(expected, result);
  }
  
  @Test
  public void testGenerateFilterNodeOnline() {
    Filter filter = new Filter(Arrays.asList(fg2Features.get(1)), SqlCondition.EQUALS, "1");
  
    String result = filterController.generateFilterNode(filter, true).toSqlString(new SparkSqlDialect(SqlDialect
      .EMPTY_CONTEXT)).getSql();
  
    String expected = "`fg2`.`fg2_ft` = 1";
    Assert.assertEquals(expected, result);
  }
  
  @Test
  public void testGenerateFilterNodeStringType() {
    Filter filter = new Filter(Arrays.asList(fg1Features.get(0)), SqlCondition.NOT_EQUALS, "abc");
  
    String result = filterController.generateFilterNode(filter, false).toSqlString(new SparkSqlDialect(SqlDialect
      .EMPTY_CONTEXT)).getSql();
  
    String expected = "`fg1`.`fg1_pk` <> 'abc'";
    Assert.assertEquals(expected, result);
  }

  @Test
  public void testGenerateFilterNodeStringArrayType() {
    Filter filter = new Filter(Arrays.asList(fg1Features.get(0)), SqlCondition.IN, "[\"ab\", \"cd\"]");

    String result = filterController.generateFilterNode(filter, false).toSqlString(new SparkSqlDialect(SqlDialect
            .EMPTY_CONTEXT)).getSql();

    String expected = "`fg1`.`fg1_pk` IN ('ab', 'cd')";
    Assert.assertEquals(expected, result);
  }

  @Test
  public void testGenerateFilterNodeNumericType() {
    Filter filter = new Filter(Arrays.asList(fg2Features.get(1)), SqlCondition.NOT_EQUALS, "10");
  
    String result = filterController.generateFilterNode(filter, false).toSqlString(new SparkSqlDialect(SqlDialect
      .EMPTY_CONTEXT)).getSql();
  
    String expected = "CASE WHEN `fg2`.`fg2_ft` IS NULL THEN 10.0 ELSE `fg2`.`fg2_ft` END <> 10";
    Assert.assertEquals(expected, result);
  }

  @Test
  public void testGenerateFilterNodeNumericArrayType() {
    Filter filter = new Filter(Arrays.asList(fg2Features.get(1)), SqlCondition.IN, "[1, 9]");

    String result = filterController.generateFilterNode(filter, false).toSqlString(new SparkSqlDialect(SqlDialect
            .EMPTY_CONTEXT)).getSql();

    String expected = "CASE WHEN `fg2`.`fg2_ft` IS NULL THEN 10.0 ELSE `fg2`.`fg2_ft` END IN (1.0, 9.0)";
    Assert.assertEquals(expected, result);
  }
  
  @Test
  public void testBuildFilterNodeSingleJoin() throws Exception {
    Query leftQuery = new Query("fs1", "project_fs1", fg1, "fg0", fg1Features, fg1Features);
    Query secondQuery = new Query("fs1", "project_fs1", fg2, "fg1", fg2Features , fg2Features);
  
    FilterLogic firstFilter = new FilterLogic(SqlFilterLogic.AND);
    firstFilter.setLeftFilter(new Filter(Arrays.asList(fg1Features.get(1)), SqlCondition.EQUALS, "10"));
    FilterLogic rightLogic = new FilterLogic(SqlFilterLogic.OR);
    rightLogic.setLeftFilter(new Filter(Arrays.asList(fg3Features.get(1)), SqlCondition.EQUALS, "10"));
    rightLogic.setRightFilter(new Filter(Arrays.asList(fg3Features.get(2)), SqlCondition.EQUALS, "10"));
    firstFilter.setRightLogic(rightLogic);
    leftQuery.setFilter(firstFilter);
  
    FilterLogic secondFilter = new FilterLogic(SqlFilterLogic.SINGLE);
    secondFilter.setLeftFilter(new Filter(Arrays.asList(fg2Features.get(1)), SqlCondition.NOT_EQUALS, "10"));
    secondQuery.setFilter(secondFilter);
  
    Join join = new Join(leftQuery, secondQuery, joinFeatures, joinFeatures, JoinType.INNER, null,
      Arrays.asList(SqlCondition.EQUALS));
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
    firstFilter.setLeftFilter(new Filter(Arrays.asList(fg1Features.get(1)), SqlCondition.EQUALS, "10"));
    FilterLogic rightLogic = new FilterLogic(SqlFilterLogic.OR);
    rightLogic.setLeftFilter(new Filter(Arrays.asList(fg3Features.get(1)), SqlCondition.EQUALS, "10"));
    rightLogic.setRightFilter(new Filter(Arrays.asList(fg3Features.get(2)), SqlCondition.EQUALS, "10"));
    firstFilter.setRightLogic(rightLogic);
    leftQuery.setFilter(firstFilter);
    
    FilterLogic secondFilter = new FilterLogic(SqlFilterLogic.SINGLE);
    secondFilter.setLeftFilter(new Filter(Arrays.asList(fg2Features.get(1)), SqlCondition.NOT_EQUALS, "10"));
    secondQuery.setFilter(secondFilter);
    
    Join join = new Join(leftQuery, secondQuery, joinFeatures, joinFeatures, JoinType.INNER, null,
      Arrays.asList(SqlCondition.EQUALS));
    Join secondJoin = new Join(leftQuery, thirdQuery, leftOn, rightOn, JoinType.INNER, null,
      Arrays.asList(SqlCondition.EQUALS, SqlCondition.EQUALS));
    leftQuery.setJoins(Arrays.asList(join, secondJoin));
    
    String result =
      filterController.buildFilterNode(leftQuery, leftQuery, leftQuery.getJoins().size() - 1, false)
        .toSqlString(new SparkSqlDialect(SqlDialect.EMPTY_CONTEXT)).getSql();
    String expected = "`fg1`.`fg1_ft` = 10 " +
      "AND (CASE WHEN `fg3`.`fg3_ft` IS NULL THEN 'default' ELSE `fg3`.`fg3_ft` END = '10' OR `fg3`.`join` = '10') " +
      "AND CASE WHEN `fg2`.`fg2_ft` IS NULL THEN 10.0 ELSE `fg2`.`fg2_ft` END <> 10";
    
    Assert.assertEquals(expected, result);
  }
  
  @Test
  public void testGetSQLNodeString() throws Exception {
    SqlNode node = filterController.getSQLNode("string", "value_string");
    String result = node.toSqlString(new SparkSqlDialect(SqlDialect.EMPTY_CONTEXT)).getSql();
    String expected = "'value_string'";
    
    Assert.assertEquals(expected, result);
  
    result = node.toSqlString(new HiveSqlDialect(SqlDialect.EMPTY_CONTEXT)).getSql();
    Assert.assertEquals(expected, result);
  }
  
  @Test
  public void testGetSQLNodeDate() throws Exception {
    SqlNode node = filterController.getSQLNode("date", "2021-11-12");
    String result = node.toSqlString(new SparkSqlDialect(SqlDialect.EMPTY_CONTEXT)).getSql();
    String expected = "DATE '2021-11-12'";
  
    Assert.assertEquals(expected, result);
  
    result = node.toSqlString(new HiveSqlDialect(SqlDialect.EMPTY_CONTEXT)).getSql();
    Assert.assertEquals(expected, result);
  }
  
  @Test
  public void testGetSQLNodeTimestamp() throws Exception {
    SqlNode node = filterController.getSQLNode("timestamp", "2021-11-12 09:55:32.084354");
    String result = node.toSqlString(new SparkSqlDialect(SqlDialect.EMPTY_CONTEXT)).getSql();
    String expected = "TIMESTAMP '2021-11-12 09:55:32.084'";
  
    Assert.assertEquals(expected, result);
  
    result = node.toSqlString(new HiveSqlDialect(SqlDialect.EMPTY_CONTEXT)).getSql();
    Assert.assertEquals(expected, result);
  }
  
  @Test
  public void testGetSQLNodeOther() throws Exception {
    SqlNode node = filterController.getSQLNode("int", "5");
    String result = node.toSqlString(new SparkSqlDialect(SqlDialect.EMPTY_CONTEXT)).getSql();
    String expected = "5";
  
    Assert.assertEquals(expected, result);
  
    result = node.toSqlString(new HiveSqlDialect(SqlDialect.EMPTY_CONTEXT)).getSql();
    Assert.assertEquals(expected, result);
  }
 }
