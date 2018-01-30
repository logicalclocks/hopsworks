package io.hops.hopsworks.kmon.command;

import io.hops.hopsworks.common.dao.command.CommandEJB;
import io.hops.hopsworks.common.dao.command.Command;
import java.util.List;
import java.util.logging.Logger;
import javax.annotation.PostConstruct;
import javax.ejb.EJB;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.ManagedProperty;
import javax.faces.bean.RequestScoped;

@ManagedBean
@RequestScoped
public class ProgressController {

  @EJB
  private CommandEJB commandEJB;
  @ManagedProperty("#{param.hostname}")
  private String hostname;
  @ManagedProperty("#{param.service}")
  private String service;
  @ManagedProperty("#{param.group}")
  private String group;
  @ManagedProperty("#{param.cluster}")
  private String cluster;
  private static final Logger logger = Logger.getLogger(
          ProgressController.class.getName());
  private List<Command> commands;

  public ProgressController() {
  }

  @PostConstruct
  public void init() {
    logger.info("init CommandProgressController");
//        setCommands(getLatestCommandByClusterServiceInstanceHost());
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

  public List<Command> getLatestCommandByClusterServiceInstanceHost() {
    return commandEJB.findLatestByClusterServiceRoleHostname(cluster, group, service, hostname);
  }

  public List<Command> getCommands() {
    return commands;
  }

  public void setCommands(List<Command> commands) {
    this.commands = commands;
  }

}
