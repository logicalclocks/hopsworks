/*
 * This file is part of Hopsworks
 * Copyright (C) 2019, Logical Clocks AB. All rights reserved
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

import io.hops.hopsworks.common.dao.featurestore.featuregroup.Featuregroup;
import io.hops.hopsworks.common.featurestore.feature.FeatureDTO;
import io.hops.hopsworks.common.featurestore.featuregroup.FeaturegroupController;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mockito.Mockito;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class TestConstructorController {

  private Featuregroup fg1;
  private Featuregroup fg2;
  private Featuregroup fg3;

  private List<FeatureDTO> fg1Features = new ArrayList<>();
  private List<FeatureDTO> fg2Features = new ArrayList<>();
  private List<FeatureDTO> fg3Features = new ArrayList<>();

  @Rule
  public ExpectedException thrown = ExpectedException.none();

  @Before
  public void setup() {
    fg1 = new Featuregroup(1);
    fg1.setName("fg1");
    fg2 = new Featuregroup(2);
    fg2.setName("fg2");
    fg3 = new Featuregroup(3);
    fg3.setName("fg3");

    fg1Features = new ArrayList<>();
    fg1Features.add(new FeatureDTO("fg1", "fg1_ft1", "Integer", "", true, false, ""));
    fg1Features.add(new FeatureDTO("fg1", "fg1_ft2", "String", "", false, false, ""));

    fg2Features = new ArrayList<>();
    fg2Features.add(new FeatureDTO("fg2", "fg2_ft1", "Integer", "", true, false, ""));
    fg2Features.add(new FeatureDTO("fg2", "fg2_ft2", "String", "", false, false, ""));

    fg3Features = new ArrayList<>();
    fg3Features.add(new FeatureDTO("fg3", "fg3_ft1", "Integer", "", true, false, ""));
    fg3Features.add(new FeatureDTO("fg3", "fg3_ft2", "String", "", false, false, ""));
  }

  @Test
  public void testExtractSelectedFeatures() {
    FeaturegroupController fgControllerClass = Mockito.mock(FeaturegroupController.class);
    ConstructorController constructorController = new ConstructorController(fgControllerClass);

    Mockito.when(fgControllerClass.getFeatures(Mockito.any())).thenReturn(fg1Features);

    List<FeatureDTO> requestedFeatures = new ArrayList<>();
    requestedFeatures.add(new FeatureDTO("fg1_ft1"));

    List<FeatureDTO> extractedFeatures =
        constructorController.extractSelectedFeatures(fg1, "fg1", requestedFeatures);
    Assert.assertEquals(1, extractedFeatures.size());
    Assert.assertEquals("fg1_ft1", extractedFeatures.get(0).getName());
    // Make sure the object returned is the one for the DB with more infomation in (e.g. Type, Primary key)
    Assert.assertTrue(extractedFeatures.get(0).getPrimary());
  }

  @Test
  public void testMissingFeature() {
    FeaturegroupController fgControllerClass = Mockito.mock(FeaturegroupController.class);
    ConstructorController constructorController = new ConstructorController(fgControllerClass);

    Mockito.when(fgControllerClass.getFeatures(Mockito.any())).thenReturn(fg1Features);

    List<FeatureDTO> requestedFeatures = new ArrayList<>();
    requestedFeatures.add(new FeatureDTO("fg1_ft3"));

    thrown.expect(IllegalArgumentException.class);
    thrown.expectMessage("Feature: fg1_ft3 not found in feature group: fg1");
    constructorController.extractSelectedFeatures(fg1, "fg1", requestedFeatures);
  }

  @Test
  public void testExtractAllFeatures() {
    FeaturegroupController fgControllerClass = Mockito.mock(FeaturegroupController.class);
    ConstructorController constructorController = new ConstructorController(fgControllerClass);

    Mockito.when(fgControllerClass.getFeatures(Mockito.any())).thenReturn(fg1Features);

    List<FeatureDTO> requestedFeatures = new ArrayList<>();
    requestedFeatures.add(new FeatureDTO("*"));

    List<FeatureDTO> extractedFeatures =
        constructorController.extractSelectedFeatures(fg1, "fg1", requestedFeatures);
    // Make sure both features have been returned.
    Assert.assertEquals(2, extractedFeatures.size());
  }

  @Test
  public void testExtractFeaturesBothSides() {
    FeaturegroupController fgControllerClass = Mockito.mock(FeaturegroupController.class);
    ConstructorController constructorController = new ConstructorController(fgControllerClass);

    Mockito.when(fgControllerClass.getFeatures(Mockito.any())).thenReturn(fg1Features, fg2Features);

    List<FeatureDTO> requestedFeatures = new ArrayList<>();
    requestedFeatures.add(new FeatureDTO("*"));
    Query query = new Query(fg1, requestedFeatures, fg2, requestedFeatures);

    List<FeatureDTO> extractedFeatures = constructorController.extractSelectedFeatures(query);
    // Make sure both features have been returned.
    Assert.assertEquals(4, extractedFeatures.size());
    // Make sure the method sets the feature group name
    Assert.assertTrue(extractedFeatures.get(0).getFeaturegroup().equals("fg1") ||
        extractedFeatures.get(0).getFeaturegroup().equals("fg2") );
  }

  @Test
  public void testExtractSelectedFeaturesRecursive() {
    FeaturegroupController fgControllerClass = Mockito.mock(FeaturegroupController.class);
    ConstructorController constructorController = new ConstructorController(fgControllerClass);

    Mockito.when(fgControllerClass.getFeatures(Mockito.any())).thenReturn(fg1Features, fg2Features, fg3Features);

    List<FeatureDTO> requestedFeatures = new ArrayList<>();
    requestedFeatures.add(new FeatureDTO("*"));
    Query innerQuery = new Query(fg2, requestedFeatures, fg3, requestedFeatures);

    Query query = new Query();
    query.setQuery(innerQuery);

    requestedFeatures = new ArrayList<>();
    requestedFeatures.add(new FeatureDTO("fg1_ft1"));
    query.setLeftFeatures(requestedFeatures);

    List<FeatureDTO> extractedFeatures = constructorController.extractSelectedFeatures(query);
    // Make sure both features have been returned.
    Assert.assertEquals(5, extractedFeatures.size());
    Assert.assertEquals(1, extractedFeatures.stream().filter(f -> f.getFeaturegroup().equals("fg1")).count());
    Assert.assertEquals(2, extractedFeatures.stream().filter(f -> f.getFeaturegroup().equals("fg2")).count());
    Assert.assertEquals(2, extractedFeatures.stream().filter(f -> f.getFeaturegroup().equals("fg3")).count());
  }

  @Test
  public void testExtractJoinOn() {
    ConstructorController constructorController = new ConstructorController();

    List<FeatureDTO> availableLeft = new ArrayList<>();
    availableLeft.add(new FeatureDTO("ft1", "Integer", ""));
    availableLeft.add(new FeatureDTO("ft2", "String", ""));
    availableLeft.add(new FeatureDTO("fg1_ft3", "Float", ""));

    List<FeatureDTO> availableRight = new ArrayList<>();
    availableRight.add(new FeatureDTO("ft1", "Integer", ""));
    availableRight.add(new FeatureDTO("ft2", "String", ""));
    availableRight.add(new FeatureDTO("fg2_ft3", "Float", ""));

    List<FeatureDTO> on = new ArrayList<>();
    on.add(new FeatureDTO("ft1"));
    on.add(new FeatureDTO("ft2"));

    Query query = new Query(fg1, fg2);
    query.setLeftAvailableFeatures(availableLeft);
    query.setRightAvailableFeatures(availableRight);
    query.setOn(on);

    Join join = constructorController.extractOn(query);
    Assert.assertEquals(2, join.getOn().size());
  }

  @Test
  public void testExtractJoinOnMissingFeature() {
    ConstructorController constructorController = new ConstructorController();

    List<FeatureDTO> availableLeft = new ArrayList<>();
    availableLeft.add(new FeatureDTO("fg1_ft2", "Float", ""));

    List<FeatureDTO> availableRight = new ArrayList<>();
    availableRight.add(new FeatureDTO("ft1", "String", ""));
    availableRight.add(new FeatureDTO("fg2_ft2", "Float", ""));

    List<FeatureDTO> on = new ArrayList<>();
    on.add(new FeatureDTO("ft1"));

    Query query = new Query(fg1, fg2);
    query.setLeftAvailableFeatures(availableLeft);
    query.setRightAvailableFeatures(availableRight);
    query.setOn(on);

    thrown.expect(IllegalArgumentException.class);
    constructorController.extractOn(query);
  }

  @Test
  public void testExtractJoinOnWrongTypes() {
    ConstructorController constructorController = new ConstructorController();

    List<FeatureDTO> availableLeft = new ArrayList<>();
    availableLeft.add(new FeatureDTO("ft1", "Integer", ""));

    List<FeatureDTO> availableRight = new ArrayList<>();
    availableRight.add(new FeatureDTO("ft1", "Float", ""));

    List<FeatureDTO> on = new ArrayList<>();
    on.add(new FeatureDTO("ft1"));

    Query query = new Query(fg1, fg2);
    query.setLeftAvailableFeatures(availableLeft);
    query.setRightAvailableFeatures(availableRight);
    query.setOn(on);

    thrown.expect(IllegalArgumentException.class);
    constructorController.extractOn(query);
  }

  @Test
  public void testExtractJoinLeftRight() {
    ConstructorController constructorController = new ConstructorController();

    List<FeatureDTO> availableLeft = new ArrayList<>();
    availableLeft.add(new FeatureDTO("fg1_ft3", "Float", ""));

    List<FeatureDTO> availableRight = new ArrayList<>();
    availableRight.add(new FeatureDTO("fg2_ft3", "Float", ""));

    List<FeatureDTO> leftOn = Arrays.asList(new FeatureDTO("fg1_ft3"));
    List<FeatureDTO> rightOn = Arrays.asList(new FeatureDTO("fg2_ft3"));

    Query query = new Query(fg1, fg2);
    query.setLeftAvailableFeatures(availableLeft);
    query.setRightAvailableFeatures(availableRight);
    query.setLeftOn(leftOn);
    query.setRightOn(rightOn);

    Join join = constructorController.extractLeftRightOn(query);
    Assert.assertEquals(1, join.getLeftOn().size());
    Assert.assertEquals(1, join.getRightOn().size());
  }

  @Test
  public void testExtractJoinLeftRightWrongSizes() {
    ConstructorController constructorController = new ConstructorController();

    List<FeatureDTO> availableLeft = new ArrayList<>();
    availableLeft.add(new FeatureDTO("fg1_ft3", "Float", ""));

    List<FeatureDTO> availableRight = new ArrayList<>();
    availableRight.add(new FeatureDTO("fg2_ft3", "Float", ""));

    List<FeatureDTO> leftOn = Arrays.asList(new FeatureDTO("fg1_ft3"), new FeatureDTO("additional"));
    List<FeatureDTO> rightOn = Arrays.asList(new FeatureDTO("fg2_ft3"));

    Query query = new Query(fg1, fg2);
    query.setLeftAvailableFeatures(availableLeft);
    query.setRightAvailableFeatures(availableRight);
    query.setLeftOn(leftOn);
    query.setRightOn(rightOn);

    thrown.expect(IllegalArgumentException.class);
    constructorController.extractLeftRightOn(query);
  }

  @Test
  public void testExtractJoinLeftRightWrongTypes() {
    ConstructorController constructorController = new ConstructorController();

    List<FeatureDTO> availableLeft = new ArrayList<>();
    availableLeft.add(new FeatureDTO("fg1_ft3", "Integer", ""));

    List<FeatureDTO> availableRight = new ArrayList<>();
    availableRight.add(new FeatureDTO("fg2_ft3", "String", ""));

    List<FeatureDTO> leftOn = Arrays.asList(new FeatureDTO("fg1_ft3"));
    List<FeatureDTO> rightOn = Arrays.asList(new FeatureDTO("fg2_ft3"));

    Query query = new Query(fg1, fg2);
    query.setLeftAvailableFeatures(availableLeft);
    query.setRightAvailableFeatures(availableRight);
    query.setLeftOn(leftOn);
    query.setRightOn(rightOn);

    thrown.expect(IllegalArgumentException.class);
    constructorController.extractLeftRightOn(query);
  }

  @Test
  public void testExtractJoinLeftRightMissingFeature() {
    ConstructorController constructorController = new ConstructorController();

    List<FeatureDTO> availableLeft = new ArrayList<>();
    availableLeft.add(new FeatureDTO("fg1_ft3", "String", ""));

    List<FeatureDTO> availableRight = new ArrayList<>();
    availableRight.add(new FeatureDTO("fg2_ft3", "String", ""));

    List<FeatureDTO> leftOn = Arrays.asList(new FeatureDTO("fg1_ft3"));
    List<FeatureDTO> rightOn = Arrays.asList(new FeatureDTO("fg2_ft1"));

    Query query = new Query(fg1, fg2);
    query.setLeftAvailableFeatures(availableLeft);
    query.setRightAvailableFeatures(availableRight);
    query.setLeftOn(leftOn);
    query.setRightOn(rightOn);

    thrown.expect(IllegalArgumentException.class);
    constructorController.extractLeftRightOn(query);
  }

  @Test
  public void testNoJoiningKeySingle() {
    ConstructorController constructorController = new ConstructorController();

    List<FeatureDTO> availableLeft = new ArrayList<>();
    availableLeft.add(new FeatureDTO("ft1", "String", "", true));

    List<FeatureDTO> availableRight = new ArrayList<>();
    availableRight.add(new FeatureDTO("ft1", "String", "", true));

    Query query = new Query(fg1, fg2);
    query.setLeftAvailableFeatures(availableLeft);
    query.setRightAvailableFeatures(availableRight);

    Join join = constructorController.extractPrimaryKeysJoin(query);
    Assert.assertEquals(1, join.getOn().size());
  }

  @Test
  public void testNoJoiningKeyMultipleDifferentTypes() {
    ConstructorController constructorController = new ConstructorController();

    List<FeatureDTO> availableLeft = new ArrayList<>();
    availableLeft.add(new FeatureDTO("ft1", "String", "", true));
    availableLeft.add(new FeatureDTO("ft2", "Integer", "", true));

    List<FeatureDTO> availableRight = new ArrayList<>();
    availableRight.add(new FeatureDTO("ft1", "String", "", true));
    availableRight.add(new FeatureDTO("ft2", "Float", "", true));

    Query query = new Query(fg1, fg2);
    query.setLeftAvailableFeatures(availableLeft);
    query.setRightAvailableFeatures(availableRight);

    Join join = constructorController.extractPrimaryKeysJoin(query);
    Assert.assertEquals(1, join.getOn().size());
  }

  @Test
  public void testNoJoiningKeyMultipleDifferentSizes() {
    ConstructorController constructorController = new ConstructorController();

    List<FeatureDTO> availableLeft = new ArrayList<>();
    availableLeft.add(new FeatureDTO("ft1", "String", "", true));
    availableLeft.add(new FeatureDTO("ft2", "Float", "", true));
    availableLeft.add(new FeatureDTO("ft4", "Integer", "", true));

    List<FeatureDTO> availableRight = new ArrayList<>();
    availableRight.add(new FeatureDTO("ft1", "String", "", true));
    availableRight.add(new FeatureDTO("ft2", "Float", "", true));
    availableRight.add(new FeatureDTO("ft3", "Integer", "", true));

    Query query = new Query(fg1, fg2);
    query.setLeftAvailableFeatures(availableLeft);
    query.setRightAvailableFeatures(availableRight);

    Join join = constructorController.extractPrimaryKeysJoin(query);
    Assert.assertEquals(2, join.getOn().size());
  }

  @Test
  public void testNoPrimaryKeys() {
    ConstructorController constructorController = new ConstructorController();

    List<FeatureDTO> availableLeft = new ArrayList<>();
    availableLeft.add(new FeatureDTO("ft1", "String", "", false));

    List<FeatureDTO> availableRight = new ArrayList<>();
    availableRight.add(new FeatureDTO("ft1", "String", "", false));

    Query query = new Query(fg1, fg2);
    query.setLeftAvailableFeatures(availableLeft);
    query.setRightAvailableFeatures(availableRight);

    thrown.expect(IllegalArgumentException.class);
    constructorController.extractPrimaryKeysJoin(query);
  }

  @Test
  public void testNoJoiningKeyWrongTypes() {
    ConstructorController constructorController = new ConstructorController();

    List<FeatureDTO> availableLeft = new ArrayList<>();
    availableLeft.add(new FeatureDTO("ft1", "String", "", true));

    List<FeatureDTO> availableRight = new ArrayList<>();
    availableRight.add(new FeatureDTO("ft1", "Integer", "", true));

    Query query = new Query(fg1, fg2);
    query.setLeftAvailableFeatures(availableLeft);
    query.setRightAvailableFeatures(availableRight);

    thrown.expect(IllegalArgumentException.class);
    constructorController.extractPrimaryKeysJoin(query);
  }

  @Test
  public void testGenerateSQLRecursive() {
    // TODO(Fabio)
  }

  // Test simple get with no join
  @Test
  public void testSingleSide() {
    ConstructorController constructorController = new ConstructorController();

  }
}
