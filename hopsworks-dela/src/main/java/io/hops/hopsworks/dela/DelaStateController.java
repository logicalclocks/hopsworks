package io.hops.hopsworks.dela;

import io.hops.hopsworks.common.util.Settings;
import io.hops.hopsworks.dela.exception.ThirdPartyException;
import java.security.KeyStore;
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
  
  private KeyStore keystore;
  private KeyStore truststore;
  private String keystorePassword;
      
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
    return hopsworksDelaSetup() && transferDelaAvailable && hopssiteAvailable;
  }

  public void checkDelaAvailable() throws ThirdPartyException {
    if (!delaAvailable()) {
      throw new ThirdPartyException(Response.Status.BAD_REQUEST.getStatusCode(), "dela not available",
        ThirdPartyException.Source.LOCAL, "bad request");
    }
  }

  public boolean transferDelaAvailable() {
    return hopsworksDelaSetup() && transferDelaAvailable;
  }

  public boolean hopssiteAvailable() {
    return hopsworksDelaSetup() && hopssiteAvailable;
  }
  
  public void checkHopssiteAvailable() throws ThirdPartyException {
    if (!hopssiteAvailable()) {
      throw new ThirdPartyException(Response.Status.BAD_REQUEST.getStatusCode(), "hopssite not available", 
        ThirdPartyException.Source.LOCAL, "bad request");
    }
  }

  public boolean hopsworksDelaSetup() {
    return delaEnabled && delaCertsAvailable;
  }

  public void checkHopsworksDelaSetup() throws ThirdPartyException {
    if (!hopsworksDelaSetup()) {
      throw new ThirdPartyException(Response.Status.BAD_REQUEST.getStatusCode(), "remote dela not available",
        ThirdPartyException.Source.LOCAL, "bad request");
    }
  }

  public void transferDelaContacted() {
    transferDelaAvailable = true;
  }

  public void hopssiteContacted() {
    hopssiteAvailable = true;
  }
  
  public void delaCertsAvailable(KeyStore keystore, KeyStore truststore, String keystorePassword) {
    delaCertsAvailable = true;
    this.keystore = keystore;
    this.truststore = truststore;
    this.keystorePassword = keystorePassword;
  }

  public KeyStore getKeystore() {
    return keystore;
  }

  public KeyStore getTruststore() {
    return truststore;
  }

  public String getKeystorePassword() {
    return keystorePassword;
  }
}
