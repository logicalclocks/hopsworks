/*
 * This file is part of Hopsworks
 * Copyright (C) 2023, Hopsworks AB. All rights reserved
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

import io.hops.hopsworks.alert.AMClient;
import io.hops.hopsworks.exceptions.ProjectException;
import io.hops.hopsworks.persistence.entity.user.Users;
import io.hops.hopsworks.restutils.RESTCodes;
import org.junit.Test;
import org.mockito.Mockito;

import java.util.logging.Level;

public class TestProjectController {

  @Test
  public void testSendAlertProjectCreationFail() throws Exception {
    String exceptionUsrMsg = "This is the RESTException user error message";
    String exceptionDevMsg = "This is the RESTException dev error message";
    ProjectController projectController = Mockito.spy(new ProjectController());
    Mockito.doThrow(
        new ProjectException(
            RESTCodes.ProjectErrorCode.PROJECT_FOLDER_NOT_CREATED, Level.SEVERE, exceptionUsrMsg, exceptionDevMsg))
        .when(projectController).createProjectInternal(Mockito.any(), Mockito.any());
    AMClient alertManager = Mockito.mock(AMClient.class);
    Mockito.doNothing().when(alertManager).asyncPostAlerts(Mockito.any());

    projectController.setAlertManager(alertManager);

    Users owner = new Users(1);
    ProjectDTO projectDTO = new ProjectDTO();
    projectDTO.setProjectName("test");

    try {
      projectController.createProject(projectDTO, owner);
    } catch (ProjectException e) {
      // Exception is expected here;
    }

    String message =
        RESTCodes.ProjectErrorCode.PROJECT_FOLDER_NOT_CREATED.getMessage() + " ErrorCode: " + RESTCodes.ProjectErrorCode.PROJECT_FOLDER_NOT_CREATED.getCode()
        + " User msg: " + exceptionUsrMsg + " Dev msg: " + exceptionDevMsg;
    Mockito.verify(projectController, Mockito.times(1)).sendProjectCreationFailAlert(
        owner, "test", message
    );
  }

  @Test
  public void testSkipAlertIfProjectExists() throws Exception {
    String exceptionUsrMsg = "This is the RESTException user error message";
    String exceptionDevMsg = "This is the RESTException dev error message";

    ProjectController projectController = Mockito.spy(new ProjectController());
    Mockito.doThrow(
            new ProjectException(
                RESTCodes.ProjectErrorCode.PROJECT_EXISTS, Level.FINE, exceptionUsrMsg, exceptionUsrMsg))
        .when(projectController).createProjectInternal(Mockito.any(), Mockito.any());
    AMClient alertManager = Mockito.mock(AMClient.class);
    Mockito.doNothing().when(alertManager).asyncPostAlerts(Mockito.any());

    projectController.setAlertManager(alertManager);

    Users owner = new Users(1);
    ProjectDTO projectDTO = new ProjectDTO();
    projectDTO.setProjectName("test");

    try {
      projectController.createProject(projectDTO, owner);
    } catch (ProjectException e) {
      // Exception is expected here;
    }

    String message =
        RESTCodes.ProjectErrorCode.PROJECT_EXISTS.getMessage() + " ErrorCode: " + RESTCodes.ProjectErrorCode.PROJECT_EXISTS.getCode()
            + " User msg: " + exceptionUsrMsg + " Dev msg: " + exceptionDevMsg;

    Mockito.verify(projectController, Mockito.never()).sendProjectCreationFailAlert(
        owner, "test", message
    );
  }

  @Test
  public void testSendAlertIfProjectExistsAndLevelNotFine() throws Exception {
    String exceptionUsrMsg = "This is the RESTException user error message project exists";
    String exceptionDevMsg = "This is the RESTException dev error message project exists";

    ProjectController projectController = Mockito.spy(new ProjectController());
    Mockito.doThrow(
            new ProjectException(
                RESTCodes.ProjectErrorCode.PROJECT_EXISTS, Level.SEVERE, exceptionUsrMsg, exceptionDevMsg))
        .when(projectController).createProjectInternal(Mockito.any(), Mockito.any());
    AMClient alertManager = Mockito.mock(AMClient.class);
    Mockito.doNothing().when(alertManager).asyncPostAlerts(Mockito.any());

    projectController.setAlertManager(alertManager);

    Users owner = new Users(1);
    ProjectDTO projectDTO = new ProjectDTO();
    projectDTO.setProjectName("test");

    try {
      projectController.createProject(projectDTO, owner);
    } catch (ProjectException e) {
      // Exception is expected here;
    }

    String message =
        RESTCodes.ProjectErrorCode.PROJECT_EXISTS.getMessage() + " ErrorCode: " + RESTCodes.ProjectErrorCode.PROJECT_EXISTS.getCode()
            + " User msg: " + exceptionUsrMsg + " Dev msg: " + exceptionDevMsg;

    Mockito.verify(projectController, Mockito.times(1)).sendProjectCreationFailAlert(
        owner, "test", message
    );
  }
}
