/*
 * Copyright (C) 2013 - 2018, Logical Clocks AB and RISE SICS AB. All rights reserved
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this
 * software and associated documentation files (the "Software"), to deal in the Software
 * without restriction, including without limitation the rights to use, copy, modify, merge,
 * publish, distribute, sublicense, and/or sell copies of the Software, and to permit
 * persons to whom the Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or
 * substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS  OR IMPLIED, INCLUDING
 * BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL  THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM,
 * DAMAGES OR  OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 *
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
