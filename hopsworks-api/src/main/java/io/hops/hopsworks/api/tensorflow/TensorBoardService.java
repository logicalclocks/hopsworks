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

package io.hops.hopsworks.api.tensorflow;

import io.hops.hopsworks.api.filter.AllowedProjectRoles;
import io.hops.hopsworks.api.filter.Audience;
import io.hops.hopsworks.api.filter.NoCacheResponse;
import io.hops.hopsworks.api.jwt.JWTHelper;
import io.hops.hopsworks.api.project.util.DsPath;
import io.hops.hopsworks.api.project.util.PathValidator;
import io.hops.hopsworks.common.dao.hdfs.inode.InodeFacade;
import io.hops.hopsworks.common.dao.project.Project;
import io.hops.hopsworks.common.dao.project.ProjectFacade;
import io.hops.hopsworks.common.dao.tensorflow.config.TensorBoardDTO;
import io.hops.hopsworks.common.dao.user.Users;
import io.hops.hopsworks.common.elastic.ElasticController;
import io.hops.hopsworks.common.exception.DatasetException;
import io.hops.hopsworks.common.exception.ProjectException;
import io.hops.hopsworks.common.exception.RESTCodes;
import io.hops.hopsworks.common.exception.ServiceException;
import io.hops.hopsworks.common.experiments.TensorBoardController;
import io.hops.hopsworks.jwt.annotation.JWTRequired;
import io.swagger.annotations.ApiOperation;

import javax.ejb.EJB;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.enterprise.context.RequestScoped;
import javax.persistence.PersistenceException;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
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

@RequestScoped
@TransactionAttribute(TransactionAttributeType.NEVER)
public class TensorBoardService {

  @EJB
  private ProjectFacade projectFacade;
  @EJB
  private InodeFacade inodesFacade;
  @EJB
  private TensorBoardController tensorBoardController;
  @EJB
  private ElasticController elasticController;
  @EJB
  private PathValidator pathValidator;
  @EJB
  private JWTHelper jWTHelper;
  @EJB
  private NoCacheResponse noCacheResponse;

  private Project project;

  public TensorBoardService(){
  }

  public void setProjectId(Integer projectId) {
    this.project = this.projectFacade.find(projectId);
  }

  public Project getProject() {
    return project;
  }

  private final static Logger LOGGER = Logger.getLogger(TensorBoardService.class.getName());

  @ApiOperation("Get the running TensorBoard of the logged in user in this project")
  @GET
  @Produces(MediaType.APPLICATION_JSON)
  @AllowedProjectRoles({AllowedProjectRoles.DATA_OWNER, AllowedProjectRoles.DATA_SCIENTIST})
  @JWTRequired(acceptedTokens={Audience.API}, allowedUserRoles={"HOPS_ADMIN", "HOPS_USER"})
  public Response getTensorBoard(@Context SecurityContext sc) throws ServiceException {

    try {
      Users user = jWTHelper.getUserPrincipal(sc);
      TensorBoardDTO tbDTO = tensorBoardController.getTensorBoard(project, user);
      if(tbDTO == null) {
        return noCacheResponse.getNoCacheResponseBuilder(Response.Status.NOT_FOUND).build();
      }
      return noCacheResponse.getNoCacheResponseBuilder(Response.Status.OK).entity(tbDTO).build();
    } catch (PersistenceException pe) {
      throw new ServiceException(RESTCodes.ServiceErrorCode.TENSORBOARD_FETCH_ERROR, Level.SEVERE, null,
        pe.getMessage(), pe);
    }
  }

  @ApiOperation("Start a new TensorBoard for the logged in user")
  @POST
  @Path("/{elasticId}")
  @Produces(MediaType.APPLICATION_JSON)
  @AllowedProjectRoles({AllowedProjectRoles.DATA_OWNER, AllowedProjectRoles.DATA_SCIENTIST})
  @JWTRequired(acceptedTokens={Audience.API}, allowedUserRoles={"HOPS_ADMIN", "HOPS_USER"})
  public Response startTensorBoard(@PathParam("elasticId") String elasticId, @Context SecurityContext sc) throws
      ServiceException, DatasetException, ProjectException {

    Users user = jWTHelper.getUserPrincipal(sc);

    String hdfsLogdir = null;
    hdfsLogdir = elasticController.getLogdirFromElastic(project, elasticId);
    hdfsLogdir = tensorBoardController.replaceNN(hdfsLogdir);

    DsPath tbPath = pathValidator.validatePath(this.project, hdfsLogdir);
    tbPath.validatePathExists(inodesFacade, true);

    TensorBoardDTO tensorBoardDTO = null;
    tensorBoardDTO = tensorBoardController.startTensorBoard(elasticId, this.project, user, hdfsLogdir);
    waitForTensorBoardLoaded(tensorBoardDTO);
    return noCacheResponse.getNoCacheResponseBuilder(Response.Status.CREATED).entity(tensorBoardDTO).build();
  }

  @ApiOperation("Stop the running TensorBoard for the logged in user")
  @DELETE
  @AllowedProjectRoles({AllowedProjectRoles.DATA_OWNER, AllowedProjectRoles.DATA_SCIENTIST})
  @JWTRequired(acceptedTokens={Audience.API}, allowedUserRoles={"HOPS_ADMIN", "HOPS_USER"})
  public Response stopTensorBoard(@Context SecurityContext sc) throws ServiceException {

    Users user = jWTHelper.getUserPrincipal(sc);
  
    TensorBoardDTO tbDTO = tensorBoardController.getTensorBoard(project, user);
    if (tbDTO == null) {
      return noCacheResponse.getNoCacheResponseBuilder(Response.Status.NOT_FOUND).build();
    }
  
    tensorBoardController.cleanup(this.project, user);
    return noCacheResponse.getNoCacheResponseBuilder(Response.Status.OK).build();
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
        response = target.request().get();
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
