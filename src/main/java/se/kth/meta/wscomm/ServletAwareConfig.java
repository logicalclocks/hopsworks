package se.kth.meta.wscomm;

import java.io.Serializable;
import java.net.URI;
import java.security.Principal;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.ejb.EJB;
import javax.enterprise.context.RequestScoped;
import javax.enterprise.context.SessionScoped;
import javax.servlet.ServletContext;
import javax.servlet.http.HttpSession;
import javax.websocket.HandshakeResponse;
import javax.websocket.server.HandshakeRequest;
import javax.websocket.server.ServerEndpointConfig;
import se.kth.bbc.project.Project;
import se.kth.bbc.project.ProjectFacade;
import se.kth.bbc.project.ProjectTeamFacade;

/**
 *
 * @author Vangelis
 */
public class ServletAwareConfig extends ServerEndpointConfig.Configurator {

  /**
   * Intercept the handshake operation so that we can take a hold of the
   * ServletContext instance to be able to retrieve attributes stored to it
   * such as the database object and other similar class instances
   * <p>
   * @param config
   * @param request
   * @param response
   */
  @Override
  public void modifyHandshake(ServerEndpointConfig config,
          HandshakeRequest request, HandshakeResponse response) {

    HttpSession httpSession = (HttpSession) request.getHttpSession();
    System.out.println("SESSION IS NULL " + (httpSession == null));
    System.out.println("WSSESSIONID " + httpSession.getId());
    ServletContext context = (ServletContext) httpSession.getServletContext();

    config.getUserProperties().put("httpSession", httpSession);
    config.getUserProperties().put("user", request.getUserPrincipal().getName());

    /**
     * store these attributes to servletContext so that they are available to
     * every created user socket session
     */
    config.getUserProperties().put("db", context.getAttribute("db"));
    config.getUserProperties().put("protocol", context.getAttribute("protocol"));
  }
}
