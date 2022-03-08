/*
 * This file is part of Hopsworks
 * Copyright (C) 2021, Logical Clocks AB. All rights reserved
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

package io.hops.hopsworks.api.featurestore.featureview;

import io.hops.hopsworks.api.featurestore.FeaturestoreKeywordResource;
import io.hops.hopsworks.api.featurestore.activities.ActivityResource;
import io.hops.hopsworks.api.featurestore.preparestatement.PreparedStatementResource;
import io.hops.hopsworks.api.featurestore.query.QueryResource;
import io.hops.hopsworks.api.featurestore.tag.TagResource;
import io.hops.hopsworks.api.featurestore.trainingdataset.TrainingDatasetResource;
import io.hops.hopsworks.api.featurestore.transformation.TransformationResource;
import io.hops.hopsworks.audit.logger.LogLevel;
import io.hops.hopsworks.audit.logger.annotation.Logged;
import io.hops.hopsworks.common.featurestore.FeaturestoreController;
import io.hops.hopsworks.common.featurestore.FeaturestoreDTO;
import io.hops.hopsworks.exceptions.FeaturestoreException;
import io.hops.hopsworks.persistence.entity.featurestore.Featurestore;
import io.hops.hopsworks.persistence.entity.project.Project;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiParam;

import javax.ejb.EJB;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;

@Logged
@RequestScoped
@TransactionAttribute(TransactionAttributeType.NEVER)
@Api(value = "Feature View service")
public class FeatureViewService {

  @Inject
  private FeatureViewResource featureViewResource;
  @Inject
  private TrainingDatasetResource trainingDatasetResource;
  @Inject
  private TagResource tagResource;
  @Inject
  private QueryResource queryResource;
  @Inject
  private TransformationResource transformationResource;
  @Inject
  private PreparedStatementResource prepareStatementResource;
  @Inject
  private FeaturestoreKeywordResource featurestoreKeywordResource;
  @EJB
  private FeaturestoreController featurestoreController;
  @Inject
  private ActivityResource activityResource;
  private Project project;
  private Featurestore featurestore;

  @Path("")
  @Logged(logLevel = LogLevel.OFF)
  public FeatureViewResource featureViewResource() {
    featureViewResource.setFeaturestore(featurestore);
    featureViewResource.setProject(project);
    return featureViewResource;
  }

  @Path("/{name: [a-z0-9_]*(?=[a-z])[a-z0-9_]+}/version/{version: [0-9]+}/td")
  @Logged(logLevel = LogLevel.OFF)
  public TrainingDatasetResource trainingDatasetResource(
      @ApiParam(value = "Name of the feature view", required = true)
      @PathParam("name")
          String featureViewName,
      @ApiParam(value = "Version of the feature view", required = true)
      @PathParam("version")
          Integer version
  ) {
    trainingDatasetResource.setFeatureView(featureViewName, version);
    return trainingDatasetResource;
  }

  @Path("/{name: [a-z0-9_]*(?=[a-z])[a-z0-9_]+}/version/{version: [0-9]+}/tags")
  @Logged(logLevel = LogLevel.OFF)
  public TagResource tagResource(
      @ApiParam(value = "Name of the feature view", required = true)
      @PathParam("name")
          String featureViewName,
      @ApiParam(value = "Version of the feature view", required = true)
      @PathParam("version")
          Integer version
  ) {
    tagResource.setFeatureView(featureViewName, version);
    return tagResource;
  }

  @Path("/{name: [a-z0-9_]*(?=[a-z])[a-z0-9_]+}/version/{version: [0-9]+}/query")
  @Logged(logLevel = LogLevel.OFF)
  public QueryResource query(
      @ApiParam(value = "Name of the feature view", required = true)
      @PathParam("name")
          String featureViewName,
      @ApiParam(value = "Version of the feature view", required = true)
      @PathParam("version")
          Integer version
  ) {
    queryResource.setFeatureView(featureViewName, version);
    return queryResource;
  }

  @Path("/{name: [a-z0-9_]*(?=[a-z])[a-z0-9_]+}/version/{version: [0-9]+}/keywords")
  @Logged(logLevel = LogLevel.OFF)
  public FeaturestoreKeywordResource keywords(
      @ApiParam(value = "Name of the feature view", required = true)
      @PathParam("name")
          String featureViewName,
      @ApiParam(value = "Version of the feature view", required = true)
      @PathParam("version")
          Integer version
  ) {
    return featurestoreKeywordResource;
  }

  @Path("/{name: [a-z0-9_]*(?=[a-z])[a-z0-9_]+}/version/{version: [0-9]+}/activity")
  @Logged(logLevel = LogLevel.OFF)
  public ActivityResource activity(
      @ApiParam(value = "Id of the feature view", required = true)
      @PathParam("name")
          String featureViewName,
      @ApiParam(value = "Version of the feature view", required = true)
      @PathParam("version")
          Integer version
  ) {
    return this.activityResource;
  }

  @Path("/{name: [a-z0-9_]*(?=[a-z])[a-z0-9_]+}/version/{version: [0-9]+}/transformation")
  @Logged(logLevel = LogLevel.OFF)
  public TransformationResource transformation(
      @ApiParam(value = "Name of the feature view", required = true)
      @PathParam("name")
          String featureViewName,
      @ApiParam(value = "Version of the feature view", required = true)
      @PathParam("version")
          Integer version
  ) {
    return transformationResource;
  }

  @Path("/{name: [a-z0-9_]*(?=[a-z])[a-z0-9_]+}/version/{version: [0-9]+}/preparedStatement")
  @Logged(logLevel = LogLevel.OFF)
  public PreparedStatementResource preparedStatement(
      @ApiParam(value = "Name of the feature view", required = true)
      @PathParam("name")
          String featureViewName,
      @ApiParam(value = "Version of the feature view", required = true)
      @PathParam("version")
          Integer version
  ) {
    return prepareStatementResource;
  }

  @Logged(logLevel = LogLevel.OFF)
  public void setProject(Project project) {
    this.project = project;
  }

  @Logged(logLevel = LogLevel.OFF)
  public void setFeaturestore(Integer id) throws FeaturestoreException {
    FeaturestoreDTO featurestoreDTO = featurestoreController.getFeaturestoreForProjectWithId(project, id);
    this.featurestore = featurestoreController.getFeaturestoreWithId(featurestoreDTO.getFeaturestoreId());
  }
}
