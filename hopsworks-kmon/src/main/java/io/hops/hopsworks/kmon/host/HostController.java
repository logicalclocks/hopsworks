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

package io.hops.hopsworks.kmon.host;

import io.hops.hopsworks.common.dao.host.HostsFacade;
import io.hops.hopsworks.common.dao.host.Hosts;
import java.io.Serializable;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.annotation.PostConstruct;
import javax.ejb.EJB;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.ManagedProperty;
import javax.faces.bean.RequestScoped;
import io.hops.hopsworks.common.dao.kagent.HostServices;
import io.hops.hopsworks.common.dao.kagent.HostServicesFacade;

@ManagedBean
@RequestScoped
public class HostController implements Serializable {

  @EJB
  private HostsFacade hostsFacade;
  @EJB
  private HostServicesFacade hostServicesFacade;

  @ManagedProperty("#{param.cluster}")
  private String cluster;
  @ManagedProperty("#{param.hostname}")
  private String hostname;
  @ManagedProperty("#{param.group}")
  private String group;
  @ManagedProperty("#{param.service}")
  private String service;
  private Hosts host;
  private boolean found;
  private List<HostServices> services;
  private static final Logger logger = Logger.getLogger(HostController.class.
          getName());

  public HostController() {
  }

  @PostConstruct
  public void init() {
    logger.log(Level.FINE, "init HostController");
    loadHost();
    loadHostServices();
  }

  public void setService(String service) {
    this.service = service;
  }

  public String getService() {
    return service;
  }

  public String getHostname() {
    return hostname;
  }

  public void setHostname(String hostname) {
    this.hostname = hostname;
  }

  public void setGroup(String group) {
    this.group = group;
  }

  public String getGroup() {
    return group;
  }

  public void setCluster(String cluster) {
    this.cluster = cluster;
  }

  public String getCluster() {
    return cluster;
  }

  public boolean isFound() {
    return found;
  }

  public void setFound(boolean found) {
    this.found = found;
  }

  public Hosts getHost() {
    loadHost();
    return host;
  }

  private void loadHost() {
    try {
      host = hostsFacade.findByHostname(hostname);
      if (host != null) {
        found = true;
      }
    } catch (Exception ex) {
      logger.log(Level.WARNING, "Host {0} not found.", hostname);
    }
  }

  private void loadHostServices() {
    services = hostServicesFacade.findHostServiceByHostname(hostname);
  }

  public List<HostServices> getHostServices() {
    loadHostServices();
    return services;
  }
}
