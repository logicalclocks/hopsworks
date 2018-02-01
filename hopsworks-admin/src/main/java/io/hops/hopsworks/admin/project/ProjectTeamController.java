/*
 * This file is part of HopsWorks
 *
 * Copyright (C) 2013 - 2018, Logical Clocks AB and RISE SICS AB. All rights reserved.
 *
 * HopsWorks is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * HopsWorks is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with HopsWorks.  If not, see <http://www.gnu.org/licenses/>.
 */

package io.hops.hopsworks.admin.project;

import io.hops.hopsworks.admin.lims.ClientSessionState;
import io.hops.hopsworks.admin.lims.MessagesController;
import io.hops.hopsworks.common.dao.project.team.ProjectTeamFacade;
import java.io.Serializable;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.ejb.EJB;
import javax.ejb.EJBException;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.ManagedProperty;
import javax.faces.bean.ViewScoped;
import io.hops.hopsworks.common.dao.user.activity.ActivityFacade;
import io.hops.hopsworks.common.dao.user.Users;

@ManagedBean
@ViewScoped
public class ProjectTeamController implements Serializable {

  private static final Logger logger = Logger.getLogger(
          ProjectTeamController.class.getName());

  private String toRemoveEmail;
  private String toRemoveName;

  @EJB
  private ProjectTeamFacade teamFacade;

  @EJB
  private ActivityFacade activityFacade;

  @ManagedProperty(value = "#{clientSessionState}")
  private ClientSessionState sessionState;

  public void setToRemove(String email, String name) {
    this.toRemoveEmail = email;
    this.toRemoveName = name;
  }

  public void clearToRemove() {
    this.toRemoveEmail = null;
    this.toRemoveName = null;
  }

  public String getToRemoveEmail() {
    return toRemoveEmail;
  }

  public String getToRemoveName() {
    return toRemoveName;
  }

  public synchronized void deleteMemberFromTeam() {
    try {
      Users user = this.teamFacade.findUserByEmail(toRemoveEmail);
      teamFacade.removeProjectTeam(sessionState.getActiveProject(),
              user);
      activityFacade.persistActivity(ActivityFacade.REMOVED_MEMBER
              + toRemoveEmail, sessionState.getActiveProject(), sessionState.
              getLoggedInUsername());
    } catch (EJBException ejb) {
      MessagesController.addErrorMessage("Deleting team member failed.");
      logger.log(Level.WARNING, "Failed to remove team member " + toRemoveEmail
              + "from project " + sessionState.getActiveProjectname(), ejb);
      return;
    }
    MessagesController.addInfoMessage("Member removed", "Team member "
            + toRemoveEmail
            + " deleted from project " + sessionState.getActiveProjectname());
    clearToRemove();
  }

  public void setSessionState(ClientSessionState sessionState) {
    this.sessionState = sessionState;
  }
}
