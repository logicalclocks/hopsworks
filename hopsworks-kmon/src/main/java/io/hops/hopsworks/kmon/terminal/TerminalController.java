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
