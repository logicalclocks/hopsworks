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

package io.hops.hopsworks.api.dela;

import com.google.common.base.Strings;
import com.google.gson.Gson;
import io.hops.hopsworks.api.dela.dto.BootstrapDTO;
import io.hops.hopsworks.api.dela.dto.DelaClientDTO;
import io.hops.hopsworks.api.filter.Audience;
import io.hops.hopsworks.api.filter.NoCacheResponse;
import io.hops.hopsworks.api.hopssite.dto.LocalDatasetDTO;
import io.hops.hopsworks.api.hopssite.dto.LocalDatasetHelper;
import io.hops.hopsworks.api.jwt.JWTHelper;
import io.hops.hopsworks.common.dao.dataset.Dataset;
import io.hops.hopsworks.common.dao.project.team.ProjectTeam;
import io.hops.hopsworks.common.dao.user.Users;
import io.hops.hopsworks.common.dataset.DatasetController;
import io.hops.hopsworks.common.dataset.FilePreviewDTO;
import io.hops.hopsworks.restutils.RESTCodes;
import io.hops.hopsworks.common.hdfs.DistributedFileSystemOps;
import io.hops.hopsworks.common.hdfs.DistributedFsService;
import io.hops.hopsworks.common.project.ProjectController;
import io.hops.hopsworks.common.util.Settings;
import io.hops.hopsworks.dela.DelaDatasetController;
import io.hops.hopsworks.dela.RemoteDelaController;
import io.hops.hopsworks.dela.TransferDelaController;
import io.hops.hopsworks.dela.dto.common.ClusterAddressDTO;
import io.hops.hopsworks.dela.dto.hopssite.SearchServiceDTO;
import io.hops.hopsworks.dela.dto.hopsworks.HopsworksSearchDTO;
import io.hops.hopsworks.exceptions.DelaException;
import io.hops.hopsworks.dela.hopssite.HopssiteController;
import io.hops.hopsworks.dela.old_dto.ElementSummaryJSON;
import io.hops.hopsworks.dela.old_dto.ErrorDescJSON;
import io.hops.hopsworks.dela.old_dto.HopsContentsSummaryJSON;
import io.hops.hopsworks.jwt.annotation.JWTRequired;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.GenericEntity;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.SecurityContext;

@Path("/dela")
@Stateless
@JWTRequired(acceptedTokens={Audience.API}, allowedUserRoles={"HOPS_ADMIN", "HOPS_USER"})
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Api(value = "Dela Service",
  description = "Dela Service")
@TransactionAttribute(TransactionAttributeType.NEVER)
public class DelaService {

  private static final  Logger LOGGER = Logger.getLogger(DelaService.class.getName());
  @EJB
  private NoCacheResponse noCacheResponse;
  @EJB
  private Settings settings;
  @EJB
  private HopssiteController hopsSite;
  @EJB
  private ProjectController projectCtrl;
  @EJB
  private TransferDelaController delaTransferCtrl;
  @EJB
  private RemoteDelaController remoteDelaCtrl;
  @EJB
  private DelaDatasetController delaDatasetCtrl;
  
  @EJB
  private DatasetController datasetCtrl;
  @EJB
  private DistributedFsService dfs;
  @EJB
  private JWTHelper jWTHelper;
  
  @GET
  @Path("/client")
  @Produces(MediaType.APPLICATION_JSON)
  public Response getClientType(@Context SecurityContext sc) {
    DelaClientDTO clientType = new DelaClientDTO(settings.getDelaClientType().type);
    return noCacheResponse.getNoCacheResponseBuilder(Response.Status.OK).entity(clientType).build();
  }
  
  @GET
  @Produces(MediaType.APPLICATION_JSON)
  public Response getPublicDatasets(@Context SecurityContext sc) {
    List<Dataset> clusterDatasets = delaDatasetCtrl.getLocalPublicDatasets();
    DistributedFileSystemOps dfso = dfs.getDfsOps();
    List<LocalDatasetDTO> localDS;
    try {
      localDS = LocalDatasetHelper.parse(datasetCtrl, dfso, clusterDatasets);
    } finally {
      dfs.closeDfsClient(dfso);
    }
    GenericEntity<List<LocalDatasetDTO>> datasets = new GenericEntity<List<LocalDatasetDTO>>(localDS) {};
    return noCacheResponse.getNoCacheResponseBuilder(Response.Status.OK).entity(datasets).build();
  }
  //********************************************************************************************************************
  @GET
  @Path("/search")
  @Produces(MediaType.APPLICATION_JSON)
  public Response publicSearch(@ApiParam(required=true) @QueryParam("query") String query, @Context SecurityContext sc)
    throws DelaException {
    LOGGER.log(Settings.DELA_DEBUG, "dela:search");
    if (Strings.isNullOrEmpty(query)) {
      throw new DelaException(RESTCodes.DelaErrorCode.ILLEGAL_ARGUMENT, Level.FINE, DelaException.Source.LOCAL,
        "query param was not provided.");
    }
    SearchServiceDTO.SearchResult searchResult = hopsSite.search(query);
    SearchServiceDTO.Item[] pageResult = hopsSite.page(searchResult.getSessionId(), 0, searchResult.getNrHits());
    HopsworksSearchDTO.Item[] parsedResult = parseSearchResult(pageResult);
    String auxResult = new Gson().toJson(parsedResult);
    LOGGER.log(Settings.DELA_DEBUG, "dela:search:done");
    return success(auxResult);
  }

  private HopsworksSearchDTO.Item[] parseSearchResult(SearchServiceDTO.Item[] items) {
    HopsworksSearchDTO.Item[] result = new HopsworksSearchDTO.Item[items.length];
    for (int i = 0; i < items.length; i++) {
      result[i] = new HopsworksSearchDTO.Item(items[i]);
    }
    return result;
  }
  //********************************************************************************************************************
  @GET
  @Path("/transfers/{publicDSId}")
  @Produces(MediaType.APPLICATION_JSON)
  public Response details(@PathParam("publicDSId")String publicDSId, @Context SecurityContext sc) throws DelaException {
    LOGGER.log(Settings.DELA_DEBUG, "dela:dataset:details {0}", publicDSId);
    SearchServiceDTO.ItemDetails result = hopsSite.details(publicDSId);
    String auxResult = new Gson().toJson(result);
    LOGGER.log(Settings.DELA_DEBUG, "dela:dataset:details:done {0}", publicDSId);
    return success(auxResult);
  }
  
  public enum TransfersFilter {
    USER
  }
  
  @GET
  @Path("/transfers")
  @Produces(MediaType.APPLICATION_JSON)
  public Response getContentsForUser(@Context SecurityContext sc, @QueryParam("filter") TransfersFilter filter)
      throws DelaException {
    if (!filter.equals(TransfersFilter.USER)) {
      throw new DelaException(RESTCodes.DelaErrorCode.ILLEGAL_ARGUMENT, Level.FINE, DelaException.Source.LOCAL,
        "not handling filter value:" + filter);
    }
    Users user = jWTHelper.getUserPrincipal(sc);
    List<ProjectTeam> teams = projectCtrl.findProjectByUser(user.getEmail());
    List<Integer> projectIds = new LinkedList<>();
    for (ProjectTeam t : teams) {
      projectIds.add(t.getProject().getId());
    }

    HopsContentsSummaryJSON.Contents resp = delaTransferCtrl.getContents(projectIds);
    List<UserContentsSummaryJSON> userContents = new ArrayList<>();
    Iterator<Map.Entry<Integer, ElementSummaryJSON[]>> it = resp.getContents().entrySet().iterator();
    while (it.hasNext()) {
      Map.Entry<Integer, ElementSummaryJSON[]> n = it.next();
      userContents.add(new UserContentsSummaryJSON(n.getKey(), n.getValue()));
    }
    GenericEntity<List<UserContentsSummaryJSON>> userContentsList
      = new GenericEntity<List<UserContentsSummaryJSON>>(userContents) {
      };
    return success(userContentsList);
  }
  //********************************************************************************************************************
  @POST
  @Path("/datasets/{publicDSId}/readme")
  @Produces(MediaType.APPLICATION_JSON)
  @ApiOperation(value = "Gets readme file from a provided list of peers")
  public Response readme(@PathParam("publicDSId") String publicDSId, BootstrapDTO peersJSON,
    @Context SecurityContext sc) throws DelaException {
    Optional<FilePreviewDTO> readme = tryReadmeLocally(publicDSId);
    if (!readme.isPresent()) {
      readme = tryReadmeRemotely(publicDSId, peersJSON);
    }
    if (!readme.isPresent()) {
      throw new DelaException(RESTCodes.DelaErrorCode.README_RETRIEVAL_FAILED, Level.FINE,
        DelaException.Source.REMOTE_DELA, "no local or remote version of readme found");
    }
    return success(readme.get());
  }
  
  private Optional<FilePreviewDTO> tryReadmeLocally(String publicDSId) throws DelaException {
    Optional<Dataset> dataset = delaDatasetCtrl.isPublicDatasetLocal(publicDSId);
    if(dataset.isPresent()) {
      try {
        FilePreviewDTO readme = delaDatasetCtrl.getLocalReadmeForPublicDataset(dataset.get());
        return Optional.of(readme);
      } catch (IOException | IllegalAccessException ex) {
        throw new DelaException(RESTCodes.DelaErrorCode.ILLEGAL_ARGUMENT, Level.SEVERE,
          DelaException.Source.HDFS, null, ex.getMessage(), ex);
      }
    }
    return Optional.empty();
  }
  
  private Optional<FilePreviewDTO> tryReadmeRemotely(String publicDSId, BootstrapDTO peersJSON) {
    for(ClusterAddressDTO peer : peersJSON.getBootstrap()) {
      try {
        FilePreviewDTO readme = remoteDelaCtrl.readme(publicDSId, peer);
        return Optional.of(readme);
      } catch (DelaException ex) {
        continue;
      }
    }
    return Optional.empty();
  }
  //********************************************************************************************************************
  private Response success(Object content) {
    return noCacheResponse.getNoCacheResponseBuilder(Response.Status.OK).entity(content).build();
  }
  

  private Response errorResponse(String msg) {
    ErrorDescJSON errorDesc = new ErrorDescJSON(msg);
    return noCacheResponse.getNoCacheResponseBuilder(Response.Status.EXPECTATION_FAILED).entity(errorDesc).build();
  }
}
