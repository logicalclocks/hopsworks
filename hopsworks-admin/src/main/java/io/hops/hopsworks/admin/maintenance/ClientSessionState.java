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

package io.hops.hopsworks.admin.maintenance;

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
