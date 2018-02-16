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
