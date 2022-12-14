/*
 * This file is part of Hopsworks
 * Copyright (C) 2022, Hopsworks AB. All rights reserved
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

package io.hops.hopsworks.api.filter.featureFlags;

import io.hops.hopsworks.api.util.RESTApiJsonResponse;
import io.hops.hopsworks.common.util.Settings;
import io.hops.hopsworks.restutils.JsonResponse;
import io.hops.hopsworks.restutils.RESTCodes;

import javax.annotation.Priority;
import javax.ejb.EJB;
import javax.ws.rs.Priorities;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.container.ResourceInfo;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.Provider;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.logging.Logger;

@Provider
@FeatureFlagRequired
@Priority(Priorities.USER)
public class FeatureFlagFilter implements ContainerRequestFilter {
  private static final Logger LOGGER = Logger.getLogger(FeatureFlagFilter.class.getName());
  
  @EJB
  private Settings settings;
  
  @Context
  private ResourceInfo resourceInfo;
  
  @Override
  public void filter(ContainerRequestContext requestContext) {
    Set<String> featureFlags = getRequiredFeatureFlags();
    try {
      checkFeatureFlags(featureFlags);
    } catch (IllegalArgumentException e) {
      RESTCodes.GenericErrorCode featureFlagNotEnabled = RESTCodes.GenericErrorCode.FEATURE_FLAG_NOT_ENABLED;
      JsonResponse jsonResponse = new RESTApiJsonResponse();
      jsonResponse.setErrorMsg(e.getMessage());
      jsonResponse.setErrorCode(featureFlagNotEnabled.getCode());
      requestContext.abortWith(
        Response.status(featureFlagNotEnabled.getRespStatus().getStatusCode()).entity(jsonResponse).build());
    }
  }
  
  private void checkFeatureFlags(Set<String> featureFlags) throws IllegalArgumentException {
    for (String featureFlag : featureFlags) {
      if (featureFlag.equals(FeatureFlags.DATA_SCIENCE_PROFILE) && !settings.getEnableDataScienceProfile()) {
        throw new IllegalArgumentException("Data science profile is required for this operation.");
      }
    }
  }
  
  private Set<String> getRequiredFeatureFlags() {
    FeatureFlagRequired featureFlagAnnotation = getAnnotation();
    if (featureFlagAnnotation == null) {
      return Collections.emptySet();
    }
    return new HashSet<>(Arrays.asList(featureFlagAnnotation.requiredFeatureFlags()));
  }
  
  private FeatureFlagRequired getAnnotation() {
    Class<?> resourceClass = resourceInfo.getResourceClass();
    Method method = resourceInfo.getResourceMethod();
    FeatureFlagRequired methodRolesAnnotation = method.getAnnotation(FeatureFlagRequired.class);
    FeatureFlagRequired classRolesAnnotation = resourceClass.getAnnotation(FeatureFlagRequired.class);
    return methodRolesAnnotation != null ? methodRolesAnnotation : classRolesAnnotation;
  }
}
