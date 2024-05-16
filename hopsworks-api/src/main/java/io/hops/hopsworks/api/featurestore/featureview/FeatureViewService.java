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

import io.hops.hopsworks.api.featurestore.FeaturestoreSubResource;
import io.hops.hopsworks.api.featurestore.activities.ActivityResource;
import io.hops.hopsworks.api.featurestore.keyword.FeatureViewKeywordResource;
import io.hops.hopsworks.api.featurestore.preparestatement.PreparedStatementResource;
import io.hops.hopsworks.api.featurestore.query.QueryResource;
import io.hops.hopsworks.api.featurestore.statistics.StatisticsResource;
import io.hops.hopsworks.api.featurestore.tag.FeatureViewTagResource;
import io.hops.hopsworks.api.featurestore.trainingdataset.TrainingDatasetResource;
import io.hops.hopsworks.api.featurestore.transformation.TransformationResource;
import io.hops.hopsworks.api.provenance.FeatureViewProvenanceResource;
import io.hops.hopsworks.common.featurestore.FeaturestoreController;
import io.hops.hopsworks.common.project.ProjectController;
import io.hops.hopsworks.common.util.Settings;
import io.hops.hopsworks.exceptions.FeaturestoreException;
import io.hops.hopsworks.restutils.RESTCodes;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiParam;

import javax.ejb.EJB;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import java.util.logging.Level;

@RequestScoped
@TransactionAttribute(TransactionAttributeType.NEVER)
@Api(value = "Feature View service")
public class FeatureViewService extends FeaturestoreSubResource {

  @Inject
  private FeatureViewResource featureViewResource;
  @Inject
  private TrainingDatasetResource trainingDatasetResource;
  @Inject
  private FeatureViewTagResource tagResource;
  @Inject
  private QueryResource queryResource;
  @Inject
  private TransformationResource transformationResource;
  @Inject
  private PreparedStatementResource preparedStatementResource;
  @Inject
  private FeatureViewKeywordResource featureViewKeywordResource;
  @EJB
  private FeaturestoreController featurestoreController;
  @Inject
  private ActivityResource activityResource;
  @Inject
  private FeatureViewProvenanceResource provenanceResource;
  @Inject
  private StatisticsResource statisticsResource;
  @Inject
  private FeatureViewFeatureMonitoringConfigurationResource featureMonitoringConfigurationResource;
  @Inject
  private FeatureViewFeatureMonitoringResultResource featureMonitoringResultResource;
  @EJB
  private Settings settings;
  @Inject
  private FeatureViewFeatureMonitorAlertResource featureViewFeatureMonitorAlertResource;
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

  @Path("")
  public FeatureViewResource featureViewResource() {
    this.featureViewResource.setProjectId(getProjectId());
    this.featureViewResource.setFeaturestoreId(getFeaturestoreId());
    return this.featureViewResource;
  }

  @Path("/{name: [a-z0-9_]*(?=[a-z])[a-z0-9_]+}/version/{version: [0-9]+}/trainingdatasets")
  public TrainingDatasetResource trainingDatasetResource(
      @ApiParam(value = "Name of the feature view", required = true)
      @PathParam("name")
          String featureViewName,
      @ApiParam(value = "Version of the feature view", required = true)
      @PathParam("version")
          Integer version
  ) {
    this.trainingDatasetResource.setProjectId(getProjectId());
    this.trainingDatasetResource.setFeaturestoreId(getFeaturestoreId());
    this.trainingDatasetResource.setFeatureView(featureViewName, version);
    return this.trainingDatasetResource;
  }
  
  @Path("/{name: [a-z0-9_]*(?=[a-z])[a-z0-9_]+}/version/{version: [0-9]+}/tags")
  public FeatureViewTagResource tags(
      @ApiParam(value = "Name of the feature view", required = true)
      @PathParam("name")
          String featureViewName,
      @ApiParam(value = "Version of the feature view", required = true)
      @PathParam("version")
          Integer version
  ) {
    this.tagResource.setProjectId(getProjectId());
    this.tagResource.setFeaturestoreId(getFeaturestoreId());
    this.tagResource.setFeatureView(featureViewName, version);
    return this.tagResource;
  }
  
  @Path("/{name: [a-z0-9_]*(?=[a-z])[a-z0-9_]+}/version/{version: [0-9]+}/query")
  public QueryResource query(
      @ApiParam(value = "Name of the feature view", required = true)
      @PathParam("name")
          String featureViewName,
      @ApiParam(value = "Version of the feature view", required = true)
      @PathParam("version")
          Integer version
  ) {
    this.queryResource.setProjectId(getProjectId());
    this.queryResource.setFeaturestoreId(getFeaturestoreId());
    this.queryResource.setFeatureView(featureViewName, version);
    return this.queryResource;
  }
  
  @Path("/{name: [a-z0-9_]*(?=[a-z])[a-z0-9_]+}/version/{version: [0-9]+}/keywords")
  public FeatureViewKeywordResource keywords(
      @ApiParam(value = "Name of the feature view", required = true)
      @PathParam("name")
          String featureViewName,
      @ApiParam(value = "Version of the feature view", required = true)
      @PathParam("version")
          Integer version
  ) {
    this.featureViewKeywordResource.setProjectId(getProjectId());
    this.featureViewKeywordResource.setFeaturestoreId(getFeaturestoreId());
    this.featureViewKeywordResource.setFeatureView(featureViewName, version);
    return this.featureViewKeywordResource;
  }
  
  @Path("/{name: [a-z0-9_]*(?=[a-z])[a-z0-9_]+}/version/{version: [0-9]+}/activity")
  public ActivityResource activity(
      @ApiParam(value = "Id of the feature view", required = true)
      @PathParam("name")
          String featureViewName,
      @ApiParam(value = "Version of the feature view", required = true)
      @PathParam("version")
          Integer version
  ) {
    this.activityResource.setProjectId(getProjectId());
    this.activityResource.setFeaturestoreId(getFeaturestoreId());
    this.activityResource.setFeatureView(featureViewName, version);
    return this.activityResource;
  }

  @Path("/{name: [a-z0-9_]*(?=[a-z])[a-z0-9_]+}/version/{version: [0-9]+}/transformation")
  public TransformationResource transformation(
      @ApiParam(value = "Name of the feature view", required = true)
      @PathParam("name") String featureViewName,
      @ApiParam(value = "Version of the feature view", required = true)
      @PathParam("version")
          Integer version
  ) {
    this.transformationResource.setProjectId(getProjectId());
    this.transformationResource.setFeaturestoreId(getFeaturestoreId());
    this.transformationResource.setFeatureView(featureViewName, version);
    return this.transformationResource;
  }
  
  @Path("/{name: [a-z0-9_]*(?=[a-z])[a-z0-9_]+}/version/{version: [0-9]+}/preparedstatement")
  public PreparedStatementResource preparedStatement(
      @ApiParam(value = "Name of the feature view", required = true)
      @PathParam("name")
          String featureViewName,
      @ApiParam(value = "Version of the feature view", required = true)
      @PathParam("version")
          Integer version
  ) {
    this.preparedStatementResource.setProjectId(getProjectId());
    this.preparedStatementResource.setFeaturestoreId(getFeaturestoreId());
    this.preparedStatementResource.setFeatureView(featureViewName, version);
    return this.preparedStatementResource;
  }

  @Path("/{name: [a-z0-9_]*(?=[a-z])[a-z0-9_]+}/version/{version: [0-9]+}/provenance")
  public FeatureViewProvenanceResource provenance(
    @PathParam("name") String name,
    @PathParam("version") Integer version) {
    this.provenanceResource.setProjectId(getProjectId());
    this.provenanceResource.setFeaturestoreId(getFeaturestoreId());
    this.provenanceResource.setFeatureView(name, version);
    return provenanceResource;
  }

  @Path("/{name: [a-z0-9_]*(?=[a-z])[a-z0-9_]+}/version/{version: [0-9]+}/statistics")
  public StatisticsResource statistics(
    @ApiParam(value = "Name of the feature view", required = true)
    @PathParam("name")
    String name,
    @ApiParam(value = "Version of the feature view", required = true)
    @PathParam("version")
    Integer version
  ) throws FeaturestoreException {
    this.statisticsResource.setProjectId(getProjectId());
    this.statisticsResource.setFeaturestoreId(getFeaturestoreId());
    this.statisticsResource.setFeatureView(name, version);
    return statisticsResource;
  }
  
  @Path("/{name: [a-z0-9_]*(?=[a-z])[a-z0-9_]+}/version/{version: [0-9]+}/featuremonitoring/config")
  public FeatureViewFeatureMonitoringConfigurationResource featureMonitoringConfigurationResource(
    @ApiParam(value = "Name of the feature view", required = true)
    @PathParam("name")
    String name,
    @ApiParam(value = "Version of the feature view", required = true)
    @PathParam("version")
    Integer version
  ) throws FeaturestoreException {
    if (!settings.isFeatureMonitoringEnabled()) {
      throw new FeaturestoreException(
        RESTCodes.FeaturestoreErrorCode.FEATURE_MONITORING_NOT_ENABLED,
        Level.FINE
      );
    }
    this.featureMonitoringConfigurationResource.setProjectId(getProjectId());
    this.featureMonitoringConfigurationResource.setFeaturestoreId(getFeaturestoreId());
    this.featureMonitoringConfigurationResource.setFeatureView(name, version);
    return this.featureMonitoringConfigurationResource;
  }
  
  @Path("/{name: [a-z0-9_]*(?=[a-z])[a-z0-9_]+}/version/{version: [0-9]+}/featuremonitoring/result")
  public FeatureViewFeatureMonitoringResultResource featureMonitoringResultResource(
    @ApiParam(value = "Name of the feature view", required = true)
    @PathParam("name")
    String name,
    @ApiParam(value = "Version of the feature view", required = true)
    @PathParam("version")
    Integer version
  ) throws FeaturestoreException {
    if (!settings.isFeatureMonitoringEnabled()) {
      throw new FeaturestoreException(
        RESTCodes.FeaturestoreErrorCode.FEATURE_MONITORING_NOT_ENABLED,
        Level.FINE
      );
    }
    this.featureMonitoringResultResource.setProjectId(getProjectId());
    this.featureMonitoringResultResource.setFeaturestoreId(getFeaturestoreId());
    this.featureMonitoringResultResource.setFeatureView(name, version);
    return this.featureMonitoringResultResource;
  }
  
  @Path("/{name: [a-z0-9_]*(?=[a-z])[a-z0-9_]+}/version/{version: [0-9]+}/alerts")
  public FeatureViewFeatureMonitorAlertResource featureViewFeatureMonitorAlertResource(
    @ApiParam(value = "Name of the feature view", required = true)
    @PathParam("name")
    String featureViewName,
    @ApiParam(value = "Version of the feature view", required = true)
    @PathParam("version")
    Integer version
  ) throws FeaturestoreException {
    if (!settings.isFeatureMonitoringEnabled()) {
      throw new FeaturestoreException(
        RESTCodes.FeaturestoreErrorCode.FEATURE_MONITORING_NOT_ENABLED,
        Level.FINE
      );
    }
    this.featureViewFeatureMonitorAlertResource.setProjectId(getProjectId());
    this.featureViewFeatureMonitorAlertResource.setFeaturestoreId(getFeaturestoreId());
    this.featureViewFeatureMonitorAlertResource.setFeatureView(featureViewName, version);
    return featureViewFeatureMonitorAlertResource;
  }
  
}
