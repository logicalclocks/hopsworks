package se.kth.hopsworks.rest;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
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
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.GenericEntity;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.ResponseBuilder;
import javax.ws.rs.core.SecurityContext;
import se.kth.bbc.activity.Activity;
import se.kth.bbc.activity.ActivityFacade;
import se.kth.bbc.fileoperations.FileOperations;
import se.kth.bbc.lims.Constants;
import se.kth.bbc.project.Project;
import se.kth.bbc.project.ProjectFacade;
import se.kth.bbc.project.fb.Inode;
import se.kth.bbc.project.fb.InodeFacade;
import se.kth.bbc.project.fb.InodeView;
import se.kth.bbc.security.ua.UserManager;
import se.kth.bbc.security.ua.model.User;
import se.kth.hopsworks.controller.DataSetDTO;
import se.kth.hopsworks.controller.FolderNameValidator;
import se.kth.meta.entity.Template;
import se.kth.meta.exception.DatabaseException;
import se.kth.hopsworks.controller.ResponseMessages;
import se.kth.hopsworks.dataset.Dataset;
import se.kth.hopsworks.dataset.DatasetFacade;
import se.kth.hopsworks.filters.AllowedRoles;
import se.kth.meta.db.TemplateFacade;

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
  private DatasetFacade datasetFacade;
  @EJB
  private ActivityFacade activityFacade;
  @EJB
  private UserManager userBean;
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
  @EJB
  private TemplateFacade template;

  private Integer projectId;
  private Project project;
  private String path;
  private Dataset dataset;

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

    Inode parent;

    List<InodeView> kids = new ArrayList<>();

    Collection<Dataset> dsInProject = this.project.getDatasetCollection();
    for (Dataset ds : dsInProject) {
      parent = inodes.findParent(ds.getInode());
      kids.add(new InodeView(parent, ds, inodes.getPath(ds.getInode())));
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
    String fullpath = getFullPath(path);
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
    String fullPath = getFullPath(path);
    logger.log(Level.INFO, "File to be downloaded from HDFS path: {0}",
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
  @Path("/shareDataSet")
  @Produces(MediaType.APPLICATION_JSON)
  @AllowedRoles(roles = {AllowedRoles.DATA_OWNER})
  public Response shareDataSet(
          DataSetDTO dataSet,
          @Context SecurityContext sc,
          @Context HttpServletRequest req) throws AppException {

    User user = userBean.getUserByEmail(sc.getUserPrincipal().getName());
    JsonResponse json = new JsonResponse();
    Inode parent = inodes.getProjectRoot(this.project.getName());
    if (dataSet == null || dataSet.getName() == null || dataSet.getName().
            isEmpty()) {
      throw new AppException(Response.Status.BAD_REQUEST.getStatusCode(),
              ResponseMessages.DATASET_NAME_EMPTY);
    }
    if (dataSet.getProjectId() == null) {
      throw new AppException(Response.Status.BAD_REQUEST.getStatusCode(),
              "No project selected.");
    }
    Project proj = projectFacade.find(dataSet.getProjectId());
    if (proj == null) {
      throw new AppException(Response.Status.BAD_REQUEST.getStatusCode(),
              ResponseMessages.PROJECT_NOT_FOUND);
    }
    Inode inode = inodes.findByParentAndName(parent, dataSet.getName());
    Dataset ds = datasetFacade.findByProjectAndInode(this.project, inode);
    if (ds == null) {//if parent id and project are not the same it is a shared ds.
      throw new AppException(Response.Status.BAD_REQUEST.getStatusCode(),
              "You can not share this dataset you are not the owner.");
    }

    ds = datasetFacade.findByProjectAndInode(proj, inode);
    if (ds != null) {//proj already have the dataset.
      throw new AppException(Response.Status.BAD_REQUEST.getStatusCode(),
              "Dataset already shared with this project");
    }

    Dataset newDS = new Dataset(inode, proj);
    if (dataSet.getDescription() != null) {
      newDS.setDescription(dataSet.getDescription());
    }
    if (dataSet.isEditable()) {
      newDS.setEditable(true);
    }
    datasetFacade.persistDataset(newDS);
    logActivity(ActivityFacade.SHARED_DATA + dataSet.getName()
            + " with project " + proj.getName(),
            ActivityFacade.FLAG_DATASET, user, this.project);

    json.setSuccessMessage("The Dataset was successfully shared.");
    return noCacheResponse.getNoCacheResponseBuilder(Response.Status.OK).entity(
            json).build();
  }

  @POST
  @Path("/createTopLevelDataSet")
  @Produces(MediaType.APPLICATION_JSON)
  @AllowedRoles(roles = {AllowedRoles.DATA_OWNER})
  public Response createTopLevelDataSet(
          DataSetDTO dataSet,
          @Context SecurityContext sc,
          @Context HttpServletRequest req) throws AppException {
    User user = userBean.getUserByEmail(sc.getUserPrincipal().getName());
    boolean success;
    JsonResponse json = new JsonResponse();
    String dsPath = File.separator + Constants.DIR_ROOT + File.separator
            + this.project.getName();
    if (dataSet == null) {
      throw new AppException(Response.Status.BAD_REQUEST.getStatusCode(),
              ResponseMessages.DATASET_NAME_EMPTY);
    }

    if (datasetNameValidator.isValidName(dataSet.getName())) {
      dsPath = dsPath + File.separator + dataSet.getName();
    }
    Inode parent = inodes.getProjectRoot(this.project.getName());
    Inode ds = inodes.findByParentAndName(parent, dataSet.getName());

    if (ds != null) {
      throw new AppException(Response.Status.BAD_REQUEST.getStatusCode(),
              ResponseMessages.FOLDER_NAME_EXIST);
    }
    success = createDataset(dsPath, parent, dataSet.getName(), dataSet.
            getTemplate());

    if (success) {
      try {
        ds = inodes.findByParentAndName(parent, dataSet.getName());
        Dataset newDS = new Dataset(ds, this.project);
        if (dataSet.getDescription() != null) {
          newDS.setDescription(dataSet.getDescription());
        }
        datasetFacade.persistDataset(newDS);
        logActivity(ActivityFacade.NEW_DATA,
                ActivityFacade.FLAG_DATASET, user, this.project);
      } catch (Exception e) {
        try {
          success = fileOps.rmRecursive(dsPath);//if dataset persist fails rm ds folder.
        } catch (IOException ex) {
          logger.log(Level.SEVERE, null, e);
        }
        throw new AppException(Response.Status.BAD_REQUEST.getStatusCode(),
                "Could not create dataset in db." + success);
      }
    } else {
      throw new AppException(Response.Status.BAD_REQUEST.getStatusCode(),
              "Could not create the directory at " + dsPath);
    }
    json.setSuccessMessage("The Dataset was created successfully.");
    return noCacheResponse.getNoCacheResponseBuilder(Response.Status.OK).entity(
            json).build();
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
    
    JsonResponse json = new JsonResponse();

    if (dataSetName == null || dataSetName.getName() == null || dataSetName.
            getName().isEmpty()) {
      throw new AppException(Response.Status.BAD_REQUEST.getStatusCode(),
              ResponseMessages.DATASET_NAME_EMPTY);
    }
    String newPath = getFullPath(dataSetName.getName());
    String[] fullPathArray = newPath.split(File.separator);
    String[] pathArray = Arrays.copyOfRange(fullPathArray, 3,
            fullPathArray.length);
    String dsPath = File.separator + Constants.DIR_ROOT + File.separator
            + fullPathArray[2];
    //check if the folder name is allowed and if it already exists
    Inode parent = inodes.getProjectRoot(fullPathArray[2]);
    Inode lastVisitedParent = new Inode(parent);

    if (!fullPathArray[2].equals(this.project.getName())) {
      if (!this.dataset.isEditable()) {
        throw new AppException(Response.Status.BAD_REQUEST.getStatusCode(),
                "You can not create a folder inside a shared dataset.");
      }
    }
    if (inodes.findByParentAndName(parent, pathArray[0]) == null) {
      // this is to make sure that this method is not used to create top level ds
      // if used it will create inconsistency by creating a ds with no entry in the 
      // dataset table.
      throw new AppException(Response.Status.BAD_REQUEST.getStatusCode(),
              "Top level dataset not found. Create the top level dataset first.");
    }
    for (String p : pathArray) {
      if (parent != null) {

        parent = inodes.findByParentAndName(parent, p);

        if (parent != null) {
          dsPath = dsPath + File.separator + p;

          /*
           * need to keep track of the last visited parent to
           * avoid NullPointerException below when retrieving the inode
           */
          lastVisitedParent = new Inode(parent);
        } else {
          //first time when we find non existing folder name
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

    success = createDataset(dsPath, lastVisitedParent,
            pathArray[pathArray.length - 1], dataSetName.getTemplate());

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
    String filePath = getFullPath(fileName);
    String[] pathArray = filePath.split(File.separator);
    //if the path does not contain this project name it is shared.
    if (!pathArray[2].equals(this.project.getName())) { // /Projects/project/ds

      if (pathArray.length > 4 && !this.dataset.isEditable()) {// a folder in the dataset
        throw new AppException(Response.Status.BAD_REQUEST.getStatusCode(),
                "You can not perform this action on a shard dataset.");
      }
      if (!this.dataset.isEditable()) {
        //remove the entry in the table that represents shared ds
        //but leave the dataset in hdfs b/c the user does not have the right to delete it.
        datasetFacade.remove(this.dataset);
        json.setSuccessMessage(ResponseMessages.SHARED_DATASET_REMOVED);
        return noCacheResponse.getNoCacheResponseBuilder(Response.Status.OK).
                entity(json).build();
      }
    }

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
          @PathParam("path") String path,
          @QueryParam("templateId") int templateId) throws AppException {
    if (path == null) {
      path = "";
    }
    path = getFullPath(path);
    String[] pathArray = path.split(File.separator);
    if (!pathArray[2].equals(this.project.getName())) {
      if (!this.dataset.isEditable()) {
        throw new AppException(Response.Status.BAD_REQUEST.getStatusCode(),
                "You can not upload to a shared dataset.");
      }
    }
    if (!path.endsWith(File.separator)) {
      path = path + File.separator;
    }

    if (templateId != 0) {
      this.uploader.setTemplateId(templateId);
    }

    this.uploader.setPath(path);

    return this.uploader;
  }

  private boolean createDataset(String dsPath, Inode parent, String dsName,
          int template) throws AppException {
    boolean success = false;
    try {
      success = fileOps.mkDir(dsPath);

      //the inode has been created in the file system
      if (success && template != 0) {

        //get the newly created inode and the template it comes with
        Inode neww = inodes.findByParentAndName(parent, dsName);

        Template templ = this.template.findByTemplateId(template);
        if (templ != null) {
          templ.getInodes().add(neww);
          //persist the relationship table
          this.template.updateTemplatesInodesMxN(templ);
        }
      }
    } catch (IOException ex) {
      logger.log(Level.SEVERE, null, ex);
      throw new AppException(Response.Status.BAD_REQUEST.getStatusCode(),
              "Could not create the directory at " + dsPath);
    } catch (DatabaseException e) {
      logger.log(Level.SEVERE, null, e);
      throw new AppException(Response.Status.INTERNAL_SERVER_ERROR.
              getStatusCode(),
              "Could not attach template to inode " + e.getMessage());
    }
    return success;
  }

  //this should be in its own class
  private void logActivity(String activityPerformed, String flag,
          User performedBy, Project performedOn) {
    Date now = new Date();
    Activity activity = new Activity();
    activity.setActivity(activityPerformed);
    activity.setFlag(flag);
    activity.setProject(performedOn);
    activity.setTimestamp(now);
    activity.setUser(performedBy);

    activityFacade.persistActivity(activity);
  }

  private String getFullPath(String path) throws AppException {
    //Strip leading slashes.
    while (path.startsWith("/")) {
      path = path.substring(1);
    }
    String dsName;
    String projectName;
    String[] parts = path.split(File.separator);
    if (parts != null && parts[0].contains(Constants.SHARED_FILE_SEPARATOR)) {
      //we can split the string and get the project name, but we have to 
      //make sure that the user have access to the dataset.
      String[] shardDS = parts[0].split(Constants.SHARED_FILE_SEPARATOR);
      if (shardDS.length < 2) {
        throw new AppException(Response.Status.BAD_REQUEST.getStatusCode(),
                ResponseMessages.DATASET_NOT_FOUND);
      }
      projectName = shardDS[0];
      dsName = shardDS[1];
      Inode parent = inodes.getProjectRoot(projectName);
      Inode dsInode = inodes.findByParentAndName(parent, dsName);
      this.dataset = datasetFacade.findByProjectAndInode(this.project, dsInode);
      if (this.dataset == null) {
        throw new AppException(Response.Status.BAD_REQUEST.getStatusCode(),
                ResponseMessages.DATASET_NOT_FOUND);
      }
      path = path.replaceFirst(projectName + Constants.SHARED_FILE_SEPARATOR
              + dsName, projectName
              + File.separator + dsName);
    } else {
      return this.path + path;
    }
    return File.separator + Constants.DIR_ROOT + File.separator
            + path;
  }
}
