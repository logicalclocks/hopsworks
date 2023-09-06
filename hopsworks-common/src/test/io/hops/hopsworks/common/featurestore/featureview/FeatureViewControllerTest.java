/*
 * This file is part of Hopsworks
 * Copyright (C) 2023, Hopsworks AB. All rights reserved
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

package io.hops.hopsworks.common.featurestore.featureview;

import com.google.common.collect.Lists;
import io.hops.hopsworks.common.featurestore.feature.FeatureGroupFeatureDTO;
import io.hops.hopsworks.common.featurestore.featuregroup.FeaturegroupController;
import io.hops.hopsworks.persistence.entity.featurestore.featuregroup.Featuregroup;
import io.hops.hopsworks.persistence.entity.featurestore.featuregroup.cached.CachedFeature;
import io.hops.hopsworks.persistence.entity.featurestore.featuregroup.cached.CachedFeatureExtraConstraints;
import io.hops.hopsworks.persistence.entity.featurestore.featuregroup.cached.CachedFeaturegroup;
import io.hops.hopsworks.persistence.entity.featurestore.featureview.FeatureView;
import io.hops.hopsworks.persistence.entity.featurestore.featureview.ServingKey;
import io.hops.hopsworks.persistence.entity.featurestore.trainingdataset.TrainingDatasetFeature;
import io.hops.hopsworks.persistence.entity.featurestore.trainingdataset.TrainingDatasetJoin;
import io.hops.hopsworks.persistence.entity.featurestore.trainingdataset.TrainingDatasetJoinCondition;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;

public class FeatureViewControllerTest {
  private FeatureViewController target = new FeatureViewController();
  private FeaturegroupController featuregroupController = Mockito.mock(FeaturegroupController.class);
  private Featuregroup fgOneKey1;
  private Featuregroup fgOneKey2;
  private Featuregroup fgOneKeyDiffName;
  private Featuregroup fgTwoKey1;
  private Featuregroup fgNoKey5;
  private Featuregroup fgLabel6;

  private List<TrainingDatasetFeature> tdf1;
  private List<TrainingDatasetFeature> tdf2;
  private List<TrainingDatasetFeature> tdf3;
  private List<TrainingDatasetFeature> tdf4;
  private List<TrainingDatasetFeature> tdf5;
  private List<TrainingDatasetFeature> tdf6;


  private TrainingDatasetJoin join1;
  private TrainingDatasetJoin join2;
  private TrainingDatasetJoin join3;
  private TrainingDatasetJoin join4;
  private TrainingDatasetJoin join5;
  private TrainingDatasetJoin join6;

  @Before
  public void before() {
    target.setFeaturegroupController(featuregroupController);
    fgOneKey1 = createFg(1, "", 1, false);
    fgOneKey2 = createFg(1, "", 2, false);
    fgOneKeyDiffName = createFg(1, "diff_", 3, false);
    fgTwoKey1 = createFg(2, "", 4, false);
    fgNoKey5 = createFg(0, "", 1, false);
    fgLabel6 = createFg(1, "", 6, true);

    tdf1 = getTdFeature(fgOneKey1);
    join1 = new TrainingDatasetJoin();
    join1.setFeatureGroup(fgOneKey1);
    join1.setFeatures(tdf1);

    tdf2 = getTdFeature(fgOneKey2);
    join2 = new TrainingDatasetJoin();
    join2.setFeatureGroup(fgOneKey2);
    join2.setFeatures(tdf2);

    tdf3 = getTdFeature(fgOneKeyDiffName);
    join3 = new TrainingDatasetJoin();
    join3.setFeatureGroup(fgOneKeyDiffName);
    join3.setFeatures(tdf3);

    tdf4 = getTdFeature(fgTwoKey1);
    join4 = new TrainingDatasetJoin();
    join4.setFeatureGroup(fgTwoKey1);
    join4.setFeatures(tdf4);

    tdf5 = getTdFeature(fgNoKey5);
    join5 = new TrainingDatasetJoin();
    join5.setFeatureGroup(fgNoKey5);
    join5.setFeatures(tdf5);

    tdf6 = getTdFeatureLabelOnly(fgLabel6);
    join6 = new TrainingDatasetJoin();
    join6.setFeatureGroup(fgLabel6);
    join6.setFeatures(tdf6);
  }

  public Featuregroup createFg(Integer nPk, String pkPrefix, Integer fgId, Boolean label) {
    Featuregroup fg1 = new Featuregroup();
    CachedFeaturegroup cacheFg1 = new CachedFeaturegroup();
    fg1.setCachedFeaturegroup(cacheFg1);
    fg1.setId(fgId);

    CachedFeature feature1 = new CachedFeature();
    feature1.setName("feature1");
    CachedFeature feature2 = new CachedFeature();
    feature2.setName("feature2");
    List<CachedFeature> cacheFeatures = Lists.newArrayList(feature1, feature2);
    List<CachedFeatureExtraConstraints> constraints = Lists.newArrayList();
    for (Integer i = 1; i < nPk + 1; i++) {
      CachedFeature pk1 = new CachedFeature();
      pk1.setName(pkPrefix + "pk" + i);
      cacheFeatures.add(pk1);
      CachedFeatureExtraConstraints constraint1 = new CachedFeatureExtraConstraints();
      constraint1.setPrimary(true);
      constraint1.setName(pk1.getName());
      constraints.add(constraint1);
    }
    cacheFg1.setCachedFeatures(cacheFeatures);
    cacheFg1.setFeaturesExtraConstraints(constraints);
    return fg1;
  }

  private List<TrainingDatasetFeature> getTdFeature(Featuregroup fg) {
    List<TrainingDatasetFeature> tdfs = Lists.newArrayList();
    for (CachedFeature feature : fg.getCachedFeaturegroup().getCachedFeatures()) {
      TrainingDatasetFeature tdf = new TrainingDatasetFeature();
      tdf.setFeatureGroup(fg);
      tdf.setName(feature.getName());
      tdfs.add(tdf);
    }
    return tdfs;
  }

  private List<TrainingDatasetFeature> getTdFeatureLabelOnly(Featuregroup fg) {
    List<TrainingDatasetFeature> tdfs = Lists.newArrayList();
    TrainingDatasetFeature tdf = new TrainingDatasetFeature();
    tdf.setFeatureGroup(fg);
    tdf.setName("label");
    tdf.setLabel(true);
    tdfs.add(tdf);
    return tdfs;
  }

  private List<FeatureGroupFeatureDTO> getFgFeature(Featuregroup fg) {
    List<FeatureGroupFeatureDTO> tdfs = Lists.newArrayList();
    for (CachedFeature feature : fg.getCachedFeaturegroup().getCachedFeatures()) {
      FeatureGroupFeatureDTO tdf = new FeatureGroupFeatureDTO();
      tdf.setFeatureGroupId(fg.getId());
      tdf.setName(feature.getName());
      if (feature.getName().contains("pk")) {
        tdf.setPrimary(true);
      } else {
        tdf.setPrimary(false);
      }
      tdfs.add(tdf);
    }
    return tdfs;
  }

  @Test
  public void getServingKeys_singleFg() throws Exception {
    doReturn(getFgFeature(fgOneKey1)).when(featuregroupController).getFeatures(eq(fgOneKey1), any(), any());
    FeatureView fv = new FeatureView();
    List<TrainingDatasetFeature> tdfs = Lists.newArrayList();
    tdfs.addAll(tdf1);
    fv.setFeatures(tdfs);
    join1.setConditions(Lists.newArrayList());
    join1.setPrefix("");
    join1.setIdx(0);
    fv.setJoins(Lists.newArrayList(join1));

    List<ServingKey> actual = target.getServingKeys(null, null, fv);
    Assert.assertEquals(1, actual.size());
    validate(actual.get(0), true, "pk1", fgOneKey1, "", null);
  }

  @Test
  public void getServingKeys_joinSameKeyName() throws Exception {
    doReturn(getFgFeature(fgOneKey1)).when(featuregroupController).getFeatures(eq(fgOneKey1), any(), any());
    doReturn(getFgFeature(fgOneKey2)).when(featuregroupController).getFeatures(eq(fgOneKey2), any(), any());

    FeatureView fv = new FeatureView();
    // set tdf
    List<TrainingDatasetFeature> tdfs = Lists.newArrayList();
    tdfs.addAll(tdf1);
    tdfs.addAll(tdf2);
    fv.setFeatures(tdfs);
    // set join
    join1.setConditions(Lists.newArrayList());
    join1.setPrefix("");
    join1.setIdx(0);
    TrainingDatasetJoinCondition condition = new TrainingDatasetJoinCondition();
    condition.setLeftFeature("pk1");
    condition.setRightFeature("pk1");
    join2.setConditions(Lists.newArrayList(condition));
    join2.setPrefix("fg2_");
    join2.setIdx(1);
    fv.setJoins(Lists.newArrayList(join1, join2));

    List<ServingKey> actual = target.getServingKeys(null, null, fv);

    // validate
    Assert.assertEquals(2, actual.size());
    validate(actual.get(0), true, "pk1", fgOneKey1, "", null);
    validate(actual.get(1), false, "pk1", fgOneKey2, "fg2_", "pk1");
  }

  @Test
  public void getServingKeys_joinDifferentKeyName() throws Exception {
    doReturn(getFgFeature(fgOneKey1)).when(featuregroupController).getFeatures(eq(fgOneKey1), any(), any());
    doReturn(getFgFeature(fgOneKeyDiffName)).when(featuregroupController)
        .getFeatures(eq(fgOneKeyDiffName), any(), any());

    FeatureView fv = new FeatureView();
    // set tdf
    List<TrainingDatasetFeature> tdfs = Lists.newArrayList();
    tdfs.addAll(tdf1);
    tdfs.addAll(tdf3);
    fv.setFeatures(tdfs);
    // set join
    join1.setConditions(Lists.newArrayList());
    join1.setPrefix("");
    join1.setIdx(0);
    TrainingDatasetJoinCondition condition = new TrainingDatasetJoinCondition();
    condition.setLeftFeature("pk1");
    condition.setRightFeature("diff_pk1");
    join3.setConditions(Lists.newArrayList(condition));
    join3.setPrefix("fg3_");
    join3.setIdx(1);
    fv.setJoins(Lists.newArrayList(join1, join3));

    List<ServingKey> actual = target.getServingKeys(null, null, fv);

    // validate
    Assert.assertEquals(2, actual.size());
    validate(actual.get(0), true, "pk1", fgOneKey1, "", null);
    validate(actual.get(1), false, "diff_pk1", fgOneKeyDiffName, "fg3_", "pk1");
  }

  @Test
  public void getServingKeys_joinSelfJoinMultiKey() throws Exception {
    doReturn(getFgFeature(fgTwoKey1)).when(featuregroupController).getFeatures(eq(fgTwoKey1), any(), any());

    FeatureView fv = new FeatureView();
    // set tdf
    List<TrainingDatasetFeature> tdfs = Lists.newArrayList();
    tdfs.addAll(tdf4);
    tdfs.addAll(tdf4);
    fv.setFeatures(tdfs);
    // set join
    join4.setConditions(Lists.newArrayList());
    join4.setPrefix("");
    join4.setIdx(0);
    TrainingDatasetJoinCondition condition = new TrainingDatasetJoinCondition();
    condition.setLeftFeature("pk1");
    condition.setRightFeature("pk1");
    TrainingDatasetJoin selfJoin4 = new TrainingDatasetJoin();
    selfJoin4.setFeatureGroup(fgTwoKey1);
    selfJoin4.setFeatures(tdf4);
    selfJoin4.setConditions(Lists.newArrayList(condition));
    selfJoin4.setPrefix("fg4_");
    selfJoin4.setIdx(1);
    fv.setJoins(Lists.newArrayList(join4, selfJoin4));

    List<ServingKey> actual = target.getServingKeys(null, null, fv);

    // validate
    Assert.assertEquals(4, actual.size());
    validate(actual.get(0), true, "pk1", fgTwoKey1, "", null);
    validate(actual.get(1), true, "pk2", fgTwoKey1, "", null);
    validate(actual.get(2), false, "pk1", fgTwoKey1, "fg4_", "pk1");
    validate(actual.get(3), true, "pk2", fgTwoKey1, "fg4_", null);
  }

  @Test
  public void getServingKeys_join_left2key_right1key() throws Exception {
    doReturn(getFgFeature(fgOneKey1)).when(featuregroupController).getFeatures(eq(fgOneKey1), any(), any());
    doReturn(getFgFeature(fgTwoKey1)).when(featuregroupController).getFeatures(eq(fgTwoKey1), any(), any());

    FeatureView fv = new FeatureView();
    // set tdf
    List<TrainingDatasetFeature> tdfs = Lists.newArrayList();
    tdfs.addAll(tdf4);
    tdfs.addAll(tdf1);
    fv.setFeatures(tdfs);
    // set join
    join4.setConditions(Lists.newArrayList());
    join4.setPrefix("");
    join4.setIdx(0);
    TrainingDatasetJoinCondition condition = new TrainingDatasetJoinCondition();
    condition.setLeftFeature("pk1");
    condition.setRightFeature("pk1");
    join1.setConditions(Lists.newArrayList(condition));
    join1.setPrefix("");
    join1.setIdx(1);
    fv.setJoins(Lists.newArrayList(join4, join1));

    List<ServingKey> actual = target.getServingKeys(null, null, fv);

    // validate
    Assert.assertEquals(3, actual.size());
    validate(actual.get(0), true, "pk1", fgTwoKey1, "", null);
    validate(actual.get(1), true, "pk2", fgTwoKey1, "", null);
    validate(actual.get(2), false, "pk1", fgOneKey1, "", "pk1");
  }

  @Test
  public void getServingKeys_join_left2key_right1key_differentKeyName() throws Exception {
    doReturn(getFgFeature(fgOneKey1)).when(featuregroupController).getFeatures(eq(fgOneKey1), any(), any());
    doReturn(getFgFeature(fgTwoKey1)).when(featuregroupController).getFeatures(eq(fgTwoKey1), any(), any());

    FeatureView fv = new FeatureView();
    // set tdf
    List<TrainingDatasetFeature> tdfs = Lists.newArrayList();
    tdfs.addAll(tdf4);
    tdfs.addAll(tdf1);
    fv.setFeatures(tdfs);
    // set join
    join4.setConditions(Lists.newArrayList());
    join4.setPrefix("");
    join4.setIdx(0);
    TrainingDatasetJoinCondition condition = new TrainingDatasetJoinCondition();
    condition.setLeftFeature("pk2");
    condition.setRightFeature("pk1");
    join1.setConditions(Lists.newArrayList(condition));
    join1.setPrefix("");
    join1.setIdx(1);
    fv.setJoins(Lists.newArrayList(join4, join1));

    List<ServingKey> actual = target.getServingKeys(null, null, fv);
    Assert.assertEquals(3, actual.size());
    validate(actual.get(0), true, "pk1", fgTwoKey1, "", null);
    validate(actual.get(1), true, "pk2", fgTwoKey1, "", null);
    validate(actual.get(2), false, "pk1", fgOneKey1, "", "pk2");
  }

  @Test
  public void getServingKeys_join_left2key_right1key_differentKeyName_multipleTimes() throws Exception {
    doReturn(getFgFeature(fgOneKey1)).when(featuregroupController).getFeatures(eq(fgOneKey1), any(), any());
    doReturn(getFgFeature(fgTwoKey1)).when(featuregroupController).getFeatures(eq(fgTwoKey1), any(), any());

    FeatureView fv = new FeatureView();
    // set tdf
    List<TrainingDatasetFeature> tdfs = Lists.newArrayList();
    tdfs.addAll(tdf4);
    tdfs.addAll(tdf1);
    tdfs.addAll(tdf1);
    fv.setFeatures(tdfs);
    // set join
    join4.setConditions(Lists.newArrayList());
    join4.setPrefix("");
    join4.setIdx(0);
    TrainingDatasetJoinCondition condition = new TrainingDatasetJoinCondition();
    condition.setLeftFeature("pk2");
    condition.setRightFeature("pk1");
    join1.setConditions(Lists.newArrayList(condition));
    join1.setPrefix("id1_");
    join1.setIdx(1);

    TrainingDatasetJoin join1Second = new TrainingDatasetJoin();
    join1Second.setFeatureGroup(fgOneKey1);
    join1Second.setFeatures(tdf1);
    TrainingDatasetJoinCondition condition2 = new TrainingDatasetJoinCondition();
    condition2.setLeftFeature("pk1");
    condition2.setRightFeature("pk1");
    join1Second.setConditions(Lists.newArrayList(condition2));
    join1Second.setPrefix("id2_");
    join1Second.setIdx(2);
    fv.setJoins(Lists.newArrayList(join4, join1, join1Second));

    List<ServingKey> actual = target.getServingKeys(null, null, fv);
    Assert.assertEquals(4, actual.size());
    validate(actual.get(0), true, "pk1", fgTwoKey1, "", null);
    validate(actual.get(1), true, "pk2", fgTwoKey1, "", null);
    validate(actual.get(2), false, "pk1", fgOneKey1, "id1_", "pk2");
    validate(actual.get(3), false, "pk1", fgOneKey1, "id2_", "pk1");
  }

  @Test
  public void getServingKeys_join_left1key_right2key() throws Exception {
    doReturn(getFgFeature(fgOneKey1)).when(featuregroupController).getFeatures(eq(fgOneKey1), any(), any());
    doReturn(getFgFeature(fgTwoKey1)).when(featuregroupController).getFeatures(eq(fgTwoKey1), any(), any());

    FeatureView fv = new FeatureView();
    // set tdf
    List<TrainingDatasetFeature> tdfs = Lists.newArrayList();
    tdfs.addAll(tdf4);
    tdfs.addAll(tdf1);
    fv.setFeatures(tdfs);
    // set join
    join1.setConditions(Lists.newArrayList());
    join1.setPrefix("");
    join1.setIdx(0);
    TrainingDatasetJoinCondition condition = new TrainingDatasetJoinCondition();
    condition.setLeftFeature("pk1");
    condition.setRightFeature("pk1");
    join4.setConditions(Lists.newArrayList(condition));
    join4.setPrefix("");
    join4.setIdx(1);
    fv.setJoins(Lists.newArrayList(join1, join4));

    List<ServingKey> actual = target.getServingKeys(null, null, fv);

    // validate
    Assert.assertEquals(3, actual.size());
    validate(actual.get(0), true, "pk1", fgOneKey1, "", null);
    validate(actual.get(1), false, "pk1", fgTwoKey1, "", "pk1");
    validate(actual.get(2), true, "pk2", fgTwoKey1, "", null);
  }

  @Test
  public void getServingKeys_join_left1key_right2key_diffName() throws Exception {
    doReturn(getFgFeature(fgOneKey1)).when(featuregroupController).getFeatures(eq(fgOneKey1), any(), any());
    doReturn(getFgFeature(fgTwoKey1)).when(featuregroupController).getFeatures(eq(fgTwoKey1), any(), any());

    FeatureView fv = new FeatureView();
    // set tdf
    List<TrainingDatasetFeature> tdfs = Lists.newArrayList();
    tdfs.addAll(tdf4);
    tdfs.addAll(tdf1);
    fv.setFeatures(tdfs);
    // set join
    join1.setConditions(Lists.newArrayList());
    join1.setPrefix("");
    join1.setIdx(0);
    TrainingDatasetJoinCondition condition = new TrainingDatasetJoinCondition();
    condition.setLeftFeature("pk1");
    condition.setRightFeature("pk2");
    join4.setConditions(Lists.newArrayList(condition));
    join4.setPrefix("");
    join4.setIdx(1);
    fv.setJoins(Lists.newArrayList(join1, join4, join4));

    List<ServingKey> actual = target.getServingKeys(null, null, fv);

    // validate
    Assert.assertEquals(5, actual.size());
    validate(actual.get(0), true, "pk1", fgOneKey1, "", null);
    validate(actual.get(1), false, "pk2", fgTwoKey1, "", "pk1");
    validate(actual.get(2), true, "pk1", fgTwoKey1, "0_", null);
    validate(actual.get(3), false, "pk2", fgTwoKey1, "", "pk1");
    validate(actual.get(4), true, "pk1", fgTwoKey1, "1_", null);
  }

  @Test
  public void getServingKeys_join_left1key_right2key_multipleTimes() throws Exception {
    doReturn(getFgFeature(fgOneKey1)).when(featuregroupController).getFeatures(eq(fgOneKey1), any(), any());
    doReturn(getFgFeature(fgTwoKey1)).when(featuregroupController).getFeatures(eq(fgTwoKey1), any(), any());

    FeatureView fv = new FeatureView();
    // set tdf
    List<TrainingDatasetFeature> tdfs = Lists.newArrayList();
    tdfs.addAll(tdf4);
    tdfs.addAll(tdf1);
    fv.setFeatures(tdfs);
    // set join
    join1.setConditions(Lists.newArrayList());
    join1.setPrefix("");
    join1.setIdx(0);
    TrainingDatasetJoinCondition condition1 = new TrainingDatasetJoinCondition();
    condition1.setLeftFeature("pk1");
    condition1.setRightFeature("pk2");
    join4.setConditions(Lists.newArrayList(condition1));
    join4.setPrefix("id1_");
    join4.setIdx(1);

    TrainingDatasetJoin join4Second = new TrainingDatasetJoin();
    join4Second.setFeatureGroup(fgTwoKey1);
    join4Second.setFeatures(tdf4);
    TrainingDatasetJoinCondition condition2 = new TrainingDatasetJoinCondition();
    condition2.setLeftFeature("pk1");
    condition2.setRightFeature("pk1");
    join4Second.setConditions(Lists.newArrayList(condition2));
    join4Second.setPrefix("id2_");
    join4Second.setIdx(2);
    fv.setJoins(Lists.newArrayList(join1, join4, join4Second));

    List<ServingKey> actual = target.getServingKeys(null, null, fv);

    // validate
    Assert.assertEquals(5, actual.size());
    validate(actual.get(0), true, "pk1", fgOneKey1, "", null);
    validate(actual.get(1), false, "pk2", fgTwoKey1, "id1_", "pk1");
    validate(actual.get(2), true, "pk1", fgTwoKey1, "id1_", null);
    validate(actual.get(3), false, "pk1", fgTwoKey1, "id2_", "pk1");
    validate(actual.get(4), true, "pk2", fgTwoKey1, "id2_", null);
  }

  @Test
  public void getServingKeys_joinOnRightColumn() throws Exception {
    doReturn(getFgFeature(fgOneKey1)).when(featuregroupController).getFeatures(eq(fgOneKey1), any(), any());
    doReturn(getFgFeature(fgOneKey2)).when(featuregroupController).getFeatures(eq(fgOneKey2), any(), any());

    FeatureView fv = new FeatureView();
    // set tdf
    List<TrainingDatasetFeature> tdfs = Lists.newArrayList();
    tdfs.addAll(tdf1);
    tdfs.addAll(tdf2);
    fv.setFeatures(tdfs);
    // set join
    join1.setConditions(Lists.newArrayList());
    join1.setPrefix("");
    join1.setIdx(0);
    TrainingDatasetJoinCondition condition = new TrainingDatasetJoinCondition();
    condition.setLeftFeature("pk1");
    condition.setRightFeature("feature1");
    join2.setConditions(Lists.newArrayList(condition));
    join2.setPrefix("fg2_");
    join2.setIdx(1);
    fv.setJoins(Lists.newArrayList(join1, join2));

    List<ServingKey> actual = target.getServingKeys(null, null, fv);

    // validate
    Assert.assertEquals(2, actual.size());
    validate(actual.get(0), true, "pk1", fgOneKey1, "", null);
    validate(actual.get(1), true, "pk1", fgOneKey2, "fg2_", null);
  }

  @Test
  public void getServingKeys_joinOnLeftColumn() throws Exception {
    doReturn(getFgFeature(fgOneKey1)).when(featuregroupController).getFeatures(eq(fgOneKey1), any(), any());
    doReturn(getFgFeature(fgOneKey2)).when(featuregroupController).getFeatures(eq(fgOneKey2), any(), any());

    FeatureView fv = new FeatureView();
    // set tdf
    List<TrainingDatasetFeature> tdfs = Lists.newArrayList();
    tdfs.addAll(tdf1);
    tdfs.addAll(tdf2);
    fv.setFeatures(tdfs);
    // set join
    join1.setConditions(Lists.newArrayList());
    join1.setPrefix(null);
    join1.setIdx(0);
    TrainingDatasetJoinCondition condition = new TrainingDatasetJoinCondition();
    condition.setLeftFeature("feature1");
    condition.setRightFeature("pk1");
    join2.setConditions(Lists.newArrayList(condition));
    join2.setPrefix(null);
    join2.setIdx(1);
    fv.setJoins(Lists.newArrayList(join1, join2));

    List<ServingKey> actual = target.getServingKeys(null, null, fv);

    // validate
    Assert.assertEquals(2, actual.size());
    validate(actual.get(0), true, "pk1", fgOneKey1, null, null);
    validate(actual.get(1), true, "pk1", fgOneKey2, "0_", "feature1");
  }

  @Test
  public void getServingKeys_joinColOnCol() throws Exception {
    doReturn(getFgFeature(fgOneKey1)).when(featuregroupController).getFeatures(eq(fgOneKey1), any(), any());
    doReturn(getFgFeature(fgOneKey2)).when(featuregroupController).getFeatures(eq(fgOneKey2), any(), any());

    FeatureView fv = new FeatureView();
    // set tdf
    List<TrainingDatasetFeature> tdfs = Lists.newArrayList();
    tdfs.addAll(tdf1);
    tdfs.addAll(tdf2);
    fv.setFeatures(tdfs);
    // set join
    join1.setConditions(Lists.newArrayList());
    join1.setPrefix(null);
    join1.setIdx(0);
    TrainingDatasetJoinCondition condition = new TrainingDatasetJoinCondition();
    condition.setLeftFeature("feature1");
    condition.setRightFeature("feature1");
    join2.setConditions(Lists.newArrayList(condition));
    join2.setPrefix(null);
    join2.setIdx(1);
    fv.setJoins(Lists.newArrayList(join1, join2));

    List<ServingKey> actual = target.getServingKeys(null, null, fv);

    // validate
    Assert.assertEquals(2, actual.size());
    validate(actual.get(0), true, "pk1", fgOneKey1, null, null);
    validate(actual.get(1), true, "pk1", fgOneKey2, "0_", null);
  }

  @Test
  public void getServingKeys_singleFg_noPrimaryKey() throws Exception {
    doReturn(getFgFeature(fgNoKey5)).when(featuregroupController).getFeatures(eq(fgNoKey5), any(), any());
    FeatureView fv = new FeatureView();
    List<TrainingDatasetFeature> tdfs = Lists.newArrayList();
    tdfs.addAll(tdf5);
    fv.setFeatures(tdfs);
    join5.setConditions(Lists.newArrayList());
    join5.setPrefix("");
    join5.setIdx(0);
    fv.setJoins(Lists.newArrayList(join5));

    List<ServingKey> actual = target.getServingKeys(null, null, fv);
    Assert.assertEquals(0, actual.size());
  }

  @Test
  public void getServingKeys_noPrimaryKey_joinOnLeftColumn() throws Exception {
    doReturn(getFgFeature(fgNoKey5)).when(featuregroupController).getFeatures(eq(fgNoKey5), any(), any());
    doReturn(getFgFeature(fgOneKey2)).when(featuregroupController).getFeatures(eq(fgOneKey2), any(), any());

    FeatureView fv = new FeatureView();
    // set tdf
    List<TrainingDatasetFeature> tdfs = Lists.newArrayList();
    tdfs.addAll(tdf5);
    tdfs.addAll(tdf2);
    fv.setFeatures(tdfs);
    // set join
    join5.setConditions(Lists.newArrayList());
    join5.setPrefix("");
    join5.setIdx(0);
    TrainingDatasetJoinCondition condition = new TrainingDatasetJoinCondition();
    condition.setLeftFeature("feature1");
    condition.setRightFeature("pk1");
    join2.setConditions(Lists.newArrayList(condition));
    join2.setPrefix("");
    join2.setIdx(1);
    fv.setJoins(Lists.newArrayList(join5, join2));

    List<ServingKey> actual = target.getServingKeys(null, null, fv);

    // validate
    Assert.assertEquals(1, actual.size());
    validate(actual.get(0), true, "pk1", fgOneKey2, "", "feature1");
  }

  @Test
  public void getServingKeys_joinRightLabelOnlyFg() throws Exception {
    doReturn(getFgFeature(fgOneKey1)).when(featuregroupController).getFeatures(eq(fgOneKey1), any(), any());
    doReturn(getFgFeature(fgLabel6)).when(featuregroupController).getFeatures(eq(fgLabel6), any(), any());

    FeatureView fv = new FeatureView();
    // set tdf
    List<TrainingDatasetFeature> tdfs = Lists.newArrayList();
    tdfs.addAll(tdf1);
    tdfs.addAll(tdf6);
    fv.setFeatures(tdfs);
    // set join
    join1.setConditions(Lists.newArrayList());
    join1.setPrefix("");
    join1.setIdx(0);
    TrainingDatasetJoinCondition condition = new TrainingDatasetJoinCondition();
    condition.setLeftFeature("pk1");
    condition.setRightFeature("pk1");
    join6.setConditions(Lists.newArrayList(condition));
    join6.setPrefix("fg2_");
    join6.setIdx(1);
    fv.setJoins(Lists.newArrayList(join1, join6));

    List<ServingKey> actual = target.getServingKeys(null, null, fv);

    // validate
    Assert.assertEquals(1, actual.size());
    validate(actual.get(0), true, "pk1", fgOneKey1, "", null);
  }

  @Test
  public void getServingKeys_joinLeftLabelOnlyFg() throws Exception {
    doReturn(getFgFeature(fgLabel6)).when(featuregroupController).getFeatures(eq(fgLabel6), any(), any());
    doReturn(getFgFeature(fgOneKey1)).when(featuregroupController).getFeatures(eq(fgOneKey1), any(), any());

    FeatureView fv = new FeatureView();
    // set tdf
    List<TrainingDatasetFeature> tdfs = Lists.newArrayList();
    tdfs.addAll(tdf1);
    tdfs.addAll(tdf6);
    fv.setFeatures(tdfs);
    // set join
    join6.setConditions(Lists.newArrayList());
    join6.setPrefix("");
    join6.setIdx(0);
    TrainingDatasetJoinCondition condition = new TrainingDatasetJoinCondition();
    condition.setLeftFeature("pk1");
    condition.setRightFeature("pk1");
    join1.setConditions(Lists.newArrayList(condition));
    join1.setPrefix("fg2_");
    join1.setIdx(1);
    fv.setJoins(Lists.newArrayList(join6, join1));

    List<ServingKey> actual = target.getServingKeys(null, null, fv);

    // validate
    Assert.assertEquals(2, actual.size());
    validate(actual.get(0), true, "pk1", fgLabel6, "", null);
    validate(actual.get(1), false, "pk1", fgOneKey1, "fg2_", "pk1");
  }

  private void validate(ServingKey servingKey, Boolean required, String featureName, Featuregroup fg, String prefix,
      String joinOn) {
    Assert.assertEquals(required, servingKey.getRequired());
    Assert.assertEquals(featureName, servingKey.getFeatureName());
    Assert.assertEquals(fg, servingKey.getFeatureGroup());
    Assert.assertEquals(prefix, servingKey.getPrefix());
    if (joinOn != null) {
      Assert.assertEquals(joinOn, servingKey.getJoinOn());
    } else {
      Assert.assertNull(servingKey.getJoinOn());
    }
  }

}