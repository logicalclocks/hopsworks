/*
 * This file is part of Hopsworks
 * Copyright (C) 2018, Logical Clocks AB. All rights reserved
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

package io.hops.hopsworks.api.experiments.tensorboard;

import io.hops.hopsworks.api.filter.AllowedProjectRoles;
import io.hops.hopsworks.api.filter.Audience;
import io.hops.hopsworks.api.jwt.JWTHelper;
import io.hops.hopsworks.api.project.util.DsPath;
import io.hops.hopsworks.api.project.util.PathValidator;
import io.hops.hopsworks.common.dao.project.Project;
import io.hops.hopsworks.common.dao.tensorflow.config.TensorBoardDTO;
import io.hops.hopsworks.common.dao.user.Users;
import io.hops.hopsworks.exceptions.DatasetException;
import io.hops.hopsworks.exceptions.ProjectException;
import io.hops.hopsworks.exceptions.TensorBoardException;
import io.hops.hopsworks.restutils.RESTCodes;
import io.hops.hopsworks.common.experiments.tensorboard.TensorBoardController;
import io.hops.hopsworks.jwt.annotation.JWTRequired;
import io.swagger.annotations.ApiOperation;
import java.io.UnsupportedEncodingException;

import javax.ejb.EJB;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.enterprise.context.RequestScoped;
import javax.persistence.PersistenceException;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Produces;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.ws.rs.core.SecurityContext;
import javax.ws.rs.core.UriBuilder;
import javax.ws.rs.core.UriInfo;

@RequestScoped
@TransactionAttribute(TransactionAttributeType.NEVER)
public class TensorBoardResource {

  @EJB
  private TensorBoardController tensorBoardController;
  @EJB
  private PathValidator pathValidator;
  @EJB
  private JWTHelper jWTHelper;

  private Project project;
  private String experimentId;
  
  public TensorBoardResource setProject(Project project, String experimentId) {
    this.project = project;
    this.experimentId = experimentId;
    return this;
  }
  
  public Project getProject() {
    return project;
  }

  private final static Logger LOGGER = Logger.getLogger(TensorBoardResource.class.getName());

  @ApiOperation(value = "Get the TensorBoard", response = TensorBoardDTO.class)
  @GET
  @Produces(MediaType.APPLICATION_JSON)
  @AllowedProjectRoles({AllowedProjectRoles.DATA_OWNER, AllowedProjectRoles.DATA_SCIENTIST})
  @JWTRequired(acceptedTokens={Audience.API}, allowedUserRoles={"HOPS_ADMIN", "HOPS_USER"})
  public Response getTensorBoard(@Context SecurityContext sc) throws TensorBoardException {
    try {
      Users user = jWTHelper.getUserPrincipal(sc);
      TensorBoardDTO tbDTO = tensorBoardController.getTensorBoard(project, user);
      if(tbDTO == null) {
        throw new TensorBoardException(RESTCodes.TensorBoardErrorCode.TENSORBOARD_NOT_FOUND, Level.FINE);
      }
      return Response.ok().entity(tbDTO).build();
    } catch (PersistenceException pe) {
      throw new TensorBoardException(RESTCodes.TensorBoardErrorCode.TENSORBOARD_FETCH_ERROR, Level.SEVERE, null,
        pe.getMessage(), pe);
    }
  }

  @ApiOperation(value = "Start a new TensorBoard", response = TensorBoardDTO.class)
  @POST
  @Produces(MediaType.APPLICATION_JSON)
  @AllowedProjectRoles({AllowedProjectRoles.DATA_OWNER, AllowedProjectRoles.DATA_SCIENTIST})
  @JWTRequired(acceptedTokens={Audience.API}, allowedUserRoles={"HOPS_ADMIN", "HOPS_USER"})
  public Response startTensorBoard(@Context SecurityContext sc, @Context UriInfo uriInfo) throws DatasetException,
      ProjectException, TensorBoardException, UnsupportedEncodingException {

    DsPath dsPath = pathValidator.validatePath(this.project, "Experiments/" + experimentId);
    String fullPath = dsPath.getFullPath().toString();
    Users user = jWTHelper.getUserPrincipal(sc);
    TensorBoardDTO tensorBoardDTO = tensorBoardController.startTensorBoard(experimentId, project, user, fullPath);
    waitForTensorBoardLoaded(tensorBoardDTO);

    UriBuilder builder = uriInfo.getAbsolutePathBuilder();
    return Response.created(builder.build()).entity(tensorBoardDTO).build();
  }

  @ApiOperation("Stop the running TensorBoard")
  @DELETE
  @AllowedProjectRoles({AllowedProjectRoles.DATA_OWNER, AllowedProjectRoles.DATA_SCIENTIST})
  @JWTRequired(acceptedTokens={Audience.API}, allowedUserRoles={"HOPS_ADMIN", "HOPS_USER"})
  public Response stopTensorBoard(@Context SecurityContext sc) throws TensorBoardException {
    Users user = jWTHelper.getUserPrincipal(sc);
    TensorBoardDTO tbDTO = tensorBoardController.getTensorBoard(project, user);
    if (tbDTO == null) {
      return Response.noContent().build();
    }
    tensorBoardController.cleanup(this.project, user);
    return Response.noContent().build();
  }


  private void waitForTensorBoardLoaded(TensorBoardDTO tbDTO) {
    int retries = 20;

    int currentConsecutiveOK = 0;
    int maxConsecutiveOK = 3;

    while (retries > 0) {
      Response response;
      String tbUrl = "http://" + tbDTO.getEndpoint() + "/";
      Client client = ClientBuilder.newClient();
      WebTarget target = client.target(tbUrl);
      try {
        Thread.currentThread().sleep(1500);
        response = target.request().head();
        if(response.getStatus() == Response.Status.OK.getStatusCode()) {
          currentConsecutiveOK += 1;
          if(currentConsecutiveOK == maxConsecutiveOK) {
            return;
          }
        } else {
          currentConsecutiveOK = 0;
        }
      } catch (Exception ex) {
        LOGGER.log(Level.FINE, "Exception trying to get TensorBoard content", ex);
      } finally {
        client.close();
        retries--;
      }
    }
  }
}
