package io.hops.kmon.host;

import com.sun.jersey.api.client.ClientResponse;
import java.io.Serializable;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.annotation.PostConstruct;
import javax.ejb.EJB;
import javax.faces.application.FacesMessage;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.ManagedProperty;
import javax.faces.bean.RequestScoped;
import javax.faces.context.FacesContext;
import io.hops.kmon.command.Command;
import io.hops.kmon.command.CommandEJB;
import io.hops.kmon.communication.WebCommunication;
import io.hops.kmon.role.Role;
import io.hops.kmon.role.RoleEJB;
import io.hops.kmon.struct.Status;
import java.util.concurrent.Callable;
import java.util.concurrent.Future;
import javax.annotation.Resource;
import javax.enterprise.concurrent.ManagedExecutorService;
import javax.json.Json;
import javax.json.JsonObject;
import javax.ws.rs.core.Response;

/**
 *
 * @author Hamidreza Afzali <afzali@kth.se>
 */
@ManagedBean
@RequestScoped
public class HostController implements Serializable {

  @EJB
  private HostEJB hostEJB;
  @EJB
  private RoleEJB roleEjb;
  @EJB
  private CommandEJB commandEJB;
  @EJB
  private WebCommunication web;

  @Resource
  private ManagedExecutorService executorService;

  @ManagedProperty("#{param.cluster}")
  private String cluster;
  @ManagedProperty("#{param.hostid}")
  private String hostId;
  @ManagedProperty("#{param.command}")
  private String command;
  @ManagedProperty("#{param.service}")
  private String service;
  @ManagedProperty("#{param.role}")
  private String role;
  private Host host;
  private boolean found;
  private List<Role> roles;
  private static final Logger logger = Logger.getLogger(HostController.class.getName());

  public HostController() {
  }

  @PostConstruct
  public void init() {
    logger.info("init HostController");
    loadHost();
    loadRoles();
  }

  public void setCommand(String command) {
    this.command = command;
  }

  public String getCommand() {
    return command;
  }

  public void setRole(String role) {
    this.role = role;
  }

  public String getRole() {
    return role;
  }

  public void setHostId(String hostId) {
    this.hostId = hostId;
  }

  public void setService(String service) {
    this.service = service;
  }

  public String getService() {
    return service;
  }

  public String getHostId() {
    return hostId;
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

  public Host getHost() {
    loadHost();
    return host;
  }

  private void loadHost() {
    try {
      host = hostEJB.findByHostId(hostId);
      if (host != null) {
        found = true;
      }
    } catch (Exception ex) {
      logger.log(Level.WARNING, "Host {0} not found.", hostId);
    }
  }

  private void loadRoles() {
    roles = roleEjb.findHostRoles(hostId);
  }

  public void doCommand() throws Exception {
    //  TODO: If the web application server crashes, status will remain 'Running'.
    Command c = new Command(command, hostId, service, role, cluster);
    commandEJB.persistCommand(c);
    Host h = hostEJB.findByHostId(hostId);
    FacesContext context = FacesContext.getCurrentInstance();
//    CommandThread r = new CommandThread(context, h.getPublicOrPrivateIp(), c);
//    Thread t = new Thread(r);
//    t.setDaemon(true);
//    t.start();
    runCommands(context, h.getPublicOrPrivateIp(), h.getAgentPassword(), c);
  }

  public List<Role> getRoles() {
    loadRoles();
    return roles;
  }

//  class CommandThread implements Runnable {//it's not bad, because he does bad thing :D
//
//    public CommandThread(FacesContext context, String hostAddress, Command c) {
//      this.hostAddress = hostAddress;
//      this.c = c;
//      this.context = context;
//    }
//    private final String hostAddress;
//    private final Command c;
//    private final FacesContext context;
//
//    @Override
//    public void run() {
//      FacesMessage message;
//      try {
//        ClientResponse response = web.doCommand(hostAddress, cluster, service, role, command);
//
//        Thread.sleep(3000);
//
//        if (response.getClientResponseStatus().getFamily() == Family.SUCCESSFUL) {
//          c.succeeded();
//          String messageText = "";
//          Role r = roleEjb.find(hostId, cluster, service, role);
//
//          if (command.equalsIgnoreCase("start")) {
//            JsonObject json = Json.createReader(response.getEntityInputStream()).readObject();
//            messageText = json.getString("msg");
//            r.setStatus(Status.Started);
//
//          } else if (command.equalsIgnoreCase("stop")) {
//            messageText = command + ": " + response.getEntity(String.class);
//            r.setStatus(Status.Stopped);
//          }
//          roleEjb.store(r);
//          message = new FacesMessage(FacesMessage.SEVERITY_INFO, "Success", messageText);
//
//        } else {
//          c.failed();
//          if (response.getStatus() == 400) {
//            message = new FacesMessage(FacesMessage.SEVERITY_ERROR, "Error", command + ": " + response.getEntity(String.class));
//          } else {
//            message = new FacesMessage(FacesMessage.SEVERITY_FATAL, "Server Error", "");
//          }
//        }
//      } catch (Exception e) {
//        c.failed();
//        message = new FacesMessage(FacesMessage.SEVERITY_FATAL, "Communication Error", e.toString());
//      }
//      commandEJB.updateCommand(c);
////            context.addMessage(null, message);
//    }
//  }


  public void runCommands(FacesContext context, String hostAddress, String agentPassword, Command c) {
    CommandTask commandTask = new CommandTask(context, hostAddress, agentPassword, c);
    Future<Command> future = executorService.submit(commandTask);
  }
  
  class CommandTask implements Callable<Command> {

    private final Logger logger = Logger.getLogger(getClass().getSimpleName());

    private final FacesContext context;
    private final String hostAddress;
    private final String agentPassword;
    private final Command c;

    public CommandTask(FacesContext context, String hostAddress, String agentPassword, Command c) {
      this.context = context;
      this.hostAddress = hostAddress;
      this.agentPassword = agentPassword;
      this.c = c;
    }

    @Override
    public Command call() {
      FacesMessage message;
      try {
        ClientResponse response = web.doCommand(hostAddress, agentPassword, cluster, service, role, command);

        Thread.sleep(3000);

        if (response.getClientResponseStatus().getFamily() == Response.Status.Family.SUCCESSFUL) {
          c.succeeded();
          String messageText = "";
          Role r = roleEjb.find(hostId, cluster, service, role);

          if (command.equalsIgnoreCase("start")) {
            JsonObject json = Json.createReader(response.getEntityInputStream()).readObject();
            messageText = json.getString("msg");
            r.setStatus(Status.Started);

          } else if (command.equalsIgnoreCase("stop")) {
            messageText = command + ": " + response.getEntity(String.class);
            r.setStatus(Status.Stopped);
          }
          roleEjb.store(r);
          message = new FacesMessage(FacesMessage.SEVERITY_INFO, "Success", messageText);

        } else {
          c.failed();
          if (response.getStatus() == 400) {
            message = new FacesMessage(FacesMessage.SEVERITY_ERROR, "Error", command + ": " + response.getEntity(String.class));
          } else {
            message = new FacesMessage(FacesMessage.SEVERITY_FATAL, "Server Error", "");
          }
        }
      } catch (Exception e) {
        c.failed();
        message = new FacesMessage(FacesMessage.SEVERITY_FATAL, "Communication Error", e.toString());
      }
      commandEJB.updateCommand(c);
      return c;
    }
  }

}
