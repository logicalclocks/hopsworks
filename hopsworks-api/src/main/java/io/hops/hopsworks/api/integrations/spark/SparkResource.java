/*
 * Copyright (C) 2020, Logical Clocks AB. All rights reserved
 */

package io.hops.hopsworks.api.integrations.spark;

import io.hops.hopsworks.api.filter.AllowedProjectRoles;
import io.hops.hopsworks.api.filter.Audience;
import io.hops.hopsworks.api.filter.JWTNotRequired;
import io.hops.hopsworks.api.jwt.JWTHelper;
import io.hops.hopsworks.api.util.RESTApiJsonResponse;
import io.hops.hopsworks.audit.logger.LogLevel;
import io.hops.hopsworks.audit.logger.annotation.Logged;
import io.hops.hopsworks.common.util.HopsUtils;
import io.hops.hopsworks.common.util.Settings;
import io.hops.hopsworks.exceptions.ServiceException;
import io.hops.hopsworks.jwt.annotation.JWTRequired;
import io.hops.hopsworks.jwt.exception.SigningKeyNotFoundException;
import io.hops.hopsworks.jwt.exception.VerificationException;
import io.hops.hopsworks.persistence.entity.project.Project;
import io.hops.hopsworks.persistence.entity.user.Users;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;

import javax.ejb.EJB;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.enterprise.context.RequestScoped;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.SecurityContext;
import javax.ws.rs.core.UriInfo;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.nio.file.Paths;

@Logged
@RequestScoped
@Api(value = "Spark integration resource")
@TransactionAttribute(TransactionAttributeType.NEVER)
public class SparkResource {

  @EJB
  private JWTHelper jwtHelper;
  @EJB
  private Settings settings;
  @EJB
  private SparkConfigurationBuilder sparkConfigurationBuilder;

  private final static String CLIENT_TOKEN_ISSUER = "INVALID/client.tar.gz";
  private final static String CLIENTS_FILE_NAME = "clients.tar.gz";

  private Project project;

  @Logged(logLevel = LogLevel.OFF)
  public void setProject(Project project) {
    this.project = project;
  }

  @GET
  @Path("client/configuration")
  @Produces(MediaType.APPLICATION_JSON)
  @ApiOperation(value = "Get Spark configuration for external clusters", response = SparkConfigurationDTO.class)
  @AllowedProjectRoles({AllowedProjectRoles.DATA_SCIENTIST, AllowedProjectRoles.DATA_OWNER})
  @JWTRequired(acceptedTokens = {Audience.API}, allowedUserRoles = {"HOPS_ADMIN", "HOPS_USER"})
  public Response getConfiguration(@Context UriInfo uriInfo, @Context SecurityContext sc) throws ServiceException {
    SparkConfigurationDTO sparkConfigurationDTO = sparkConfigurationBuilder.build(uriInfo, project);
    return Response.ok().entity(sparkConfigurationDTO).build();
  }

  @GET
  @Path("client/token")
  @Produces(MediaType.APPLICATION_JSON)
  @ApiOperation(value = "Get a one time download token for the client", response = RESTApiJsonResponse.class)
  @AllowedProjectRoles({AllowedProjectRoles.DATA_SCIENTIST, AllowedProjectRoles.DATA_OWNER})
  @JWTRequired(acceptedTokens = {Audience.API}, allowedUserRoles = {"HOPS_ADMIN", "HOPS_USER"})
  public Response clientToken(@Context SecurityContext sc) {
    Users user = jwtHelper.getUserPrincipal(sc);
    RESTApiJsonResponse response = new RESTApiJsonResponse();
    response.setData(jwtHelper.createOneTimeToken(user, CLIENT_TOKEN_ISSUER, null));
    return Response.ok().entity(response).build();
  }

  @GET
  @Path("client/download")
  @Produces({MediaType.APPLICATION_OCTET_STREAM, MediaType.APPLICATION_JSON})
  @ApiOperation(value = "Download the client jars")
  @JWTNotRequired
  public Response client(@QueryParam("token") String token, @Context SecurityContext sc)
      throws FileNotFoundException, SigningKeyNotFoundException, VerificationException {
    jwtHelper.verifyOneTimeToken(token, CLIENT_TOKEN_ISSUER);
    InputStream stream = new FileInputStream(Paths.get(settings.getClientPath(), CLIENTS_FILE_NAME).toFile());
    Response.ResponseBuilder response = Response.ok(HopsUtils.buildOutputStream(stream));
    response.header("Content-disposition", "attachment; filename=\"clients.tar.gz\"");
    return response.build();
  }
}
