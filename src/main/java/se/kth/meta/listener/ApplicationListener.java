package se.kth.meta.listener;

import java.util.logging.Level;
import java.util.logging.Logger;
import javax.servlet.ServletContext;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import javax.servlet.ServletRequestEvent;
import javax.servlet.ServletRequestListener;
import javax.servlet.annotation.WebListener;
import javax.websocket.DeploymentException;
import javax.websocket.server.ServerContainer;
import se.kth.meta.wscomm.Protocol;
import se.kth.meta.wscomm.WebSocketEndpoint;

/**
 *
 * @author Vangelis
 */
@WebListener
public class ApplicationListener implements ServletContextListener,
        ServletRequestListener {

  private static final Logger logger = Logger.getLogger(
          ApplicationListener.class.getName());

  @Override
  public void contextInitialized(ServletContextEvent servletContextEvent) {

    ServletContext context = servletContextEvent.getServletContext();

    final ServerContainer serverContainer = (ServerContainer) context
            .getAttribute("javax.websocket.server.ServerContainer");

    try {

      context.setAttribute("protocol", new Protocol());

      //attach the WebSockets Endpoint to the web container
      serverContainer.addEndpoint(WebSocketEndpoint.class);

      logger.log(Level.INFO, "HOPSWORKS DEPLOYED");
    } catch (DeploymentException ex) {
      logger.log(Level.SEVERE, null, ex);
    }
  }

  @Override
  public void contextDestroyed(ServletContextEvent servletContextEvent) {
    ServletContext context = servletContextEvent.getServletContext();
    context.removeAttribute("protocol");
    logger.log(Level.INFO, "HOPSWORKS UNDEPLOYED");
  }

  @Override
  public void requestInitialized(ServletRequestEvent event) {
  }

  @Override
  public void requestDestroyed(ServletRequestEvent sre) {
  }
}
