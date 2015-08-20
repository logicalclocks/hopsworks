package se.kth.hopsworks.rest;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
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
import se.kth.hopsworks.controller.DatasetController;
import se.kth.hopsworks.controller.FileTemplateDTO;
import se.kth.meta.entity.Template;
import se.kth.meta.exception.DatabaseException;
import se.kth.hopsworks.controller.ResponseMessages;
import se.kth.hopsworks.dataset.Dataset;
import se.kth.hopsworks.dataset.DatasetFacade;
import se.kth.hopsworks.dataset.DatasetRequest;
import se.kth.hopsworks.dataset.DatasetRequestFacade;
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
  private DatasetRequestFacade datasetRequest;
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
  @Inject
  private UploadService uploader;
  @EJB
  private TemplateFacade template;
  @EJB
  private DatasetController datasetController;

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

    Dataset dst = datasetFacade.findByProjectAndInode(proj, inode);
    if (dst != null) {//proj already have the dataset.
      throw new AppException(Response.Status.BAD_REQUEST.getStatusCode(),
              "Dataset already in " + proj.getName());
    }

    DatasetRequest dsReq = datasetRequest.findByProjectAndDataset(proj, ds);

    Dataset newDS = new Dataset(inode, proj);
    if (dataSet.getDescription() != null) {
      newDS.setDescription(dataSet.getDescription());
    }
    if (!dataSet.isEditable()) {
      newDS.setEditable(false);
    }
    // if the dataset is not requested or is requested by a data scientist
    // set status to pending. 
    if (dsReq == null || dsReq.getProjectTeam().getTeamRole().equals(
            AllowedRoles.DATA_SCIENTIST)) {
      newDS.setStatus(Dataset.PENDING);
    }
    datasetFacade.persistDataset(newDS);
    if (dsReq != null) {
      datasetRequest.remove(dsReq);//the dataset is shared so remove the request.
    }

    activityFacade.persistActivity(ActivityFacade.SHARED_DATA + dataSet.
            getName() + " with project " + proj.getName(), project, user);

    json.setSuccessMessage("The Dataset was successfully shared.");
    return noCacheResponse.getNoCacheResponseBuilder(Response.Status.OK).entity(
            json).build();
  }

  @GET
  @Path("/accept/{inodeId}")
  @Produces(MediaType.APPLICATION_JSON)
  @AllowedRoles(roles = {AllowedRoles.DATA_OWNER})
  public Response acceptRequest(@PathParam("inodeId") Integer inodeId,
          @Context SecurityContext sc,
          @Context HttpServletRequest req) throws AppException {
    JsonResponse json = new JsonResponse();
    if (inodeId == null) {
      throw new AppException(Response.Status.BAD_REQUEST.getStatusCode(),
              "Incomplete request!");
    }
    Inode inode = inodes.findById(inodeId);
    if (inode == null) {
      throw new AppException(Response.Status.BAD_REQUEST.getStatusCode(),
              ResponseMessages.DATASET_NOT_FOUND);
    }

    Dataset ds = datasetFacade.findByProjectAndInode(this.project, inode);

    if (ds == null) {
      throw new AppException(Response.Status.BAD_REQUEST.getStatusCode(),
              ResponseMessages.DATASET_NOT_FOUND);
    }

    ds.setStatus(Dataset.ACCEPTED);
    datasetFacade.merge(ds);
    json.setSuccessMessage("The Dataset is now accessable.");
    return noCacheResponse.getNoCacheResponseBuilder(Response.Status.OK).entity(
            json).build();
  }

  @GET
  @Path("/reject/{inodeId}")
  @Produces(MediaType.APPLICATION_JSON)
  @AllowedRoles(roles = {AllowedRoles.DATA_OWNER})
  public Response rejectRequest(@PathParam("inodeId") Integer inodeId,
          @Context SecurityContext sc,
          @Context HttpServletRequest req) throws AppException {
    JsonResponse json = new JsonResponse();
    if (inodeId == null) {
      throw new AppException(Response.Status.BAD_REQUEST.getStatusCode(),
              "Incomplete request!");
    }
    Inode inode = inodes.findById(inodeId);
    if (inode == null) {
      throw new AppException(Response.Status.BAD_REQUEST.getStatusCode(),
              ResponseMessages.DATASET_NOT_FOUND);
    }

    Dataset ds = datasetFacade.findByProjectAndInode(this.project, inode);

    if (ds == null) {
      throw new AppException(Response.Status.BAD_REQUEST.getStatusCode(),
              ResponseMessages.DATASET_NOT_FOUND);
    }

    datasetFacade.remove(ds);
    json.setSuccessMessage("The Dataset has been removed.");
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
    try {
      datasetController.createDataset(user, project, dataSet.getName(), dataSet.
              getDescription(), dataSet.getTemplate());
    } catch (NullPointerException c) {
      throw new AppException(Response.Status.BAD_REQUEST.getStatusCode(), c.
              getLocalizedMessage());
    } catch (IllegalArgumentException e) {
      throw new AppException(Response.Status.BAD_REQUEST.getStatusCode(),
              "Failed to create dataset: " + e.getLocalizedMessage());
    } catch (IOException e) {
      throw new AppException(Response.Status.INTERNAL_SERVER_ERROR.
              getStatusCode(), "Failed to create dataset: " + e.
              getLocalizedMessage());
    }

    JsonResponse json = new JsonResponse();
    json.setSuccessMessage("The Dataset was created successfully.");
    return noCacheResponse.getNoCacheResponseBuilder(Response.Status.OK).entity(
            json).build();
  }

  //TODO: put this in DatasetController.
  @POST
  @Produces(MediaType.APPLICATION_JSON)
  @AllowedRoles(roles = {AllowedRoles.DATA_OWNER})
  public Response createDataSetDir(
          DataSetDTO dataSetName,
          @Context SecurityContext sc,
          @Context HttpServletRequest req) throws AppException {
    JsonResponse json = new JsonResponse();
    String newPath = getFullPath(dataSetName.getName());
    while(newPath.startsWith("/")){
      newPath = newPath.substring(1);
    }
    String[] fullPathArray = newPath.split(File.separator);
    String[] datasetRelativePathArray = Arrays.copyOfRange(fullPathArray, 4,
            fullPathArray.length);
    String dsPath = File.separator + Constants.DIR_ROOT + File.separator
            + fullPathArray[2];
    //Check if the DataSet is writeable.
    if (!fullPathArray[2].equals(this.project.getName())) {
      if (!this.dataset.isEditable()) {
        throw new AppException(Response.Status.BAD_REQUEST.getStatusCode(),
                "You can not create a folder inside a shared dataset.");
      }
    }
    StringBuilder dsRelativePath = new StringBuilder();
    for (String s : datasetRelativePathArray) {
      dsRelativePath.append(s).append("/");
    }
    try {
      datasetController.createSubDirectory(project, fullPathArray[2],
              dsRelativePath.toString(), dataSetName.getTemplate());
    } catch (IOException e) {
      throw new AppException(Response.Status.INTERNAL_SERVER_ERROR.
              getStatusCode(), "Error while creating directory: " + e.
              getLocalizedMessage());
    } catch (IllegalArgumentException | NullPointerException e) {
      throw new AppException(Response.Status.BAD_REQUEST.getStatusCode(),
              "Invalid directory: " + e.getLocalizedMessage());
    }
    json.setSuccessMessage("A directory was created at " + dsPath);
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

    if (templateId != 0 && templateId != -1) {
      this.uploader.setTemplateId(templateId);
    }

    this.uploader.setPath(path);

    return this.uploader;
  }

  @POST
  @Path("/attachTemplate")
  @Produces(MediaType.APPLICATION_JSON)
  @AllowedRoles(roles = {AllowedRoles.DATA_OWNER})
  public Response attachTemplate(FileTemplateDTO filetemplateData) throws
          AppException {

    if (filetemplateData == null || filetemplateData.getInodePath() == null
            || filetemplateData.getInodePath().equals("")) {
      throw new AppException(Response.Status.BAD_REQUEST.getStatusCode(),
              ResponseMessages.TEMPLATE_INODEID_EMPTY);
    }

    String inodePath = filetemplateData.getInodePath();
    int templateid = filetemplateData.getTemplateId();

    Inode inode = inodes.getInodeAtPath(inodePath);
    Template temp = template.findByTemplateId(templateid);
    temp.getInodes().add(inode);

    logger.log(Level.INFO, "ATTACHING TEMPLATE {0} TO INODE {0}",
            new Object[]{templateid, inode.getId()});

    try {
      //persist the relationship
      this.template.updateTemplatesInodesMxN(temp);
    } catch (DatabaseException e) {
      throw new AppException(Response.Status.INTERNAL_SERVER_ERROR.
              getStatusCode(),
              ResponseMessages.TEMPLATE_NOT_ATTACHED);
    }

    JsonResponse json = new JsonResponse();
    json.setSuccessMessage("The template was attached to file "
            + inode.getId());
    return noCacheResponse.getNoCacheResponseBuilder(Response.Status.OK).entity(
            json).build();
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
      if (this.dataset.getStatus() == Dataset.PENDING) {
        throw new AppException(Response.Status.BAD_REQUEST.getStatusCode(),
                "Dataset is not yet accessible. Accept the share requst to access it.");
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
