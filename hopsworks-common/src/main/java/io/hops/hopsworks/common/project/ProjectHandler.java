/*
 * This file is part of Hopsworks
 * Copyright (C) 2018, Logical Clocks AB. All rights reserved
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

package io.hops.hopsworks.common.project;

import io.hops.hopsworks.exceptions.ProjectException;
import io.hops.hopsworks.persistence.entity.project.Project;
import io.hops.hopsworks.restutils.RESTCodes;

import javax.enterprise.inject.Instance;
import java.util.logging.Level;

public interface ProjectHandler {
  void preCreate(Project project) throws Exception;
  void postCreate(Project project) throws Exception;

  void preDelete(Project project) throws Exception;
  void postDelete(Project project) throws Exception;

  String getClassName();
  
  static void runProjectPreCreateHandlers(Instance<ProjectHandler> projectHandlers, Project project)
      throws ProjectException {
    for (ProjectHandler projectHandler : projectHandlers) {
      try {
        projectHandler.preCreate(project);
      } catch (Exception e) {
        throw new ProjectException(RESTCodes.ProjectErrorCode.PROJECT_HANDLER_PRECREATE_ERROR, Level.SEVERE,
          e.getMessage(), "project: " + project.getName() + ", handler: " + projectHandler.getClassName(), e);
      }
    }
  }
  
  static void runProjectPostCreateHandlers(Instance<ProjectHandler> projectHandlers, Project project)
      throws ProjectException {
    for (ProjectHandler projectHandler : projectHandlers) {
      try {
        projectHandler.postCreate(project);
      } catch (Exception e) {
        throw new ProjectException(RESTCodes.ProjectErrorCode.PROJECT_HANDLER_POSTCREATE_ERROR, Level.SEVERE,
          e.getMessage(), "project: " + project.getName() + ", handler: " + projectHandler.getClassName(), e);
      }
    }
  }
  
  static void runProjectPreDeleteHandlers(Instance<ProjectHandler> projectHandlers, Project project)
      throws ProjectException {
    for (ProjectHandler projectHandler : projectHandlers) {
      try {
        projectHandler.preDelete(project);
      } catch (Exception e) {
        throw new ProjectException(RESTCodes.ProjectErrorCode.PROJECT_HANDLER_PREDELETE_ERROR, Level.SEVERE,
          "project: " + project.getName() + ", handler: " + projectHandler.getClassName(), e.getMessage(), e);
      }
    }
  }
  
  static void runProjectPostDeleteHandlers(Instance<ProjectHandler> projectHandlers, Project project)
      throws ProjectException {
    for (ProjectHandler projectHandler : projectHandlers) {
      try {
        projectHandler.postDelete(project);
      } catch (Exception e) {
        throw new ProjectException(RESTCodes.ProjectErrorCode.PROJECT_HANDLER_POSTDELETE_ERROR, Level.SEVERE,
          "project: " + project.getName() + ", handler: " + projectHandler.getClassName(), e.getMessage(), e);
      }
    }
  }
}
