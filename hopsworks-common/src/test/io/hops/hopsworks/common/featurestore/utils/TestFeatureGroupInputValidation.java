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
import io.hops.hopsworks.exceptions.FeaturestoreException;
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
    new FeatureGroupInputValidation(new FeaturestoreInputValidation());
  
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
}
