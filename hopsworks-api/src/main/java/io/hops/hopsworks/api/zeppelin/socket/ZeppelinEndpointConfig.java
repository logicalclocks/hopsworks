package io.hops.hopsworks.api.zeppelin.socket;

import java.util.logging.Level;
import java.util.logging.Logger;
import javax.servlet.http.HttpSession;
import javax.websocket.HandshakeResponse;
import javax.websocket.server.HandshakeRequest;
import javax.websocket.server.ServerEndpointConfig;

public class ZeppelinEndpointConfig extends ServerEndpointConfig.Configurator {

  private static final Logger logger = Logger.getLogger(
          ZeppelinEndpointConfig.class.
          getName());

  @Override
  public void modifyHandshake(ServerEndpointConfig config,
          HandshakeRequest request, HandshakeResponse response) {

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
