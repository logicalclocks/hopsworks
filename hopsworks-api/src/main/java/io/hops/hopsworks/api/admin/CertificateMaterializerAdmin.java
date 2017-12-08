/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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
