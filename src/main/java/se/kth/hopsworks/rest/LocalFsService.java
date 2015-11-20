package se.kth.hopsworks.rest;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.ejb.EJB;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.enterprise.context.RequestScoped;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.GenericEntity;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.SecurityContext;
import se.kth.bbc.activity.ActivityFacade;
import se.kth.bbc.project.Project;
import se.kth.bbc.project.ProjectFacade;
import se.kth.bbc.project.fb.FsView;
import se.kth.bbc.project.fb.Inode;
import se.kth.bbc.project.fb.InodeView;
import se.kth.hopsworks.controller.DataSetDTO;
import se.kth.hopsworks.controller.ResponseMessages;
import se.kth.hopsworks.dataset.Dataset;
import se.kth.hopsworks.filters.AllowedRoles;
import se.kth.hopsworks.util.Settings;

@RequestScoped
@TransactionAttribute(TransactionAttributeType.NEVER)
public class LocalFsService {

  private final static Logger logger = Logger.getLogger(LocalFsService.class.
      getName());

  @EJB
  private ProjectFacade projectFacade;
//  @EJB
//  private DatasetRequestFacade datasetRequest;
  @EJB
  private ActivityFacade activityFacade;
//  @EJB
//  private UserManager userBean;
  @EJB
  private NoCacheResponse noCacheResponse;
//  @EJB
//  private FileOperations fileOps;

  @EJB
  private Settings settings;
  @EJB
  DownloadService downloader;

  private Integer projectId;
  private Project project;
  private String path;
//  private Dataset dataset;

  public LocalFsService() {
  }

  public void setProjectId(Integer projectId) {
    this.projectId = projectId;
    this.project = this.projectFacade.find(projectId);
    String rootDir = Settings.DIR_ROOT;
    String projectPath = File.separator + rootDir + File.separator
        + this.project.getName();
    this.path = projectPath + File.separator;
  }

  public Integer getProjectId() {
    return projectId;
  }

  private Response dirListing(String path) {
    File baseDir = new File(path);
    if (baseDir.exists() == false || baseDir.isDirectory() == false) {
      return noCacheResponse.getNoCacheResponseBuilder(Response.Status.INTERNAL_SERVER_ERROR).build();
    }
    List<FsView> results = new ArrayList<>();
    File[] files = baseDir.listFiles();

    for (File file : files) {
      if (file.isDirectory()) {
        results.add(new FsView(file.getAbsolutePath(), true));
      } else {
        results.add(new FsView(file.getAbsolutePath(), false));
      }
    }

    GenericEntity<List<FsView>> fileViews = new GenericEntity<List<FsView>>(results) {
    };
    return noCacheResponse.getNoCacheResponseBuilder(Response.Status.OK).entity(
        fileViews).build();
  }

  @GET
  @Produces(MediaType.APPLICATION_JSON)
  @AllowedRoles(roles = {AllowedRoles.DATA_SCIENTIST, AllowedRoles.DATA_OWNER})
  public Response findFilesInCharonProjectID(
      @Context SecurityContext sc,
      @Context HttpServletRequest req) throws AppException {
    return dirListing(settings.getCharonDir());
  }

  /**
   * Get the inodes in the given project-relative path.
   * <p/>
   * @param path
   * @param sc
   * @param req
   * @return
   * @throws AppException
   */
  @GET
  @Path("/{path: .+}")
  @Produces(MediaType.APPLICATION_JSON)
  @AllowedRoles(roles = {AllowedRoles.DATA_SCIENTIST, AllowedRoles.DATA_OWNER})
  public Response getDirContent(
      @PathParam("path") String path,
      @Context SecurityContext sc,
      @Context HttpServletRequest req) throws AppException {
    return dirListing(path);
  }

  @POST
  @Produces(MediaType.APPLICATION_JSON)
  @AllowedRoles(roles = {AllowedRoles.DATA_OWNER})
  public Response createDataSetDir(
      String path,
      @Context SecurityContext sc,
      @Context HttpServletRequest req) throws AppException {
    JsonResponse json = new JsonResponse();
    File f = new File(path);
    if (f.exists()) {
      json.setErrorMsg("File already exists: " + path);
    } else {
      boolean res = f.mkdir();
      if (res) {
        json.setSuccessMessage("Created directory: " + path);
            return noCacheResponse.getNoCacheResponseBuilder(Response.Status.OK).entity(
        json).build();
      } else {
        json.setErrorMsg("Could not create directory: " + path);
      }
    }

    return noCacheResponse.getNoCacheResponseBuilder(Response.Status.INTERNAL_SERVER_ERROR).entity(
        json).build();
  }

  @DELETE
  @Path("/{fileName: .+}")
  @Produces(MediaType.APPLICATION_JSON)
  @AllowedRoles(roles = {AllowedRoles.DATA_OWNER})
  public Response removedataSetdir(
      @PathParam("fileName") String fileName,
      @Context SecurityContext sc,
      @Context HttpServletRequest req) throws AppException {
    boolean success = false;
    JsonResponse json = new JsonResponse();
    if (fileName == null || fileName.isEmpty()) {
      throw new AppException(Response.Status.BAD_REQUEST.getStatusCode(),
          ResponseMessages.DATASET_NAME_EMPTY);
    }
    File f = new File(fileName);
    boolean res = f.delete();

    if (res) {
      json.setSuccessMessage(ResponseMessages.DATASET_REMOVED_FROM_HDFS);
    } else {
      json.setErrorMsg(ResponseMessages.FILE_NOT_FOUND);
    }
    return noCacheResponse.getNoCacheResponseBuilder(Response.Status.OK).entity(
        json).build();

  }

  @GET
  @Path("fileExists/{path: .+}")
  @Produces(MediaType.APPLICATION_JSON)
  @AllowedRoles(roles = {AllowedRoles.DATA_SCIENTIST, AllowedRoles.DATA_OWNER})
  public Response checkFileExist(@PathParam("path") String path) throws
      AppException {
    if (path == null) {
      path = "";
    }
    File f = new File(path);
    boolean exists = f.exists();

    String message = "";
    JsonResponse response = new JsonResponse();

    //if it exists and it's not a dir, it must be a file
    if (exists) {
      message = "FILE";
      response.setSuccessMessage(message);
      return noCacheResponse.getNoCacheResponseBuilder(Response.Status.OK).
          entity(response).build();
    }
    throw new AppException(Response.Status.BAD_REQUEST.getStatusCode(),
        "The requested path does not resolve to a valid file");
  }

  @GET
  @Path("isDir/{path: .+}")
  @Produces(MediaType.APPLICATION_JSON)
  @AllowedRoles(roles = {AllowedRoles.DATA_SCIENTIST, AllowedRoles.DATA_OWNER})
  public Response isDir(@PathParam("path") String path) throws
      AppException {

    if (path == null) {
      path = "";
    }

    File f = new File(path);
    boolean exists = f.exists();
    boolean isDir = f.isDirectory();

    String message = "";
    JsonResponse response = new JsonResponse();

    //if it exists and it's not a dir, it must be a file
    if (exists && !isDir) {
      message = "FILE";
      response.setSuccessMessage(message);
      return noCacheResponse.getNoCacheResponseBuilder(Response.Status.OK).
          entity(response).build();
    } else if (exists && isDir) {
      message = "DIR";
      response.setSuccessMessage(message);
      return noCacheResponse.getNoCacheResponseBuilder(Response.Status.OK).
          entity(response).build();
    }

    throw new AppException(Response.Status.BAD_REQUEST.getStatusCode(),
        "The requested path does not resolve to a valid dir");
  }

}
