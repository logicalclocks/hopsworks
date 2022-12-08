/*
 * Copyright (C) 2022, Hopsworks AB. All rights reserved
 */
package io.hops.hopsworks.provenance.explicit;

import io.hops.hopsworks.common.dataset.util.DatasetHelper;
import io.hops.hopsworks.common.featurestore.FeaturestoreController;
import io.hops.hopsworks.common.featurestore.featureview.FeatureViewController;
import io.hops.hopsworks.common.featurestore.trainingdatasets.TrainingDatasetController;
import io.hops.hopsworks.common.featurestore.trainingdatasets.TrainingDatasetFacade;
import io.hops.hopsworks.common.integrations.EnterpriseStereotype;
import io.hops.hopsworks.common.provenance.explicit.FeatureGroupLinkFacade;
import io.hops.hopsworks.common.provenance.explicit.FeatureViewLinkFacade;
import io.hops.hopsworks.common.provenance.explicit.ProvArtifact;
import io.hops.hopsworks.common.provenance.explicit.ProvExplicitControllerIface;
import io.hops.hopsworks.common.provenance.explicit.ProvExplicitLink;
import io.hops.hopsworks.common.util.AccessController;
import io.hops.hopsworks.common.util.Settings;
import io.hops.hopsworks.exceptions.DatasetException;
import io.hops.hopsworks.exceptions.FeaturestoreException;
import io.hops.hopsworks.exceptions.GenericException;
import io.hops.hopsworks.persistence.entity.dataset.Dataset;
import io.hops.hopsworks.persistence.entity.dataset.DatasetType;
import io.hops.hopsworks.persistence.entity.featurestore.featuregroup.Featuregroup;
import io.hops.hopsworks.persistence.entity.featurestore.featureview.FeatureView;
import io.hops.hopsworks.persistence.entity.featurestore.trainingdataset.TrainingDataset;
import io.hops.hopsworks.persistence.entity.project.Project;
import io.hops.hopsworks.persistence.entity.provenance.ProvExplicitNode;
import io.hops.hopsworks.persistence.entity.provenance.FeatureGroupLink;
import io.hops.hopsworks.persistence.entity.provenance.FeatureViewLink;
import io.hops.hopsworks.restutils.RESTCodes;

import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;

@Stateless
@EnterpriseStereotype
@TransactionAttribute(TransactionAttributeType.NEVER)
public class ProvExplicitControllerEEImpl implements ProvExplicitControllerIface {
  @EJB
  private Settings settings;
  @EJB
  private FeatureGroupLinkFacade featureGroupLinkFacade;
  @EJB
  private FeatureViewLinkFacade featureViewLinkFacade;
  @EJB
  private TrainingDatasetFacade trainingDatasetFacade;
  @EJB
  private FeaturestoreController featureStoreCtrl;
  @EJB
  private FeatureViewController featureViewCtrl;
  @EJB
  private TrainingDatasetController trainingDatasetCtrl;
  @EJB
  private DatasetHelper datasetHelper;
  @EJB
  private AccessController accessCtrl;
  
  public ProvExplicitControllerEEImpl() {}
  
  //for test
  public ProvExplicitControllerEEImpl(Settings settings,
                                      FeatureGroupLinkFacade featureGroupLinkFacade,
                                      FeatureViewLinkFacade featureViewLinkFacade,
                                      TrainingDatasetFacade trainingDatasetFacade,
                                      FeaturestoreController featureStoreCtrl,
                                      FeatureViewController featureViewCtrl,
                                      TrainingDatasetController trainingDatasetCtrl,
                                      DatasetHelper datasetHelper,
                                      AccessController accessCtrl) {
    this.settings = settings;
    this.featureGroupLinkFacade = featureGroupLinkFacade;
    this.featureViewLinkFacade = featureViewLinkFacade;
    this.trainingDatasetFacade = trainingDatasetFacade;
    this.featureStoreCtrl = featureStoreCtrl;
    this.featureViewCtrl = featureViewCtrl;
    this.trainingDatasetCtrl = trainingDatasetCtrl;
    this.datasetHelper = datasetHelper;
    this.accessCtrl = accessCtrl;
  }
  
  private void checkPagination(Integer upstreamLevels, Integer downstreamLevels) throws GenericException {
    if(upstreamLevels < -1 || downstreamLevels < -1) {
      throw new GenericException(RESTCodes.GenericErrorCode.ILLEGAL_ARGUMENT, Level.FINE,
        "Explicit provenance pagination is malformed - expected an integer of -1 or >0");
    }
  }
  
  @Override
  public ProvExplicitLink<Featuregroup> featureGroupLinks(Project accessProject,
                                                          Featuregroup root,
                                                          Integer upstreamLevels,
                                                          Integer downstreamLevels)
    throws GenericException, FeaturestoreException, DatasetException {
    checkPagination(upstreamLevels, downstreamLevels);
  
    ProvExplicitLink<Featuregroup> rootNode = new ProvExplicitLink<>();
    rootNode.setNode(root, fg -> String.valueOf(fg.getId()));
    rootNode.setTraversed();
    rootNode.setArtifactType(getFeatureGroupType(root));
    
    TraverseCollector upstreamCollector = new TraverseCollector();
    TraverseCollector downstreamCollector = new TraverseCollector();
    upstreamCollector.featureGroups.add(root);
    upstreamCollector.nodes.put(String.valueOf(root.getId()), rootNode);
    downstreamCollector.featureGroups.add(root);
    downstreamCollector.nodes.put(String.valueOf(root.getId()), rootNode);
    Dataset datasetLocation =
      featureStoreCtrl.getProjectFeaturestoreDataset(root.getFeaturestore().getProject());
    rootNode.setShared(!accessProject.getId().equals(root.getFeaturestore().getProject().getId()));
    rootNode.setAccessible(accessCtrl.hasAccess(accessProject, datasetLocation));
    traverse(accessProject, upstreamCollector, downstreamCollector, upstreamLevels, downstreamLevels);
    return  rootNode;
  }
  
  @Override
  public ProvExplicitLink<Featuregroup> featureGroupLinks(Project accessProject, Featuregroup root)
    throws GenericException, FeaturestoreException, DatasetException {
    return featureGroupLinks(accessProject, root, 1, 1);
  }
  
  @Override
  public ProvExplicitLink<FeatureView> featureViewLinks(Project accessProject,
                                                        FeatureView root,
                                                        Integer upstreamLevels,
                                                        Integer downstreamLevels)
    throws GenericException, FeaturestoreException, DatasetException {
    checkPagination(upstreamLevels, downstreamLevels);
    
    ProvExplicitLink<FeatureView> rootNode = new ProvExplicitLink<>();
    rootNode.setNode(root, fv -> String.valueOf(fv.getId()));
    rootNode.setTraversed();
    rootNode.setArtifactType(ProvExplicitNode.Type.FEATURE_VIEW);
    
    TraverseCollector upstreamCollector = new TraverseCollector();
    TraverseCollector downstreamCollector = new TraverseCollector();
    upstreamCollector.featureViews.add(root);
    upstreamCollector.nodes.put(String.valueOf(root.getId()), rootNode);
    downstreamCollector.featureViews.add(root);
    downstreamCollector.nodes.put(String.valueOf(root.getId()), rootNode);
    Dataset datasetLocation =
      featureStoreCtrl.getProjectFeaturestoreDataset(root.getFeaturestore().getProject());
    rootNode.setShared(!accessProject.getId().equals(root.getFeaturestore().getProject().getId()));
    rootNode.setAccessible(accessCtrl.hasAccess(accessProject, datasetLocation));
    traverse(accessProject, upstreamCollector, downstreamCollector, upstreamLevels, downstreamLevels);
    return rootNode;
  }
  
  @Override
  public ProvExplicitLink<FeatureView> featureViewLinks(Project accessProject, FeatureView root)
    throws GenericException, FeaturestoreException, DatasetException {
    return featureViewLinks(accessProject, root, 1, 1);
  }
  
  @Override
  public ProvExplicitLink<TrainingDataset> trainingDatasetLinks(Project accessProject,
                                                                TrainingDataset root,
                                                                Integer upstreamLevels,
                                                                Integer downstreamLevels)
    throws GenericException, FeaturestoreException, DatasetException {
    checkPagination(upstreamLevels, downstreamLevels);
  
    ProvExplicitLink<TrainingDataset> rootNode = new ProvExplicitLink<>();
    rootNode.setNode(root, td -> String.valueOf(td.getId()));
    rootNode.setTraversed();
    rootNode.setArtifactType(ProvExplicitNode.Type.TRAINING_DATASET);
    TraverseCollector upstreamCollector = new TraverseCollector();
    TraverseCollector downstreamCollector = new TraverseCollector();
    upstreamCollector.trainingDatasets.add(root);
    upstreamCollector.nodes.put(String.valueOf(root.getId()), rootNode);
    Dataset datasetLocation =
      featureStoreCtrl.getProjectFeaturestoreDataset(root.getFeaturestore().getProject());
    rootNode.setShared(!accessProject.getId().equals(root.getFeaturestore().getProject().getId()));
    rootNode.setAccessible(accessCtrl.hasAccess(accessProject, datasetLocation));
    traverse(accessProject, upstreamCollector, downstreamCollector, upstreamLevels, downstreamLevels);
    return rootNode;
  }
  
  @Override
  public ProvExplicitLink<TrainingDataset> trainingDatasetLinks(Project accessProject, TrainingDataset root)
    throws GenericException, FeaturestoreException, DatasetException {
    return trainingDatasetLinks(accessProject, root, 1, 1);
  }
  
  private static class TraverseCollector {
    Map<String, ProvExplicitLink> nodes = new HashMap<>();
    List<Featuregroup> featureGroups = new ArrayList<>();
    List<Featuregroup> nextFeatureGroups = new ArrayList<>();
    List<FeatureView> featureViews = new ArrayList<>();
    List<FeatureView> nextFeatureViews = new ArrayList<>();
    List<TrainingDataset> trainingDatasets = new ArrayList<>();
    int lvl = 0;
    
    void next() {
      lvl++;
      featureGroups = nextFeatureGroups;
      nextFeatureGroups = new ArrayList<>();
      featureViews = nextFeatureViews;
      nextFeatureViews = new ArrayList<>();
      trainingDatasets.clear();
    }
    
    boolean isEmpty() {
      return featureGroups.isEmpty() && featureViews.isEmpty() && trainingDatasets.isEmpty();
    }
    
    int graphSize() {
      return nodes.size();
    }
    
    void clearStep() {
      featureGroups.clear();
      featureViews.clear();
      trainingDatasets.clear();
    }
  }
  
  private void traverse(Project accessProject,
                        TraverseCollector upstreamCollector, TraverseCollector downstreamCollector,
                        Integer upstreamLevels, Integer downstreamLevels)
    throws FeaturestoreException, DatasetException {
    while(true) {
      if(upstreamLevels == -1 || upstreamLevels > upstreamCollector.lvl) {
        traverseUpstreamFeatureGroups(accessProject, upstreamCollector, downstreamCollector);
        traverseUpstreamFeatureViews(accessProject, upstreamCollector, downstreamCollector);
        traverseUpstreamTrainingDatasets(accessProject, upstreamCollector, downstreamCollector);
        upstreamCollector.next();
      } else {
        upstreamCollector.clearStep();
      }
      if(downstreamLevels == -1 || downstreamLevels > downstreamCollector.lvl) {
        traverseDownstreamFeatureGroups(accessProject, downstreamCollector, upstreamCollector);
        traverseDownstreamFeatureViews(accessProject, downstreamCollector, upstreamCollector);
        traverseDownstreamTrainingDatasets(accessProject, downstreamCollector, upstreamCollector);
        downstreamCollector.next();
      } else {
        downstreamCollector.clearStep();
      }
      if(upstreamCollector.isEmpty() && downstreamCollector.isEmpty()) {
        break;
      }
      if(upstreamCollector.graphSize() + downstreamCollector.graphSize() > settings.getProvenanceGraphMaxSize()) {
        break;
      }
    }
  }
  
  private ProvExplicitLink<ProvArtifact> buildArtifactLink(ProvArtifact artifact, ProvExplicitNode.Type artifactType,
                                                           boolean deleted, boolean shared, boolean accessible) {
    ProvExplicitLink<ProvArtifact> node = new ProvExplicitLink<>();
    node.setDeleted(deleted);
    node.setShared(shared);
    node.setAccessible(accessible);
    node.setNode(artifact, a -> a.getId());
    node.setArtifactType(artifactType);
    return node;
  }
  
  private ProvExplicitLink<Featuregroup> buildFGLink(Featuregroup fg, ProvExplicitNode.Type artifactType,
                                                     boolean deleted, boolean shared, boolean accessible) {
    ProvExplicitLink<Featuregroup> node = new ProvExplicitLink<>();
    node.setDeleted(deleted);
    node.setShared(shared);
    node.setAccessible(accessible);
    node.setNode(fg, f -> String.valueOf(f.getId()));
    node.setArtifactType(artifactType);
    return node;
  }
  
  private ProvExplicitLink<FeatureView> buildFVLink(FeatureView fv,
                                                    boolean deleted, boolean shared, boolean accessible) {
    ProvExplicitLink<FeatureView> node = new ProvExplicitLink<>();
    node.setDeleted(deleted);
    node.setShared(shared);
    node.setAccessible(accessible);
    node.setNode(fv, f -> String.valueOf(f.getId()));
    node.setArtifactType(ProvExplicitNode.Type.FEATURE_VIEW);
    return node;
  }
  
  private ProvExplicitLink<TrainingDataset> buildTDLink(TrainingDataset td,
                                                        boolean deleted, boolean shared, boolean accessible) {
    ProvExplicitLink<TrainingDataset> node = new ProvExplicitLink<>();
    node.setDeleted(deleted);
    node.setShared(shared);
    node.setAccessible(accessible);
    node.setNode(td, t -> String.valueOf(t.getId()));
    node.setArtifactType(ProvExplicitNode.Type.TRAINING_DATASET);
    return node;
  }
  
  private ProvExplicitNode.Type getFeatureGroupType(Featuregroup fg) {
    switch(fg.getFeaturegroupType()) {
      case ON_DEMAND_FEATURE_GROUP:
        return ProvExplicitNode.Type.EXTERNAL_FEATURE_GROUP;
      case CACHED_FEATURE_GROUP:
      case STREAM_FEATURE_GROUP:
      default:
        return ProvExplicitNode.Type.FEATURE_GROUP;
    }
  }
  
  private void addUpstreamFeatureGroup(Project accessProject, TraverseCollector collector, ProvExplicitLink provNode,
                                       ProvExplicitNode dbLink, Featuregroup parentFeatureGroup)
    throws FeaturestoreException {
    if(parentFeatureGroup == null) {
      ProvExplicitLink<ProvArtifact> upstreamNode = buildArtifactLink(
        ProvArtifact.fromLinkAsParent(dbLink),
        ProvExplicitNode.Type.FEATURE_GROUP,
        true, false, false);
      provNode.addUpstream(upstreamNode);
    } else {
      Dataset datasetLocation = featureStoreCtrl.getProjectFeaturestoreDataset(
        parentFeatureGroup.getFeaturestore().getProject());
      boolean shared = !accessProject.getId().equals(
        parentFeatureGroup.getFeaturestore().getProject().getId());
    
      if(accessCtrl.hasAccess(accessProject, datasetLocation)) {
        ProvExplicitLink<Featuregroup> upstreamNode = buildFGLink(
          parentFeatureGroup,
          getFeatureGroupType(parentFeatureGroup),
          false, shared, true);
        provNode.addUpstream(upstreamNode);
        if (!collector.nodes.containsKey(upstreamNode.getNodeId())) {
          //prepare next step
          collector.nextFeatureGroups.add(parentFeatureGroup);
          collector.nodes.put(upstreamNode.getNodeId(), upstreamNode);
        }
      } else {
        ProvExplicitLink<ProvArtifact> upstreamNode = buildArtifactLink(
          ProvArtifact.fromFeatureGroup(parentFeatureGroup),
          getFeatureGroupType(parentFeatureGroup),
          false, shared, false);
        provNode.addUpstream(upstreamNode);
      }
    }
  }
  
  private void traverseUpstreamFeatureGroups(Project accessProject,
                                             TraverseCollector collector,
                                             TraverseCollector other)
    throws FeaturestoreException {
    if(collector.featureGroups.isEmpty()) {
      return;
    }
    if(collector.graphSize() + other.graphSize() > settings.getProvenanceGraphMaxSize()) {
      return;
    }
    Collection<FeatureGroupLink> links = featureGroupLinkFacade.findByChildren(collector.featureGroups);
    collector.featureGroups.forEach(fg -> collector.nodes.get(String.valueOf(fg.getId())).setTraversed());
    for (FeatureGroupLink link : links) {
      if(collector.graphSize() + other.graphSize() > settings.getProvenanceGraphMaxSize()) {
        //skipped child due to size - node not fully traversed
        collector.nodes.get(String.valueOf(link.getFeatureGroup().getId())).setTraversed(false);
      } else {
        ProvExplicitLink<Featuregroup> node = collector.nodes.get(String.valueOf(link.getFeatureGroup().getId()));
        addUpstreamFeatureGroup(accessProject, collector, node, link, link.getParentFeatureGroup());
      }
    }
  }
  
  private void traverseUpstreamFeatureViews(Project accessProject,
                                            TraverseCollector collector,
                                            TraverseCollector other)
    throws FeaturestoreException {
    if(collector.featureViews.isEmpty()) {
      return;
    }
    if(collector.graphSize() + other.graphSize() > settings.getProvenanceGraphMaxSize()) {
      return;
    }
    Collection<FeatureViewLink> links = featureViewLinkFacade.findByChildren(collector.featureViews);
    collector.featureViews.forEach(fv -> collector.nodes.get(String.valueOf(fv.getId())).setTraversed());
    for (FeatureViewLink link : links) {
      if(collector.graphSize() + other.graphSize() > settings.getProvenanceGraphMaxSize()) {
        //skipped child due to size - node not fully traversed
        collector.nodes.get(String.valueOf(link.getFeatureView().getId())).setTraversed(false);
      } else {
        ProvExplicitLink<FeatureView> node = collector.nodes.get(String.valueOf(link.getFeatureView().getId()));
        addUpstreamFeatureGroup(accessProject, collector, node, link, link.getParentFeatureGroup());
      }
    }
  }
  
  private void traverseUpstreamTrainingDatasets(Project accessProject,
                                                TraverseCollector collector,
                                                TraverseCollector other)
    throws DatasetException {
    if(collector.trainingDatasets.isEmpty()) {
      return;
    }
    if(collector.graphSize() + other.graphSize() > settings.getProvenanceGraphMaxSize()) {
      return;
    }
    List<Integer> trainingDatasetIds = new LinkedList<>();
    collector.trainingDatasets.forEach( td -> {
      trainingDatasetIds.add(td.getId());
      collector.nodes.get(String.valueOf(td.getId())).setTraversed();
    });
    Collection<TrainingDataset> links = trainingDatasetFacade.findByIds(trainingDatasetIds);
    for (TrainingDataset link : links) {
      if(collector.graphSize() + other.graphSize() > settings.getProvenanceGraphMaxSize()) {
        //skipped child due to size - node not fully traversed
        collector.nodes.get(String.valueOf(link.getId())).setTraversed(false);
      } else {
        ProvExplicitLink<TrainingDataset> node = collector.nodes.get(String.valueOf(link.getId()));
        if (link.getFeatureView() == null) {
          //this can only happen for legacy training datasets with no feature view
          continue;
        }
  
        Dataset datasetLocation = datasetHelper.getDatasetPath(link.getFeatureView().getFeaturestore().getProject(),
          featureViewCtrl.getLocation(link.getFeatureView()), DatasetType.DATASET).getDataset();
        boolean shared = !accessProject.getId().equals(link.getFeatureView().getFeaturestore().getProject().getId());
  
        if (accessCtrl.hasAccess(accessProject, datasetLocation)) {
          ProvExplicitLink upstreamNode = buildFVLink(link.getFeatureView(),
            false, shared, true);
          node.addUpstream(upstreamNode);
          if (!collector.nodes.containsKey(upstreamNode.getNodeId())) {
            //prepare next step
            collector.nextFeatureViews.add(link.getFeatureView());
            collector.nodes.put(upstreamNode.getNodeId(), upstreamNode);
          }
        } else {
          ProvExplicitLink<ProvArtifact> upstreamNode = buildArtifactLink(
            ProvArtifact.fromFeatureView(link.getFeatureView()),
            ProvExplicitNode.Type.FEATURE_VIEW,
            false, shared, false);
          node.addUpstream(upstreamNode);
        }
      }
    }
  }
  
  private void traverseDownstreamFeatureGroups(Project accessProject,
                                               TraverseCollector collector,
                                               TraverseCollector other)
    throws FeaturestoreException {
    if(collector.featureGroups.isEmpty()) {
      return;
    }
    if(collector.graphSize() + other.graphSize() > settings.getProvenanceGraphMaxSize()) {
      return;
    }
    Collection<FeatureGroupLink> links = featureGroupLinkFacade.findByParents(collector.featureGroups);
    collector.featureGroups.forEach(fg -> collector.nodes.get(String.valueOf(fg.getId())).setTraversed());
    for (FeatureGroupLink link : links) {
      if(collector.graphSize() + other.graphSize() > settings.getProvenanceGraphMaxSize()) {
        //skipped child due to size - node not fully traversed
        collector.nodes.get(String.valueOf(link.getParentFeatureGroup().getId())).setTraversed(false);
      } else {
        ProvExplicitLink<Featuregroup> node = collector.nodes.get(String.valueOf(link.getParentFeatureGroup().getId()));
  
        Dataset datasetLocation = featureStoreCtrl.getProjectFeaturestoreDataset(
          link.getFeatureGroup().getFeaturestore().getProject());
        boolean shared = !accessProject.getId().equals(link.getFeatureGroup().getFeaturestore().getProject().getId());
  
        if (accessCtrl.hasAccess(accessProject, datasetLocation)) {
          ProvExplicitLink<Featuregroup> downstreamNode = buildFGLink(
            link.getFeatureGroup(),
            getFeatureGroupType(link.getFeatureGroup()),
            false, shared, true);
          node.addDownstream(downstreamNode);
          if (!collector.nodes.containsKey(String.valueOf(downstreamNode.getNodeId()))) {
            //prepare next step
            collector.nextFeatureGroups.add(link.getFeatureGroup());
            collector.nodes.put(downstreamNode.getNodeId(), downstreamNode);
          }
        } else {
          ProvExplicitLink<ProvArtifact> downstreamNode = buildArtifactLink(
            ProvArtifact.fromFeatureGroup(link.getFeatureGroup()),
            getFeatureGroupType(link.getFeatureGroup()),
            false, shared, false);
          node.addDownstream(downstreamNode);
        }
      }
    }
  }
  
  private void traverseDownstreamFeatureViews(Project accessProject,
                                              TraverseCollector collector,
                                              TraverseCollector other)
    throws DatasetException {
    if(collector.featureGroups.isEmpty()) {
      return;
    }
    if(collector.graphSize() + other.graphSize() > settings.getProvenanceGraphMaxSize()) {
      return;
    }
    Collection<FeatureViewLink> links = featureViewLinkFacade.findByParents(collector.featureGroups);
    collector.featureGroups.forEach(fg -> collector.nodes.get(String.valueOf(fg.getId())).setTraversed());
    for (FeatureViewLink link : links) {
      if(collector.graphSize() + other.graphSize() > settings.getProvenanceGraphMaxSize()) {
        //skipped child due to size - node not fully traversed
        collector.nodes.get(String.valueOf(link.getParentFeatureGroup().getId())).setTraversed(false);
      } else {
        ProvExplicitLink<FeatureView> node = collector.nodes.get(String.valueOf(link.getParentFeatureGroup().getId()));
  
        Dataset datasetLocation = datasetHelper.getDatasetPath(link.getFeatureView().getFeaturestore().getProject(),
          featureViewCtrl.getLocation(link.getFeatureView()), DatasetType.DATASET).getDataset();
        boolean shared = !accessProject.getId().equals(link.getFeatureView().getFeaturestore().getProject().getId());
        if (accessCtrl.hasAccess(accessProject, datasetLocation)) {
          ProvExplicitLink<FeatureView> downstreamNode = buildFVLink(
            link.getFeatureView(),
            false, shared, true);
          node.addDownstream(downstreamNode);
          if (downstreamNode.isAccessible() && !collector.nodes.containsKey(downstreamNode.getNodeId())) {
            //prepare next step
            collector.nextFeatureViews.add(downstreamNode.getNode());
            collector.nodes.put(downstreamNode.getNodeId(), downstreamNode);
          }
        } else {
          ProvExplicitLink<ProvArtifact> downstreamNode = buildArtifactLink(
            ProvArtifact.fromFeatureView(link.getFeatureView()),
            ProvExplicitNode.Type.FEATURE_VIEW,
            false, shared, false);
          node.addDownstream(downstreamNode);
        }
      }
    }
  }
  
  private void traverseDownstreamTrainingDatasets(Project accessProject,
                                                  TraverseCollector collector, TraverseCollector other)
    throws DatasetException {
    if(collector.featureViews.isEmpty()) {
      return;
    }
    if(collector.graphSize() + other.graphSize() > settings.getProvenanceGraphMaxSize()) {
      return;
    }
    Collection<TrainingDataset> links = trainingDatasetFacade.findByFeatureViews(collector.featureViews);
    collector.featureViews.forEach(fv -> collector.nodes.get(String.valueOf(fv.getId())).setTraversed());
    for (TrainingDataset td : links) {
      if(collector.graphSize() + other.graphSize() > settings.getProvenanceGraphMaxSize()) {
        //skipped child due to size - node not fully traversed
        collector.nodes.get(String.valueOf(td.getFeatureView().getId())).setTraversed(false);
      } else {
        ProvExplicitLink<FeatureView> node = collector.nodes.get(String.valueOf(td.getFeatureView().getId()));
  
        Dataset datasetLocation = datasetHelper.getDatasetPath(td.getFeaturestore().getProject(),
          trainingDatasetCtrl.getTrainingDatasetInodePath(td), DatasetType.DATASET).getDataset();
        boolean shared = !accessProject.getId().equals(td.getFeaturestore().getProject().getId());
        if (accessCtrl.hasAccess(accessProject, datasetLocation)) {
          ProvExplicitLink<TrainingDataset> downstreamNode = buildTDLink(
            td,
            false, shared, true);
          node.addDownstream(downstreamNode);
    
          if (downstreamNode.isAccessible() && !collector.nodes.containsKey(downstreamNode.getNodeId())) {
            //prepare next step
            collector.nodes.put(downstreamNode.getNodeId(), downstreamNode);
          }
          //when we add model/experiments remove this auto traverse
          downstreamNode.setTraversed();
        } else {
          ProvExplicitLink<ProvArtifact> downstreamNode = buildArtifactLink(
            ProvArtifact.fromTrainingDataset(td),
            ProvExplicitNode.Type.TRAINING_DATASET,
            false, shared, false);
          node.addDownstream(downstreamNode);
        }
      }
    }
  }
}
