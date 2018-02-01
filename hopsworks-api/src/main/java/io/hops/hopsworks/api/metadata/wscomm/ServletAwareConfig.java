/*
 * This file is part of HopsWorks
 *
 * Copyright (C) 2013 - 2018, Logical Clocks AB and RISE SICS AB. All rights reserved.
 *
 * HopsWorks is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * HopsWorks is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with HopsWorks.  If not, see <http://www.gnu.org/licenses/>.
 */

package io.hops.hopsworks.api.metadata.wscomm;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpSession;
import javax.websocket.HandshakeResponse;
import javax.websocket.server.HandshakeRequest;
import javax.websocket.server.ServerEndpointConfig;

public class ServletAwareConfig extends ServerEndpointConfig.Configurator {

  /**
   * Intercept the handshake operation so that we can take a hold of the
   * ServletContext instance to be able to retrieve attributes stored to it
   * such as the database object and other similar class instances
   * <p/>
   * @param config
   * @param request
   * @param response
   */
  @Override
  public void modifyHandshake(ServerEndpointConfig config,
          HandshakeRequest request, HandshakeResponse response) {

    HttpSession httpSession = (HttpSession) request.getHttpSession();
    ServletContext context = (ServletContext) httpSession.getServletContext();

    config.getUserProperties().put("httpSession", httpSession);
    config.getUserProperties().put("user", request.getUserPrincipal().getName());

    /*
     * store these attributes to servletContext so that they are available to
     * every created user socket session
     */
    config.getUserProperties().put("protocol", context.getAttribute("protocol"));
  }
}
