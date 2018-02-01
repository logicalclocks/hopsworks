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

package io.hops.hopsworks.api.zeppelin.socket;

import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.servlet.http.HttpSession;
import javax.websocket.HandshakeResponse;
import javax.websocket.server.HandshakeRequest;
import javax.websocket.server.ServerEndpointConfig;
import org.apache.zeppelin.util.WatcherSecurityKey;

public class ZeppelinEndpointConfig extends ServerEndpointConfig.Configurator {

  private static final Logger logger = Logger.getLogger(
          ZeppelinEndpointConfig.class.
          getName());

  @Override
  public void modifyHandshake(ServerEndpointConfig config,
          HandshakeRequest request, HandshakeResponse response) {

    Map<String, List<String>> headers = request.getHeaders();
    if (headers != null && headers.containsKey(WatcherSecurityKey.HTTP_HEADER)) {
      List<String> header = headers.get(WatcherSecurityKey.HTTP_HEADER);
      if (header.size() > 0) {
        config.getUserProperties().put(WatcherSecurityKey.HTTP_HEADER, header.
                get(0));
      }
    }
    HttpSession httpSession = (HttpSession) request.getHttpSession();
    String user = request.getUserPrincipal().getName();
    config.getUserProperties().put("httpSession", httpSession);
    config.getUserProperties().put("user", user);
    logger.log(Level.INFO, "Hand shake for upgrade to websocket by: {0}", user);
  }

//  @Override
//  public boolean checkOrigin(String originHeaderValue) {
//    try {
//      return SecurityUtils.isValidOrigin(originHeaderValue, zeppelin.getConf());
//    } catch (UnknownHostException | URISyntaxException e) {
//      logger.log(Level.INFO, "{0}", e.getMessage());
//    }
//
//    return false;
//  } 
}
