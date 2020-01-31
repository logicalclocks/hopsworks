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
import io.hops.hopsworks.api.filter.Audience;
import io.hops.hopsworks.api.filter.NoCacheResponse;
import io.hops.hopsworks.api.util.RESTApiJsonResponse;
import io.hops.hopsworks.common.dao.dataset.Dataset;
import io.hops.hopsworks.common.dao.dataset.DatasetFacade;
import io.hops.hopsworks.common.dao.hdfs.inode.Inode;
import io.hops.hopsworks.common.dao.hdfs.inode.InodeFacade;
import io.hops.hopsworks.common.dao.project.Project;
import io.hops.hopsworks.common.dao.project.ProjectFacade;
import io.hops.hopsworks.restutils.RESTCodes;
import io.hops.hopsworks.dela.cluster.ClusterDatasetController;
import io.hops.hopsworks.exceptions.DelaException;
import io.hops.hopsworks.jwt.annotation.JWTRequired;
import io.swagger.annotations.Api;
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
import java.util.logging.Level;
import java.util.logging.Logger;

@RequestScoped
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Api(value = "Dela Cluster Project Service",
  description = "Dela Cluster Project Service")
@TransactionAttribute(TransactionAttributeType.NEVER)
public class DelaClusterProjectService {
  private static final Logger LOGGER = Logger.getLogger(DelaClusterProjectService.class.getName());
  
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
  @JWTRequired(acceptedTokens={Audience.API}, allowedUserRoles={"HOPS_ADMIN", "HOPS_USER"})
  public Response share(InodeIdDTO inodeId, @Context SecurityContext sc) throws DelaException {
    Inode inode = getInode(inodeId.getId());
    Dataset dataset = getDatasetByInode(inode);
    clusterCtrl.shareWithCluster(this.project, dataset);
    RESTApiJsonResponse json = new RESTApiJsonResponse();
    json.setSuccessMessage("Dataset transfer is started - published");
    return successResponse(json);
  }
  
  @DELETE
  @Path("/{inodeId}")
  @Produces(MediaType.APPLICATION_JSON)
  @AllowedProjectRoles({AllowedProjectRoles.DATA_OWNER})
  @JWTRequired(acceptedTokens={Audience.API}, allowedUserRoles={"HOPS_ADMIN", "HOPS_USER"})
  public Response removePublic(@PathParam("inodeId") Long inodeId, @Context SecurityContext sc) throws DelaException {
    Inode inode = getInode(inodeId);
    Dataset dataset = getDatasetByInode(inode);
    clusterCtrl.unshareFromCluster(this.project, dataset);
    RESTApiJsonResponse json = new RESTApiJsonResponse();
    json.setSuccessMessage("Dataset is now private");
    return successResponse(json);
  }
  
  private Inode getInode(Long inodeId) throws DelaException {
    if (inodeId == null) {
      throw new DelaException(RESTCodes.DelaErrorCode.ILLEGAL_ARGUMENT, Level.FINE, DelaException.Source.LOCAL,
        "inodeId was not provided.");
    }
    return inodeFacade.findById(inodeId);
  }
  
  private Dataset getDatasetByInode(Inode inode) throws DelaException {
    Dataset dataset = datasetFacade.findByProjectAndInode(this.project, inode);
    if (dataset == null) {
      throw new DelaException(RESTCodes.DelaErrorCode.DATASET_DOES_NOT_EXIST, Level.FINE, DelaException.Source.LOCAL);
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
