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
package io.hops.hopsworks.admin.maintenance;

import io.hops.hopsworks.admin.lims.MessagesController;
import io.hops.hopsworks.common.dao.util.Variables;
import io.hops.hopsworks.common.util.Settings;
import org.primefaces.context.RequestContext;
import org.primefaces.event.RowEditEvent;

import javax.annotation.PostConstruct;
import javax.ejb.EJB;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.ViewScoped;
import java.io.Serializable;
import java.util.List;
import java.util.logging.Logger;

@ManagedBean(name = "hopsworksVariablesBean")
@ViewScoped
public class HopsworksVariablesBean implements Serializable {
  private final Logger LOG = Logger.getLogger(HopsworksVariablesBean.class.getName());
  
  @EJB
  private Settings settings;
  
  private List<Variables> allVariables;
  
  public HopsworksVariablesBean() {
  }
  
  @PostConstruct
  public void init() {
    allVariables = settings.getAllVariables();
  }
  
  public List<Variables> getAllVariables() {
    return allVariables;
  }
  
  public void setAllVariables(List<Variables> allVariables) {
    this.allVariables = allVariables;
  }
  
  public void onRowEdit(RowEditEvent event) {
    String varName = ((Variables) event.getObject()).getId();
    String varValue = ((Variables) event.getObject()).getValue();
    settings.updateVariable(varName, varValue);
    MessagesController.addInfoMessage("Updated variable : " + varName + " to: " + varValue);
  }
  
  public void refreshVariables() {
    settings.refreshCache();
    allVariables = settings.getAllVariables();
    MessagesController.addInfoMessage("Hopsworks variables refreshed");
    RequestContext.getCurrentInstance().update("updateVariablesForm:variablesTable");
  }
}
