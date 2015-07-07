
package se.kth.hopsworks.zeppelin.socket;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.servlet.ServletContext;
import javax.servlet.http.HttpSession;
import javax.websocket.HandshakeResponse;
import javax.websocket.server.HandshakeRequest;
import javax.websocket.server.ServerEndpointConfig;
import javax.ws.rs.core.Cookie;

/**
 *
 * @author ermiasg
 */
public class ZeppelinEndpointConfig extends ServerEndpointConfig.Configurator {
private static final Logger logger = Logger.getLogger(ZeppelinEndpointConfig.class.
          getName());
  @Override
  public void modifyHandshake(ServerEndpointConfig config,
          HandshakeRequest request, HandshakeResponse response) {
    
    HttpSession httpSession = (HttpSession) request.getHttpSession();
    ServletContext context = (ServletContext) httpSession.getServletContext();
    Map<String, String> cookies = new HashMap<>();
    String cookie = request.getHeaders().get("Cookie").get(0);
    String[] cookieParts = cookie.split("; ");//should have space after ;
    for (String c : cookieParts){
      cookies.put(c.substring(0, c.indexOf("=")), c.substring(c.indexOf("=")+1, c.length()));
    }
    logger.log(Level.INFO, "cookies.get(\"email\") : {0}", cookies.get("email"));
    logger.log(Level.INFO, "cookies.get(\"SESSIONID\") : {0}", cookies.get("SESSIONID"));
    logger.log(Level.INFO, "cookies.get(\"projectID\") : {0}", cookies.get("projectID"));
    config.getUserProperties().put("httpSession", httpSession);
    config.getUserProperties().put("user", request.getUserPrincipal().getName());
    config.getUserProperties().put("projectID", cookies.get("projectID"));

  }
}
