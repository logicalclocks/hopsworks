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
package io.hops.hopsworks.api.filter;

import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.interfaces.DecodedJWT;
import io.hops.hopsworks.api.util.RESTApiJsonResponse;
import io.hops.hopsworks.common.exception.RESTCodes;
import io.hops.hopsworks.common.util.JsonResponse;
import io.hops.hopsworks.common.util.Settings;
import io.hops.hopsworks.jwt.AlgorithmFactory;
import io.hops.hopsworks.jwt.JWTController;
import io.hops.hopsworks.jwt.annotation.JWTRequired;
import io.hops.hopsworks.jwt.exception.SigningKeyNotFoundException;
import io.hops.hopsworks.jwt.filter.JWTFilter;
import java.io.IOException;
import java.lang.reflect.Method;
import java.security.Principal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Logger;
import javax.annotation.Priority;
import javax.ejb.EJB;
import javax.ws.rs.Priorities;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ResourceInfo;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.SecurityContext;
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
  @EJB
  private Settings settings;
  
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
  public String getIssuer() {
    return settings.getJWTIssuer();
  }

  @Override
  public Set<String> allowedRoles() {
    Class<?> resourceClass = resourceInfo.getResourceClass();
    Method method = resourceInfo.getResourceMethod();
    JWTRequired methodRolesAnnotation = method.getAnnotation(JWTRequired.class);
    JWTRequired classRolesAnnotation = resourceClass.getAnnotation(JWTRequired.class);
    JWTRequired rolesAnnotation = methodRolesAnnotation != null ? methodRolesAnnotation : classRolesAnnotation;
    if (rolesAnnotation == null) {
      return null;
    }
    Set<String> rolesSet = new HashSet<>(Arrays.asList(rolesAnnotation.allowedUserRoles()));
    return rolesSet;
  }

  @Override
  public Set<String> acceptedTokens() {
    Class<?> resourceClass = resourceInfo.getResourceClass();
    Method method = resourceInfo.getResourceMethod();
    JWTRequired methodAcceptedTokens = method.getAnnotation(JWTRequired.class);
    JWTRequired classAcceptedTokens = resourceClass.getAnnotation(JWTRequired.class);
    JWTRequired acceptedTokens = methodAcceptedTokens != null ? methodAcceptedTokens : classAcceptedTokens;
    if (acceptedTokens == null) {
      return null;
    }
    Set<String> acceptedTokensSet = new HashSet<>(Arrays.asList(acceptedTokens.acceptedTokens()));
    return acceptedTokensSet;
  }

  @Override
  public void postJWTFilter(ContainerRequestContext requestContext, DecodedJWT jwt) throws IOException {
    String scheme = requestContext.getUriInfo().getRequestUri().getScheme();
    requestContext.setSecurityContext(new JWTSecurityContext(jwt, scheme));
  }

  @Override
  public Object responseEntity(Response.Status status, String msg) {
    JsonResponse jsonResponse = new RESTApiJsonResponse();
    if (null == status) {
      jsonResponse.setErrorCode(RESTCodes.GenericErrorCode.UNKNOWN_ERROR.getCode());
    } else {
      switch (status) {
        case UNAUTHORIZED:
          jsonResponse.setErrorCode(RESTCodes.SecurityErrorCode.EJB_ACCESS_LOCAL.getCode());
          break;
        case FORBIDDEN:
          jsonResponse.setErrorCode(RESTCodes.SecurityErrorCode.REST_ACCESS_CONTROL.getCode());
          break;
        default:
          jsonResponse.setErrorCode(RESTCodes.GenericErrorCode.UNKNOWN_ERROR.getCode());
          break;
      }
    }
    jsonResponse.setErrorMsg(msg);
    return jsonResponse;
  }

  private class JWTSecurityContext implements SecurityContext {

    private final String scheme;
    private final DecodedJWT jwt;

    public JWTSecurityContext(DecodedJWT jwt, String scheme) {
      this.scheme = scheme;
      this.jwt = jwt;
    }

    @Override
    public Principal getUserPrincipal() {
      if (this.jwt == null) {
        return null;
      }
      return () -> this.jwt.getSubject();
    }

    @Override
    public boolean isUserInRole(String role) {
      String[] roles = jwtController.getRolesClaim(jwt);
      if (roles != null) {
        List<String> rolesList = new ArrayList<>(Arrays.asList(roles));
        if (!rolesList.isEmpty()) {
          return rolesList.contains(role);
        }
      }
      return false;
    }

    @Override
    public boolean isSecure() {
      return "https".equals(this.scheme);
    }

    @Override
    public String getAuthenticationScheme() {
      return SecurityContext.BASIC_AUTH;
    }

  }

}
