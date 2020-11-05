/*
 * Copyright (C) 2020, Logical Clocks AB. All rights reserved
 */

package io.hops.hopsworks.api.integrations;

import io.hops.hopsworks.api.filter.Audience;
import io.hops.hopsworks.api.integrations.databricks.DatabricksResource;
import io.hops.hopsworks.api.integrations.spark.SparkResource;
import io.hops.hopsworks.audit.logger.LogLevel;
import io.hops.hopsworks.audit.logger.annotation.Logged;
import io.hops.hopsworks.common.dao.project.ProjectFacade;
import io.hops.hopsworks.jwt.annotation.JWTRequired;
import io.hops.hopsworks.persistence.entity.project.Project;
import io.swagger.annotations.Api;

import javax.ejb.EJB;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;
import javax.ws.rs.Path;

@Logged
@RequestScoped
@JWTRequired(acceptedTokens = {Audience.API}, allowedUserRoles = {"HOPS_ADMIN", "HOPS_USER"})
@Api(value = "Integration resource")
@TransactionAttribute(TransactionAttributeType.NEVER)
public class IntegrationsResource {

  @Inject
  private DatabricksResource databricksResource;
  @Inject
  private SparkResource sparkResource;
  @EJB
  private ProjectFacade projectFacade;

  private Project project;

  @Logged(logLevel = LogLevel.OFF)
  public void setProjectId(Integer id) {
    this.project = projectFacade.find(id);
  }

  @Path("/databricks")
  @Logged(logLevel = LogLevel.OFF)
  public DatabricksResource databricksResource(){
    databricksResource.setProject(project);
    return databricksResource;
  }

  @Path("/spark")
  @Logged(logLevel = LogLevel.OFF)
  public SparkResource sparkResource(){
    sparkResource.setProject(project);
    return sparkResource;
  }
}
