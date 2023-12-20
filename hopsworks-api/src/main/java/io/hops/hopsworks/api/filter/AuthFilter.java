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
import io.hops.hopsworks.api.auth.HopsworksSecurityContext;
import io.hops.hopsworks.api.auth.Subject;
import io.hops.hopsworks.api.auth.key.ApiKeyFilter;
import io.hops.hopsworks.api.auth.key.ApiKeyRequired;
import io.hops.hopsworks.api.util.RESTApiJsonResponse;
import io.hops.hopsworks.common.util.Settings;
import io.hops.hopsworks.jwt.AlgorithmFactory;
import io.hops.hopsworks.jwt.JWTController;
import io.hops.hopsworks.jwt.annotation.JWTRequired;
import io.hops.hopsworks.jwt.exception.SigningKeyNotFoundException;
import io.hops.hopsworks.jwt.filter.JWTFilter;
import io.hops.hopsworks.restutils.JsonResponse;
import io.hops.hopsworks.restutils.RESTCodes;

import javax.annotation.Priority;
import javax.ejb.EJB;
import javax.ws.rs.Priorities;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ResourceInfo;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.Provider;
import java.io.IOException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import static io.hops.hopsworks.jwt.Constants.WWW_AUTHENTICATE_VALUE;

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
  public boolean preJWTFilter(ContainerRequestContext requestContext) throws IOException {
    String authorizationHeader = requestContext.getHeaderString(HttpHeaders.AUTHORIZATION);
    if (authorizationHeader != null && authorizationHeader.startsWith(ApiKeyFilter.API_KEY)) {
      LOGGER.log(Level.FINEST, "{0} found, leaving JWT interceptor", ApiKeyFilter.API_KEY);
      if (getApiKeyAnnotation() == null) {
        requestContext.abortWith(Response.status(Response.Status.UNAUTHORIZED).header(HttpHeaders.WWW_AUTHENTICATE,
          WWW_AUTHENTICATE_VALUE).entity(responseEntity(Response.Status.UNAUTHORIZED, "Authorization method not " +
          "supported."))
          .build());
      }
      return false;
    }
    return true;
  }

  @Override
  public String getIssuer() {
    return settings.getJWTIssuer();
  }

  @Override
  public Set<String> allowedRoles() {
    JWTRequired rolesAnnotation = getAnnotation();
    if (rolesAnnotation == null) {
      return null;
    }
    return new HashSet<>(Arrays.asList(rolesAnnotation.allowedUserRoles()));
  }

  @Override
  public Set<String> acceptedTokens() {
    JWTRequired acceptedTokens = getAnnotation();
    if (acceptedTokens == null) {
      return null;
    }
    return new HashSet<>(Arrays.asList(acceptedTokens.acceptedTokens()));
  }
  
  private JWTRequired getAnnotation() {
    Class<?> resourceClass = resourceInfo.getResourceClass();
    Method method = resourceInfo.getResourceMethod();
    JWTRequired methodAcceptedTokens = method.getAnnotation(JWTRequired.class);
    JWTRequired classAcceptedTokens = resourceClass.getAnnotation(JWTRequired.class);
    return methodAcceptedTokens != null ? methodAcceptedTokens : classAcceptedTokens;
  }
  
  private ApiKeyRequired getApiKeyAnnotation() {
    Class<?> resourceClass = resourceInfo.getResourceClass();
    Method method = resourceInfo.getResourceMethod();
    ApiKeyRequired methodRolesAnnotation = method.getAnnotation(ApiKeyRequired.class);
    ApiKeyRequired classRolesAnnotation = resourceClass.getAnnotation(ApiKeyRequired.class);
    return methodRolesAnnotation != null ? methodRolesAnnotation : classRolesAnnotation;
  }

  @Override
  public void postJWTFilter(ContainerRequestContext requestContext, DecodedJWT jwt) throws IOException {
    String scheme = requestContext.getUriInfo().getRequestUri().getScheme();
    String[] roles = jwtController.getRolesClaim(jwt);
    Subject subject = new Subject(jwt.getSubject(), new ArrayList<>(Arrays.asList(roles)));
    requestContext.setSecurityContext(new HopsworksSecurityContext(subject, scheme));
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
}
