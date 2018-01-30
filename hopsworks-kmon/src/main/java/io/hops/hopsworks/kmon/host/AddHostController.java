package io.hops.hopsworks.kmon.host;

import io.hops.hopsworks.common.dao.host.HostsFacade;
import io.hops.hopsworks.common.dao.host.Hosts;
import java.io.Serializable;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.ejb.EJB;
import javax.faces.application.FacesMessage;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.RequestScoped;
import javax.faces.context.FacesContext;
import javax.faces.event.ActionEvent;
import org.primefaces.context.RequestContext;
import io.hops.hopsworks.common.dao.command.CommandEJB;

@ManagedBean
@RequestScoped
public class AddHostController implements Serializable {

  @EJB
  private HostsFacade hostEJB;
  @EJB
  private CommandEJB commandEJB;
  private String hostname;
  private String privateIp;
  private String publicIp;
  private static final Logger logger = Logger.getLogger(AddHostController.class.
          getName());

  public AddHostController() {
    logger.info("AddHostController");
  }

  public void addHost(ActionEvent actionEvent) {
    FacesContext context = FacesContext.getCurrentInstance();
    FacesMessage msg;
    if (hostEJB.hostExists(hostname)) {
      logger.log(Level.INFO, "Host with id {0} already exists.", hostname);
      msg = new FacesMessage(FacesMessage.SEVERITY_ERROR, "Host Exists",
              "A host with id " + hostname + " already exists.");
      context.addMessage(null, msg);
    } else {
      Hosts host = new Hosts();
      host.setHostname(hostname);
      host.setPrivateIp(privateIp);
      host.setPublicIp(publicIp);
      host.setHostIp("");
      hostEJB.storeHost(host, true);
      RequestContext reqInstace = RequestContext.getCurrentInstance();
      reqInstace.addCallbackParam("hostadded", true);
      msg = new FacesMessage(FacesMessage.SEVERITY_INFO, "Host Added",
              "Host " + hostname + " added successfully.");
      context.addMessage(null, msg);
      resetValues();
    }
  }

  private void resetValues() {
    hostname = "";
    privateIp = "";
    publicIp = "";
  }

  public String getHostname() {
    return hostname;
  }

  public void setHostname(String hostname) {
    this.hostname = hostname;
  }

  public String getPrivateIp() {
    return privateIp;
  }

  public void setPrivateIp(String privateIp) {
    this.privateIp = privateIp;
  }

  public String getPublicIp() {
    return publicIp;
  }

  public void setPublicIp(String publicIp) {
    this.publicIp = publicIp;
  }
}
