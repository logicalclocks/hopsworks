/*
 * Copyright (C) 2020, Logical Clocks AB. All rights reserved
 */
package io.hops.hopsworks.remote.user;

import io.hops.hopsworks.common.constants.auth.AllowedRoles;
import io.hops.hopsworks.common.dao.remote.group.RemoteGroupProjectMappingFacade;
import io.hops.hopsworks.common.dao.user.BbcGroupFacade;
import io.hops.hopsworks.common.project.ProjectController;
import io.hops.hopsworks.common.util.Settings;
import io.hops.hopsworks.exceptions.FeaturestoreException;
import io.hops.hopsworks.exceptions.GenericException;
import io.hops.hopsworks.exceptions.HopsSecurityException;
import io.hops.hopsworks.exceptions.JobException;
import io.hops.hopsworks.exceptions.KafkaException;
import io.hops.hopsworks.exceptions.ProjectException;
import io.hops.hopsworks.exceptions.ServiceException;
import io.hops.hopsworks.exceptions.TensorBoardException;
import io.hops.hopsworks.exceptions.UserException;
import io.hops.hopsworks.persistence.entity.project.team.ProjectTeam;
import io.hops.hopsworks.persistence.entity.remote.group.RemoteGroupProjectMapping;
import io.hops.hopsworks.persistence.entity.remote.user.RemoteUserType;
import io.hops.hopsworks.persistence.entity.user.BbcGroup;
import io.hops.hopsworks.persistence.entity.user.Users;

import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;
import java.util.logging.Level;
import java.util.logging.Logger;

@Stateless
@TransactionAttribute(TransactionAttributeType.NEVER)
public class RemoteUserGroupMapper {
  private static final Logger LOGGER = Logger.getLogger(RemoteUserGroupMapper.class.getName());
  private static final String ANY_GROUP = "ANY_GROUP";
  private static final String GROUP_SEPARATOR = ",";
  private static final String MAPPING_SEPARATOR = "->";
  private static final String GROUP_MAPPING_SEPARATOR = ";";
  
  @EJB
  private Settings settings;
  @EJB
  private BbcGroupFacade groupFacade;
  @EJB
  private RemoteGroupProjectMappingFacade remoteGroupProjectMappingFacade;
  @EJB
  private ProjectController projectController;
  
  /**
   * Creates a group mapping from remote user to local
   *
   * @return group mapping
   */
  public Map<String, List<String>> getGroupMappings(String mappingStr) {
    Map<String, List<String>> mappings = new HashMap<>();
    if (mappingStr == null || mappingStr.isEmpty()) {
      return mappings;
    }
    StringTokenizer tokenizer = new StringTokenizer(mappingStr, GROUP_MAPPING_SEPARATOR);
    while (tokenizer.hasMoreElements()) {
      String mapping = tokenizer.nextToken();
      String[] mappingGroups = mapping.split(MAPPING_SEPARATOR);
      String mappedGroup = null;
      String[] mappedToGroups = null;
      if (mappingGroups != null && mappingGroups.length == 2) {
        mappedGroup = mappingGroups[0].trim();
        mappedToGroups = mappingGroups[1].split(GROUP_SEPARATOR);
      }
      if (mappedGroup == null || mappedGroup.isEmpty() || mappedToGroups == null || mappedToGroups.length < 1) {
        continue;
      }
      List<String> mappedTOGroupList = new ArrayList<>();
      for (String grp : mappedToGroups) {
        mappedTOGroupList.add(grp.trim());
      }
      mappings.put(mappedGroup, mappedTOGroupList);
    }
    return mappings;
  }
  
  /**
   * Returns a list of local group names based on the group mapping in getMappingStr()
   *
   * @param userGroups
   * @return
   */
  public List<String> getMappedGroups(List<String> userGroups, RemoteUserType type) {
    List<String> mappedGroups = new ArrayList<>();
    Map<String, List<String>> mappings;
    switch (type) {
      case KRB:
      case LDAP:
        mappings = getGroupMappings(settings.getLdapGroupMapping());
        break;
      case OAUTH2:
        mappings = getGroupMappings(settings.getOAuthGroupMapping());
        break;
      default:
        mappings = getGroupMappings("");
    }
    if (mappings == null || mappings.isEmpty()) {
      return mappedGroups;
    }
    addUnique(mappings.get(ANY_GROUP), mappedGroups);
    if (userGroups == null || userGroups.isEmpty()) {
      return mappedGroups;
    }
    for (String group : userGroups) {
      addUnique(mappings.get(group), mappedGroups);
    }
    return mappedGroups;
  }
  
  public void mapRemoteGroupToProject(Users user, List<String> remoteGroups) {
    if (!settings.isLdapGroupMappingSyncEnabled()) {
      return;
    }
    Collection<RemoteGroupProjectMapping> remoteGroupProjectMappings = getUnique(remoteGroups);
    for (RemoteGroupProjectMapping remoteGroupProjectMapping : remoteGroupProjectMappings) {
      try {
        projectController.addMember(user, remoteGroupProjectMapping.getProjectRole(),
          remoteGroupProjectMapping.getProject());
      } catch (KafkaException | ProjectException | UserException | FeaturestoreException | IOException e) {
        LOGGER.log(Level.WARNING, "Failed to add user: {0} to project: {1} for GroupProjectMapping: {2}. Error: {3}",
          new Object[]{user.getUsername(), remoteGroupProjectMapping.getProject(), remoteGroupProjectMapping.getId(),
            e.getMessage()});
      }
    }
  }
  
  public void cleanRemoteGroupToProjectMapping(Users user, List<String> remoteGroups) {
    if (!settings.isLdapGroupMappingSyncEnabled()) {
      return;
    }
    List<ProjectTeam> projectTeams = projectController.findProjectByUser(user.getEmail());
    Collection<RemoteGroupProjectMapping> remoteGroupProjectMappings = getUnique(remoteGroups);
    for (ProjectTeam projectTeam : projectTeams) {
      boolean exist = false;
      for (RemoteGroupProjectMapping remoteGroupProjectMapping : remoteGroupProjectMappings) {
        if (projectTeam.getProject().equals(remoteGroupProjectMapping.getProject())) {
          if (!projectTeam.getTeamRole().equals(remoteGroupProjectMapping.getProjectRole())) {
            try {
              projectController
                .updateMemberRole(projectTeam.getProject(), user, remoteGroupProjectMapping.getProjectRole());
            } catch (ProjectException | FeaturestoreException | KafkaException | UserException | IOException e) {
              LOGGER.log(Level.WARNING, "Failed to update role for user: {0} in project: {1} for GroupProjectMapping:" +
                " {2}. Error: {3}", new Object[]{user.getUsername(), remoteGroupProjectMapping.getProject(),
                remoteGroupProjectMapping.getId(), e.getMessage()});
            }
          }
          exist = true;
          break;
        }
      }
      if (!exist) {
        removeFromProject(projectTeam);
      }
    }
  }
  
  public void removeFromAllProjects(Users user) {
    if (!settings.isLdapGroupMappingSyncEnabled()) {
      return;
    }
    List<ProjectTeam> projectTeams = projectController.findProjectByUser(user.getEmail());
    for (ProjectTeam projectTeam : projectTeams) {
      removeFromProject(projectTeam);
    }
  }
  
  private void removeFromProject(ProjectTeam projectTeam) {
    try {
      projectController.removeMemberFromTeam(projectTeam.getProject(), projectTeam.getUser());
    } catch (ProjectException | ServiceException | IOException | GenericException | JobException |
      HopsSecurityException | TensorBoardException | FeaturestoreException e) {
      LOGGER.log(Level.WARNING, "Failed to remove user: {0} from project: {1} Error: {2}",
        new Object[]{projectTeam.getUser().getUsername(), projectTeam.getProject(), e.getMessage()});
    }
  }
  
  private Collection<RemoteGroupProjectMapping> getUnique(List<String> remoteGroups) {
    HashMap<String, RemoteGroupProjectMapping> projectToRemoteGroupProjectMappings = new HashMap<>();
    for (String group : remoteGroups) {
      List<RemoteGroupProjectMapping> remoteGroupProjectMappingList =
        remoteGroupProjectMappingFacade.findByGroup(group);
      for (RemoteGroupProjectMapping remoteGroupProjectMapping : remoteGroupProjectMappingList) {
        RemoteGroupProjectMapping mapping =
          projectToRemoteGroupProjectMappings.get(remoteGroupProjectMapping.getProject().getName());
        if (mapping == null || remoteGroupProjectMapping.getProjectRole().equals(AllowedRoles.DATA_OWNER)) {
          projectToRemoteGroupProjectMappings
            .put(remoteGroupProjectMapping.getProject().getName(), remoteGroupProjectMapping);
        }
      }
    }
    return projectToRemoteGroupProjectMappings.values();
  }
  
  public void mapRemoteGroupToGroup(Users user, List<String> remoteGroups, RemoteUserType type) {
    List<String> groups = getMappedGroups(remoteGroups, type);
    BbcGroup group;
    for (String grp : groups) {
      group = groupFacade.findByGroupName(grp);
      if (group != null) {
        user.getBbcGroupCollection().add(group);
      }
    }
  }
  
  private void addUnique(List<String> src, List<String> dest) {
    if (src == null) {
      return;
    }
    for (String str : src) {
      if (!dest.contains(str)) {
        dest.add(str);
      }
    }
  }
  
}
