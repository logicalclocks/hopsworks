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
package io.hops.hopsworks.api.dataset.inode.attribute;

import io.hops.hopsworks.common.api.ResourceRequest;
import io.hops.hopsworks.common.dao.hdfs.inode.Inode;
import io.hops.hopsworks.common.dao.hdfsUser.HdfsUsers;
import io.hops.hopsworks.common.dao.user.UserFacade;
import io.hops.hopsworks.common.dao.user.Users;
import io.hops.hopsworks.common.hdfs.inode.InodeController;
import org.apache.hadoop.fs.permission.FsPermission;

import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import java.io.File;
import java.util.Date;
import java.util.logging.Logger;

@Stateless
@TransactionAttribute(TransactionAttributeType.NEVER)
public class InodeAttributeBuilder {
  
  private static final Logger LOGGER = Logger.getLogger(InodeAttributeBuilder.class.getName());
  
  @EJB
  private InodeController inodeController;
  @EJB
  private UserFacade userFacade;
  
  public boolean expand(ResourceRequest resourceRequest) {
    if (resourceRequest != null && resourceRequest.contains(ResourceRequest.Name.INODES)) {
      return true;
    }
    return false;
  }
  
  private String getFullName(Users user, String hdfsUserName) {
    if (user == null) {
      return hdfsUserName;
    }
    String firstName = user.getFname() == null ? "" : user.getFname();
    String lastName = user.getLname() == null ? "" : user.getLname();
    if (firstName.isEmpty() && lastName.isEmpty()) {
      return user.getUsername();
    }
    return firstName + " " + lastName;
  }
  
  private String getUserName(HdfsUsers hdfsUser, Users dirOwner) {
    if (dirOwner != null && hdfsUser.getUsername().equals(dirOwner.getUsername())) {
      return getFullName(dirOwner, hdfsUser.getUsername());
    }
    //Most files in a dir will be owned by the dir owner, so this should not be called that often
    Users user = userFacade.findByUsername(hdfsUser.getUsername());
    return getFullName(user, hdfsUser.getUsername());
  }
  
  public InodeAttributeDTO build(InodeAttributeDTO dto, ResourceRequest resourceRequest, Inode inode,
    String parentPath, Users dirOwner) {
    if (expand(resourceRequest)) {
      dto.setId(inode.getId());
      dto.setName(inode.getInodePK().getName());
      dto.setAccessTime(new Date(inode.getAccessTime().longValue()));
      dto.setModificationTime(new Date(inode.getModificationTime().longValue()));
      dto.setDir(inode.isDir());
      dto.setOwner(getUserName(inode.getHdfsUser(), dirOwner));
      dto.setGroup(inode.getHdfsGroup().getName());
      dto.setParentId(inode.getInodePK().getParentId());
      String path = parentPath != null ? parentPath + File.separator + inode.getInodePK().getName() :
        inodeController.getPath(inode);
      dto.setPath(path);
      dto.setUnderConstruction(inode.isUnderConstruction());
      dto.setPermission(FsPermission.createImmutable(inode.getPermission()).toString());
      dto.setSize(inode.getSize());
    }
    return dto;
  }
  
}