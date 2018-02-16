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

package io.hops.hopsworks.kmon.service;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;
import javax.annotation.PostConstruct;
import javax.ejb.EJB;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.ManagedProperty;
import javax.faces.bean.RequestScoped;
import io.hops.hopsworks.common.dao.host.Health;
import io.hops.hopsworks.kmon.struct.InstanceFullInfo;
import io.hops.hopsworks.common.dao.host.Status;
import io.hops.hopsworks.common.dao.kagent.HostServicesFacade;
import io.hops.hopsworks.common.dao.kagent.HostServicesInfo;
import io.hops.hopsworks.common.util.FormatUtils;

@ManagedBean
@RequestScoped
public class ServicesController {

  @EJB
  private HostServicesFacade hostServicesFacade;
  @ManagedProperty("#{param.hostname}")
  private String hostname;
  @ManagedProperty("#{param.service}")
  private String service;
  @ManagedProperty("#{param.group}")
  private String group;
  @ManagedProperty("#{param.cluster}")
  private String cluster;
  private List<InstanceFullInfo> instanceInfoList = new ArrayList<>();
  private String health;
//  private boolean renderWebUi;
  private boolean found;
  private static final Logger logger = Logger.getLogger(ServicesController.class.
          getName());

  public ServicesController() {
  }

  @PostConstruct
  public void init() {
  }

  public void loadServices() {

    instanceInfoList.clear();
    logger.info("init ServicesController");
    try {
      HostServicesInfo serviceHost = hostServicesFacade.findHostServices(cluster, group, service,
              hostname);
      String ip = serviceHost.getHost().getPublicOrPrivateIp();
      InstanceFullInfo info = new InstanceFullInfo(serviceHost.getHostServices().
              getCluster(),
              serviceHost.getHostServices().getService(), serviceHost.getHostServices().getService(),
              serviceHost.getHostServices().getHost().getHostname(), ip, 0,
//          roleHost.getGroup().getWebPort(),
              serviceHost.getStatus(), serviceHost.getHealth().toString());
      info.setPid(serviceHost.getHostServices().getPid());
      String upTime = serviceHost.getHealth() == Health.Good ? FormatUtils.time(
              serviceHost.getHostServices().getUptime() * 1000) : "";
      info.setUptime(upTime);
      instanceInfoList.add(info);
//      renderWebUi = roleHost.getGroup().getWebPort() != null && roleHost.
//              getGroup().getWebPort() != 0;
      health = serviceHost.getHealth().toString();
      found = true;
    } catch (Exception ex) {
      logger.warning("init ServicesController: ".concat(ex.getMessage()));
    }

  }

  public String getHealth() {
    return health;
  }

  public String getService() {
    loadServices();
    return service;
  }

  public void setService(String service) {
    this.service = service;
  }

  public String getGroup() {
    loadServices();
    return group;
  }

  public void setGroup(String group) {
    this.group = group;
  }

  public String getHostname() {
    loadServices();
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

  public boolean isFound() {
    return found;
  }

  public void setFound(boolean found) {
    this.found = found;
  }

//  public boolean getRenderWebUi() {
//    return renderWebUi;
//  }

  public List<InstanceFullInfo> getInstanceFullInfo() {
    loadServices();
    return instanceInfoList;
  }

  public String serviceLongName() {
    if (!instanceInfoList.isEmpty()) {
      return instanceInfoList.get(0).getName();
    }
    return null;
  }

  public boolean disableStart() {
    if (!instanceInfoList.isEmpty() && instanceInfoList.get(0).getStatus()
            == Status.Stopped) {
      return false;
    }
    return true;
  }

  public boolean disableStop() {
    if (!instanceInfoList.isEmpty() && instanceInfoList.get(0).getStatus()
            == Status.Started) {
      return false;
    }
    return true;
  }
}
