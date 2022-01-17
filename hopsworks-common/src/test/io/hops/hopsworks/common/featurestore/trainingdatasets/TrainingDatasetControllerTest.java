package io.hops.hopsworks.common.featurestore.trainingdatasets;

import com.google.common.collect.Maps;
import io.hops.hopsworks.common.featurestore.query.Feature;
import io.hops.hopsworks.common.featurestore.query.SqlCondition;
import io.hops.hopsworks.common.featurestore.query.filter.Filter;
import io.hops.hopsworks.common.featurestore.query.filter.FilterLogic;
import io.hops.hopsworks.common.featurestore.query.filter.FilterValue;
import io.hops.hopsworks.common.featurestore.query.filter.SqlFilterLogic;
import io.hops.hopsworks.persistence.entity.featurestore.featuregroup.Featuregroup;
import io.hops.hopsworks.persistence.entity.featurestore.trainingdataset.TrainingDataset;
import io.hops.hopsworks.persistence.entity.featurestore.trainingdataset.TrainingDatasetFilter;
import io.hops.hopsworks.persistence.entity.featurestore.trainingdataset.TrainingDatasetFilterCondition;
import junit.framework.TestCase;
import org.junit.Assert;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class TrainingDatasetControllerTest extends TestCase {

  TrainingDatasetController target = new TrainingDatasetController();

  @Test
  public void testconvertToFilterEntities_leftFilterRightLogic() throws Exception {
    // fg.feature > 1 and (fg.feature > 2 OR fg.feature > 3)
    // "fg.feature > 1" stores as filter
    TrainingDataset trainingDataset = new TrainingDataset();
    Feature f1 = new Feature("test_f", "fg0");
    FilterLogic head = new FilterLogic();
    head.setType(SqlFilterLogic.AND);
    Filter left = new Filter(f1, SqlCondition.GREATER_THAN, "1");
    head.setLeftFilter(left);
    FilterLogic right = new FilterLogic();
    right.setType(SqlFilterLogic.OR);
    Filter right_left = new Filter(f1, SqlCondition.GREATER_THAN, "2");
    Filter right_right = new Filter(f1, SqlCondition.GREATER_THAN, "3");
    right.setLeftFilter(right_left);
    right.setRightFilter(right_right);
    head.setRightLogic(right);

    List<TrainingDatasetFilter> actual = target.convertToFilterEntities(head, trainingDataset, "L");
    List<TrainingDatasetFilter> expected = new ArrayList<>();
    expected.add(createTrainingDatasetFilter(null, "AND", "L"));
    expected.add(createTrainingDatasetFilter(
        createTrainingDatasetFilterCondition("test_f", "GREATER_THAN", "1"),
        "SINGLE", "L.L"));
    expected.add(createTrainingDatasetFilter(null, "OR", "L.R"));
    expected.add(createTrainingDatasetFilter(
        createTrainingDatasetFilterCondition("test_f", "GREATER_THAN", "2"),
        "SINGLE", "L.R.L"));
    expected.add(createTrainingDatasetFilter(
        createTrainingDatasetFilterCondition("test_f", "GREATER_THAN", "3"),
        "SINGLE", "L.R.R"));

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
    head.setType(SqlFilterLogic.AND);
    FilterLogic left = new FilterLogic();
    Filter left_left = new Filter(f1, SqlCondition.GREATER_THAN, "1");
    left.setType(SqlFilterLogic.SINGLE);
    left.setLeftFilter(left_left);
    head.setLeftLogic(left);
    FilterLogic right = new FilterLogic();
    right.setType(SqlFilterLogic.OR);
    Filter right_left = new Filter(f1, SqlCondition.GREATER_THAN, "2");
    Filter right_right = new Filter(f1, SqlCondition.GREATER_THAN, "3");
    right.setLeftFilter(right_left);
    right.setRightFilter(right_right);
    head.setRightLogic(right);

    List<TrainingDatasetFilter> actual = target.convertToFilterEntities(head, trainingDataset, "L");
    List<TrainingDatasetFilter> expected = new ArrayList<>();
    expected.add(createTrainingDatasetFilter(null, "AND", "L"));
    expected.add(createTrainingDatasetFilter(
        createTrainingDatasetFilterCondition("test_f", "GREATER_THAN", "1"),
        "SINGLE", "L.L"));
    expected.add(createTrainingDatasetFilter(null, "OR", "L.R"));
    expected.add(createTrainingDatasetFilter(
        createTrainingDatasetFilterCondition("test_f", "GREATER_THAN", "2"),
        "SINGLE", "L.R.L"));
    expected.add(createTrainingDatasetFilter(
        createTrainingDatasetFilterCondition("test_f", "GREATER_THAN", "3"),
        "SINGLE", "L.R.R"));

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
    head.setType(SqlFilterLogic.AND);
    Filter left = new Filter(f1, SqlCondition.GREATER_THAN, "1");
    head.setLeftFilter(left);
    Filter right = new Filter(f1, SqlCondition.GREATER_THAN, "2");
    head.setRightFilter(right);

    List<TrainingDatasetFilter> actual = target.convertToFilterEntities(head, trainingDataset, "L");
    List<TrainingDatasetFilter> expected = new ArrayList<>();
    expected.add(createTrainingDatasetFilter(null, "AND", "L"));
    expected.add(createTrainingDatasetFilter(
        createTrainingDatasetFilterCondition("test_f", "GREATER_THAN", "2"),
        "SINGLE", "L.R"));
    expected.add(createTrainingDatasetFilter(
        createTrainingDatasetFilterCondition("test_f", "GREATER_THAN", "1"),
        "SINGLE", "L.L"));

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
    head.setType(SqlFilterLogic.AND);
    FilterLogic left = new FilterLogic();
    Filter left_left = new Filter(f1, SqlCondition.GREATER_THAN, "1");
    left.setLeftFilter(left_left);
    left.setType(SqlFilterLogic.SINGLE);
    head.setLeftLogic(left);
    Filter right = new Filter(f1, SqlCondition.GREATER_THAN, "2");
    head.setRightFilter(right);

    List<TrainingDatasetFilter> actual = target.convertToFilterEntities(head, trainingDataset, "L");
    List<TrainingDatasetFilter> expected = new ArrayList<>();
    expected.add(createTrainingDatasetFilter(null, "AND", "L"));
    expected.add(createTrainingDatasetFilter(
        createTrainingDatasetFilterCondition("test_f", "GREATER_THAN", "2"),
        "SINGLE", "L.R"));
    expected.add(createTrainingDatasetFilter(
        createTrainingDatasetFilterCondition("test_f", "GREATER_THAN", "1"),
        "SINGLE", "L.L"));

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
    head.setType(SqlFilterLogic.SINGLE);
    Filter right = new Filter(f1, SqlCondition.GREATER_THAN, "2");
    head.setRightFilter(right);

    List<TrainingDatasetFilter> actual = target.convertToFilterEntities(head, trainingDataset, "L");
    List<TrainingDatasetFilter> expected = new ArrayList<>();
    expected.add(createTrainingDatasetFilter(
        createTrainingDatasetFilterCondition("test_f", "GREATER_THAN", "2"),
        "SINGLE", "L"));

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
    head.setType(SqlFilterLogic.SINGLE);
    Filter right = new Filter(f1, SqlCondition.GREATER_THAN, "2");
    head.setLeftFilter(right);

    List<TrainingDatasetFilter> actual = target.convertToFilterEntities(head, trainingDataset, "L");
    List<TrainingDatasetFilter> expected = new ArrayList<>();
    expected.add(createTrainingDatasetFilter(
        createTrainingDatasetFilterCondition("test_f", "GREATER_THAN", "2"),
        "SINGLE", "L"));

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
    head.setType(SqlFilterLogic.AND);
    FilterValue filterValueLeft = new FilterValue(0,"fg0","test_f1");
    Filter left = new Filter(f1, SqlCondition.GREATER_THAN, filterValueLeft);
    head.setLeftFilter(left);
    FilterValue filterValueRight = new FilterValue(1,"fg1","test_f2");
    Filter right = new Filter(f1, SqlCondition.GREATER_THAN, filterValueRight);
    head.setRightFilter(right);

    List<TrainingDatasetFilter> actual = target.convertToFilterEntities(head, trainingDataset, "L");
    List<TrainingDatasetFilter> expected = new ArrayList<>();
    expected.add(createTrainingDatasetFilter(null, "AND", "L"));
    expected.add(createTrainingDatasetFilter(
        createTrainingDatasetFilterCondition("test_f", "GREATER_THAN", "test_f1", null, 0),
        "SINGLE", "L.L"));
    expected.add(createTrainingDatasetFilter(
        createTrainingDatasetFilterCondition("test_f", "GREATER_THAN", "test_f2", null, 1),
        "SINGLE", "L.R"));

    Assert.assertEquals(expected.size(), actual.size());
    Assert.assertTrue(expected.containsAll(actual));
    Assert.assertTrue(actual.containsAll(expected));
  }

  public TrainingDatasetFilter createTrainingDatasetFilter(
      TrainingDatasetFilterCondition condition, String type, String path) {
    TrainingDatasetFilter filter = new TrainingDatasetFilter();
    filter.setCondition(condition);
    filter.setType(type);
    filter.setPath(path);
    return filter;
  }

  public TrainingDatasetFilterCondition createTrainingDatasetFilterCondition(
      String feature, String condition, String value) {
    return createTrainingDatasetFilterCondition(feature, condition, value, null);
  }

  public TrainingDatasetFilterCondition createTrainingDatasetFilterCondition(
      String feature, String condition, String value, Integer fgId) {
    return  createTrainingDatasetFilterCondition(feature, condition, value, fgId, null);
  }

  public TrainingDatasetFilterCondition createTrainingDatasetFilterCondition(
      String feature, String condition, String value, Integer fgId, Integer filterValueFgId) {
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
    filters.add(createTrainingDatasetFilter(null, "AND", "L"));
    filters.add(createTrainingDatasetFilter(
        createTrainingDatasetFilterCondition("test_f", "GREATER_THAN", "2", 0),
        "SINGLE", "L.R"));
    filters.add(createTrainingDatasetFilter(
        createTrainingDatasetFilterCondition("test_f", "GREATER_THAN", "1", 0),
        "SINGLE", "L.L"));

    Map<String, Feature> featureLookup = Maps.newHashMap();
    Feature feature = new Feature("test_f", "fg0");
    Featuregroup featuregroup = new Featuregroup();
    featuregroup.setId(0);
    feature.setFeatureGroup(featuregroup);
    featureLookup.put("0.test_f", feature);

    FilterLogic actual = target.convertToFilterLogic(filters, featureLookup, "L");

    Feature f1 = new Feature("test_f", "fg0", null, false, null, null);
    f1.setFeatureGroup(featuregroup);
    Filter left = new Filter(f1, SqlCondition.GREATER_THAN, "1");
    Filter right = new Filter(f1, SqlCondition.GREATER_THAN, "2");

    Assert.assertEquals(actual.getType(), SqlFilterLogic.AND);
    Assert.assertEquals(actual.getLeftFilter(), left);
    Assert.assertNull(actual.getLeftLogic());
    Assert.assertEquals(actual.getRightFilter(), right);
    Assert.assertNull(actual.getRightLogic());
  }

  public void testConvertToFilterLogic_singleCondition() throws Exception {
    // fg.feature > 1
    List<TrainingDatasetFilter> filters = new ArrayList<>();
    filters.add(createTrainingDatasetFilter(
        createTrainingDatasetFilterCondition("test_f", "GREATER_THAN", "1", 0),
        "SINGLE", "L"));

    Map<String, Feature> featureLookup = Maps.newHashMap();
    Feature feature = new Feature("test_f", "fg0");
    Featuregroup featuregroup = new Featuregroup();
    featuregroup.setId(0);
    feature.setFeatureGroup(featuregroup);
    featureLookup.put("0.test_f", feature);

    FilterLogic actual = target.convertToFilterLogic(filters, featureLookup, "L");

    Feature f1 = new Feature("test_f", "fg0", null, false, null, null);
    f1.setFeatureGroup(featuregroup);
    Filter left = new Filter(f1, SqlCondition.GREATER_THAN, "1");

    Assert.assertEquals(actual.getType(), SqlFilterLogic.SINGLE);
    Assert.assertEquals(actual.getLeftFilter(), left);
    Assert.assertNull(actual.getLeftLogic());
    Assert.assertNull(actual.getRightFilter());
    Assert.assertNull(actual.getRightLogic());
  }

  public void testConvertToFilterLogic_featureValueCondition() throws Exception {
    // fg.feature > fg.otherFeature
    List<TrainingDatasetFilter> filters = new ArrayList<>();
    filters.add(createTrainingDatasetFilter(
        createTrainingDatasetFilterCondition("test_f", "GREATER_THAN", "test_f1", 0, 0),
        "SINGLE", "L"));

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

    Filter left = new Filter(feature, SqlCondition.GREATER_THAN, new FilterValue(0, "fg0", "test_f1"));

    Assert.assertEquals(actual.getType(), SqlFilterLogic.SINGLE);
    Assert.assertEquals(actual.getLeftFilter(), left);
    Assert.assertNull(actual.getLeftLogic());
    Assert.assertNull(actual.getRightFilter());
    Assert.assertNull(actual.getRightLogic());
  }

  public void testConvertToFilterLogic_multipleFeatureValueCondition() throws Exception {
    // fg.feature > fg.otherFeature and fg.feature > fg1.otherFeature
    List<TrainingDatasetFilter> filters = new ArrayList<>();
    filters.add(createTrainingDatasetFilter(null, "AND", "L"));
    filters.add(createTrainingDatasetFilter(
        createTrainingDatasetFilterCondition("test_f", "GREATER_THAN", "test_f1", 0,0),
        "SINGLE", "L.L"));
    filters.add(createTrainingDatasetFilter(
        createTrainingDatasetFilterCondition("test_f", "GREATER_THAN", "test_f2", 0, 1),
        "SINGLE", "L.R"));

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

    Filter left = new Filter(feature, SqlCondition.GREATER_THAN, new FilterValue(0, "fg0", "test_f1"));
    Filter right = new Filter(feature, SqlCondition.GREATER_THAN, new FilterValue(1, "fg1", "test_f2"));

    Assert.assertEquals(actual.getType(), SqlFilterLogic.AND);
    Assert.assertEquals(actual.getLeftFilter(), left);
    Assert.assertNull(actual.getLeftLogic());
    Assert.assertEquals(actual.getRightFilter(), right);
    Assert.assertNull(actual.getRightLogic());
  }

}