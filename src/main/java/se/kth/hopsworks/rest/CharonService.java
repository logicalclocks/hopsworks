package se.kth.hopsworks.rest;

import io.hops.bbc.CharonDTO;
import java.io.File;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.ejb.EJB;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.enterprise.context.RequestScoped;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.SecurityContext;
import se.kth.bbc.activity.ActivityFacade;
import se.kth.bbc.fileoperations.FileOperations;
import se.kth.bbc.project.Project;
import se.kth.bbc.project.fb.InodeFacade;
import javax.ws.rs.POST;
import se.kth.hopsworks.filters.AllowedRoles;

@RequestScoped
@TransactionAttribute(TransactionAttributeType.NEVER)
public class CharonService {

  private static final Logger logger = Logger.getLogger(CharonService.class.
      getName());

  private final String CHARON_PATH = "/srv/Charon";

  private final String charonMountPointPath = "/srv/charon_fs";

  private final String addNewGranteePath = CHARON_PATH + File.separator + "NewSiteIds";
  private final String addNewSNSPath = CHARON_PATH + File.separator + "NewSNSs";
  private final String addedGrantees = CHARON_PATH + File.separator + "config/addedGrantees";

  @EJB
  private NoCacheResponse noCacheResponse;
  @EJB
  private FileOperations fops;
  @EJB
  private ActivityFacade activityFacade;
  @EJB
  private InodeFacade inodeFacade;
  @EJB
  private FileOperations fileOps;

  private Project project;

  CharonService setProject(Project project) {
    this.project = project;
    return this;
  }

  @GET
  @Consumes(MediaType.APPLICATION_JSON)
  @AllowedRoles(roles = {AllowedRoles.DATA_SCIENTIST, AllowedRoles.DATA_OWNER})
  public Response copyFromHDFS(@Context SecurityContext sc,
      @Context HttpServletRequest req, CharonDTO charon)
      throws AppException {
    JsonResponse json = new JsonResponse();

    String src = charon.getHdfsPath();
    String dest = charon.getCharonPath();

    if (src == null || dest == null) {
      throw new AppException(Response.Status.BAD_REQUEST.getStatusCode(),
          "Some of the paths 'from' and 'to' are set to null!");
    }

    try {
      fileOps.copyToLocal(src, dest);
    } catch (IOException ex) {
      Logger.getLogger(CharonService.class.getName()).log(Level.SEVERE, null, ex);
      throw new AppException(Response.Status.SERVICE_UNAVAILABLE.getStatusCode(),
          "Could not copy file from Charon to HDFS.");
    }

    json.setSuccessMessage("File copied successfully from HDFS to Charon .");
    Response.ResponseBuilder response = Response.ok();
    return response.entity(json).build();
  }

  @POST
  @Consumes(MediaType.APPLICATION_JSON)
  @AllowedRoles(roles = {AllowedRoles.DATA_SCIENTIST, AllowedRoles.DATA_OWNER})
  public Response copyToHDFS(@Context SecurityContext sc,
      @Context HttpServletRequest req, CharonDTO charon)
      throws AppException {
    JsonResponse json = new JsonResponse();

    String src = charon.getCharonPath();
    String dest = charon.getHdfsPath();

    if (src == null || dest == null) {
      throw new AppException(Response.Status.BAD_REQUEST.getStatusCode(),
          "Some of the paths 'from' and 'to' are set to null!");
    }

    try {
      fileOps.copyToHDFSFromLocal(false, src, dest);
    } catch (IOException ex) {
      Logger.getLogger(CharonService.class.getName()).log(Level.SEVERE, null, ex);
      throw new AppException(Response.Status.SERVICE_UNAVAILABLE.getStatusCode(),
          "Could not copy file from Charon to HDFS.");
    }

    json.setSuccessMessage("File copied successfully from Charon to HDFS.");
    Response.ResponseBuilder response = Response.ok();
    return response.entity(json).build();
  }

//  
//   private boolean isHDFSDir(String path) throws IOException {
//        boolean flag = hopsFS.isDirectory(new org.apache.hadoop.fs.Path(path));
//        return flag;
//    }
//    
//    private boolean isHDFSFile(String path) throws IOException {
//        boolean flag = hopsFS.isFile(new org.apache.hadoop.fs.Path(path));
//        return flag;
//    }
  private boolean isLocalFile(String path) {
    File file = new File(path);
    return file.isFile();
  }

  private boolean isLocalDir(String path) {
    File file = new File(path);
    return file.isDirectory();
  }

  /**
   * Check if the given path exists
   *
   * @param path - the path
   * @return - true is the path exists, false otherwise
   */
  private boolean isFile(String path) {
    File file = new File(charonMountPointPath + parsePath(path));
    return file.isFile();
  }

  /**
   * Check if the given path is a directory
   *
   * @param path - the path
   * @return - true is the path is a directory, false otherwise
   */
  private boolean isDirectory(String path) {
    File file = new File(charonMountPointPath + parsePath(path));
    return file.isDirectory();
  }

  /**
   * Get Metadata info about a node
   *
   * @param path - the path of the node we want information about
   * @return
   */
  private String[] getMetadata(String path) {

    // ADD MORE INFO : add more fields.
    String[] res = new String[2];
    File file = new File(charonMountPointPath + parsePath(path));

    res[0] = file.isDirectory() ? "DIR" : "FILE";
    res[1] = file.getName();

    return res;
  }

  private String parsePath(String p) {
    if (!p.startsWith("/")) {
      return "/".concat(p);
    }
    return p;
  }

  private String parseLocation(String location) {
    if (location == null) {
      return null;
    }

    switch (location) {
      case "coc":
        return null;
      case "single":
        return "cloud";
      case "hdfs":
        return "external";
      case "local":
        return "local";
      default:
        return null;
    }
  }

}
