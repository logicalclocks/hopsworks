/*
 * This file is part of Hopsworks
 * Copyright (C) 2020, Logical Clocks AB. All rights reserved
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

package io.hops.hopsworks.api.featurestore.featuregroup;


import io.hops.hopsworks.api.filter.AllowedProjectRoles;
import io.hops.hopsworks.api.filter.Audience;
import io.hops.hopsworks.api.filter.apiKey.ApiKeyRequired;
import io.hops.hopsworks.api.jwt.JWTHelper;
import io.hops.hopsworks.common.featurestore.featuregroup.FeaturegroupController;
import io.hops.hopsworks.common.featurestore.featuregroup.cached.FeatureGroupStorage;
import io.hops.hopsworks.common.util.Settings;
import io.hops.hopsworks.exceptions.FeaturestoreException;
import io.hops.hopsworks.exceptions.HopsSecurityException;
import io.hops.hopsworks.jwt.annotation.JWTRequired;
import io.hops.hopsworks.persistence.entity.featurestore.Featurestore;
import io.hops.hopsworks.persistence.entity.featurestore.featuregroup.Featuregroup;
import io.hops.hopsworks.persistence.entity.project.Project;
import io.hops.hopsworks.persistence.entity.user.Users;
import io.hops.hopsworks.persistence.entity.user.security.apiKey.ApiScope;
import io.swagger.annotations.ApiOperation;

import javax.ejb.EJB;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.enterprise.context.RequestScoped;
import javax.ws.rs.BeanParam;
import javax.ws.rs.GET;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.SecurityContext;
import javax.ws.rs.core.UriInfo;

@RequestScoped
@TransactionAttribute(TransactionAttributeType.NEVER)
public class FeatureGroupPreviewResource {

  @EJB
  private PreviewBuilder previewBuilder;
  @EJB
  private FeaturegroupController featuregroupController;
  @EJB
  private JWTHelper jwtHelper;
  @EJB
  private Settings settings;

  private Project project;
  private Featurestore featurestore;
  private Featuregroup featuregroup;

  public FeatureGroupPreviewResource setProject(Project project) {
    this.project = project;
    return this;
  }

  public void setFeaturestore(Featurestore featurestore) {
    this.featurestore = featurestore;
  }

  public void setFeatureGroupId(int featureGroupId) throws FeaturestoreException {
    featuregroup = featuregroupController.getFeaturegroupById(featurestore, featureGroupId);
  }

  @ApiOperation(value = "Get feature group preview", response = PreviewDTO.class)
  @GET
  @Produces(MediaType.APPLICATION_JSON)
  @AllowedProjectRoles({AllowedProjectRoles.DATA_OWNER, AllowedProjectRoles.DATA_SCIENTIST})
  @JWTRequired(acceptedTokens={Audience.API}, allowedUserRoles={"HOPS_ADMIN", "HOPS_USER"})
  @ApiKeyRequired( acceptedScopes = {ApiScope.FEATURESTORE}, allowedUserRoles = {"HOPS_ADMIN", "HOPS_USER"})
  public Response getPreview(@BeanParam FeatureGroupPreviewBeanParam featureGroupPreviewBeanParam,
                             @Context UriInfo uriInfo, @Context SecurityContext sc)
      throws FeaturestoreException, HopsSecurityException {
    Users user = jwtHelper.getUserPrincipal(sc);

    if (featureGroupPreviewBeanParam.getLimit() != null && (
        featureGroupPreviewBeanParam.getLimit() < 0  ||
        featureGroupPreviewBeanParam.getLimit() > settings.getFGPreviewLimit())) {
      throw new IllegalArgumentException(
          "Row limit should greater than 0 and lower than: " + settings.getFGPreviewLimit());
    }

    PreviewDTO previewDTO = previewBuilder.build(uriInfo, user, project, featuregroup,
        featureGroupPreviewBeanParam.getPartition(),
        featureGroupPreviewBeanParam.getStorage().equals(FeatureGroupStorage.ONLINE),
        featureGroupPreviewBeanParam.getLimit() == null ? 20 : featureGroupPreviewBeanParam.getLimit());

    return Response.ok().entity(previewDTO).build();
  }
}
