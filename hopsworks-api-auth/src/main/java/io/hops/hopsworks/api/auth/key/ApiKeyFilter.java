/*
 * This file is part of Hopsworks
 * Copyright (C) 2023, Hopsworks AB. All rights reserved
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
package io.hops.hopsworks.api.auth.key;

import io.hops.hopsworks.api.auth.HopsworksSecurityContext;
import io.hops.hopsworks.api.auth.Subject;
import io.hops.hopsworks.exceptions.ApiKeyException;
import io.hops.hopsworks.exceptions.UserException;
import io.hops.hopsworks.jwt.annotation.JWTRequired;
import io.hops.hopsworks.persistence.entity.user.Users;
import io.hops.hopsworks.persistence.entity.user.security.apiKey.ApiKey;
import io.hops.hopsworks.persistence.entity.user.security.apiKey.ApiScope;
import io.hops.hopsworks.restutils.JsonResponse;
import io.hops.hopsworks.restutils.RESTApiJsonResponse;
import io.hops.hopsworks.restutils.RESTCodes;
import io.hops.hopsworks.restutils.RESTLogLevel;

import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Response;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import static io.hops.hopsworks.jwt.Constants.BEARER;
import static io.hops.hopsworks.jwt.Constants.WWW_AUTHENTICATE_VALUE;

public abstract class ApiKeyFilter implements ContainerRequestFilter {

  private static final Logger LOGGER = Logger.getLogger(ApiKeyFilter.class.getName());
  public static final String API_KEY = "ApiKey ";

  @Override
  public void filter(ContainerRequestContext requestContext) {
    String authorizationHeader = requestContext.getHeaderString(HttpHeaders.AUTHORIZATION);
    JsonResponse jsonResponse = new RESTApiJsonResponse();
    if (authorizationHeader == null) {
      LOGGER.log(Level.FINEST, "Authorization header not set.");
      jsonResponse.setErrorCode(RESTCodes.SecurityErrorCode.EJB_ACCESS_LOCAL.getCode());
      jsonResponse.setErrorMsg("Authorization header not set.");
      requestContext.abortWith(Response.status(Response.Status.UNAUTHORIZED).header(HttpHeaders.WWW_AUTHENTICATE,
          WWW_AUTHENTICATE_VALUE).entity(jsonResponse).build());
      return;
    }
    if (authorizationHeader.startsWith(BEARER)) {
      LOGGER.log(Level.FINEST, "{0} token found, leaving Api key interceptor", BEARER);
      if (getJWTAnnotation() == null) {
        jsonResponse.setErrorCode(RESTCodes.SecurityErrorCode.EJB_ACCESS_LOCAL.getCode());
        jsonResponse.setErrorMsg("Authorization method not supported.");
        requestContext.abortWith(Response.status(Response.Status.UNAUTHORIZED).header(HttpHeaders.WWW_AUTHENTICATE,
            WWW_AUTHENTICATE_VALUE).entity(jsonResponse).build());
      }
      return;
    }
    if (!authorizationHeader.startsWith(API_KEY)) {
      LOGGER.log(Level.FINEST, "Invalid Api key. AuthorizationHeader : {0}", authorizationHeader);
      jsonResponse.setErrorCode(RESTCodes.SecurityErrorCode.EJB_ACCESS_LOCAL.getCode());
      jsonResponse.setErrorMsg("Invalidated Api key.");
      requestContext.abortWith(Response.status(Response.Status.UNAUTHORIZED).header(HttpHeaders.WWW_AUTHENTICATE,
          WWW_AUTHENTICATE_VALUE).entity(jsonResponse).build());
      return;
    }

    String key = authorizationHeader.substring(API_KEY.length()).trim();
    try {
      ApiKey apiKey = getApiKey(key);
      Users user = apiKey.getUser();
      validateUserStatus(user);
      List<String> roles = getUserRoles(user);
      Set<ApiScope> scopes = getApiScopes(apiKey);
      checkRole(roles);
      checkScope(scopes);
      Subject subject = new Subject(user.getUsername(), roles);
      String scheme = requestContext.getUriInfo().getRequestUri().getScheme();
      requestContext.setSecurityContext(new HopsworksSecurityContext(subject, scheme));
    } catch (ApiKeyException | UserException e) {
      LOGGER.log(Level.FINEST, "Api key Verification Exception: {0}", e.getMessage());
      e.buildJsonResponse(jsonResponse, getRestLogLevel());
      requestContext.abortWith(Response.status(e.getErrorCode().getRespStatus().getStatusCode())
          .header(HttpHeaders.WWW_AUTHENTICATE, WWW_AUTHENTICATE_VALUE).entity(jsonResponse).build());
    }
  }

  protected abstract void validateUserStatus(Users user) throws UserException;
  protected abstract ApiKey getApiKey(String key) throws ApiKeyException;
  protected abstract Set<ApiScope> getApiScopes(ApiKey key);
  protected abstract List<String> getUserRoles(Users user);
  protected abstract RESTLogLevel getRestLogLevel();
  protected abstract Class<?> getResourceClass();
  protected abstract Method getResourceMethod();

  private void checkRole(List<String> userRoles) throws ApiKeyException {
    Set<String> annotationRoles = getAllowedRoles();
    if (annotationRoles.isEmpty() || userRoles == null || userRoles.isEmpty()) {
      throw new ApiKeyException(RESTCodes.ApiKeyErrorCode.KEY_ROLE_CONTROL_EXCEPTION, Level.FINE);
    }
    annotationRoles.retainAll(userRoles);
    if (annotationRoles.isEmpty()) {
      throw new ApiKeyException(RESTCodes.ApiKeyErrorCode.KEY_ROLE_CONTROL_EXCEPTION, Level.FINE);
    }
  }

  private void checkScope(Set<ApiScope> scopes) throws ApiKeyException {
    Set<ApiScope> annotationScopes = getAllowedScopes();
    if (annotationScopes.isEmpty() || scopes == null || scopes.isEmpty()) {
      throw new ApiKeyException(RESTCodes.ApiKeyErrorCode.KEY_SCOPE_CONTROL_EXCEPTION, Level.FINE);
    }
    annotationScopes.retainAll(scopes);
    if (annotationScopes.isEmpty()) {
      throw new ApiKeyException(RESTCodes.ApiKeyErrorCode.KEY_SCOPE_CONTROL_EXCEPTION, Level.FINE);
    }
  }

  private Set<String> getAllowedRoles() {
    ApiKeyRequired rolesAnnotation = getAnnotation();
    if (rolesAnnotation == null) {
      return Collections.emptySet();
    }
    return new HashSet<>(Arrays.asList(rolesAnnotation.allowedUserRoles()));
  }

  private Set<ApiScope> getAllowedScopes() {
    ApiKeyRequired scopeAnnotation = getAnnotation();
    if (scopeAnnotation == null) {
      return Collections.emptySet();
    }
    return new HashSet<>(Arrays.asList(scopeAnnotation.acceptedScopes()));
  }

  private ApiKeyRequired getAnnotation() {
    Class<?> resourceClass = getResourceClass();
    Method method = getResourceMethod();
    ApiKeyRequired methodRolesAnnotation = method.getAnnotation(ApiKeyRequired.class);
    ApiKeyRequired classRolesAnnotation = resourceClass.getAnnotation(ApiKeyRequired.class);
    return methodRolesAnnotation != null ? methodRolesAnnotation : classRolesAnnotation;
  }

  private JWTRequired getJWTAnnotation() {
    Class<?> resourceClass = getResourceClass();
    Method method = getResourceMethod();
    JWTRequired methodAcceptedTokens = method.getAnnotation(JWTRequired.class);
    JWTRequired classAcceptedTokens = resourceClass.getAnnotation(JWTRequired.class);
    JWTRequired annotation = methodAcceptedTokens != null ? methodAcceptedTokens : classAcceptedTokens;
    return annotation;
  }
}
