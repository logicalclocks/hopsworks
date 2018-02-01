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
package io.hops.hopsworks.apiV2.projects;

import io.hops.hopsworks.apiV2.filter.AllowedProjectRoles;
import io.hops.hopsworks.common.constants.message.ResponseMessages;
import io.hops.hopsworks.common.dao.dataset.Dataset;
import io.hops.hopsworks.common.dao.dataset.DatasetPermissions;
import io.hops.hopsworks.common.dao.project.Project;
import io.hops.hopsworks.common.dao.user.UserFacade;
import io.hops.hopsworks.common.dao.user.Users;
import io.hops.hopsworks.common.exception.AppException;
import io.hops.hopsworks.common.hdfs.DistributedFileSystemOps;
import io.hops.hopsworks.common.hdfs.DistributedFsService;
import io.hops.hopsworks.common.hdfs.HdfsUsersController;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.apache.hadoop.fs.FSDataInputStream;
import org.apache.hadoop.security.AccessControlException;

import javax.ejb.EJB;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.SecurityContext;
import javax.ws.rs.core.StreamingOutput;
import java.io.IOException;
import java.io.OutputStream;
import java.util.logging.Level;
import java.util.logging.Logger;

@Api("Blobs")
@RequestScoped
@TransactionAttribute(TransactionAttributeType.NEVER)
public class BlobsResource {
  private final static Logger logger = Logger.getLogger(BlobsResource.class.getName());
  
  @EJB
  private PathValidator pathValidator;
  @EJB
  private UserFacade userFacade;
  @EJB
  private HdfsUsersController hdfsUsersBean;
  @Inject
  private DistributedFsService dfs;

  private Dataset ds;
  
  private Project project;
  
  public void setDataset(Dataset ds){
    this.ds = ds;
  }
  
  public void setProject(Project project){
    this.project = project;
  }
  
  @ApiOperation("Download a file")
  @GET
  @Path("/{path: .+}")
  @AllowedProjectRoles({AllowedProjectRoles.DATA_SCIENTIST, AllowedProjectRoles.DATA_OWNER})
  public Response downloadFile(@PathParam("path") String path,
      @Context SecurityContext sc) throws AppException, AccessControlException {
    if(ds == null){
      throw new AppException(Response.Status.NOT_FOUND, "Data set not found.");
    }
    
    if (ds.isShared() && ds.getEditable() == DatasetPermissions.OWNER_ONLY && !ds.isPublicDs()) {
      throw new AppException(Response.Status.BAD_REQUEST.getStatusCode(),
          ResponseMessages.DOWNLOAD_ERROR);
    }
    
    Users user = userFacade.findByEmail(sc.getUserPrincipal().getName());
    String hdfsUserName = hdfsUsersBean.getHdfsUserName(project, user);
    
    DatasetPath dsPath = new DatasetPath(ds, path);
    org.apache.hadoop.fs.Path fullPath = pathValidator.getFullPath(dsPath);
    return downloadFromHdfs(hdfsUserName, fullPath);
  }
  
  private Response downloadFromHdfs(String projectUsername, org.apache.hadoop.fs.Path fullPath) throws AppException,
      AccessControlException {
    
    FSDataInputStream stream;
    DistributedFileSystemOps udfso;
    try {
      if (projectUsername != null) {
        udfso = dfs.getDfsOps(projectUsername);
        stream = udfso.open(fullPath);
        return Response.ok(buildOutputStream(stream, udfso))
            .header("Content-disposition", "attachment;")
            .build();
      } else {
        throw new AppException(Response.Status.INTERNAL_SERVER_ERROR,
            "No matching HDFS-user found.");
      }
      
    } catch (AccessControlException ex) {
      throw new AccessControlException(
          "Permission denied: You can not download the file ");
    } catch (IOException ex) {
      logger.log(Level.SEVERE, null, ex);
      throw new AppException(Response.Status.NOT_FOUND,
          "File does not exist: " + fullPath);
    }
  }
  
  private StreamingOutput buildOutputStream(final FSDataInputStream stream,
      final DistributedFileSystemOps udfso) {
    return (OutputStream out) -> {
      try {
        int length;
        byte[] buffer = new byte[1024];
        while ((length = stream.read(buffer)) != -1) {
          out.write(buffer, 0, length);
        }
        out.flush();
        stream.close();
      } finally {
        dfs.closeDfsClient(udfso);
      }
    };
  }
  
}
