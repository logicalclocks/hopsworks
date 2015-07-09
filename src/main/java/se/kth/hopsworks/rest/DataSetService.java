package se.kth.hopsworks.rest;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.ejb.EJB;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;
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
import javax.ws.rs.core.Response.ResponseBuilder;
import javax.ws.rs.core.SecurityContext;
import se.kth.bbc.fileoperations.FileOperations;
import se.kth.bbc.lims.Constants;
import se.kth.bbc.project.Project;
import se.kth.bbc.project.ProjectFacade;
import se.kth.bbc.project.fb.Inode;
import se.kth.bbc.project.fb.InodeFacade;
import se.kth.bbc.project.fb.InodeView;
import se.kth.hopsworks.controller.DataSetDTO;
import se.kth.hopsworks.controller.FolderNameValidator;
import se.kth.hopsworks.controller.ResponseMessages;
import se.kth.hopsworks.filters.AllowedRoles;

/**
 * @author André<amore@kth.se>
 * @author Ermias<ermiasg@kth.se>
 */
@RequestScoped
@TransactionAttribute(TransactionAttributeType.NEVER)
public class DataSetService {

  private final static Logger logger = Logger.getLogger(DataSetService.class.
          getName());

  @EJB
  private ProjectFacade projectFacade;
  @EJB
  private NoCacheResponse noCacheResponse;
  @EJB
  private FileOperations fileOps;
  @EJB
  private InodeFacade inodes;
  @EJB
  private FolderNameValidator datasetNameValidator;
  @Inject
  private UploadService uploader;

  private Integer projectId;
  private Project project;
  private String path;

  public DataSetService() {
  }

  public void setProjectId(Integer projectId) {
    this.projectId = projectId;
    this.project = projectFacade.find(projectId);
    String rootDir = Constants.DIR_ROOT;
    String projectPath = File.separator + rootDir + File.separator
            + this.project.getName();
    this.path = projectPath + File.separator;
  }

  public Integer getProjectId() {
    return projectId;
  }

  @GET
  @Produces(MediaType.APPLICATION_JSON)
  @AllowedRoles(roles = {AllowedRoles.DATA_SCIENTIST, AllowedRoles.DATA_OWNER})
  public Response findDataSetsInProjectID(
          @Context SecurityContext sc,
          @Context HttpServletRequest req) throws AppException {

    Inode parent = inodes.getProjectRoot(this.project.getName());
    List<Inode> cwdChildren;
    cwdChildren = inodes.findByParent(parent);
    List<InodeView> kids = new ArrayList<>();
    for (Inode i : cwdChildren) {
      kids.add(new InodeView(i, inodes.getPath(i)));
    }
    GenericEntity<List<InodeView>> inodViews
            = new GenericEntity<List<InodeView>>(kids) {
            };

    return noCacheResponse.getNoCacheResponseBuilder(Response.Status.OK).entity(
            inodViews).build();
  }

  /**
   * Get the inodes in the given project-relative path.
   * <p>
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
    //Strip leading slashes.
    while (path.startsWith("/")) {
      path = path.substring(1);
    }
    String fullpath = "/" + Constants.DIR_ROOT + "/" + project.getName() + "/"
            + path;
    List<Inode> cwdChildren;
    try {
      cwdChildren = inodes.getChildren(fullpath);
    } catch (IllegalArgumentException ex) {
      logger.log(Level.WARNING, "Trying to access children of file.", ex);
      throw new AppException(Response.Status.NO_CONTENT.getStatusCode(),
              "Cannot list the directory contents of a regular file.");
    } catch (FileNotFoundException ex) {
      logger.log(Level.WARNING, "Trying to access non-existent path.", ex);
      throw new AppException(Response.Status.NOT_FOUND.getStatusCode(),
              "Path not found.");
    }
    List<InodeView> kids = new ArrayList<>();
    for (Inode i : cwdChildren) {
      kids.add(new InodeView(i, inodes.getPath(i)));
    }
    GenericEntity<List<InodeView>> inodViews
            = new GenericEntity<List<InodeView>>(kids) {
            };

    return noCacheResponse.getNoCacheResponseBuilder(Response.Status.OK).entity(
            inodViews).build();
  }

  @GET
  @Path("download/{filePath: .+}")
  @Produces(MediaType.APPLICATION_OCTET_STREAM)
  @AllowedRoles(roles = {AllowedRoles.DATA_OWNER})
  public Response downloadFile(
          @PathParam("filePath") String filePath,
          @Context SecurityContext sc,
          @Context HttpServletRequest req) throws AppException {
    InputStream is = null;
    String fullPath = this.path + filePath;
    logger.
            log(Level.INFO, "File to be downloaded from HDFS path: {0}",
                    fullPath);
    if (inodes.getInodeAtPath(fullPath) == null) {
      throw new AppException(Response.Status.NOT_FOUND.getStatusCode(),
              ResponseMessages.FILE_NOT_FOUND);
    }
    try {
      is = fileOps.getInputStream(fullPath);
      logger.
              log(Level.FINE, "File was downloaded from HDFS path: {0}",
                      fullPath);
    } catch (IOException ex) {
      logger.log(Level.INFO, null, ex);
      throw new AppException(Response.Status.NOT_FOUND.getStatusCode(),
              "Could not open stream.");
    }
    String[] p = filePath.split(File.separator);
    ResponseBuilder response = noCacheResponse.getNoCacheResponseBuilder(
            Response.Status.OK);
    response.header("filename", p[p.length - 1]);
    return response.entity(is).build();
  }

  @POST
  @Produces(MediaType.APPLICATION_JSON)
  @AllowedRoles(roles = {AllowedRoles.DATA_OWNER})
  public Response createDataSetDir(
          DataSetDTO dataSetName,
          @Context SecurityContext sc,
          @Context HttpServletRequest req) throws AppException {
    boolean success = false;
    boolean exist = true;
    String dsPath = File.separator + Constants.DIR_ROOT + File.separator
            + this.project.getName();
    JsonResponse json = new JsonResponse();
    if (dataSetName == null || dataSetName.getName() == null || dataSetName.
            getName().isEmpty()) {
      throw new AppException(Response.Status.BAD_REQUEST.getStatusCode(),
              ResponseMessages.DATASET_NAME_EMPTY);
    }

    //check if the folder is allowed and if it already exists
    Inode parent = inodes.getProjectRoot(this.project.getName());
    String[] pathArray = dataSetName.getName().split(File.separator);
    for (String p : pathArray) {

      if (parent != null) {
        parent = inodes.findByParentAndName(parent, p);
        if (parent != null) {
          dsPath = dsPath + File.separator + p;
        } else {//first time when we find non existing folder name
          if (datasetNameValidator.isValidName(p)) {
            dsPath = dsPath + File.separator + p;
            exist = false;
          }
        }
      } else {
        if (datasetNameValidator.isValidName(p)) {
          dsPath = dsPath + File.separator + p;
          exist = false;
        }
      }
    }

    if (exist) {
      throw new AppException(Response.Status.BAD_REQUEST.getStatusCode(),
              ResponseMessages.FOLDER_NAME_EXIST);
    }

    try {
      success = fileOps.mkDir(dsPath);
    } catch (IOException ex) {
      logger.log(Level.SEVERE, null, ex);
      throw new AppException(Response.Status.BAD_REQUEST.getStatusCode(),
              "Could not create the directory at " + dsPath);
    }
    if (!success) {
      throw new AppException(Response.Status.BAD_REQUEST.getStatusCode(),
              "Could not create the directory at " + dsPath);
    }
    json.setSuccessMessage("A directory for the dataset was created at "
            + dsPath);
    return noCacheResponse.getNoCacheResponseBuilder(Response.Status.OK).entity(
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
    String filePath = this.path + fileName;
    try {
      success = fileOps.rmRecursive(filePath);
    } catch (IOException ex) {
      logger.log(Level.SEVERE, null, ex);
      throw new AppException(Response.Status.BAD_REQUEST.getStatusCode(),
              "Could not delete the file at " + filePath);
    }
    if (!success) {
      throw new AppException(Response.Status.BAD_REQUEST.getStatusCode(),
              "Could not delete the file at " + filePath);
    }
    json.setSuccessMessage(ResponseMessages.DATASET_REMOVED_FROM_HDFS);
    return noCacheResponse.getNoCacheResponseBuilder(Response.Status.OK).entity(
            json).build();

  }

  @Path("upload/{path: .+}")
  @AllowedRoles(roles = {AllowedRoles.DATA_OWNER})
  public UploadService upload(
          @PathParam("path") String path) throws AppException {
    String uploadPath;
    if (path == null) {
      path = "";
    }
    if (path.equals("") || path.endsWith(File.separator)) {
      uploadPath = this.path + path;
    } else {
      uploadPath = this.path + path + File.separator;
    }
    this.uploader.setPath(uploadPath);

    return this.uploader;
  }
}
