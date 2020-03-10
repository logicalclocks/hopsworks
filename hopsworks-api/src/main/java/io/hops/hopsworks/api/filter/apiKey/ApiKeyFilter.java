/*
 * This file is part of Hopsworks
 * Copyright (C) 2019, Logical Clocks AB. All rights reserved
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
package io.hops.hopsworks.api.filter.apiKey;

import io.hops.hopsworks.api.filter.util.HopsworksSecurityContext;
import io.hops.hopsworks.api.filter.util.Subject;
import io.hops.hopsworks.api.util.RESTApiJsonResponse;
import io.hops.hopsworks.common.user.UsersController;
import io.hops.hopsworks.common.user.security.apiKey.ApiKeyController;
import io.hops.hopsworks.common.util.Settings;
import io.hops.hopsworks.exceptions.ApiKeyException;
import io.hops.hopsworks.persistence.entity.user.Users;
import io.hops.hopsworks.persistence.entity.user.security.apiKey.ApiKey;
import io.hops.hopsworks.persistence.entity.user.security.apiKey.ApiScope;
import io.hops.hopsworks.restutils.JsonResponse;
import io.hops.hopsworks.restutils.RESTCodes;

import javax.annotation.Priority;
import javax.ejb.EJB;
import javax.ws.rs.Priorities;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.container.ResourceInfo;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.Provider;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import static io.hops.hopsworks.jwt.Constants.BEARER;

@Provider
@ApiKeyRequired
@Priority(Priorities.AUTHENTICATION - 1)
public class ApiKeyFilter implements ContainerRequestFilter {
  private static final Logger LOGGER = Logger.getLogger(ApiKeyFilter.class.getName());
  private static final String WWW_AUTHENTICATE_VALUE = "ApiKey realm=\"Cauth Realm\"";

  public static final String API_KEY = "ApiKey ";
  
  @EJB
  private ApiKeyController apiKeyController;
  @EJB
  private UsersController usersController;
  @EJB
  private Settings settings;
  
  @Context
  private ResourceInfo resourceInfo;
  
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
      LOGGER.log(Level.FINEST, "{0}token found, leaving Api key interceptor", BEARER);
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
      ApiKey apiKey = apiKeyController.getApiKey(key);
      Users user = apiKey.getUser();
      List<String> roles = usersController.getUserRoles(user);
      Set<ApiScope> scopes = apiKeyController.getScopes(apiKey);
      checkRole(roles);
      checkScope(scopes);
      Subject subject = new Subject(user.getUsername(), roles);
      String scheme = requestContext.getUriInfo().getRequestUri().getScheme();
      requestContext.setSecurityContext(new HopsworksSecurityContext(subject, scheme));
    } catch (ApiKeyException e) {
      LOGGER.log(Level.FINEST, "Api key Verification Exception: {0}", e.getMessage());
      e.buildJsonResponse(jsonResponse, settings.getHopsworksRESTLogLevel());
      requestContext.abortWith(Response.status(e.getErrorCode().getRespStatus().getStatusCode())
        .header(HttpHeaders.WWW_AUTHENTICATE, WWW_AUTHENTICATE_VALUE).entity(jsonResponse).build());
    }
  }
  
  
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
    Class<?> resourceClass = resourceInfo.getResourceClass();
    Method method = resourceInfo.getResourceMethod();
    ApiKeyRequired methodRolesAnnotation = method.getAnnotation(ApiKeyRequired.class);
    ApiKeyRequired classRolesAnnotation = resourceClass.getAnnotation(ApiKeyRequired.class);
    return methodRolesAnnotation != null ? methodRolesAnnotation : classRolesAnnotation;
  }
}
