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
import se.kth.hopsworks.meta.db.InodeBasicMetadataFacade;
import se.kth.hopsworks.meta.db.TemplateFacade;
import se.kth.hopsworks.meta.entity.InodeBasicMetadata;
import se.kth.hopsworks.meta.entity.Template;
import se.kth.hopsworks.meta.exception.ApplicationException;
import se.kth.hopsworks.meta.exception.DatabaseException;
import se.kth.hopsworks.meta.wscomm.ResponseBuilder;
import se.kth.hopsworks.meta.wscomm.message.UploadedTemplateMessage;

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
  private TemplateFacade template;
  @EJB
  private ResponseBuilder responseBuilder;
  @EJB
  private InodeBasicMetadataFacade basicMetaFacade;

  private String path;
  private Inode fileParent;
  private int templateId;

  public UploadService() {
  }

  /**
   * Sets the upload path for the file to be uploaded.
   * <p/>
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
        FolderNameValidator.isValidName(pathArray[i]);
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
   * Sets the path for the file to be uploaded. It does not require a project
   * name since the file to be uploaded is a template schema, irrelevant to any
   * project or dataset. The only requirement is that the upload has to be
   * performed in the Uploads directory
   * <p/>
   * @param path
   * @throws se.kth.hopsworks.rest.AppException
   */
  public void setUploadPath(String path) throws AppException {
    String[] pathArray = path.substring(1).split(File.separator);
    if (pathArray.length < 2) { // if path does not contain project name.
      throw new AppException(Response.Status.BAD_REQUEST.getStatusCode(),
              "Not a valid path!");
    }

    //check if the parent directory exists. If it doesn't create it first
    if (!fileOps.isDir(File.separator + pathArray[0])) {
      try {
        fileOps.mkDir(File.separator + pathArray[0]);
      } catch (IOException e) {
        throw new AppException(Response.Status.INTERNAL_SERVER_ERROR.
                getStatusCode(),
                "Upload directory could not be created in the file system");
      }
    }

    this.path = path;
  }

  /**
   * Sets the template id to be attached to the file that's being uploaded.
   * <p/>
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
    if (info.addChunkAndCheckIfFinished(new ResumableInfo.ResumableChunkNumber(
            resumableChunkNumber), content_length)) { //Check if all chunks uploaded, and change filename
      ResumableInfoStorage.getInstance().remove(info);
      logger.log(Level.INFO, "All finished.");
      finished = true;
    } else {
      json.setSuccessMessage("Upload");//This Chunk has been Uploaded.
      return noCacheResponse.getNoCacheResponseBuilder(Response.Status.OK).
              entity(json).build();
    }

    if (finished) {
      try {
        String fileContent = null;

        //if it is about a template file check its validity
        if ((this.path + fileName).endsWith(".json")) {

          String filePath = stagingManager.getStagingPath() + this.path
                  + fileName;

          if (!Utils.checkJsonValidity(filePath)) {
            json.setErrorMsg("This was an invalid json file");
            return noCacheResponse.getNoCacheResponseBuilder(
                    Response.Status.NOT_ACCEPTABLE).entity(json).build();
          }
          fileContent = Utils.getFileContents(filePath);
        }

        this.path = Utils.ensurePathEndsInSlash(this.path);
        fileOps.copyToHDFSFromLocal(true, stagingManager.getStagingPath()
                + this.path + fileName, this.path
                + fileName);
        logger.log(Level.INFO, "Copied to HDFS");

        if (templateid != 0 && templateid != -1) {
          this.attachTemplateToInode(info, this.path + fileName);
        }

        //if it is about a template file persist it in the database as well
        if ((this.path + fileName).endsWith(".json")) {
          //TODO. More checks needed to ensure the valid template format
          this.persistUploadedTemplate(fileContent);
        } //this is a common file being uploaded so add basic metadata to it
        //description and searchable
        else if (!(this.path + fileName).endsWith(".json")) {
          //find the corresponding inode
          Inode parent = this.inodes.getInodeAtPath(this.path);
          Inode file = this.inodes.findByParentAndName(parent, fileName);
          
          InodeBasicMetadata basicMeta = new InodeBasicMetadata(file, "", true);
          
          this.basicMetaFacade.addBasicMetadata(basicMeta);
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

    Template templ = template.findByTemplateId(info.getResumableTemplateId());
    templ.getInodes().add(inode);

    try {
      //persist the relationship table
      template.updateTemplatesInodesMxN(templ);
    } catch (DatabaseException e) {
      logger.log(Level.SEVERE, "Something went wrong.", e);
    }
  }

  /**
   * Persist a template to the database after it has been uploaded to hopsfs
   * <p/>
   * @param filePath
   * @throws AppException
   */
  private void persistUploadedTemplate(String fileContent) throws AppException {
    try {
      //the file content has to be wrapped in a TemplateMessage message
      UploadedTemplateMessage message = new UploadedTemplateMessage();
      message.setMessage(fileContent);

      this.responseBuilder.persistUploadedTemplate(message);
    } catch (ApplicationException e) {
      throw new AppException(Response.Status.INTERNAL_SERVER_ERROR.
              getStatusCode(), e.getMessage());
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
    //this will create a folder if it does not exist inside the tmp folder 
    //specific to where the file is being uploaded
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
    //this will create a folder if it does not exist inside the tmp folder 
    //specific to where the file is being uploaded
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
