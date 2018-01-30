package io.hops.hopsworks.kmon.group;

import io.hops.hopsworks.common.dao.kagent.HostServicesFacade;
import java.util.logging.Logger;
import javax.annotation.PostConstruct;
import javax.ejb.EJB;
import javax.faces.application.FacesMessage;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.ManagedProperty;
import javax.faces.bean.RequestScoped;
import javax.faces.context.FacesContext;

@ManagedBean
@RequestScoped
public class ServiceController {

  @ManagedProperty("#{param.hostname}")
  private String hostname;
  @ManagedProperty("#{param.service}")
  private String service;
  @ManagedProperty("#{param.group}")
  private String group;
  @ManagedProperty("#{param.cluster}")
  private String cluster;
  @ManagedProperty("#{param.status}")
  private String status;
  @EJB
  private HostServicesFacade hostServicesFacade;

  private static final Logger logger = Logger.getLogger(ServiceController.class.
          getName());

  public ServiceController() {

  }

  @PostConstruct
  public void init() {
    logger.info("ServiceController: status: " + status + " ; cluster: " + cluster + "; group: " + group
        + " ; service: " + service + " ; hostname: " + hostname);
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

  public void setStatus(String status) {
    this.status = status;
  }

  public String getStatus() {
    return status;
  }

  public boolean isServiceFound() {
    return hostServicesFacade.countServices(cluster, group) > 0;
  }

  public void addMessage(String summary) {
    FacesMessage message = new FacesMessage(FacesMessage.SEVERITY_INFO, summary, null);
    FacesContext.getCurrentInstance().addMessage(null, message);
  }

  public void startService() {
    addMessage("Start not implemented!");
  }

  public void stopService() {
    addMessage("Stop not implemented!");
  }

  public void restartService() {
    addMessage("Restart not implemented!");
  }

  public void deleteService() {
    addMessage("Delete not implemented!");
  }

}
