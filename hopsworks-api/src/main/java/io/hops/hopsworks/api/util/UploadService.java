/*
 * Changes to this file committed after and not including commit-id: ccc0d2c5f9a5ac661e60e6eaf138de7889928b8b
 * are released under the following license:
 *
 * This file is part of Hopsworks
 * Copyright (C) 2018, Logical Clocks AB. All rights reserved
 *
 * Hopsworks is free software: you can redistribute it and/or modify it under the terms of
 * the GNU Affero General Public License as published by the Free Software Foundation,
 * either version 3 of the License, or (at your option) any later version.
 *
 * Hopsworks is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR
 * PURPOSE.  See the GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License along with this program.
 * If not, see <https://www.gnu.org/licenses/>.
 *
 * Changes to this file committed before and including commit-id: ccc0d2c5f9a5ac661e60e6eaf138de7889928b8b
 * are released under the following license:
 *
 * Copyright (C) 2013 - 2018, Logical Clocks AB and RISE SICS AB. All rights reserved
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this
 * software and associated documentation files (the "Software"), to deal in the Software
 * without restriction, including without limitation the rights to use, copy, modify, merge,
 * publish, distribute, sublicense, and/or sell copies of the Software, and to permit
 * persons to whom the Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or
 * substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS  OR IMPLIED, INCLUDING
 * BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL  THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM,
 * DAMAGES OR  OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */
package io.hops.hopsworks.api.util;

import com.google.api.client.repackaged.com.google.common.base.Strings;
import io.hops.hopsworks.api.filter.AllowedProjectRoles;
import io.hops.hopsworks.api.filter.Audience;
import io.hops.hopsworks.api.filter.NoCacheResponse;
import io.hops.hopsworks.api.filter.apiKey.ApiKeyRequired;
import io.hops.hopsworks.api.jwt.JWTHelper;
import io.hops.hopsworks.common.dao.hdfs.inode.InodeFacade;
import io.hops.hopsworks.common.dao.project.ProjectFacade;
import io.hops.hopsworks.common.dao.project.team.ProjectTeamFacade;
import io.hops.hopsworks.common.dataset.DatasetController;
import io.hops.hopsworks.common.dataset.util.DatasetHelper;
import io.hops.hopsworks.common.dataset.util.DatasetPath;
import io.hops.hopsworks.common.hdfs.DistributedFileSystemOps;
import io.hops.hopsworks.common.hdfs.DistributedFsService;
import io.hops.hopsworks.common.hdfs.HdfsUsersController;
import io.hops.hopsworks.common.hdfs.Utils;
import io.hops.hopsworks.common.upload.HttpUtils;
import io.hops.hopsworks.common.upload.ResumableInfo;
import io.hops.hopsworks.common.upload.ResumableInfoStorage;
import io.hops.hopsworks.common.upload.StagingManager;
import io.hops.hopsworks.common.util.HopsUtils;
import io.hops.hopsworks.exceptions.DatasetException;
import io.hops.hopsworks.exceptions.GenericException;
import io.hops.hopsworks.exceptions.MetadataException;
import io.hops.hopsworks.exceptions.ProjectException;
import io.hops.hopsworks.jwt.annotation.JWTRequired;
import io.hops.hopsworks.persistence.entity.dataset.DatasetType;
import io.hops.hopsworks.persistence.entity.hdfs.inode.Inode;
import io.hops.hopsworks.persistence.entity.project.Project;
import io.hops.hopsworks.persistence.entity.user.Users;
import io.hops.hopsworks.persistence.entity.user.security.apiKey.ApiScope;
import io.hops.hopsworks.restutils.RESTCodes;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.security.AccessControlException;
import org.glassfish.jersey.media.multipart.FormDataContentDisposition;
import org.glassfish.jersey.media.multipart.FormDataParam;

import javax.ejb.EJB;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.enterprise.context.RequestScoped;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.SecurityContext;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.util.logging.Level;
import java.util.logging.Logger;

@RequestScoped
@TransactionAttribute(TransactionAttributeType.NEVER)
public class UploadService {

  private static final Logger logger = Logger.getLogger(UploadService.class.getName());

  @EJB
  private NoCacheResponse noCacheResponse;
  @EJB
  private InodeFacade inodes;
  @EJB
  private StagingManager stagingManager;
  @EJB
  private DistributedFsService dfs;
  @EJB
  private DatasetController datasetController;
  @EJB
  private JWTHelper jWTHelper;
  @EJB
  private ProjectTeamFacade projectTeamFacade;
  @EJB
  private DatasetHelper datasetHelper;
  @EJB
  private HdfsUsersController hdfsUsersBean;
  @EJB
  private ProjectFacade projectFacade;

  private String path;
  private DatasetType datasetType;
  private String username;
  private String role;
  private Inode fileParent;
  
  private Integer projectId;
  private String projectName;
  
  private Project getProjectById() throws ProjectException {
    Project project = projectFacade.find(this.projectId);
    if (project == null) {
      throw new ProjectException(RESTCodes.ProjectErrorCode.PROJECT_NOT_FOUND, Level.FINE, "projectId: " + projectId);
    }
    return project;
  }
  
  private Project getProjectByName() throws ProjectException {
    Project project = projectFacade.findByName(this.projectName);
    if (project == null) {
      throw new ProjectException(RESTCodes.ProjectErrorCode.PROJECT_NOT_FOUND, Level.FINE, "projectName: " +
        projectName);
    }
    return project;
  }
  
  private Project getProject() throws ProjectException {
    return this.projectId != null ? getProjectById() : getProjectByName();
  }

  public UploadService() {
  }
  
  public void setParams(Integer projectId, String path, DatasetType datasetType) {
    this.projectId = projectId;
    this.path = path;
    this.datasetType = datasetType;
  }
  
  public void setParams(String projectName, String path, DatasetType datasetType) {
    this.projectName = projectName;
    this.path = path;
    this.datasetType = datasetType;
  }

  /**
   * Sets the upload path for the file to be uploaded.
   * <p/>
   * @param datasetPath the dsPath object built by the DatasetService.java
   * @throws DatasetException DatasetException
   */
  private void confFileUpload(DatasetPath datasetPath) {
    if (datasetPath.getRelativePath() != null) {
      // We need to validate that each component of the path, either it exists
      // or it is a valid directory name
      String[] dsPathComponents = datasetPath.getDatasetRelativePath().split(File.separator);

      // Used to compute the partition id. Start from the depth of the Ds dir
      int depth = datasetController.getDatasetPath(datasetPath.getDataset()).depth() + 1;
      Inode parent = datasetPath.getDataset().getInode();
      boolean exist = true;

      for (String dirName : dsPathComponents) {
        if (parent != null) {
          int pathLen = depth;
          long partitionId = HopsUtils.calculatePartitionId(parent.getId(), dirName, pathLen);
          parent = inodes.findByInodePK(parent, dirName, partitionId);
          depth += 1;
        } else {
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
      this.fileParent = datasetPath.getDataset().getInode();
    }

    this.path = datasetPath.getFullPath().toString();
  }
  
  private void configureUploader(SecurityContext sc) throws DatasetException, ProjectException {
    Users user = jWTHelper.getUserPrincipal(sc);
    Project project =this.getProject();
    this.username = hdfsUsersBean.getHdfsUserName(project, user);
    DatasetPath datasetPath = datasetHelper.getDatasetPath(project, path, this.datasetType);
    Project owning = datasetController.getOwningProject(datasetPath.getDataset());
    //Is user a member of this project? If so get their role
    boolean isMember = projectTeamFacade.isUserMemberOfProject(owning, user);
    this.role = null;
    if (isMember) {
      role = projectTeamFacade.findCurrentRole(owning, user);
    }
    confFileUpload(datasetPath);
  }

  @GET
  @Consumes(MediaType.MULTIPART_FORM_DATA)
  @Produces(MediaType.APPLICATION_JSON)
  @AllowedProjectRoles({AllowedProjectRoles.DATA_SCIENTIST, AllowedProjectRoles.DATA_OWNER})
  @JWTRequired(acceptedTokens={Audience.API, Audience.JOB}, allowedUserRoles={"HOPS_ADMIN", "HOPS_USER"})
  @ApiKeyRequired( acceptedScopes = {ApiScope.DATASET_CREATE}, allowedUserRoles = {"HOPS_ADMIN", "HOPS_USER"})
  public Response testMethod(@QueryParam("flowChunkNumber") String flowChunkNumber,
    @QueryParam("flowChunkSize") String flowChunkSize,
    @QueryParam("flowCurrentChunkSize") String flowCurrentChunkSize,
    @QueryParam("flowFilename") String flowFilename,
    @QueryParam("flowIdentifier") String flowIdentifier,
    @QueryParam("flowRelativePath") String flowRelativePath,
    @QueryParam("flowTotalChunks") String flowTotalChunks,
    @QueryParam("flowTotalSize") String flowTotalSize,
    @Context HttpServletRequest request, @Context SecurityContext sc) throws IOException,
    DatasetException, ProjectException {
    configureUploader(sc);
    String fileName = flowFilename;
    Inode parent;
    RESTApiJsonResponse json = new RESTApiJsonResponse();
    int resumableChunkNumber = getResumableChunkNumber(request);
    if (resumableChunkNumber == 1) {//check if file exist, permission only on the first chunk
      if (this.fileParent != null) {
        int pathLen = Utils.pathLen(this.path) - 1;
        long partitionId = HopsUtils.calculatePartitionId(this.fileParent.getId(), fileName, pathLen);
        parent = inodes.findByInodePK(this.fileParent, fileName, partitionId);
        if (parent != null) {
          throw new DatasetException(RESTCodes.DatasetErrorCode.DESTINATION_EXISTS, Level.FINE,
            "filename: " + fileName);
        }
      }
      //test if the user have permission to create a file in the path.
      //the file will be overwriten by the uploaded 
      //TODO: *** WARNING ***
      //Check permissions before creating file
      if (this.username != null) {
        DistributedFileSystemOps udfso = null;
        try {
          udfso = dfs.getDfsOps(username);
          udfso.touchz(new Path(this.path, fileName));
        } catch (AccessControlException ex) {
          throw new AccessControlException("Permission denied: You can not upload to this folder. ");
        } finally {
          dfs.closeDfsClient(udfso);
        }
      }
    }
    ResumableInfo info = getResumableInfo(request, this.path);
    if (info.isUploaded(new ResumableInfo.ResumableChunkNumber(resumableChunkNumber))) {
      json.setSuccessMessage("Uploaded");//This Chunk has been Uploaded.
      return noCacheResponse.getNoCacheResponseBuilder(Response.Status.OK).entity(json).build();
    } else {
      json.setErrorMsg("Not uploaded");
      return noCacheResponse.getNoCacheResponseBuilder(Response.Status.NO_CONTENT).entity(json).build();
    }

  }

  @POST
  @Consumes(MediaType.MULTIPART_FORM_DATA)
  @Produces(MediaType.APPLICATION_JSON)
  @AllowedProjectRoles({AllowedProjectRoles.DATA_SCIENTIST, AllowedProjectRoles.DATA_OWNER})
  @JWTRequired(acceptedTokens = {Audience.API, Audience.JOB}, allowedUserRoles = {"HOPS_ADMIN", "HOPS_USER"})
  @ApiKeyRequired( acceptedScopes = {ApiScope.DATASET_CREATE}, allowedUserRoles = {"HOPS_ADMIN", "HOPS_USER"})
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
      @FormDataParam("flowTotalSize") String flowTotalSize, @Context SecurityContext sc)
      throws IOException, GenericException, MetadataException, DatasetException, ProjectException {
    configureUploader(sc);
    RESTApiJsonResponse json = new RESTApiJsonResponse();

    int resumableChunkNumber = HttpUtils.toInt(flowChunkNumber, -1);
    ResumableInfo info = getResumableInfo(flowChunkSize, flowFilename, flowIdentifier, flowRelativePath, flowTotalSize,
      this.path);
    String fileName = info.getResumableFilename();

    long content_length;
    //Seek to position
    try (RandomAccessFile raf = new RandomAccessFile(info.getResumableFilePath(), "rw");
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
    if (info.addChunkAndCheckIfFinished(new ResumableInfo.ResumableChunkNumber(resumableChunkNumber), content_length)) {
      //Check if all chunks uploaded, and change filename
      ResumableInfoStorage.getInstance().remove(info);
      logger.log(Level.INFO, "All finished.");
      finished = true;
    } else {
      json.setSuccessMessage("Upload");//This Chunk has been Uploaded.
      return noCacheResponse.getNoCacheResponseBuilder(Response.Status.OK).entity(json).build();
    }

    if (finished) {
      DistributedFileSystemOps dfsOps = null;
      try {
        String fileContent = null;
        Path location = new Path(this.path, fileName);
        String stagingFilePath = new Path(stagingManager.getStagingPath() + this.path, fileName).toString();

        //If the user has a role in the owning project of the Dataset and that is Data Owner
        //perform operation as superuser
        if ((!Strings.isNullOrEmpty(role) && role.equals(AllowedProjectRoles.DATA_OWNER))) {
          dfsOps = dfs.getDfsOps();
        } else {
          dfsOps = dfs.getDfsOps(username);
        }

        dfsOps.copyToHDFSFromLocal(true, stagingFilePath, location.toString());
        dfsOps.setPermission(location, dfsOps.getParentPermission(location));
        dfsOps.setOwner(location, username, dfsOps.getFileStatus(location).getGroup());
        logger.log(Level.INFO, "Copied to HDFS");

        json.setSuccessMessage("Successfuly uploaded file to " + this.path);
        return noCacheResponse.getNoCacheResponseBuilder(Response.Status.OK).entity(json).build();

      } catch (AccessControlException ex) {
        throw new AccessControlException("Permission denied: You can not upload to this folder. ");
      } finally {
        if (dfsOps != null) {
          dfs.closeDfsClient(dfsOps);
        }
      }
    }

    json.setSuccessMessage("Uploading...");
    return noCacheResponse.getNoCacheResponseBuilder(Response.Status.OK).entity(json).build();
  }

  private int getResumableChunkNumber(HttpServletRequest request) {
    return HttpUtils.toInt(request.getParameter("flowChunkNumber"), -1);
  }

  private ResumableInfo getResumableInfo(HttpServletRequest request, String hdfsPath)
    throws DatasetException {
    //this will give us a tmp folder
    String baseDir = stagingManager.getStagingPath();
    //this will create a folder if it does not exist inside the tmp folder 
    //specific to where the file is being uploaded
    File userTmpDir = new File(baseDir + hdfsPath);
    if (!userTmpDir.exists()) {
      userTmpDir.mkdirs();
    }
    baseDir = userTmpDir.getAbsolutePath();

    int resumableChunkSize = HttpUtils.toInt(request.getParameter("flowChunkSize"), -1);
    long resumableTotalSize = HttpUtils.toLong(request.getParameter("flowTotalSize"), -1);
    String resumableIdentifier = request.getParameter("flowIdentifier");
    String resumableFilename = request.getParameter("flowFilename");
    String resumableRelativePath = request.getParameter("flowRelativePath");

    File file = new File(baseDir, resumableFilename);
    //check if the file alrady exists in the staging dir.
    if (file.exists() && file.canRead()) {
      //file.exists returns true after deleting a file 
      //so make sure the file exists by trying to read from it.
      try (FileReader fileReader = new FileReader(file.getAbsolutePath())) {
        fileReader.read();
        throw new DatasetException(RESTCodes.DatasetErrorCode.UPLOAD_CONCURRENT_ERROR, Level.WARNING);
      } catch (IOException ex) {
        throw new DatasetException(RESTCodes.DatasetErrorCode.UPLOAD_ERROR, Level.SEVERE, null, ex.getMessage(), ex);
      }
    }

    //Here we add a ".temp" to every upload file to indicate NON-FINISHED
    String resumableFilePath = file.getAbsolutePath() + ".temp";

    ResumableInfoStorage storage = ResumableInfoStorage.getInstance();

    ResumableInfo info = storage.get(resumableChunkSize, resumableTotalSize, resumableIdentifier, resumableFilename,
      resumableRelativePath, resumableFilePath);
    if (!info.valid()) {
      storage.remove(info);
      throw new DatasetException(RESTCodes.DatasetErrorCode.UPLOAD_RESUMABLEINFO_INVALID, Level.WARNING);
    }
    return info;
  }

  private ResumableInfo getResumableInfo(String flowChunkSize, String flowFilename, String flowIdentifier,
                                         String flowRelativePath, String flowTotalSize, String hdfsPath)
      throws DatasetException {
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

    File file = new File(base_dir, flowFilename);
    //check if the file alrady exists in the staging dir.
    if (file.exists() && file.canRead()) {
      //file.exists returns true after deleting a file 
      //so make sure the file exists by trying to read from it.
      try (FileReader fileReader = new FileReader(file.getAbsolutePath())) {
        fileReader.read();
        throw new DatasetException(RESTCodes.DatasetErrorCode.UPLOAD_CONCURRENT_ERROR, Level.WARNING);
      } catch (IOException ex) {
        throw new DatasetException(RESTCodes.DatasetErrorCode.UPLOAD_ERROR, Level.SEVERE, null, ex.getMessage(), ex);
      }
    }

    //Here we add a ".temp" to every upload file to indicate NON-FINISHED
    String resumableFilePath = file.getAbsolutePath() + ".temp";

    ResumableInfoStorage storage = ResumableInfoStorage.getInstance();

    ResumableInfo info = storage.get(resumableChunkSize, resumableTotalSize, flowIdentifier, flowFilename,
      flowRelativePath, resumableFilePath);
    if (!info.valid()) {
      storage.remove(info);
      throw new DatasetException(RESTCodes.DatasetErrorCode.UPLOAD_RESUMABLEINFO_INVALID, Level.WARNING);
    }
    return info;
  }
}
