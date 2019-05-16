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

import io.hops.hopsworks.api.dataset.inode.attribute.InodeAttributeBuilder;
import io.hops.hopsworks.api.dataset.inode.attribute.InodeAttributeDTO;
import io.hops.hopsworks.api.dataset.util.DatasetHelper;
import io.hops.hopsworks.common.api.ResourceRequest;
import io.hops.hopsworks.common.dao.AbstractFacade;
import io.hops.hopsworks.common.dao.dataset.Dataset;
import io.hops.hopsworks.common.dao.dataset.DatasetFacade;
import io.hops.hopsworks.common.dao.dataset.DatasetPermissions;
import io.hops.hopsworks.common.dao.dataset.DatasetSharedWith;
import io.hops.hopsworks.common.dao.dataset.DatasetSharedWithFacade;
import io.hops.hopsworks.common.dao.dataset.DatasetType;
import io.hops.hopsworks.common.dao.hdfs.inode.Inode;
import io.hops.hopsworks.common.dao.project.Project;
import io.hops.hopsworks.common.dao.user.UserFacade;
import io.hops.hopsworks.common.dao.user.Users;
import io.hops.hopsworks.common.dataset.DatasetController;
import io.hops.hopsworks.common.hdfs.inode.InodeController;
import io.hops.hopsworks.common.util.Settings;
import io.hops.hopsworks.exceptions.DatasetException;
import io.hops.hopsworks.restutils.RESTCodes;
import org.apache.hadoop.fs.permission.FsPermission;

import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.ws.rs.core.UriInfo;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

@Stateless
@TransactionAttribute(TransactionAttributeType.NEVER)
public class DatasetBuilder {
  
  private static final Logger LOGGER = Logger.getLogger(DatasetBuilder.class.getName());
  
  @EJB
  private DatasetFacade datasetFacade;
  @EJB
  private DatasetSharedWithFacade datasetSharedWithFacade;
  @EJB
  private DatasetController datasetController;
  @EJB
  private InodeAttributeBuilder inodeAttributeBuilder;
  @EJB
  private InodeController inodeController;
  @EJB
  private UserFacade userFacade;
  @EJB
  private DatasetHelper datasetHelper;
  
  private DatasetDTO uri(DatasetDTO dto, UriInfo uriInfo) {
    dto.setHref(uriInfo.getAbsolutePathBuilder()
      .build());
    return dto;
  }
  
  private DatasetDTO uri(DatasetDTO dto, UriInfo uriInfo, Dataset dataset) {
    dto.setHref(uriInfo.getAbsolutePathBuilder()
      .path(dataset.getId().toString())
      .build());
    return dto;
  }
  
  private DatasetDTO uriItems(DatasetDTO dto, UriInfo uriInfo, Project project, Dataset dataset) {
    dto.setHref(uriInfo.getBaseUriBuilder()
      .path(ResourceRequest.Name.PROJECT.toString())
      .path(project.getId().toString())
      .path(ResourceRequest.Name.DATASETS.toString())
      .path(dataset.getName())
      .build());
    return dto;
  }
  
  public DatasetDTO expand(DatasetDTO dto, ResourceRequest resourceRequest) {
    if (resourceRequest != null && resourceRequest.contains(ResourceRequest.Name.DATASETS)) {
      dto.setExpand(true);
    }
    return dto;
  }
  
  private DatasetDTO build(DatasetDTO dto, ResourceRequest resourceRequest, Dataset dataset, String parentPath,
    Users dirOwner, Project project) {
    expand(dto, resourceRequest);
    if (dto.isExpand()) {
      dto.setId(dataset.getId());
      dto.setDescription(dataset.getDescription());
      dto.setPublicDataset(dataset.getPublicDs());
      dto.setPublicId(dataset.getPublicDsId());
      dto.setSearchable(dataset.isSearchable());
      dto.setDatasetType(dataset.getDsType());
      dto.setShared(dataset.isShared(project));
      String permission = FsPermission.createImmutable(dataset.getInode().getPermission()).toString();
      dto.setPermission(DatasetPermissions.fromPermissionString(permission));
      dto.setAccepted(true);
      dto.setSharedWith(dataset.getDatasetSharedWithCollection().size());
      if (dto.isShared()) {
        DatasetSharedWith sharedWith = datasetSharedWithFacade.findByProjectAndDataset(project, dataset);
        if (sharedWith != null) {
          dto.setAccepted(sharedWith.getAccepted());
        }
        dto.setName(dataset.getProject().getName() + Settings.SHARED_FILE_SEPARATOR + dataset.getName());
        dto.setAttributes(inodeAttributeBuilder.build(new InodeAttributeDTO(), resourceRequest, dataset.getInode(),
          null, null));//if shared parent and owner not this project
      } else if (DatasetType.DATASET.equals(dataset.getDsType())) {
        dto.setName(dataset.getName());
        dto.setAttributes(inodeAttributeBuilder.build(new InodeAttributeDTO(), resourceRequest, dataset.getInode(),
          parentPath, dirOwner));
      } else {
        dto.setName(dataset.getName());
        dto.setAttributes(inodeAttributeBuilder.build(new InodeAttributeDTO(), resourceRequest, dataset.getInode(),
          null, dirOwner));
      }
    }
    return dto;
  }
  
  public DatasetDTO build(UriInfo uriInfo, ResourceRequest resourceRequest, Project project, Dataset dataset,
    String parentPath, Users dirOwner, boolean expandSharedWith) {
    DatasetDTO dto = new DatasetDTO();
    uri(dto, uriInfo);
    build(dto, resourceRequest, dataset, parentPath, dirOwner, project);
    //will be changed to expand when project is done.
    if (expandSharedWith) {
      List<DatasetSharedWith> dsSharedWith = datasetSharedWithFacade.findByDataset(dataset);
      List<ProjectDTO> projectDTOS = new ArrayList<>();
      for(DatasetSharedWith datasetSharedWith : dsSharedWith) {
        projectDTOS.add(new ProjectDTO(datasetSharedWith.getProject()));
      }
      dto.setProjectsSharedWith(projectDTOS);
    }
    return dto;
  }
  
  public DatasetDTO buildItems(UriInfo uriInfo, ResourceRequest resourceRequest, Project project, Dataset dataset,
    String parentPath, Users dirOwner) {
    DatasetDTO dto = new DatasetDTO();
    uriItems(dto, uriInfo, project, dataset);
    return build(dto, resourceRequest, dataset, parentPath, dirOwner, project);
  }
  
  /**
   * Build a single Dataset
   *
   * @param uriInfo
   * @param resourceRequest
   * @param name
   * @return
   */
  public DatasetDTO build(UriInfo uriInfo, ResourceRequest resourceRequest, Project project, String name)
    throws DatasetException {
    Dataset dataset = datasetController.getByProjectAndDsName(project, null, name);
    if (dataset == null) {
      throw new DatasetException(RESTCodes.DatasetErrorCode.DATASET_NOT_FOUND, Level.FINE);
    }
    return build(uriInfo, resourceRequest, project, dataset, null, null, false);
  }
  
  /**
   * Build a list of Datasets
   *
   * @param uriInfo
   * @param resourceRequest
   * @param project
   * @return
   */
  public DatasetDTO buildItems(UriInfo uriInfo, ResourceRequest resourceRequest,
    ResourceRequest sharedDatasetResourceRequest, Project project) {
    Inode parent = project.getInode();
    datasetHelper.checkResourceRequestLimit(resourceRequest, parent.getChildrenNum());
    String parentPath = inodeController.getPath(parent);
    Users dirOwner = userFacade.findByUsername(parent.getHdfsUser().getUsername());
    return items(new DatasetDTO(), uriInfo, resourceRequest, sharedDatasetResourceRequest, project, parentPath,
      dirOwner);
  }
  
  private DatasetDTO items(DatasetDTO dto, UriInfo uriInfo, ResourceRequest resourceRequest,
    ResourceRequest sharedDatasetResourceRequest, Project project, String parentPath, Users dirOwner) {
    uri(dto, uriInfo);
    expand(dto, resourceRequest);
    if (dto.isExpand()) {
      DatasetDTO ownedDatasets = ownItems(new DatasetDTO(), uriInfo, resourceRequest, project, parentPath, dirOwner);
      DatasetDTO sharedDatasets =
        sharedItems(new DatasetDTO(), uriInfo, sharedDatasetResourceRequest, project, parentPath, dirOwner);
      return mergeAndApplyOffsetAndLimit(dto, resourceRequest, ownedDatasets, sharedDatasets);
    }
    return dto;
  }
  
  // datasets in the project
  private DatasetDTO ownItems(DatasetDTO dto, UriInfo uriInfo, ResourceRequest resourceRequest, Project project,
    String parentPath, Users dirOwner) {
    AbstractFacade.CollectionInfo collectionInfo = datasetFacade.findAllDatasetByProject(null, null,
      resourceRequest.getFilter(), resourceRequest.getSort(), project);
    dto.setCount(collectionInfo.getCount());
    return datasetItems(dto, uriInfo, resourceRequest, collectionInfo.getItems(), project, parentPath, dirOwner);
  }
  
  // shared datasets
  private DatasetDTO sharedItems(DatasetDTO dto, UriInfo uriInfo, ResourceRequest resourceRequest, Project project,
    String parentPath, Users dirOwner) {
    AbstractFacade.CollectionInfo collectionInfo = datasetSharedWithFacade.findAllDatasetByProject(null, null,
      resourceRequest.getFilter(), resourceRequest.getSort(), project);
    dto.setCount(collectionInfo.getCount());
    return datasetSharedWithItems(dto, uriInfo, resourceRequest, collectionInfo.getItems(), project, parentPath,
      dirOwner);
  }
  
  // create dto from a list of dataset
  private DatasetDTO datasetItems(DatasetDTO dto, UriInfo uriInfo, ResourceRequest resourceRequest,
    List<Dataset> datasets, Project project, String parentPath, Users dirOwner) {
    if (datasets != null && !datasets.isEmpty()) {
      datasets.forEach((dataset) -> dto.addItem(buildItems(uriInfo, resourceRequest, project, dataset, parentPath,
        dirOwner)));
    }
    return dto;
  }
  
  // create dto from a list of DatasetSharedWith objects
  private DatasetDTO datasetSharedWithItems(DatasetDTO dto, UriInfo uriInfo, ResourceRequest resourceRequest,
    List<DatasetSharedWith> datasetSharedWithList, Project project, String parentPath, Users dirOwner) {
    if (datasetSharedWithList != null && !datasetSharedWithList.isEmpty()) {
      datasetSharedWithList.forEach((datasetSharedWith) -> dto.addItem(buildItems(uriInfo, resourceRequest, project,
        datasetSharedWith.getDataset(), parentPath,
        dirOwner)));
    }
    return dto;
  }
  
  public DatasetDTOComparator getComparator(ResourceRequest property) {
    Set<DatasetFacade.SortBy> sortBy = (Set<DatasetFacade.SortBy>) property.getSort();
    if (property.getSort() != null && !property.getSort().isEmpty()) {
      return new DatasetDTOComparator(sortBy);
    }
    return null;
  }
  
  class DatasetDTOComparator implements Comparator<DatasetDTO> {
    
    Set<DatasetFacade.SortBy> sortBy;
    
    public DatasetDTOComparator(Set<DatasetFacade.SortBy> sortBy) {
      this.sortBy = sortBy;
    }
    
    private int compare(DatasetDTO a, DatasetDTO b, DatasetFacade.SortBy sortBy) {
      switch (DatasetFacade.Sorts.valueOf(sortBy.getValue())) {
        case ID:
          return order(a.getId(), b.getId(), sortBy.getParam());
        case NAME:
          return order(a.getName(), b.getName(), sortBy.getParam());
        case PUBLIC:
          return order(a.getPublicDataset(), b.getPublicDataset(), sortBy.getParam());
        case SIZE:
          return order(a.getAttributes().getSize(), b.getAttributes().getSize(), sortBy.getParam());
        case TYPE:
          return order(a.getDatasetType().name(), b.getDatasetType().name(), sortBy.getParam());
        case SEARCHABLE:
          return order(a.isSearchable(), b.isSearchable(), sortBy.getParam());
        case MODIFICATION_TIME:
          return order(a.getAttributes().getModificationTime(), b.getAttributes().getModificationTime(),
            sortBy.getParam());
        case ACCESS_TIME:
          return order(a.getAttributes().getAccessTime(), b.getAttributes().getAccessTime(), sortBy.getParam());
        default:
          throw new UnsupportedOperationException("Sort By " + sortBy + " not supported");
      }
    }
    
    private int order(String a, String b, AbstractFacade.OrderBy orderBy) {
      switch (orderBy) {
        case ASC:
          return String.CASE_INSENSITIVE_ORDER.compare(a, b);
        case DESC:
          return String.CASE_INSENSITIVE_ORDER.compare(b, a);
        default:
          throw new UnsupportedOperationException("Order By " + orderBy + " not supported");
      }
    }
    
    private int order(Boolean a, Boolean b, AbstractFacade.OrderBy orderBy) {
      switch (orderBy) {
        case ASC:
          return a.compareTo(b);
        case DESC:
          return b.compareTo(a);
        default:
          throw new UnsupportedOperationException("Order By " + orderBy + " not supported");
      }
    }
    
    private int order(Integer a, Integer b, AbstractFacade.OrderBy orderBy) {
      switch (orderBy) {
        case ASC:
          return a.compareTo(b);
        case DESC:
          return b.compareTo(a);
        default:
          throw new UnsupportedOperationException("Order By " + orderBy + " not supported");
      }
    }
    
    private int order(Long a, Long b, AbstractFacade.OrderBy orderBy) {
      switch (orderBy) {
        case ASC:
          return a.compareTo(b);
        case DESC:
          return b.compareTo(a);
        default:
          throw new UnsupportedOperationException("Order By " + orderBy + " not supported");
      }
    }
    
    private int order(Date a, Date b, AbstractFacade.OrderBy orderBy) {
      switch (orderBy) {
        case ASC:
          return a.compareTo(b);
        case DESC:
          return b.compareTo(a);
        default:
          throw new UnsupportedOperationException("Order By " + orderBy + " not supported");
      }
    }
    
    @Override
    public int compare(DatasetDTO a, DatasetDTO b) {
      Iterator<DatasetFacade.SortBy> sort = sortBy.iterator();
      int c = compare(a, b, sort.next());
      for (; sort.hasNext() && c == 0; ) {
        c = compare(a, b, sort.next());
      }
      return c;
    }
  }
  
  private DatasetDTO mergeAndApplyOffsetAndLimit(DatasetDTO dto, ResourceRequest resourceRequest, DatasetDTO dto1,
    DatasetDTO dto2) {
    if ((dto1 == null || dto1.getItems() == null || dto1.getItems().isEmpty()) &&
      (dto2 == null || dto2.getItems() == null || dto2.getItems().isEmpty())) {
      return dto;
    } else if (dto1 == null || dto1.getItems() == null || dto1.getItems().isEmpty()) {
      applyOffsetAndLimit(dto, dto2.getItems(), resourceRequest.getOffset(), resourceRequest.getLimit());
    } else if (dto2 == null || dto2.getItems() == null || dto2.getItems().isEmpty()) {
      applyOffsetAndLimit(dto, dto1.getItems(), resourceRequest.getOffset(), resourceRequest.getLimit());
    } else {
      applyOffsetAndLimit(dto, merge(dto1.getItems(), dto2.getItems(), resourceRequest), resourceRequest.getOffset(),
        resourceRequest.getLimit());
    }
    return dto;
  }
  
  private List<DatasetDTO> merge(List<DatasetDTO> items1, List<DatasetDTO> items2, ResourceRequest resourceRequest) {
    DatasetDTOComparator comparator = getComparator(resourceRequest);
    List<DatasetDTO> items = new ArrayList<>();
    if (comparator == null) {
      items.addAll(items1);
      items.addAll(items2);
    } else {
      int i = 0, j = 0;
      while (i < items1.size() && j < items2.size()) {
        if (comparator.compare(items1.get(i), items2.get(j)) < 0) {
          items.add(items1.get(i));
          i++;
        } else if (comparator.compare(items1.get(i), items2.get(j)) > 0) {
          items.add(items2.get(j));
          j++;
        } else {
          items.add(items1.get(i));
          items.add(items2.get(j));
          i++;
          j++;
        }
      }
      if (i < items1.size()) {
        items.addAll(items1.subList(i, items1.size()));
      }
      if (j < items2.size()) {
        items.addAll(items2.subList(j, items2.size()));
      }
    }
    return items;
  }
  
  private void applyOffsetAndLimit(DatasetDTO dto, List<DatasetDTO> items, Integer offset, Integer limit) {
    if (offset == null || offset < 0) {
      offset = 0;
    }
    if (limit == null || limit <= 0) {
      limit = items.size();
    }
    List<DatasetDTO> subItems = items.stream().skip(offset).limit(limit).collect(Collectors.toList());
    dto.setItems(subItems);
    dto.setCount((long)items.size());
  }
}