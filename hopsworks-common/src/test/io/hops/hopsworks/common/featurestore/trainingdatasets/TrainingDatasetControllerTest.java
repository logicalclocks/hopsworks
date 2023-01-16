/*
 * This file is part of Hopsworks
 * Copyright (C) 2022, Logical Clocks AB. All rights reserved
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
package io.hops.hopsworks.common.featurestore.trainingdatasets;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import io.hops.hopsworks.common.featurestore.feature.TrainingDatasetFeatureDTO;
import io.hops.hopsworks.common.featurestore.featuregroup.FeaturegroupDTO;
import io.hops.hopsworks.common.featurestore.query.Feature;
import io.hops.hopsworks.common.featurestore.query.Query;
import io.hops.hopsworks.common.featurestore.query.join.Join;
import io.hops.hopsworks.common.featurestore.transformationFunction.TransformationFunctionDTO;
import io.hops.hopsworks.persistence.entity.featurestore.featureview.FeatureView;
import io.hops.hopsworks.persistence.entity.featurestore.trainingdataset.SqlCondition;
import io.hops.hopsworks.common.featurestore.query.filter.Filter;
import io.hops.hopsworks.common.featurestore.query.filter.FilterLogic;
import io.hops.hopsworks.common.featurestore.query.filter.FilterValue;
import io.hops.hopsworks.persistence.entity.featurestore.trainingdataset.SqlFilterLogic;
import io.hops.hopsworks.persistence.entity.featurestore.featuregroup.Featuregroup;
import io.hops.hopsworks.persistence.entity.featurestore.trainingdataset.TrainingDataset;
import io.hops.hopsworks.persistence.entity.featurestore.trainingdataset.TrainingDatasetFeature;
import io.hops.hopsworks.persistence.entity.featurestore.trainingdataset.TrainingDatasetFilter;
import io.hops.hopsworks.persistence.entity.featurestore.trainingdataset.TrainingDatasetFilterCondition;
import io.hops.hopsworks.persistence.entity.featurestore.trainingdataset.TrainingDatasetJoin;
import io.hops.hopsworks.persistence.entity.featurestore.transformationFunction.TransformationFunction;
import io.hops.hopsworks.persistence.entity.project.Project;
import io.hops.hopsworks.persistence.entity.user.Users;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static io.hops.hopsworks.persistence.entity.featurestore.trainingdataset.SqlCondition.GREATER_THAN;
import static io.hops.hopsworks.persistence.entity.featurestore.trainingdataset.SqlFilterLogic.AND;
import static io.hops.hopsworks.persistence.entity.featurestore.trainingdataset.SqlFilterLogic.OR;
import static io.hops.hopsworks.persistence.entity.featurestore.trainingdataset.SqlFilterLogic.SINGLE;
import static org.mockito.Mockito.doReturn;

public class TrainingDatasetControllerTest {

  private TrainingDatasetController target;

  @Before
  public void before() throws Exception {
    target = Mockito.spy(new TrainingDatasetController());
    TransformationFunction tf1 = new TransformationFunction();
    tf1.setId(1);
    TransformationFunction tf2 = new TransformationFunction();
    tf2.setId(2);
    doReturn(tf1).when(target).getTransformationFunctionById(1);
    doReturn(tf2).when(target).getTransformationFunctionById(2);
  }

  @Test
  public void testconvertToFilterEntities_leftFilterRightLogic() throws Exception {
    // fg.feature > 1 and (fg.feature > 2 OR fg.feature > 3)
    // "fg.feature > 1" stores as filter
    TrainingDataset trainingDataset = new TrainingDataset();
    Feature f1 = new Feature("test_f", "fg0");
    FilterLogic head = new FilterLogic();
    head.setType(AND);
    Filter left = new Filter(f1, GREATER_THAN, "1");
    head.setLeftFilter(left);
    FilterLogic right = new FilterLogic();
    right.setType(OR);
    Filter right_left = new Filter(f1, GREATER_THAN, "2");
    Filter right_right = new Filter(f1, GREATER_THAN, "3");
    right.setLeftFilter(right_left);
    right.setRightFilter(right_right);
    head.setRightLogic(right);

    List<TrainingDatasetFilter> actual = target.convertToFilterEntities(head, trainingDataset, "L");
    List<TrainingDatasetFilter> expected = new ArrayList<>();
    expected.add(createTrainingDatasetFilter(null, AND, "L"));
    expected.add(createTrainingDatasetFilter(
        createTrainingDatasetFilterCondition("test_f", GREATER_THAN, "1"),
        SINGLE, "L.L"));
    expected.add(createTrainingDatasetFilter(null, OR, "L.R"));
    expected.add(createTrainingDatasetFilter(
        createTrainingDatasetFilterCondition("test_f", GREATER_THAN, "2"),
        SINGLE, "L.R.L"));
    expected.add(createTrainingDatasetFilter(
        createTrainingDatasetFilterCondition("test_f", GREATER_THAN, "3"),
        SINGLE, "L.R.R"));

    Assert.assertEquals(expected.size(), actual.size());
    Assert.assertTrue(expected.containsAll(actual));
    Assert.assertTrue(actual.containsAll(expected));
  }

  @Test
  public void testconvertToFilterEntities_bothLogic() throws Exception {
    // fg.feature > 1 and (fg.feature > 2 OR fg.feature > 3)
    // "fg.feature > 1" stores as filter logic
    TrainingDataset trainingDataset = new TrainingDataset();
    Feature f1 = new Feature("test_f", "fg0");
    FilterLogic head = new FilterLogic();
    head.setType(AND);
    FilterLogic left = new FilterLogic();
    Filter left_left = new Filter(f1, GREATER_THAN, "1");
    left.setType(SINGLE);
    left.setLeftFilter(left_left);
    head.setLeftLogic(left);
    FilterLogic right = new FilterLogic();
    right.setType(OR);
    Filter right_left = new Filter(f1, GREATER_THAN, "2");
    Filter right_right = new Filter(f1, GREATER_THAN, "3");
    right.setLeftFilter(right_left);
    right.setRightFilter(right_right);
    head.setRightLogic(right);

    List<TrainingDatasetFilter> actual = target.convertToFilterEntities(head, trainingDataset, "L");
    List<TrainingDatasetFilter> expected = new ArrayList<>();
    expected.add(createTrainingDatasetFilter(null, AND, "L"));
    expected.add(createTrainingDatasetFilter(
        createTrainingDatasetFilterCondition("test_f", GREATER_THAN, "1"),
        SINGLE, "L.L"));
    expected.add(createTrainingDatasetFilter(null, OR, "L.R"));
    expected.add(createTrainingDatasetFilter(
        createTrainingDatasetFilterCondition("test_f", GREATER_THAN, "2"),
        SINGLE, "L.R.L"));
    expected.add(createTrainingDatasetFilter(
        createTrainingDatasetFilterCondition("test_f", GREATER_THAN, "3"),
        SINGLE, "L.R.R"));

    Assert.assertEquals(expected.size(), actual.size());
    Assert.assertTrue(expected.containsAll(actual));
    Assert.assertTrue(actual.containsAll(expected));
  }

  @Test
  public void testconvertToFilterEntities_bothFilter() throws Exception {
    // fg.feature > 1 and fg.feature > 2
    // "fg.feature > 1" and "fg.feature > 2" stores as filter
    TrainingDataset trainingDataset = new TrainingDataset();
    Feature f1 = new Feature("test_f", "fg0");
    FilterLogic head = new FilterLogic();
    head.setType(AND);
    Filter left = new Filter(f1, GREATER_THAN, "1");
    head.setLeftFilter(left);
    Filter right = new Filter(f1, GREATER_THAN, "2");
    head.setRightFilter(right);

    List<TrainingDatasetFilter> actual = target.convertToFilterEntities(head, trainingDataset, "L");
    List<TrainingDatasetFilter> expected = new ArrayList<>();
    expected.add(createTrainingDatasetFilter(null, AND, "L"));
    expected.add(createTrainingDatasetFilter(
        createTrainingDatasetFilterCondition("test_f", GREATER_THAN, "2"),
        SINGLE, "L.R"));
    expected.add(createTrainingDatasetFilter(
        createTrainingDatasetFilterCondition("test_f", GREATER_THAN, "1"),
        SINGLE, "L.L"));

    Assert.assertEquals(expected.size(), actual.size());
    Assert.assertTrue(expected.containsAll(actual));
    Assert.assertTrue(actual.containsAll(expected));
  }

  @Test
  public void testconvertToFilterEntities_leftLogicRightFilter() throws Exception {
    // fg.feature > 1 and fg.feature > 2
    // "fg.feature > 1" stores as filter logic and "fg.feature > 2" stores as filter
    TrainingDataset trainingDataset = new TrainingDataset();
    Feature f1 = new Feature("test_f", "fg0");
    FilterLogic head = new FilterLogic();
    head.setType(AND);
    FilterLogic left = new FilterLogic();
    Filter left_left = new Filter(f1, GREATER_THAN, "1");
    left.setLeftFilter(left_left);
    left.setType(SINGLE);
    head.setLeftLogic(left);
    Filter right = new Filter(f1, GREATER_THAN, "2");
    head.setRightFilter(right);

    List<TrainingDatasetFilter> actual = target.convertToFilterEntities(head, trainingDataset, "L");
    List<TrainingDatasetFilter> expected = new ArrayList<>();
    expected.add(createTrainingDatasetFilter(null, AND, "L"));
    expected.add(createTrainingDatasetFilter(
        createTrainingDatasetFilterCondition("test_f", GREATER_THAN, "2"),
        SINGLE, "L.R"));
    expected.add(createTrainingDatasetFilter(
        createTrainingDatasetFilterCondition("test_f", GREATER_THAN, "1"),
        SINGLE, "L.L"));

    Assert.assertEquals(expected.size(), actual.size());
    Assert.assertTrue(expected.containsAll(actual));
    Assert.assertTrue(actual.containsAll(expected));
  }

  @Test
  public void testconvertToFilterEntities_rightOnlyFiler() throws Exception {
    // fg.feature > 2
    // "fg.feature > 2" stores in the right-hand side
    TrainingDataset trainingDataset = new TrainingDataset();
    Feature f1 = new Feature("test_f", "fg0");
    FilterLogic head = new FilterLogic();
    head.setType(SINGLE);
    Filter right = new Filter(f1, GREATER_THAN, "2");
    head.setRightFilter(right);

    List<TrainingDatasetFilter> actual = target.convertToFilterEntities(head, trainingDataset, "L");
    List<TrainingDatasetFilter> expected = new ArrayList<>();
    expected.add(createTrainingDatasetFilter(
        createTrainingDatasetFilterCondition("test_f", GREATER_THAN, "2"),
        SINGLE, "L"));

    Assert.assertEquals(expected.size(), actual.size());
    Assert.assertTrue(expected.containsAll(actual));
    Assert.assertTrue(actual.containsAll(expected));
  }

  @Test
  public void testconvertToFilterEntities_leftOnlyFilter() throws Exception {
    // fg.feature > 2
    // "fg.feature > 2" stores in the left-hand side
    TrainingDataset trainingDataset = new TrainingDataset();
    Feature f1 = new Feature("test_f", "fg0");
    FilterLogic head = new FilterLogic();
    head.setType(SINGLE);
    Filter right = new Filter(f1, GREATER_THAN, "2");
    head.setLeftFilter(right);

    List<TrainingDatasetFilter> actual = target.convertToFilterEntities(head, trainingDataset, "L");
    List<TrainingDatasetFilter> expected = new ArrayList<>();
    expected.add(createTrainingDatasetFilter(
        createTrainingDatasetFilterCondition("test_f", GREATER_THAN, "2"),
        SINGLE, "L"));

    Assert.assertEquals(expected.size(), actual.size());
    Assert.assertTrue(expected.containsAll(actual));
    Assert.assertTrue(actual.containsAll(expected));
  }

  @Test
  public void testconvertToFilterEntities_featureComparison() throws Exception {
    // fg.feature > fg.otherFeature and fg.feature > fg1.otherFeature
    TrainingDataset trainingDataset = new TrainingDataset();
    Feature f1 = new Feature("test_f", "fg0");
    FilterLogic head = new FilterLogic();
    head.setType(AND);
    FilterValue filterValueLeft = new FilterValue(0,"fg0","test_f1");
    Filter left = new Filter(f1, GREATER_THAN, filterValueLeft);
    head.setLeftFilter(left);
    FilterValue filterValueRight = new FilterValue(1,"fg1","test_f2");
    Filter right = new Filter(f1, GREATER_THAN, filterValueRight);
    head.setRightFilter(right);

    List<TrainingDatasetFilter> actual = target.convertToFilterEntities(head, trainingDataset, "L");
    List<TrainingDatasetFilter> expected = new ArrayList<>();
    expected.add(createTrainingDatasetFilter(null, AND, "L"));
    expected.add(createTrainingDatasetFilter(
        createTrainingDatasetFilterCondition("test_f", GREATER_THAN, "test_f1", null, 0),
        SINGLE, "L.L"));
    expected.add(createTrainingDatasetFilter(
        createTrainingDatasetFilterCondition("test_f", GREATER_THAN, "test_f2", null, 1),
        SINGLE, "L.R"));

    Assert.assertEquals(expected.size(), actual.size());
    Assert.assertTrue(expected.containsAll(actual));
    Assert.assertTrue(actual.containsAll(expected));
  }

  public TrainingDatasetFilter createTrainingDatasetFilter(
      TrainingDatasetFilterCondition condition, SqlFilterLogic type, String path) {
    TrainingDatasetFilter filter = new TrainingDatasetFilter();
    filter.setCondition(condition);
    filter.setType(type);
    filter.setPath(path);
    return filter;
  }

  public TrainingDatasetFilterCondition createTrainingDatasetFilterCondition(
      String feature, SqlCondition condition, String value) {
    return createTrainingDatasetFilterCondition(feature, condition, value, null);
  }

  public TrainingDatasetFilterCondition createTrainingDatasetFilterCondition(
      String feature, SqlCondition condition, String value, Integer fgId) {
    return  createTrainingDatasetFilterCondition(feature, condition, value, fgId, null);
  }

  public TrainingDatasetFilterCondition createTrainingDatasetFilterCondition(
      String feature, SqlCondition condition, String value, Integer fgId, Integer filterValueFgId) {
    TrainingDatasetFilterCondition filter = new TrainingDatasetFilterCondition();
    filter.setFeature(feature);
    filter.setCondition(condition);
    filter.setValue(value);
    Featuregroup fg = new Featuregroup();
    fg.setId(fgId);
    filter.setFeatureGroup(fgId == null ? null : fg);
    filter.setValueFeatureGroupId(filterValueFgId);
    return filter;
  }

  public void testConvertToFilterLogic_multipleConditions() throws Exception {
    // fg.feature > 1 and fg.feature > 2
    List<TrainingDatasetFilter> filters = new ArrayList<>();
    filters.add(createTrainingDatasetFilter(null, AND, "L"));
    filters.add(createTrainingDatasetFilter(
        createTrainingDatasetFilterCondition("test_f", GREATER_THAN, "2", 0),
        SINGLE, "L.R"));
    filters.add(createTrainingDatasetFilter(
        createTrainingDatasetFilterCondition("test_f", GREATER_THAN, "1", 0),
        SINGLE, "L.L"));

    Map<String, Feature> featureLookup = Maps.newHashMap();
    Feature feature = new Feature("test_f", "fg0");
    Featuregroup featuregroup = new Featuregroup();
    featuregroup.setId(0);
    feature.setFeatureGroup(featuregroup);
    featureLookup.put("0.test_f", feature);

    FilterLogic actual = target.convertToFilterLogic(filters, featureLookup, "L");

    Feature f1 = new Feature("test_f", "fg0", null, false, null, null);
    f1.setFeatureGroup(featuregroup);
    Filter left = new Filter(f1, GREATER_THAN, "1");
    Filter right = new Filter(f1, GREATER_THAN, "2");

    Assert.assertEquals(actual.getType(), AND);
    Assert.assertEquals(actual.getLeftFilter(), left);
    Assert.assertNull(actual.getLeftLogic());
    Assert.assertEquals(actual.getRightFilter(), right);
    Assert.assertNull(actual.getRightLogic());
  }

  public void testConvertToFilterLogic_singleCondition() throws Exception {
    // fg.feature > 1
    List<TrainingDatasetFilter> filters = new ArrayList<>();
    filters.add(createTrainingDatasetFilter(
        createTrainingDatasetFilterCondition("test_f", GREATER_THAN, "1", 0),
        SINGLE, "L"));

    Map<String, Feature> featureLookup = Maps.newHashMap();
    Feature feature = new Feature("test_f", "fg0");
    Featuregroup featuregroup = new Featuregroup();
    featuregroup.setId(0);
    feature.setFeatureGroup(featuregroup);
    featureLookup.put("0.test_f", feature);

    FilterLogic actual = target.convertToFilterLogic(filters, featureLookup, "L");

    Feature f1 = new Feature("test_f", "fg0", null, false, null, null);
    f1.setFeatureGroup(featuregroup);
    Filter left = new Filter(f1, GREATER_THAN, "1");

    Assert.assertEquals(actual.getType(), SINGLE);
    Assert.assertEquals(actual.getLeftFilter(), left);
    Assert.assertNull(actual.getLeftLogic());
    Assert.assertNull(actual.getRightFilter());
    Assert.assertNull(actual.getRightLogic());
  }

  public void testConvertToFilterLogic_featureValueCondition() throws Exception {
    // fg.feature > fg.otherFeature
    List<TrainingDatasetFilter> filters = new ArrayList<>();
    filters.add(createTrainingDatasetFilter(
        createTrainingDatasetFilterCondition("test_f", GREATER_THAN, "test_f1", 0, 0),
        SINGLE, "L"));

    Map<String, Feature> featureLookup = Maps.newHashMap();
    Feature feature = new Feature("test_f", "fg0");
    Feature feature1 = new Feature("test_f1", "fg0");

    Featuregroup featuregroup = new Featuregroup();
    featuregroup.setId(0);
    feature.setFeatureGroup(featuregroup);
    feature1.setFeatureGroup(featuregroup);

    featureLookup.put("0.test_f", feature);
    featureLookup.put("0.test_f1", feature1);

    FilterLogic actual = target.convertToFilterLogic(filters, featureLookup, "L");

    Filter left = new Filter(feature, GREATER_THAN, new FilterValue(0, "fg0", "test_f1"));

    Assert.assertEquals(actual.getType(), SINGLE);
    Assert.assertEquals(actual.getLeftFilter(), left);
    Assert.assertNull(actual.getLeftLogic());
    Assert.assertNull(actual.getRightFilter());
    Assert.assertNull(actual.getRightLogic());
  }

  public void testConvertToFilterLogic_multipleFeatureValueCondition() throws Exception {
    // fg.feature > fg.otherFeature and fg.feature > fg1.otherFeature
    List<TrainingDatasetFilter> filters = new ArrayList<>();
    filters.add(createTrainingDatasetFilter(null, AND, "L"));
    filters.add(createTrainingDatasetFilter(
        createTrainingDatasetFilterCondition("test_f", GREATER_THAN, "test_f1", 0,0),
        SINGLE, "L.L"));
    filters.add(createTrainingDatasetFilter(
        createTrainingDatasetFilterCondition("test_f", GREATER_THAN, "test_f2", 0, 1),
        SINGLE, "L.R"));

    Map<String, Feature> featureLookup = Maps.newHashMap();
    Feature feature = new Feature("test_f", "fg0");
    Feature feature1 = new Feature("test_f1", "fg0");
    Feature feature2 = new Feature("test_f2", "fg1");

    Featuregroup featuregroup0 = new Featuregroup();
    featuregroup0.setId(0);
    Featuregroup featuregroup1 = new Featuregroup();
    featuregroup1.setId(1);
    feature.setFeatureGroup(featuregroup0);
    feature1.setFeatureGroup(featuregroup0);
    feature2.setFeatureGroup(featuregroup1);

    featureLookup.put("0.test_f", feature);
    featureLookup.put("0.test_f1", feature1);
    featureLookup.put("1.test_f2", feature2);

    FilterLogic actual = target.convertToFilterLogic(filters, featureLookup, "L");

    Filter left = new Filter(feature, GREATER_THAN, new FilterValue(0, "fg0", "test_f1"));
    Filter right = new Filter(feature, GREATER_THAN, new FilterValue(1, "fg1", "test_f2"));

    Assert.assertEquals(actual.getType(), AND);
    Assert.assertEquals(actual.getLeftFilter(), left);
    Assert.assertNull(actual.getLeftLogic());
    Assert.assertEquals(actual.getRightFilter(), right);
    Assert.assertNull(actual.getRightLogic());
  }
  
  @Test
  public void testGetQuery_deletedFeatureGroup() throws Exception {
    List<TrainingDatasetFeature> tdFeatures = new ArrayList<>();
    tdFeatures.add(new TrainingDatasetFeature("feature_missing", null));
    tdFeatures.add(new TrainingDatasetFeature("feature_existing", new Featuregroup()));
    
    Query result = target.getQuery(new ArrayList<>(), tdFeatures, Collections.emptyList(), Mockito.mock(Project.class),
      Mockito.mock(Users.class), false);
  
    Assert.assertFalse(result.getDeletedFeatureGroups().isEmpty());
    Assert.assertEquals(1, result.getDeletedFeatureGroups().size());
    Assert.assertEquals("feature_missing", result.getDeletedFeatureGroups().get(0));
  }

  @Test
  public void testCollectFeatures() throws Exception {
    // prepare TransformationFunctionDTO
    TransformationFunctionDTO tfDto1 = new TransformationFunctionDTO();
    tfDto1.setId(1);
    TransformationFunctionDTO tfDto2 = new TransformationFunctionDTO();
    tfDto2.setId(2);
    // prepare TrainingDatasetJoin
    List<TrainingDatasetJoin> tdJoins = new ArrayList<>();
    tdJoins.add(new TrainingDatasetJoin(1));
    TrainingDatasetJoin tdJoin2 = new TrainingDatasetJoin(2);
    tdJoin2.setPrefix("fg1_");
    tdJoins.add(tdJoin2);
    // prepare TrainingDatasetFeatureDTO
    FeaturegroupDTO fgDto = new FeaturegroupDTO();
    fgDto.setId(1);
    List<TrainingDatasetFeatureDTO> tdFeatureDtos = new ArrayList<>();
    tdFeatureDtos.add(new TrainingDatasetFeatureDTO("f1", "double", fgDto, "f1", 0, false, tfDto1));
    tdFeatureDtos.add(new TrainingDatasetFeatureDTO("fg1_f1", "double", fgDto, "f1", 1, false, tfDto2));
    // prepare Query
    Query query = new Query();
    Feature feature1 = new Feature("f1", false);
    feature1.setFeatureGroup(new Featuregroup(1));
    query.setFeatures(Lists.newArrayList(feature1));
    query.setFeaturegroup(new Featuregroup(1));
    Query joinQuery = new Query();
    Feature feature2 = new Feature("f1", false);
    feature2.setFeatureGroup(new Featuregroup(1));
    feature2.setPrefix("fg1_");
    joinQuery.setFeatures(Lists.newArrayList(feature2));
    joinQuery.setFeaturegroup(new Featuregroup(1));
    query.setJoins(Lists.newArrayList(
        new Join(null, joinQuery, null, null, null, "fg1_", null))
    );

    // execute
    List<TrainingDatasetFeature> tdFeatures = target.collectFeatures(
        query,
        tdFeatureDtos,
        Mockito.mock(TrainingDataset.class),
        Mockito.mock(FeatureView.class),
        0,
        tdJoins,
        0
    );

    Assert.assertEquals(tdFeatures.size(), 2);
    TrainingDatasetFeature f1 = tdFeatures.get(0);
    Assert.assertEquals("f1", f1.getName());
    Assert.assertEquals(1, f1.getFeatureGroup().getId().intValue());
    Assert.assertNull(f1.getTrainingDatasetJoin().getPrefix());
    Assert.assertEquals(f1.getTransformationFunction().getId(), tfDto1.getId());
    TrainingDatasetFeature f2 = tdFeatures.get(1);
    Assert.assertEquals("f1", f2.getName());
    Assert.assertEquals(1, f2.getFeatureGroup().getId().intValue());
    Assert.assertEquals("fg1_", f2.getTrainingDatasetJoin().getPrefix());
    Assert.assertEquals(f2.getTransformationFunction().getId(), tfDto2.getId());

  }
}