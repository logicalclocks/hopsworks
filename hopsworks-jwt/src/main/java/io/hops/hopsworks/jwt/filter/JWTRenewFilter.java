/*
 * This file is part of Hopsworks
 * Copyright (C) 2018, Logical Clocks AB. All rights reserved
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
package io.hops.hopsworks.jwt.filter;

import static io.hops.hopsworks.jwt.Constants.BEARER;
import io.hops.hopsworks.jwt.exception.JWTException;
import io.hops.hopsworks.jwt.exception.NotRenewableException;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerResponseContext;
import javax.ws.rs.container.ContainerResponseFilter;
import static javax.ws.rs.core.HttpHeaders.AUTHORIZATION;
import javax.ws.rs.core.Response;

public abstract class JWTRenewFilter implements ContainerResponseFilter {

  private final static Logger LOGGER = Logger.getLogger(JWTRenewFilter.class.getName());

  @Override
  public void filter(ContainerRequestContext requestContext, ContainerResponseContext responseContext) throws
      IOException {
    if (!responseContext.getStatusInfo().getFamily().equals(Response.Status.Family.SUCCESSFUL)) {
      return;
    }
    String authorizationHeader = requestContext.getHeaderString(AUTHORIZATION);
    if (authorizationHeader == null || !authorizationHeader.startsWith(BEARER)) {
      return;
    }
    String jwt = authorizationHeader.substring(BEARER.length()).trim();
    String token = null;
    try {
      token = renewToken(jwt);
    } catch (NotRenewableException ne) {
      // Nothing to do
    } catch (JWTException ex) {
      LOGGER.log(Level.WARNING, "Failed to renew token. {0}", ex.getMessage());
    } catch (Exception e) {
      LOGGER.log(Level.SEVERE, "Failed to renew token.", e);
    }
    if (token != null) {
      responseContext.getHeaders().putSingle(AUTHORIZATION, BEARER + token);
    }
  }

  public abstract String renewToken(String token) throws JWTException;
}
