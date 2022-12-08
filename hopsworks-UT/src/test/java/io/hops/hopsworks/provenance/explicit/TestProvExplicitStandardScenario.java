/*
 * Copyright (C) 2022, Hopsworks AB. All rights reserved
 */
package io.hops.hopsworks.provenance.explicit;

import io.hops.hopsworks.common.provenance.explicit.ProvExplicitLink;
import io.hops.hopsworks.exceptions.DatasetException;
import io.hops.hopsworks.exceptions.FeaturestoreException;
import io.hops.hopsworks.exceptions.GenericException;
import io.hops.hopsworks.persistence.entity.featurestore.featuregroup.Featuregroup;
import io.hops.hopsworks.persistence.entity.featurestore.featureview.FeatureView;
import io.hops.hopsworks.persistence.entity.featurestore.trainingdataset.TrainingDataset;
import io.hops.hopsworks.provenance.explicit.util.MockProvExplicitControllerEEImpl;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class TestProvExplicitStandardScenario {
  
  private MockProvExplicitControllerEEImpl.Scenario scenario;
  private ProvExplicitControllerEEImpl provExplicitCtrl;
  
  @Before
  public void setup() throws FeaturestoreException, DatasetException {
    scenario = MockProvExplicitControllerEEImpl.standardScenario();
    provExplicitCtrl = MockProvExplicitControllerEEImpl.baseSetup(scenario);
  }
  
  @Test
  public void testFG1Base() throws GenericException, FeaturestoreException, DatasetException {
    ProvExplicitLink<Featuregroup> result = provExplicitCtrl.featureGroupLinks(scenario.project, scenario.fg1);
    assertEquals(scenario.fg1, result.getNode());
    assertEquals(0, result.getUpstream().size());
    assertEquals(1, result.getDownstream().size());
    assertTrue(result.isTraversed());
    ProvExplicitLink<Featuregroup> downstream1Lvl1 = (ProvExplicitLink<Featuregroup>)result.getDownstream().get(0);
    assertEquals(scenario.fg3, downstream1Lvl1.getNode());
    assertEquals(0, downstream1Lvl1.getUpstream().size());
    assertEquals(0, downstream1Lvl1.getDownstream().size());
    assertFalse(downstream1Lvl1.isTraversed());
  }
  
  @Test
  public void testFG1Full() throws GenericException, FeaturestoreException, DatasetException {
    ProvExplicitLink<Featuregroup> result = provExplicitCtrl.featureGroupLinks(scenario.project, scenario.fg1, -1, -1);
    assertEquals(scenario.fg1, result.getNode());
    assertEquals(0, result.getUpstream().size());
    assertEquals(1, result.getDownstream().size());
    assertTrue(result.isTraversed());
    ProvExplicitLink<Featuregroup> downstream1Lvl1 = (ProvExplicitLink<Featuregroup>)result.getDownstream().get(0);
    assertEquals(scenario.fg3, downstream1Lvl1.getNode());
    assertEquals(0, downstream1Lvl1.getUpstream().size());
    assertEquals(1, downstream1Lvl1.getDownstream().size());
    assertTrue(downstream1Lvl1.isTraversed());
    ProvExplicitLink<FeatureView> downstream1Lvl2 = (ProvExplicitLink<FeatureView>)downstream1Lvl1.getDownstream().get(0);
    assertEquals(scenario.fv1, downstream1Lvl2.getNode());
    assertEquals(0, downstream1Lvl2.getUpstream().size());
    assertEquals(2, downstream1Lvl2.getDownstream().size());
    assertTrue(downstream1Lvl2.isTraversed());
    ProvExplicitLink<TrainingDataset> downstream1Lvl3 = (ProvExplicitLink<TrainingDataset>)downstream1Lvl2.getDownstream().get(0);
    assertEquals(scenario.td1, downstream1Lvl3.getNode());
    assertEquals(0, downstream1Lvl3.getUpstream().size());
    assertEquals(0, downstream1Lvl3.getDownstream().size());
    assertTrue(downstream1Lvl3.isTraversed());
    ProvExplicitLink<TrainingDataset> downstream2Lvl3 = (ProvExplicitLink<TrainingDataset>)downstream1Lvl2.getDownstream().get(1);
    assertEquals(scenario.td2, downstream2Lvl3.getNode());
    assertEquals(0, downstream2Lvl3.getUpstream().size());
    assertEquals(0, downstream2Lvl3.getDownstream().size());
    assertTrue(downstream2Lvl3.isTraversed());
  }
  
  @Test
  public void testFV1Base() throws GenericException, FeaturestoreException, DatasetException {
    ProvExplicitLink<FeatureView> result = provExplicitCtrl.featureViewLinks(scenario.project, scenario.fv1);
    assertEquals(scenario.fv1, result.getNode());
    assertEquals(2, result.getUpstream().size());
    assertEquals(2, result.getDownstream().size());
    assertTrue(result.isTraversed());
    ProvExplicitLink<Featuregroup> upstream1Lvl1 = (ProvExplicitLink<Featuregroup>)result.getUpstream().get(0);
    assertEquals(scenario.fg3, upstream1Lvl1.getNode());
    assertEquals(0, upstream1Lvl1.getUpstream().size());
    assertEquals(0, upstream1Lvl1.getDownstream().size());
    assertFalse(upstream1Lvl1.isTraversed());
    ProvExplicitLink<Featuregroup> upstream2Lvl1 = (ProvExplicitLink<Featuregroup>)result.getUpstream().get(1);
    assertEquals(scenario.fg6, upstream2Lvl1.getNode());
    assertEquals(0, upstream2Lvl1.getUpstream().size());
    assertEquals(0, upstream2Lvl1.getDownstream().size());
    assertFalse(upstream2Lvl1.isTraversed());
    
    ProvExplicitLink<TrainingDataset> downstream1Lvl1 = (ProvExplicitLink<TrainingDataset>)result.getDownstream().get(0);
    assertEquals(scenario.td1, downstream1Lvl1.getNode());
    assertEquals(0, downstream1Lvl1.getUpstream().size());
    assertEquals(0, downstream1Lvl1.getDownstream().size());
    assertTrue(downstream1Lvl1.isTraversed());
    ProvExplicitLink<TrainingDataset> downstream2Lvl1 = (ProvExplicitLink<TrainingDataset>)result.getDownstream().get(1);
    assertEquals(scenario.td2, downstream2Lvl1.getNode());
    assertEquals(0, downstream2Lvl1.getUpstream().size());
    assertEquals(0, downstream2Lvl1.getDownstream().size());
    assertTrue(downstream2Lvl1.isTraversed());
  }
  
  @Test
  public void testFV1Full() throws GenericException, FeaturestoreException, DatasetException {
    ProvExplicitLink<FeatureView> result = provExplicitCtrl.featureViewLinks(scenario.project, scenario.fv1, -1, -1);
    assertEquals(scenario.fv1, result.getNode());
    assertEquals(2, result.getUpstream().size());
    assertEquals(2, result.getDownstream().size());
    assertTrue(result.isTraversed());
    ProvExplicitLink<Featuregroup> upstream1Lvl1 = (ProvExplicitLink<Featuregroup>)result.getUpstream().get(0);
    assertEquals(scenario.fg3, upstream1Lvl1.getNode());
    assertEquals(2, upstream1Lvl1.getUpstream().size());
    assertEquals(0, upstream1Lvl1.getDownstream().size());
    assertTrue(upstream1Lvl1.isTraversed());
    ProvExplicitLink<Featuregroup> upstream2Lvl1 = (ProvExplicitLink<Featuregroup>)result.getUpstream().get(1);
    assertEquals(scenario.fg6, upstream2Lvl1.getNode());
    assertEquals(1, upstream2Lvl1.getUpstream().size());
    assertEquals(0, upstream2Lvl1.getDownstream().size());
    assertTrue(upstream2Lvl1.isTraversed());
    ProvExplicitLink<Featuregroup> upstream1Lvl2 = (ProvExplicitLink<Featuregroup>)upstream1Lvl1.getUpstream().get(0);
    assertEquals(scenario.fg1, upstream1Lvl2.getNode());
    assertEquals(0, upstream1Lvl2.getUpstream().size());
    assertEquals(0, upstream1Lvl2.getDownstream().size());
    assertTrue(upstream1Lvl2.isTraversed());
    ProvExplicitLink<Featuregroup> upstream2Lvl2 = (ProvExplicitLink<Featuregroup>)upstream1Lvl1.getUpstream().get(1);
    assertEquals(scenario.fg2, upstream2Lvl2.getNode());
    assertEquals(0, upstream1Lvl2.getUpstream().size());
    assertEquals(0, upstream1Lvl2.getDownstream().size());
    assertTrue(upstream2Lvl2.isTraversed());
    ProvExplicitLink<Featuregroup> upstream3Lvl2 = (ProvExplicitLink<Featuregroup>)upstream2Lvl1.getUpstream().get(0);
    assertEquals(scenario.fg5, upstream3Lvl2.getNode());
    assertEquals(1, upstream3Lvl2.getUpstream().size());
    assertEquals(0, upstream3Lvl2.getDownstream().size());
    assertTrue(upstream3Lvl2.isTraversed());
    ProvExplicitLink<Featuregroup> upstream1Lvl3 = (ProvExplicitLink<Featuregroup>)upstream3Lvl2.getUpstream().get(0);
    assertEquals(scenario.fg4, upstream1Lvl3.getNode());
    assertEquals(0, upstream1Lvl3.getUpstream().size());
    assertEquals(0, upstream1Lvl3.getDownstream().size());
    assertTrue(upstream1Lvl3.isTraversed());
    
    ProvExplicitLink<TrainingDataset> downstream1Lvl1 = (ProvExplicitLink<TrainingDataset>)result.getDownstream().get(0);
    assertEquals(scenario.td1, downstream1Lvl1.getNode());
    assertEquals(0, downstream1Lvl1.getUpstream().size());
    assertEquals(0, downstream1Lvl1.getDownstream().size());
    assertTrue(downstream1Lvl1.isTraversed());
    ProvExplicitLink<TrainingDataset> downstream2Lvl1 = (ProvExplicitLink<TrainingDataset>)result.getDownstream().get(1);
    assertEquals(scenario.td2, downstream2Lvl1.getNode());
    assertEquals(0, downstream2Lvl1.getUpstream().size());
    assertEquals(0, downstream2Lvl1.getDownstream().size());
    assertTrue(downstream2Lvl1.isTraversed());
  }
  
  @Test
  public void testTD1Base() throws GenericException, FeaturestoreException, DatasetException {
    ProvExplicitLink<TrainingDataset> result = provExplicitCtrl.trainingDatasetLinks(scenario.project, scenario.td1);
    assertEquals(scenario.td1, result.getNode());
    assertEquals(1, result.getUpstream().size());
    assertEquals(0, result.getDownstream().size());
    assertTrue(result.isTraversed());
    ProvExplicitLink<FeatureView> upstream1Lvl1 = (ProvExplicitLink<FeatureView>)result.getUpstream().get(0);
    assertEquals(scenario.fv1, upstream1Lvl1.getNode());
    assertEquals(0, upstream1Lvl1.getUpstream().size());
    assertEquals(0, upstream1Lvl1.getDownstream().size());
    assertFalse(upstream1Lvl1.isTraversed());
  }
  
  @Test
  public void testTd1Full() throws GenericException, FeaturestoreException, DatasetException {
    ProvExplicitLink<TrainingDataset> result = provExplicitCtrl.trainingDatasetLinks(scenario.project, scenario.td1, -1, -1);
    assertEquals(scenario.td1, result.getNode());
    assertEquals(1, result.getUpstream().size());
    assertEquals(0, result.getDownstream().size());
    assertTrue(result.isTraversed());
    ProvExplicitLink<FeatureView> upstream1Lvl1 = (ProvExplicitLink<FeatureView>)result.getUpstream().get(0);
    assertEquals(scenario.fv1, upstream1Lvl1.getNode());
    assertEquals(2, upstream1Lvl1.getUpstream().size());
    assertEquals(0, upstream1Lvl1.getDownstream().size());
    assertTrue(upstream1Lvl1.isTraversed());
    ProvExplicitLink<Featuregroup> upstream1Lvl2 = (ProvExplicitLink<Featuregroup>)upstream1Lvl1.getUpstream().get(0);
    assertEquals(scenario.fg3, upstream1Lvl2.getNode());
    assertEquals(2, upstream1Lvl2.getUpstream().size());
    assertEquals(0, upstream1Lvl2.getDownstream().size());
    assertTrue(upstream1Lvl2.isTraversed());
    ProvExplicitLink<Featuregroup> upstream2Lvl2 = (ProvExplicitLink<Featuregroup>)upstream1Lvl1.getUpstream().get(1);
    assertEquals(scenario.fg6, upstream2Lvl2.getNode());
    assertEquals(1, upstream2Lvl2.getUpstream().size());
    assertEquals(0, upstream2Lvl2.getDownstream().size());
    assertTrue(upstream2Lvl2.isTraversed());
    ProvExplicitLink<Featuregroup> upstream1Lvl3 = (ProvExplicitLink<Featuregroup>)upstream1Lvl2.getUpstream().get(0);
    assertEquals(scenario.fg1, upstream1Lvl3.getNode());
    assertEquals(0, upstream1Lvl3.getUpstream().size());
    assertEquals(0, upstream1Lvl3.getDownstream().size());
    assertTrue(upstream1Lvl3.isTraversed());
    ProvExplicitLink<Featuregroup> upstream2Lvl3 = (ProvExplicitLink<Featuregroup>)upstream1Lvl2.getUpstream().get(1);
    assertEquals(scenario.fg2, upstream2Lvl3.getNode());
    assertEquals(0, upstream2Lvl3.getUpstream().size());
    assertEquals(0, upstream2Lvl3.getDownstream().size());
    assertTrue(upstream2Lvl3.isTraversed());
    ProvExplicitLink<Featuregroup> upstream3Lvl3 = (ProvExplicitLink<Featuregroup>)upstream2Lvl2.getUpstream().get(0);
    assertEquals(scenario.fg5, upstream3Lvl3.getNode());
    assertEquals(1, upstream3Lvl3.getUpstream().size());
    assertEquals(0, upstream2Lvl3.getDownstream().size());
    assertTrue(upstream3Lvl3.isTraversed());
    ProvExplicitLink<Featuregroup> upstream1Lvl4 = (ProvExplicitLink<Featuregroup>)upstream3Lvl3.getUpstream().get(0);
    assertEquals(scenario.fg4, upstream1Lvl4.getNode());
    assertEquals(0, upstream1Lvl4.getUpstream().size());
    assertEquals(0, upstream1Lvl4.getDownstream().size());
    assertTrue(upstream1Lvl4.isTraversed());
  }
  
  @Test
  public void testTd1Custom1() throws GenericException, FeaturestoreException, DatasetException {
    ProvExplicitLink<TrainingDataset> result = provExplicitCtrl.trainingDatasetLinks(scenario.project, scenario.td1, 1, 1);
    assertEquals(scenario.td1, result.getNode());
    assertEquals(1, result.getUpstream().size());
    assertEquals(0, result.getDownstream().size());
    assertTrue(result.isTraversed());
    ProvExplicitLink<FeatureView> upstream1Lvl1 = (ProvExplicitLink<FeatureView>)result.getUpstream().get(0);
    assertEquals(scenario.fv1, upstream1Lvl1.getNode());
    assertEquals(0, upstream1Lvl1.getUpstream().size());
    assertEquals(0, upstream1Lvl1.getDownstream().size());
    assertFalse(upstream1Lvl1.isTraversed());
  }
  
  @Test
  public void testTd1Custom2() throws GenericException, FeaturestoreException, DatasetException {
    ProvExplicitLink<TrainingDataset> result = provExplicitCtrl.trainingDatasetLinks(scenario.project, scenario.td1, 2, -1);
    assertEquals(scenario.td1, result.getNode());
    assertEquals(1, result.getUpstream().size());
    assertEquals(0, result.getDownstream().size());
    assertTrue(result.isTraversed());
    ProvExplicitLink<FeatureView> upstream1Lvl1 = (ProvExplicitLink<FeatureView>)result.getUpstream().get(0);
    assertEquals(scenario.fv1, upstream1Lvl1.getNode());
    assertEquals(2, upstream1Lvl1.getUpstream().size());
    assertEquals(0, upstream1Lvl1.getDownstream().size());
    assertTrue(upstream1Lvl1.isTraversed());
    ProvExplicitLink<Featuregroup> upstream1Lvl2 = (ProvExplicitLink<Featuregroup>)upstream1Lvl1.getUpstream().get(0);
    assertEquals(scenario.fg3, upstream1Lvl2.getNode());
    assertEquals(0, upstream1Lvl2.getUpstream().size());
    assertEquals(0, upstream1Lvl2.getDownstream().size());
    assertFalse(upstream1Lvl2.isTraversed());
    ProvExplicitLink<Featuregroup> upstream2Lvl2 = (ProvExplicitLink<Featuregroup>)upstream1Lvl1.getUpstream().get(1);
    assertEquals(scenario.fg6, upstream2Lvl2.getNode());
    assertEquals(0, upstream2Lvl2.getUpstream().size());
    assertEquals(0, upstream2Lvl2.getDownstream().size());
    assertFalse(upstream2Lvl2.isTraversed());
  }
}
