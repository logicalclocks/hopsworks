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
package io.hops.hopsworks.api.dataset.inode;

import io.hops.hopsworks.api.dataset.inode.attribute.InodeAttributeBuilder;
import io.hops.hopsworks.api.dataset.inode.attribute.InodeAttributeDTO;
import io.hops.hopsworks.api.dataset.util.DatasetHelper;
import io.hops.hopsworks.api.dataset.util.DatasetPath;
import io.hops.hopsworks.api.util.FilePreviewImageTypes;
import io.hops.hopsworks.common.api.ResourceRequest;
import io.hops.hopsworks.common.dao.AbstractFacade;
import io.hops.hopsworks.common.dao.hdfs.inode.Inode;
import io.hops.hopsworks.common.dao.hdfs.inode.InodeFacade;
import io.hops.hopsworks.common.dao.project.Project;
import io.hops.hopsworks.common.dao.user.UserFacade;
import io.hops.hopsworks.common.dao.user.Users;
import io.hops.hopsworks.common.dataset.DatasetController;
import io.hops.hopsworks.common.dataset.FilePreviewMode;
import io.hops.hopsworks.common.util.Settings;
import io.hops.hopsworks.exceptions.DatasetException;
import io.hops.hopsworks.restutils.RESTCodes;
import org.apache.hadoop.fs.Path;

import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.ws.rs.core.UriInfo;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Stateless
@TransactionAttribute(TransactionAttributeType.NEVER)
public class InodeBuilder {
  
  private static final Logger LOGGER = Logger.getLogger(InodeBuilder.class.getName());
  
  @EJB
  private InodeFacade inodeFacade;
  @EJB
  private UserFacade userFacade;
  @EJB
  private InodeAttributeBuilder inodeAttributeBuilder;
  @EJB
  private DatasetController datasetController;
  @EJB
  private DatasetHelper datasetHelper;
  @EJB
  private Settings settings;
  
  private InodeDTO uri(InodeDTO dto, UriInfo uriInfo) {
    dto.setHref(uriInfo.getAbsolutePathBuilder().build());
    return dto;
  }
  
  private InodeDTO uri(InodeDTO dto, UriInfo uriInfo, Inode inode) {
    dto.setHref(uriInfo.getAbsolutePathBuilder()
      .path(inode.getInodePK().getName())
      .build());
    return dto;
  }
  
  private InodeDTO uriItems(InodeDTO dto, UriInfo uriInfo, Project project) {
    dto.setHref(uriInfo.getBaseUriBuilder()
      .path(ResourceRequest.Name.PROJECT.toString())
      .path(project.getId().toString())
      .path(ResourceRequest.Name.DATASETS.toString())
      .path(dto.getAttributes().getPath())
      .build());
    return dto;
  }
  
  private InodeDTO expand(InodeDTO dto, ResourceRequest resourceRequest) {
    if (resourceRequest != null && resourceRequest.contains(ResourceRequest.Name.INODES)) {
      dto.setExpand(true);
    }
    return dto;
  }
  
  public InodeDTO buildStat(UriInfo uriInfo, ResourceRequest resourceRequest, Inode inode, String parentPath,
    Users dirOwner) {
    InodeDTO dto = new InodeDTO();
    uri(dto, uriInfo, inode);
    expand(dto, resourceRequest);
    if (dto.isExpand()) {
      dto.setAttributes(inodeAttributeBuilder.build(new InodeAttributeDTO(), resourceRequest, inode, parentPath,
        dirOwner));
    }
    return dto;
  }
  
  public InodeDTO buildStat(UriInfo uriInfo, ResourceRequest resourceRequest, Inode inode) {
    InodeDTO dto = new InodeDTO();
    uri(dto, uriInfo);
    expand(dto, resourceRequest);
    if (dto.isExpand()) {
      dto.setAttributes(inodeAttributeBuilder.build(new InodeAttributeDTO(), resourceRequest, inode, null, null));
      dto.setZipState(settings.getZipState(dto.getAttributes().getPath()));
    }
    return dto;
  }
  
  private InodeDTO buildBlob(UriInfo uriInfo, ResourceRequest resourceRequest, Inode inode, Project project,
    Users user, Path fullPath, FilePreviewMode mode) throws DatasetException {
    InodeDTO dto = new InodeDTO();
    uri(dto, uriInfo);
    expand(dto, resourceRequest);
    if (dto.isExpand()) {
      dto.setAttributes(inodeAttributeBuilder.build(new InodeAttributeDTO(), resourceRequest, inode, null, null));
    }
    List<String> ext = Stream.of(FilePreviewImageTypes.values())
      .map(FilePreviewImageTypes::name)
      .collect(Collectors.toList());
    dto.setPreview(datasetController.filePreview(project, user, fullPath, mode, ext));
    return dto;
  }
  
  /**
   *
   * @param uriInfo
   * @param resourceRequest
   * @param datasetPath
   * @return
   */
  public InodeDTO buildStat(UriInfo uriInfo, ResourceRequest resourceRequest, DatasetPath datasetPath)
    throws DatasetException {
    Inode inode = datasetPath.getInode();
    if (inode == null) {
      throw new DatasetException(RESTCodes.DatasetErrorCode.PATH_NOT_FOUND, Level.FINE);
    }
    return buildStat(uriInfo, resourceRequest, inode);
  }
  
  /**
   *
   * @param uriInfo
   * @param resourceRequest
   * @param datasetPath
   * @return
   */
  public InodeDTO buildBlob(UriInfo uriInfo, ResourceRequest resourceRequest, Project project, Users user,
    DatasetPath datasetPath, FilePreviewMode mode) throws DatasetException {
    Inode inode = datasetPath.getInode();
    if (inode == null) {
      throw new DatasetException(RESTCodes.DatasetErrorCode.PATH_NOT_FOUND, Level.FINE);
    }
    if (inode.isDir()) {
      throw new DatasetException(RESTCodes.DatasetErrorCode.INVALID_PATH_FILE, Level.FINE);
    }
    return buildBlob(uriInfo, resourceRequest, inode, project, user, datasetPath.getFullPath(), mode);
  }
  
  /**
   * Build a list of Inodes under the given path
   * @param uriInfo
   * @param resourceRequest
   * @param project used for filtering by user email and if no path is given.
   * @param datasetPath
   * @return
   */
  public InodeDTO buildItems(UriInfo uriInfo, ResourceRequest resourceRequest, Project project, DatasetPath datasetPath)
    throws DatasetException {
    Inode parent = datasetPath.getInode();
    if (parent == null) {
      throw new DatasetException(RESTCodes.DatasetErrorCode.PATH_NOT_FOUND, Level.FINE);
    }
    if (!parent.isDir()) {
      throw new DatasetException(RESTCodes.DatasetErrorCode.INVALID_PATH_DIR, Level.FINE);
    }
    return build(uriInfo, new InodeDTO(), resourceRequest, parent, datasetPath.getFullPath().toString(), project);
  }
  
  private InodeDTO build(UriInfo uriInfo, InodeDTO dto, ResourceRequest resourceRequest, Inode parent, String parentPath
    , Project project) {
    uri(dto, uriInfo);
    Users dirOwner = userFacade.findByUsername(parent.getHdfsUser().getUsername());
    datasetHelper.checkResourceRequestLimit(resourceRequest, parent.getChildrenNum());
    InodeDTO inodeDTO = items(uriInfo, dto, resourceRequest, parent, parentPath, dirOwner, project);
    return inodeDTO;
  }
  
  private InodeDTO items(UriInfo uriInfo, InodeDTO dto, ResourceRequest resourceRequest, Inode parent, String parentPath
    , Users dirOwner, Project project) {
    expand(dto, resourceRequest);
    AbstractFacade.CollectionInfo collectionInfo;
    if (dto.isExpand()) {
      collectionInfo = inodeFacade.findByParent(resourceRequest.getOffset(), resourceRequest.getLimit(),
        resourceRequest.getFilter(), resourceRequest.getSort(), parent, project);
      items(uriInfo, dto, resourceRequest, collectionInfo.getItems(), parentPath, dirOwner);
      dto.setCount(collectionInfo.getCount());
    }
    return dto;
  }
  
  private InodeDTO items(UriInfo uriInfo, InodeDTO dto, ResourceRequest resourceRequest, List<Inode> inodes,
    String parentPath, Users dirOwner) {
    if (inodes != null && !inodes.isEmpty()) {
      inodes.forEach((inode) -> dto.addItem(buildStat(uriInfo, resourceRequest, inode, parentPath, dirOwner)));
    } else if (inodes != null && inodes.isEmpty()) {
      dto.setItems(new ArrayList<>());
    }
    return dto;
  }
  
}