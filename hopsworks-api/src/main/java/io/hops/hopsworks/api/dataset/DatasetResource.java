/*
 * This file is part of Hopsworks
 * Copyright (C) 2019, Logical Clocks AB. All rights reserved
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
 */
package io.hops.hopsworks.api.dataset;

import io.hops.hopsworks.api.dataset.inode.InodeBeanParam;
import io.hops.hopsworks.api.dataset.inode.InodeBuilder;
import io.hops.hopsworks.api.dataset.inode.InodeDTO;
import io.hops.hopsworks.api.dataset.tags.DatasetTagsResource;
import io.hops.hopsworks.api.filter.AllowedProjectRoles;
import io.hops.hopsworks.api.filter.Audience;
import io.hops.hopsworks.api.filter.apiKey.ApiKeyRequired;
import io.hops.hopsworks.api.jwt.JWTHelper;
import io.hops.hopsworks.api.util.DownloadService;
import io.hops.hopsworks.api.util.Pagination;
import io.hops.hopsworks.api.util.UploadService;
import io.hops.hopsworks.common.api.ResourceRequest;
import io.hops.hopsworks.common.constants.auth.AllowedRoles;
import io.hops.hopsworks.common.dao.project.team.ProjectTeamFacade;
import io.hops.hopsworks.common.dataset.DatasetController;
import io.hops.hopsworks.common.dataset.FilePreviewMode;
import io.hops.hopsworks.common.dataset.util.DatasetHelper;
import io.hops.hopsworks.common.dataset.util.DatasetPath;
import io.hops.hopsworks.common.hdfs.inode.InodeController;
import io.hops.hopsworks.common.project.ProjectController;
import io.hops.hopsworks.common.provenance.core.HopsFSProvenanceController;
import io.hops.hopsworks.common.provenance.core.Provenance;
import io.hops.hopsworks.common.provenance.core.dto.ProvTypeDTO;
import io.hops.hopsworks.exceptions.DatasetException;
import io.hops.hopsworks.exceptions.HopsSecurityException;
import io.hops.hopsworks.exceptions.MetadataException;
import io.hops.hopsworks.exceptions.ProjectException;
import io.hops.hopsworks.exceptions.ProvenanceException;
import io.hops.hopsworks.exceptions.SchematizedTagException;
import io.hops.hopsworks.jwt.annotation.JWTRequired;
import io.hops.hopsworks.persistence.entity.dataset.Dataset;
import io.hops.hopsworks.persistence.entity.dataset.DatasetAccessPermission;
import io.hops.hopsworks.persistence.entity.dataset.DatasetType;
import io.hops.hopsworks.persistence.entity.hdfs.inode.Inode;
import io.hops.hopsworks.persistence.entity.project.Project;
import io.hops.hopsworks.persistence.entity.user.Users;
import io.hops.hopsworks.persistence.entity.user.security.apiKey.ApiScope;
import io.hops.hopsworks.restutils.RESTCodes;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;

import javax.ejb.EJB;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;
import javax.ws.rs.BeanParam;
import javax.ws.rs.DELETE;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.SecurityContext;
import javax.ws.rs.core.UriInfo;
import java.util.logging.Level;
import java.util.logging.Logger;

@Api(value = "Dataset Resource")
@RequestScoped
@TransactionAttribute(TransactionAttributeType.NEVER)
public class DatasetResource {
  
  private static final Logger LOGGER = Logger.getLogger(DatasetResource.class.getName());
  
  @EJB
  private DatasetController datasetController;
  @EJB
  private DatasetBuilder datasetBuilder;
  @EJB
  private ProjectTeamFacade projectTeamFacade;
  @EJB
  private ProjectController projectController;
  @EJB
  private InodeBuilder inodeBuilder;
  @EJB
  private InodeController inodeController;
  @EJB
  private JWTHelper jwtHelper;
  @EJB
  private DatasetHelper datasetHelper;
  @Inject
  private DownloadService downloadService;
  @Inject
  private UploadService uploader;
  @EJB
  private HopsFSProvenanceController fsProvenanceController;
  @Inject
  private DatasetTagsResource tagsResource;
  @EJB
  private JWTHelper jWTHelper;

  private Integer projectId;
  private String projectName;

  public void setProjectId(Integer projectId) {
    this.projectId = projectId;
  }

  public void setProjectName(String projectName) {
    this.projectName = projectName;
  }
  
  private Project getProject() throws ProjectException {
    if (this.projectId != null) {
      return projectController.findProjectById(this.projectId);
    } else if (this.projectName != null) {
      return projectController.findProjectByName(this.projectName);
    }
    throw new ProjectException(RESTCodes.ProjectErrorCode.PROJECT_NOT_FOUND, Level.FINE);
  }
  
  private void checkIfDataOwner(Project project, Users user) throws DatasetException {
    if (!projectTeamFacade.findCurrentRole(project, user).equals(AllowedRoles.DATA_OWNER)) {
      throw new DatasetException(RESTCodes.DatasetErrorCode.DATASET_ACCESS_PERMISSION_DENIED, Level.FINE);
    }
  }

  @GET
  @Produces(MediaType.APPLICATION_JSON)
  @ApiOperation(value = "Get all datasets.", response = DatasetDTO.class)
  @AllowedProjectRoles({AllowedProjectRoles.DATA_OWNER, AllowedProjectRoles.DATA_SCIENTIST})
  @JWTRequired(acceptedTokens = {Audience.API}, allowedUserRoles = {"HOPS_ADMIN", "HOPS_USER"})
  @ApiKeyRequired( acceptedScopes = {ApiScope.DATASET_VIEW}, allowedUserRoles = {"HOPS_ADMIN", "HOPS_USER"})
  public Response get(@BeanParam Pagination pagination, @BeanParam DatasetBeanParam datasetBeanParam,
    @Context UriInfo uriInfo, @Context SecurityContext sc)
    throws ProjectException, DatasetException, MetadataException, SchematizedTagException {
    Users user = jWTHelper.getUserPrincipal(sc);
    ResourceRequest resourceRequest = new ResourceRequest(ResourceRequest.Name.DATASET);
    resourceRequest.setOffset(pagination.getOffset());
    resourceRequest.setLimit(pagination.getLimit());
    resourceRequest.setSort(datasetBeanParam.getSortBySet());
    resourceRequest.setFilter(datasetBeanParam.getFilter());
    resourceRequest.setExpansions(datasetBeanParam.getExpansions().getResources());
    ResourceRequest sharedDatasetResourceRequest = new ResourceRequest(ResourceRequest.Name.DATASET);
    sharedDatasetResourceRequest.setOffset(pagination.getOffset());
    sharedDatasetResourceRequest.setLimit(pagination.getLimit());
    sharedDatasetResourceRequest.setSort(datasetBeanParam.getSharedWithSortBySet());
    sharedDatasetResourceRequest.setFilter(datasetBeanParam.getSharedWithFilter());
    sharedDatasetResourceRequest.setExpansions(datasetBeanParam.getExpansions().getResources());
    DatasetDTO dto = datasetBuilder.buildItems(uriInfo, resourceRequest, sharedDatasetResourceRequest,
      this.getProject(), user);
    return Response.ok().entity(dto).build();
  }
  
  @GET
  @Path("{path: .+}")
  @Produces(MediaType.APPLICATION_JSON)
  @ApiOperation(value = "Get or list files in path.", response = InodeDTO.class)
  @AllowedProjectRoles({AllowedProjectRoles.DATA_OWNER, AllowedProjectRoles.DATA_SCIENTIST})
  @JWTRequired(acceptedTokens = {Audience.API, Audience.JOB}, allowedUserRoles = {"HOPS_ADMIN", "HOPS_USER"})
  @ApiKeyRequired( acceptedScopes = {ApiScope.DATASET_VIEW}, allowedUserRoles = {"HOPS_ADMIN", "HOPS_USER"})
  public Response getByPath(@PathParam("path") String path, @QueryParam("type") DatasetType datasetType,
    @QueryParam("action") DatasetActions.Get action, @QueryParam("mode") FilePreviewMode mode,
    @BeanParam Pagination pagination, @BeanParam InodeBeanParam inodeBeanParam,
    @BeanParam DatasetExpansionBeanParam datasetExpansionBeanParam, @Context UriInfo uriInfo,
    @Context SecurityContext sc) throws DatasetException, ProjectException, MetadataException, SchematizedTagException {
    Users user = jwtHelper.getUserPrincipal(sc);
    ResourceRequest resourceRequest = new ResourceRequest(ResourceRequest.Name.INODES);
    resourceRequest.setExpansions(datasetExpansionBeanParam.getResources());
    Project project = this.getProject();
    DatasetPath datasetPath = datasetHelper.getDatasetPathIfFileExist(project, path, datasetType);
    InodeDTO dto;
    switch (action == null? DatasetActions.Get.STAT : action) {
      case BLOB:
        dto = inodeBuilder.buildBlob(uriInfo, resourceRequest, user, datasetPath, mode);
        break;
      case LISTING:
        resourceRequest.setOffset(pagination.getOffset());
        resourceRequest.setLimit(pagination.getLimit());
        resourceRequest.setSort(inodeBeanParam.getSortBySet());
        resourceRequest.setFilter(inodeBeanParam.getFilter());
        dto = inodeBuilder.buildItems(uriInfo, resourceRequest, user, datasetPath);
        break;
      case STAT:
        if (datasetPath.isTopLevelDataset()) {
          ResourceRequest datasetResourceRequest = new ResourceRequest(ResourceRequest.Name.DATASET);
          datasetResourceRequest.setExpansions(datasetExpansionBeanParam.getResources());
          DatasetDTO datasetDTO = datasetBuilder.build(uriInfo, datasetResourceRequest, user, datasetPath,
            null, null, true);
          return Response.ok().entity(datasetDTO).build();
        } else {
          dto = inodeBuilder.buildStat(uriInfo, resourceRequest, user, datasetPath);
        }
        break;
      default:
        throw new WebApplicationException("Action not valid.", Response.Status.NOT_FOUND);
    }
    return Response.ok().entity(dto).build();
  }
  
  @POST
  @Path("{path: .+}")
  @Produces(MediaType.APPLICATION_JSON)
  @ApiOperation(value = "Post an action on a file, dir or dataset.")
  @AllowedProjectRoles({AllowedProjectRoles.DATA_OWNER, AllowedProjectRoles.DATA_SCIENTIST})
  @JWTRequired(acceptedTokens = {Audience.API, Audience.JOB}, allowedUserRoles = {"HOPS_ADMIN", "HOPS_USER"})
  @ApiKeyRequired( acceptedScopes = {ApiScope.DATASET_CREATE}, allowedUserRoles = {"HOPS_ADMIN", "HOPS_USER"})
  public Response postByPath(@Context UriInfo uriInfo, @Context SecurityContext sc,
                             @PathParam("path") String path,
                             @QueryParam("type") DatasetType datasetType,
                             @QueryParam("target_project") String targetProjectName,
                             @QueryParam("action") DatasetActions.Post action,
                             @QueryParam("description") String description,
                             @QueryParam("searchable") Boolean searchable,
                             @QueryParam("generate_readme") Boolean generateReadme,
                             @QueryParam("destination_path") String destPath,
                             @QueryParam("destination_type") DatasetType destDatasetType,
                             @DefaultValue("READ_ONLY") @QueryParam("permission") DatasetAccessPermission permission)
    throws DatasetException, ProjectException, HopsSecurityException, ProvenanceException, MetadataException,
           SchematizedTagException {
    Users user = jwtHelper.getUserPrincipal(sc);
    DatasetPath datasetPath;
    DatasetPath distDatasetPath;
    Project project = this.getProject();
    switch (action == null? DatasetActions.Post.CREATE : action) {
      case CREATE:
        if(datasetType != null && !datasetType.equals(DatasetType.DATASET)) {
          //can only create dataset
          throw new DatasetException(RESTCodes.DatasetErrorCode.DATASET_OPERATION_INVALID, Level.FINE);
        }
        datasetPath = datasetHelper.getNewDatasetPath(project, path, DatasetType.DATASET);
        if (datasetPath.isTopLevelDataset()) {
          checkIfDataOwner(project, user);
        }
        if(datasetPath.isTopLevelDataset() && !datasetHelper.isBasicDatasetProjectParent(project, datasetPath)) {
          //fake shared dataset with :: in dataset name at dataset creation
          throw new DatasetException(RESTCodes.DatasetErrorCode.DATASET_NAME_INVALID, Level.FINE);
        }
        ProvTypeDTO projectProvCore = fsProvenanceController.getMetaStatus(user, project, searchable);
        ResourceRequest resourceRequest;
        if (datasetPath.isTopLevelDataset()) {
          datasetController.createDirectory(project, user, datasetPath.getFullPath(), datasetPath.getDatasetName(),
            datasetPath.isTopLevelDataset(), description,
            Provenance.getDatasetProvCore(projectProvCore, Provenance.MLType.DATASET), generateReadme, permission);
          resourceRequest = new ResourceRequest(ResourceRequest.Name.DATASET);
          Dataset ds = datasetController.getByProjectAndFullPath(project, datasetPath.getFullPath().toString());
          datasetHelper.updateDataset(project, datasetPath, ds);
          datasetPath.setInode(ds.getInode());
          DatasetDTO dto = datasetBuilder.build(uriInfo, resourceRequest, user, datasetPath, null, null, false);
          return Response.created(dto.getHref()).entity(dto).build();
        } else {
          datasetHelper.checkIfDatasetExists(project, datasetPath);
          datasetHelper.updateDataset(project, datasetPath);
          datasetController.createDirectory(project, user, datasetPath.getFullPath(), datasetPath.getDatasetName(),
            datasetPath.isTopLevelDataset(), description,
            Provenance.getDatasetProvCore(projectProvCore, Provenance.MLType.DATASET), generateReadme, permission);
          resourceRequest = new ResourceRequest(ResourceRequest.Name.INODES);
          Inode inode = inodeController.getInodeAtPath(datasetPath.getFullPath().toString());
          datasetPath.setInode(inode);
          InodeDTO dto = inodeBuilder.buildStat(uriInfo, resourceRequest, user, datasetPath, inode);
          return Response.created(dto.getHref()).entity(dto).build();
        }
      case COPY:
        datasetPath = datasetHelper.getDatasetPathIfFileExist(project, path, datasetType);
        distDatasetPath = datasetHelper.getDatasetPath(project, destPath, destDatasetType);
        datasetController.copy(project, user, datasetPath.getFullPath(), distDatasetPath.getFullPath(),
          datasetPath.getDataset(), distDatasetPath.getDataset());
        break;
      case MOVE:
        datasetPath = datasetHelper.getDatasetPathIfFileExist(project, path, datasetType);
        distDatasetPath = datasetHelper.getDatasetPath(project, destPath, destDatasetType);
        datasetController.move(project, user, datasetPath.getFullPath(), distDatasetPath.getFullPath(),
          datasetPath.getDataset(), distDatasetPath.getDataset());
        break;
      case SHARE:
        checkIfDataOwner(project, user);
        datasetPath = datasetHelper.getDatasetPathIfFileExist(project, path, datasetType);
        datasetController.share(targetProjectName, datasetPath.getFullPath().toString(), permission, project, user);
        break;
      case ACCEPT:
        checkIfDataOwner(project, user);
        datasetPath = datasetHelper.getDatasetPathIfFileExist(project, path, datasetType);
        datasetController.acceptShared(project, user, datasetPath.getDatasetSharedWith());
        break;
      case ZIP:
        datasetPath = datasetHelper.getDatasetPathIfFileExist(project, path, datasetType);
        if(destPath != null) {
          distDatasetPath = datasetHelper.getDatasetPath(project, destPath, destDatasetType);
          datasetController.zip(project, user, datasetPath.getFullPath(), distDatasetPath.getFullPath());
        } else {
          datasetController.zip(project, user, datasetPath.getFullPath(), null);
        }
        break;
      case UNZIP:
        datasetPath = datasetHelper.getDatasetPathIfFileExist(project, path, datasetType);
        if(destPath != null) {
          distDatasetPath = datasetHelper.getDatasetPath(project, destPath, destDatasetType);
          datasetController.unzip(project, user, datasetPath.getFullPath(), distDatasetPath.getFullPath());
        } else {
          datasetController.unzip(project, user, datasetPath.getFullPath(), null);
        }
        break;
      case REJECT:
        checkIfDataOwner(project, user);
        datasetPath = datasetHelper.getDatasetPathIfFileExist(project, path, datasetType);
        datasetController.rejectShared(datasetPath.getDatasetSharedWith());
        break;
      case PUBLISH:
        checkIfDataOwner(project, user);
        datasetPath = datasetHelper.getDatasetPathIfFileExist(project, path, datasetType);
        datasetController.shareWithCluster(project, datasetPath.getDataset(), user, datasetPath.getFullPath());
        break;
      case UNPUBLISH:
        checkIfDataOwner(project, user);
        datasetPath = datasetHelper.getDatasetPathIfFileExist(project, path, datasetType);
        datasetController.unshareFromCluster(project, datasetPath.getDataset(), user, datasetPath.getFullPath());
        break;
      case IMPORT:
        checkIfDataOwner(project, user);
        Project srcProject = projectController.findProjectByName(targetProjectName);
        datasetPath = datasetHelper.getDatasetPathIfFileExist(srcProject, path, datasetType);
        datasetController.share(project.getName(), datasetPath.getFullPath().toString(),
          DatasetAccessPermission.READ_ONLY, srcProject, user);
        break;
      case UNSHARE_ALL:
        checkIfDataOwner(project, user);
        datasetPath = datasetHelper.getDatasetPathIfFileExist(project, path, datasetType);
        datasetController.unshareAll(datasetPath.getDataset(), user);
        break;
      default:
        throw new WebApplicationException("Action not valid.", Response.Status.NOT_FOUND);
    }
    return Response.noContent().build();
  }
  
  @PUT
  @Path("{path: .+}")
  @Produces(MediaType.APPLICATION_JSON)
  @AllowedProjectRoles({AllowedProjectRoles.DATA_SCIENTIST, AllowedProjectRoles.DATA_OWNER})
  @ApiOperation(value = "Set permissions (potentially with sticky bit) for datasets",
    notes = "Allow data scientists to create and modify own files in dataset.", response = DatasetDTO.class)
  @JWTRequired(acceptedTokens={Audience.API, Audience.JOB}, allowedUserRoles={"HOPS_ADMIN", "HOPS_USER"})
  @ApiKeyRequired( acceptedScopes = {ApiScope.DATASET_CREATE}, allowedUserRoles = {"HOPS_ADMIN", "HOPS_USER"})
  public Response update(@PathParam("path") String path, @QueryParam("type") DatasetType datasetType,
    @QueryParam("action") DatasetActions.Put action, @QueryParam("description") String description,
    @DefaultValue("READ_ONLY") @QueryParam("permissions") DatasetAccessPermission datasetPermissions,
    @QueryParam("target_project") String targetProjectName, @Context UriInfo uriInfo, @Context SecurityContext sc)
    throws DatasetException, ProjectException, MetadataException, SchematizedTagException {
    Project project = this.getProject();
    DatasetPath datasetPath = datasetHelper.getDatasetPath(project, path, datasetType);
    ResourceRequest resourceRequest = new ResourceRequest(ResourceRequest.Name.DATASET);
    Users user = jwtHelper.getUserPrincipal(sc);
    DatasetDTO dto;
    switch (action == null ? DatasetActions.Put.DESCRIPTION : action) {
      case PERMISSION:
        checkIfDataOwner(project, user);
        datasetController.updatePermission(datasetPath.getDataset(), datasetPermissions, project, project, user);
        dto = datasetBuilder.build(uriInfo, resourceRequest, user, datasetPath, null, null, false);
        break;
      case SHARE_PERMISSION:
        checkIfDataOwner(project, user);
        datasetController.updateSharePermission(datasetPath.getDataset(), datasetPermissions, project, targetProjectName
          , user);
        dto = datasetBuilder.build(uriInfo, resourceRequest, user, datasetPath, null, null, false);
        break;
      case DESCRIPTION:
        datasetController.updateDescription(project, user, datasetPath.getDataset(), description);
        dto = datasetBuilder.build(uriInfo, resourceRequest, user, datasetPath, null, null, false);
        break;
      default:
        throw new WebApplicationException("Action not valid.", Response.Status.NOT_FOUND);
    }
    return Response.ok(dto).build();
  }
  
  @DELETE
  @Path("{path: .+}")
  @Produces(MediaType.APPLICATION_JSON)
  @AllowedProjectRoles({AllowedProjectRoles.DATA_SCIENTIST, AllowedProjectRoles.DATA_OWNER})
  @ApiOperation(value = "Delete/unshare dataset")
  @JWTRequired(acceptedTokens={Audience.API, Audience.JOB}, allowedUserRoles={"HOPS_ADMIN", "HOPS_USER"})
  @ApiKeyRequired( acceptedScopes = {ApiScope.DATASET_DELETE}, allowedUserRoles = {"HOPS_ADMIN", "HOPS_USER"})
  public Response delete(@PathParam("path") String path, @QueryParam("type") DatasetType datasetType,
    @QueryParam("action") DatasetActions.Delete action, @QueryParam("target_project") String targetProject,
    @Context SecurityContext sc) throws DatasetException, ProjectException {
    DatasetPath datasetPath = datasetHelper.getDatasetPath(this.getProject(), path, datasetType);
    Users user = jwtHelper.getUserPrincipal(sc);
    Project project = this.getProject();
    if (action == null) {
      datasetController.delete(project, user, datasetPath.getFullPath(), datasetPath.getDataset(),
        datasetPath.isTopLevelDataset());
    } else {
      switch (action) {
        case UNSHARE:
          checkIfDataOwner(project, user);
          datasetController.unshare(project, user, datasetPath.getDataset(), targetProject);
          break;
        case CORRUPTED:
          if (datasetPath.isTopLevelDataset()) {
            throw new IllegalArgumentException("Use DELETE /{datasetName} to delete top level dataset)");
          }
          datasetController
            .deleteCorrupted(project, user, datasetPath.getFullPath(), datasetPath.getDataset());
          break;
        default:
          throw new WebApplicationException("Action not valid.", Response.Status.NOT_FOUND);
      }
    }
    return Response.noContent().build();
  }
  
  @Path("/download")
  public DownloadService download() {
    this.downloadService.setProjectId(this.projectId);
    return this.downloadService;
  }
  
  @Path("upload/{path: .+}")
  public UploadService upload(@PathParam("path") String path,
                              @QueryParam("type") DatasetType datasetType) {
    this.uploader.setParams(this.projectId, path, datasetType);
    return this.uploader;
  }
  
  @Path("tags")
  public DatasetTagsResource tags() throws ProjectException {
    this.tagsResource.setParams(getProject());
    return this.tagsResource;
  }
}