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

package io.hops.hopsworks.api.featurestore;

import io.hops.hopsworks.api.featurestore.json.datavalidation.ConstraintGroupDTO;
import io.hops.hopsworks.api.featurestore.json.datavalidation.DataValidationSettingsDTO;
import io.hops.hopsworks.api.filter.AllowedProjectRoles;
import io.hops.hopsworks.api.filter.Audience;
import io.hops.hopsworks.api.filter.apiKey.ApiKeyRequired;
import io.hops.hopsworks.api.jwt.JWTHelper;
import io.hops.hopsworks.common.featurestore.FeaturestoreFacade;
import io.hops.hopsworks.common.featurestore.datavalidation.ConstraintGroup;
import io.hops.hopsworks.common.featurestore.datavalidation.DataValidationController;
import io.hops.hopsworks.common.featurestore.datavalidation.ValidationResult;
import io.hops.hopsworks.common.featurestore.featuregroup.FeaturegroupController;
import io.hops.hopsworks.common.featurestore.featuregroup.FeaturegroupDTO;
import io.hops.hopsworks.common.util.Settings;
import io.hops.hopsworks.exceptions.FeaturestoreException;
import io.hops.hopsworks.jwt.annotation.JWTRequired;
import io.hops.hopsworks.persistence.entity.featurestore.Featurestore;
import io.hops.hopsworks.persistence.entity.user.Users;
import io.hops.hopsworks.persistence.entity.user.security.apiKey.ApiScope;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;

import javax.annotation.PostConstruct;
import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.SecurityContext;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

@Api(value = "Feature store data validation service")
@Stateless
@TransactionAttribute(TransactionAttributeType.NEVER)
@JWTRequired(acceptedTokens={Audience.API}, allowedUserRoles={"HOPS_ADMIN", "HOPS_USER"})
@ApiKeyRequired( acceptedScopes = {ApiScope.FEATURESTORE}, allowedUserRoles = {"HOPS_ADMIN", "HOPS_USER"})
@AllowedProjectRoles({AllowedProjectRoles.DATA_OWNER, AllowedProjectRoles.DATA_SCIENTIST})
public class DataValidationResource {
  private static Logger LOGGER = Logger.getLogger(DataValidationResource.class.getName());
  private static final String HOPS_VERIFICATION_JAR_TEMPLATE = "hdfs:///user" + org.apache.hadoop.fs.Path.SEPARATOR
      + "%s" + org.apache.hadoop.fs.Path.SEPARATOR + "hops-verification-assembly-%s.jar";
  
  @EJB
  private FeaturestoreFacade featurestoreFacade;
  @EJB
  private FeaturegroupController featuregroupController;
  @EJB
  private DataValidationController dataValidationController;
  @EJB
  private JWTHelper jwtHelper;
  @EJB
  private Settings settings;
  
  private Featurestore featurestore;
  private String path2hopsverification;
  
  public DataValidationResource setFeatureStore(Integer featureStoreId) {
    this.featurestore = featurestoreFacade.findById(featureStoreId);
    return this;
  }
  
  @PostConstruct
  public void init() {
    path2hopsverification = String.format(HOPS_VERIFICATION_JAR_TEMPLATE, settings.getSparkUser(),
        settings.getHopsVerificationVersion());
  }
  
  @ApiOperation(value = "Write Deequ validation rules to Filesystem so validation job can pick it up",
    response = DataValidationSettingsDTO.class)
  @POST
  @Path("{featuregroupId}/rules")
  @Produces(MediaType.APPLICATION_JSON)
  public Response addValidationRules(ConstraintGroupDTO constraintGroups,
      @PathParam("featuregroupId") Integer featureGroupId,
      @Context SecurityContext sc) throws FeaturestoreException {
    Users user = jwtHelper.getUserPrincipal(sc);
    FeaturegroupDTO featureGroup = featuregroupController.getFeaturegroupWithIdAndFeaturestore(featurestore,
        featureGroupId);
  
    String rulesPath = dataValidationController.writeRulesToFile(user, featurestore.getProject(), featureGroup,
        constraintGroups.toConstraintGroups());
    
    DataValidationSettingsDTO settings = new DataValidationSettingsDTO();
    settings.setValidationRulesPath(rulesPath);
    settings.setExecutablePath(path2hopsverification);
    settings.setExecutableMainClass(dataValidationController.getHopsVerificationMainClass(
        new org.apache.hadoop.fs.Path(path2hopsverification)));
    LOGGER.log(Level.FINE, "Validation settings: " + settings);
    return Response.ok().entity(settings).build();
  }
  
  @ApiOperation(value = "Get previously stored Deequ validation rules", response = ConstraintGroupDTO.class)
  @GET
  @Path("{featuregroupId}/rules")
  @Produces(MediaType.APPLICATION_JSON)
  public Response getValidationRules(@PathParam("featuregroupId") Integer featureGroupId,
      @Context SecurityContext sc) throws FeaturestoreException {
    Users user = jwtHelper.getUserPrincipal(sc);
    FeaturegroupDTO featureGroup = featuregroupController.getFeaturegroupWithIdAndFeaturestore(featurestore,
        featureGroupId);
    List<ConstraintGroup> constraintGroups = dataValidationController.readRulesForFeatureGroup(user,
        featurestore.getProject(), featureGroup);
    ConstraintGroupDTO response = ConstraintGroupDTO.fromConstraintGroups(constraintGroups);
    return Response.ok(response).build();
  }
  
  @ApiOperation(value = "Fetch the result of a Deequ data validation job", response = ValidationResult.class)
  @GET
  @Path("{featuregroupId}/result")
  @Produces(MediaType.APPLICATION_JSON)
  public Response getValidationResult(@PathParam("featuregroupId") Integer featureGroupId,
      @Context SecurityContext sc) throws FeaturestoreException {
    Users user = jwtHelper.getUserPrincipal(sc);
    FeaturegroupDTO featureGroup = featuregroupController.getFeaturegroupWithIdAndFeaturestore(featurestore,
        featureGroupId);
    ValidationResult result = dataValidationController.getValidationResultForFeatureGroup(user,
        featurestore.getProject(), featureGroup);
    return Response.ok(result).build();
  }
}
