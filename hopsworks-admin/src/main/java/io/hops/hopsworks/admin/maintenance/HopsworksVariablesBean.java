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
