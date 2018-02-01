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

package io.hops.hopsworks.api.dela;

import com.google.gson.Gson;
import io.hops.hopsworks.api.dela.dto.BootstrapDTO;
import io.hops.hopsworks.api.dela.dto.DelaClientDTO;
import io.hops.hopsworks.api.filter.AllowedProjectRoles;
import io.hops.hopsworks.api.filter.NoCacheResponse;
import io.hops.hopsworks.api.hopssite.dto.LocalDatasetDTO;
import io.hops.hopsworks.api.hopssite.dto.LocalDatasetHelper;
import io.hops.hopsworks.common.dao.dataset.Dataset;
import io.hops.hopsworks.common.dao.project.team.ProjectTeam;
import io.hops.hopsworks.common.dataset.DatasetController;
import io.hops.hopsworks.common.dataset.FilePreviewDTO;
import io.hops.hopsworks.common.exception.AppException;
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
import io.hops.hopsworks.dela.exception.ThirdPartyException;
import io.hops.hopsworks.dela.hopssite.HopssiteController;
import io.hops.hopsworks.dela.old_dto.ElementSummaryJSON;
import io.hops.hopsworks.dela.old_dto.ErrorDescJSON;
import io.hops.hopsworks.dela.old_dto.HopsContentsSummaryJSON;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;
import javax.annotation.security.RolesAllowed;
import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.servlet.http.HttpServletRequest;
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
@RolesAllowed({"HOPS_ADMIN", "HOPS_USER"})
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Stateless
@TransactionAttribute(TransactionAttributeType.NEVER)
@Api(value = "Dela Service",
  description = "Dela Service")
public class DelaService {

  private final static Logger LOG = Logger.getLogger(DelaService.class.getName());
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
  
  @GET
  @Path("/client")
  @Produces(MediaType.APPLICATION_JSON)
  public Response getClientType() {
    DelaClientDTO clientType = new DelaClientDTO(settings.getDelaClientType().type);
    return noCacheResponse.getNoCacheResponseBuilder(Response.Status.OK).entity(clientType).build();
  }
  
  @GET
  @Produces(MediaType.APPLICATION_JSON)
  @AllowedProjectRoles({AllowedProjectRoles.ANYONE})
  public Response getPublicDatasets(@Context SecurityContext sc, @Context HttpServletRequest req) throws AppException {
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
  public Response publicSearch(@ApiParam(required=true) @QueryParam("query") String query)
    throws ThirdPartyException {
    LOG.log(Settings.DELA_DEBUG, "dela:search");
    searchSanityCheck(query);
    SearchServiceDTO.SearchResult searchResult = hopsSite.search(query);
    SearchServiceDTO.Item[] pageResult = hopsSite.page(searchResult.getSessionId(), 0, searchResult.getNrHits());
    HopsworksSearchDTO.Item[] parsedResult = parseSearchResult(pageResult);
    String auxResult = new Gson().toJson(parsedResult);
    LOG.log(Settings.DELA_DEBUG, "dela:search:done");
    return success(auxResult);
  }

  private void searchSanityCheck(String searchTerm) throws ThirdPartyException {
    if (searchTerm == null || searchTerm.isEmpty()) {
      throw new ThirdPartyException(Response.Status.BAD_REQUEST.getStatusCode(), "search term",
        ThirdPartyException.Source.LOCAL, "bad request");
    }
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
  public Response details(@PathParam("publicDSId")String publicDSId) throws ThirdPartyException {
    LOG.log(Settings.DELA_DEBUG, "dela:dataset:details {0}", publicDSId);
    SearchServiceDTO.ItemDetails result = hopsSite.details(publicDSId);
    String auxResult = new Gson().toJson(result);
    LOG.log(Settings.DELA_DEBUG, "dela:dataset:details:done {0}", publicDSId);
    return success(auxResult);
  }
  
  public static enum TransfersFilter {
    USER
  }
  
  @GET
  @Path("/transfers")
  @Produces(MediaType.APPLICATION_JSON)
  public Response getContentsForUser(@Context SecurityContext sc, 
    @QueryParam("filter") TransfersFilter filter) throws ThirdPartyException {
    if(!filter.equals(TransfersFilter.USER)) {
      throw new ThirdPartyException(Response.Status.BAD_REQUEST.getStatusCode(), "bad request",
        ThirdPartyException.Source.LOCAL, "not handling filter value:" + filter);
    }
    String email = sc.getUserPrincipal().getName();
    List<ProjectTeam> teams = projectCtrl.findProjectByUser(email);
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
  public Response readme(@PathParam("publicDSId") String publicDSId, BootstrapDTO peersJSON) 
    throws ThirdPartyException {
    for(ClusterAddressDTO peer : peersJSON.getBootstrap()) {
      try {
        FilePreviewDTO readme = remoteDelaCtrl.readme(publicDSId, peer);
        return success(readme);
      } catch (ThirdPartyException ex) {
        continue;
      }
    }
    throw new ThirdPartyException(Response.Status.EXPECTATION_FAILED.getStatusCode(), "communication fail",
        ThirdPartyException.Source.REMOTE_DELA, "all peers for:" + publicDSId);
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
