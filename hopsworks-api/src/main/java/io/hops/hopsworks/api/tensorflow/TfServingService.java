/*
 * Changes to this file committed after and not including commit-id: ccc0d2c5f9a5ac661e60e6eaf138de7889928b8b
 * are released under the following license:
 *
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
 *
 * Changes to this file committed before and including commit-id: ccc0d2c5f9a5ac661e60e6eaf138de7889928b8b
 * are released under the following license:
 *
 * Copyright (C) 2013 - 2018, Logical Clocks AB and RISE SICS AB. All rights reserved
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this
 * software and associated documentation files (the "Software"), to deal in the Software
 * without restriction, including without limitation the rights to use, copy, modify, merge,
 * publish, distribute, sublicense, and/or sell copies of the Software, and to permit
 * persons to whom the Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or
 * substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS  OR IMPLIED, INCLUDING
 * BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL  THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM,
 * DAMAGES OR  OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package io.hops.hopsworks.api.tensorflow;

import javax.ejb.EJB;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.enterprise.context.RequestScoped;

import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.DELETE;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.Consumes;
import javax.ws.rs.PathParam;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.GenericEntity;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import io.hops.hopsworks.api.filter.AllowedProjectRoles;

import io.hops.hopsworks.api.filter.NoCacheResponse;
import io.hops.hopsworks.common.dao.project.Project;
import io.hops.hopsworks.common.serving.tf.TfServingCommands;
import io.hops.hopsworks.common.dao.user.Users;
import io.hops.hopsworks.common.serving.tf.TfServingController;
import io.hops.hopsworks.common.serving.tf.TfServingException;
import io.hops.hopsworks.common.serving.tf.TfServingWrapper;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;

import java.util.ArrayList;
import java.util.List;

@RequestScoped
@TransactionAttribute(TransactionAttributeType.NEVER)
@Api(value = "TensorFlow Serving service", description = "Manage TFServing instances")
public class TfServingService {

  @Inject
  private TfServingController tfServingController;

  @EJB
  private NoCacheResponse noCacheResponse;

  /*
    @POST
    project/id/serving/

    TFserving {
      model_dir
      model_name
    }

    Get @GET  project/id/serving/
    Get Single @GET project/id/serving/12
    Delete @Delete project/id/serving/12
    POST project/id/serving/12 {action: start | stop}
   */

  private Project project;
  private Users user;

  public TfServingService(){ }

  public void setProject(Project project) {
    this.project = project;
  }

  public void setUser(Users user) {
    this.user = user;
  }

  @GET
  @Produces(MediaType.APPLICATION_JSON)
  @AllowedProjectRoles({AllowedProjectRoles.DATA_OWNER, AllowedProjectRoles.DATA_SCIENTIST})
  @ApiOperation(value = "Get the list of TfServing instances for the project",
      response = TfServingView.class,
      responseContainer = "List")
  public Response getTfServings() throws TfServingException {
    List<TfServingWrapper> servingDAOList = tfServingController.getTfServings(project);


    ArrayList<TfServingView> servingViewList = new ArrayList<>();
    for (TfServingWrapper tfServingWrapper : servingDAOList) {
      servingViewList.add(new TfServingView(tfServingWrapper));
    }

    GenericEntity<ArrayList<TfServingView>> genericListTfView =
        new GenericEntity<ArrayList<TfServingView>>(servingViewList){};

    return noCacheResponse.getNoCacheResponseBuilder(Response.Status.OK)
        .entity(genericListTfView)
        .build();
  }

  @GET
  @Path("/{servingId}")
  @Produces(MediaType.APPLICATION_JSON)
  @AllowedProjectRoles({AllowedProjectRoles.DATA_OWNER, AllowedProjectRoles.DATA_SCIENTIST})
  @ApiOperation(value = "Get info about a TfServing instance for the project", response = TfServingView.class)
  public Response getTfserving(
      @ApiParam(value = "Id of the TfServing instance", required = true) @PathParam("servingId") Integer servingId)
      throws TfServingException {
    if (servingId == null) {
      throw new TfServingException(TfServingException.TfServingExceptionErrors.INSTANCENOTFOUND);
    }
    TfServingWrapper tfServingWrapper = tfServingController.getTfServing(project, servingId);

    TfServingView tfServingView = new TfServingView(tfServingWrapper);
    GenericEntity<TfServingView> tfServingEntity = new GenericEntity<TfServingView>(tfServingView){};

    return noCacheResponse.getNoCacheResponseBuilder(Response.Status.OK)
        .entity(tfServingEntity)
        .build();
  }

  @DELETE
  @Path("/{servingId}")
  @AllowedProjectRoles({AllowedProjectRoles.DATA_OWNER, AllowedProjectRoles.DATA_SCIENTIST})
  @ApiOperation(value = "Delete a TfServing instance")
  public Response deleteTfServing(
      @ApiParam(value = "Id of the TfServing instance", required = true) @PathParam("servingId") Integer servingId)
      throws TfServingException {
    if (servingId == null) {
      throw new TfServingException(TfServingException.TfServingExceptionErrors.INSTANCENOTFOUND);
    }

    tfServingController.deleteTfServing(project, servingId);

    return Response.ok().build();
  }

  @PUT
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  @AllowedProjectRoles({AllowedProjectRoles.DATA_OWNER, AllowedProjectRoles.DATA_SCIENTIST})
  @ApiOperation(value = "Create or update a TfServing instance")
  public Response createOrUpdate(
      @ApiParam(value = "TfServing specification", required = true) TfServingView tfServing)
      throws TfServingException {
    if (tfServing == null) {
      throw new TfServingException(TfServingException.TfServingExceptionErrors.SPECNOTPROVIDED);
    }

    tfServingController.createOrUpdate(project, user, tfServing.getTfServingDAO());

    return Response.status(Response.Status.CREATED).build();
  }

  @POST
  @Path("/{servingId}")
  @Consumes(MediaType.APPLICATION_JSON)
  @AllowedProjectRoles({AllowedProjectRoles.DATA_OWNER, AllowedProjectRoles.DATA_SCIENTIST})
  @ApiOperation(value = "Start or stop a TfServing instance")
  public Response startOrStop(
      @ApiParam(value = "ID of the TfServing instance to start/stop", required = true)
      @PathParam("servingId") Integer servingId,
      @ApiParam(value = "Action", required = true) @QueryParam("action") TfServingCommands servingCommand)
      throws TfServingException {

    if (servingId == null) {
      throw new TfServingException(TfServingException.TfServingExceptionErrors.INSTANCENOTFOUND);
    }

    if (servingCommand == null) {
      throw new TfServingException(TfServingException.TfServingExceptionErrors.COMMANDNOTPROVIDED);
    }

    tfServingController.startOrStop(project, user, servingId, servingCommand);

    return Response.ok().build();
  }
}