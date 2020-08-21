/*
 * This file is part of Hopsworks
 * Copyright (C) 2020, Logical Clocks AB. All rights reserved
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
package io.hops.hopsworks.common.remote.oauth;

import io.hops.hopsworks.common.integrations.NullRemoteAuthStereotype;
import io.hops.hopsworks.common.remote.RemoteUserStateDTO;
import io.hops.hopsworks.persistence.entity.remote.oauth.OauthClient;

import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.security.auth.login.LoginException;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;

@NullRemoteAuthStereotype
@Stateless
@TransactionAttribute(TransactionAttributeType.NEVER)
public class NullOAuthHelper implements OAuthHelper {
  @Override
  public boolean oauthAvailable() {
    return false;
  }
  
  @Override
  public RemoteUserStateDTO login(String code, String state, boolean consent, String chosenEmail)
    throws LoginException {
    return null;
  }
  
  @Override
  public OpenIdProviderConfig getOpenIdProviderConfiguration(String providerURI)
    throws IOException, URISyntaxException {
    return null;
  }
  
  @Override
  public URI getAuthenticationRequestURL(String providerName) throws URISyntaxException {
    return null;
  }
  
  @Override
  public void registerClient(OpenIdProviderConfig openidConf) throws URISyntaxException, IOException {
    throw new UnsupportedOperationException("Remote auth not supported.");
  }
  
  @Override
  public void saveClient(OauthClient oauthClient) {
    throw new UnsupportedOperationException("Remote auth not supported.");
  }
  
  @Override
  public void updateClient(OauthClient oauthClient) {
    throw new UnsupportedOperationException("Remote auth not supported.");
  }
  
  @Override
  public void removeClient(OauthClient oauthClient) {
    throw new UnsupportedOperationException("Remote auth not supported.");
  }
}
