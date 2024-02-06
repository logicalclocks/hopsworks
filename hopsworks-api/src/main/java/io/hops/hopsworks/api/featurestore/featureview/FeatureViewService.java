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
import io.hops.hopsworks.api.featurestore.statistics.StatisticsResource;
import io.hops.hopsworks.api.featurestore.tag.FeatureViewTagResource;
import io.hops.hopsworks.api.featurestore.trainingdataset.TrainingDatasetResource;
import io.hops.hopsworks.api.featurestore.transformation.TransformationResource;
import io.hops.hopsworks.api.provenance.FeatureViewProvenanceResource;
import io.hops.hopsworks.common.featurestore.FeaturestoreController;
import io.hops.hopsworks.common.util.Settings;
import io.hops.hopsworks.exceptions.FeaturestoreException;
import io.hops.hopsworks.persistence.entity.featurestore.Featurestore;
import io.hops.hopsworks.persistence.entity.project.Project;
import io.hops.hopsworks.restutils.RESTCodes;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiParam;
import java.util.logging.Level;

import javax.ejb.EJB;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;

@RequestScoped
@TransactionAttribute(TransactionAttributeType.NEVER)
@Api(value = "Feature View service")
public class FeatureViewService {

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
  private FeaturestoreKeywordResource featurestoreKeywordResource;
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

  private Project project;
  private Featurestore featurestore;

  @Path("")
  public FeatureViewResource featureViewResource() {
    this.featureViewResource.setProject(project);
    this.featureViewResource.setFeaturestore(featurestore);
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
  ) throws FeaturestoreException {
    this.trainingDatasetResource.setProject(project);
    this.trainingDatasetResource.setFeaturestore(featurestore);
    this.trainingDatasetResource.setFeatureView(featureViewName, version);
    return this.trainingDatasetResource;
  }

  @Path("/{name: [a-z0-9_]*(?=[a-z])[a-z0-9_]+}/version/{version: [0-9]+}/query")
  public QueryResource query(
      @ApiParam(value = "Name of the feature view", required = true)
      @PathParam("name")
          String featureViewName,
      @ApiParam(value = "Version of the feature view", required = true)
      @PathParam("version")
          Integer version
  ) throws FeaturestoreException {
    this.queryResource.setProject(project);
    this.queryResource.setFeaturestore(featurestore);
    this.queryResource.setFeatureView(featureViewName, version);
    return this.queryResource;
  }

  @Path("/{name: [a-z0-9_]*(?=[a-z])[a-z0-9_]+}/version/{version: [0-9]+}/keywords")
  public FeaturestoreKeywordResource keywords(
      @ApiParam(value = "Name of the feature view", required = true)
      @PathParam("name")
          String featureViewName,
      @ApiParam(value = "Version of the feature view", required = true)
      @PathParam("version")
          Integer version
  ) throws FeaturestoreException {
    this.featurestoreKeywordResource.setProject(project);
    this.featurestoreKeywordResource.setFeaturestore(featurestore);
    this.featurestoreKeywordResource.setFeatureView(featureViewName, version);
    return this.featurestoreKeywordResource;
  }

  @Path("/{name: [a-z0-9_]*(?=[a-z])[a-z0-9_]+}/version/{version: [0-9]+}/activity")
  public ActivityResource activity(
      @ApiParam(value = "Id of the feature view", required = true)
      @PathParam("name")
          String featureViewName,
      @ApiParam(value = "Version of the feature view", required = true)
      @PathParam("version")
          Integer version
  ) throws FeaturestoreException {
    this.activityResource.setProject(project);
    this.activityResource.setFeaturestore(featurestore);
    this.activityResource.setFeatureViewByNameAndVersion(featureViewName, version);
    return this.activityResource;
  }

  @Path("/{name: [a-z0-9_]*(?=[a-z])[a-z0-9_]+}/version/{version: [0-9]+}/transformation")
  public TransformationResource transformation(
      @ApiParam(value = "Name of the feature view", required = true)
      @PathParam("name") String featureViewName,
      @ApiParam(value = "Version of the feature view", required = true)
      @PathParam("version")
          Integer version
  ) throws FeaturestoreException {
    this.transformationResource.setProject(project);
    this.transformationResource.setFeaturestore(featurestore);
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
  ) throws FeaturestoreException {
    this.preparedStatementResource.setProject(project);
    this.preparedStatementResource.setFeatureStore(featurestore);
    this.preparedStatementResource.setFeatureView(featureViewName, version);
    return this.preparedStatementResource;
  }

  public void setProject(Project project) {
    this.project = project;
  }

  public void setFeaturestore(Integer featurestoreId) throws FeaturestoreException {
    //This call verifies that the project have access to the featurestoreId provided
    this.featurestore = featurestoreController.getFeaturestoreForProjectWithId(project, featurestoreId);
  }
  
  @Path("/{name: [a-z0-9_]*(?=[a-z])[a-z0-9_]+}/version/{version: [0-9]+}/provenance")
  public FeatureViewProvenanceResource provenance(
    @PathParam("name") String name,
    @PathParam("version") Integer version) {
    this.provenanceResource.setProject(project);
    this.provenanceResource.setFeatureStore(featurestore);
    this.provenanceResource.setFeatureViewName(name);
    this.provenanceResource.setFeatureViewVersion(version);
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
    this.statisticsResource.setProject(project);
    this.statisticsResource.setFeaturestore(featurestore);
    this.statisticsResource.setFeatureViewByNameAndVersion(name, version);
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
    this.featureMonitoringConfigurationResource.setProject(project);
    this.featureMonitoringConfigurationResource.setFeatureStore(featurestore);
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
    this.featureMonitoringResultResource.setProject(project);
    this.featureMonitoringResultResource.setFeatureStore(featurestore);
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
    featureViewFeatureMonitorAlertResource.setFeatureStore(featurestore);
    featureViewFeatureMonitorAlertResource.setFeatureView(featureViewName, version, featurestore);
    featureViewFeatureMonitorAlertResource.setProject(project);
    return featureViewFeatureMonitorAlertResource;
  }
  
}
