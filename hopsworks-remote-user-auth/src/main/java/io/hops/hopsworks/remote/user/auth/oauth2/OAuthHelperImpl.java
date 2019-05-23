/*
 * Copyright (C) 2019, Logical Clocks AB. All rights reserved
 */
package io.hops.hopsworks.remote.user.auth.oauth2;

import io.hops.hopsworks.common.remote.OAuthHelper;
import io.hops.hopsworks.common.remote.OpenIdProviderConfig;
import io.hops.hopsworks.common.remote.RemoteUserStateDTO;

import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.security.auth.login.LoginException;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.logging.Logger;

@Stateless
@TransactionAttribute(TransactionAttributeType.NEVER)
public class OAuthHelperImpl implements OAuthHelper {
  
  private final static Logger LOGGER = Logger.getLogger(OAuthHelperImpl.class.getName());
  
  @EJB
  private OAuthController oAuthController;
  @EJB
  private OIDAuthorizationCodeFlowHelper oidAuthorizationCodeFlowHelper;
  
  @Override
  public RemoteUserStateDTO login(String code, String state, boolean consent, String chosenEmail)
    throws LoginException {
    return  oAuthController.login(code, state, consent, chosenEmail);
  }
  
  @Override
  public OpenIdProviderConfig getOpenIdProviderConfiguration(String providerURI)
    throws IOException, URISyntaxException {
    return oidAuthorizationCodeFlowHelper.getOpenIdProviderConfig(providerURI);
  }
  
  @Override
  public void registerClient(OpenIdProviderConfig openidConf) throws IOException, URISyntaxException {
    throw new UnsupportedOperationException();
  }
  
  @Override
  public URI getAuthenticationRequestURL(String providerName) throws URISyntaxException {
    return  oidAuthorizationCodeFlowHelper.getAuthenticationRequestURL(providerName);
  }
  
}
