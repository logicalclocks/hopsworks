/*
 * Copyright (C) 2022, Hopsworks AB. All rights reserved
 */
package io.hops.hopsworks.provenance.explicit.util;

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
import io.hops.hopsworks.common.util.AccessController;
import io.hops.hopsworks.common.util.Settings;
import io.hops.hopsworks.exceptions.DatasetException;
import io.hops.hopsworks.exceptions.FeaturestoreException;
import io.hops.hopsworks.persistence.entity.featurestore.Featurestore;
import io.hops.hopsworks.persistence.entity.featurestore.featuregroup.Featuregroup;
import io.hops.hopsworks.persistence.entity.featurestore.featuregroup.FeaturegroupType;
import io.hops.hopsworks.persistence.entity.featurestore.featureview.FeatureView;
import io.hops.hopsworks.persistence.entity.featurestore.trainingdataset.TrainingDataset;
import io.hops.hopsworks.persistence.entity.project.Project;
import io.hops.hopsworks.persistence.entity.provenance.FeatureGroupLink;
import io.hops.hopsworks.persistence.entity.provenance.FeatureViewLink;
import io.hops.hopsworks.persistence.entity.user.Users;
import io.hops.hopsworks.provenance.explicit.ProvExplicitControllerEEImpl;
import org.apache.hadoop.fs.Path;
import org.mockito.Mockito;
import org.mockito.stubbing.Answer;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class MockProvExplicitControllerEEImpl {
  public static class Scenario {
    public Users user;
    public Project project;
    public Featuregroup fg1;
    public Featuregroup fg2;
    public Featuregroup fg3;
    public Featuregroup fg4;
    public Featuregroup fg5;
    public Featuregroup fg6;
    public FeatureView fv1;
    public FeatureView fv2;
    public TrainingDataset td1;
    public TrainingDataset td2;
    public TrainingDataset td3;
    
    //<parent, child, value>
    public Table<Integer, Integer, FeatureGroupLink> fgLinkTable = HashBasedTable.create();
    public Table<Integer, Integer, FeatureViewLink> fvLinkTable = HashBasedTable.create();
    public Table<Integer, Integer, TrainingDataset> tdLinkTable = HashBasedTable.create();
  }
  
  public static Scenario standardScenario() {
    MockProvExplicitControllerEEImpl.Scenario scenario = new MockProvExplicitControllerEEImpl.Scenario();
  
    scenario.user = new Users();
    
    scenario.project = new Project();
    scenario.project.setName("project1");
    scenario.project.setId(1);
  
    Featurestore fs = new Featurestore();
    fs.setId(11);
    fs.setProject(scenario.project);
  
    scenario.fg1 = new Featuregroup(1);
    scenario.fg1.setFeaturestore(fs);
    scenario.fg1.setFeaturegroupType(FeaturegroupType.ON_DEMAND_FEATURE_GROUP);
  
    scenario.fg2 = new Featuregroup(2);
    scenario.fg2.setFeaturestore(fs);
    scenario.fg2.setFeaturegroupType(FeaturegroupType.CACHED_FEATURE_GROUP);
  
    scenario.fg3 = new Featuregroup(3);
    scenario.fg3.setFeaturestore(fs);
    scenario.fg3.setFeaturegroupType(FeaturegroupType.CACHED_FEATURE_GROUP);
  
    scenario.fg4 = new Featuregroup(4);
    scenario.fg4.setFeaturestore(fs);
    scenario.fg4.setFeaturegroupType(FeaturegroupType.STREAM_FEATURE_GROUP);
  
    scenario.fg5 = new Featuregroup(5);
    scenario.fg5.setFeaturestore(fs);
    scenario.fg5.setFeaturegroupType(FeaturegroupType.STREAM_FEATURE_GROUP);
  
    scenario.fg6 = new Featuregroup(6);
    scenario.fg6.setFeaturestore(fs);
    scenario.fg6.setFeaturegroupType(FeaturegroupType.CACHED_FEATURE_GROUP);
  
    scenario.fv1 = new FeatureView();
    scenario.fv1.setId(101);
    scenario.fv1.setName("fv1");
    scenario.fv1.setVersion(1);
    scenario.fv1.setFeaturestore(fs);
  
    scenario.fv2 = new FeatureView();
    scenario.fv2.setId(102);
    scenario.fv2.setName("fv2");
    scenario.fv2.setVersion(1);
    scenario.fv2.setFeaturestore(fs);
  
    scenario.td1 = new TrainingDataset();
    scenario.td1.setId(1001);
    scenario.td1.setName("fv1_1");
    scenario.td1.setVersion(1);
    scenario.td1.setFeaturestore(fs);
  
    scenario.td2 = new TrainingDataset();
    scenario.td2.setId(1002);
    scenario.td2.setName("fv1_1");
    scenario.td2.setVersion(2);
    scenario.td2.setFeaturestore(fs);
  
    scenario.td3 = new TrainingDataset();
    scenario.td3.setId(1003);
    scenario.td2.setName("fv2_1");
    scenario.td3.setVersion(1);
    scenario.td3.setFeaturestore(fs);
  
    FeatureGroupLink fgLink1 = new FeatureGroupLink();
    fgLink1.setParentFeatureGroup(scenario.fg1);
    fgLink1.setFeatureGroup(scenario.fg3);
    scenario.fgLinkTable.put(scenario.fg1.getId(), scenario.fg3.getId(), fgLink1);
  
    FeatureGroupLink fgLink2 = new FeatureGroupLink();
    fgLink2.setParentFeatureGroup(scenario.fg2);
    fgLink2.setFeatureGroup(scenario.fg3);
    scenario.fgLinkTable.put(scenario.fg2.getId(), scenario.fg3.getId(), fgLink2);
  
    FeatureGroupLink fgLink3 = new FeatureGroupLink();
    fgLink3.setParentFeatureGroup(scenario.fg4);
    fgLink3.setFeatureGroup(scenario.fg5);
    scenario.fgLinkTable.put(scenario.fg4.getId(), scenario.fg5.getId(), fgLink3);
  
    FeatureGroupLink fgLink4 = new FeatureGroupLink();
    fgLink4.setParentFeatureGroup(scenario.fg5);
    fgLink4.setFeatureGroup(scenario.fg6);
    scenario.fgLinkTable.put(scenario.fg5.getId(), scenario.fg6.getId(), fgLink4);
  
    FeatureViewLink fvLink1 = new FeatureViewLink();
    fvLink1.setParentFeatureGroup(scenario.fg3);
    fvLink1.setFeatureView(scenario.fv1);
    scenario.fvLinkTable.put(scenario.fg3.getId(), scenario.fv1.getId(), fvLink1);
  
    FeatureViewLink fvLink2 = new FeatureViewLink();
    fvLink2.setParentFeatureGroup(scenario.fg6);
    fvLink2.setFeatureView(scenario.fv1);
    scenario.fvLinkTable.put(scenario.fg6.getId(), scenario.fv1.getId(), fvLink2);
  
    FeatureViewLink fvLink3 = new FeatureViewLink();
    fvLink3.setParentFeatureGroup(scenario.fg6);
    fvLink3.setFeatureView(scenario.fv2);
    scenario.fvLinkTable.put(scenario.fg6.getId(), scenario.fv2.getId(), fvLink3);
  
    scenario.td1.setFeatureView(scenario.fv1);
    scenario.tdLinkTable.put(scenario.fv1.getId(), scenario.td1.getId(), scenario.td1);
  
    scenario.td2.setFeatureView(scenario.fv1);
    scenario.tdLinkTable.put(scenario.fv1.getId(), scenario.td2.getId(), scenario.td2);
  
    scenario.td3.setFeatureView(scenario.fv2);
    scenario.tdLinkTable.put(scenario.fv2.getId(), scenario.td3.getId(), scenario.td3);
    return scenario;
  }
  
  private static class Setup {
    public Settings settings;
    public FeatureGroupLinkFacade fgLinkFacade;
    public FeatureViewLinkFacade fvLinkFacade;
    public TrainingDatasetFacade tdFacade;
    public FeaturestoreController featureStoreCtrl;
    public FeatureViewController featureViewCtrl;
    public TrainingDatasetController trainingDatasetCtrl;
    public DatasetHelper datasetHelper;
    public AccessController accessCtrl;
  }
  
  public static ProvExplicitControllerEEImpl baseSetup(Scenario scenario)
    throws FeaturestoreException, DatasetException {
    Setup setup = new Setup();
    setup.fgLinkFacade = Mockito.mock(FeatureGroupLinkFacade.class);
    setupFeatureGroupLinkTable(setup, scenario);
    setup.fvLinkFacade = Mockito.mock(FeatureViewLinkFacade.class);
    setupFeatureViewLinkTable(setup, scenario);
    setup.tdFacade = Mockito.mock(TrainingDatasetFacade.class);
    setupTrainingDatasetLinkTable(setup, scenario);
    setup.settings = Mockito.mock(Settings.class);
    setup.featureStoreCtrl = Mockito.mock(FeaturestoreController.class);
    setup.featureViewCtrl = Mockito.mock(FeatureViewController.class);
    setup.trainingDatasetCtrl = Mockito.mock(TrainingDatasetController.class);
    setup.datasetHelper = Mockito.mock(DatasetHelper.class);
    setup.accessCtrl = Mockito.mock(AccessController.class);
    //always has access in this scenario
    Mockito.when(setup.accessCtrl.hasAccess(Mockito.any(), Mockito.any())).thenReturn(true);
    //don't care about location as we always grant permission to access
    Mockito.when(setup.featureStoreCtrl.getProjectFeaturestoreDataset(Mockito.any())).thenReturn(null);
    Mockito.when(setup.datasetHelper.getDatasetPath(Mockito.any(), Mockito.any(), Mockito.any()))
      .thenReturn(new DatasetPath((Path)null, (Path)null, (Path)null));
    Mockito.when(setup.featureViewCtrl.getLocation(Mockito.any())).thenReturn(null);
    Mockito.when(setup.trainingDatasetCtrl.getTrainingDatasetInodePath(Mockito.any())).thenReturn(null);

    Mockito.when(setup.settings.getProvenanceGraphMaxSize()).thenReturn(10000);
    return new ProvExplicitControllerEEImpl(setup.settings, setup.fgLinkFacade, setup.fvLinkFacade, setup.tdFacade,
      setup.featureStoreCtrl, setup.featureViewCtrl, setup.trainingDatasetCtrl, setup.datasetHelper, setup.accessCtrl);
  }

  private static void setupFeatureGroupLinkTable(Setup setup, Scenario scenario) {
    Mockito.when(setup.fgLinkFacade.findByChild(Mockito.any())).thenAnswer(
      (Answer<Collection<FeatureGroupLink>>) invocation -> {
        Featuregroup fg = invocation.getArgument(0);
        return scenario.fgLinkTable.column(fg.getId()).values();
      });
    Mockito.when(setup.fgLinkFacade.findByChildren(Mockito.any())).thenAnswer(
      (Answer<Collection<FeatureGroupLink>>) invocation -> {
        List<Featuregroup> fgs = invocation.getArgument(0);
        List<FeatureGroupLink> result = new ArrayList<>();
        for(Featuregroup fg : fgs) {
          result.addAll(scenario.fgLinkTable.column(fg.getId()).values());
        }
        return result;
      });
    Mockito.when(setup.fgLinkFacade.findByParent(Mockito.any())).thenAnswer(
      (Answer<Collection<FeatureGroupLink>>) invocation -> {
        Featuregroup fg = invocation.getArgument(0);
        return scenario.fgLinkTable.row(fg.getId()).values();
      });
    Mockito.when(setup.fgLinkFacade.findByParents(Mockito.any())).thenAnswer(
      (Answer<Collection<FeatureGroupLink>>) invocation -> {
        List<Featuregroup> fgs = invocation.getArgument(0);
        List<FeatureGroupLink> result = new ArrayList<>();
        for(Featuregroup fg : fgs) {
          result.addAll(scenario.fgLinkTable.row(fg.getId()).values());
        }
        return result;
      });
  }
  private static void setupFeatureViewLinkTable(Setup setup, Scenario scenario) {
    Mockito.when(setup.fvLinkFacade.findByChild(Mockito.any())).thenAnswer(
      (Answer<Collection<FeatureViewLink>>) invocation -> {
        FeatureView fv = invocation.getArgument(0);
        return scenario.fvLinkTable.column(fv.getId()).values();
      });
    Mockito.when(setup.fvLinkFacade.findByChildren(Mockito.any())).thenAnswer(
      (Answer<Collection<FeatureViewLink>>) invocation -> {
        List<FeatureView> fvs = invocation.getArgument(0);
        List<FeatureViewLink> result = new ArrayList<>();
        for(FeatureView fv : fvs) {
          result.addAll(scenario.fvLinkTable.column(fv.getId()).values());
        }
        return result;
      });
    Mockito.when(setup.fvLinkFacade.findByParent(Mockito.any())).thenAnswer(
      (Answer<Collection<FeatureViewLink>>) invocation -> {
        Featuregroup fg = invocation.getArgument(0);
        return scenario.fvLinkTable.row(fg.getId()).values();
      });
    Mockito.when(setup.fvLinkFacade.findByParents(Mockito.any())).thenAnswer(
      (Answer<Collection<FeatureViewLink>>) invocation -> {
        List<Featuregroup> fgs = invocation.getArgument(0);
        List<FeatureViewLink> result = new ArrayList<>();
        for(Featuregroup fg : fgs) {
          result.addAll(scenario.fvLinkTable.row(fg.getId()).values());
        }
        return result;
      });
  }
  private static void setupTrainingDatasetLinkTable(Setup setup, Scenario scenario) {
    Mockito.when(setup.tdFacade.findByIds(Mockito.any())).thenAnswer(
      (Answer<Collection<TrainingDataset>>) invocation -> {
        List<Integer> tds = invocation.getArgument(0);
        List<TrainingDataset> result = new ArrayList<>();
        for(Integer td : tds) {
          result.addAll(scenario.tdLinkTable.column(td).values());
        }
        return result;
      });
    Mockito.when(setup.tdFacade.findByFeatureViews(Mockito.any())).thenAnswer(
      (Answer<Collection<TrainingDataset>>) invocation -> {
        List<FeatureView> fvs = invocation.getArgument(0);
        List<TrainingDataset> result = new ArrayList<>();
        for(FeatureView fv : fvs) {
          result.addAll(scenario.tdLinkTable.row(fv.getId()).values());
        }
        return result;
      });
  }
}
