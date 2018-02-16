/*
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
 *
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
import io.hops.hopsworks.dela.cluster.ClusterDatasetController;
import io.hops.hopsworks.dela.exception.ThirdPartyException;
import io.swagger.annotations.Api;
import java.util.logging.Logger;
import javax.annotation.security.RolesAllowed;
import javax.ejb.EJB;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.enterprise.context.RequestScoped;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.SecurityContext;

@RolesAllowed({"HOPS_ADMIN", "HOPS_USER"})
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@RequestScoped
@TransactionAttribute(TransactionAttributeType.NEVER)
@Api(value = "Dela Cluster Project Service",
  description = "Dela Cluster Project Service")
public class DelaClusterProjectService {
  private final static Logger logger = Logger.getLogger(DelaClusterProjectService.class.getName());
  
  @EJB
  private NoCacheResponse noCacheResponse;
  @EJB
  private ProjectFacade projectFacade;
  @EJB
  private InodeFacade inodeFacade;
  @EJB
  private DatasetFacade datasetFacade;
  @EJB
  private ClusterDatasetController clusterCtrl;
  
  private Project project;
  private Integer projectId;
  
  @POST
  @Produces(MediaType.APPLICATION_JSON)
  @AllowedProjectRoles({AllowedProjectRoles.DATA_OWNER})
  public Response share(@Context SecurityContext sc, InodeIdDTO inodeId) throws ThirdPartyException {
    Inode inode = getInode(inodeId.getId());
    Dataset dataset = getDatasetByInode(inode);
    clusterCtrl.shareWithCluster(dataset);
    JsonResponse json = new JsonResponse();
    json.setSuccessMessage("Dataset transfer is started - published");
    return successResponse(json);
  }
  
  @DELETE
  @Path("/{inodeId}")
  @Produces(MediaType.APPLICATION_JSON)
  @AllowedProjectRoles({AllowedProjectRoles.DATA_OWNER})
  public Response removePublic(@Context SecurityContext sc, @PathParam("inodeId") Integer inodeId) 
    throws ThirdPartyException {
    Inode inode = getInode(inodeId);
    Dataset dataset = getDatasetByInode(inode);
    clusterCtrl.unshareFromCluster(dataset);
    JsonResponse json = new JsonResponse();
    json.setSuccessMessage("Dataset is now private");
    return successResponse(json);
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
  
  private Dataset getDatasetByInode(Inode inode) throws ThirdPartyException {
    Dataset dataset = datasetFacade.findByProjectAndInode(this.project, inode);
    if (dataset == null) {
      throw new ThirdPartyException(Response.Status.BAD_REQUEST.getStatusCode(), "dataset not found",
        ThirdPartyException.Source.LOCAL, "bad request");
    }
    return dataset;
  }
  
  private Response successResponse(Object content) {
    return noCacheResponse.getNoCacheResponseBuilder(Response.Status.OK).entity(content).build();
  }
  
  public void setProjectId(Integer projectId) {
    this.projectId = projectId;
    this.project = projectFacade.find(projectId);
  }

  public Integer getProjectId() {
    return projectId;
  }
}
