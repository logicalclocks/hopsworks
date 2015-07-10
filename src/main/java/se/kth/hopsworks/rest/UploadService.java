package se.kth.hopsworks.rest;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.ejb.EJB;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import org.glassfish.jersey.media.multipart.FormDataContentDisposition;
import org.glassfish.jersey.media.multipart.FormDataParam;
import se.kth.bbc.fileoperations.FileOperations;
import se.kth.bbc.lims.StagingManager;
import se.kth.bbc.lims.Utils;
import se.kth.bbc.project.fb.Inode;
import se.kth.bbc.project.fb.InodeFacade;
import se.kth.bbc.upload.HttpUtils;
import se.kth.bbc.upload.ResumableInfo;
import se.kth.bbc.upload.ResumableInfoStorage;
import se.kth.hopsworks.controller.FolderNameValidator;
import se.kth.hopsworks.controller.ResponseMessages;
import se.kth.hopsworks.filters.AllowedRoles;
import se.kth.meta.db.Dbao;
import se.kth.meta.entity.Template;
import se.kth.meta.exception.DatabaseException;

/**
 *
 * @author ermiasg
 */
@RequestScoped
@TransactionAttribute(TransactionAttributeType.NEVER)
public class UploadService {

  private final static Logger logger = Logger.getLogger(UploadService.class.
          getName());

  @EJB
  private NoCacheResponse noCacheResponse;
  @EJB
  private FileOperations fileOps;
  @EJB
  private InodeFacade inodes;
  @EJB
  private StagingManager stagingManager;
  @EJB
  private FolderNameValidator datasetNameValidator;
  @Inject
  private Dbao db;

  private String path;
  private Inode fileParent;
  private int templateId;

  public UploadService() {
  }

  /**
   * Sets the upload path for the file to be uploaded.
   * <p>
   * @param uploadPath starting with Projects/projectName/...
   * @throws AppException if there is a folder name that is not valid in
   * the given path, the path is empty, or project name was not found.
   */
  public void setPath(String uploadPath) throws AppException {
    boolean exist = true;
    String[] pathArray = uploadPath.split(File.separator);
    if (pathArray.length < 3) { // if path does not contain project name.
      throw new AppException(Response.Status.BAD_REQUEST.getStatusCode(),
              "Not a valid path!");
    }
    //check if all the non existing dir names in the path are valid.
    Inode parent = inodes.getProjectRoot(pathArray[2]);
    if (parent == null) {
      throw new AppException(Response.Status.BAD_REQUEST.getStatusCode(),
              ResponseMessages.PROJECT_NOT_FOUND);
    }

    for (int i = 3; i < pathArray.length; i++) {
      if (parent != null) {
        parent = inodes.findByParentAndName(parent, pathArray[i]);
      } else {
        datasetNameValidator.isValidName(pathArray[i]);
        exist = false;
      }
    }

    if (exist) { //if the path exists check if the file exists.
      this.fileParent = parent;
    }
    logger.log(Level.INFO, "Constructor end. by setting fileParent to {0}",
            this.fileParent);
    this.path = uploadPath;
  }

  /**
   * Sets the template id to be attached to the file that's being uploaded.
   * <p>
   * <p>
   * @param templateId
   */
  public void setTemplateId(int templateId) {
    this.templateId = templateId;
  }

  @GET
  @Consumes(MediaType.MULTIPART_FORM_DATA)
  @Produces(MediaType.APPLICATION_JSON)
  @AllowedRoles(roles = {AllowedRoles.DATA_SCIENTIST, AllowedRoles.DATA_OWNER})
  public Response testMethod(
          @Context HttpServletRequest request)
          throws AppException {
    String fileName = request.getParameter("flowFilename");
    logger.log(Level.INFO, "File parent. {0}", this.fileParent);
    Inode parent;
    if (this.fileParent != null) {
      parent = inodes.findByParentAndName(this.fileParent, fileName);
      if (parent != null) {
        throw new AppException(Response.Status.BAD_REQUEST.getStatusCode(),
                ResponseMessages.FILE_NAME_EXIST);
      }
    }

    JsonResponse json = new JsonResponse();
    int resumableChunkNumber = getResumableChunkNumber(request);
    ResumableInfo info = getResumableInfo(request, this.path, this.templateId);

    if (info.isUploaded(new ResumableInfo.ResumableChunkNumber(
            resumableChunkNumber))) {
      json.setSuccessMessage("Uploaded");//This Chunk has been Uploaded.
      return noCacheResponse.getNoCacheResponseBuilder(Response.Status.OK).
              entity(json).build();
    } else {
      json.setErrorMsg("Not uploaded");
      return noCacheResponse.getNoCacheResponseBuilder(
              Response.Status.NO_CONTENT).entity(json).build();
    }

  }

  @POST
  @Consumes(MediaType.MULTIPART_FORM_DATA)
  @Produces(MediaType.APPLICATION_JSON)
  @AllowedRoles(roles = {AllowedRoles.DATA_SCIENTIST, AllowedRoles.DATA_OWNER})
  public Response uploadMethod(
          @FormDataParam("file") InputStream uploadedInputStream,
          @FormDataParam("file") FormDataContentDisposition fileDetail,
          @FormDataParam("flowChunkNumber") String flowChunkNumber,
          @FormDataParam("flowChunkSize") String flowChunkSize,
          @FormDataParam("flowCurrentChunkSize") String flowCurrentChunkSize,
          @FormDataParam("flowFilename") String flowFilename,
          @FormDataParam("flowIdentifier") String flowIdentifier,
          @FormDataParam("flowRelativePath") String flowRelativePath,
          @FormDataParam("flowTotalChunks") String flowTotalChunks,
          @FormDataParam("flowTotalSize") String flowTotalSize)
          throws AppException, IOException {

    JsonResponse json = new JsonResponse();

    int resumableChunkNumber = HttpUtils.toInt(flowChunkNumber, -1);
    ResumableInfo info = getResumableInfo(flowChunkSize, flowFilename,
            flowIdentifier, flowRelativePath, flowTotalSize, this.path,
            this.templateId);
    String fileName = info.getResumableFilename();
    int templateid = info.getResumableTemplateId();

    long content_length;
    //Seek to position
    try (RandomAccessFile raf
            = new RandomAccessFile(info.getResumableFilePath(), "rw");
            InputStream is = uploadedInputStream) {
      //Seek to position
      raf.seek((resumableChunkNumber - 1) * (long) info.getResumableChunkSize());
      //Save to file
      long readed = 0;
      content_length = HttpUtils.toLong(flowCurrentChunkSize, -1);
      byte[] bytes = new byte[1024 * 1024];//Default chunk size for ng-flow.js is set to chunkSize: 1024 * 1024
      while (readed < content_length) {
        int r = is.read(bytes);
        if (r < 0) {
          break;
        }
        raf.write(bytes, 0, r);
        readed += r;
      }
    }

    boolean finished = false;

    //Mark as uploaded and check if finished
    if (info.addChuckAndCheckIfFinished(new ResumableInfo.ResumableChunkNumber(
            resumableChunkNumber), content_length)) { //Check if all chunks uploaded, and change filename
      ResumableInfoStorage.getInstance().remove(info);
      logger.log(Level.INFO, "All finished.");
      finished = true;
    } else {
      logger.log(Level.INFO, "Upload");
      json.setSuccessMessage("Upload");//This Chunk has been Uploaded.
      return noCacheResponse.getNoCacheResponseBuilder(Response.Status.OK).
              entity(json).build();
    }

    if (finished) {
      try {
        this.path = Utils.ensurePathEndsInSlash(this.path);
        fileOps.copyToHDFSFromLocal(true, stagingManager.getStagingPath()
                + this.path + fileName, this.path
                + fileName);
        logger.log(Level.INFO, "Copied to HDFS");

        if (templateid != 0) {
          this.attachTemplateToInode(info, this.path + fileName);
        }

        json.setSuccessMessage("Successfuly uploaded file to " + this.path);
        return noCacheResponse.getNoCacheResponseBuilder(Response.Status.OK).
                entity(json).build();
      } catch (IOException e) {
        logger.log(Level.INFO, "Failed to write to HDFS", e);
        json.setErrorMsg("Failed to write to HDFS");
        return noCacheResponse.getNoCacheResponseBuilder(
                Response.Status.BAD_REQUEST).entity(json).build();
      }
    }

    json.setSuccessMessage("Uploading...");
    return noCacheResponse.getNoCacheResponseBuilder(Response.Status.OK).entity(
            json).build();
  }

  private void attachTemplateToInode(ResumableInfo info, String path) {
    //find the inode
    Inode inode = inodes.getInodeAtPath(path);

    Template template = db.findTemplateById(info.getResumableTemplateId());
    template.getInodes().add(inode);

    try {
      //persist the relationship table
      db.updateTemplatesInodesMxN(template);
    } catch (DatabaseException e) {
      logger.log(Level.SEVERE, "Something went wrong.", e);
    }
  }

  private int getResumableChunkNumber(HttpServletRequest request) {
    return HttpUtils.toInt(request.getParameter("flowChunkNumber"), -1);
  }

  private ResumableInfo getResumableInfo(HttpServletRequest request,
          String hdfsPath, int templateId) throws
          AppException {
    //this will give us a tmp folder
    String base_dir = stagingManager.getStagingPath();
    //this will create a folder if it doesnot exist inside the tmp folder 
    //spesific to where the file is being uploaded
    File userTmpDir = new File(base_dir + hdfsPath);
    if (!userTmpDir.exists()) {
      userTmpDir.mkdirs();
    }
    base_dir = userTmpDir.getAbsolutePath();

    int resumableChunkSize = HttpUtils.toInt(request.getParameter(
            "flowChunkSize"), -1);
    long resumableTotalSize = HttpUtils.toLong(request.getParameter(
            "flowTotalSize"), -1);
    String resumableIdentifier = request.getParameter("flowIdentifier");
    String resumableFilename = request.getParameter("flowFilename");
    String resumableRelativePath = request.getParameter("flowRelativePath");

    File file = new File(base_dir, resumableFilename);
    //check if the file alrady exists in the staging dir.
    if (file.exists() && file.canRead()) {
      //file.exists returns true after deleting a file 
      //so make sure the file exists by trying to read from it.
      try (FileReader fileReader = new FileReader(file.getAbsolutePath())) {
        fileReader.read();
        throw new AppException(Response.Status.BAD_REQUEST.getStatusCode(),
                "A file with the same name is being uploaded.");
      } catch (Exception e) {
      }
    }

    //Here we add a ".temp" to every upload file to indicate NON-FINISHED
    String resumableFilePath = file.getAbsolutePath() + ".temp";

    ResumableInfoStorage storage = ResumableInfoStorage.getInstance();

    ResumableInfo info = storage.get(resumableChunkSize, resumableTotalSize,
            resumableIdentifier, resumableFilename, resumableRelativePath,
            resumableFilePath, templateId);
    if (!info.valid()) {
      storage.remove(info);
      throw new AppException(Response.Status.BAD_REQUEST.getStatusCode(),
              "Invalid request params.");
    }
    return info;
  }

  private ResumableInfo getResumableInfo(String flowChunkSize,
          String flowFilename,
          String flowIdentifier,
          String flowRelativePath,
          String flowTotalSize,
          String hdfsPath,
          int templateId) throws AppException {
    //this will give us a tmp folder
    String base_dir = stagingManager.getStagingPath();
    //this will create a folder if it doesnot exist inside the tmp folder 
    //spesific to where the file is being uploaded
    File userTmpDir = new File(base_dir + hdfsPath);
    if (!userTmpDir.exists()) {
      userTmpDir.mkdirs();
    }
    base_dir = userTmpDir.getAbsolutePath();

    int resumableChunkSize = HttpUtils.toInt(flowChunkSize, -1);
    long resumableTotalSize = HttpUtils.toLong(flowTotalSize, -1);
    String resumableIdentifier = flowIdentifier;
    String resumableFilename = flowFilename;
    String resumableRelativePath = flowRelativePath;

    File file = new File(base_dir, resumableFilename);
    //check if the file alrady exists in the staging dir.
    if (file.exists() && file.canRead()) {
      //file.exists returns true after deleting a file 
      //so make sure the file exists by trying to read from it.
      try (FileReader fileReader = new FileReader(file.getAbsolutePath())) {
        fileReader.read();
        throw new AppException(Response.Status.BAD_REQUEST.getStatusCode(),
                "A file with the same name is being uploaded.");
      } catch (Exception e) {
      }
    }

    //Here we add a ".temp" to every upload file to indicate NON-FINISHED
    String resumableFilePath = file.getAbsolutePath() + ".temp";

    ResumableInfoStorage storage = ResumableInfoStorage.getInstance();

    ResumableInfo info = storage.get(resumableChunkSize, resumableTotalSize,
            resumableIdentifier, resumableFilename, resumableRelativePath,
            resumableFilePath, templateId);
    if (!info.valid()) {
      storage.remove(info);
      throw new AppException(Response.Status.BAD_REQUEST.getStatusCode(),
              "Invalid request params.");
    }
    return info;
  }

}
