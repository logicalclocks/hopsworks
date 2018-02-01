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
