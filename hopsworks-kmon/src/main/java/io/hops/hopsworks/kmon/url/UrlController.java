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

package io.hops.hopsworks.kmon.url;

import java.util.logging.Logger;
import javax.faces.application.NavigationHandler;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.ManagedProperty;
import javax.faces.bean.RequestScoped;
import javax.faces.context.FacesContext;

@ManagedBean
@RequestScoped
public class UrlController {

  @ManagedProperty("#{param.hostname}")
  private String hostname;
  @ManagedProperty("#{param.service}")
  private String service;
  @ManagedProperty("#{param.group}")
  private String group;
  @ManagedProperty("#{param.cluster}")
  private String cluster;
  @ManagedProperty("#{param.status}")
  private String status;
  @ManagedProperty("#{param.target}")
  private String target;
  private static final Logger logger = Logger.getLogger(UrlController.class.getName());

  public UrlController() {
    logger.info("UrlController - hostname: " + hostname + " ; cluster: " + cluster + "; group: " + group
        + " ; service: " + service + " ; status: " + status + " ; target: " + target);
  }

  public String getService() {
    return service;
  }

  public void setService(String service) {
    this.service = service;
  }

  public String getGroup() {
    return group;
  }

  public void setGroup(String group) {
    this.group = group;
  }

  public String getHostname() {
    return hostname;
  }

  public void setHostname(String hostname) {
    this.hostname = hostname;
  }

  public void setCluster(String cluster) {
    this.cluster = cluster;
  }

  public String getCluster() {
    return cluster;
  }

  public void setStatus(String status) {
    this.status = status;
  }

  public String getStatus() {
    return status;
  }

  public String getTarget() {
    return target;
  }

  public void setTarget(String target) {
    this.target = target;
  }

  public String host() {
    return "host?faces-redirect=true&hostname=" + hostname;
  }

  public String clustersStatus(){
    return "clusters?faces-redirect=true";
  }
  
  public String clusterStatus() {
    return "cluster-status?faces-redirect=true&cluster=" + cluster;
  }

  public String clusterStatus(String cluster) {
    return "cluster-status?faces-redirect=true&cluster=" + cluster;
  }
  
  public String groupInstance() {
    return "services-instances-status?faces-redirect=true&hostname=" 
        + hostname + "&cluster=" + cluster + "&group=" + group;
  }

  public String clusterActionHistory() {
    return "cluster-actionhistory?faces-redirect=true&cluster=" + cluster;
  }

  public String groupStatus() {
    return "group-status?faces-redirect=true&cluster=" + cluster + "&group=" + group;
  }

  public String groupInstances() {
    String url = "service-instances?faces-redirect=true";
    if (hostname != null) {
      url += "&hostname=" + hostname;
    }
    if (cluster != null) {
      url += "&cluster=" + cluster;
    }
    if (group != null) {
      url += "&group=" + group;
    }
    if (service != null) {
      url += "&service=" + service;
    }
    if (status != null) {
      url += "&status=" + status;
    }
    return url;
  }

  public String groupActionHistory() {
    return "group-actionhistory?faces-redirect=true&cluster=" + cluster + "&group=" + group;
  }

  public String groupTerminal() {
    return "group-terminal?faces-redirect=true&cluster=" + cluster + "&group=" + group;
  }

  public String serviceStatus() {
    return "service-status?faces-redirect=true&hostname=" + hostname + "&cluster="
            + cluster + "&group=" + group + "&service=" + service;
  }

  public String serviceActionHistory() {
    return "service-actionhistory?faces-redirect=true&hostname=" + hostname
            + "&cluster=" + cluster + "&group=" + group + "&service=" + service;
  }

  public void redirectToEditGraphs() {
    String outcome = "edit-graphs?faces-redirect=true";
    if (target != null) {
      outcome += "&target=" + target;
    }
    FacesContext context = FacesContext.getCurrentInstance();
    NavigationHandler navigationHandler = context.getApplication().
            getNavigationHandler();
    navigationHandler.handleNavigation(context, null, outcome);
  }
}
