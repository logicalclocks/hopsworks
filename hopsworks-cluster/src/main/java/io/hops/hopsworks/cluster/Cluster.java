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
package io.hops.hopsworks.cluster;

import io.hops.hopsworks.cluster.controller.ClusterController;
import io.hops.hopsworks.common.dao.user.cluster.ClusterCert;
import io.hops.hopsworks.common.exception.UserException;
import io.swagger.annotations.Api;

import javax.ejb.EJB;
import javax.mail.MessagingException;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.Consumes;
import javax.ws.rs.FormParam;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.GenericEntity;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.IOException;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

@Path("cluster")
@Api(value = "Cluster registration Service",
    description = "Cluster registration Service")
public class Cluster {

  private static final Logger LOGGER = Logger.getLogger(Cluster.class.getName());
  @EJB
  private ClusterController clusterController;
  @EJB
  private ClusterState clusterState;
  
  @POST
  @Path("register")
  @Consumes(MediaType.APPLICATION_JSON)
  public Response register(ClusterDTO cluster, @Context HttpServletRequest req) throws MessagingException,
    UserException {
    LOGGER.log(Level.INFO, "Registering : {0}", cluster.getEmail());
    boolean autoValidate = clusterState.bypassActivationLink();
    clusterController.registerClusterNewUser(cluster, req, autoValidate);
    ClusterJsonResponse res = new ClusterJsonResponse();
    res.setSuccessMessage("Cluster registerd. Please validate your email within "
        + ClusterController.VALIDATION_KEY_EXPIRY_DATE + " hours before installing your new cluster.");
    return Response.ok().entity(res).build();
  }
  
  @POST
  @Path("register/existing")
  @Consumes(MediaType.APPLICATION_JSON)
  public Response registerExisting(ClusterDTO cluster, @Context HttpServletRequest req)
    throws MessagingException, UserException {
    LOGGER.log(Level.INFO, "Registering : {0}", cluster.getEmail());
    boolean autoValidate = clusterState.bypassActivationLink();
    clusterController.registerClusterWithUser(cluster, req, autoValidate);
    ClusterJsonResponse res = new ClusterJsonResponse();
    res.setSuccessMessage("Cluster registerd. Please validate your email within "
        + ClusterController.VALIDATION_KEY_EXPIRY_DATE + " hours before installing your new cluster.");
    return Response.ok().entity(res).build();
  }

  @POST
  @Path("unregister")
  @Consumes(MediaType.APPLICATION_JSON)
  public Response unregister(ClusterDTO cluster, @Context HttpServletRequest req) 
    throws MessagingException, UserException {
    LOGGER.log(Level.INFO, "Unregistering : {0}", cluster.getEmail());
    clusterController.unregister(cluster, req);
    ClusterJsonResponse res = new ClusterJsonResponse();
    res.setSuccessMessage("Cluster unregisterd. Please validate your email within "
        + ClusterController.VALIDATION_KEY_EXPIRY_DATE + " hours to complite the unregistration.");
    return Response.ok().entity(res).build();
  }

  @GET
  @Path("register/confirm/{validationKey}")
  public Response confirmRegister(@PathParam("validationKey") String validationKey, @Context HttpServletRequest req) {
    ClusterJsonResponse res = new ClusterJsonResponse();
    try {
      clusterController.validateRequest(validationKey, req, ClusterController.OP_TYPE.REGISTER);
    } catch (IOException  ex) {
      LOGGER.log(Level.SEVERE, null, ex);
      res.setSuccessMessage("Could not validate registration.");
      return Response.ok().entity(res).build();
    }
    res.setSuccessMessage("Cluster registration validated.");
    return Response.ok().entity(res).build();
  }

  @GET
  @Path("unregister/confirm/{validationKey}")
  public Response confirmUnregister(@PathParam("validationKey") String validationKey, @Context HttpServletRequest req) {
    ClusterJsonResponse res = new ClusterJsonResponse();
    try {
      clusterController.validateRequest(validationKey, req, ClusterController.OP_TYPE.UNREGISTER);
    } catch (IOException  ex) {
      LOGGER.log(Level.SEVERE, null, ex);
      res.setSuccessMessage("Could not validate unregistration.");
      return Response.ok().entity(res).build();
    }
    res.setSuccessMessage("Cluster unregistration validated.");
    return Response.ok().entity(res).build();
  }

  @POST
  @Path("all")
  @Produces(MediaType.APPLICATION_JSON)
  @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
  public Response getRegisterdClusters(@FormParam("email") String email, @FormParam("pwd") String pwd,
      @Context HttpServletRequest req) throws UserException {
    ClusterDTO cluster = new ClusterDTO();
    cluster.setEmail(email);
    cluster.setChosenPassword(pwd);
    List<ClusterYmlDTO> clusters = clusterController.getAllClusterYml(cluster, req);
    GenericEntity<List<ClusterYmlDTO>> clustersEntity = new GenericEntity<List<ClusterYmlDTO>>(clusters) {
    };
    return Response.ok().entity(clustersEntity).build();
  }

  @POST
  @Produces(MediaType.APPLICATION_JSON)
  @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
  public Response getRegisterdCluster(@FormParam("email") String email, @FormParam("pwd") String pwd, @FormParam(
      "orgName") String organizationName, @FormParam("orgUnitName") String organizationalUnitName,
      @Context HttpServletRequest req) throws UserException {
    ClusterDTO cluster = new ClusterDTO();
    cluster.setEmail(email);
    cluster.setChosenPassword(pwd);
    cluster.setOrganizationName(organizationName);
    cluster.setOrganizationalUnitName(organizationalUnitName);
    ClusterCert clusters = clusterController.getCluster(cluster, req);
    return Response.ok().entity(clusters).build();
  }
}
