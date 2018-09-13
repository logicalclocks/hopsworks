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

package io.hops.hopsworks.kmon.terminal;

import io.hops.hopsworks.kmon.struct.GroupType;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.annotation.PostConstruct;
import javax.ejb.EJB;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.ManagedProperty;
import javax.faces.bean.RequestScoped;
import io.hops.hopsworks.common.util.WebCommunication;
import io.hops.hopsworks.common.dao.host.Hosts;
import io.hops.hopsworks.common.dao.host.HostsFacade;
import io.hops.hopsworks.kmon.struct.ServiceType;
import io.hops.hopsworks.common.dao.host.Status;

@ManagedBean
@RequestScoped
public class TerminalController {

  @ManagedProperty("#{param.cluster}")
  private String cluster;
  @ManagedProperty("#{param.service}")
  private String service;
  @ManagedProperty("#{param.group}")
  private String group;
  @EJB
  private HostsFacade hostEjb;
  @EJB
  private WebCommunication web;
  private static final Logger logger = Logger.getLogger(
          TerminalController.class.getName());
  private static final String welcomeMessage;

  static {
    welcomeMessage = ("Welcome to \n"
            + "     __  __               \n"
            + "    / / / /___  ____  _____       \n"
            + "   / /_/ / __ \\/ __ \\/ ___/       \n"
            + "  / __  / /_/ / /_/ (__  )        \n"
            + " /_/ /_/\\____/ .___/____/         \n"
            + "            /_/                   \n"
            + "                                  \n")
            .replace(" ", "&nbsp;")
            .replace("\\", "&#92;")
            .replace("\n", "<br/>");
  }

  public TerminalController() {
  }

  @PostConstruct
  public void init() {
    logger.info("init TerminalController");
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

  public void setCluster(String cluster) {
    this.cluster = cluster;
  }

  public String getCluster() {
    return cluster;
  }

  public String getWelcomeMessage() {
    return welcomeMessage;
  }

  public String handleCommand(String command, String[] params) {
//      TODO: Check special characters like ";" to avoid injection
    String serviceName;
    if (group.equalsIgnoreCase(GroupType.HDFS.toString())) {
      if (command.equals("hdfs")) {
        serviceName = ServiceType.datanode.toString();
      } else {
        return "Unknown command. Accepted commands are: hdfs";
      }

    } else if (group.equalsIgnoreCase(GroupType.NDB.toString())) {
      if (command.equals("mysql")) {
        serviceName = ServiceType.mysqld.toString();
      } else if (command.equals("ndb_mgm")) {
        serviceName = ServiceType.ndb_mgmd.toString();
      } else {
        return "Unknown command. Accepted commands are: mysql, ndb_mgm";
      }
    } else if (group.equalsIgnoreCase(GroupType.YARN.toString())) {
      if (command.equals("yarn")) {
        serviceName = ServiceType.resourcemanager.toString();
      } else {
        return "Unknown command. Accepted commands are: yarn";
      }
    } else {
      return null;
    }
    try {
//          TODO: get only one host
      List<Hosts> hosts = hostEjb.
              find(cluster, group, serviceName, Status.Started);
      if (hosts.isEmpty()) {
        throw new RuntimeException("No live node available.");
      }
      String result = web.executeRun(hosts.get(0).getPublicOrPrivateIp(), hosts.
              get(0).getAgentPassword(),
              cluster, group, serviceName, command, params);
      return result;
    } catch (Exception ex) {
      logger.log(Level.SEVERE, null, ex);
      return "Error: Could not contact a node";
    }
  }
}
