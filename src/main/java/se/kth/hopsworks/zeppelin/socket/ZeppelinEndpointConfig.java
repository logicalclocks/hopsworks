/*
 */
package se.kth.hopsworks.zeppelin.socket;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpSession;
import javax.websocket.HandshakeResponse;
import javax.websocket.server.HandshakeRequest;
import javax.websocket.server.ServerEndpointConfig;

/**
 *
 * @author ermiasg
 */
public class ZeppelinEndpointConfig extends ServerEndpointConfig.Configurator {

  @Override
  public void modifyHandshake(ServerEndpointConfig config,
          HandshakeRequest request, HandshakeResponse response) {

    HttpSession httpSession = (HttpSession) request.getHttpSession();
    ServletContext context = (ServletContext) httpSession.getServletContext();

    config.getUserProperties().put("httpSession", httpSession);
    config.getUserProperties().put("user", request.getUserPrincipal().getName());

  }
}
