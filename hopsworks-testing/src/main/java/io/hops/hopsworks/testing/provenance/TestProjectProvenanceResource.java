/*
 * This file is part of Hopsworks
 * Copyright (C) 2019, Logical Clocks AB. All rights reserved
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
 */
package io.hops.hopsworks.testing.provenance;

import io.hops.hopsworks.api.filter.AllowedProjectRoles;
import io.hops.hopsworks.api.filter.Audience;
import io.hops.hopsworks.common.dao.hdfs.inode.InodeFacade;
import io.hops.hopsworks.common.hdfs.DistributedFileSystemOps;
import io.hops.hopsworks.common.hdfs.DistributedFsService;
import io.hops.hopsworks.exceptions.DatasetException;
import io.hops.hopsworks.exceptions.MetadataException;
import io.hops.hopsworks.persistence.entity.project.Project;
import io.hops.hopsworks.common.dao.project.ProjectFacade;
import io.hops.hopsworks.common.hdfs.inode.InodeController;
import io.hops.hopsworks.common.hdfs.xattrs.XAttrsController;
import io.hops.hopsworks.common.provenance.core.Provenance;
import io.hops.hopsworks.common.provenance.core.opensearch.OpenSearchCache;
import io.hops.hopsworks.common.provenance.core.opensearch.dto.OpenSearchIndexMappingDTO;
import io.hops.hopsworks.exceptions.OpenSearchException;
import io.hops.hopsworks.jwt.annotation.JWTRequired;
import io.hops.hopsworks.persistence.entity.hdfs.inode.Inode;
import io.swagger.annotations.Api;

import javax.ejb.EJB;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.enterprise.context.RequestScoped;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.SecurityContext;
import java.util.Map;

@RequestScoped
@TransactionAttribute(TransactionAttributeType.NEVER)
@Api(value = "Provenance Testing Service", description = "Provenance Testing Service")
public class TestProjectProvenanceResource {
  @EJB
  private ProjectFacade projectFacade;
  @EJB
  private InodeFacade inodeFacade;
  @EJB
  private InodeController inodeCtrl;
  @EJB
  private OpenSearchCache cache;
  @EJB
  private XAttrsController xattrCtrl;
  @EJB
  private DistributedFsService dfs;

  private Project project;

  public void setProjectId(Integer projectId) {
    this.project = projectFacade.find(projectId);
  }

  @POST
  @Path("xattr")
  @Produces(MediaType.APPLICATION_JSON)
    @AllowedProjectRoles({AllowedProjectRoles.DATA_SCIENTIST, AllowedProjectRoles.DATA_OWNER})
    @JWTRequired(acceptedTokens = {Audience.API}, allowedUserRoles = {"HOPS_ADMIN", "HOPS_USER"})
  public Response upsertXAttr (
    @QueryParam("inodeId") Long inodeId,
    @QueryParam("xattrName") String xattrName,
    @QueryParam("xattrValue") String xattrValue) throws MetadataException, DatasetException {
    Inode inode = inodeFacade.findById(inodeId);
    String path = inodeCtrl.getPath(inode);
    DistributedFileSystemOps dfso = dfs.getDfsOps();
    try {
      xattrCtrl.upsertProvXAttr(dfso, path, xattrName, xattrValue.getBytes());
    } finally {
      if (dfso != null) {
        dfs.closeDfsClient(dfso);
      }
    }
    return Response.ok().build();
  }
  
  @DELETE
  @Path("xattr")
  @Produces(MediaType.APPLICATION_JSON)
  @AllowedProjectRoles({AllowedProjectRoles.DATA_SCIENTIST, AllowedProjectRoles.DATA_OWNER})
  @JWTRequired(acceptedTokens = {Audience.API}, allowedUserRoles = {"HOPS_ADMIN", "HOPS_USER"})
  public Response removeXAttr(@Context SecurityContext sc,
    @QueryParam("inodeId") Long inodeId,
    @QueryParam("xattrName") String xattrName) throws MetadataException, DatasetException {
    Inode inode = inodeFacade.findById(inodeId);
    String path = inodeCtrl.getPath(inode);
    DistributedFileSystemOps dfso = dfs.getDfsOps();
    try {
      xattrCtrl.removeProvXAttr(dfso, path, xattrName);
    } finally {
      if (dfso != null) {
        dfs.closeDfsClient(dfso);
      }
    }
    return Response.ok().build();
  }

  @GET
  @Path("index/mapping")
  @Produces(MediaType.APPLICATION_JSON)
    @AllowedProjectRoles({AllowedProjectRoles.DATA_SCIENTIST, AllowedProjectRoles.DATA_OWNER})
    @JWTRequired(acceptedTokens = {Audience.API}, allowedUserRoles = {"HOPS_ADMIN", "HOPS_USER"})
  public Response testGetIndexMapping() throws OpenSearchException {
    String index = Provenance.getProjectIndex(project);
    Map<String, String> mapping = cache.mngIndexGetMapping(index, true);
    return Response.ok().entity(new OpenSearchIndexMappingDTO(index, mapping)).build();
  }
}
