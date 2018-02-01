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

package io.hops.hopsworks.api.util;

import java.io.IOException;
import java.io.OutputStream;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.ejb.EJB;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.enterprise.context.RequestScoped;
import javax.ws.rs.GET;
import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.StreamingOutput;

import org.apache.hadoop.fs.FSDataInputStream;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.security.AccessControlException;
import io.hops.hopsworks.api.filter.AllowedProjectRoles;
import io.hops.hopsworks.api.project.util.DsPath;
import io.hops.hopsworks.api.project.util.PathValidator;
import io.hops.hopsworks.common.constants.message.ResponseMessages;
import io.hops.hopsworks.common.dao.dataset.Dataset;
import io.hops.hopsworks.common.dao.dataset.DatasetPermissions;
import io.hops.hopsworks.common.dao.project.Project;
import io.hops.hopsworks.common.exception.AppException;
import io.hops.hopsworks.common.hdfs.DistributedFileSystemOps;
import io.hops.hopsworks.common.hdfs.DistributedFsService;
import javax.ws.rs.PathParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.SecurityContext;

@RequestScoped
@TransactionAttribute(TransactionAttributeType.NEVER)
public class DownloadService {

  private static final Logger LOG = Logger.getLogger(DownloadService.class.getName());

  @EJB
  private DistributedFsService dfs;
  @EJB
  private PathValidator pathValidator;

  private String projectUsername;
  private Project project;

  public DownloadService() {
  }

  public void setProjectUsername(String projectUsername) {
    this.projectUsername = projectUsername;
  }

  public void setProject(Project project) {
    this.project = project;
  }

  @GET
  @javax.ws.rs.Path("/{path: .+}")
  @Produces(MediaType.APPLICATION_OCTET_STREAM)
  @AllowedProjectRoles({AllowedProjectRoles.DATA_SCIENTIST, AllowedProjectRoles.DATA_OWNER})
  public Response downloadFromHDFS(@PathParam("path") String path, @Context SecurityContext sc) throws AppException,
      AccessControlException {

    DsPath dsPath = pathValidator.validatePath(this.project, path);
    String fullPath = dsPath.getFullPath().toString();
    Dataset ds = dsPath.getDs();
    if (ds.isShared() && ds.getEditable()==DatasetPermissions.OWNER_ONLY && !ds.isPublicDs()) {
      throw new AppException(Response.Status.BAD_REQUEST.getStatusCode(),
          ResponseMessages.DOWNLOAD_ERROR);
    }

    FSDataInputStream stream;
    DistributedFileSystemOps udfso;
    try {
      if (projectUsername != null) {
        udfso = dfs.getDfsOps(projectUsername);
        stream = udfso.open(new Path(fullPath));
        Response.ResponseBuilder response = Response.ok(buildOutputStream(stream, udfso));
        response.header("Content-disposition", "attachment;");
        return response.build();
      } else {
        throw new AppException(Response.Status.BAD_REQUEST.getStatusCode(),
            ResponseMessages.DOWNLOAD_ERROR);
      }

    } catch (AccessControlException ex) {
      throw new AccessControlException(
          "Permission denied: You can not download the file ");
    } catch (IOException ex) {
      LOG.log(Level.SEVERE, null, ex);
      throw new AppException(Response.Status.BAD_REQUEST.getStatusCode(),
          "File does not exist: " + fullPath);
    }
  }

  /**
   *
   * @param stream
   * @return
   */
  private StreamingOutput buildOutputStream(final FSDataInputStream stream,
      final DistributedFileSystemOps udfso) {
    StreamingOutput output = new StreamingOutput() {
      @Override
      public void write(OutputStream out) throws IOException,
          WebApplicationException {
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
      }
    };

    return output;
  }

}
