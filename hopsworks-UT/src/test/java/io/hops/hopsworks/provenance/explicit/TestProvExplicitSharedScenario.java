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
import io.hops.hopsworks.persistence.entity.dataset.Dataset;
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

public class TestProvExplicitSharedScenario {
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
  
  Project project[] = new Project[4];
  Featuregroup fg1_1, fg1_2, fg1_3;
  Featuregroup fg2_1;
  Featuregroup fg3_1, fg3_2, fg3_3;
  FeatureView fv1_1, fv1_2, fv1_3;
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
    Mockito.when(featureStoreCtrl.getProjectFeaturestoreDataset(Mockito.any())).thenAnswer(
      (Answer<Dataset>) invocation -> {
        Project p = invocation.getArgument(0);
        Dataset d = new Dataset();
        d.setProject(p);
        return d;
      });
    featureViewCtrl = Mockito.mock(FeatureViewController.class);
    trainingDatasetCtrl = Mockito.mock(TrainingDatasetController.class);
    datasetHelper = Mockito.mock(DatasetHelper.class);
    Mockito.when(datasetHelper.getDatasetPath(Mockito.any(), Mockito.any(), Mockito.any()))
      .thenAnswer(
        (Answer<DatasetPath>) invocation -> {
          String fakePath =  invocation.getArgument(1);
          DatasetPath dp = new DatasetPath((Path)null, (Path)null, (Path)null);
          Dataset d = new Dataset();
          d.setProject(project[Integer.valueOf(fakePath)]);
          dp.setDataset(d);
          return dp;
        });
    accessCtrl = Mockito.mock(AccessController.class);
    //always has access in this scenario
    Mockito.when(accessCtrl.hasAccess(Mockito.any(), Mockito.any())).thenAnswer(
      (Answer<Boolean>) invocation -> {
        Project p = invocation.getArgument(0);
        Dataset d = invocation.getArgument(1);
        if (p.getId().equals(1) && (d.getProject().getId().equals(1) || d.getProject().getId().equals(2))) {
          return true;
        }
        return false;
      });
    Mockito.when(featureViewCtrl.getLocation(Mockito.any())).thenAnswer(
      (Answer<String>) invocation -> {
        //we use this in DatasetPath return, so we use this string to pass project id
        FeatureView d = invocation.getArgument(0);
        return String.valueOf(d.getFeaturestore().getProject().getId());
      });
    Mockito.when(trainingDatasetCtrl.getTrainingDatasetInodePath(Mockito.any())).thenReturn(null);
    //
    provExplicitCtrl = new ProvExplicitControllerEEImpl(settings, fgLinkFacade, fvLinkFacade, tdFacade,
      featureStoreCtrl, featureViewCtrl, trainingDatasetCtrl, datasetHelper, accessCtrl);
    setupScenario();
    
    Mockito.when(settings.getProvenanceGraphMaxSize()).thenReturn(10000);
  }
  
  private void setupScenario() {
    project[1] = new Project();
    project[1].setName("project1");
    project[1].setId(1);
    
    project[2] = new Project();
    project[2].setName("project2");
    project[2].setId(2);
    
    project[3] = new Project();
    project[3].setName("project3");
    project[3].setId(3);
    
    Featurestore fs1 = new Featurestore();
    fs1.setProject(project[1]);
    Featurestore fs2 = new Featurestore();
    fs2.setProject(project[2]);
    Featurestore fs3 = new Featurestore();
    fs3.setProject(project[3]);
    
    fg1_1 = createFG(11, fs1, "fg1_1", 1);
    fg1_2 = createFG(12, fs2, "fg1_2", 1);
    fg1_3 = createFG(13, fs3, "fg1_3", 1);
    
    fg2_1 = createFG(21, fs1, "fg2_1", 1);
    
    fg3_1 = createFG(31, fs1, "fg3_1", 1);
    fg3_2 = createFG(32, fs2, "fg3_2", 1);
    fg3_3 = createFG(33, fs3, "fg3_3", 1);
    
    fv1_1 = createFV(101, fs1, "fv1_1");
    fv1_2 = createFV(102, fs2, "fv1_2");
    fv1_3 = createFV(103, fs3, "fv1_3");
  
    setFGtoFGLink(fg1_1, fg2_1);
    setFGtoFGLink(fg1_2, fg2_1);
    setFGtoFGLink(fg1_3, fg2_1);
  
    setFGtoFGLink(fg2_1, fg3_1);
    setFGtoFGLink(fg2_1, fg3_2);
    setFGtoFGLink(fg2_1, fg3_3);
  
    setFVtoFGLink(fg2_1, fv1_1);
    setFVtoFGLink(fg2_1, fv1_2);
    setFVtoFGLink(fg2_1, fv1_3);
  
    setFVtoFGLink(fg1_2, fv1_1);
    setFVtoFGLink(fg1_3, fv1_1);
  }
  
  private Featuregroup createFG(Integer id, Featurestore fs, String name, Integer version) {
    Featuregroup fg = new Featuregroup(id);
    fg.setFeaturestore(fs);
    fg.setFeaturegroupType(FeaturegroupType.CACHED_FEATURE_GROUP);
    fg.setName(name);
    fg.setVersion(version);
    return fg;
  }
  
  private FeatureView createFV(Integer id, Featurestore fs, String name) {
    FeatureView fv = new FeatureView();
    fv.setId(id);
    fv.setFeaturestore(fs);
    fv.setName(name);
    return fv;
  }
  
  private void setFGtoFGLink(Featuregroup parent, Featuregroup child) {
    FeatureGroupLink fgLink1 = new FeatureGroupLink();
    fgLink1.setParentFeatureGroup(parent);
    fgLink1.setParentFeatureStore(parent.getFeaturestore().getProject().getName());
    fgLink1.setParentFeatureGroupName(parent.getName());
    fgLink1.setParentFeatureGroupVersion(parent.getVersion());
    fgLink1.setFeatureGroup(child);
    fgLinkTable.put(parent.getId(), child.getId(), fgLink1);
  }
  
  private void setFVtoFGLink(Featuregroup parent, FeatureView child) {
    FeatureViewLink fvLink1 = new FeatureViewLink();
    fvLink1.setParentFeatureGroup(parent);
    fvLink1.setParentFeatureStore(parent.getFeaturestore().getProject().getName());
    fvLink1.setParentFeatureGroupName(parent.getName());
    fvLink1.setParentFeatureGroupVersion(parent.getVersion());
    fvLink1.setFeatureView(child);
    fvLinkTable.put(parent.getId(), child.getId(), fvLink1);
  }
  
  private void checkFG(Featuregroup expected, ProvExplicitLink actual,
                       boolean shared, boolean accessible, boolean traversed) {
    assertEquals(shared, actual.isShared());
    assertEquals(accessible, actual.isAccessible());
    assertEquals(false, actual.isDeleted());
    assertEquals(traversed, actual.isTraversed());
    
    if(accessible) {
      Featuregroup actualFG = (Featuregroup)actual.getNode();
      assertEquals(expected.getFeaturestore().getProject().getName(),
        actualFG.getFeaturestore().getProject().getName());
      assertEquals(expected.getName(), actualFG.getName());
      assertEquals(expected.getVersion(), actualFG.getVersion());
    } else {
      ProvArtifact actualA = (ProvArtifact)actual.getNode();
      assertEquals(expected.getFeaturestore().getProject().getName(), actualA.getProject());
      assertEquals(expected.getName(), actualA.getName());
      assertEquals(expected.getVersion(), actualA.getVersion());
    }
    
  }
  
  private void checkFV(FeatureView expected, ProvExplicitLink actual,
                       boolean shared, boolean accessible, boolean traversed) {
    assertEquals(shared, actual.isShared());
    assertEquals(accessible, actual.isAccessible());
    assertEquals(false, actual.isDeleted());
    assertEquals(traversed, actual.isTraversed());
    
    if(accessible) {
      FeatureView actualFV = (FeatureView)actual.getNode();
      assertEquals(expected.getFeaturestore().getProject().getName(),
        actualFV.getFeaturestore().getProject().getName());
      assertEquals(expected.getName(), actualFV.getName());
      assertEquals(expected.getVersion(), actualFV.getVersion());
    } else {
      ProvArtifact actualA = (ProvArtifact)actual.getNode();
      assertEquals(expected.getFeaturestore().getProject().getName(), actualA.getProject());
      assertEquals(expected.getName(), actualA.getName());
      assertEquals(expected.getVersion(), actualA.getVersion());
    }
  }
  
  @Test
  public void testSharedUnsharedFG() throws GenericException, FeaturestoreException, DatasetException {
    ProvExplicitLink<Featuregroup> result = provExplicitCtrl.featureGroupLinks(project[1], fg2_1, -1, -1);
    assertEquals(fg2_1, result.getNode());
    assertEquals(3, result.getUpstream().size());
    assertEquals(6, result.getDownstream().size());
    assertTrue(result.isTraversed());
    
    //HashBased table order
    for(ProvExplicitLink link : result.getUpstream()) {
      if(link.getNode() instanceof Featuregroup) {
        Featuregroup fg = (Featuregroup)link.getNode();
        if(fg.getName().equals(fg1_1.getName())) {
          checkFG(fg1_1, link, false, true, true);
        } else if(fg.getName().equals(fg1_2.getName())) {
          checkFG(fg1_2, link, true, true, true);
        } else {
          assertFalse("should never get here", true);
        }
      } else if(link.getNode() instanceof ProvArtifact) {
        ProvArtifact a = (ProvArtifact)link.getNode();
        if(a.getName().equals(fg1_3.getName())) {
          checkFG(fg1_3, link, true, false, false);
        } else {
          assertFalse("should never get here", true);
        }
      } else {
        assertFalse("should never get here", true);
      }
    }
    for(ProvExplicitLink link : result.getDownstream()) {
      if(link.getNode() instanceof Featuregroup) {
        Featuregroup fg = (Featuregroup)link.getNode();
        if(fg.getName().equals(fg3_1.getName())) {
          checkFG(fg3_1, link, false, true, true);
        } else if(fg.getName().equals(fg3_2.getName())) {
          checkFG(fg3_2, link, true, true, true);
        } else {
          assertFalse("should never get here", true);
        }
      } else if(link.getNode() instanceof  FeatureView) {
        FeatureView fv = (FeatureView)link.getNode();
        if(fv.getName().equals(fv1_1.getName())) {
          checkFV(fv1_1, link,false, true, true);
        } else if(fv.getName().equals(fv1_2.getName())) {
          checkFV(fv1_2, link,true, true, true);
        } else {
          assertFalse("should never get here", true);
        }
      } else if(link.getNode() instanceof ProvArtifact) {
        ProvArtifact a = (ProvArtifact)link.getNode();
        if(a.getName().equals(fg3_3.getName())) {
          checkFG(fg3_3, link, true, false, false);
        } else if(a.getName().equals(fv1_3.getName())) {
          checkFV(fv1_3, link,true, false, false);
        } else {
          assertFalse("should never get here", true);
        }
      } else {
        assertFalse("should never get here", true);
      }
    }
  }
  
  @Test
  public void testSharedUnsharedFV() throws GenericException, FeaturestoreException, DatasetException {
    ProvExplicitLink<FeatureView> result = provExplicitCtrl.featureViewLinks(project[1], fv1_1, -1, -1);
    assertEquals(fv1_1, result.getNode());
    assertEquals(3, result.getUpstream().size());
    assertEquals(0, result.getDownstream().size());
    assertTrue(result.isTraversed());
    
    //HashBased table order
    for(ProvExplicitLink link : result.getUpstream()) {
      if(link.getNode() instanceof Featuregroup) {
        Featuregroup fg = (Featuregroup)link.getNode();
        if(fg.getName().equals(fg2_1.getName())) {
          checkFG(fg2_1, link, false, true, true);
        } else if(fg.getName().equals(fg1_2.getName())) {
          checkFG(fg1_2, link, true, true, true);
        } else {
          assertFalse("should never get here", true);
        }
      } else if(link.getNode() instanceof  ProvArtifact) {
        ProvArtifact a = (ProvArtifact)link.getNode();
        if(a.getName().equals(fg1_3.getName())) {
          checkFG(fg1_3, link, true, false, false);
        } else {
          assertFalse("should never get here", true);
        }
      } else {
        assertFalse("should never get here", true);
      }
    }
  }
}
