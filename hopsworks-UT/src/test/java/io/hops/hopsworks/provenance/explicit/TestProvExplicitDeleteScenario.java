/*
 * Copyright (C) 2022, Hopsworks AB. All rights reserved
 */
package io.hops.hopsworks.provenance.explicit;

import com.google.common.collect.HashBasedTable;
import com.google.common.collect.Table;
import io.hops.hopsworks.common.dataset.util.DatasetHelper;
import io.hops.hopsworks.common.dataset.util.DatasetPath;
import io.hops.hopsworks.common.featurestore.FeaturestoreController;
import io.hops.hopsworks.common.featurestore.featureview.FeatureViewController;
import io.hops.hopsworks.common.featurestore.trainingdatasets.TrainingDatasetController;
import io.hops.hopsworks.common.featurestore.trainingdatasets.TrainingDatasetFacade;
import io.hops.hopsworks.common.provenance.explicit.FeatureGroupLinkFacade;
import io.hops.hopsworks.common.provenance.explicit.FeatureViewLinkFacade;
import io.hops.hopsworks.common.provenance.explicit.ProvArtifact;
import io.hops.hopsworks.common.provenance.explicit.ProvExplicitLink;
import io.hops.hopsworks.common.util.AccessController;
import io.hops.hopsworks.common.util.Settings;
import io.hops.hopsworks.exceptions.DatasetException;
import io.hops.hopsworks.exceptions.FeaturestoreException;
import io.hops.hopsworks.exceptions.GenericException;
import io.hops.hopsworks.persistence.entity.featurestore.Featurestore;
import io.hops.hopsworks.persistence.entity.featurestore.featuregroup.Featuregroup;
import io.hops.hopsworks.persistence.entity.featurestore.featuregroup.FeaturegroupType;
import io.hops.hopsworks.persistence.entity.featurestore.featureview.FeatureView;
import io.hops.hopsworks.persistence.entity.featurestore.trainingdataset.TrainingDataset;
import io.hops.hopsworks.persistence.entity.project.Project;
import io.hops.hopsworks.persistence.entity.provenance.FeatureGroupLink;
import io.hops.hopsworks.persistence.entity.provenance.FeatureViewLink;
import org.apache.hadoop.fs.Path;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.mockito.stubbing.Answer;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class TestProvExplicitDeleteScenario {
  private Settings settings;
  private ProvExplicitControllerEEImpl provExplicitCtrl;
  private FeatureGroupLinkFacade fgLinkFacade;
  private FeatureViewLinkFacade fvLinkFacade;
  private TrainingDatasetFacade tdFacade;
  private FeaturestoreController featureStoreCtrl;
  private FeatureViewController featureViewCtrl;
  private TrainingDatasetController trainingDatasetCtrl;
  private DatasetHelper datasetHelper;
  private AccessController accessCtrl;
  private Set<Integer> deleted = new HashSet<>();
  
  //<parent, child, value>
  Table<Integer, Integer, FeatureGroupLink> fgLinkTable = HashBasedTable.create();
  Table<Integer, Integer, FeatureViewLink> fvLinkTable = HashBasedTable.create();
  Table<Integer, Integer, TrainingDataset> tdLinkTable = HashBasedTable.create();
  
  Project project;
  Featuregroup fg1;
  Featuregroup fg2;
  FeatureView fv1;
  
  private void setupFeatureGroupLinkTable() {
    Mockito.when(fgLinkFacade.findByChild(Mockito.any())).thenAnswer(
      (Answer<Collection<FeatureGroupLink>>) invocation -> {
        Featuregroup fg = invocation.getArgument(0);
        return fgLinkTable.column(fg.getId()).values();
      });
    Mockito.when(fgLinkFacade.findByChildren(Mockito.any())).thenAnswer(
      (Answer<Collection<FeatureGroupLink>>) invocation -> {
        List<Featuregroup> fgs = invocation.getArgument(0);
        List<FeatureGroupLink> result = new ArrayList<>();
        for(Featuregroup fg : fgs) {
          result.addAll(fgLinkTable.column(fg.getId()).values());
        }
        return result;
      });
    Mockito.when(fgLinkFacade.findByParent(Mockito.any())).thenAnswer(
      (Answer<Collection<FeatureGroupLink>>) invocation -> {
        Featuregroup fg = invocation.getArgument(0);
        return fgLinkTable.row(fg.getId()).values();
      });
    Mockito.when(fgLinkFacade.findByParents(Mockito.any())).thenAnswer(
      (Answer<Collection<FeatureGroupLink>>) invocation -> {
        List<Featuregroup> fgs = invocation.getArgument(0);
        List<FeatureGroupLink> result = new ArrayList<>();
        for(Featuregroup fg : fgs) {
          result.addAll(fgLinkTable.row(fg.getId()).values());
        }
        return result;
      });
  }
  private void setupFeatureViewLinkTable() {
    Mockito.when(fvLinkFacade.findByChild(Mockito.any())).thenAnswer(
      (Answer<Collection<FeatureViewLink>>) invocation -> {
        FeatureView fv = invocation.getArgument(0);
        return fvLinkTable.column(fv.getId()).values();
      });
    Mockito.when(fvLinkFacade.findByChildren(Mockito.any())).thenAnswer(
      (Answer<Collection<FeatureViewLink>>) invocation -> {
        List<FeatureView> fvs = invocation.getArgument(0);
        List<FeatureViewLink> result = new ArrayList<>();
        for(FeatureView fv : fvs) {
          result.addAll(fvLinkTable.column(fv.getId()).values());
        }
        return result;
      });
    Mockito.when(fvLinkFacade.findByParent(Mockito.any())).thenAnswer(
      (Answer<Collection<FeatureViewLink>>) invocation -> {
        Featuregroup fg = invocation.getArgument(0);
        return fvLinkTable.row(fg.getId()).values();
      });
    Mockito.when(fvLinkFacade.findByParents(Mockito.any())).thenAnswer(
      (Answer<Collection<FeatureViewLink>>) invocation -> {
        List<Featuregroup> fgs = invocation.getArgument(0);
        List<FeatureViewLink> result = new ArrayList<>();
        for(Featuregroup fg : fgs) {
          result.addAll(fvLinkTable.row(fg.getId()).values());
        }
        return result;
      });
  }
  private void setupTrainingDatasetLinkTable() {
    Mockito.when(tdFacade.findByIds(Mockito.any())).thenAnswer(
      (Answer<Collection<TrainingDataset>>) invocation -> {
        List<Integer> tds = invocation.getArgument(0);
        List<TrainingDataset> result = new ArrayList<>();
        for(Integer td : tds) {
          result.addAll(tdLinkTable.column(td).values());
        }
        return result;
      });
    Mockito.when(tdFacade.findByFeatureViews(Mockito.any())).thenAnswer(
      (Answer<Collection<TrainingDataset>>) invocation -> {
        List<FeatureView> fvs = invocation.getArgument(0);
        List<TrainingDataset> result = new ArrayList<>();
        for(FeatureView fv : fvs) {
          result.addAll(tdLinkTable.row(fv.getId()).values());
        }
        return result;
      });
  }
  @Before
  public void setup() throws FeaturestoreException, DatasetException {
    fgLinkFacade = Mockito.mock(FeatureGroupLinkFacade.class);
    setupFeatureGroupLinkTable();
    fvLinkFacade = Mockito.mock(FeatureViewLinkFacade.class);
    setupFeatureViewLinkTable();
    tdFacade = Mockito.mock(TrainingDatasetFacade.class);
    setupTrainingDatasetLinkTable();
    settings = Mockito.mock(Settings.class);
    featureStoreCtrl = Mockito.mock(FeaturestoreController.class);
    featureViewCtrl = Mockito.mock(FeatureViewController.class);
    trainingDatasetCtrl = Mockito.mock(TrainingDatasetController.class);
    datasetHelper = Mockito.mock(DatasetHelper.class);
    accessCtrl = Mockito.mock(AccessController.class);
    //always has access in this scenario
    Mockito.when(accessCtrl.hasAccess(Mockito.any(), Mockito.any())).thenReturn(true);
    //don't care about location as we always grant permission to access
    Mockito.when(featureStoreCtrl.getProjectFeaturestoreDataset(Mockito.any())).thenReturn(null);
    Mockito.when(datasetHelper.getDatasetPath(Mockito.any(), Mockito.any(), Mockito.any()))
      .thenReturn(new DatasetPath((Path)null, (Path)null, (Path)null));
    Mockito.when(featureViewCtrl.getLocation(Mockito.any())).thenReturn(null);
    //
    provExplicitCtrl = new ProvExplicitControllerEEImpl(settings, fgLinkFacade, fvLinkFacade, tdFacade,
      featureStoreCtrl, featureViewCtrl, trainingDatasetCtrl, datasetHelper, accessCtrl);
    setupScenario();
    
    Mockito.when(settings.getProvenanceGraphMaxSize()).thenReturn(10000);
  }
  
  private void setupScenario() {
    project = new Project();
    project.setName("project1");
    project.setId(1);
    
    Featurestore fs = new Featurestore();
    fs.setProject(project);
    
    fg1 = new Featuregroup(1);
    fg1.setFeaturestore(fs);
    fg1.setFeaturegroupType(FeaturegroupType.CACHED_FEATURE_GROUP);
    fg1.setName("fg1");
    fg1.setVersion(1);
    deleted.add(fg1.getId());
    
    fg2 = new Featuregroup(2);
    fg2.setFeaturestore(fs);
    fg2.setFeaturegroupType(FeaturegroupType.CACHED_FEATURE_GROUP);
  
    fv1 = new FeatureView();
    fv1.setId(101);
    fv1.setFeaturestore(fs);
    
    FeatureGroupLink fgLink1 = new FeatureGroupLink();
    fgLink1.setParentFeatureGroup(null);
    fgLink1.setParentFeatureStore(fg1.getFeaturestore().getProject().getName());
    fgLink1.setParentFeatureGroupName(fg1.getName());
    fgLink1.setParentFeatureGroupVersion(fg1.getVersion());
    fgLink1.setFeatureGroup(fg2);
    fgLinkTable.put(fg1.getId(), fg2.getId(), fgLink1);
  
    FeatureViewLink fvLink1 = new FeatureViewLink();
    fvLink1.setParentFeatureGroup(null);
    fvLink1.setParentFeatureStore(fg1.getFeaturestore().getProject().getName());
    fvLink1.setParentFeatureGroupName(fg1.getName());
    fvLink1.setParentFeatureGroupVersion(fg1.getVersion());
    fvLink1.setFeatureView(fv1);
    fvLinkTable.put(fg1.getId(), fv1.getId(), fvLink1);
  }
  
  @Test
  public void testFgToFgDeleteParent() throws GenericException, FeaturestoreException, DatasetException {
    ProvExplicitLink<Featuregroup> result = provExplicitCtrl.featureGroupLinks(project, fg2);
    assertEquals(fg2, result.getNode());
    assertEquals(1, result.getUpstream().size());
    assertEquals(0, result.getDownstream().size());
    assertTrue(result.isTraversed());
    ProvExplicitLink<ProvArtifact> upstream1Lvl1 = (ProvExplicitLink<ProvArtifact>)result.getUpstream().get(0);
    assertEquals(true, upstream1Lvl1.isDeleted());
    assertEquals(fg1.getFeaturestore().getProject().getName(), upstream1Lvl1.getNode().getProject());
    assertEquals(fg1.getName(), upstream1Lvl1.getNode().getName());
    assertEquals(fg1.getVersion(), upstream1Lvl1.getNode().getVersion());
    assertFalse(upstream1Lvl1.isTraversed());
  }
  @Test
  public void testFgToFvDeleteParent() throws GenericException, FeaturestoreException, DatasetException {
    ProvExplicitLink<FeatureView> result = provExplicitCtrl.featureViewLinks(project, fv1);
    assertEquals(fv1, result.getNode());
    assertEquals(1, result.getUpstream().size());
    assertEquals(0, result.getDownstream().size());
    assertTrue(result.isTraversed());
    ProvExplicitLink<ProvArtifact> upstream1Lvl1 = (ProvExplicitLink<ProvArtifact>)result.getUpstream().get(0);
    assertEquals(true, upstream1Lvl1.isDeleted());
    assertEquals(fg1.getFeaturestore().getProject().getName(), upstream1Lvl1.getNode().getProject());
    assertEquals(fg1.getName(), upstream1Lvl1.getNode().getName());
    assertEquals(fg1.getVersion(), upstream1Lvl1.getNode().getVersion());
    assertFalse(upstream1Lvl1.isTraversed());
  }
}
