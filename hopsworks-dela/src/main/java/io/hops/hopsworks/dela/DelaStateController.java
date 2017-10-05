package io.hops.hopsworks.dela;

import io.hops.hopsworks.common.util.Settings;
import io.hops.hopsworks.dela.exception.ThirdPartyException;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.annotation.PostConstruct;
import javax.ejb.EJB;
import javax.ejb.Singleton;
import javax.ejb.Startup;
import javax.ws.rs.core.Response;

@Startup
@Singleton
public class DelaStateController {

  private final static Logger LOG = Logger.getLogger(DelaStateController.class.getName());

  @EJB
  private Settings settings;

  private boolean delaEnabled = false;
  private boolean delaCertsAvailable = false;
  private boolean transferDelaAvailable = false;
  private boolean hopssiteAvailable = false;

  @PostConstruct
  private void init() {
    if (settings.isDelaEnabled()) {
      delaEnabled = settings.isDelaEnabled();
      LOG.log(Level.INFO, "dela enabled");
    } else {
      LOG.log(Level.INFO, "dela disabled");
    }
  }

  public boolean delaEnabled() {
    return delaEnabled;
  }

  public boolean delaAvailable() {
    return delaEnabled && delaCertsAvailable && transferDelaAvailable && hopssiteAvailable;
  }

  public void checkDelaAvailable() throws ThirdPartyException {
    if (!delaAvailable()) {
      throw new ThirdPartyException(Response.Status.BAD_REQUEST.getStatusCode(), "dela not available",
        ThirdPartyException.Source.LOCAL, "bad request");
    }
  }

  public boolean transferDelaAvailable() {
    return delaEnabled && transferDelaAvailable;
  }

  public boolean hopssiteAvailable() {
    return delaEnabled && delaCertsAvailable && hopssiteAvailable;
  }
  
  public void checkHopssiteState() throws ThirdPartyException {
    if (!hopssiteAvailable()) {
      throw new ThirdPartyException(Response.Status.BAD_REQUEST.getStatusCode(), "hopssite not available", 
        ThirdPartyException.Source.LOCAL, "bad request");
    }
  }

  public boolean remoteDelaAvailable() {
    return delaEnabled && delaCertsAvailable;
  }

  public void checkRemoteDelaAvaileble() throws ThirdPartyException {
    if (!remoteDelaAvailable()) {
      throw new ThirdPartyException(Response.Status.BAD_REQUEST.getStatusCode(), "remote dela not available",
        ThirdPartyException.Source.LOCAL, "bad request");
    }
  }

  public void delaCertsAvailable() {
    delaCertsAvailable = true;
  }

  public void transferDelaContacted() {
    transferDelaAvailable = true;
  }

  public void hopssiteContacted() {
    hopssiteAvailable = true;
  }
}
