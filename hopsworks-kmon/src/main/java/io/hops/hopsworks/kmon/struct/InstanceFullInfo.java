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
