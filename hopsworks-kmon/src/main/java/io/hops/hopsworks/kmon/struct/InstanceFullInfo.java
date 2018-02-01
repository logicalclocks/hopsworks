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

package io.hops.hopsworks.kmon.struct;

import io.hops.hopsworks.common.dao.host.Status;
import java.io.Serializable;
import javax.faces.context.FacesContext;
import javax.servlet.http.HttpServletRequest;

public class InstanceFullInfo implements Serializable {

  private String name;
  private String host;
  private String ip;
  private String cluster;
  private String group;
  private String service;
  private Status status;
  private String health;
  private int pid;
  private String uptime;
  private Integer webPort;

  public InstanceFullInfo(String cluster, String group, String service,
          String host, String ip, Integer webPort, Status status, String health) {

    this.name = service + " @" + host;
    this.host = host;
    this.ip = ip;
    this.cluster = cluster;
    this.service = service;
    this.group = group;
    this.status = status;
    this.health = health;
    this.webPort = webPort;
  }

  public String getName() {
    return name;
  }

  public String getHost() {
    return host;
  }

  public String getIp() {
    return ip;
  }

  public Status getStatus() {
    return status;
  }

  public String getHealth() {
    return health;
  }

  public String getService() {
    return service;
  }

  public void setService(String service) {
    this.service = service;
  }
  
  public int getPid() {
    return pid;
  }

  public void setPid(int pid) {
    this.pid = pid;
  }

  public String getUptime() {
    return uptime;
  }

  public void setUptime(String uptime) {
    this.uptime = uptime;
  }

  public Integer getWebPort() {
    return webPort;
  }

  public void setWebPort(Integer webPort) {
    this.webPort = webPort;
  }

  public String getCluster() {
    return cluster;
  }

  public void setCluster(String cluster) {
    this.cluster = cluster;
  }

  public String getGroup() {
    return group;
  }

  public void setGroup(String service) {
    this.group = service;
  }

  public String getWebUiLink() {
//      String url = "http://" + ip + ":" + webPort;

//  TODO Only if using private IP address        
    String path = ((HttpServletRequest) FacesContext.getCurrentInstance()
            .getExternalContext().getRequest()).getContextPath();
    String url = path + "/rest/web/" + ip + "/" + webPort + "/";
    return url;
  }

  public String getWebUiLabel() {
//      return host + ":" + webPort;
    return ip + ":" + webPort;
  }
}
