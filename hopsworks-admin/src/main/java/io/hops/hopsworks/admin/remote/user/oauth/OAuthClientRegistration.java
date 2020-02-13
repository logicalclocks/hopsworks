/*
 * This file is part of Hopsworks
 * Copyright (C) 2019, Logical Clocks AB. All rights reserved
 *
 * Hopsworks is free software: you can redistribute it and/or modify it under the terms of
 * the GNU Affero General Public License as published by the Free Software Foundation,
 * either version 3 of the License, or (at your option) any later version.
 *
 * Hopsworks is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR
 * PURPOSE.  See the GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License along with this program.
 * If not, see <https://www.gnu.org/licenses/>.
 */
package io.hops.hopsworks.admin.remote.user.oauth;

import io.hops.hopsworks.admin.maintenance.MessagesController;
import io.hops.hopsworks.common.dao.remote.oauth.OauthClientFacade;
import io.hops.hopsworks.common.remote.OAuthHelper;
import io.hops.hopsworks.common.remote.OpenIdProviderConfig;
import io.hops.hopsworks.common.util.Settings;
import io.hops.hopsworks.persistence.entity.remote.oauth.OauthClient;
import org.primefaces.context.RequestContext;
import org.primefaces.event.RowEditEvent;

import javax.annotation.PostConstruct;
import javax.ejb.EJB;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.ViewScoped;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import java.io.IOException;
import java.io.Serializable;
import java.net.URISyntaxException;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

@ManagedBean
@ViewScoped
public class OAuthClientRegistration implements Serializable {
  
  private final Logger LOGGER = Logger.getLogger(OAuthClientRegistration.class.getName());
  
  private String clientId;
  private String clientSecret;
  private String providerLogoURI;
  private String providerURI;
  private String providerName;
  private String providerDisplayName;
  private String authorisationEndpoint;
  private String tokenEndpoint;
  private String userInfoEndpoint;
  private String jwksURI;
  private boolean providerMetadataEndpointSupported;
  private List<OauthClient> oauthClients;
  private String autoProviderURI;
  private OpenIdProviderConfig openIdProviderConfig;
  private boolean registrationDisabled;
  private boolean oauthEnabled;
  
  @EJB
  private OauthClientFacade oauthClientFacade;
  @EJB
  private Settings settings;
  
  private OAuthHelper oAuthHelper;
  
  @PostConstruct
  public void init() {
    this.clientId = "";
    this.clientSecret = "";
    this.providerLogoURI = "";
    this.providerURI = "";
    this.providerName = "";
    this.providerDisplayName = "";
    this.providerMetadataEndpointSupported = true;
    this.authorisationEndpoint = "";
    this.tokenEndpoint = "";
    this.userInfoEndpoint = "";
    this.jwksURI = "";
    this.autoProviderURI = "";
    this.oauthClients = oauthClientFacade.findAll();
    this.oauthEnabled = settings.isOAuthEnabled();
    initOAuthHelper();
  }
  
  public String getClientId() {
    return clientId;
  }
  
  public void setClientId(String clientId) {
    this.clientId = clientId;
  }
  
  public String getClientSecret() {
    return clientSecret;
  }
  
  public void setClientSecret(String clientSecret) {
    this.clientSecret = clientSecret;
  }
  
  public String getProviderLogoURI() {
    return providerLogoURI;
  }
  
  public void setProviderLogoURI(String providerLogoURI) {
    this.providerLogoURI = providerLogoURI;
  }
  
  public String getProviderURI() {
    return providerURI;
  }
  
  public void setProviderURI(String providerURI) {
    this.providerURI = providerURI;
  }
  
  public String getProviderName() {
    return providerName;
  }
  
  public void setProviderName(String providerName) {
    this.providerName = providerName;
  }
  
  public String getProviderDisplayName() {
    return providerDisplayName;
  }
  
  public void setProviderDisplayName(String providerDisplayName) {
    this.providerDisplayName = providerDisplayName;
  }
  
  public List<OauthClient> getOauthClients() {
    return oauthClients;
  }
  
  public void setOauthClients(List<OauthClient> oauthClients) {
    this.oauthClients = oauthClients;
  }
  
  public String getAuthorisationEndpoint() {
    return authorisationEndpoint;
  }
  
  public void setAuthorisationEndpoint(String authorisationEndpoint) {
    this.authorisationEndpoint = authorisationEndpoint;
  }
  
  public String getTokenEndpoint() {
    return tokenEndpoint;
  }
  
  public void setTokenEndpoint(String tokenEndpoint) {
    this.tokenEndpoint = tokenEndpoint;
  }
  
  public String getUserInfoEndpoint() {
    return userInfoEndpoint;
  }
  
  public void setUserInfoEndpoint(String userInfoEndpoint) {
    this.userInfoEndpoint = userInfoEndpoint;
  }
  
  public String getJwksURI() {
    return jwksURI;
  }
  
  public void setJwksURI(String jwksURI) {
    this.jwksURI = jwksURI;
  }
  
  public boolean isProviderMetadataEndpointSupported() {
    return providerMetadataEndpointSupported;
  }
  
  public void setProviderMetadataEndpointSupported(boolean providerMetadataEndpointSupported) {
    this.providerMetadataEndpointSupported = providerMetadataEndpointSupported;
  }
  
  public boolean isRegistrationDisabled() {
    return registrationDisabled;
  }
  
  public void setRegistrationDisabled(boolean registrationDisabled) {
    this.registrationDisabled = registrationDisabled;
  }
  
  private boolean registrationDisabled() {
    String regEndpoint = this.getOpenIdProviderConfig() != null?
      this.getOpenIdProviderConfig().getRegistrationEndpointURI() : null;
    return regEndpoint == null || regEndpoint.isEmpty();
  }
  
  public String getAutoProviderURI() {
    return autoProviderURI;
  }
  
  public void setAutoProviderURI(String autoProviderURI) {
    this.autoProviderURI = autoProviderURI;
  }
  
  public boolean isOauthEnabled() {
    return oauthEnabled;
  }
  
  public void setOauthEnabled(boolean oauthEnabled) {
    this.oauthEnabled = oauthEnabled;
  }
  
  private void initOAuthHelper() {
    String moduleName = null;
    String fullModuleName = null;
    try {
      String applicationName = InitialContext.doLookup("java:app/AppName");
      moduleName = InitialContext.doLookup("java:module/ModuleName");
      moduleName = moduleName.replace("admin", "remote-user-auth");
      fullModuleName = "java:global/" + applicationName + "/" + moduleName + "/OAuthHelperImpl";
      oAuthHelper = InitialContext.doLookup(fullModuleName);
    } catch (NamingException ex) {
      oAuthHelper = null;
    }
  }
  
  public void fetchOpenIdProviderConfig() {
    if (!checkOAuthHelper() || autoProviderURI == null || autoProviderURI.isEmpty()) {
      return;
    }
    try {
      this.setOpenIdProviderConfig(oAuthHelper.getOpenIdProviderConfiguration(autoProviderURI));
      if (this.getAuthorisationEndpoint() != null) {
        setRegistrationDisabled(registrationDisabled());
        RequestContext context = RequestContext.getCurrentInstance();
        context.execute("PF('dlg2').show();");
      }
    } catch (IOException | URISyntaxException e) {
      LOGGER.log(Level.WARNING,"Failed to get provider configuration. {0}", e.getMessage());
      MessagesController.addErrorMessage("Failed to get provider configuration.", getRootCause(e));
    }
  }
  
  public void registerClient() {
    if (!checkOAuthHelper()) {
      return;
    }
    try {
      oAuthHelper.registerClient(openIdProviderConfig);
    } catch (IOException | URISyntaxException e) {
      LOGGER.log(Level.WARNING,"Failed to register client. {0}", e.getMessage());
      MessagesController.addErrorMessage(e.getMessage(), getRootCause(e));
    }
  }
  
  private boolean checkOAuthHelper() {
    if (oAuthHelper == null) {
      MessagesController.addErrorMessage("Not supported", "Remote auth is not available.");
      return false;
    }
    return true;
  }
  
  public OpenIdProviderConfig getOpenIdProviderConfig() {
    return openIdProviderConfig;
  }
  
  public void setOpenIdProviderConfig(OpenIdProviderConfig openIdProviderConfig) {
    this.openIdProviderConfig = openIdProviderConfig;
  }
  
  public void saveClient() {
    if (!providerMetadataEndpointSupported && (authorisationEndpoint == null || authorisationEndpoint.isEmpty() ||
      tokenEndpoint == null || tokenEndpoint.isEmpty() || userInfoEndpoint == null || userInfoEndpoint.isEmpty() ||
      jwksURI == null || jwksURI.isEmpty())) {
      LOGGER.log(Level.WARNING,"Failed to create client. Required field/s missing.");
      MessagesController.addErrorMessage("Failed to create client.", "Missing required field/s.");
      return;
    }
    try {
      OauthClient oauthClient = new OauthClient(this.clientId, this.clientSecret, this.providerURI, this.providerName,
        this.providerLogoURI, this.providerDisplayName, this.providerMetadataEndpointSupported,
        this.authorisationEndpoint, this.tokenEndpoint, this.userInfoEndpoint, this.jwksURI);
      oauthClientFacade.save(oauthClient);
      MessagesController.addInfoMessage("Added new OAuth server.");
      init();
    } catch (Exception e) {
      LOGGER.log(Level.WARNING,"Failed to create client. {0}", e.getMessage());
      MessagesController.addErrorMessage(e.getMessage(), getRootCause(e));
    }
  }
  
  public void onRowEdit(RowEditEvent event) {
    OauthClient oauthClient = (OauthClient) event.getObject();
    try {
      oauthClientFacade.update(oauthClient);
      MessagesController.addInfoMessage("Updated OAuth server.");
    } catch (Exception e) {
      MessagesController.addErrorMessage(e.getMessage(), getRootCause(e));
    }
  }
  
  public void delete(OauthClient oauthClient) {
    try {
      oauthClientFacade.remove(oauthClient);
      MessagesController.addInfoMessage("Deleted OAuth server.");
      init();
    } catch (Exception e) {
      MessagesController.addErrorMessage(e.getMessage(), getRootCause(e));
    }
  }
  
  private String getRootCause(Exception e) {
    Throwable t = e.getCause();
    if (t == null) {
      return e.getMessage();
    }
    while (t.getCause() != null) {
      t = t.getCause();
    }
    return t.getMessage();
  }
  
}
