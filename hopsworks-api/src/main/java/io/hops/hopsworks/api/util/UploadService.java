package io.hops.hopsworks.api.util;

import io.hops.hopsworks.api.filter.NoCacheResponse;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.io.StringReader;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.ejb.EJB;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.enterprise.context.RequestScoped;
import javax.json.Json;
import javax.json.JsonObject;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.apache.hadoop.fs.Path;
import org.apache.hadoop.security.AccessControlException;
import org.glassfish.jersey.media.multipart.FormDataContentDisposition;
import org.glassfish.jersey.media.multipart.FormDataParam;
import io.hops.hopsworks.api.filter.AllowedProjectRoles;
import io.hops.hopsworks.api.metadata.wscomm.ResponseBuilder;
import io.hops.hopsworks.api.metadata.wscomm.message.UploadedTemplateMessage;
import io.hops.hopsworks.api.project.util.DsPath;
import io.hops.hopsworks.common.dataset.DatasetController;
import io.hops.hopsworks.common.constants.message.ResponseMessages;
import io.hops.hopsworks.common.dao.hdfs.inode.Inode;
import io.hops.hopsworks.common.dao.hdfs.inode.InodeFacade;
import io.hops.hopsworks.common.dao.metadata.InodeBasicMetadata;
import io.hops.hopsworks.common.dao.metadata.Template;
import io.hops.hopsworks.common.dao.metadata.db.InodeBasicMetadataFacade;
import io.hops.hopsworks.common.dao.metadata.db.TemplateFacade;
import io.hops.hopsworks.common.dataset.FolderNameValidator;
import io.hops.hopsworks.common.exception.AppException;
import io.hops.hopsworks.common.hdfs.DistributedFileSystemOps;
import io.hops.hopsworks.common.hdfs.DistributedFsService;
import io.hops.hopsworks.common.hdfs.Utils;
import io.hops.hopsworks.common.metadata.exception.ApplicationException;
import io.hops.hopsworks.common.metadata.exception.DatabaseException;
import io.hops.hopsworks.common.upload.HttpUtils;
import io.hops.hopsworks.common.upload.ResumableInfo;
import io.hops.hopsworks.common.upload.ResumableInfoStorage;
import io.hops.hopsworks.common.upload.StagingManager;
import io.hops.hopsworks.common.util.Settings;
import io.hops.hopsworks.common.util.HopsUtils;
import com.google.api.client.repackaged.com.google.common.base.Strings;

@RequestScoped
@TransactionAttribute(TransactionAttributeType.NEVER)
public class UploadService {

  private final static Logger logger = Logger.getLogger(UploadService.class.
          getName());

  @EJB
  private NoCacheResponse noCacheResponse;
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
  @EJB
  private DistributedFsService dfs;
  @EJB
  private DatasetController datasetController;

  private String path;
  private String username;
  private String role;
  private Inode fileParent;
  private boolean isTemplate;
  private int templateId;

  public UploadService() {
  }

  /**
   * Sets the upload path for the file to be uploaded.
   * <p/>
   * @param dsPath the dsPath object built by the DatasetService.java
   * @param username the username of the user uploading the file
   * @param templateId the template to associate the the uploaded file
   * @param role
   * @throws AppException if new directories need to be created and the name
   * is not valid
   */
  public void confFileUpload(DsPath dsPath, String username,
                             int templateId, String role) throws AppException {
    if (dsPath.getDsRelativePath() != null) {
      // We need to validate that each component of the path, either it exists
      // or it is a valid directory name
      String[] dsPathComponents = dsPath.getDsRelativePath()
          .toString().split(File.separator);

      // Used to compute the partition id. Start from the depth of the Ds dir
      int depth = datasetController.getDatasetPath(dsPath.getDs()).depth() + 1;
      Inode parent = dsPath.getDs().getInode();
      boolean exist = true;

      for (String dirName : dsPathComponents) {
        if (parent != null) {
          int pathLen = depth;
          int partitionId = HopsUtils.calculatePartitionId(parent.getId(),
              dirName, pathLen);
          parent = inodes.findByInodePK(parent, dirName, partitionId);
          depth += 1;
        } else {
          FolderNameValidator.isValidName(dirName, true);
          exist = false;
        }
      }

      //if the path exists check if the file exists.
      if (exist) {
        this.fileParent = parent;
      }
    } else {
      // The user is trying to upload directly in a dataset.
      // We are sure the dir exists and the inode is the dataset inode
      this.fileParent = dsPath.getDs().getInode();
    }

    this.username = username;
    this.role = role;
    this.templateId = templateId;
    this.isTemplate = false;
    this.path = dsPath.getFullPath().toString();
  }


  /**
   * Configure the uploader to upload a metadata Template.
   * All the templates are uploaded to /Projects/Uploads
   * <p/>
   * @throws AppException
   */
  public void confUploadTemplate() throws AppException {
    DistributedFileSystemOps dfso = null;
    try {
      dfso = dfs.getDfsOps();
      if (!dfso.isDir(Settings.DIR_META_TEMPLATES)) {
        dfso.mkdir(Settings.DIR_META_TEMPLATES);
      }
    } catch (IOException e) {
      throw new AppException(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode(),
              "Uploads directory could not be created in the file system");
    } finally {
      if (dfso != null) {
        dfso.close();
      }
    }

    this.isTemplate = true;
    this.path = Settings.DIR_META_TEMPLATES;
  }

  @GET
  @Consumes(MediaType.MULTIPART_FORM_DATA)
  @Produces(MediaType.APPLICATION_JSON)
  @AllowedProjectRoles({AllowedProjectRoles.DATA_SCIENTIST, AllowedProjectRoles.DATA_OWNER})
  public Response testMethod(
          @Context HttpServletRequest request)
          throws AppException, IOException, AccessControlException {
    String fileName = request.getParameter("flowFilename");
    Inode parent;
    JsonResponse json = new JsonResponse();
    int resumableChunkNumber = getResumableChunkNumber(request);
    if (resumableChunkNumber == 1) {//check if file exist, permission only on the first chunk
      if (this.fileParent != null) {

        int pathLen = Utils.pathLen(this.path) - 1;
        int partitionId = HopsUtils.calculatePartitionId(this.fileParent.getId(),
                        fileName, pathLen);
        parent = inodes.findByInodePK(this.fileParent, fileName, partitionId);
        if (parent != null) {
          throw new AppException(Response.Status.BAD_REQUEST.getStatusCode(),
                  ResponseMessages.FILE_NAME_EXIST);
        }
      }
      //test if the user have permission to create a file in the path.
      //the file will be overwriten by the uploaded 
      //TODO: *** WARNING ***
      //Check permissions before creating file
      if (this.username != null) {
        DistributedFileSystemOps udfso = null;
        try {
          //If the user is a Data Owner in the owning project perform operation as superuser
          if (!Strings.isNullOrEmpty(role) && role.equals(AllowedProjectRoles.DATA_OWNER)) {
            udfso = dfs.getDfsOps();
          } else {
            udfso = dfs.getDfsOps(username);
          }
          udfso.touchz(new Path(this.path, fileName));
        } catch (AccessControlException ex) {
          throw new AccessControlException(
                  "Permission denied: You can not upload to this folder. ");
        } finally {
          dfs.closeDfsClient(udfso);
        }
      }
    }
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
  @AllowedProjectRoles({AllowedProjectRoles.DATA_SCIENTIST, AllowedProjectRoles.DATA_OWNER})
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
      DistributedFileSystemOps dfsOps = null;
      try {
        String fileContent = null;
        Path location = new Path(this.path, fileName);
        String stagingFilePath = new Path(stagingManager.getStagingPath() + this.path,
            fileName).toString();

        //if it is about a template file check its validity
        if (this.isTemplate) {
          //TODO. More checks needed to ensure the valid template format
          if (!Utils.checkJsonValidity(stagingFilePath)) {
            json.setErrorMsg("This was an invalid json file");
            return noCacheResponse.getNoCacheResponseBuilder(
                    Response.Status.NOT_ACCEPTABLE).entity(json).build();
          }
          fileContent = Utils.getFileContents(stagingFilePath);
          JsonObject obj = Json.createReader(new StringReader(fileContent)).
                  readObject();
          String templateName = obj.getString("templateName");
          if (template.isTemplateAvailable(templateName.toLowerCase())) {
            logger.log(Level.INFO, "{0} already exists.", templateName.
                    toLowerCase());
            json.setErrorMsg("Already exists.");
            return noCacheResponse.getNoCacheResponseBuilder(
                    Response.Status.NOT_ACCEPTABLE).entity(json).build();
          }

        }
        
        
        //If the user has a role in the owning project of the Dataset and that is Data Owner
        //perform operation as superuser
        if (!Strings.isNullOrEmpty(role) && role.equals(AllowedProjectRoles.DATA_OWNER)) {
          dfsOps = dfs.getDfsOps();
        } else {
          dfsOps = dfs.getDfsOps(username);
        } 

        dfsOps.copyToHDFSFromLocal(true, stagingFilePath, location.toString());
        dfsOps.setPermission(location, dfsOps.getParentPermission(location));
        dfsOps.setOwner(location, username, dfsOps.getFileStatus(location).getGroup());
        logger.log(Level.INFO, "Copied to HDFS");

        if (templateid != 0 && templateid != -1) {
          this.attachTemplateToInode(info, this.path + fileName);
        }

        if (this.isTemplate) {
          //if it is about a template file persist it in the database as well
          this.persistUploadedTemplate(fileContent);
        } else {
          //this is a common file being uploaded so add basic metadata to it
          //description and searchable
          Inode fileInode = inodes.getInodeAtPath(location.toString());
          InodeBasicMetadata basicMeta = new InodeBasicMetadata(fileInode, "", true);
          this.basicMetaFacade.addBasicMetadata(basicMeta);
        }

        json.setSuccessMessage("Successfuly uploaded file to " + this.path);
        return noCacheResponse.getNoCacheResponseBuilder(Response.Status.OK).
                entity(json).build();

      } catch (AccessControlException ex) {
        throw new AccessControlException(
                "Permission denied: You can not upload to this folder. ");

      } catch (IOException e) {
        logger.log(Level.INFO, "Failed to write to HDFS", e);
        json.setErrorMsg("Failed to write to HDFS");
        return noCacheResponse.getNoCacheResponseBuilder(
                Response.Status.BAD_REQUEST).entity(json).build();
      } finally {
        if (dfsOps != null) {
          dfs.closeDfsClient(dfsOps);
        }
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