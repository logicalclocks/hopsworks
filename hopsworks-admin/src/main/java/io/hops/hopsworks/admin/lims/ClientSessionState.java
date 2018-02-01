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

package io.hops.hopsworks.admin.lims;

import java.io.Serializable;
import javax.ejb.EJB;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.SessionScoped;
import javax.faces.context.FacesContext;
import javax.servlet.http.HttpServletRequest;
import io.hops.hopsworks.common.dao.project.Project;
import io.hops.hopsworks.common.dao.project.ProjectFacade;
import io.hops.hopsworks.common.dao.user.UserFacade;
import io.hops.hopsworks.common.dao.user.Users;
import java.io.IOException;
import javax.faces.context.ExternalContext;

@ManagedBean
@SessionScoped
public class ClientSessionState implements Serializable {

  @EJB
  private ProjectFacade projectFacade;

  @EJB
  private UserFacade userFacade;

  private Project activeProject;

  private Users user;

  public void setActiveProject(Project project) {
    this.activeProject = project;
  }

  public Project getActiveProject() {
    return activeProject;
  }

  public String getActiveProjectname() {
    if (activeProject != null) {
      return activeProject.getName();
    } else {
      return null;
    }
  }

  public void setActiveProjectByUserAndName(Users user, String projectname) {
    activeProject = projectFacade.findByNameAndOwner(projectname, user);
  }

  public HttpServletRequest getRequest() {
    return (HttpServletRequest) FacesContext.getCurrentInstance().
            getExternalContext().getRequest();
  }

  /**
   * Get the username of the user currently logged in.
   * <p/>
   * @return Email address of the user currently logged in. (Serves as
   * username.)
   */
  public String getLoggedInUsername() {
    return getRequest().getUserPrincipal().getName();
  }

  public Users getLoggedInUser() {
    if (user == null) {
      String email = getRequest().getUserPrincipal().getName();
      user = userFacade.findByEmail(email);
    }
    return user;
  }

  public void redirect() throws IOException {
    ExternalContext externalContext = FacesContext.getCurrentInstance().
            getExternalContext();
    externalContext.redirect("/hopsworks-kmon/monitor/clusters.xhtml");
  }

}
