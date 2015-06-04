package se.kth.hopsworks.rest;

import java.io.IOException;
import java.io.InputStream;
import javax.annotation.security.RolesAllowed;
import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.SecurityContext;
import se.kth.bbc.fileoperations.FileOperations;
import se.kth.bbc.project.Project;
import se.kth.bbc.project.ProjectFacade;

/**
 *
 * @author stig
 */
@Path("/files")
@RolesAllowed({"SYS_ADMIN", "BBC_USER"})
@Produces(MediaType.APPLICATION_JSON)
@Stateless
@TransactionAttribute(TransactionAttributeType.NEVER)
public class FileService {

  @EJB
  private FileOperations fops;
  @EJB
  private ProjectFacade projects;

  /**
   * Download a file with given HDFS path, from the Project identified by
   * projectId.
   * The path can be both an absolute and relative path.
   * <p>
   * @param projectId The id of the project in which capacity the file is
   * downloaded.
   * @param path The HDFS path to download the file from. May be absolute (i.e.
   * hdfs://[...]), or relative to the project base directory.
   * @param sc
   * @param req
   * @return
   */
  @GET
  @Path("/{projectId}")
  @Produces(MediaType.APPLICATION_JSON)
  public Response downloadFile(@PathParam("projectId") Integer projectId,
          @Context SecurityContext sc,
          @Context HttpServletRequest req) {
    String path = "hdfs:///Projects//LifeGene/data/testfile";
    if (!path.startsWith("hdfs://")) {
      Project p = projects.find(projectId);
      if (p == null) {
        return Response.status(Response.Status.NOT_FOUND).build();
      }
      String projectname = p.getName();
      //relative path
      path = "hdfs://" + projectname + (path.startsWith("/") ? (path) : ("/"
              + path));
    }
    //Now we have an absolute path.
    InputStream is;
    try {
      is = fops.getInputStream(path);
    } catch (IOException e) {
      return Response.serverError().build();
    }
    return Response.ok(is).build();
  }

}
