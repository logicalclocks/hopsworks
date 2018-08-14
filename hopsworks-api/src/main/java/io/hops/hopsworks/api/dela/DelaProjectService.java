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

import io.hops.hopsworks.api.dela.dto.InodeIdDTO;
import io.hops.hopsworks.api.filter.AllowedProjectRoles;
import io.hops.hopsworks.api.filter.NoCacheResponse;
import io.hops.hopsworks.api.util.JsonResponse;
import io.hops.hopsworks.common.dao.dataset.Dataset;
import io.hops.hopsworks.common.dao.dataset.DatasetFacade;
import io.hops.hopsworks.common.dao.hdfs.inode.Inode;
import io.hops.hopsworks.common.dao.hdfs.inode.InodeFacade;
import io.hops.hopsworks.common.dao.project.Project;
import io.hops.hopsworks.common.dao.project.ProjectFacade;
import io.hops.hopsworks.common.dao.user.UserFacade;
import io.hops.hopsworks.common.dao.user.Users;
import io.hops.hopsworks.common.exception.AppException;
import io.hops.hopsworks.common.kafka.KafkaController;
import io.hops.hopsworks.common.util.Settings;
import io.hops.hopsworks.dela.DelaHdfsController;
import io.hops.hopsworks.dela.DelaWorkerController;
import io.hops.hopsworks.dela.TransferDelaController;
import io.hops.hopsworks.dela.dto.hopsworks.HopsworksTransferDTO;
import io.hops.hopsworks.dela.exception.ThirdPartyException;
import io.hops.hopsworks.dela.old_dto.ElementSummaryJSON;
import io.hops.hopsworks.dela.old_dto.HopsContentsSummaryJSON;
import io.hops.hopsworks.dela.old_dto.KafkaEndpoint;
import io.hops.hopsworks.dela.old_dto.ManifestJSON;
import io.hops.hopsworks.dela.old_dto.SuccessJSON;
import io.hops.hopsworks.dela.old_dto.TorrentExtendedStatusJSON;
import io.hops.hopsworks.dela.old_dto.TorrentId;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiParam;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.annotation.security.RolesAllowed;
import javax.ejb.EJB;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.enterprise.context.RequestScoped;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.SecurityContext;

@RolesAllowed({"HOPS_ADMIN", "HOPS_USER"})
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@RequestScoped
@TransactionAttribute(TransactionAttributeType.NEVER)
@Api(value = "Dela Project Service",
    description = "Dela Project Service")
public class DelaProjectService {

  private final static Logger logger = Logger.getLogger(DelaProjectService.class.getName());
  @EJB
  private NoCacheResponse noCacheResponse;
  @EJB
  private Settings settings;
  @EJB
  private DelaWorkerController delaWorkerCtrl;
  @EJB
  private TransferDelaController delaTransferCtrl;
  @EJB
  private DelaHdfsController delaHdfsCtrl;
  @EJB
  private KafkaController kafkaController;
  @EJB
  private UserFacade userFacade;
  @EJB
  private DatasetFacade datasetFacade;
  @EJB
  private ProjectFacade projectFacade;
  @EJB
  private InodeFacade inodeFacade;

  private Project project;
  private Integer projectId;

  public void setProjectId(Integer projectId) {
    this.projectId = projectId;
    this.project = projectFacade.find(projectId);
  }

  public Integer getProjectId() {
    return projectId;
  }

  private Response successResponse(Object content) {
    return noCacheResponse.getNoCacheResponseBuilder(Response.Status.OK).entity(content).build();
  }

  @GET
  @Path("/transfers")
  @Produces(MediaType.APPLICATION_JSON)
  @AllowedProjectRoles({AllowedProjectRoles.DATA_SCIENTIST, AllowedProjectRoles.DATA_OWNER})
  public Response getProjectContents(@Context SecurityContext sc) throws ThirdPartyException {

    List<Integer> projectIds = new LinkedList<>();
    projectIds.add(projectId);

    HopsContentsSummaryJSON.Contents resp = delaTransferCtrl.getContents(projectIds);
    ElementSummaryJSON[] projectContents = resp.getContents().get(projectId);
    if (projectContents == null) {
      projectContents = new ElementSummaryJSON[0];
    }
    return successResponse(projectContents);
  }

  @POST
  @Path("/uploads")
  @Produces(MediaType.APPLICATION_JSON)
  @AllowedProjectRoles({AllowedProjectRoles.DATA_OWNER})
  public Response publish(@Context SecurityContext sc, InodeIdDTO inodeId) throws ThirdPartyException {
    Inode inode = getInode(inodeId.getId());
    Dataset dataset = getDatasetByInode(inode);
    Users user = getUser(sc.getUserPrincipal().getName());
    delaWorkerCtrl.shareDatasetWithHops(project, dataset, user);
    JsonResponse json = new JsonResponse();
    json.setSuccessMessage("Dataset transfer is started - published");
    return successResponse(json);
  }

  @GET
  @Path("/transfers/{publicDSId}")
  @Produces(MediaType.APPLICATION_JSON)
  @Consumes(MediaType.APPLICATION_JSON)
  @AllowedProjectRoles({AllowedProjectRoles.DATA_SCIENTIST, AllowedProjectRoles.DATA_OWNER})
  public Response getExtendedDetails(@Context SecurityContext sc, @PathParam("publicDSId") String publicDSId)
      throws ThirdPartyException {

    TorrentId torrentId = new TorrentId(publicDSId);
    TorrentExtendedStatusJSON resp = delaTransferCtrl.details(torrentId);
    return successResponse(resp);
  }

  @POST
  @Path("/transfers/{publicDSId}/cancel")
  @Produces(MediaType.APPLICATION_JSON)
  @AllowedProjectRoles({AllowedProjectRoles.DATA_OWNER})
  public Response removePublic(@Context SecurityContext sc, @PathParam("publicDSId") String publicDSId,
      @ApiParam(value = "delete dataset",
          required = true) @QueryParam("clean") boolean clean) throws ThirdPartyException {
    Dataset dataset = getDatasetByPublicId(publicDSId);
    Users user = getUser(sc.getUserPrincipal().getName());
    if (clean) {
      delaWorkerCtrl.unshareFromHopsAndClean(project, dataset, user);
    } else {
      delaWorkerCtrl.unshareFromHops(project, dataset, user);
    }
    JsonResponse json = new JsonResponse();
    json.setSuccessMessage("Dataset is now private");
    return successResponse(json);
  }

  @POST
  @Path("/downloads/{publicDSId}/manifest")
  @Produces(MediaType.APPLICATION_JSON)
  @Consumes(MediaType.APPLICATION_JSON)
  @AllowedProjectRoles({AllowedProjectRoles.DATA_SCIENTIST, AllowedProjectRoles.DATA_OWNER})
  public Response startDownload(@Context SecurityContext sc, @PathParam("publicDSId") String publicDSId,
      HopsworksTransferDTO.Download downloadDTO) throws ThirdPartyException {
    Users user = getUser(sc.getUserPrincipal().getName());
    //dataset not createed yet

    ManifestJSON manifest = delaWorkerCtrl.startDownload(project, user, downloadDTO);
    return successResponse(manifest);
  }

  @POST
  @Path("/downloads/{publicDSId}/hdfs")
  @Produces(MediaType.APPLICATION_JSON)
  @Consumes(MediaType.APPLICATION_JSON)
  @AllowedProjectRoles({AllowedProjectRoles.DATA_SCIENTIST, AllowedProjectRoles.DATA_OWNER})
  public Response downloadDatasetHdfs(@Context SecurityContext sc, @PathParam("publicDSId") String publicDSId,
      HopsworksTransferDTO.Download downloadDTO) throws ThirdPartyException {
    Users user = getUser(sc.getUserPrincipal().getName());
    Dataset dataset = getDatasetByPublicId(downloadDTO.getPublicDSId());

    delaWorkerCtrl.advanceDownload(project, dataset, user, downloadDTO, null, null);
    return successResponse(new SuccessJSON(""));
  }

  @POST
  @Path("/downloads/{publicDSId}/kafka")
  @Produces(MediaType.APPLICATION_JSON)
  @Consumes(MediaType.APPLICATION_JSON)
  @AllowedProjectRoles({AllowedProjectRoles.DATA_SCIENTIST, AllowedProjectRoles.DATA_OWNER})
  public Response downloadDatasetKafka(@Context SecurityContext sc, @Context HttpServletRequest req,
      @PathParam("publicDSId") String publicDSId, HopsworksTransferDTO.Download downloadDTO)
      throws ThirdPartyException {
    Users user = getUser(sc.getUserPrincipal().getName());
    Dataset dataset = getDatasetByPublicId(publicDSId);

    String certPath = kafkaController.getKafkaCertPaths(project);
    String brokerEndpoint = settings.getRandomKafkaBroker();
    String restEndpoint = settings.getRestEndpoint();
    String keyStore = certPath + "/keystore.jks";
    String trustStore = certPath + "/truststore.jks";
    KafkaEndpoint kafkaEndpoint = new KafkaEndpoint(brokerEndpoint, restEndpoint, settings.getDELA_DOMAIN(),
        "" + project.getId(), keyStore, trustStore);

    delaWorkerCtrl.advanceDownload(project, dataset, user, downloadDTO, req.getSession().getId(),
        kafkaEndpoint);
    return successResponse(new SuccessJSON(""));
  }

  @GET
  @Path("/transfers/{publicDSId}/manifest/")
  @Produces(MediaType.APPLICATION_JSON)
  @AllowedProjectRoles({AllowedProjectRoles.DATA_OWNER})
  public Response showManifest(@Context SecurityContext sc, @PathParam("publicDSId") String publicDSId)
      throws ThirdPartyException {
    JsonResponse json = new JsonResponse();
    Dataset dataset = getDatasetByPublicId(publicDSId);
    Users user = getUser(sc.getUserPrincipal().getName());
    if (!dataset.isPublicDs()) {
      throw new ThirdPartyException(Response.Status.BAD_REQUEST.getStatusCode(), "dataset not public - no manifest",
          ThirdPartyException.Source.LOCAL, "bad request");
    }

    ManifestJSON manifestJSON = delaHdfsCtrl.readManifest(project, dataset, user);
    return successResponse(manifestJSON);
  }

  private Users getUser(String email) throws ThirdPartyException {
    Users user;
    try {
      user = userFacade.findByEmail(email);
    } catch (AppException ex) {
      Logger.getLogger(DelaProjectService.class.getName()).log(Level.SEVERE, null, ex);
      throw new ThirdPartyException(ex.getStatus(), "DB Problem",
          ThirdPartyException.Source.MYSQL, ex.getMessage());
    }
    if (user == null) {
      throw new ThirdPartyException(Response.Status.FORBIDDEN.getStatusCode(), "user not found",
          ThirdPartyException.Source.LOCAL, "exception");
    }
    return user;
  }

  private Dataset getDatasetByPublicId(String publicDSId) throws ThirdPartyException {
    Optional<Dataset> d = datasetFacade.findByPublicDsIdProject(publicDSId, project);
    if (!d.isPresent()) {
      throw new ThirdPartyException(Response.Status.EXPECTATION_FAILED.getStatusCode(),
          "dataset by publicId and project", ThirdPartyException.Source.MYSQL, "not found");
    }
    return d.get();
  }

  private Dataset getDatasetByInode(Inode inode) throws ThirdPartyException {
    Dataset dataset = datasetFacade.findByProjectAndInode(this.project, inode);
    if (dataset == null) {
      throw new ThirdPartyException(Response.Status.BAD_REQUEST.getStatusCode(), "dataset not found",
          ThirdPartyException.Source.LOCAL, "bad request");
    }
    return dataset;
  }

  private Inode getInode(Integer inodeId) throws ThirdPartyException {
    if (inodeId == null) {
      throw new ThirdPartyException(Response.Status.BAD_REQUEST.getStatusCode(), "inode not found",
          ThirdPartyException.Source.LOCAL, "bad request");
    }
    Inode inode = inodeFacade.findById(inodeId);
    if (inode == null) {
      throw new ThirdPartyException(Response.Status.BAD_REQUEST.getStatusCode(), "inode not found",
          ThirdPartyException.Source.LOCAL, "bad request");
    }
    return inode;
  }

}
