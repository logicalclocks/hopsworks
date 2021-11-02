/*
 * Copyright (C) 2021, Logical Clocks AB. All rights reserved
 */

package io.hops.hopsworks.common.project;

import io.hops.hopsworks.exceptions.ProjectException;
import io.hops.hopsworks.persistence.entity.project.Project;
import io.hops.hopsworks.persistence.entity.project.team.ProjectRoleTypes;
import io.hops.hopsworks.persistence.entity.user.Users;
import io.hops.hopsworks.restutils.RESTCodes;

import javax.enterprise.inject.Instance;
import java.util.List;
import java.util.logging.Level;

public interface ProjectTeamRoleHandler {
  void addMembers(Project project, List<Users> members, ProjectRoleTypes teamRole, boolean serviceUsers)
    throws Exception;
  void updateMembers(Project project, List<Users> members, ProjectRoleTypes teamRole) throws Exception;
  void removeMembers(Project project, List<Users> members) throws Exception;
  
  String getClassName();
  
  static void runProjectTeamRoleAddMembersHandlers(Instance<ProjectTeamRoleHandler> projectTeamRoleHandlers,
    Project project, List<Users> members, ProjectRoleTypes teamRole, boolean serviceUsers) throws ProjectException {
    for (ProjectTeamRoleHandler handler : projectTeamRoleHandlers) {
      try {
        handler.addMembers(project, members, teamRole, serviceUsers);
      } catch (Exception e) {
        throw new ProjectException(RESTCodes.ProjectErrorCode.PROJECT_TEAM_ROLE_HANDLER_ADD_MEMBER_ERROR,
          Level.SEVERE, e.getMessage(), "project: " + project.getId() + ", handler: " + handler.getClassName(), e);
      }
    }
  }
  
  static void runProjectTeamRoleUpdateMembersHandlers(Instance<ProjectTeamRoleHandler> projectTeamRoleHandlers,
    Project project, List<Users> members, ProjectRoleTypes teamRole) throws ProjectException {
    for (ProjectTeamRoleHandler handler : projectTeamRoleHandlers) {
      try {
        handler.updateMembers(project, members, teamRole);
      } catch (Exception e) {
        throw new ProjectException(RESTCodes.ProjectErrorCode.PROJECT_TEAM_ROLE_HANDLER_UPDATE_MEMBERS_ERROR,
          Level.SEVERE, e.getMessage(),
          "project: " + project.getId() + ", project team role: " + teamRole.getRole() + ", handler: " +
            handler.getClassName(), e);
      }
    }
  }
  
  static void runProjectTeamRoleRemoveMembersHandlers(Instance<ProjectTeamRoleHandler> projectTeamRoleHandlers,
    Project project, List<Users> members) throws ProjectException {
    for (ProjectTeamRoleHandler handler : projectTeamRoleHandlers) {
      try {
        handler.removeMembers(project, members);
      } catch (Exception e) {
        throw new ProjectException(RESTCodes.ProjectErrorCode.PROJECT_TEAM_ROLE_HANDLER_REMOVE_MEMBER_ERROR,
          Level.SEVERE, e.getMessage(), "project: " + project.getId() + ", handler: " + handler.getClassName(), e);
      }
    }
  }
}
