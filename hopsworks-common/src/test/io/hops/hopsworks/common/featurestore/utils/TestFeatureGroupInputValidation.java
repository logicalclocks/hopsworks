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

import com.google.common.collect.Lists;
import io.hops.hopsworks.common.featurestore.embedding.EmbeddingController;
import io.hops.hopsworks.common.featurestore.feature.FeatureGroupFeatureDTO;
import io.hops.hopsworks.common.featurestore.featuregroup.EmbeddingDTO;
import io.hops.hopsworks.common.featurestore.featuregroup.EmbeddingFeatureDTO;
import io.hops.hopsworks.common.featurestore.featuregroup.FeatureGroupInputValidation;
import io.hops.hopsworks.common.featurestore.featuregroup.FeaturegroupDTO;
import io.hops.hopsworks.common.featurestore.featuregroup.cached.CachedFeaturegroupDTO;
import io.hops.hopsworks.common.featurestore.featuregroup.ondemand.OnDemandFeaturegroupDTO;
import io.hops.hopsworks.common.featurestore.featuregroup.online.OnlineFeaturegroupController;
import io.hops.hopsworks.common.featurestore.featuregroup.stream.StreamFeatureGroupDTO;
import io.hops.hopsworks.exceptions.FeaturestoreException;
import io.hops.hopsworks.persistence.entity.featurestore.featuregroup.cached.TimeTravelFormat;
import io.hops.hopsworks.persistence.entity.project.Project;
import org.apache.commons.lang.StringUtils;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mockito.Mockito;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

public class TestFeatureGroupInputValidation {

  private EmbeddingController embeddingController;

  private FeatureGroupInputValidation featureGroupInputValidation;
  private Project project;

  List<FeatureGroupFeatureDTO> features;

  @Rule
  public ExpectedException thrown = ExpectedException.none();

  @Before
  public void setup() {
    project = mock(Project.class);
    embeddingController = Mockito.mock(EmbeddingController.class);
    features = new ArrayList<>();
    features.add(new FeatureGroupFeatureDTO("feature", "TIMESTAMP", "", true, false, "10", null));
    features.add(new FeatureGroupFeatureDTO("feature2", "String", "", false, false, null, null));
    featureGroupInputValidation =
        new FeatureGroupInputValidation(new FeaturestoreInputValidation(), new OnlineFeaturegroupController(),
            embeddingController);
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

  @Test
  public void verifyNoDuplicatedFeatures_success() throws Exception {
    CachedFeaturegroupDTO featuregroupDTO = new CachedFeaturegroupDTO();
    featuregroupDTO.setFeatures(features);

    featureGroupInputValidation.verifyNoDuplicatedFeatures(featuregroupDTO);
  }

  @Test(expected = FeaturestoreException.class)
  public void verifyNoDuplicatedFeatures_fail() throws Exception {
    CachedFeaturegroupDTO featuregroupDTO = new CachedFeaturegroupDTO();
    List<FeatureGroupFeatureDTO> featuresWithDuplicate = new ArrayList<>(features);
    featuresWithDuplicate.add(new FeatureGroupFeatureDTO("feature", "TIMESTAMP", "", true, false, "10", null));
    featuregroupDTO.setFeatures(featuresWithDuplicate);

    featureGroupInputValidation.verifyNoDuplicatedFeatures(featuregroupDTO);
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
    newSchema.add(new FeatureGroupFeatureDTO("part_param2", "String", "", false, false));
    newSchema.add(new FeatureGroupFeatureDTO("part_param3", "Timestamp", "", false, true));
    featuregroupDTO.setFeatures(newSchema);
    thrown.expect(FeaturestoreException.class);
    featureGroupInputValidation.verifyPartitionKeySupported(featuregroupDTO);
  }

  @Test
  public void testVerifyAndGetNewFeaturesIfPrimary() throws Exception {
    List<FeatureGroupFeatureDTO> newSchema = new ArrayList<>();
    newSchema.add(new FeatureGroupFeatureDTO("part_param", "Integer", "", true, false));
    newSchema.add(new FeatureGroupFeatureDTO("part_param2", "String", "", false, false));
    newSchema.add(new FeatureGroupFeatureDTO("part_param3", "String", "", true, false));

    thrown.expect(FeaturestoreException.class);
    featureGroupInputValidation.verifyAndGetNewFeatures(features, newSchema);
  }

  @Test
  public void testVerifyAndGetNewFeaturesIfPartition() throws Exception {
    List<FeatureGroupFeatureDTO> newSchema = new ArrayList<>();
    newSchema.add(new FeatureGroupFeatureDTO("part_param", "Integer", "", true, false));
    newSchema.add(new FeatureGroupFeatureDTO("part_param2", "String", "", false, false));
    newSchema.add(new FeatureGroupFeatureDTO("part_param3", "String", "", false, true));

    thrown.expect(FeaturestoreException.class);
    featureGroupInputValidation.verifyAndGetNewFeatures(features, newSchema);
  }

  @Test
  public void testVerifyAndGetNewFeaturesIfMissingType() throws Exception {
    List<FeatureGroupFeatureDTO> newSchema = new ArrayList<>();
    newSchema.add(new FeatureGroupFeatureDTO("part_param", "Integer", "", true, false));
    newSchema.add(new FeatureGroupFeatureDTO("part_param2", "String", "", false, false));
    newSchema.add(new FeatureGroupFeatureDTO("part_param3", null, "", false, false));

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
  public void testVerifyOnlineOfflineTypeMatchStringMediumText() throws Exception {
    // Arrange
    CachedFeaturegroupDTO featuregroupDTO = new CachedFeaturegroupDTO();
    featuregroupDTO.setOnlineEnabled(true);
    List<FeatureGroupFeatureDTO> newSchema = new ArrayList<>();
    FeatureGroupFeatureDTO featureGroupFeatureDTO = new FeatureGroupFeatureDTO("part_param", "STRING");
    featureGroupFeatureDTO.setOnlineType("MEDIUMTEXT");
    newSchema.add(featureGroupFeatureDTO);
    featuregroupDTO.setFeatures(newSchema);

    // Act
    featureGroupInputValidation.verifyOnlineOfflineTypeMatch(featuregroupDTO);
  }

  @Test
  public void testVerifyOnlineOfflineTypeMatchStringLongText() throws Exception {
    // Arrange
    CachedFeaturegroupDTO featuregroupDTO = new CachedFeaturegroupDTO();
    featuregroupDTO.setOnlineEnabled(true);
    List<FeatureGroupFeatureDTO> newSchema = new ArrayList<>();
    FeatureGroupFeatureDTO featureGroupFeatureDTO = new FeatureGroupFeatureDTO("part_param", "STRING");
    featureGroupFeatureDTO.setOnlineType("LONGTEXT");
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
  public void testVerifyOnlineOfflineTypeMatchArrayMediumBlob() throws Exception {
    // Arrange
    CachedFeaturegroupDTO featuregroupDTO = new CachedFeaturegroupDTO();
    featuregroupDTO.setOnlineEnabled(true);
    List<FeatureGroupFeatureDTO> newSchema = new ArrayList<>();
    FeatureGroupFeatureDTO featureGroupFeatureDTO = new FeatureGroupFeatureDTO("part_param", "ARRAY");
    featureGroupFeatureDTO.setOnlineType("MEDIUMBLOB");
    newSchema.add(featureGroupFeatureDTO);
    featuregroupDTO.setFeatures(newSchema);

    // Act
    featureGroupInputValidation.verifyOnlineOfflineTypeMatch(featuregroupDTO);
  }

  @Test
  public void testVerifyOnlineOfflineTypeMatchArrayLongBlob() throws Exception {
    // Arrange
    CachedFeaturegroupDTO featuregroupDTO = new CachedFeaturegroupDTO();
    featuregroupDTO.setOnlineEnabled(true);
    List<FeatureGroupFeatureDTO> newSchema = new ArrayList<>();
    FeatureGroupFeatureDTO featureGroupFeatureDTO = new FeatureGroupFeatureDTO("part_param", "ARRAY");
    featureGroupFeatureDTO.setOnlineType("LONGBLOB");
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
  public void testVerifyOnlineOfflineTypeMatchStructMediumBlob() throws Exception {
    // Arrange
    CachedFeaturegroupDTO featuregroupDTO = new CachedFeaturegroupDTO();
    featuregroupDTO.setOnlineEnabled(true);
    List<FeatureGroupFeatureDTO> newSchema = new ArrayList<>();
    FeatureGroupFeatureDTO featureGroupFeatureDTO = new FeatureGroupFeatureDTO("part_param", "STRUCT");
    featureGroupFeatureDTO.setOnlineType("MEDIUMBLOB");
    newSchema.add(featureGroupFeatureDTO);
    featuregroupDTO.setFeatures(newSchema);

    // Act
    featureGroupInputValidation.verifyOnlineOfflineTypeMatch(featuregroupDTO);
  }

  @Test
  public void testVerifyOnlineOfflineTypeMatchStructLongBlob() throws Exception {
    // Arrange
    CachedFeaturegroupDTO featuregroupDTO = new CachedFeaturegroupDTO();
    featuregroupDTO.setOnlineEnabled(true);
    List<FeatureGroupFeatureDTO> newSchema = new ArrayList<>();
    FeatureGroupFeatureDTO featureGroupFeatureDTO = new FeatureGroupFeatureDTO("part_param", "STRUCT");
    featureGroupFeatureDTO.setOnlineType("LONGBLOB");
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

  @Test
  public void testVerifyEmbeddingFeatureExist_pass() throws FeaturestoreException {
    FeatureGroupFeatureDTO feature1 = new FeatureGroupFeatureDTO("feature1");
    FeatureGroupFeatureDTO feature2 = new FeatureGroupFeatureDTO("feature2");
    FeatureGroupFeatureDTO feature3 = new FeatureGroupFeatureDTO("feature3");

    EmbeddingFeatureDTO embeddingFeature1 = new EmbeddingFeatureDTO("feature1", "l2_norm", 3);
    EmbeddingFeatureDTO embeddingFeature2 = new EmbeddingFeatureDTO("feature2", "l2_norm", 3);
    EmbeddingFeatureDTO embeddingFeature3 = new EmbeddingFeatureDTO("feature3", "l2_norm", 3);

    List<FeatureGroupFeatureDTO> features = Arrays.asList(feature1, feature2, feature3);
    List<EmbeddingFeatureDTO> embeddingFeatures =
        Arrays.asList(embeddingFeature1, embeddingFeature2, embeddingFeature3);

    EmbeddingDTO embeddingDTO = new EmbeddingDTO("name", "prefix", embeddingFeatures);

    FeaturegroupDTO featureGroupDTO = new FeaturegroupDTO();
    featureGroupDTO.setEmbeddingIndex(embeddingDTO);
    featureGroupDTO.setFeatures(features);

    featureGroupInputValidation =
        new FeatureGroupInputValidation(new FeaturestoreInputValidation(), new OnlineFeaturegroupController(),
            new EmbeddingController());
    featureGroupInputValidation.verifyEmbeddingFeatureExist(featureGroupDTO);
  }

  @Test
  public void testVerifyEmbeddingFeatureExist_fail() throws FeaturestoreException {
    FeatureGroupFeatureDTO feature1 = new FeatureGroupFeatureDTO("feature1");
    FeatureGroupFeatureDTO feature2 = new FeatureGroupFeatureDTO("feature2");
    FeatureGroupFeatureDTO feature3 = new FeatureGroupFeatureDTO("feature3");

    EmbeddingFeatureDTO embeddingFeature1 = new EmbeddingFeatureDTO("feature1", "l2_norm", 3);
    EmbeddingFeatureDTO embeddingFeature2 = new EmbeddingFeatureDTO("feature2", "l2_norm", 3);
    EmbeddingFeatureDTO embeddingFeature3 =
        new EmbeddingFeatureDTO("feature4", "l2_norm", 3);  // this does not exist in feature group

    List<FeatureGroupFeatureDTO> features = Arrays.asList(feature1, feature2, feature3);
    List<EmbeddingFeatureDTO> embeddingFeatures =
        Arrays.asList(embeddingFeature1, embeddingFeature2, embeddingFeature3);

    EmbeddingDTO embeddingDTO = new EmbeddingDTO("name", "prefix", embeddingFeatures);

    FeaturegroupDTO featureGroupDTO = new FeaturegroupDTO();
    featureGroupDTO.setEmbeddingIndex(embeddingDTO);
    featureGroupDTO.setFeatures(features);

    assertThrows(FeaturestoreException.class,
        () -> featureGroupInputValidation.verifyEmbeddingFeatureExist(featureGroupDTO));
  }

  @Test
  public void testVerifyEmbeddingIndex_pass() throws FeaturestoreException {
    EmbeddingFeatureDTO embeddingFeature1 = new EmbeddingFeatureDTO("feature1", "l2_norm", 3);
    EmbeddingFeatureDTO embeddingFeature2 = new EmbeddingFeatureDTO("feature2", "l2_norm", 3);
    EmbeddingFeatureDTO embeddingFeature3 = new EmbeddingFeatureDTO("feature3", "l2_norm", 3);

    List<EmbeddingFeatureDTO> embeddingFeatures =
        Arrays.asList(embeddingFeature1, embeddingFeature2, embeddingFeature3);
    EmbeddingDTO embeddingDTO = new EmbeddingDTO("name", "prefix", embeddingFeatures);

    FeaturegroupDTO featureGroupDTO = new FeaturegroupDTO();
    featureGroupDTO.setEmbeddingIndex(embeddingDTO);

    doNothing().when(embeddingController).verifyIndexName(any(), anyString());

    featureGroupInputValidation =
        new FeatureGroupInputValidation(new FeaturestoreInputValidation(), new OnlineFeaturegroupController(),
            embeddingController);

    featureGroupInputValidation.verifyEmbeddingIndexNotExist(project, featureGroupDTO);
  }

  @Test
  public void testVerifyEmbeddingIndex_fail() throws FeaturestoreException {
    EmbeddingFeatureDTO embeddingFeature1 = new EmbeddingFeatureDTO("feature1", "l2_norm", 3);
    EmbeddingFeatureDTO embeddingFeature2 = new EmbeddingFeatureDTO("feature2", "l2_norm", 3);
    EmbeddingFeatureDTO embeddingFeature3 = new EmbeddingFeatureDTO("feature3", "l2_norm", 3);

    List<EmbeddingFeatureDTO> embeddingFeatures =
        Arrays.asList(embeddingFeature1, embeddingFeature2, embeddingFeature3);
    EmbeddingDTO embeddingDTO = new EmbeddingDTO("name", "prefix", embeddingFeatures);

    FeaturegroupDTO featureGroupDTO = new FeaturegroupDTO();
    featureGroupDTO.setEmbeddingIndex(embeddingDTO);

    doThrow(FeaturestoreException.class).when(embeddingController).verifyIndexName(any(), anyString());

    featureGroupInputValidation =
        new FeatureGroupInputValidation(new FeaturestoreInputValidation(), new OnlineFeaturegroupController(),
            embeddingController);

    assertThrows(FeaturestoreException.class, () -> featureGroupInputValidation.verifyEmbeddingIndexNotExist(project, featureGroupDTO));
  }

  private FeaturegroupDTO createFeaturegroupDtoWithIndexName(String indexName) {
    EmbeddingFeatureDTO embeddingFeature = new EmbeddingFeatureDTO("feature3", "l2_norm", 3);

    List<EmbeddingFeatureDTO> embeddingFeatures =
        Arrays.asList(embeddingFeature);
    EmbeddingDTO embeddingDTO = new EmbeddingDTO(indexName, "prefix", embeddingFeatures);

    FeaturegroupDTO featureGroupDTO = new FeaturegroupDTO();
    featureGroupDTO.setEmbeddingIndex(embeddingDTO);
    return featureGroupDTO;
  }

  @Test(expected = FeaturestoreException.class)
  public void testInvalidUpperCaseName() throws Exception {
    featureGroupInputValidation.verifyEmbeddingIndexName(createFeaturegroupDtoWithIndexName("MyInvalidName"));
    featureGroupInputValidation.verifyEmbeddingIndexName(createFeaturegroupDtoWithIndexName("NAMEWITHUPPERCASE"));
  }

  @Test(expected = FeaturestoreException.class)
  public void testInvalidStartingCharacters() throws Exception {
    featureGroupInputValidation.verifyEmbeddingIndexName(createFeaturegroupDtoWithIndexName("_invalid_name"));
    featureGroupInputValidation.verifyEmbeddingIndexName(createFeaturegroupDtoWithIndexName("-starting_with_hyphen"));
  }

  @Test(expected = FeaturestoreException.class)
  public void testInvalidCharacters() throws Exception {
    featureGroupInputValidation.verifyEmbeddingIndexName(createFeaturegroupDtoWithIndexName("name,with,comma"));
    featureGroupInputValidation.verifyEmbeddingIndexName(createFeaturegroupDtoWithIndexName("invalid*name"));
    featureGroupInputValidation.verifyEmbeddingIndexName(createFeaturegroupDtoWithIndexName("with<special>chars"));
  }

  @Test
  public void testNullOrEmptyName() throws Exception {
    featureGroupInputValidation.verifyEmbeddingIndexName(createFeaturegroupDtoWithIndexName(null));
    featureGroupInputValidation.verifyEmbeddingIndexName(createFeaturegroupDtoWithIndexName(""));
  }

  @Test
  public void testValidLowerCaseName() throws Exception {
    featureGroupInputValidation.verifyEmbeddingIndexName(createFeaturegroupDtoWithIndexName("myindexname"));
    featureGroupInputValidation.verifyEmbeddingIndexName(createFeaturegroupDtoWithIndexName("valid_name123"));
  }

  @Test
  public void testValidNameWithAllowedCharacters() throws Exception {
    featureGroupInputValidation.verifyEmbeddingIndexName(createFeaturegroupDtoWithIndexName("name.with.dots"));
    featureGroupInputValidation.verifyEmbeddingIndexName(createFeaturegroupDtoWithIndexName("name_with_underscores"));
  }

  @Test
  public void testVerifyVectorDatabaseIndexMappingLimit() throws FeaturestoreException {
    // Call the method under test
    featureGroupInputValidation.verifyVectorDatabaseIndexMappingLimit(createFeaturegroupDtoWithIndexName("myindexname"), 10);

    // Verify that the method called the embeddingController with the correct parameters
    verify(embeddingController).validateWithinMappingLimit(any(), any());
  }

  @Test
  public void testVerifyVectorDatabaseIndexMappingLimit_EmbeddingIndexNull() throws FeaturestoreException {
    // Call the method under test
    featureGroupInputValidation.verifyVectorDatabaseIndexMappingLimit(createFeaturegroupDtoWithIndexName(null), 10);

    // Verify that the method did not call the embeddingController
    verify(embeddingController, never()).validateWithinMappingLimit(any(), any());
  }

  @Test
  public void testVerifyVectorDatabaseIndexMappingLimit_EmbeddingIndexWithoutName() throws FeaturestoreException {
    // Call the method under test
    featureGroupInputValidation.verifyVectorDatabaseIndexMappingLimit(createFeaturegroupDtoWithIndexName(""), 10);

    // Verify that the method did not call the embeddingController
    verify(embeddingController, never()).validateWithinMappingLimit(any(), any());
  }

  @Test
  public void testVerifyVectorDatabaseSupportedDataType_Success() throws Exception {
    // Mocking FeatureGroupFeatureDTO objects
    FeatureGroupFeatureDTO feature1 = new FeatureGroupFeatureDTO("feature1", "int");
    FeatureGroupFeatureDTO feature2 = new FeatureGroupFeatureDTO("feature2", "float");
    FeaturegroupDTO featureGroupDTO = createFeaturegroupDtoWithIndexName("test");
    featureGroupDTO.setFeatures(Lists.newArrayList(feature1, feature2));

    // Call the method
    featureGroupInputValidation.verifyVectorDatabaseSupportedDataType(featureGroupDTO);
  }

  @Test
  public void testVerifyVectorDatabaseSupportedDataType_Fail() throws Exception {
    // Mocking FeatureGroupFeatureDTO objects
    FeatureGroupFeatureDTO feature1 = new FeatureGroupFeatureDTO("feature1", "not_supported_type");
    FeaturegroupDTO featureGroupDTO = createFeaturegroupDtoWithIndexName("test");
    featureGroupDTO.setFeatures(Lists.newArrayList(feature1));

    // Call the method
    assertThrows(FeaturestoreException.class, () -> featureGroupInputValidation.verifyVectorDatabaseSupportedDataType(featureGroupDTO));
  }

  @Test
  public void testVerifyVectorDatabaseSupportedDataType_FeatureGroupFeatureDTO_Success() throws Exception {
    // Mocking FeatureGroupFeatureDTO objects
    FeatureGroupFeatureDTO feature1 = new FeatureGroupFeatureDTO("feature1", "int");
    FeatureGroupFeatureDTO feature2 = new FeatureGroupFeatureDTO("feature2", "float");

    // Call the method
    featureGroupInputValidation.verifyVectorDatabaseSupportedDataType(Lists.newArrayList(feature1, feature2));
  }

  @Test
  public void testVerifyVectorDatabaseSupportedDataType_FeatureGroupFeatureDTO_Fail() throws Exception {
    // Mocking FeatureGroupFeatureDTO objects
    FeatureGroupFeatureDTO feature1 = new FeatureGroupFeatureDTO("feature1", "not_supported_type");

    // Call the method
    assertThrows(FeaturestoreException.class, () -> featureGroupInputValidation.verifyVectorDatabaseSupportedDataType(Lists.newArrayList(feature1)));
  }
}
