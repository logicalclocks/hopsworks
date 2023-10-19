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
package io.hops.hopsworks.api.opensearch;

import io.hops.hopsworks.common.api.RestDTO;
import io.hops.hopsworks.common.dao.hdfs.inode.InodeFacade;
import io.hops.hopsworks.common.dao.project.ProjectFacade;
import io.hops.hopsworks.common.dao.project.team.ProjectTeamFacade;
import io.hops.hopsworks.common.dao.user.UserFacade;
import io.hops.hopsworks.common.dataset.DatasetController;
import io.hops.hopsworks.common.opensearch.OpenSearchController;
import io.hops.hopsworks.common.hdfs.HdfsUsersController;
import io.hops.hopsworks.common.hdfs.inode.InodeController;
import io.hops.hopsworks.common.util.ProjectUtils;
import io.hops.hopsworks.common.util.Settings;
import io.hops.hopsworks.exceptions.OpenSearchException;
import io.hops.hopsworks.exceptions.ServiceException;
import io.hops.hopsworks.persistence.entity.dataset.Dataset;
import io.hops.hopsworks.persistence.entity.dataset.DatasetSharedWith;
import io.hops.hopsworks.persistence.entity.hdfs.inode.Inode;
import io.hops.hopsworks.persistence.entity.project.Project;
import io.hops.hopsworks.persistence.entity.project.team.ProjectTeam;
import io.hops.hopsworks.persistence.entity.user.Users;
import org.opensearch.search.SearchHit;

import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

@Stateless
@TransactionAttribute(TransactionAttributeType.NEVER)
public class OpenSearchHitsBuilder {
  
  private static final Logger LOGGER = Logger.getLogger(OpenSearchHitsBuilder.class.getName());
  
  @EJB
  private OpenSearchController openSearchController;
  @EJB
  private ProjectFacade projectFacade;
  @EJB
  private ProjectTeamFacade projectTeamFacade;
  @EJB
  private DatasetController datasetController;
  @EJB
  private InodeController inodeController;
  @EJB
  private InodeFacade inodeFacade;
  @EJB
  private UserFacade userFacade;
  @EJB
  private HdfsUsersController hdfsUsersController;
  @EJB
  private ProjectUtils projectUtils;
  
  public OpenSearchHitDTO buildOpenSearchHits(String searchTerm, Users user)
      throws ServiceException, OpenSearchException {
    OpenSearchHitDTO openSearchHitDTO = new OpenSearchHitDTO();
    openSearchHitDTO.setProjects(new OpenSearchProjectDTO());
    openSearchHitDTO.setDatasets(new OpenSearchDatasetDTO());
    openSearchHitDTO.setInodes(new OpenSearchInodeDTO());
    SearchHit[] openSearchHits = openSearchController.globalSearchHighLevel(searchTerm);
    for (SearchHit hit : openSearchHits) {
      if (hit.getSourceAsMap().containsKey(Settings.META_DOC_TYPE_FIELD)) {
        String type = (String) hit.getSourceAsMap().get(Settings.META_DOC_TYPE_FIELD);
        switch (type) {
          case Settings.DOC_TYPE_PROJECT:
            buildOpenSearchProjects(hit, openSearchHitDTO.getProjects(), user);
            break;
          case Settings.DOC_TYPE_DATASET:
            buildOpenSearchDatasets(hit, openSearchHitDTO.getDatasets(), user);
            break;
          case Settings.DOC_TYPE_INODE:
            buildOpenSearchInodes(hit, openSearchHitDTO.getInodes());
            break;
          default:
            LOGGER
              .log(Level.WARNING, "Got a wrong document type [{0}] was expecting one of these types [{1}, {2}, {3}]",
                new Object[]{type, Settings.DOC_TYPE_INODE, Settings.DOC_TYPE_DATASET, Settings.DOC_TYPE_PROJECT});
        }
      }
    }
    setCount(openSearchHitDTO.getProjects());
    setCount(openSearchHitDTO.getDatasets());
    setCount(openSearchHitDTO.getInodes());
    return openSearchHitDTO;
  }
  
  public OpenSearchHitDTO buildOpenSearchHits(Integer projectId, String searchTerm, Users user)
      throws ServiceException, OpenSearchException {
    OpenSearchHitDTO openSearchHitDTO = new OpenSearchHitDTO();
    openSearchHitDTO.setProjects(new OpenSearchProjectDTO());
    openSearchHitDTO.setDatasets(new OpenSearchDatasetDTO());
    openSearchHitDTO.setInodes(new OpenSearchInodeDTO());
    SearchHit[] openSearchHits = openSearchController.projectSearchHighLevel(projectId, searchTerm);
    for (SearchHit hit : openSearchHits) {
      if (hit.getSourceAsMap().containsKey(Settings.META_DOC_TYPE_FIELD)) {
        String type = (String) hit.getSourceAsMap().get(Settings.META_DOC_TYPE_FIELD);
        switch (type) {
          case Settings.DOC_TYPE_PROJECT:
            buildOpenSearchProjects(hit, openSearchHitDTO.getProjects(), user);
            break;
          case Settings.DOC_TYPE_DATASET:
            buildOpenSearchDatasets(hit, openSearchHitDTO.getDatasets(), user);
            break;
          case Settings.DOC_TYPE_INODE:
            buildOpenSearchInodes(hit, openSearchHitDTO.getInodes());
            break;
          default:
            LOGGER
              .log(Level.WARNING, "Got a wrong document type [{0}] was expecting one of these types [{1}, {2}, {3}]",
                new Object[]{type, Settings.DOC_TYPE_INODE, Settings.DOC_TYPE_DATASET, Settings.DOC_TYPE_PROJECT});
        }
      }
    }
    setCount(openSearchHitDTO.getProjects());
    setCount(openSearchHitDTO.getDatasets());
    setCount(openSearchHitDTO.getInodes());
    return openSearchHitDTO;
  }
  
  public OpenSearchHitDTO buildOpenSearchHits(Integer projectId, String datasetName, String searchTerm, Users user)
    throws ServiceException, OpenSearchException {
    OpenSearchHitDTO openSearchHitDTO = new OpenSearchHitDTO();
    openSearchHitDTO.setProjects(new OpenSearchProjectDTO());
    openSearchHitDTO.setDatasets(new OpenSearchDatasetDTO());
    openSearchHitDTO.setInodes(new OpenSearchInodeDTO());
    SearchHit[] openSearchHits = openSearchController.datasetSearchHighLevel(projectId, datasetName, searchTerm);
    for (SearchHit hit : openSearchHits) {
      if (hit.getSourceAsMap().containsKey(Settings.META_DOC_TYPE_FIELD)) {
        String type = (String) hit.getSourceAsMap().get(Settings.META_DOC_TYPE_FIELD);
        switch (type) {
          case Settings.DOC_TYPE_PROJECT:
            buildOpenSearchProjects(hit, openSearchHitDTO.getProjects(), user);
            break;
          case Settings.DOC_TYPE_DATASET:
            buildOpenSearchDatasets(hit, openSearchHitDTO.getDatasets(), user);
            break;
          case Settings.DOC_TYPE_INODE:
            buildOpenSearchInodes(hit, openSearchHitDTO.getInodes());
            break;
          default:
            LOGGER
              .log(Level.WARNING, "Got a wrong document type [{0}] was expecting one of these types [{1}, {2}, {3}]",
                new Object[]{type, Settings.DOC_TYPE_INODE, Settings.DOC_TYPE_DATASET, Settings.DOC_TYPE_PROJECT});
        }
      }
    }
    setCount(openSearchHitDTO.getProjects());
    setCount(openSearchHitDTO.getDatasets());
    setCount(openSearchHitDTO.getInodes());
    return openSearchHitDTO;
  }
  
  public void buildOpenSearchProjects(SearchHit hit, OpenSearchProjectDTO openSearchProjectDTO, Users user) {
    OpenSearchProjectDTO item = new OpenSearchProjectDTO();
    item.setMap(hit.getSourceAsMap());
    item.setScore(hit.getScore());
    item.setProjectId(getIntValue(hit, "project_id"));
    item.setName(getStringValue(hit, "name"));
    item.setCreator(getStringValue(hit, "user"));
    item.setDescription(getStringValue(hit,"description"));
    item.setHighlights(hit.getHighlightFields());
    if (openSearchProjectDTO.getItems() == null) {
      openSearchProjectDTO.setItems(new ArrayList<>());
    }
    if (item.getProjectId() != null) {
      Project project = projectFacade.find(item.getProjectId());
      if (project != null) {
        item.setMember(projectTeamFacade.isUserMemberOfProject(project, user));
        item.setProjectIId(inodeController.getProjectRoot(project.getName()).getId());
        item.setMembers(getMembers(project));
        item.setCreated(project.getCreated());
      }
      
    }
    openSearchProjectDTO.getItems().add(item);
  }
  
  public void buildOpenSearchDatasets(SearchHit hit, OpenSearchDatasetDTO openSearchDatasetDTO, Users user) {
    OpenSearchDatasetDTO item = new OpenSearchDatasetDTO();
    item.setMap(hit.getSourceAsMap());
    item.setScore(hit.getScore());
    item.setDatasetIId(getLongValue(hit, "dataset_id"));
    item.setParentProjectId(getIntValue(hit, "project_id"));
    item.setName(getStringValue(hit, "name"));
    item.setCreator(getStringValue(hit, "user"));
    item.setDescription(getStringValue(hit,"description"));
    item.setPublicDataset(getBooleanValue(hit, "public_ds"));
    //item.setSize(getLongValue(hit, "size")); not indexed
    item.setHighlights(hit.getHighlightFields());
    if (openSearchDatasetDTO.getItems() == null) {
      openSearchDatasetDTO.setItems(new ArrayList<>());
    }
    if (item.getDatasetIId() != null) {
      Inode datasetInode = inodeFacade.findById(item.getDatasetIId());
      Project project = projectFacade.findById(item.getParentProjectId()).get();
      Dataset dataset = datasetController.getDatasetByName(project, datasetInode.getInodePK().getName());
      if (dataset != null) {
        item.setParentProjectName(dataset.getProject().getName());
        item.setSize(datasetInode.getSize());
        item.setDatasetId(dataset.getId());
        item.setPublicDatasetId(dataset.getPublicDsId());
        item.setModificationTime(new Date(datasetInode.getModificationTime().longValue()));
        item.setAccessProjects(accessProject(dataset, user));
      }
    }
    item.setCreator(setUserName(item.getCreator()));
    openSearchDatasetDTO.getItems().add(item);
  }
  
  public void buildOpenSearchInodes(SearchHit hit, OpenSearchInodeDTO openSearchInodeDTO) {
    OpenSearchInodeDTO item = new OpenSearchInodeDTO();
    item.setMap(hit.getSourceAsMap());
    item.setScore(hit.getScore());
    item.setInodeId(Long.parseLong(hit.getId()));
    item.setParentDatasetIId(getLongValue(hit, "dataset_id"));
    item.setParentProjectId(getIntValue(hit, "project_id"));
    item.setName(getStringValue(hit, "name"));
    item.setCreator(getStringValue(hit, "user"));
    item.setDescription(getStringValue(hit,"description"));
    item.setSize(getLongValue(hit, "size"));
    item.setHighlights(hit.getHighlightFields());
    if (openSearchInodeDTO.getItems() == null) {
      openSearchInodeDTO.setItems(new ArrayList<>());
    }
    if (item.getParentDatasetIId() != null) {
      Inode datasetInode = inodeFacade.findById(item.getParentDatasetIId());
      Project project = projectFacade.findById(item.getParentProjectId()).get();
      Dataset dataset = datasetController.getDatasetByName(project, datasetInode.getInodePK().getName());
      if (dataset != null) {
        item.setParentDatasetId(dataset.getId());
        item.setParentDatasetName(dataset.getName());
        item.setModificationTime(new Date(datasetInode.getModificationTime().longValue()));
      }
    }
    if (item.getInodeId() != null) {
      Inode inode = inodeFacade.findById(item.getInodeId());
      if (inode != null) {
        item.setPath(inodeController.getPath(inode));
      }
    }
    item.setCreator(setUserName(item.getCreator()));
    openSearchInodeDTO.getItems().add(item);
  }
  
  private String setUserName(String creator) {
    if (creator != null && creator.contains(HdfsUsersController.USER_NAME_DELIMITER)) {
      Users user = userFacade.findByUsername(hdfsUsersController.getUserName(creator));
      if (user != null) {
        return user.getFname() + " " + user.getLname();
      }
    }
    return creator;
  }
  
  private void setCount(RestDTO restDTO) {
    if (restDTO.getItems() != null) {
      restDTO.setCount(Long.valueOf(restDTO.getItems().size()));
    } else {
      restDTO.setItems(new ArrayList<>());
      restDTO.setCount(0l);
    }
  }
  
  private Long getLongValue(SearchHit hit, String name) {
    return hit.getSourceAsMap().containsKey(name)? Long.parseLong(hit.getSourceAsMap().get(name).toString()) : null;
  }
  
  private Integer getIntValue(SearchHit hit, String name) {
    return hit.getSourceAsMap().containsKey(name)? Integer.parseInt(hit.getSourceAsMap().get(name).toString()) : null;
  }
  
  private String getStringValue(SearchHit hit, String name) {
    return hit.getSourceAsMap().containsKey(name)? hit.getSourceAsMap().get(name).toString() : "";
  }
  
  private Boolean getBooleanValue(SearchHit hit, String name) {
    return hit.getSourceAsMap().containsKey(name)? Boolean.valueOf(hit.getSourceAsMap().get(name).toString()) : null;
  }
  
  private List<String> getMembers(Project project) {
    List<String> members = new ArrayList<>();
    for (ProjectTeam member: projectUtils.getProjectTeamCollection(project)) {
      members.add(member.getUser().getFname() + " " + member.getUser().getLname());
    }
    return members;
  }
  
  private Map<Integer, String> accessProject(Dataset dataset, Users user) {
    Map<Integer, String> accessProjects = new HashMap<>();
    if (projectTeamFacade.isUserMemberOfProject(dataset.getProject(), user)) {
      accessProjects.put(dataset.getProject().getId(), dataset.getProject().getName());
    }
    for (DatasetSharedWith datasetSharedWith : dataset.getDatasetSharedWithCollection()) {
      if (projectTeamFacade.isUserMemberOfProject(datasetSharedWith.getProject(), user)) {
        accessProjects.put(datasetSharedWith.getProject().getId(), datasetSharedWith.getProject().getName());
      }
    }
    return accessProjects;
  }
}
