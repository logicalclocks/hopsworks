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
package io.hops.hopsworks.api.elastic.featurestore;

import io.hops.hopsworks.common.dao.dataset.DatasetFacade;
import io.hops.hopsworks.common.dao.dataset.DatasetSharedWithFacade;
import io.hops.hopsworks.common.dao.hdfs.inode.InodeFacade;
import io.hops.hopsworks.common.dao.project.ProjectFacade;
import io.hops.hopsworks.common.dao.project.team.ProjectTeamFacade;
import io.hops.hopsworks.common.elastic.FeaturestoreDocType;
import io.hops.hopsworks.common.featurestore.trainingdatasets.TrainingDatasetController;
import io.hops.hopsworks.common.hdfs.Utils;
import io.hops.hopsworks.persistence.entity.dataset.Dataset;
import io.hops.hopsworks.persistence.entity.dataset.DatasetSharedWith;
import io.hops.hopsworks.persistence.entity.hdfs.inode.Inode;
import io.hops.hopsworks.persistence.entity.project.Project;
import io.hops.hopsworks.persistence.entity.user.Users;
import org.javatuples.Pair;

import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

@Stateless
@TransactionAttribute(TransactionAttributeType.NEVER)
public class DatasetAccessController {
  private static final Logger LOGGER = Logger.getLogger(DatasetAccessController.class.getName());
  
  @EJB
  private ProjectFacade projectFacade;
  @EJB
  private ProjectTeamFacade projectTeamFacade;
  @EJB
  private InodeFacade inodeFacade;
  @EJB
  private DatasetFacade datasetFacade;
  @EJB
  private DatasetSharedWithFacade dsSharedWithFacade;
  @EJB
  private TrainingDatasetController tdCtrl;
  
  private String getDatasetType(DatasetSharedWith ds) {
    Project parentProject = ds.getDataset().getProject();
    if(Utils.getFeaturestoreName(parentProject).equals(ds.getDataset().getName())) {
      return "FEATURESTORE";
    } else if(tdCtrl.getTrainingDatasetFolderName(parentProject).equals(ds.getDataset().getName())) {
      return "TRAINING_DATASET";
    } else {
      return "DATASET";
    }
  }
  /**
   *
   * @param targetProject
   * @param docType
   * @return target project as well as any project that might have shared their featurestore and/or training datasets
   */
  public Map<FeaturestoreDocType, Set<Integer>> featurestoreSearchContext(Project targetProject,
    FeaturestoreDocType docType) {
    Map<FeaturestoreDocType, Set<Integer>> searchProjects = new HashMap<>();
  
    Set<Integer> featuregroupProjects = new HashSet<>();
    Set<Integer> trainingdatasetProjects = new HashSet<>();
    Set<Integer> featuresProjects = new HashSet<>();
  
    /**
     * Split the shared datasets in FEATURESTORE/TD/DATASET and get the parent project they are coming from.
     * Also I am only interested in the parents of the shared datasets - thus the name sharedFromProjects. We need to
     * know the parent projects that shared their FEATURESTORE/TD with us.
     */
    Map<String, Set<Integer>> sharedFromProjects
      = targetProject.getDatasetSharedWithCollection().stream()
      .filter(DatasetSharedWith::getAccepted)
      .collect(Collectors.groupingBy((ds) -> getDatasetType(ds),
      Collectors.mapping((ds) -> ds.getDataset().getProject().getId(), Collectors.toCollection(HashSet::new))));
    switch(docType) {
      case FEATUREGROUP:
        featuregroupProjects.add(targetProject.getId());
        Optional.ofNullable(sharedFromProjects.get("FEATURESTORE")).ifPresent(featuregroupProjects::addAll);
        searchProjects.put(FeaturestoreDocType.FEATUREGROUP, featuregroupProjects);
        break;
      case TRAININGDATASET:
        trainingdatasetProjects.add(targetProject.getId());
        Optional.ofNullable(sharedFromProjects.get("TRAINING_DATASET")).ifPresent(trainingdatasetProjects::addAll);
        searchProjects.put(FeaturestoreDocType.TRAININGDATASET, trainingdatasetProjects);
        break;
      case FEATURE:
        featuresProjects.add(targetProject.getId());
        Optional.ofNullable(sharedFromProjects.get("FEATURESTORE")).ifPresent(featuresProjects::addAll);
        searchProjects.put(FeaturestoreDocType.FEATURE, featuresProjects);
        break;
      case ALL:
        featuregroupProjects.add(targetProject.getId());
        Optional.ofNullable(sharedFromProjects.get("FEATURESTORE")).ifPresent(featuregroupProjects::addAll);
        searchProjects.put(FeaturestoreDocType.FEATUREGROUP, featuregroupProjects);
  
        trainingdatasetProjects.add(targetProject.getId());
        Optional.ofNullable(sharedFromProjects.get("TRAINING_DATASET")).ifPresent(trainingdatasetProjects::addAll);
        searchProjects.put(FeaturestoreDocType.TRAININGDATASET, trainingdatasetProjects);
  
        featuresProjects.add(targetProject.getId());
        Optional.ofNullable(sharedFromProjects.get("FEATURESTORE")).ifPresent(featuresProjects::addAll);
        searchProjects.put(FeaturestoreDocType.FEATURE, featuresProjects);
        break;
    }
    return searchProjects;
  }
  
  public interface DatasetAccessCtrl extends BiConsumer<DatasetDetails, ProjectsCollector> {
  }
  
  /**
   * DatasetAccessCtrl - DatasetDetails - supplies parent dataset inode id and parent project id
   * DatasetAccessCtrl - ProjectsCollector - collects all the results
   *
   * This method adds in the ProjectsCollector all Projects that:
   * 1. the user has access to and
   * 2. the user can use to access the dataset with the id supplied by the DatasetDetails (shared)
   *
   * This cache is intended to be used over short periods of time - used for processing one request (with multiple
   * values in it), but not shared over multiple requests. For example parsing the results of one elasticsearch query.
   * It should not be shared across queries since then you would have to deal with stale values.
   * Since this cache is used per request, it is safe to use with multiple instances of glassfish.
   * Discarded(reset) between different requests(short intervals) in order to avoid stale cache values.
   *
   * @param user
   */
  public DatasetAccessCtrl memoizedAccessorProjects(Users user) {
    ShortLivedCache cache = new ShortLivedCache();
    return (i, c) -> accessorProjects(user, i, c, cache);
  }
  
  /**
   * This method adds in the ProjectsCollector all Projects that:
   * 1. the user has access to and
   * 2. the user can use to access the dataset with the id supplied by the DatasetDetails (shared)
   *
   * @param user
   * @param datasetDetails - supplies parent dataset inode id and parent project id
   * @param collector - collects all the results
   */
  public void accessorProjects(Users user, DatasetDetails datasetDetails, ProjectsCollector collector) {
    ShortLivedCache cache = new ShortLivedCache();
    accessorProjects(user, datasetDetails, collector, cache);
  }
  
  private void accessorProjects(Users user, DatasetDetails datasetDetails, ProjectsCollector collector,
    ShortLivedCache cache) {
    //<project, userProjectRole>
    Pair<Project, String> projectAux = getProjectWithCache(user, datasetDetails.getParentProjectId(), cache);
    //check if parent project and parent dataset still exist or is this a stale item
    if(projectAux == null) {
      LOGGER.log(Level.FINE, "parent project of item - not found - probably stale item:{0}", datasetDetails);
      return;
    }
    Dataset dataset = getDatasetWithCache(projectAux.getValue0(), datasetDetails.getParentDatasetIId(), cache);
    if(dataset == null) {
      LOGGER.log(Level.FINE, "parent dataset of item - not found - probably stale item:{0}", datasetDetails);
      return;
    }
    //check parent project for access
    if (projectAux.getValue1() != null) {
      collector.addAccessProject(projectAux.getValue0());
    }
    //check shared datasets for access
    checkSharedDatasetsAccess(user, dataset, collector, cache);
  }
  
  private void checkSharedDatasetsAccess(Users user, Dataset dataset, ProjectsCollector collector,
    ShortLivedCache cache) {
    if(cache.sharedWithProjectsCache.containsKey(dataset.getInodeId())) {
      //cached
      Set<Integer> projectIds  = cache.sharedWithProjectsCache.get(dataset.getInodeId());
      for(Integer projectId : projectIds) {
        Pair<Project, String> projectAux = getProjectWithCache(user, projectId, cache);
        if(projectAux != null && projectAux.getValue1() != null) {
          collector.addAccessProject(projectAux.getValue0());
        }
      }
    } else {
      //not yet cached
      List<DatasetSharedWith> dsSharedWith = dsSharedWithFacade.findByDataset(dataset);
      Set<Integer> projectIds = new HashSet<>();
      cache.sharedWithProjectsCache.put(dataset.getInodeId(), projectIds);
      for(DatasetSharedWith ds : dsSharedWith) {
        projectIds.add(ds.getProject().getId());
        Pair<Project, String> projectAux = getProjectWithCache(user, ds.getProject(), cache);
        if(projectAux != null && projectAux.getValue1() != null) {
          collector.addAccessProject(projectAux.getValue0());
        }
      }
    }
  }
  
  private Dataset getDatasetWithCache(Project project, Long datasetIId, ShortLivedCache cache) {
    if(cache.datasetCache.containsKey(datasetIId)) {
      return cache.datasetCache.get(datasetIId);
    } else {
      Inode datasetInode = inodeFacade.findById(datasetIId);
      if(datasetInode == null) {
        return null;
      }
      Dataset dataset = datasetFacade.findByProjectAndInode(project, datasetInode);
      if(dataset == null) {
        return null;
      }
      cache.datasetCache.put(dataset.getInodeId(), dataset);
      return dataset;
    }
  }
  
  private Pair<Project, String> getProjectWithCache(Users user, Integer projectId, ShortLivedCache cache) {
    if (cache.projectCache.containsKey(projectId)) {
      return cache.projectCache.get(projectId);
    } else {
      Project project = projectFacade.find(projectId);
      if(project == null) {
        return null;
      }
      String projectRole = projectTeamFacade.findCurrentRole(project, user);
      cache.projectCache.put(project.getId(), Pair.with(project, projectRole));
      return Pair.with(project, projectRole);
    }
  }
  
  private Pair<Project, String> getProjectWithCache(Users user, Project project, ShortLivedCache cache) {
    if (cache.projectCache.containsKey(project.getId())) {
      return cache.projectCache.get(project.getId());
    } else {
      String projectRole = projectTeamFacade.findCurrentRole(project, user);
      cache.projectCache.put(project.getId(), Pair.with(project, projectRole));
      return Pair.with(project, projectRole);
    }
  }
  
  public interface DatasetDetails {
    Integer getParentProjectId();
    Long getParentDatasetIId();
    String toString();
  }
  
  public interface ProjectsCollector {
    void addAccessProject(Project project);
    String toString();
  }
  
  /**
   * This cache is intended to be used over short periods of time - used for processing one request (with multiple
   * values in it), but not shared over multiple requests. For example parsing the results of one elasticsearch query.
   * It should not be shared across queries since then you would have to deal with stale values.
   * Since this cache is used per request, it is safe to use with multiple instances of glassfish.
   * Discarded(reset) between different requests(short intervals) in order to avoid stale cache values.
   */
  private static class ShortLivedCache {
    Map<Integer, Pair<Project, String>> projectCache = new HashMap<>();
    Map<Long, Dataset> datasetCache = new HashMap<>();
    Map<Long, Set<Integer>> sharedWithProjectsCache = new HashMap<>();
  }
}
