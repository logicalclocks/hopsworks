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
