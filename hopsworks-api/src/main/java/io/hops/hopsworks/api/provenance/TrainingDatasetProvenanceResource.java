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
package io.hops.hopsworks.api.provenance;

import io.hops.hopsworks.api.auth.key.ApiKeyRequired;
import io.hops.hopsworks.api.featurestore.trainingdataset.TrainingDatasetSubResource;
import io.hops.hopsworks.api.filter.AllowedProjectRoles;
import io.hops.hopsworks.api.filter.Audience;
import io.hops.hopsworks.api.jwt.JWTHelper;
import io.hops.hopsworks.api.provenance.explicit.ExplicitProvenanceExpansionBeanParam;
import io.hops.hopsworks.api.provenance.explicit.ProvExplicitLinksBuilder;
import io.hops.hopsworks.api.provenance.explicit.dto.ProvExplicitLinkDTO;
import io.hops.hopsworks.api.provenance.ops.ProvLinksBeanParams;
import io.hops.hopsworks.api.provenance.ops.ProvUsageBeanParams;
import io.hops.hopsworks.api.provenance.ops.ProvUsageBuilder;
import io.hops.hopsworks.api.provenance.ops.dto.ProvArtifactUsageParentDTO;
import io.hops.hopsworks.common.api.ResourceRequest;
import io.hops.hopsworks.common.dataset.util.DatasetHelper;
import io.hops.hopsworks.common.dataset.util.DatasetPath;
import io.hops.hopsworks.common.featurestore.FeaturestoreController;
import io.hops.hopsworks.common.featurestore.featureview.FeatureViewController;
import io.hops.hopsworks.common.featurestore.trainingdatasets.TrainingDatasetController;
import io.hops.hopsworks.common.project.ProjectController;
import io.hops.hopsworks.common.provenance.explicit.ProvExplicitControllerIface;
import io.hops.hopsworks.common.provenance.explicit.ProvExplicitLink;
import io.hops.hopsworks.common.provenance.ops.dto.ProvLinksDTO;
import io.hops.hopsworks.exceptions.DatasetException;
import io.hops.hopsworks.exceptions.FeatureStoreMetadataException;
import io.hops.hopsworks.exceptions.FeaturestoreException;
import io.hops.hopsworks.exceptions.GenericException;
import io.hops.hopsworks.exceptions.MetadataException;
import io.hops.hopsworks.exceptions.ModelRegistryException;
import io.hops.hopsworks.exceptions.ProjectException;
import io.hops.hopsworks.exceptions.ProvenanceException;
import io.hops.hopsworks.exceptions.ServiceException;
import io.hops.hopsworks.jwt.annotation.JWTRequired;
import io.hops.hopsworks.persistence.entity.dataset.DatasetType;
import io.hops.hopsworks.persistence.entity.featurestore.featureview.FeatureView;
import io.hops.hopsworks.persistence.entity.featurestore.trainingdataset.TrainingDataset;
import io.hops.hopsworks.persistence.entity.project.Project;
import io.hops.hopsworks.persistence.entity.user.Users;
import io.hops.hopsworks.persistence.entity.user.security.apiKey.ApiScope;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;

import javax.ejb.EJB;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.BeanParam;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.SecurityContext;
import javax.ws.rs.core.UriInfo;
import java.io.IOException;

@RequestScoped
@TransactionAttribute(TransactionAttributeType.NEVER)
@Api(value = "Training Dataset Explicit Provenance Resource")
public class TrainingDatasetProvenanceResource extends TrainingDatasetSubResource {
  @EJB
  private TrainingDatasetController trainingDatasetController;

  @Inject
  private ProvExplicitControllerIface provCtrl;
  @EJB
  private ProvExplicitLinksBuilder linksBuilder;
  @EJB
  private ProvUsageBuilder usageBuilder;
  @EJB
  private JWTHelper jwtHelper;
  @EJB
  private DatasetHelper datasetHelper;
  @EJB
  private FeaturestoreController featurestoreController;
  @EJB
  private FeatureViewController featureViewController;
  @EJB
  private ProjectController projectController;

  @Override
  protected ProjectController getProjectController() {
    return projectController;
  }

  @Override
  protected FeaturestoreController getFeaturestoreController() {
    return featurestoreController;
  }
  @Override
  protected FeatureViewController getFeatureViewController() {
    return featureViewController;
  }

  @Override
  protected TrainingDatasetController getTrainingDatasetController() {
    return trainingDatasetController;
  }

  private DatasetPath getTrainingDatasetPath(Project project, FeatureView featureView)
      throws FeaturestoreException, DatasetException {
    TrainingDataset trainingDataset
      = trainingDatasetController.getTrainingDatasetByFeatureViewAndVersion(featureView,
      getTrainingDatasetVersion());
    return datasetHelper.getDatasetPath(project, trainingDataset.getTagPath(), DatasetType.DATASET);
  }

  @GET
  @Path("links")
  @Produces(MediaType.APPLICATION_JSON)
  @AllowedProjectRoles({AllowedProjectRoles.DATA_SCIENTIST, AllowedProjectRoles.DATA_OWNER})
  @JWTRequired(acceptedTokens = {Audience.API, Audience.JOB},
      allowedUserRoles = {"HOPS_ADMIN", "HOPS_USER", "HOPS_SERVICE_USER"})
  @ApiKeyRequired(acceptedScopes = {ApiScope.FEATURESTORE},
      allowedUserRoles = {"HOPS_ADMIN", "HOPS_USER", "HOPS_SERVICE_USER"})
  @ApiOperation(value = "Links Provenance query endpoint",
      response = ProvLinksDTO.class)
  public Response getLinks(
      @BeanParam ProvLinksBeanParams params,
      @BeanParam LinksPagination pagination,
      @BeanParam ExplicitProvenanceExpansionBeanParam explicitProvenanceExpansionBeanParam,
      @Context UriInfo uriInfo,
      @Context HttpServletRequest req,
      @Context SecurityContext sc)
      throws GenericException, FeaturestoreException, DatasetException, ServiceException, MetadataException,
      FeatureStoreMetadataException, IOException, ModelRegistryException, ProjectException {
    Users user = jwtHelper.getUserPrincipal(sc);
    Project project = getProject();
    ResourceRequest resourceRequest = new ResourceRequest(ResourceRequest.Name.PROVENANCE);
    resourceRequest.setExpansions(explicitProvenanceExpansionBeanParam.getResources());
    TrainingDataset td
      = trainingDatasetController.getTrainingDatasetByFeatureViewAndVersion(getFeatureView(project),
      getTrainingDatasetId());
    ProvExplicitLink<TrainingDataset> provenance
        = provCtrl.trainingDatasetLinks(project, td, pagination.getUpstreamLvls(), pagination.getDownstreamLvls());
    ProvExplicitLinkDTO<?> result = linksBuilder.build(uriInfo, resourceRequest, project, user, provenance);
    return Response.ok().entity(result).build();
  }

  @GET
  @Path("usage")
  @Produces(MediaType.APPLICATION_JSON)
  @AllowedProjectRoles({AllowedProjectRoles.DATA_SCIENTIST, AllowedProjectRoles.DATA_OWNER})
  @JWTRequired(acceptedTokens = {Audience.API}, allowedUserRoles = {"HOPS_ADMIN", "HOPS_USER"})
  @ApiOperation(value = "Artifact usage", response = ProvArtifactUsageParentDTO.class)
  public Response status(@BeanParam ProvUsageBeanParams params,
                         @Context UriInfo uriInfo,
                         @Context HttpServletRequest req,
                         @Context SecurityContext sc)
      throws ProvenanceException, GenericException, DatasetException, MetadataException, FeatureStoreMetadataException,
      FeaturestoreException, ProjectException {
    Users user = jwtHelper.getUserPrincipal(sc);
    Project project = getProject();
    FeatureView featureView = getFeatureView(project);
    TrainingDataset trainingDataset
        = trainingDatasetController.getTrainingDatasetByFeatureViewAndVersion(featureView, getTrainingDatasetId());
    String tdProvenanceId = trainingDataset.getName() + "_" + trainingDataset.getVersion();
    ProvArtifactUsageParentDTO status =
      usageBuilder.buildAccessible(uriInfo, user, getTrainingDatasetPath(project, featureView),
        tdProvenanceId, params.getUsageType());
    return Response.ok().entity(status).build();
  }
}
