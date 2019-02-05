/*
 * Changes to this file committed after and not including commit-id: ccc0d2c5f9a5ac661e60e6eaf138de7889928b8b
 * are released under the following license:
 *
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
 *
 * Changes to this file committed before and including commit-id: ccc0d2c5f9a5ac661e60e6eaf138de7889928b8b
 * are released under the following license:
 *
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
 */
package io.hops.hopsworks.admin.project;

import io.hops.hopsworks.admin.maintenance.MessagesController;
import io.hops.hopsworks.common.dao.project.PaymentType;
import io.hops.hopsworks.common.dao.project.Project;
import io.hops.hopsworks.common.dao.project.ProjectFacade;
import io.hops.hopsworks.common.exception.GenericException;
import io.hops.hopsworks.common.exception.ProjectException;
import io.hops.hopsworks.common.project.ProjectController;
import org.primefaces.component.datatable.DataTable;
import org.primefaces.context.RequestContext;
import org.primefaces.event.RowEditEvent;
import org.primefaces.event.SelectEvent;

import javax.annotation.PostConstruct;
import javax.ejb.EJB;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.ViewScoped;
import javax.faces.context.FacesContext;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

@ManagedBean(name = "projectsmanagement")
@ViewScoped
public class ProjectsManagementBean implements Serializable {
  private static final long serialVersionUID = -1L;

  @EJB
  private ProjectFacade projectFacade;
  @EJB
  private ProjectController projectController;

  private List<ProjectQuotas> projectsQuotas = null;
  private List<ProjectQuotas> filteredProjectsQuotas = null;
  private ProjectQuotas projectQuotasSelected = null;
  private String projectNameForceCleanup;
  private List<PaymentType> paymentTypes = null;
  private Map<String, Object> dialogOptions;

  private static Logger logger = Logger.getLogger(ProjectsManagementBean.class.toString());

  @PostConstruct
  public void init() {
    List<Project> projects = projectFacade.findAll();
    projectsQuotas = new ArrayList<>();
    for (Project project : projects) {
      projectsQuotas.add(new ProjectQuotas(project, projectController.getQuotasInternal(project)));
    }

    paymentTypes = new ArrayList<>();
    for (PaymentType paymentType : PaymentType.values()) {
      paymentTypes.add(paymentType);
    }

    dialogOptions = new HashMap<>(3);
    dialogOptions.put("resizable", false);
    dialogOptions.put("draggable", false);
    dialogOptions.put("modal", true);
  }

  public List<ProjectQuotas> getProjectsQuotas() { return projectsQuotas; }

  public void setProjectQuotas(List<ProjectQuotas> projectsQuotas) {
    this.projectsQuotas = projectsQuotas;
  }

  public List<ProjectQuotas> getFilteredProjectsQuotas () { return filteredProjectsQuotas; }

  public void setFilteredProjectsQuotas(List<ProjectQuotas> filteredProjectsQuotas) {
    this.filteredProjectsQuotas = filteredProjectsQuotas;
  }

  public List<PaymentType> getPaymentTypes() { return paymentTypes; }

  public ProjectQuotas getProjectQuotasSelected() {
    return projectQuotasSelected;
  }

  public void setProjectQuotasSelected(ProjectQuotas projectQuotasSelected) {
    this.projectQuotasSelected = projectQuotasSelected;
  }

  public void onRowEdit(RowEditEvent event) {
    ProjectQuotas pQuotas = (ProjectQuotas) event.getObject();
    try {
      projectController.adminProjectUpdate(pQuotas.getProject(), pQuotas.getNormalizedQuotas());
    } catch (ProjectException e) {
      logger.log(Level.SEVERE, "Error updating the quota for the project: " + pQuotas.getName(),
          e);
      MessagesController.addErrorMessage("Error updating the project, " +
            "check the logs for more information");
    }

    // Update the timestamp in the table
    Date timestamp = new Date();
    pQuotas.setLastQuotaUpdate(timestamp);
    int rowIndex = ((DataTable)event.getSource()).getRowIndex();
    RequestContext.getCurrentInstance().update("projectmodifyForm:projectsTable:" + rowIndex
        + ":lastQuotaUpdateColumn");

    logger.log(Level.INFO, "Quotas successfully updated for project: " + pQuotas.getName());
  }

  public void deleteProject() {
    logger.log(Level.INFO, "Deleting project: " + projectQuotasSelected.getName());
    Cookie[] cookies = ((HttpServletRequest) FacesContext.getCurrentInstance().getExternalContext().getRequest())
        .getCookies();
    try {
      String sessionId = "";
      for (Cookie cookie : cookies) {
        if (cookie.getName().equalsIgnoreCase("SESSION")) {
          sessionId = cookie.getValue();
          break;
        }
      }

      projectController.removeProject(projectQuotasSelected.getOwnerEmail(),
          projectQuotasSelected.getId(), sessionId);

      projectsQuotas.remove(projectQuotasSelected);
      projectQuotasSelected = null;
      MessagesController.addInfoMessage("Project deleted!");
    } catch (ProjectException | GenericException ex) {
      logger.log(Level.SEVERE, "Failed to delete project " + projectQuotasSelected.getName(), ex);
      MessagesController.addErrorMessage("Deletion failed", "Failed deleting project "
          + projectQuotasSelected.getName());
    }
  }

  public void dialogForceCleanup() {
    RequestContext.getCurrentInstance().openDialog("projectForceRemoveDialog", dialogOptions, null);
  }

  public String getProjectNameForceCleanup() {
    return projectNameForceCleanup;
  }

  public void setProjectNameForceCleanup(String projectNameForceCleanup) {
    this.projectNameForceCleanup = projectNameForceCleanup;
  }

  public void selectedProjectForceCleanup() {
    RequestContext.getCurrentInstance().closeDialog(projectNameForceCleanup);
  }
  
  public void onProjectForceCleanupChosen(SelectEvent event) {
    String projectName = (String) event.getObject();
    logger.log(Level.INFO, "Project force cleanup: " + projectName);
    String userEmail = FacesContext.getCurrentInstance().getExternalContext()
        .getUserPrincipal().getName();
    MessagesController.addInfoMessage("Force project deletion", "Check Inbox for status");
    projectController.forceCleanup(projectName, userEmail, getSessionId());
  }
  
  private String getSessionId() {
    Cookie[] cookies = ((HttpServletRequest) FacesContext.getCurrentInstance().getExternalContext().getRequest())
        .getCookies();
    String sessionId = "";
    for (Cookie cookie : cookies) {
      if (cookie.getName().equalsIgnoreCase("SESSION")) {
        sessionId = cookie.getValue();
        break;
      }
    }
    return sessionId;
  }

  public void onRowCancel(RowEditEvent event) { }

}
