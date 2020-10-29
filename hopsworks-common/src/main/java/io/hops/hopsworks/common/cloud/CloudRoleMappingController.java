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
package io.hops.hopsworks.common.cloud;

import io.hops.hopsworks.common.constants.auth.AllowedRoles;
import io.hops.hopsworks.common.dao.cloud.CloudRoleMappingFacade;
import io.hops.hopsworks.common.dao.project.team.ProjectTeamFacade;
import io.hops.hopsworks.exceptions.CloudException;
import io.hops.hopsworks.persistence.entity.cloud.CloudRoleMapping;
import io.hops.hopsworks.persistence.entity.cloud.ProjectRoles;
import io.hops.hopsworks.persistence.entity.project.Project;
import io.hops.hopsworks.persistence.entity.user.Users;
import io.hops.hopsworks.restutils.RESTCodes;

import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import java.util.logging.Level;
import java.util.logging.Logger;

@Stateless
@TransactionAttribute(TransactionAttributeType.NEVER)
public class CloudRoleMappingController {
  private static final Logger LOGGER = Logger.getLogger(CloudRoleMappingController.class.getName());
  @EJB
  private CloudRoleMappingFacade cloudRoleMappingFacade;
  @EJB
  private ProjectTeamFacade projectTeamFacade;

  /**
   * Get the default role mapping for user in project
   * @param project
   * @param user
   * @return
   */
  public CloudRoleMapping getDefault(Project project, Users user) {
    String role = projectTeamFacade.findCurrentRole(project, user);
    CloudRoleMapping defaultCloudRoleMappings =
      cloudRoleMappingFacade.findDefaultForRole(project, ProjectRoles.fromDisplayName(role));
    if (defaultCloudRoleMappings == null) {
      defaultCloudRoleMappings = cloudRoleMappingFacade.findDefaultForRole(project, ProjectRoles.ALL);
    }
    return defaultCloudRoleMappings;
  }

  /**
   * Get role mapping for project and cloud role if it exists otherwise returns the default mapping
   * @param cloudRole
   * @param project
   * @param user
   * @return
   */
  public CloudRoleMapping getRoleMappingOrDefault(String cloudRole, Project project, Users user) throws CloudException {
    CloudRoleMapping cloudRoleMapping;
    String msg = "No mapping found.";
    if (cloudRole == null || cloudRole.isEmpty()) {
      cloudRoleMapping = getDefault(project, user);
      msg = "No default mapping found.";
    } else {
      cloudRoleMapping = cloudRoleMappingFacade.findByProjectAndCloudRole(project, cloudRole);
    }
    if (cloudRoleMapping == null) {
      throw new CloudException(RESTCodes.CloudErrorCode.MAPPING_NOT_FOUND, Level.FINE, msg);
    }
    return cloudRoleMapping;
  }

  /**
   * Set mapping as default
   * @param cloudRoleMapping
   * @param defaultRole
   */
  public void setDefault(CloudRoleMapping cloudRoleMapping, boolean defaultRole) {
    CloudRoleMapping defaultCloudRoleMappings =
      cloudRoleMappingFacade.findDefaultForRole(cloudRoleMapping.getProjectId(),
        ProjectRoles.fromDisplayName(cloudRoleMapping.getProjectRole()));
    if (defaultCloudRoleMappings != null && !defaultCloudRoleMappings.equals(cloudRoleMapping) && defaultRole) {
      cloudRoleMappingFacade.changeDefault(defaultCloudRoleMappings, cloudRoleMapping);
    } else if (cloudRoleMapping.isDefaultRole() != defaultRole) {
      cloudRoleMappingFacade.setDefault(cloudRoleMapping, defaultRole);
    }
  }

  /**
   * Set mapping as default
   * @param id
   * @param defaultRole
   * @return
   */
  public CloudRoleMapping setDefault(Integer id, Project project, boolean defaultRole) throws CloudException {
    CloudRoleMapping cloudRoleMapping = cloudRoleMappingFacade.findByIdAndProject(id, project);
    if (cloudRoleMapping == null) {
      throw new CloudException(RESTCodes.CloudErrorCode.MAPPING_NOT_FOUND, Level.FINE);
    }
    setDefault(cloudRoleMapping, defaultRole);
    return cloudRoleMapping;
  }

  /**
   * Add a new mapping
   * @param project
   * @param cloudRole
   * @param projectRole
   * @param defaultRole
   * @return the newly created mapping
   * @throws CloudException if a mapping for the project and cloud role already exists
   */
  public CloudRoleMapping saveMapping(Project project, String cloudRole, ProjectRoles projectRole, boolean defaultRole)
    throws CloudException {
    CloudRoleMapping cloudRoleMapping = cloudRoleMappingFacade.findByProjectAndCloudRole(project, cloudRole);
    if (cloudRoleMapping != null) {
      throw new CloudException(RESTCodes.CloudErrorCode.MAPPING_ALREADY_EXISTS, Level.FINE);
    }
    cloudRoleMapping = new CloudRoleMapping(project, projectRole.getDisplayName(), cloudRole);
    cloudRoleMappingFacade.save(cloudRoleMapping);
    if (defaultRole) {
      setDefault(cloudRoleMapping, true);
    }
    return cloudRoleMapping;
  }

  /**
   * Check if there is a mapping that will allow the user to assume the cloud role
   * @param user
   * @param cloudRoleMapping
   * @return
   */
  public boolean canAssumeRole(Users user, CloudRoleMapping cloudRoleMapping) {
    return isUserInRole(user, cloudRoleMapping.getProjectId(), cloudRoleMapping.getProjectRole());
  }

  /**
   *
   * @param id
   * @param cloudRole
   * @param projectRole
   * @param defaultRole
   * @return
   * @throws CloudException
   */
  public CloudRoleMapping update(Integer id, String cloudRole, ProjectRoles projectRole, Boolean defaultRole)
    throws CloudException {
    CloudRoleMapping cloudRoleMapping = cloudRoleMappingFacade.find(id);
    if (cloudRoleMapping == null) {
      throw new CloudException(RESTCodes.CloudErrorCode.MAPPING_NOT_FOUND, Level.FINE);
    }
    if (cloudRole != null && !cloudRole.isEmpty()) {
      cloudRoleMapping.setCloudRole(cloudRole);
    }
    if (projectRole != null) {
      cloudRoleMapping.setProjectRole(projectRole.getDisplayName());
    }
    if (defaultRole == null) {
      defaultRole = cloudRoleMapping.isDefaultRole();
    }
    return update(cloudRoleMapping, defaultRole);
  }

  /**
   *
   * @param mappingToUpdate
   * @param defaultRole
   * @return
   * @throws CloudException
   */
  public CloudRoleMapping update(CloudRoleMapping mappingToUpdate, boolean defaultRole) throws CloudException {
    CloudRoleMapping cloudRoleMappingManaged = cloudRoleMappingFacade.find(mappingToUpdate.getId());
    if (cloudRoleMappingManaged == null) {
      throw new CloudException(RESTCodes.CloudErrorCode.MAPPING_NOT_FOUND, Level.FINE);
    }
    if (!cloudRoleMappingManaged.equals(mappingToUpdate)) {
      CloudRoleMapping existingMapping =
        cloudRoleMappingFacade.findByProjectAndCloudRole(mappingToUpdate.getProjectId(),
          mappingToUpdate.getCloudRole());
      if (existingMapping != null && !existingMapping.getId().equals(mappingToUpdate.getId())) {
        throw new CloudException(RESTCodes.CloudErrorCode.MAPPING_ALREADY_EXISTS, Level.FINE,
          "Could not create mapping. Mapping already exists.");
      }
      if (mappingToUpdate.isDefaultRole() && defaultRole) {
        CloudRoleMapping cloudRoleMappingDefault =
          cloudRoleMappingFacade.findDefaultForRole(mappingToUpdate.getProjectId(),
            ProjectRoles.fromDisplayName(mappingToUpdate.getProjectRole()));
        if (cloudRoleMappingDefault != null && !cloudRoleMappingDefault.getId().equals(mappingToUpdate.getId())) {
          throw new CloudException(RESTCodes.CloudErrorCode.MAPPING_ALREADY_EXISTS, Level.FINE,
            "A default mapping for the given project and project role already exists.");
        }
      }
      mappingToUpdate = cloudRoleMappingFacade.update(mappingToUpdate);
    }
    if (mappingToUpdate.isDefaultRole() != defaultRole) {
      setDefault(mappingToUpdate, defaultRole);
      mappingToUpdate = cloudRoleMappingFacade.find(mappingToUpdate.getId());
    }
    return mappingToUpdate;
  }

  private boolean isUserInRole(Users user, Project project, String role) {
    String userRole = projectTeamFacade.findCurrentRole(project, user);
    return userRole != null && (AllowedRoles.ALL.equals(role) || userRole.equals(role));
  }
}
