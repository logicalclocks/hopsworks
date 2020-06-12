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
package io.hops.hopsworks.api.elastic;

import io.hops.hopsworks.common.api.RestDTO;
import io.hops.hopsworks.common.dao.hdfs.inode.InodeFacade;
import io.hops.hopsworks.common.dao.project.ProjectFacade;
import io.hops.hopsworks.common.dao.project.team.ProjectTeamFacade;
import io.hops.hopsworks.common.dao.user.UserFacade;
import io.hops.hopsworks.common.dataset.DatasetController;
import io.hops.hopsworks.common.elastic.ElasticController;
import io.hops.hopsworks.common.hdfs.HdfsUsersController;
import io.hops.hopsworks.common.hdfs.inode.InodeController;
import io.hops.hopsworks.common.util.Settings;
import io.hops.hopsworks.exceptions.ElasticException;
import io.hops.hopsworks.exceptions.ServiceException;
import io.hops.hopsworks.persistence.entity.dataset.Dataset;
import io.hops.hopsworks.persistence.entity.dataset.DatasetSharedWith;
import io.hops.hopsworks.persistence.entity.hdfs.inode.Inode;
import io.hops.hopsworks.persistence.entity.project.Project;
import io.hops.hopsworks.persistence.entity.project.team.ProjectTeam;
import io.hops.hopsworks.persistence.entity.user.Users;
import org.elasticsearch.search.SearchHit;

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
public class ElasticHitsBuilder {
  
  private static final Logger LOGGER = Logger.getLogger(ElasticHitsBuilder.class.getName());
  
  @EJB
  private ElasticController elasticController;
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
  
  public ElasticHitDTO buildElasticHits(String searchTerm, Users user) throws ServiceException, ElasticException {
    ElasticHitDTO elasticHitDTO = new ElasticHitDTO();
    elasticHitDTO.setProjects(new ElasticProjectDTO());
    elasticHitDTO.setDatasets(new ElasticDatasetDTO());
    elasticHitDTO.setInodes(new ElasticInodeDTO());
    SearchHit[] elasticHits = elasticController.globalSearchHighLevel(searchTerm);
    for (SearchHit hit : elasticHits) {
      if (hit.getSourceAsMap().containsKey(Settings.META_DOC_TYPE_FIELD)) {
        String type = (String) hit.getSourceAsMap().get(Settings.META_DOC_TYPE_FIELD);
        switch (type) {
          case Settings.DOC_TYPE_PROJECT:
            buildElasticProjects(hit, elasticHitDTO.getProjects(), user);
            break;
          case Settings.DOC_TYPE_DATASET:
            buildElasticDatasets(hit, elasticHitDTO.getDatasets(), user);
            break;
          case Settings.DOC_TYPE_INODE:
            buildElasticInodes(hit, elasticHitDTO.getInodes());
            break;
          default:
            LOGGER
              .log(Level.WARNING, "Got a wrong document type [{0}] was expecting one of these types [{1}, {2}, {3}]",
                new Object[]{type, Settings.DOC_TYPE_INODE, Settings.DOC_TYPE_DATASET, Settings.DOC_TYPE_PROJECT});
        }
      }
    }
    setCount(elasticHitDTO.getProjects());
    setCount(elasticHitDTO.getDatasets());
    setCount(elasticHitDTO.getInodes());
    return elasticHitDTO;
  }
  
  public ElasticHitDTO buildElasticHits(Integer projectId, String searchTerm, Users user) throws ServiceException,
    ElasticException {
    ElasticHitDTO elasticHitDTO = new ElasticHitDTO();
    elasticHitDTO.setProjects(new ElasticProjectDTO());
    elasticHitDTO.setDatasets(new ElasticDatasetDTO());
    elasticHitDTO.setInodes(new ElasticInodeDTO());
    SearchHit[] elasticHits = elasticController.projectSearchHighLevel(projectId, searchTerm);
    for (SearchHit hit : elasticHits) {
      if (hit.getSourceAsMap().containsKey(Settings.META_DOC_TYPE_FIELD)) {
        String type = (String) hit.getSourceAsMap().get(Settings.META_DOC_TYPE_FIELD);
        switch (type) {
          case Settings.DOC_TYPE_PROJECT:
            buildElasticProjects(hit, elasticHitDTO.getProjects(), user);
            break;
          case Settings.DOC_TYPE_DATASET:
            buildElasticDatasets(hit, elasticHitDTO.getDatasets(), user);
            break;
          case Settings.DOC_TYPE_INODE:
            buildElasticInodes(hit, elasticHitDTO.getInodes());
            break;
          default:
            LOGGER
              .log(Level.WARNING, "Got a wrong document type [{0}] was expecting one of these types [{1}, {2}, {3}]",
                new Object[]{type, Settings.DOC_TYPE_INODE, Settings.DOC_TYPE_DATASET, Settings.DOC_TYPE_PROJECT});
        }
      }
    }
    setCount(elasticHitDTO.getProjects());
    setCount(elasticHitDTO.getDatasets());
    setCount(elasticHitDTO.getInodes());
    return elasticHitDTO;
  }
  
  public ElasticHitDTO buildElasticHits(Integer projectId, String datasetName, String searchTerm, Users user)
    throws ServiceException, ElasticException {
    ElasticHitDTO elasticHitDTO = new ElasticHitDTO();
    elasticHitDTO.setProjects(new ElasticProjectDTO());
    elasticHitDTO.setDatasets(new ElasticDatasetDTO());
    elasticHitDTO.setInodes(new ElasticInodeDTO());
    SearchHit[] elasticHits = elasticController.datasetSearchHighLevel(projectId, datasetName, searchTerm);
    for (SearchHit hit : elasticHits) {
      if (hit.getSourceAsMap().containsKey(Settings.META_DOC_TYPE_FIELD)) {
        String type = (String) hit.getSourceAsMap().get(Settings.META_DOC_TYPE_FIELD);
        switch (type) {
          case Settings.DOC_TYPE_PROJECT:
            buildElasticProjects(hit, elasticHitDTO.getProjects(), user);
            break;
          case Settings.DOC_TYPE_DATASET:
            buildElasticDatasets(hit, elasticHitDTO.getDatasets(), user);
            break;
          case Settings.DOC_TYPE_INODE:
            buildElasticInodes(hit, elasticHitDTO.getInodes());
            break;
          default:
            LOGGER
              .log(Level.WARNING, "Got a wrong document type [{0}] was expecting one of these types [{1}, {2}, {3}]",
                new Object[]{type, Settings.DOC_TYPE_INODE, Settings.DOC_TYPE_DATASET, Settings.DOC_TYPE_PROJECT});
        }
      }
    }
    setCount(elasticHitDTO.getProjects());
    setCount(elasticHitDTO.getDatasets());
    setCount(elasticHitDTO.getInodes());
    return elasticHitDTO;
  }
  
  public void buildElasticProjects (SearchHit hit, ElasticProjectDTO elasticProjectDTO, Users user) {
    ElasticProjectDTO item = new ElasticProjectDTO();
    item.setMap(hit.getSourceAsMap());
    item.setScore(hit.getScore());
    item.setProjectId(getIntValue(hit, "project_id"));
    item.setName(getStringValue(hit, "name"));
    item.setCreator(getStringValue(hit, "user"));
    item.setDescription(getStringValue(hit,"description"));
    item.setHighlights(hit.getHighlightFields());
    if (elasticProjectDTO.getItems() == null) {
      elasticProjectDTO.setItems(new ArrayList<>());
    }
    if (item.getProjectId() != null) {
      Project project = projectFacade.find(item.getProjectId());
      if (project != null) {
        item.setMember(projectTeamFacade.isUserMemberOfProject(project, user));
        item.setProjectIId(project.getInode().getId());
        item.setMembers(getMembers(project));
        item.setCreated(project.getCreated());
      }
      
    }
    elasticProjectDTO.getItems().add(item);
  }
  
  public void buildElasticDatasets (SearchHit hit, ElasticDatasetDTO elasticDatasetDTO, Users user) {
    ElasticDatasetDTO item = new ElasticDatasetDTO();
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
    if (elasticDatasetDTO.getItems() == null) {
      elasticDatasetDTO.setItems(new ArrayList<>());
    }
    if (item.getDatasetIId() != null) {
      Dataset dataset = datasetController.getDatasetByInodeId(item.getDatasetIId());
      if (dataset != null) {
        item.setParentProjectName(dataset.getProject().getName());
        item.setSize(dataset.getInode().getSize());
        item.setDatasetId(dataset.getId());
        item.setPublicDatasetId(dataset.getPublicDsId());
        item.setModificationTime(new Date(dataset.getInode().getModificationTime().longValue()));
        item.setAccessProjects(accessProject(dataset, user));
      }
    }
    item.setCreator(setUserName(item.getCreator()));
    elasticDatasetDTO.getItems().add(item);
  }
  
  public void buildElasticInodes (SearchHit hit, ElasticInodeDTO elasticInodeDTO) {
    ElasticInodeDTO item = new ElasticInodeDTO();
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
    if (elasticInodeDTO.getItems() == null) {
      elasticInodeDTO.setItems(new ArrayList<>());
    }
    if (item.getParentDatasetIId() != null) {
      Dataset dataset = datasetController.getDatasetByInodeId(item.getParentDatasetIId());
      if (dataset != null) {
        item.setParentDatasetId(dataset.getId());
        item.setParentDatasetName(dataset.getName());
        item.setModificationTime(new Date(dataset.getInode().getModificationTime().longValue()));
      }
    }
    if (item.getInodeId() != null) {
      Inode inode = inodeFacade.findById(item.getInodeId());
      if (inode != null) {
        item.setPath(inodeController.getPath(inode));
      }
    }
    item.setCreator(setUserName(item.getCreator()));
    elasticInodeDTO.getItems().add(item);
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
    for (ProjectTeam member: project.getProjectTeamCollection()) {
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
