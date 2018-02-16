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
import javax.faces.application.FacesMessage;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.ManagedProperty;
import javax.faces.bean.RequestScoped;
import javax.faces.context.FacesContext;
import io.hops.hopsworks.common.dao.command.Command;
import io.hops.hopsworks.common.dao.command.CommandEJB;
import io.hops.hopsworks.common.util.WebCommunication;
import io.hops.hopsworks.common.dao.host.Status;
import io.hops.hopsworks.common.dao.kagent.HostServices;
import io.hops.hopsworks.common.dao.kagent.HostServicesFacade;
import java.io.Reader;
import java.util.concurrent.Callable;
import java.util.concurrent.Future;
import javax.annotation.Resource;
import javax.enterprise.concurrent.ManagedExecutorService;
import javax.json.Json;
import javax.json.JsonObject;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status.Family;

@ManagedBean
@RequestScoped
public class HostController implements Serializable {

  @EJB
  private HostsFacade hostsFacade;
  @EJB
  private HostServicesFacade hostServicesFacade;
  @EJB
  private CommandEJB commandEJB;
  @EJB
  private WebCommunication web;

  @Resource(lookup = "concurrent/kagentExecutorService")
  private ManagedExecutorService executorService;

  @ManagedProperty("#{param.cluster}")
  private String cluster;
  @ManagedProperty("#{param.hostname}")
  private String hostname;
  @ManagedProperty("#{param.command}")
  private String command;
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
    logger.info("init HostController");
    loadHost();
    loadHostServices();
  }

  public void setCommand(String command) {
    this.command = command;
  }

  public String getCommand() {
    return command;
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

  public void doCommand() throws Exception {
    //  TODO: If the web application server crashes, status will remain 'Running'.
    Command c = new Command(command, hostname, group, service, cluster);
    commandEJB.persistCommand(c);
    Hosts h = hostsFacade.findByHostname(hostname);
    FacesContext context = FacesContext.getCurrentInstance();
//    CommandThread r = new CommandThread(context, h.getPublicOrPrivateIp(), c);
//    Thread t = new Thread(r);
//    t.setDaemon(true);
//    t.start();
    runCommands(context, h.getPublicOrPrivateIp(), h.getAgentPassword(), c);
  }

  public List<HostServices> getHostServices() {
    loadHostServices();
    return services;
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
//        ClientResponse response = web.doCommand(hostAddress, cluster, group,
//                service, command);
//
//        Thread.sleep(3000);
//
//        if (response.getClientResponseStatus().getFamily() == Family.SUCCESSFUL) {
//          c.succeeded();
//          String messageText = "";
//          Role r = roleEjb.find(hostname, cluster, group, service);
//
//          if (command.equalsIgnoreCase("start")) {
//            JsonObject json
//                    = Json.createReader(response.getEntityInputStream()).
//                    readObject();
//            messageText = json.getString("msg");
//            r.setStatus(Status.Started);
//
//          } else if (command.equalsIgnoreCase("stop")) {
//            messageText = command + ": " + response.getEntity(String.class);
//            r.setStatus(Status.Stopped);
//          }
//          roleEjb.store(r);
//          message = new FacesMessage(FacesMessage.SEVERITY_INFO, "Success",
//                  messageText);
//
//        } else {
//          c.failed();
//          if (response.getStatus() == 400) {
//            message = new FacesMessage(FacesMessage.SEVERITY_ERROR, "Error",
//                    command + ": " + response.getEntity(String.class));
//          } else {
//            message = new FacesMessage(FacesMessage.SEVERITY_FATAL,
//                    "Server Error", "");
//          }
//        }
//      } catch (Exception e) {
//        c.failed();
//        message = new FacesMessage(FacesMessage.SEVERITY_FATAL,
//                "Communication Error", e.toString());
//      }
//      commandEJB.updateCommand(c);
////            context.addMessage(null, message);
//    }
//  }
  public void runCommands(FacesContext context, String hostAddress,
          String agentPassword, Command c) {
    CommandTask commandTask = new CommandTask(context, hostAddress,
            agentPassword, c);
    Future<Command> future = executorService.submit(commandTask);
  }

  class CommandTask implements Callable<Command> {

    private final Logger logger = Logger.getLogger(getClass().getSimpleName());

    private final FacesContext context;
    private final String hostAddress;
    private final String agentPassword;
    private final Command c;

    public CommandTask(FacesContext context, String hostAddress,
            String agentPassword, Command c) {
      this.context = context;
      this.hostAddress = hostAddress;
      this.agentPassword = agentPassword;
      this.c = c;
    }

    @Override
    public Command call() {
      FacesMessage message;
      try {
        Response response = web.doCommand(hostAddress, agentPassword,
                cluster, group, service, command);

        Thread.sleep(3000);
        int code = response.getStatus();
        Family res = Response.Status.Family.familyOf(code);
        if (res == Response.Status.Family.SUCCESSFUL) {
          c.succeeded();
          String messageText = "";
          HostServices hs = hostServicesFacade.find(hostname, cluster, group, service);

          if (command.equalsIgnoreCase("start")) {
            JsonObject json
                    = Json.createReader(response.readEntity(Reader.class)).
                    readObject();
            messageText = json.getString("msg");
            hs.setStatus(Status.Started);

          } else if (command.equalsIgnoreCase("stop")) {
            messageText = command + ": " + response.readEntity(String.class);
            hs.setStatus(Status.Stopped);
          }
          hostServicesFacade.store(hs);
          message = new FacesMessage(FacesMessage.SEVERITY_INFO, "Success",
                  messageText);

        } else {
          c.failed();
          if (response.getStatus() == 400) {
            message = new FacesMessage(FacesMessage.SEVERITY_ERROR, "Error",
                    command + ": " + response.readEntity(String.class));
          } else {
            message = new FacesMessage(FacesMessage.SEVERITY_FATAL,
                    "Server Error", "");
          }
        }
      } catch (Exception e) {
        c.failed();
        message = new FacesMessage(FacesMessage.SEVERITY_FATAL,
                "Communication Error", e.toString());
      }
      commandEJB.updateCommand(c);
      return c;
    }
  }

}
