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
package io.hops.hopsworks.common.remote.oauth;

import io.hops.hopsworks.common.remote.RemoteUserStateDTO;
import io.hops.hopsworks.persistence.entity.remote.oauth.OauthClient;

import javax.security.auth.login.LoginException;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;

public interface OAuthHelper {
  
  boolean oauthAvailable();
  RemoteUserStateDTO login(String code, String state, boolean consent, String chosenEmail) throws LoginException;
  OpenIdProviderConfig getOpenIdProviderConfiguration(String providerURI) throws IOException, URISyntaxException;
  URI getAuthenticationRequestURL(String providerName) throws URISyntaxException;
  void registerClient(OpenIdProviderConfig openidConf) throws URISyntaxException, IOException;
  void saveClient(OauthClient  oauthClient);
  void updateClient(OauthClient  oauthClient);
  void removeClient(OauthClient  oauthClient);
}
