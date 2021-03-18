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

package io.hops.hopsworks.common.featurestore.featuregroup.cached;


import io.hops.hopsworks.common.featurestore.feature.FeatureGroupFeatureDTO;
import io.hops.hopsworks.exceptions.FeaturestoreException;
import io.hops.hopsworks.persistence.entity.featurestore.featuregroup.cached.TimeTravelFormat;
import org.apache.calcite.sql.SqlDialect;
import org.apache.calcite.sql.dialect.HiveSqlDialect;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import java.util.ArrayList;
import java.util.List;

public class TestCachedFeatureGroupController {

  List<FeatureGroupFeatureDTO> features = new ArrayList<>();
  List<FeatureGroupFeatureDTO> features2 = new ArrayList<>();
  List<FeatureGroupFeatureDTO> partitionFeatures = new ArrayList<>();
  
  private CachedFeaturegroupController cachedFeaturegroupController = new CachedFeaturegroupController();
  
  @Rule
  public ExpectedException thrown = ExpectedException.none();
  
  @Before
  public void setup() {
    features = new ArrayList<>();
    features.add(new FeatureGroupFeatureDTO("part_param", "Integer", "", true, false));
    features.add(new FeatureGroupFeatureDTO("part_param2", "String", "", false, false));
  
    features2 = new ArrayList<>();
    features2.add(new FeatureGroupFeatureDTO("part_param", "String", "", true, false));
    features2.add(new FeatureGroupFeatureDTO("part_param2", "String", "", false, false));

    partitionFeatures.add(new FeatureGroupFeatureDTO("part_param", "Integer", "", false, true));
    partitionFeatures.add(new FeatureGroupFeatureDTO("part_param2", "String", "", false, true));
    partitionFeatures.add(new FeatureGroupFeatureDTO("part_param3", "String", "", false, true));
  }

  @Test
  public void testPreviewWhereSingle() throws Exception {
    String queryPart = "part_param=3";
    String output = cachedFeaturegroupController.getWhereCondition(queryPart, partitionFeatures)
      .toSqlString(new HiveSqlDialect(SqlDialect.EMPTY_CONTEXT)).getSql();
    Assert.assertEquals("`part_param` = 3", output);
  }

  @Test
  public void testPreviewWhereDouble() throws Exception {
    String queryPart = "part_param=3/part_param2=hello";
    String output = cachedFeaturegroupController.getWhereCondition(queryPart, partitionFeatures)
      .toSqlString(new HiveSqlDialect(SqlDialect.EMPTY_CONTEXT)).getSql();
    Assert.assertEquals("`part_param` = 3 AND `part_param2` = 'hello'", output);
  }

  @Test
  public void testPreviewWhereDoubleSpace() throws Exception {
    String queryPart = "part_param2=3 4/part_param3=hello";
    String output = cachedFeaturegroupController.getWhereCondition(queryPart, partitionFeatures)
      .toSqlString(new HiveSqlDialect(SqlDialect.EMPTY_CONTEXT)).getSql();
    Assert.assertEquals("`part_param2` = '3 4' AND `part_param3` = 'hello'", output);
  }

  @Test
  public void testPreviewWhereDoubleEquals() throws Exception {
    String queryPart = "part_param2=3=4/part_param3=hello";
    String output = cachedFeaturegroupController.getWhereCondition(queryPart, partitionFeatures)
      .toSqlString(new HiveSqlDialect(SqlDialect.EMPTY_CONTEXT)).getSql();
    Assert.assertEquals("`part_param2` = '3=4' AND `part_param3` = 'hello'", output);
  }

  @Test
  public void testPreviewWhereNoPartitionColumn() throws Exception {
    String queryPart = "part_param=3";
    thrown.expect(FeaturestoreException.class);
    String output = cachedFeaturegroupController.getWhereCondition(queryPart, features)
      .toSqlString(new HiveSqlDialect(SqlDialect.EMPTY_CONTEXT)).getSql();
  }
  
  @Test
  public void testVerifyPreviousSchemaUnchangedIfFeaturesChanged() throws Exception {
    List<FeatureGroupFeatureDTO> newSchema = new ArrayList<>();
    newSchema.add(new FeatureGroupFeatureDTO("part_param", "Integer", "", true, false));
    
    thrown.expect(FeaturestoreException.class);
    cachedFeaturegroupController.verifyPreviousSchemaUnchanged(features, newSchema);
  }
  
  @Test
  public void testVerifyPreviousSchemaUnchangedIfPartitionChanged() throws Exception {
    List<FeatureGroupFeatureDTO> newSchema = new ArrayList<>();
    newSchema.add(new FeatureGroupFeatureDTO("part_param", "Integer", "", true, false));
    newSchema.add(new FeatureGroupFeatureDTO("part_param2", "String", "", false , true));
  
    thrown.expect(FeaturestoreException.class);
    cachedFeaturegroupController.verifyPreviousSchemaUnchanged(features, newSchema);
  }
  
  @Test
  public void testVerifyPreviousSchemaUnchangedIfPrimaryChanged() throws Exception {
    List<FeatureGroupFeatureDTO> newSchema = new ArrayList<>();
    newSchema.add(new FeatureGroupFeatureDTO("part_param", "Integer", "", false, false));
    newSchema.add(new FeatureGroupFeatureDTO("part_param2", "String", "", true , false));
    
    thrown.expect(FeaturestoreException.class);
    cachedFeaturegroupController.verifyPreviousSchemaUnchanged(features, newSchema);
  }
  
  @Test
  public void testVerifyPreviousSchemaUnchangedIfTypeChanged() throws Exception {
    List<FeatureGroupFeatureDTO> newSchema = new ArrayList<>();
    newSchema.add(new FeatureGroupFeatureDTO("part_param", "String", "", true, false));
    newSchema.add(new FeatureGroupFeatureDTO("part_param2", "String", "", false , false));
    
    thrown.expect(FeaturestoreException.class);
    cachedFeaturegroupController.verifyPreviousSchemaUnchanged(features, newSchema);
  }
  
  @Test
  public void testVerifyAndGetNewFeaturesIfPrimary() throws Exception {
    List<FeatureGroupFeatureDTO> newSchema = new ArrayList<>();
    newSchema.add(new FeatureGroupFeatureDTO("part_param", "Integer", "", true, false));
    newSchema.add(new FeatureGroupFeatureDTO("part_param2", "String", "", false , false));
    newSchema.add(new FeatureGroupFeatureDTO("part_param3", "String", "", true , false));
  
    thrown.expect(FeaturestoreException.class);
    cachedFeaturegroupController.verifyAndGetNewFeatures(features, newSchema);
  }
  
  @Test
  public void testVerifyAndGetNewFeaturesIfPartition() throws Exception {
    List<FeatureGroupFeatureDTO> newSchema = new ArrayList<>();
    newSchema.add(new FeatureGroupFeatureDTO("part_param", "Integer", "", true, false));
    newSchema.add(new FeatureGroupFeatureDTO("part_param2", "String", "", false , false));
    newSchema.add(new FeatureGroupFeatureDTO("part_param3", "String", "", false , true));
    
    thrown.expect(FeaturestoreException.class);
    cachedFeaturegroupController.verifyAndGetNewFeatures(features, newSchema);
  }
  
  @Test
  public void testVerifyAndGetNewFeaturesIfMissingType() throws Exception {
    List<FeatureGroupFeatureDTO> newSchema = new ArrayList<>();
    newSchema.add(new FeatureGroupFeatureDTO("part_param", "Integer", "", true, false));
    newSchema.add(new FeatureGroupFeatureDTO("part_param2", "String", "", false , false));
    newSchema.add(new FeatureGroupFeatureDTO("part_param3", null, "", false , false));
    
    thrown.expect(FeaturestoreException.class);
    cachedFeaturegroupController.verifyAndGetNewFeatures(features, newSchema);
  }
  
  @Test
  public void testVerifyPrimaryKeyHudi() throws Exception {
    List<FeatureGroupFeatureDTO> newSchema = new ArrayList<>();
    newSchema.add(new FeatureGroupFeatureDTO("part_param", "Integer", "", false, true));

    thrown.expect(FeaturestoreException.class);
    cachedFeaturegroupController.verifyPrimaryKey(newSchema, TimeTravelFormat.HUDI);
  }
}