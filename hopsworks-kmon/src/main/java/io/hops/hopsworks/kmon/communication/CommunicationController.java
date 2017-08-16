package io.hops.hopsworks.kmon.communication;

import io.hops.hopsworks.common.util.WebCommunication;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;
import javax.annotation.PostConstruct;
import javax.ejb.EJB;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.ManagedProperty;
import javax.faces.bean.RequestScoped;
import io.hops.hopsworks.common.dao.host.Host;
import io.hops.hopsworks.common.dao.host.HostEJB;
import io.hops.hopsworks.common.dao.role.RoleEJB;
import io.hops.hopsworks.common.util.NodesTableItem;
import io.hops.hopsworks.kmon.service.ServiceInstancesController;
import io.hops.hopsworks.kmon.struct.InstanceInfo;
import java.util.concurrent.Future;
import java.util.logging.Level;
import javax.faces.application.FacesMessage;
import javax.faces.context.FacesContext;

@ManagedBean
@RequestScoped
public class CommunicationController {

  @EJB
  private HostEJB hostEJB;
  @EJB
  private RoleEJB roleEjb;
  @EJB
  private WebCommunication web;
  
  @ManagedProperty(value="#{serviceInstancesController}")
  private ServiceInstancesController serviceInstancesController;
  
  @ManagedProperty("#{param.hostid}")
  private String hostId;
  @ManagedProperty("#{param.role}")
  private String role;
  @ManagedProperty("#{param.service}")
  private String service;
  @ManagedProperty("#{param.cluster}")
  private String cluster;

  private List<InstanceInfo> instances;
  
  private static final Logger logger = Logger.getLogger(
          CommunicationController.class.getName());

  public CommunicationController() {
    logger.info("CommunicationController");
  }

  @PostConstruct
  public void init() {
  }

  public String getRole() {
    return role;
  }

  public void setRole(String role) {
    this.role = role;
  }

  public String getService() {
    return service;
  }

  public void setService(String service) {
    this.service = service;
  }

  public void setCluster(String cluster) {
    this.cluster = cluster;
  }

  public String getCluster() {
    return cluster;
  }

  public String getHostId() {
    return hostId;
  }

  public void setHostId(String hostId) {
    this.hostId = hostId;
  }

  public void setServiceInstancesController(ServiceInstancesController serviceInstancesController) {
    this.serviceInstancesController = serviceInstancesController;
  }

  private Host findHostById(String hostId) throws Exception {
    try {
      Host host = hostEJB.findByHostId(hostId);
      return host;
    } catch (Exception ex) {
      throw new RuntimeException("HostId " + hostId + " not found.");
    }
  }

  private Host findHostByRole(String cluster, String service, String role)
          throws Exception {
    String id = roleEjb.findRoles(cluster, service, role).get(0).getHostId();
    return findHostById(id);
  }


  public String mySqlClusterConfig() throws Exception {
    // Finds hostId of mgmserver
    // Role=mgmserver , Service=MySQLCluster, Cluster=cluster
    String mgmserverRole = "ndb_mgmd";
    Host h = findHostByRole(cluster, service, mgmserverRole);
    String ip = h.getPublicOrPrivateIp();
    String agentPassword = h.getAgentPassword();
    return web.getConfig(ip, agentPassword, cluster, service, mgmserverRole);
  }

  public String getRoleLog(int lines) {
    try {
      Host h = findHostById(hostId);
      String ip = h.getPublicOrPrivateIp();
      String agentPassword = h.getAgentPassword();
      return web.getRoleLog(ip, agentPassword, cluster, service, role, lines);
    } catch (Exception ex) {
      return ex.getMessage();
    }
  }

  private void uiMsg(String res) {
    FacesContext context = FacesContext.getCurrentInstance();
    FacesMessage msg = null;
    if (res.contains("Error")) {
      msg = new FacesMessage(FacesMessage.SEVERITY_ERROR, res,
              "There was a problem when executing the operation.");
    } else {
      msg = new FacesMessage(FacesMessage.SEVERITY_INFO, res,
              "Successfully executed the operation.");
    }
    context.addMessage(null, msg);
  }

  public void roleStart() {
    uiMsg(roleOperation("startRole"));

  }

  public void roleStartAll() {
    uiMsg(roleOperationAll("startRole"));
  }
  
  public void roleRestart() {
    uiMsg(roleOperation("restartRole"));
  }

  public void roleRestartAll() {
    uiMsg(roleOperationAll("restartRole"));
  }
  
  public void roleStop() {
    uiMsg(roleOperation("stopRole"));
  }

  public void roleStopAll() {
    logger.log(Level.SEVERE, "roleStopAll 1");
    uiMsg(roleOperationAll("stopRole"));
  }
  
  private String roleOperationAll(String operation) {
    instances = serviceInstancesController.getInstances();
    List<Future<String>> results = new ArrayList<>();
    String result = "";
    for (InstanceInfo instance : instances) {
      if (instance.getRole().equals(role)) {
        try {
          Host h = findHostById(instance.getHost());
          String ip = h.getPublicOrPrivateIp();
          String agentPassword = h.getAgentPassword();
          results.add(web.asyncRoleOp(operation, ip, agentPassword, cluster, service, role));
        } catch (Exception ex) {
          result = result + ex.getMessage() + "\n";
        }
      }
    }
    for(Future<String> r: results){
      try {
        result = result + r.get() + "\n";
      } catch (Exception ex) {
        result = result + ex.getMessage() + "\n";
      }
    }
    return result;
  }
  
  private String roleOperation(String operation) {
    try {
      Host h = findHostById(hostId);
      String ip = h.getPublicOrPrivateIp();
      String agentPassword = h.getAgentPassword();
      return web.roleOp(operation, ip, agentPassword, cluster, service, role);
    } catch (Exception ex) {
      return ex.getMessage();
    }
  }

  public String getAgentLog(int lines) {
    try {
      Host h = findHostById(hostId);
      String ip = h.getPublicOrPrivateIp();
      String agentPassword = h.getAgentPassword();
      return web.getAgentLog(ip, agentPassword, lines);
    } catch (Exception ex) {
      return ex.getMessage();
    }
  }

  public List<NodesTableItem> getNdbinfoNodesTable() throws Exception {

    // Finds host of mysqld
    // Role=mysqld , Service=MySQLCluster, Cluster=cluster
    final String ROLE = "mysqld";
    List<NodesTableItem> results;
    try {
      String id = roleEjb.findRoles(cluster, service, ROLE).get(0).getHostId();
      Host h = findHostById(hostId);
      String ip = h.getPublicOrPrivateIp();
      String agentPassword = h.getAgentPassword();
      results = web.getNdbinfoNodesTable(ip, agentPassword);
    } catch (Exception ex) {
      results = new ArrayList<>();
    }
    return results;
  }

}
