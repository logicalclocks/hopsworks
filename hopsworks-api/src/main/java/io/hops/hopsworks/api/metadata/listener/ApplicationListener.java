/*
 * Copyright (C) 2013 - 2018, Logical Clocks AB and RISE SICS AB. All rights reserved
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this
 * software and associated documentation files (the "Software"), to deal in the Software
 * without restriction, including without limitation the rights to use, copy, modify, merge,
 * publish, distribute, sublicense, and/or sell copies of the Software, and to permit
 * persons to whom the Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or
 * substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS  OR IMPLIED, INCLUDING
 * BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL  THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM,
 * DAMAGES OR  OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 *
 */

package io.hops.hopsworks.api.metadata.listener;

import io.hops.hopsworks.api.metadata.wscomm.MetadataProtocol;
import io.hops.hopsworks.api.metadata.wscomm.WebSocketEndpoint;
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

      context.setAttribute("protocol", new MetadataProtocol());

      //attach the WebSockets Endpoint to the web container
      serverContainer.addEndpoint(WebSocketEndpoint.class);

      logger.log(Level.INFO, "HOPSWORKS DEPLOYED");
    } catch (DeploymentException ex) {
      logger.log(Level.SEVERE, ex.getMessage(), ex);
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
