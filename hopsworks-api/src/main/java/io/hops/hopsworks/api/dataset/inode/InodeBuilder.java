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
import io.hops.hopsworks.api.dataset.tags.InodeTagUri;
import io.hops.hopsworks.api.tags.TagBuilder;
import io.hops.hopsworks.common.dataset.util.DatasetHelper;
import io.hops.hopsworks.common.dataset.util.DatasetPath;
import io.hops.hopsworks.api.util.FilePreviewImageTypes;
import io.hops.hopsworks.common.api.ResourceRequest;
import io.hops.hopsworks.common.dao.AbstractFacade;
import io.hops.hopsworks.common.dao.hdfs.inode.InodeFacade;
import io.hops.hopsworks.common.dao.user.UserFacade;
import io.hops.hopsworks.common.dataset.DatasetController;
import io.hops.hopsworks.common.dataset.FilePreviewMode;
import io.hops.hopsworks.common.util.Settings;
import io.hops.hopsworks.exceptions.DatasetException;
import io.hops.hopsworks.exceptions.MetadataException;
import io.hops.hopsworks.exceptions.SchematizedTagException;
import io.hops.hopsworks.persistence.entity.hdfs.inode.Inode;
import io.hops.hopsworks.persistence.entity.project.Project;
import io.hops.hopsworks.persistence.entity.user.Users;
import io.hops.hopsworks.restutils.RESTCodes;

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
  @EJB
  private TagBuilder tagsBuilder;
  
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
  
  public InodeDTO uri(InodeDTO dto, UriInfo uriInfo, Project project, DatasetPath dsPath) {
    dto.setHref(uriInfo.getBaseUriBuilder()
      .path(ResourceRequest.Name.PROJECT.toString())
      .path(project.getId().toString())
      .path(ResourceRequest.Name.DATASET.toString())
      .path(dsPath.getFullPath().toString())
      .build());
    return dto;
  }
  
  private InodeDTO expand(InodeDTO dto, ResourceRequest resourceRequest) {
    if (resourceRequest != null && resourceRequest.contains(ResourceRequest.Name.INODES)) {
      dto.setExpand(true);
    }
    return dto;
  }
  
  public InodeDTO buildStat(UriInfo uriInfo, ResourceRequest resourceRequest, Users user, DatasetPath datasetPath,
                            Inode inode, Users dirOwner)
    throws DatasetException, SchematizedTagException, MetadataException {
    InodeDTO dto = new InodeDTO();
    uri(dto, uriInfo, inode);
    expand(dto, resourceRequest);
    if (dto.isExpand()) {
      String parentPath = datasetPath.getFullPath().toString();
      dto.setAttributes(inodeAttributeBuilder.build(new InodeAttributeDTO(), resourceRequest, inode, parentPath,
        dirOwner));
      dto.setTags(tagsBuilder.build(new InodeTagUri(uriInfo), resourceRequest, user, datasetPath));
    }
    return dto;
  }

  public InodeDTO buildResource(UriInfo uriInfo, Project project, DatasetPath datasetPath) {
    InodeDTO dto = new InodeDTO();
    uri(dto, uriInfo, project, datasetPath);
    return dto;
  }
  
  /**
   *
   * @param uriInfo
   * @param resourceRequest
   * @param datasetPath
   * @param inode - pass the inode if the datasetPath does not contain it
   * @return
   */
  public InodeDTO buildStat(UriInfo uriInfo, ResourceRequest resourceRequest, Users user, DatasetPath datasetPath,
                            Inode inode)
    throws DatasetException, SchematizedTagException, MetadataException {
    InodeDTO dto = new InodeDTO();
    uri(dto, uriInfo);
    expand(dto, resourceRequest);
    if (dto.isExpand()) {
      dto.setAttributes(inodeAttributeBuilder.build(new InodeAttributeDTO(), resourceRequest, inode, null, null));
      dto.setZipState(settings.getZipState(dto.getAttributes().getPath()));
      dto.setTags(tagsBuilder.build(new InodeTagUri(uriInfo), resourceRequest, user, datasetPath));
    }
    return dto;
  }
  
  public InodeDTO buildBlob(UriInfo uriInfo, ResourceRequest resourceRequest, Users user, DatasetPath datasetPath,
                            Inode inode, FilePreviewMode mode)
    throws DatasetException, SchematizedTagException, MetadataException {
    InodeDTO dto = new InodeDTO();
    uri(dto, uriInfo);
    expand(dto, resourceRequest);
    if (dto.isExpand()) {
      dto.setAttributes(inodeAttributeBuilder.build(new InodeAttributeDTO(), resourceRequest, inode, null, null));
      dto.setTags(tagsBuilder.build(new InodeTagUri(uriInfo), resourceRequest, user, datasetPath));
    }
    List<String> ext = Stream.of(FilePreviewImageTypes.values())
      .map(FilePreviewImageTypes::name)
      .collect(Collectors.toList());
    dto.setPreview(
      datasetController.filePreview(datasetPath.getAccessProject(), user, datasetPath.getFullPath(), mode, ext));
    return dto;
  }
  
  /**
   *
   * @param uriInfo
   * @param resourceRequest
   * @param datasetPath
   * @return
   */
  public InodeDTO buildStat(UriInfo uriInfo, ResourceRequest resourceRequest, Users user, DatasetPath datasetPath)
    throws DatasetException, SchematizedTagException, MetadataException {
    Inode inode = datasetPath.getInode();
    if (inode == null) {
      throw new DatasetException(RESTCodes.DatasetErrorCode.PATH_NOT_FOUND, Level.FINE);
    }
    return buildStat(uriInfo, resourceRequest, user, datasetPath, datasetPath.getInode());
  }
  
  /**
   *
   * @param uriInfo
   * @param resourceRequest
   * @param user
   * @param datasetPath
   * @param mode
   * @return
   */
  public InodeDTO buildBlob(UriInfo uriInfo, ResourceRequest resourceRequest, Users user, DatasetPath datasetPath,
                            FilePreviewMode mode)
    throws DatasetException, SchematizedTagException, MetadataException {
    Inode inode = datasetPath.getInode();
    if (inode == null) {
      throw new DatasetException(RESTCodes.DatasetErrorCode.PATH_NOT_FOUND, Level.FINE);
    }
    if (inode.isDir()) {
      throw new DatasetException(RESTCodes.DatasetErrorCode.INVALID_PATH_FILE, Level.FINE);
    }
    return buildBlob(uriInfo, resourceRequest, user, datasetPath, inode, mode);
  }
  
  /**
   * Build a list of Inodes under the given path
   * @param uriInfo
   * @param resourceRequest
   * @param datasetPath
   * @return
   */
  public InodeDTO buildItems(UriInfo uriInfo, ResourceRequest resourceRequest, Users user, DatasetPath datasetPath)
    throws DatasetException, SchematizedTagException, MetadataException {
    Inode parent = datasetPath.getInode();
    if (parent == null) {
      throw new DatasetException(RESTCodes.DatasetErrorCode.PATH_NOT_FOUND, Level.FINE);
    }
    if (!parent.isDir()) {
      throw new DatasetException(RESTCodes.DatasetErrorCode.INVALID_PATH_DIR, Level.FINE);
    }
    return build(uriInfo, new InodeDTO(), resourceRequest, user, datasetPath, parent);
  }
  
  private InodeDTO build(UriInfo uriInfo, InodeDTO dto, ResourceRequest resourceRequest, Users user,
                         DatasetPath datasetPath, Inode parent)
    throws DatasetException, SchematizedTagException, MetadataException {
    uri(dto, uriInfo);
    Users dirOwner = userFacade.findByUsername(parent.getHdfsUser().getUsername());
    datasetHelper.checkResourceRequestLimit(resourceRequest, parent.getChildrenNum());
    InodeDTO inodeDTO = items(uriInfo, dto, resourceRequest, user, datasetPath, parent, dirOwner);
    return inodeDTO;
  }
  
  private InodeDTO items(UriInfo uriInfo, InodeDTO dto, ResourceRequest resourceRequest, Users user,
                         DatasetPath datasetPath, Inode parent, Users dirOwner)
    throws DatasetException, SchematizedTagException, MetadataException {
    expand(dto, resourceRequest);
    AbstractFacade.CollectionInfo collectionInfo;
    if (dto.isExpand()) {
      collectionInfo = inodeFacade.findByParent(resourceRequest.getOffset(), resourceRequest.getLimit(),
        resourceRequest.getFilter(), resourceRequest.getSort(), parent, datasetPath.getAccessProject());
      items(uriInfo, dto, resourceRequest, user, datasetPath, collectionInfo.getItems(), dirOwner);
      dto.setCount(collectionInfo.getCount());
    }
    return dto;
  }
  
  private InodeDTO items(UriInfo uriInfo, InodeDTO dto, ResourceRequest resourceRequest, Users user,
                         DatasetPath datasetPath, List<Inode> inodes, Users dirOwner)
    throws DatasetException, SchematizedTagException, MetadataException {
    if (inodes != null && !inodes.isEmpty()) {
      for(Inode inode : inodes) {
        dto.addItem(buildStat(uriInfo, resourceRequest, user, datasetPath, inode, dirOwner));
      }
    } else if (inodes != null && inodes.isEmpty()) {
      dto.setItems(new ArrayList<>());
    }
    return dto;
  }
  
}