/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 * <p/>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p/>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
 * express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */
package io.hops.hopsworks.admin.jupyter;

import io.hops.hopsworks.common.dao.hdfsUser.HdfsUsers;
import io.hops.hopsworks.common.dao.hdfsUser.HdfsUsersFacade;
import io.hops.hopsworks.common.dao.jupyter.JupyterProject;
import io.hops.hopsworks.common.dao.jupyter.JupyterSettingsFacade;
import io.hops.hopsworks.common.dao.jupyter.config.JupyterFacade;
import io.hops.hopsworks.common.dao.jupyter.config.JupyterProcessMgr;
import io.hops.hopsworks.common.exception.AppException;
import io.hops.hopsworks.common.util.Settings;

import javax.ejb.EJB;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.ViewScoped;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.faces.application.FacesMessage;
import javax.faces.context.FacesContext;

@ManagedBean(name = "JupyterNotebooks")
@ViewScoped
public class JupyterNotebooksBean {

  private static final Logger LOGGER = Logger.getLogger(JupyterNotebooksBean.class.getName());

  @EJB
  private JupyterFacade jupyterFacade;
  @EJB
  private JupyterSettingsFacade jupyterSettingsFacade;
  @EJB
  private JupyterProcessMgr jupyterProcessFacade;
  @EJB
  private HdfsUsersFacade hdfsUsersFacade;
  @EJB
  private Settings settings;

  public String action;

  private List<JupyterProject> filteredNotebooks;

  private List<JupyterProject> allNotebooks;

  public void setFilteredNotebooks(List<JupyterProject> filteredNotebooks) {
    this.filteredNotebooks = filteredNotebooks;
  }

  public List<JupyterProject> getFilteredNotebooks() {
    return filteredNotebooks;
  }

  public void setAllNotebooks(List<JupyterProject> allNotebooks) {
    this.allNotebooks = allNotebooks;
  }

  public List<JupyterProject> getAllNotebooks() {
    this.allNotebooks = jupyterProcessFacade.getAllNotebooks();
    return this.allNotebooks;
  }

  public String getAction() {
    return action;
  }

  public void setAction(String action) {
    this.action = action;
  }

  public String getHdfsUser(JupyterProject notebook) {
    int hdfsId = notebook.getHdfsUserId();
    if (hdfsId == -1) {
      return "Orphaned";
    }
    HdfsUsers hdfsUser = hdfsUsersFacade.find(hdfsId);
    return hdfsUser.getName();
  }

  public String kill(JupyterProject notebook) {
    String jupyterHomePath;
    String hdfsUser = getHdfsUser(notebook);
    try {
      if (hdfsUser.compareTo("Orphaned") == 0) {
        jupyterHomePath = "";
      } else {
        jupyterHomePath = jupyterProcessFacade.getJupyterHome(hdfsUser, notebook);
      }
      jupyterProcessFacade.killServerJupyterUser(hdfsUser, jupyterHomePath, notebook.getPid(), notebook.getPort());
      FacesContext context = FacesContext.getCurrentInstance();
      context.addMessage(null, new FacesMessage("Successful", "Successfully killed Jupyter Notebook Server."));
    } catch (AppException ex) {
      Logger.getLogger(JupyterNotebooksBean.class.getName()).log(Level.SEVERE, null, ex);
      FacesContext context = FacesContext.getCurrentInstance();
      context.addMessage(null, new FacesMessage("Failure", "Failed to kill Jupyter Notebook Server."));
      return "KILL_NOTEBOOK_FAILED";
    }
    return "KILL_NOTEBOOK_SUCCESS";
  }

}
