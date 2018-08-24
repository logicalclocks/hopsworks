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
package io.hops.hopsworks.apiV2.filter;

import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.interfaces.DecodedJWT;
import io.hops.hopsworks.jwt.AlgorithmFactory;
import io.hops.hopsworks.jwt.JWTController;
import io.hops.hopsworks.jwt.annotation.AcceptedTokens;
import io.hops.hopsworks.jwt.annotation.AllowedUserRoles;
import io.hops.hopsworks.jwt.annotation.JWTRequired;
import io.hops.hopsworks.jwt.exception.SigningKeyNotFoundException;
import io.hops.hopsworks.jwt.filter.JWTFilter;
import java.io.IOException;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.logging.Logger;
import javax.annotation.Priority;
import javax.ejb.EJB;
import javax.ws.rs.Priorities;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ResourceInfo;
import javax.ws.rs.core.Context;
import javax.ws.rs.ext.Provider;

@Provider
@JWTRequired
@Priority(Priorities.AUTHENTICATION)
public class AuthFilter extends JWTFilter {

  private final static Logger LOGGER = Logger.getLogger(AuthFilter.class.getName());
  @EJB
  private JWTController jwtController;
  @EJB
  private AlgorithmFactory algorithmFactory;
  @Context
  private ResourceInfo resourceInfo;

  @Override
  public Algorithm getAlgorithm(DecodedJWT jwt) throws SigningKeyNotFoundException {
    return algorithmFactory.getAlgorithm(jwt);
  }

  @Override
  public boolean isTokenValid(DecodedJWT jwt) {
    return !jwtController.isTokenInvalidated(jwt);
  }

  @Override
  public void preJWTFilter(ContainerRequestContext requestContext) throws IOException {

  }

  @Override
  public void postJWTFilter(ContainerRequestContext requestContext, DecodedJWT jwt) throws IOException {

  }

  @Override
  public String getIssuer() {
    return null;
  }

  @Override
  public Set<String> allowedRoles() {
    Method method = resourceInfo.getResourceMethod();
    if (!method.isAnnotationPresent(AllowedUserRoles.class)) {
      return null;
    }
    AllowedUserRoles rolesAnnotation = method.getAnnotation(AllowedUserRoles.class);
    Set<String> rolesSet = new HashSet<>(Arrays.asList(rolesAnnotation.value()));
    return rolesSet;
  }

  @Override
  public Set<String> acceptedTokens() {
    Method method = resourceInfo.getResourceMethod();
    if (!method.isAnnotationPresent(AcceptedTokens.class)) {
      return null;
    }
    AcceptedTokens acceptedTokens = method.getAnnotation(AcceptedTokens.class);
    Set<String> acceptedTokensSet = new HashSet<>(Arrays.asList(acceptedTokens.value()));
    return acceptedTokensSet;
  }

}
