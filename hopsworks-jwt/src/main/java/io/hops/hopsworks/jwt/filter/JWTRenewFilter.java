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

import io.hops.hopsworks.jwt.exception.JWTException;
import io.hops.hopsworks.jwt.exception.NotRenewableException;
import io.hops.hopsworks.jwt.utils.ProxyAuthHelper;

import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerResponseContext;
import javax.ws.rs.container.ContainerResponseFilter;
import javax.ws.rs.core.Cookie;
import javax.ws.rs.core.NewCookie;
import javax.ws.rs.core.Response;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

import static io.hops.hopsworks.jwt.Constants.BEARER;
import static io.hops.hopsworks.jwt.Constants.PROXY_JWT_COOKIE_NAME;
import static javax.ws.rs.core.HttpHeaders.AUTHORIZATION;

public abstract class JWTRenewFilter implements ContainerResponseFilter {

  private final static Logger LOGGER = Logger.getLogger(JWTRenewFilter.class.getName());

  @Override
  public void filter(ContainerRequestContext requestContext, ContainerResponseContext responseContext) throws
      IOException {
    if (!responseContext.getStatusInfo().getFamily().equals(Response.Status.Family.SUCCESSFUL)) {
      return;
    }
    renewJWT(requestContext, responseContext);
    renewProxyJWT(requestContext, responseContext);
  }
  
  private String renew(String jwt) {
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
    return token;
  }
  
  private void renewJWT(ContainerRequestContext requestContext, ContainerResponseContext responseContext) {
    String authorizationHeader = requestContext.getHeaderString(AUTHORIZATION);
    if (authorizationHeader == null || !authorizationHeader.startsWith(BEARER)) {
      return;
    }
    String jwt = authorizationHeader.substring(BEARER.length()).trim();
    String token = renew(jwt);
    if (token != null) {
      responseContext.getHeaders().putSingle(AUTHORIZATION, BEARER + token);
    }
  }
  
  private void renewProxyJWT(ContainerRequestContext requestContext, ContainerResponseContext responseContext) {
    if (requestContext.getCookies().containsKey(PROXY_JWT_COOKIE_NAME)) {
      Cookie cookie = requestContext.getCookies().get(PROXY_JWT_COOKIE_NAME);
      String token = renew(cookie.getValue());
      if (token != null) {
        NewCookie newCookie =
          ProxyAuthHelper.getNewCookie(token, getTokenLifeMs());
        responseContext.getHeaders().add("Set-Cookie", newCookie);
      }
    }
  }

  public abstract String renewToken(String token) throws JWTException;
  
  public abstract long getTokenLifeMs();
}
