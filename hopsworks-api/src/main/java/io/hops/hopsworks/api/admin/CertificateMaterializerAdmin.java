/*
 * This file is part of HopsWorks
 *
 * Copyright (C) 2013 - 2018, Logical Clocks AB and RISE SICS AB. All rights reserved.
 *
 * HopsWorks is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * HopsWorks is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with HopsWorks.  If not, see <http://www.gnu.org/licenses/>.
 */

package io.hops.hopsworks.api.admin;

import io.hops.hopsworks.api.admin.dto.MaterializerStateResponse;
import io.hops.hopsworks.api.filter.NoCacheResponse;
import io.hops.hopsworks.api.util.JsonResponse;
import io.hops.hopsworks.common.exception.AppException;
import io.hops.hopsworks.common.hdfs.HdfsUsersController;
import io.hops.hopsworks.common.security.CertificateMaterializer;
import io.swagger.annotations.Api;

import javax.annotation.security.RolesAllowed;
import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.GenericEntity;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.SecurityContext;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * REST API to monitor and control CertificateMaterializer service
 */
@Path("/admin/materializer")
@RolesAllowed({"HOPS_ADMIN"})
@Api(value = "Admin")
@Produces(MediaType.APPLICATION_JSON)
@Stateless
@TransactionAttribute(TransactionAttributeType.NEVER)
public class CertificateMaterializerAdmin {
  
  private final Pattern projectSpecificPattern = Pattern.compile("(\\w*)" + HdfsUsersController.USER_NAME_DELIMITER +
      "(\\w*)");
  private final Logger LOG = Logger.getLogger(CertificateMaterializer.class.getName());
  
  @EJB
  private NoCacheResponse noCacheResponse;
  @EJB
  private CertificateMaterializer certificateMaterializer;
  
  /**
   * Gets the name of the materialized crypto along with their number of references and the materials that are
   * scheduled for removal.
   * @param sc
   * @param request
   * @return
   * @throws AppException
   */
  @GET
  public Response getMaterializerState(@Context SecurityContext sc, @Context HttpServletRequest request)
    throws AppException {
    CertificateMaterializer.MaterializerState<Map<String, Integer>, Set<String>> materializerState =
        certificateMaterializer.getState();
    
    List<MaterializerStateResponse.CryptoMaterial> materializedState = new ArrayList<>(materializerState
        .getMaterializedState().size());
    for (Map.Entry<String, Integer> entry : materializerState.getMaterializedState().entrySet()) {
      materializedState.add(new MaterializerStateResponse.CryptoMaterial(
          entry.getKey(), entry.getValue()));
    }
    
    MaterializerStateResponse responseState = new MaterializerStateResponse(materializedState, materializerState
        .getScheduledRemovals());
    GenericEntity<MaterializerStateResponse> response = new GenericEntity<MaterializerStateResponse>(responseState){};
    return noCacheResponse.getNoCacheResponseBuilder(Response.Status.OK).entity(response).build();
  }
  
  /**
   * Removes crypto material from the store. It SHOULD be used *wisely*!!!
   * @param sc
   * @param request
   * @param materialName Name of the materialized crypto
   * @return
   * @throws AppException
   */
  @DELETE
  @Path("/{name}")
  public Response removeMaterializedCrypto(@Context SecurityContext sc, @Context HttpServletRequest request,
      @PathParam("name") String materialName) throws AppException {
    if (materialName == null || materialName.isEmpty()) {
      LOG.log(Level.WARNING, "Request to remove crypto material but the material name is either null or empty");
      throw new AppException(Response.Status.BAD_REQUEST.getStatusCode(), "Material name is null or empty");
    }
    
    JsonResponse response;
    Matcher psuMatcher = projectSpecificPattern.matcher(materialName);
    if (psuMatcher.matches()) {
      String projectName = psuMatcher.group(1);
      String userName = psuMatcher.group(2);
      if (certificateMaterializer.existsInStore(userName, projectName)) {
        certificateMaterializer.forceRemoveCertificates(userName, projectName, false);
      } else {
        response = noCacheResponse.buildJsonResponse(Response.Status.NOT_FOUND, "Material for user " + materialName
            + " does not exist in the store");
        return noCacheResponse.getNoCacheResponseBuilder(Response.Status.NOT_FOUND).entity(response).build();
      }
    } else {
      if (certificateMaterializer.existsInStore(null, materialName)) {
        certificateMaterializer.forceRemoveCertificates(null, materialName, false);
      } else {
        response = noCacheResponse.buildJsonResponse(Response.Status.NOT_FOUND, "Material for project " +
            materialName + " does not exist in the store");
        return noCacheResponse.getNoCacheResponseBuilder(Response.Status.NOT_FOUND).entity(response).build();
      }
    }
    
    response = noCacheResponse.buildJsonResponse(Response.Status.OK, "Deleted material");
    return noCacheResponse.getNoCacheResponseBuilder(Response.Status.OK).entity(response).build();
  }
}
