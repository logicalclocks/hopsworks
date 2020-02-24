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

package io.hops.hopsworks.common.featurestore.query;

import io.hops.hopsworks.common.dao.featurestore.Featurestore;
import io.hops.hopsworks.common.dao.featurestore.featuregroup.Featuregroup;
import io.hops.hopsworks.common.featurestore.FeaturestoreFacade;
import io.hops.hopsworks.common.featurestore.feature.FeatureDTO;
import io.hops.hopsworks.common.featurestore.featuregroup.FeaturegroupController;
import io.hops.hopsworks.common.featurestore.featuregroup.FeaturegroupDTO;
import io.hops.hopsworks.common.featurestore.featuregroup.FeaturegroupFacade;
import io.hops.hopsworks.exceptions.FeaturestoreException;
import org.apache.calcite.sql.JoinType;
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

  private Featurestore fs;

  private Featuregroup fg1;
  private Featuregroup fg2;
  private Featuregroup fg3;

  private List<FeatureDTO> fg1Features = new ArrayList<>();
  private List<FeatureDTO> fg2Features = new ArrayList<>();
  private List<FeatureDTO> fg3Features = new ArrayList<>();

  private FeaturegroupController featuregroupController;
  private FeaturestoreFacade featurestoreFacade;
  private FeaturegroupFacade featuregroupFacade;

  private ConstructorController constructorController;

  @Rule
  public ExpectedException thrown = ExpectedException.none();

  @Before
  public void setup() {
    fs = new Featurestore();
    fs.setHiveDbId(1l);
    fg1 = new Featuregroup(1);
    fg1.setName("fg1");
    fg1.setVersion(1);
    fg1.setFeaturestore(fs);
    fg2 = new Featuregroup(2);
    fg2.setName("fg2");
    fg2.setVersion(1);
    fg2.setFeaturestore(fs);
    fg3 = new Featuregroup(3);
    fg3.setName("fg3");
    fg3.setVersion(1);
    fg3.setFeaturestore(fs);

    fg1Features = new ArrayList<>();
    fg1Features.add(new FeatureDTO("pr", "Integer", "", true, false, ""));
    fg1Features.add(new FeatureDTO("fg1_ft2", "String", "", false, false, ""));

    fg2Features = new ArrayList<>();
    fg2Features.add(new FeatureDTO("pr", "Integer", "", true, false, ""));
    fg2Features.add(new FeatureDTO("fg2_ft2", "String", "", false, false, ""));

    fg3Features = new ArrayList<>();
    fg3Features.add(new FeatureDTO("fg3_ft1", "Integer", "", true, false, ""));
    fg3Features.add(new FeatureDTO("fg3_ft2", "String", "", false, false, ""));

    featuregroupController = Mockito.mock(FeaturegroupController.class);
    featuregroupFacade = Mockito.mock(FeaturegroupFacade.class);
    featurestoreFacade = Mockito.mock(FeaturestoreFacade.class);

    constructorController = new ConstructorController(featuregroupController, featurestoreFacade, featuregroupFacade);
  }

  @Test
  public void testValidateFeatures() throws Exception {
    List<FeatureDTO> requestedFeatures = new ArrayList<>();
    requestedFeatures.add(new FeatureDTO("fg1_ft2"));

    List<FeatureDTO> extractedFeatures =
        constructorController.validateFeatures(fg1, "fg1", requestedFeatures, fg1Features);
    Assert.assertEquals(1, extractedFeatures.size());
    Assert.assertEquals("fg1_ft2", extractedFeatures.get(0).getName());
    // Make sure the object returned is the one for the DB with more infomation in (e.g. Type, Primary key)
    Assert.assertFalse(extractedFeatures.get(0).getPrimary());
  }

  @Test
  public void testMissingFeature() throws Exception {
    List<FeatureDTO> requestedFeatures = new ArrayList<>();
    requestedFeatures.add(new FeatureDTO("fg1_ft3"));

    thrown.expect(FeaturestoreException.class);
    constructorController.validateFeatures(fg1, "fg1", requestedFeatures, fg1Features);
  }

  @Test
  public void testExtractAllFeatures() throws Exception {
    List<FeatureDTO> requestedFeatures = new ArrayList<>();
    requestedFeatures.add(new FeatureDTO("*"));

    List<FeatureDTO> extractedFeatures =
        constructorController.validateFeatures(fg1, "fg1", requestedFeatures, fg1Features);
    // Make sure both features have been returned.
    Assert.assertEquals(2, extractedFeatures.size());
  }

  @Test
  public void testExtractFeaturesBothSides() throws Exception {
    Mockito.when(featuregroupController.getFeatures(Mockito.any())).thenReturn(fg1Features, fg2Features);
    Mockito.when(featuregroupFacade.findById(Mockito.any())).thenReturn(fg1, fg2);
    Mockito.when(featurestoreFacade.getHiveDbName(Mockito.any())).thenReturn("fg1", "fg2");

    FeaturegroupDTO fg1 = new FeaturegroupDTO();
    fg1.setId(1);
    FeaturegroupDTO fg2 = new FeaturegroupDTO();
    fg2.setId(2);

    List<FeatureDTO> requestedFeatures = new ArrayList<>();
    requestedFeatures.add(new FeatureDTO("*"));

    QueryDTO rightQueryDTO = new QueryDTO(fg2, requestedFeatures);
    JoinDTO joinDTO = new JoinDTO(rightQueryDTO, null, null);

    QueryDTO queryDTO = new QueryDTO(fg1, requestedFeatures, Arrays.asList(joinDTO));

    Query query = constructorController.convertQueryDTO(queryDTO, 1);

    List<FeatureDTO> extractedFeatures = constructorController.collectFeatures(query);
    // Make sure both features have been returned.
    Assert.assertEquals(4, extractedFeatures.size());
    // Make sure the method sets the feature group name
    Assert.assertTrue(extractedFeatures.get(0).getFeaturegroup().equals("fg1") ||
        extractedFeatures.get(0).getFeaturegroup().equals("fg2") );
  }

  @Test
  public void testExtractJoinOn() throws Exception {
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

    Query leftQuery = new Query("fs1", fg1, "fg1", availableLeft, availableLeft);
    Query rightQuery = new Query("fs1", fg2, "fg1", availableRight, availableRight);

    Join join = constructorController.extractOn(leftQuery, rightQuery, on, JoinType.INNER);
    Assert.assertEquals(2, join.getOn().size());
  }

  @Test
  public void testExtractJoinOnMissingFeature() throws Exception {
    ConstructorController constructorController = new ConstructorController();

    List<FeatureDTO> availableLeft = new ArrayList<>();
    availableLeft.add(new FeatureDTO("fg1_ft2", "Float", ""));

    List<FeatureDTO> availableRight = new ArrayList<>();
    availableRight.add(new FeatureDTO("ft1", "String", ""));
    availableRight.add(new FeatureDTO("fg2_ft2", "Float", ""));

    List<FeatureDTO> on = new ArrayList<>();
    on.add(new FeatureDTO("ft1"));

    Query leftQuery = new Query("fs1", fg1, "fg1", availableLeft, availableLeft);
    Query rightQuery = new Query("fs1", fg2, "fg1", availableRight, availableRight);

    thrown.expect(FeaturestoreException.class);
    constructorController.extractOn(leftQuery, rightQuery, on, JoinType.INNER);
  }

  @Test
  public void testExtractJoinLeftRight() throws Exception {
    ConstructorController constructorController = new ConstructorController();

    List<FeatureDTO> availableLeft = new ArrayList<>();
    availableLeft.add(new FeatureDTO("fg1_ft3", "Float", ""));

    List<FeatureDTO> availableRight = new ArrayList<>();
    availableRight.add(new FeatureDTO("fg2_ft3", "Float", ""));

    List<FeatureDTO> leftOn = Arrays.asList(new FeatureDTO("fg1_ft3"));
    List<FeatureDTO> rightOn = Arrays.asList(new FeatureDTO("fg2_ft3"));

    Query leftQuery = new Query("fs1", fg1, "fg1", availableLeft, availableLeft);
    Query rightQuery = new Query("fs1", fg2, "fg1", availableRight, availableRight);

    Join join = constructorController.extractLeftRightOn(leftQuery, rightQuery, leftOn, rightOn, JoinType.INNER);
    Assert.assertEquals(1, join.getLeftOn().size());
    Assert.assertEquals(1, join.getRightOn().size());
  }

  @Test
  public void testExtractJoinLeftRightWrongSizes() throws Exception {
    ConstructorController constructorController = new ConstructorController();

    List<FeatureDTO> availableLeft = new ArrayList<>();
    availableLeft.add(new FeatureDTO("fg1_ft3", "Float", ""));

    List<FeatureDTO> availableRight = new ArrayList<>();
    availableRight.add(new FeatureDTO("fg2_ft3", "Float", ""));

    List<FeatureDTO> leftOn = Arrays.asList(new FeatureDTO("fg1_ft3"), new FeatureDTO("additional"));
    List<FeatureDTO> rightOn = Arrays.asList(new FeatureDTO("fg2_ft3"));

    Query leftQuery = new Query("fs1", fg1, "fg1", availableLeft, availableLeft);
    Query rightQuery = new Query("fs1", fg2, "fg1", availableRight, availableRight);

    thrown.expect(FeaturestoreException.class);
    constructorController.extractLeftRightOn(leftQuery, rightQuery, leftOn, rightOn, JoinType.INNER);
  }


  @Test
  public void testExtractJoinLeftRightMissingFeature() throws Exception {
    ConstructorController constructorController = new ConstructorController();

    List<FeatureDTO> availableLeft = new ArrayList<>();
    availableLeft.add(new FeatureDTO("fg1_ft3", "String", ""));

    List<FeatureDTO> availableRight = new ArrayList<>();
    availableRight.add(new FeatureDTO("fg2_ft3", "String", ""));

    List<FeatureDTO> leftOn = Arrays.asList(new FeatureDTO("fg1_ft3"));
    List<FeatureDTO> rightOn = Arrays.asList(new FeatureDTO("fg2_ft1"));

    Query leftQuery = new Query("fs1", fg1, "fg1", availableLeft, availableLeft);
    Query rightQuery = new Query("fs1", fg2, "fg1", availableRight, availableRight);

    thrown.expect(FeaturestoreException.class);
    constructorController.extractLeftRightOn(leftQuery, rightQuery, leftOn, rightOn, JoinType.INNER);
  }

  @Test
  public void testNoJoiningKeySingle() throws Exception {
    ConstructorController constructorController = new ConstructorController();

    List<FeatureDTO> availableLeft = new ArrayList<>();
    availableLeft.add(new FeatureDTO("ft1", "String", "", true));

    List<FeatureDTO> availableRight = new ArrayList<>();
    availableRight.add(new FeatureDTO("ft1", "String", "", true));

    Query leftQuery = new Query("fs1", fg1, "fg1", availableLeft, availableLeft);
    Query rightQuery = new Query("fs1", fg2, "fg1", availableRight, availableRight);

    Join join = constructorController.extractPrimaryKeysJoin(leftQuery, rightQuery, JoinType.INNER);
    Assert.assertEquals(1, join.getOn().size());
  }

  @Test
  public void testNoJoiningKeyMultipleDifferentSizes() throws Exception {
    ConstructorController constructorController = new ConstructorController();

    List<FeatureDTO> availableLeft = new ArrayList<>();
    availableLeft.add(new FeatureDTO("ft1", "String", "", true));
    availableLeft.add(new FeatureDTO("ft2", "Float", "", true));
    availableLeft.add(new FeatureDTO("ft4", "Integer", "", true));

    List<FeatureDTO> availableRight = new ArrayList<>();
    availableRight.add(new FeatureDTO("ft1", "String", "", true));
    availableRight.add(new FeatureDTO("ft2", "Float", "", true));
    availableRight.add(new FeatureDTO("ft3", "Integer", "", true));

    Query leftQuery = new Query("fs1", fg1, "fg1", availableLeft, availableLeft);
    Query rightQuery = new Query("fs1", fg2, "fg1", availableRight, availableRight);

    Join join = constructorController.extractPrimaryKeysJoin(leftQuery, rightQuery, JoinType.INNER);
    Assert.assertEquals(2, join.getOn().size());
  }

  @Test
  public void testNoPrimaryKeys() throws Exception {
    ConstructorController constructorController = new ConstructorController();

    List<FeatureDTO> availableLeft = new ArrayList<>();
    availableLeft.add(new FeatureDTO("ft1", "String", "", false));

    List<FeatureDTO> availableRight = new ArrayList<>();
    availableRight.add(new FeatureDTO("ft1", "String", "", false));

    Query leftQuery = new Query("fs1", fg1, "fg1", availableLeft, availableLeft);
    Query rightQuery = new Query("fs1", fg2, "fg1", availableRight, availableRight);

    thrown.expect(FeaturestoreException.class);
    constructorController.extractPrimaryKeysJoin(leftQuery, rightQuery, JoinType.INNER);
  }

  @Test
  public void testSingleSideSQLQuery() {
    ConstructorController constructorController = new ConstructorController();

    List<FeatureDTO> availableLeft = new ArrayList<>();
    availableLeft.add(new FeatureDTO("ft1", "String", "", "fg0", true));

    Query singleSideQuery = new Query("fs1", fg1, "fg0", availableLeft, availableLeft);
    String query = constructorController.generateSQL(singleSideQuery).replace("\n", " ");
    Assert.assertEquals("SELECT fg0.ft1 FROM fs1.fg1_1 fg0", query);
  }

  @Test
  public void testSingleJoinSQLQuery() {
    ConstructorController constructorController = new ConstructorController();

    List<FeatureDTO> availableLeft = new ArrayList<>();
    availableLeft.add(new FeatureDTO("ft1", "String", "", "fg0", true));

    List<FeatureDTO> availableRight = new ArrayList<>();
    availableRight.add(new FeatureDTO("ft1", "Integer", "", "fg1", true));

    Query rightQuery = new Query("fs1", fg2, "fg0", availableRight, availableRight);
    Query leftQuery = new Query("fs1", fg1, "fg1", availableLeft, availableLeft);
    Join join = new Join(leftQuery, rightQuery, availableLeft, JoinType.INNER);
    leftQuery.setJoins(Arrays.asList(join));

    String query = constructorController.generateSQL(leftQuery).replace("\n", " ");
    Assert.assertEquals("SELECT fg0.ft1 FROM fs1.fg1_1 fg1 INNER JOIN " +
        "fs1.fg2_1 fg0 ON fg1.ft1 = fg0.ft1", query);
  }

  @Test
  public void testTreeWayJoinSQLNode() {
    ConstructorController constructorController = new ConstructorController();

    List<FeatureDTO> availableLeft = new ArrayList<>();
    availableLeft.add(new FeatureDTO("ft1", "String", "", "fg0", true));

    List<FeatureDTO> availableSecond = new ArrayList<>();
    availableSecond.add(new FeatureDTO("ft2", "Integer", "", "fg1", true));

    List<FeatureDTO> availableThird = new ArrayList<>();
    availableThird.add(new FeatureDTO("ft1", "Integer", "", "fg2", true));

    Query thirdQuery = new Query("fs1", fg3,"fg2", availableThird, availableThird);
    Query secondQuery = new Query("fs1", fg2, "fg1", availableSecond , availableSecond);
    Query leftQuery = new Query("fs1", fg1, "fg0", availableLeft, availableLeft);
    Join join = new Join(leftQuery, secondQuery, availableLeft, availableSecond, JoinType.INNER);
    Join secondJoin = new Join(leftQuery, thirdQuery, availableLeft, JoinType.INNER);
    leftQuery.setJoins(Arrays.asList(join, secondJoin));

    String query = constructorController.generateSQL(leftQuery).replace("\n", " ");
    Assert.assertEquals("SELECT fg0.ft1, fg1.ft2 " +
        "FROM fs1.fg1_1 fg0 " +
        "INNER JOIN fs1.fg2_1 fg1 ON fg0.ft1 = fg1.ft2 " +
        "INNER JOIN fs1.fg3_1 fg2 ON fg0.ft1 = fg2.ft1", query);
  }
}
