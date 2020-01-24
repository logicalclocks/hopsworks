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
import io.hops.hopsworks.common.dao.hdfs.inode.Inode;
import io.hops.hopsworks.common.dao.hdfs.inode.InodeFacade;
import io.hops.hopsworks.common.dao.project.Project;
import io.hops.hopsworks.common.dao.project.ProjectFacade;
import io.hops.hopsworks.common.hdfs.DistributedFileSystemOps;
import io.hops.hopsworks.common.hdfs.DistributedFsService;
import io.hops.hopsworks.common.hdfs.inode.InodeController;
import io.hops.hopsworks.common.provenance.core.Provenance;
import io.hops.hopsworks.common.provenance.core.elastic.ElasticCache;
import io.hops.hopsworks.common.provenance.core.elastic.dto.ElasticIndexMappingDTO;
import io.hops.hopsworks.exceptions.ElasticException;
import io.hops.hopsworks.exceptions.ProvenanceException;
import io.hops.hopsworks.jwt.annotation.JWTRequired;
import io.hops.hopsworks.restutils.RESTCodes;
import io.swagger.annotations.Api;
import org.apache.hadoop.fs.XAttrSetFlag;

import javax.ejb.EJB;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.enterprise.context.RequestScoped;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.IOException;
import java.util.EnumSet;
import java.util.Map;
import java.util.logging.Level;

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
  private DistributedFsService dfs;
  @EJB
  private ElasticCache cache;

  private Project project;

  public void setProjectId(Integer projectId) {
    this.project = projectFacade.find(projectId);
  }

  @POST
  @Path("xattr")
  @Produces(MediaType.APPLICATION_JSON)
    @AllowedProjectRoles({AllowedProjectRoles.DATA_SCIENTIST, AllowedProjectRoles.DATA_OWNER})
    @JWTRequired(acceptedTokens = {Audience.API}, allowedUserRoles = {"HOPS_ADMIN", "HOPS_USER"})
  public Response testExpAddAppId(
    @QueryParam("inodeId") Long inodeId,
    @QueryParam("xattrName") String xattrName,
    @QueryParam("xattrValue") String xattrValue)
    throws ProvenanceException {
    Inode inode = inodeFacade.findById(inodeId);
    String path = inodeCtrl.getPath(inode);
    DistributedFileSystemOps dfso = dfs.getDfsOps();
    createXAttr(dfso, path, "provenance." + xattrName, xattrValue);
    return Response.ok().build();
  }

  private void createXAttr(DistributedFileSystemOps dfso, String datasetPath, String key, String val)
    throws ProvenanceException {
    EnumSet<XAttrSetFlag> flags = EnumSet.noneOf(XAttrSetFlag.class);
    flags.add(XAttrSetFlag.CREATE);
    xattrOp(dfso, datasetPath, flags, key, val);
  }

  private void xattrOp(DistributedFileSystemOps dfso, String datasetPath, EnumSet<XAttrSetFlag> flags,
    String key, String val)
    throws ProvenanceException {
    try {
      dfso.setXAttr(datasetPath, key, val.getBytes(), flags);
    } catch (IOException e) {
      throw new ProvenanceException(RESTCodes.ProvenanceErrorCode.FS_ERROR, Level.INFO,
        "xattrs persistance exception");
    }
  }

  @GET
  @Path("index/mapping")
  @Produces(MediaType.APPLICATION_JSON)
    @AllowedProjectRoles({AllowedProjectRoles.DATA_SCIENTIST, AllowedProjectRoles.DATA_OWNER})
    @JWTRequired(acceptedTokens = {Audience.API}, allowedUserRoles = {"HOPS_ADMIN", "HOPS_USER"})
  public Response testGetIndexMapping() throws ElasticException {
    String index = Provenance.getProjectIndex(project);
    Map<String, String> mapping = cache.mngIndexGetMapping(index, true);
    return Response.ok().entity(new ElasticIndexMappingDTO(index, mapping)).build();
  }
}
