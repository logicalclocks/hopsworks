
/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package se.kth.hopsworks.rest;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
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
import se.kth.bbc.fileoperations.FileOperations;
import se.kth.bbc.lims.Constants;
import se.kth.bbc.project.Project;
import se.kth.bbc.project.ProjectFacade;
import se.kth.hopsworks.controller.ResponseMessages;
import se.kth.hopsworks.filters.AllowedRoles;
import se.kth.bbc.project.fb.Inode;
import se.kth.bbc.project.fb.InodeFacade;
import se.kth.bbc.project.fb.InodeView;
import se.kth.hopsworks.controller.DataSetDTO;
import se.kth.meta.entity.Templates;

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
    this.path = projectPath + File.separator
            + Constants.DIR_DATASET + File.separator;
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

    Inode projectInode = inodes.findByName(this.project.getName());
    Inode parent = inodes.findByParentAndName(projectInode,
            Constants.DIR_DATASET);
    logger.log(Level.FINE, "findDataSetsInProjectID Parent name: {0}", parent.
            getName());
    
    List<Inode> cwdChildren;
    cwdChildren = inodes.findByParent(parent);
    List<InodeView> kids = new ArrayList<>();
    
    for (Inode i : cwdChildren) {
      kids.add(new InodeView(i, inodes.getPath(i)));
      logger.log(Level.FINE, "path: {0}", inodes.getPath(i));
    }

    logger.log(Level.FINE, "Num of children: {0}", cwdChildren.size());
    GenericEntity<List<InodeView>> inodViews
            = new GenericEntity<List<InodeView>>(kids) {
            };

    return noCacheResponse.getNoCacheResponseBuilder(Response.Status.OK).entity(
            inodViews).build();
  }

  @GET
  @Path("/{path: .+}")
  @Produces(MediaType.APPLICATION_JSON)
  @AllowedRoles(roles = {AllowedRoles.DATA_SCIENTIST, AllowedRoles.DATA_OWNER})
  public Response getDirContent(
          @PathParam("path") String path,
          @Context SecurityContext sc,
          @Context HttpServletRequest req) throws AppException {

    Inode projectInode = inodes.findByName(this.project.getName());
    Inode parent = inodes.findByParentAndName(projectInode,Constants.DIR_DATASET);
    String[] pathArray = path.split(File.separator);
    
    for (String p : pathArray) {
      parent = inodes.findByParentAndName(parent, p);
    }
    
    List<Inode> cwdChildren;
    cwdChildren = inodes.findByParent(parent);
    List<InodeView> kids = new ArrayList<>();
    
    for (Inode i : cwdChildren) {
      kids.add(new InodeView(i, inodes.getPath(i)));
      logger.log(Level.FINE, "path: {0}", inodes.getPath(i));
    }

    logger.log(Level.FINE, "Num of children: {0}", cwdChildren.size());
    GenericEntity<List<InodeView>> inodViews
            = new GenericEntity<List<InodeView>>(kids) {
            };

    return noCacheResponse.getNoCacheResponseBuilder(Response.Status.OK).entity(
            inodViews).build();
  }

  @GET
  @Path("download/{fileName}")
  @Produces(MediaType.WILDCARD)
  @AllowedRoles(roles = {AllowedRoles.DATA_OWNER})
  public InputStream downloadFile(
          @PathParam("fileName") String fileName,
          @Context SecurityContext sc,
          @Context HttpServletRequest req) throws AppException {
      
    InputStream is = null;
    String filePath = this.path + fileName;
    
    try {
      is = fileOps.getInputStream(filePath);
      logger.log(Level.FINE, "File was downloaded from HDFS path: {0}",
              filePath);
    } catch (IOException ex) {
      logger.log(Level.SEVERE, null, ex);
    }
    
    return is;
  }
/*
  @POST
  @Path("upload/{dataSetPath}")
  @Consumes(MediaType.MULTIPART_FORM_DATA)
  @Produces(MediaType.APPLICATION_JSON)
  @AllowedRoles(roles = {AllowedRoles.DATA_OWNER})
  public Response uploadFile(
          @PathParam("dataSetPath") String dataSetPath,
          @FormDataParam("file") InputStream uploadedInputStream,
          @FormDataParam("file") FormDataContentDisposition fileDetail) throws
          AppException {
    JsonResponse json = new JsonResponse();
    String dsPath = this.path + dataSetPath;
    try {
      fileOps.writeToHDFS(uploadedInputStream, fileDetail.getSize(), dsPath);
    } catch (IOException ex) {
      logger.log(Level.SEVERE, null, ex);
      throw new AppException(Response.Status.BAD_REQUEST.getStatusCode(),
              "Could not upload file to " + dsPath);
    }
    json.setSuccessMessage("Successfuly uploaded file to " + dsPath);
    return noCacheResponse.getNoCacheResponseBuilder(Response.Status.OK).entity(
            json).build();
  }
*/
  @POST
  @Produces(MediaType.APPLICATION_JSON)
  @AllowedRoles(roles = {AllowedRoles.DATA_OWNER})
  public Response createDataSetDir(
          DataSetDTO dataSetName,
          @Context SecurityContext sc,
          @Context HttpServletRequest req) throws AppException {
    boolean success = false;
    JsonResponse json = new JsonResponse();
    if (dataSetName == null || dataSetName.getName().isEmpty()) {
      throw new AppException(Response.Status.BAD_REQUEST.getStatusCode(),
              ResponseMessages.DATASET_NAME_EMPTY);
    }
    
    String dsPath = this.path + dataSetName.getName();
    try {
      success = fileOps.mkDir(dsPath, dataSetName.getTemplate());
      logger.log(Level.SEVERE, "DATASET RECEIVED {0} ", dataSetName.getTemplate());
      
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

}
