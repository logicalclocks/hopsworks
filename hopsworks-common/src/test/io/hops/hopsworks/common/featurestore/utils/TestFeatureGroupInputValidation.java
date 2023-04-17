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

package io.hops.hopsworks.common.featurestore.utils;

import io.hops.hopsworks.common.featurestore.feature.FeatureGroupFeatureDTO;
import io.hops.hopsworks.common.featurestore.featuregroup.FeatureGroupInputValidation;
import io.hops.hopsworks.common.featurestore.featuregroup.cached.CachedFeaturegroupDTO;
import io.hops.hopsworks.common.featurestore.featuregroup.ondemand.OnDemandFeaturegroupDTO;
import io.hops.hopsworks.common.featurestore.featuregroup.online.OnlineFeaturegroupController;
import io.hops.hopsworks.common.featurestore.featuregroup.stream.StreamFeatureGroupDTO;
import io.hops.hopsworks.exceptions.FeaturestoreException;
import io.hops.hopsworks.persistence.entity.featurestore.featuregroup.cached.TimeTravelFormat;
import org.apache.commons.lang.StringUtils;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class TestFeatureGroupInputValidation {
  
  private FeatureGroupInputValidation featureGroupInputValidation =
    new FeatureGroupInputValidation(new FeaturestoreInputValidation(), new OnlineFeaturegroupController());
  
  List<FeatureGroupFeatureDTO> features;
  
  @Rule
  public ExpectedException thrown = ExpectedException.none();
  
  @Before
  public void setup() {
    features = new ArrayList<>();
    features.add(new FeatureGroupFeatureDTO("feature", "TIMESTAMP", "", true, false, "10", null));
    features.add(new FeatureGroupFeatureDTO("feature2", "String", "", false, false, null, null));
  }
  
  @Test
  public void testVerifyEventTimeFeature() throws Exception {
    featureGroupInputValidation.verifyEventTimeFeature("feature", features);
  }
  
  @Test
  public void testVerifyEventTimeFeatureType() throws Exception {
    thrown.expect(FeaturestoreException.class);
    featureGroupInputValidation.verifyEventTimeFeature("feature2", features);
  }
  
  @Test
  public void testVerifyEventTimeUnavailable() throws Exception {
    thrown.expect(FeaturestoreException.class);
    featureGroupInputValidation.verifyEventTimeFeature("time", features);
  }
  
  @Test
  public void testverifySchemaProvided_success() throws Exception {
    CachedFeaturegroupDTO featuregroupDTO = new CachedFeaturegroupDTO();
    featuregroupDTO.setFeatures(features);
    featuregroupDTO.setOnlineEnabled(true);
  
    featureGroupInputValidation.verifySchemaProvided(featuregroupDTO);
  }
  
  @Test(expected = FeaturestoreException.class)
  public void testverifySchemaProvided_fail() throws Exception {
    CachedFeaturegroupDTO featuregroupDTO = new CachedFeaturegroupDTO();
    featuregroupDTO.setFeatures(new ArrayList<>());
    featuregroupDTO.setOnlineEnabled(true);
  
    featureGroupInputValidation.verifySchemaProvided(featuregroupDTO);
  }

  @Test(expected = FeaturestoreException.class)
  public void testVerifyFeatureOfflineTypeProvided_null() throws Exception {
    FeatureGroupFeatureDTO featureDTO = new FeatureGroupFeatureDTO("feature_name", null);
    
    featureGroupInputValidation.verifyOfflineFeatureType(featureDTO);
  }
  
  @Test(expected = FeaturestoreException.class)
  public void testVerifyFeatureOfflineTypeProvided_empty() throws Exception {
    FeatureGroupFeatureDTO featureDTO = new FeatureGroupFeatureDTO("feature_name", "");
    
    featureGroupInputValidation.verifyOfflineFeatureType(featureDTO);
  }

  @Test(expected = FeaturestoreException.class)
  public void testVerifyFeatureGroupFeatureList_name() throws Exception {
    List<FeatureGroupFeatureDTO> featureList = Arrays.asList(
      new FeatureGroupFeatureDTO("feature_name", "string", "description"),
      new FeatureGroupFeatureDTO("1234", "string", "description")
    );
    
    featureGroupInputValidation.verifyFeatureGroupFeatureList(featureList);
  }

  @Test(expected = FeaturestoreException.class)
  public void testVerifyFeatureGroupFeatureList_description() throws Exception {
    List<FeatureGroupFeatureDTO> featureList = Arrays.asList(
      new FeatureGroupFeatureDTO("feature_name", "string", StringUtils.repeat("a", 300)),
      new FeatureGroupFeatureDTO("ft2", "string", "description")
    );
  
    featureGroupInputValidation.verifyFeatureGroupFeatureList(featureList);
  }

  @Test(expected = FeaturestoreException.class)
  public void testVerifyFeatureGroupFeatureList_type() throws Exception {
    List<FeatureGroupFeatureDTO> featureList = Arrays.asList(
      new FeatureGroupFeatureDTO("feature_name", "string", "description"),
      new FeatureGroupFeatureDTO("1234", "", "description")
    );
  
    featureGroupInputValidation.verifyFeatureGroupFeatureList(featureList);
  }
  
  @Test
  public void testVerifyUserInputFeatureGroup() throws Exception {
    CachedFeaturegroupDTO featuregroupDTO = new CachedFeaturegroupDTO();
    featuregroupDTO.setTimeTravelFormat(TimeTravelFormat.HUDI);
    
    // timestamp type camel case
    List<FeatureGroupFeatureDTO> newSchema = new ArrayList<>();
    newSchema.add(new FeatureGroupFeatureDTO("part_param", "Integer", "", true, false));
    newSchema.add(new FeatureGroupFeatureDTO("part_param2", "String", "", false , false));
    newSchema.add(new FeatureGroupFeatureDTO("part_param3", "Timestamp", "", false , true));
    featuregroupDTO.setFeatures(newSchema);
    thrown.expect(FeaturestoreException.class);
    featureGroupInputValidation.verifyPartitionKeySupported(featuregroupDTO);
  }
  
  @Test
  public void testVerifyAndGetNewFeaturesIfPrimary() throws Exception {
    List<FeatureGroupFeatureDTO> newSchema = new ArrayList<>();
    newSchema.add(new FeatureGroupFeatureDTO("part_param", "Integer", "", true, false));
    newSchema.add(new FeatureGroupFeatureDTO("part_param2", "String", "", false , false));
    newSchema.add(new FeatureGroupFeatureDTO("part_param3", "String", "", true , false));
    
    thrown.expect(FeaturestoreException.class);
    featureGroupInputValidation.verifyAndGetNewFeatures(features, newSchema);
  }
  
  @Test
  public void testVerifyAndGetNewFeaturesIfPartition() throws Exception {
    List<FeatureGroupFeatureDTO> newSchema = new ArrayList<>();
    newSchema.add(new FeatureGroupFeatureDTO("part_param", "Integer", "", true, false));
    newSchema.add(new FeatureGroupFeatureDTO("part_param2", "String", "", false , false));
    newSchema.add(new FeatureGroupFeatureDTO("part_param3", "String", "", false , true));
    
    thrown.expect(FeaturestoreException.class);
    featureGroupInputValidation.verifyAndGetNewFeatures(features, newSchema);
  }
  
  @Test
  public void testVerifyAndGetNewFeaturesIfMissingType() throws Exception {
    List<FeatureGroupFeatureDTO> newSchema = new ArrayList<>();
    newSchema.add(new FeatureGroupFeatureDTO("part_param", "Integer", "", true, false));
    newSchema.add(new FeatureGroupFeatureDTO("part_param2", "String", "", false , false));
    newSchema.add(new FeatureGroupFeatureDTO("part_param3", null, "", false , false));
    
    thrown.expect(FeaturestoreException.class);
    featureGroupInputValidation.verifyAndGetNewFeatures(features, newSchema);
  }

  @Test
  public void testVerifyOnlineOfflineTypeMatchCheckCachedFeatureGroupDTO() throws Exception {
    // Arrange
    CachedFeaturegroupDTO featuregroupDTO = new CachedFeaturegroupDTO();
    featuregroupDTO.setOnlineEnabled(true);
    List<FeatureGroupFeatureDTO> newSchema = new ArrayList<>();
    FeatureGroupFeatureDTO featureGroupFeatureDTO = new FeatureGroupFeatureDTO("part_param", "Integer");
    featureGroupFeatureDTO.setOnlineType("bad_type");
    newSchema.add(featureGroupFeatureDTO);
    featuregroupDTO.setFeatures(newSchema);

    // Act
    thrown.expect(FeaturestoreException.class);
    featureGroupInputValidation.verifyOnlineOfflineTypeMatch(featuregroupDTO);
  }

  @Test
  public void testVerifyOnlineOfflineTypeMatchCheckStreamFeatureGroupDTO() throws Exception {
    // Arrange
    StreamFeatureGroupDTO featuregroupDTO = new StreamFeatureGroupDTO();
    featuregroupDTO.setOnlineEnabled(true);
    List<FeatureGroupFeatureDTO> newSchema = new ArrayList<>();
    FeatureGroupFeatureDTO featureGroupFeatureDTO = new FeatureGroupFeatureDTO("part_param", "Integer");
    featureGroupFeatureDTO.setOnlineType("bad_type");
    newSchema.add(featureGroupFeatureDTO);
    featuregroupDTO.setFeatures(newSchema);

    // Act
    thrown.expect(FeaturestoreException.class);
    featureGroupInputValidation.verifyOnlineOfflineTypeMatch(featuregroupDTO);
  }

  @Test
  public void testVerifyOnlineOfflineTypeMatchCheckOnDemandFeaturegroupDTO() throws Exception {
    // Arrange
    OnDemandFeaturegroupDTO featuregroupDTO = new OnDemandFeaturegroupDTO();
    List<FeatureGroupFeatureDTO> newSchema = new ArrayList<>();
    FeatureGroupFeatureDTO featureGroupFeatureDTO = new FeatureGroupFeatureDTO("part_param", "Integer");
    featureGroupFeatureDTO.setOnlineType("bad_type");
    newSchema.add(featureGroupFeatureDTO);
    featuregroupDTO.setFeatures(newSchema);

    // Act
    featureGroupInputValidation.verifyOnlineOfflineTypeMatch(featuregroupDTO);
  }

  @Test
  public void testVerifyOnlineOfflineTypeMatchCheckOffline() throws Exception {
    // Arrange
    CachedFeaturegroupDTO featuregroupDTO = new CachedFeaturegroupDTO();
    featuregroupDTO.setOnlineEnabled(false);
    List<FeatureGroupFeatureDTO> newSchema = new ArrayList<>();
    FeatureGroupFeatureDTO featureGroupFeatureDTO = new FeatureGroupFeatureDTO("part_param", "Integer");
    featureGroupFeatureDTO.setOnlineType("bad_type");
    newSchema.add(featureGroupFeatureDTO);
    featuregroupDTO.setFeatures(newSchema);

    // Act
    featureGroupInputValidation.verifyOnlineOfflineTypeMatch(featuregroupDTO);
  }

  @Test
  public void testVerifyOnlineOfflineTypeMatchBoolean() throws Exception {
    // Arrange
    CachedFeaturegroupDTO featuregroupDTO = new CachedFeaturegroupDTO();
    featuregroupDTO.setOnlineEnabled(true);
    List<FeatureGroupFeatureDTO> newSchema = new ArrayList<>();
    FeatureGroupFeatureDTO featureGroupFeatureDTO = new FeatureGroupFeatureDTO("part_param", "BOOLEAN");
    featureGroupFeatureDTO.setOnlineType("TINYINT");
    newSchema.add(featureGroupFeatureDTO);
    featuregroupDTO.setFeatures(newSchema);

    // Act
    featureGroupInputValidation.verifyOnlineOfflineTypeMatch(featuregroupDTO);
  }

  @Test
  public void testVerifyOnlineOfflineTypeMatchTinyint() throws Exception {
    // Arrange
    CachedFeaturegroupDTO featuregroupDTO = new CachedFeaturegroupDTO();
    featuregroupDTO.setOnlineEnabled(true);
    List<FeatureGroupFeatureDTO> newSchema = new ArrayList<>();
    FeatureGroupFeatureDTO featureGroupFeatureDTO = new FeatureGroupFeatureDTO("part_param", "TINYINT");
    featureGroupFeatureDTO.setOnlineType("TINYINT");
    newSchema.add(featureGroupFeatureDTO);
    featuregroupDTO.setFeatures(newSchema);

    // Act
    featureGroupInputValidation.verifyOnlineOfflineTypeMatch(featuregroupDTO);
  }

  @Test
  public void testVerifyOnlineOfflineTypeMatchSmallint() throws Exception {
    // Arrange
    CachedFeaturegroupDTO featuregroupDTO = new CachedFeaturegroupDTO();
    featuregroupDTO.setOnlineEnabled(true);
    List<FeatureGroupFeatureDTO> newSchema = new ArrayList<>();
    FeatureGroupFeatureDTO featureGroupFeatureDTO = new FeatureGroupFeatureDTO("part_param", "SMALLINT");
    featureGroupFeatureDTO.setOnlineType("SMALLINT");
    newSchema.add(featureGroupFeatureDTO);
    featuregroupDTO.setFeatures(newSchema);

    // Act
    featureGroupInputValidation.verifyOnlineOfflineTypeMatch(featuregroupDTO);
  }

  @Test
  public void testVerifyOnlineOfflineTypeMatchInt() throws Exception {
    // Arrange
    CachedFeaturegroupDTO featuregroupDTO = new CachedFeaturegroupDTO();
    featuregroupDTO.setOnlineEnabled(true);
    List<FeatureGroupFeatureDTO> newSchema = new ArrayList<>();
    FeatureGroupFeatureDTO featureGroupFeatureDTO = new FeatureGroupFeatureDTO("part_param", "INT");
    featureGroupFeatureDTO.setOnlineType("INT");
    newSchema.add(featureGroupFeatureDTO);
    featuregroupDTO.setFeatures(newSchema);

    // Act
    featureGroupInputValidation.verifyOnlineOfflineTypeMatch(featuregroupDTO);
  }

  @Test
  public void testVerifyOnlineOfflineTypeMatchIntTinyint() throws Exception {
    // Arrange
    CachedFeaturegroupDTO featuregroupDTO = new CachedFeaturegroupDTO();
    featuregroupDTO.setOnlineEnabled(true);
    List<FeatureGroupFeatureDTO> newSchema = new ArrayList<>();
    FeatureGroupFeatureDTO featureGroupFeatureDTO = new FeatureGroupFeatureDTO("part_param", "INT");
    featureGroupFeatureDTO.setOnlineType("TINYINT");
    newSchema.add(featureGroupFeatureDTO);
    featuregroupDTO.setFeatures(newSchema);

    // Act
    featureGroupInputValidation.verifyOnlineOfflineTypeMatch(featuregroupDTO);
  }

  @Test
  public void testVerifyOnlineOfflineTypeMatchIntSmallint() throws Exception {
    // Arrange
    CachedFeaturegroupDTO featuregroupDTO = new CachedFeaturegroupDTO();
    featuregroupDTO.setOnlineEnabled(true);
    List<FeatureGroupFeatureDTO> newSchema = new ArrayList<>();
    FeatureGroupFeatureDTO featureGroupFeatureDTO = new FeatureGroupFeatureDTO("part_param", "INT");
    featureGroupFeatureDTO.setOnlineType("SMALLINT");
    newSchema.add(featureGroupFeatureDTO);
    featuregroupDTO.setFeatures(newSchema);

    // Act
    featureGroupInputValidation.verifyOnlineOfflineTypeMatch(featuregroupDTO);
  }

  @Test
  public void testVerifyOnlineOfflineTypeMatchBigint() throws Exception {
    // Arrange
    CachedFeaturegroupDTO featuregroupDTO = new CachedFeaturegroupDTO();
    featuregroupDTO.setOnlineEnabled(true);
    List<FeatureGroupFeatureDTO> newSchema = new ArrayList<>();
    FeatureGroupFeatureDTO featureGroupFeatureDTO = new FeatureGroupFeatureDTO("part_param", "BIGINT");
    featureGroupFeatureDTO.setOnlineType("BIGINT");
    newSchema.add(featureGroupFeatureDTO);
    featuregroupDTO.setFeatures(newSchema);

    // Act
    featureGroupInputValidation.verifyOnlineOfflineTypeMatch(featuregroupDTO);
  }

  @Test
  public void testVerifyOnlineOfflineTypeMatchFloat() throws Exception {
    // Arrange
    CachedFeaturegroupDTO featuregroupDTO = new CachedFeaturegroupDTO();
    featuregroupDTO.setOnlineEnabled(true);
    List<FeatureGroupFeatureDTO> newSchema = new ArrayList<>();
    FeatureGroupFeatureDTO featureGroupFeatureDTO = new FeatureGroupFeatureDTO("part_param", "FLOAT");
    featureGroupFeatureDTO.setOnlineType("FLOAT");
    newSchema.add(featureGroupFeatureDTO);
    featuregroupDTO.setFeatures(newSchema);

    // Act
    featureGroupInputValidation.verifyOnlineOfflineTypeMatch(featuregroupDTO);
  }

  @Test
  public void testVerifyOnlineOfflineTypeMatchDouble() throws Exception {
    // Arrange
    CachedFeaturegroupDTO featuregroupDTO = new CachedFeaturegroupDTO();
    featuregroupDTO.setOnlineEnabled(true);
    List<FeatureGroupFeatureDTO> newSchema = new ArrayList<>();
    FeatureGroupFeatureDTO featureGroupFeatureDTO = new FeatureGroupFeatureDTO("part_param", "DOUBLE");
    featureGroupFeatureDTO.setOnlineType("DOUBLE");
    newSchema.add(featureGroupFeatureDTO);
    featuregroupDTO.setFeatures(newSchema);

    // Act
    featureGroupInputValidation.verifyOnlineOfflineTypeMatch(featuregroupDTO);
  }

  @Test
  public void testVerifyOnlineOfflineTypeMatchDecimal() throws Exception {
    // Arrange
    CachedFeaturegroupDTO featuregroupDTO = new CachedFeaturegroupDTO();
    featuregroupDTO.setOnlineEnabled(true);
    List<FeatureGroupFeatureDTO> newSchema = new ArrayList<>();
    FeatureGroupFeatureDTO featureGroupFeatureDTO = new FeatureGroupFeatureDTO("part_param", "DECIMAL");
    featureGroupFeatureDTO.setOnlineType("DECIMAL");
    newSchema.add(featureGroupFeatureDTO);
    featuregroupDTO.setFeatures(newSchema);

    // Act
    featureGroupInputValidation.verifyOnlineOfflineTypeMatch(featuregroupDTO);
  }

  @Test
  public void testVerifyOnlineOfflineTypeMatchTimestamp() throws Exception {
    // Arrange
    CachedFeaturegroupDTO featuregroupDTO = new CachedFeaturegroupDTO();
    featuregroupDTO.setOnlineEnabled(true);
    List<FeatureGroupFeatureDTO> newSchema = new ArrayList<>();
    FeatureGroupFeatureDTO featureGroupFeatureDTO = new FeatureGroupFeatureDTO("part_param", "TIMESTAMP");
    featureGroupFeatureDTO.setOnlineType("TIMESTAMP");
    newSchema.add(featureGroupFeatureDTO);
    featuregroupDTO.setFeatures(newSchema);

    // Act
    featureGroupInputValidation.verifyOnlineOfflineTypeMatch(featuregroupDTO);
  }

  @Test
  public void testVerifyOnlineOfflineTypeMatchDate() throws Exception {
    // Arrange
    CachedFeaturegroupDTO featuregroupDTO = new CachedFeaturegroupDTO();
    featuregroupDTO.setOnlineEnabled(true);
    List<FeatureGroupFeatureDTO> newSchema = new ArrayList<>();
    FeatureGroupFeatureDTO featureGroupFeatureDTO = new FeatureGroupFeatureDTO("part_param", "DATE");
    featureGroupFeatureDTO.setOnlineType("DATE");
    newSchema.add(featureGroupFeatureDTO);
    featuregroupDTO.setFeatures(newSchema);

    // Act
    featureGroupInputValidation.verifyOnlineOfflineTypeMatch(featuregroupDTO);
  }

  @Test
  public void testVerifyOnlineOfflineTypeMatchStringVarchar() throws Exception {
    // Arrange
    CachedFeaturegroupDTO featuregroupDTO = new CachedFeaturegroupDTO();
    featuregroupDTO.setOnlineEnabled(true);
    List<FeatureGroupFeatureDTO> newSchema = new ArrayList<>();
    FeatureGroupFeatureDTO featureGroupFeatureDTO = new FeatureGroupFeatureDTO("part_param", "STRING");
    featureGroupFeatureDTO.setOnlineType("VARCHAR");
    newSchema.add(featureGroupFeatureDTO);
    featuregroupDTO.setFeatures(newSchema);

    // Act
    featureGroupInputValidation.verifyOnlineOfflineTypeMatch(featuregroupDTO);
  }

  @Test
  public void testVerifyOnlineOfflineTypeMatchStringText() throws Exception {
    // Arrange
    CachedFeaturegroupDTO featuregroupDTO = new CachedFeaturegroupDTO();
    featuregroupDTO.setOnlineEnabled(true);
    List<FeatureGroupFeatureDTO> newSchema = new ArrayList<>();
    FeatureGroupFeatureDTO featureGroupFeatureDTO = new FeatureGroupFeatureDTO("part_param", "STRING");
    featureGroupFeatureDTO.setOnlineType("TEXT");
    newSchema.add(featureGroupFeatureDTO);
    featuregroupDTO.setFeatures(newSchema);

    // Act
    featureGroupInputValidation.verifyOnlineOfflineTypeMatch(featuregroupDTO);
  }

  @Test
  public void testVerifyOnlineOfflineTypeMatchArrayVarbinary() throws Exception {
    // Arrange
    CachedFeaturegroupDTO featuregroupDTO = new CachedFeaturegroupDTO();
    featuregroupDTO.setOnlineEnabled(true);
    List<FeatureGroupFeatureDTO> newSchema = new ArrayList<>();
    FeatureGroupFeatureDTO featureGroupFeatureDTO = new FeatureGroupFeatureDTO("part_param", "ARRAY");
    featureGroupFeatureDTO.setOnlineType("VARBINARY");
    newSchema.add(featureGroupFeatureDTO);
    featuregroupDTO.setFeatures(newSchema);

    // Act
    featureGroupInputValidation.verifyOnlineOfflineTypeMatch(featuregroupDTO);
  }

  @Test
  public void testVerifyOnlineOfflineTypeMatchArrayBlob() throws Exception {
    // Arrange
    CachedFeaturegroupDTO featuregroupDTO = new CachedFeaturegroupDTO();
    featuregroupDTO.setOnlineEnabled(true);
    List<FeatureGroupFeatureDTO> newSchema = new ArrayList<>();
    FeatureGroupFeatureDTO featureGroupFeatureDTO = new FeatureGroupFeatureDTO("part_param", "ARRAY");
    featureGroupFeatureDTO.setOnlineType("BLOB");
    newSchema.add(featureGroupFeatureDTO);
    featuregroupDTO.setFeatures(newSchema);

    // Act
    featureGroupInputValidation.verifyOnlineOfflineTypeMatch(featuregroupDTO);
  }

  @Test
  public void testVerifyOnlineOfflineTypeMatchStructVarbinary() throws Exception {
    // Arrange
    CachedFeaturegroupDTO featuregroupDTO = new CachedFeaturegroupDTO();
    featuregroupDTO.setOnlineEnabled(true);
    List<FeatureGroupFeatureDTO> newSchema = new ArrayList<>();
    FeatureGroupFeatureDTO featureGroupFeatureDTO = new FeatureGroupFeatureDTO("part_param", "STRUCT");
    featureGroupFeatureDTO.setOnlineType("VARBINARY");
    newSchema.add(featureGroupFeatureDTO);
    featuregroupDTO.setFeatures(newSchema);

    // Act
    featureGroupInputValidation.verifyOnlineOfflineTypeMatch(featuregroupDTO);
  }

  @Test
  public void testVerifyOnlineOfflineTypeMatchStructBlob() throws Exception {
    // Arrange
    CachedFeaturegroupDTO featuregroupDTO = new CachedFeaturegroupDTO();
    featuregroupDTO.setOnlineEnabled(true);
    List<FeatureGroupFeatureDTO> newSchema = new ArrayList<>();
    FeatureGroupFeatureDTO featureGroupFeatureDTO = new FeatureGroupFeatureDTO("part_param", "STRUCT");
    featureGroupFeatureDTO.setOnlineType("BLOB");
    newSchema.add(featureGroupFeatureDTO);
    featuregroupDTO.setFeatures(newSchema);

    // Act
    featureGroupInputValidation.verifyOnlineOfflineTypeMatch(featuregroupDTO);
  }

  @Test
  public void testVerifyOnlineOfflineTypeMatchBinaryVarbinary() throws Exception {
    // Arrange
    CachedFeaturegroupDTO featuregroupDTO = new CachedFeaturegroupDTO();
    featuregroupDTO.setOnlineEnabled(true);
    List<FeatureGroupFeatureDTO> newSchema = new ArrayList<>();
    FeatureGroupFeatureDTO featureGroupFeatureDTO = new FeatureGroupFeatureDTO("part_param", "BINARY");
    featureGroupFeatureDTO.setOnlineType("VARBINARY");
    newSchema.add(featureGroupFeatureDTO);
    featuregroupDTO.setFeatures(newSchema);

    // Act
    featureGroupInputValidation.verifyOnlineOfflineTypeMatch(featuregroupDTO);
  }

  @Test
  public void testVerifyOnlineOfflineTypeMatchBinaryBlob() throws Exception {
    // Arrange
    CachedFeaturegroupDTO featuregroupDTO = new CachedFeaturegroupDTO();
    featuregroupDTO.setOnlineEnabled(true);
    List<FeatureGroupFeatureDTO> newSchema = new ArrayList<>();
    FeatureGroupFeatureDTO featureGroupFeatureDTO = new FeatureGroupFeatureDTO("part_param", "BINARY");
    featureGroupFeatureDTO.setOnlineType("BLOB");
    newSchema.add(featureGroupFeatureDTO);
    featuregroupDTO.setFeatures(newSchema);

    // Act
    featureGroupInputValidation.verifyOnlineOfflineTypeMatch(featuregroupDTO);
  }

  @Test
  public void testVerifyOnlineOfflineTypeMatchMapVarbinary() throws Exception {
    // Arrange
    CachedFeaturegroupDTO featuregroupDTO = new CachedFeaturegroupDTO();
    featuregroupDTO.setOnlineEnabled(true);
    List<FeatureGroupFeatureDTO> newSchema = new ArrayList<>();
    FeatureGroupFeatureDTO featureGroupFeatureDTO = new FeatureGroupFeatureDTO("part_param", "MAP");
    featureGroupFeatureDTO.setOnlineType("VARBINARY");
    newSchema.add(featureGroupFeatureDTO);
    featuregroupDTO.setFeatures(newSchema);

    // Act
    featureGroupInputValidation.verifyOnlineOfflineTypeMatch(featuregroupDTO);
  }

  @Test
  public void testVerifyOnlineOfflineTypeMatchMapBlob() throws Exception {
    // Arrange
    CachedFeaturegroupDTO featuregroupDTO = new CachedFeaturegroupDTO();
    featuregroupDTO.setOnlineEnabled(true);
    List<FeatureGroupFeatureDTO> newSchema = new ArrayList<>();
    FeatureGroupFeatureDTO featureGroupFeatureDTO = new FeatureGroupFeatureDTO("part_param", "MAP");
    featureGroupFeatureDTO.setOnlineType("BLOB");
    newSchema.add(featureGroupFeatureDTO);
    featuregroupDTO.setFeatures(newSchema);

    // Act
    featureGroupInputValidation.verifyOnlineOfflineTypeMatch(featuregroupDTO);
  }
}
