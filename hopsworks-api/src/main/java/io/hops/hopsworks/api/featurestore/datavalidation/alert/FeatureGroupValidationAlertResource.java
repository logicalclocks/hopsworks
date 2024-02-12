/*
 * This file is part of Hopsworks
 * Copyright (C) 2024, Hopsworks AB. All rights reserved
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
package io.hops.hopsworks.api.featurestore.datavalidation.alert;

import io.hops.hopsworks.alert.util.Constants;
import io.hops.hopsworks.api.filter.AllowedProjectRoles;
import io.hops.hopsworks.api.filter.Audience;
import io.hops.hopsworks.api.auth.key.ApiKeyRequired;
import io.hops.hopsworks.common.api.ResourceRequest;
import io.hops.hopsworks.common.featurestore.datavalidation.FeatureGroupAlertFacade;
import io.hops.hopsworks.exceptions.FeaturestoreException;
import io.hops.hopsworks.jwt.annotation.JWTRequired;
import io.hops.hopsworks.persistence.entity.featurestore.featuregroup.datavalidation.alert.FeatureGroupAlert;
import io.hops.hopsworks.persistence.entity.user.security.apiKey.ApiScope;
import io.hops.hopsworks.restutils.RESTCodes;
import io.swagger.annotations.ApiOperation;

import javax.ejb.EJB;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.enterprise.context.RequestScoped;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.SecurityContext;
import javax.ws.rs.core.UriInfo;
import java.util.logging.Level;

@RequestScoped
@TransactionAttribute(TransactionAttributeType.NEVER)
public class FeatureGroupValidationAlertResource extends FeatureStoreAlertResource {
  @Override
  protected ResourceRequest.Name getEntityType() {
    return ResourceRequest.Name.FEATUREGROUPS;
  }
  
  @EJB
  FeatureGroupAlertBuilder featureGroupAlertBuilder;
  @EJB
  FeatureGroupAlertFacade featureGroupAlertFacade;
  
  @PUT
  @Path("{id}")
  @Produces(MediaType.APPLICATION_JSON)
  @ApiOperation(value = "Update a feature group alert.", response = FeatureGroupAlertDTO.class)
  @AllowedProjectRoles({AllowedProjectRoles.DATA_OWNER, AllowedProjectRoles.DATA_SCIENTIST})
  @JWTRequired(acceptedTokens = {Audience.API}, allowedUserRoles = {"HOPS_ADMIN", "HOPS_USER", "HOPS_SERVICE_USER"})
  @ApiKeyRequired(acceptedScopes = {ApiScope.FEATURESTORE},
    allowedUserRoles = {"HOPS_ADMIN", "HOPS_USER", "HOPS_SERVICE_USER"})
  public Response createOrUpdate(
    @PathParam("id")
    Integer id,
    FeatureGroupAlertDTO dto,
    @Context
    UriInfo uriInfo,
    @Context
    HttpServletRequest req,
    @Context
    SecurityContext sc) throws FeaturestoreException {
    featureStoreAlertValidation.validateEntityType(getEntityType(), this.featuregroup, this.featureView);
    if (dto == null) {
      throw new FeaturestoreException(RESTCodes.FeaturestoreErrorCode.ALERT_ILLEGAL_ARGUMENT, Level.FINE,
        Constants.NO_PAYLOAD);
    }
    FeatureGroupAlert featureGroupAlert = featureGroupAlertFacade.findByFeatureGroupAndId(this.featuregroup, id);
    featureStoreAlertValidation.validateUpdate(featureGroupAlert, dto.getStatus(), featuregroup);
    featureGroupAlert = featureStoreAlertController.updateAlert(dto, featureGroupAlert, project);
    ResourceRequest resourceRequest = new ResourceRequest(ResourceRequest.Name.ALERTS);
    dto = featureGroupAlertBuilder.build(uriInfo, resourceRequest, featureGroupAlert);
    return Response.ok().entity(dto).build();
  }
}