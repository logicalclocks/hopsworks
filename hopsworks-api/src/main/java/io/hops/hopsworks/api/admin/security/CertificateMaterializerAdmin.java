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

package io.hops.hopsworks.api.admin.security;

import com.google.common.base.Strings;
import io.hops.hopsworks.api.admin.dto.MaterializerStateResponse;
import io.hops.hopsworks.api.filter.Audience;
import io.hops.hopsworks.api.filter.NoCacheResponse;
import io.hops.hopsworks.api.util.RESTApiJsonResponse;
import io.hops.hopsworks.common.hdfs.HdfsUsersController;
import io.hops.hopsworks.common.security.CertificateMaterializer;
import io.hops.hopsworks.jwt.annotation.JWTRequired;
import io.swagger.annotations.Api;

import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
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
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * REST API to monitor and control CertificateMaterializer service
 */
@Path("/admin/materializer")
@Stateless
@JWTRequired(acceptedTokens={Audience.API}, allowedUserRoles={"HOPS_ADMIN"})
@Produces(MediaType.APPLICATION_JSON)
@Api(value = "Admin")
@TransactionAttribute(TransactionAttributeType.NEVER)
public class CertificateMaterializerAdmin {
  
  private final Pattern projectSpecificPattern = Pattern.compile("(\\w*)" + HdfsUsersController.USER_NAME_DELIMITER +
      "(\\w*)");
  private final Logger LOGGER = Logger.getLogger(CertificateMaterializerAdmin.class.getName());
  
  @EJB
  private NoCacheResponse noCacheResponse;
  @EJB
  private CertificateMaterializer certificateMaterializer;
  
  /**
   * Gets the name of the materialized crypto along with their number of references and the materials that are
   * scheduled for removal.
   * @return
   */
  @GET
  public Response getMaterializerState(@Context SecurityContext sc) {
  
    CertificateMaterializer.MaterializerState<Map<String, Map<String, Integer>>, Map<String, Map<String, Integer>>,
        Map<String, Set<String>>, Map<String, Boolean>> materializerState = certificateMaterializer.getState();
    
    List<MaterializerStateResponse.CryptoMaterial> localStateResponse = createMaterializerResponse(materializerState
        .getLocalMaterial());
    List<MaterializerStateResponse.CryptoMaterial> remoteStateResponse = createMaterializerResponse(materializerState
        .getRemoteMaterial());
    List<MaterializerStateResponse.CryptoMaterial> fileRemovalsResponse = new ArrayList<>();
    Map<String, Set<String>> fileRemovalsState = materializerState.getScheduledRemovals();
    for (Map.Entry<String, Set<String>> entry : fileRemovalsState.entrySet()) {
      String username = entry.getKey();
      for (String path : entry.getValue()) {
        MaterializerStateResponse.CryptoMaterial remover = new MaterializerStateResponse.CryptoMaterial(username,
            path, 0);
        fileRemovalsResponse.add(remover);
      }
    }
    
    MaterializerStateResponse responseState = new MaterializerStateResponse(localStateResponse, remoteStateResponse,
        fileRemovalsResponse, materializerState.getMaterialKeyLocks());
    
    GenericEntity<MaterializerStateResponse> response = new GenericEntity<MaterializerStateResponse>(responseState){};
    return noCacheResponse.getNoCacheResponseBuilder(Response.Status.OK).entity(response).build();
  }
  
  private List<MaterializerStateResponse.CryptoMaterial> createMaterializerResponse(
      Map<String, Map<String, Integer>> materializerState) {
    List<MaterializerStateResponse.CryptoMaterial> materializerStateResponse = new ArrayList<>();
    for (Map.Entry<String, Map<String, Integer>> entry : materializerState.entrySet()) {
      String username = entry.getKey();
      for (Map.Entry<String, Integer> refs : entry.getValue().entrySet()) {
        MaterializerStateResponse.CryptoMaterial material = new MaterializerStateResponse.CryptoMaterial(
            username, refs.getKey(), refs.getValue());
        materializerStateResponse.add(material);
      }
    }
    
    return materializerStateResponse;
  }
  
  /**
   * Removes crypto material from the store. It SHOULD be used *wisely*!!!
   *
   * @param materialName Name of the materialized crypto
   * @param directory Local directory of the crypto material
   * @return
   */
  @DELETE
  @Path("/local/{name}/{directory}")
  public Response removeLocalMaterializedCrypto(@PathParam("name") String materialName,
      @PathParam("directory") String directory, @Context SecurityContext sc) {
    if (Strings.isNullOrEmpty(materialName)) {
      throw new IllegalArgumentException("materialName was not provided or was empty");
    }
    
    RESTApiJsonResponse response;
    Matcher psuMatcher = projectSpecificPattern.matcher(materialName);
    if (psuMatcher.matches()) {
      String projectName = psuMatcher.group(1);
      String userName = psuMatcher.group(2);
      if (certificateMaterializer.existsInLocalStore(userName, projectName, directory)) {
        certificateMaterializer.forceRemoveLocalMaterial(userName, projectName, directory, false);
      } else {
        response = noCacheResponse.buildJsonResponse(Response.Status.NOT_FOUND, "Material for user " + materialName
            + " does not exist in the store");
        return noCacheResponse.getNoCacheResponseBuilder(Response.Status.NOT_FOUND).entity(response).build();
      }
    } else {
      if (certificateMaterializer.existsInLocalStore(null, materialName, directory)) {
        certificateMaterializer.forceRemoveLocalMaterial(null, materialName, directory, false);
      } else {
        response = noCacheResponse.buildJsonResponse(Response.Status.NOT_FOUND, "Material for project " +
            materialName + " does not exist in the store");
        return noCacheResponse.getNoCacheResponseBuilder(Response.Status.NOT_FOUND).entity(response).build();
      }
    }
    
    response = noCacheResponse.buildJsonResponse(Response.Status.OK, "Deleted material");
    return noCacheResponse.getNoCacheResponseBuilder(Response.Status.OK).entity(response).build();
  }
  
  /**
   * Removes crypto material from remote filesystem.
   *
   * CAUTION: This is a *dangerous* operation as other instances of Hopsworks might be
   * running and using the remote crypto material
   *
   * @param materialName Name of the materialized crypto
   * @param directory Remote directory of the crypto material
   * @return
   */
  @DELETE
  @Path("/remote/{name}/{directory}")
  public Response removeRemoteMaterializedCrypto(@PathParam("name") String materialName,
      @PathParam("directory") String directory, @Context SecurityContext sc) {
    if (Strings.isNullOrEmpty(materialName)) {
      throw new IllegalArgumentException("materialName was not provided or was empty");
    }
    
    RESTApiJsonResponse response;
    Matcher psuMatcher = projectSpecificPattern.matcher(materialName);
    if (psuMatcher.matches()) {
      String projectName = psuMatcher.group(1);
      String userName = psuMatcher.group(2);
      if (certificateMaterializer.existsInRemoteStore(userName, projectName, directory)) {
        certificateMaterializer.forceRemoveRemoteMaterial(userName, projectName, directory, false);
      } else {
        response = noCacheResponse.buildJsonResponse(Response.Status.NOT_FOUND, "Material for user " + materialName
            + " does not exist in the store");
        return noCacheResponse.getNoCacheResponseBuilder(Response.Status.NOT_FOUND).entity(response).build();
      }
    } else {
      if (certificateMaterializer.existsInRemoteStore(null, materialName, directory)) {
        certificateMaterializer.forceRemoveRemoteMaterial(null, materialName, directory, false);
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
