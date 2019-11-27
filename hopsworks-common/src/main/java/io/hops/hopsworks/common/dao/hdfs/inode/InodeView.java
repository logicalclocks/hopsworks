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

package io.hops.hopsworks.common.dao.hdfs.inode;

import io.hops.hopsworks.common.dao.dataset.Dataset;
import io.hops.hopsworks.common.dao.dataset.DatasetSharedWith;
import io.hops.hopsworks.common.dao.dataset.DatasetType;
import io.hops.hopsworks.common.dao.dataset.SharedState;
import io.hops.hopsworks.common.util.Settings;
import org.apache.hadoop.fs.permission.FsPermission;

import javax.xml.bind.annotation.XmlRootElement;
import java.util.Date;
import java.util.Objects;

/**
 * Simplified version of the Inode entity to allow for easier access through web
 * interface.
 */
@XmlRootElement
public final class InodeView {

  private String name;
  private boolean dir;
  private boolean parent;
  private String path;
  private long size;
  private boolean shared;
  private String owningProjectName;
  private Date modification;
  private Date accessTime;
  private Long id;
  private Long parentId;
  private int template;
  private String description;
  private boolean status = true;
  private boolean underConstruction;
  private String owner;
  private String permission;
  private String email;
  private int publicDs = 0;
  private int sharedWith = 0;
  private boolean searchable = false;
  // FSM states: STAGING, ZIPPING, UNZIPPING, UPLOADING, CHOWNING, SUCCESS, FAILED
  private String zipState = "NONE";
  private String publicId;
  private DatasetType type;

  public InodeView() {
  }

  /**
   * Constructor for sub folders
   * <p/>
   * @param i
   * @param path
   */
  public InodeView(Inode i, String path) {
    this.name = i.getInodePK().getName();
    this.dir = i.isDir();
    this.id = i.getId();
    this.parentId = i.getInodePK().getParentId();
    this.size = i.getSize();
    //put the template id in the REST response
    this.template = i.getTemplate();
    this.underConstruction = i.isUnderConstruction();
    this.parent = false;
    this.path = path;
    this.modification = new Date(i.getModificationTime().longValue());
    this.accessTime = new Date(i.getAccessTime().longValue());
    // if it is a sub folder the status should be default true
    // this is used in the front-end to tell apart accepted and pending shared 
    // top level datasets. 
    this.status = true;
    this.owner = i.getHdfsUser().getUsername();
    this.permission = FsPermission.createImmutable(i.getPermission()).toString();
  }

  /**
   * Constructor for top level datasets.
   * <p/>
   * @param parent
   * @param ds
   * @param path
   */
  public InodeView(Inode parent, Dataset ds, String path) {
    this.name = ds.getInode().getInodePK().getName();
    this.parentId = parent.getId();
    this.dir = ds.getInode().isDir();
    this.id = ds.getInode().getId();
    this.size = ds.getInode().getSize();
    this.template = ds.getInode().getTemplate();
    this.underConstruction = ds.getInode().isUnderConstruction();
    this.publicId = ds.getPublicDsId();
    this.parent = false;
    this.path = path;
    this.modification
      = new Date(ds.getInode().getModificationTime().longValue());
    this.accessTime = new Date(ds.getInode().getAccessTime().longValue());
    this.shared = false;
    this.owningProjectName = parent.inodePK.getName();
    this.type = ds.getDsType();
    this.description = ds.getDescription();
    this.status = true;
    if (ds.getInode().getHdfsUser() != null) {
      this.owner = ds.getInode().getHdfsUser().getUsername();
    } else {
      this.owner = "";
    }
    this.permission = FsPermission.
      createImmutable(ds.getInode().getPermission()).toString();
    this.publicDs = ds.getPublicDs();
    this.searchable = ds.isSearchable();
  }
  
  public InodeView(Inode parent, DatasetSharedWith ds, String path) {
    this.name = ds.getDataset().getInode().getInodePK().getName();
    this.parentId = parent.getId();
    this.dir = ds.getDataset().getInode().isDir();
    this.id = ds.getDataset().getInode().getId();
    this.size = ds.getDataset().getInode().getSize();
    this.template = ds.getDataset().getInode().getTemplate();
    this.underConstruction = ds.getDataset().getInode().isUnderConstruction();
    this.publicId = ds.getDataset().getPublicDsId();
    this.parent = false;
    this.path = path;
    this.modification
      = new Date(ds.getDataset().getInode().getModificationTime().longValue());
    this.accessTime = new Date(ds.getDataset().getInode().getAccessTime().longValue());
    this.shared = true;
    this.owningProjectName = parent.inodePK.getName();
    this.type = ds.getDataset().getDsType();
    if (this.shared) {
      switch (ds.getDataset().getDsType()) {
        case DATASET:
          this.owningProjectName = parent.getInodePK().getName();
          break;
        case HIVEDB:
          this.owningProjectName = this.name.substring(0, this.name.lastIndexOf("."));
          break;
        case FEATURESTORE:
          this.owningProjectName = this.name.substring(0, this.name.lastIndexOf("_"));
      }
      this.name = this.owningProjectName + Settings.SHARED_FILE_SEPARATOR + this.name;
    }
    this.description = ds.getDataset().getDescription();
    this.status = ds.getAccepted();
    if (ds.getDataset().getInode().getHdfsUser() != null) {
      this.owner = ds.getDataset().getInode().getHdfsUser().getUsername();
    } else {
      this.owner = "";
    }
    this.permission = FsPermission.
      createImmutable(ds.getDataset().getInode().getPermission()).toString();
    this.publicDs = ds.getDataset().getPublicDs();
    this.searchable = ds.getDataset().isSearchable();
  }

  private InodeView(String name, boolean dir, boolean parent, String path) {
    this.name = name;
    this.dir = dir;
    this.parent = parent;
    this.path = path;
    this.modification = null;
  }

  public static InodeView getParentInode(String path) {
    String name = "..";
    boolean dir = true;
    boolean parent = true;
    int lastSlash = path.lastIndexOf("/");
    if (lastSlash == path.length() - 1) {
      lastSlash = path.lastIndexOf("/", lastSlash - 1);
    }
    path = path.substring(0, lastSlash);
    return new InodeView(name, dir, parent, path);
  }

  public void setName(String name) {
    this.name = name;
  }

  public void setDir(boolean dir) {
    this.dir = dir;
  }

  public void setParentId(Long parentId) {
    this.parentId = parentId;
  }

  public void setId(Long id) {
    this.id = id;
  }

  public void setTemplate(int template) {
    this.template = template;
  }

  public void setParent(boolean parent) {
    this.parent = parent;
  }

  public void setPath(String path) {
    this.path = path;
  }

  public void setModification(Date modification) {
    this.modification = modification;
  }

  public void setAccessTime(Date accessTime) {
    this.accessTime = accessTime;
  }

  public String getName() {
    return name;
  }

  public boolean isDir() {
    return dir;
  }

  public Long getId() {
    return this.id;
  }

  public Long getParentId() {
    return this.parentId;
  }

  public int getTemplate() {
    return this.template;
  }

  public boolean isParent() {
    return parent;
  }

  public String getPath() {
    return path;
  }

  public Date getModification() {
    return modification;
  }

  public Date getAccessTime() {
    return accessTime;
  }

  public long getSize() {
    return size;
  }

  public void setSize(long size) {
    this.size = size;
  }

  public boolean isShared() {
    return shared;
  }

  public void setShared(boolean shared) {
    this.shared = shared;
  }

  public String getOwningProjectName() {
    return owningProjectName;
  }

  public void setOwningProjectName(String owningProjectName) {
    this.owningProjectName = owningProjectName;
  }

  public String getDescription() {
    return description;
  }

  public void setDescription(String description) {
    this.description = description;
  }

  public boolean getStatus() {
    return status;
  }

  public void setStatus(boolean status) {
    this.status = status;
  }

  public boolean isUnderConstruction() {
    return underConstruction;
  }

  public void setUnderConstruction(boolean underConstruction) {
    this.underConstruction = underConstruction;
  }

  public String getOwner() {
    return owner;
  }

  public void setOwner(String owner) {
    this.owner = owner;
  }

  public String getEmail() {
    return email;
  }

  public void setEmail(String email) {
    this.email = email;
  }

  public String getPermission() {
    return permission;
  }

  public void setPermission(String permission) {
    this.permission = permission;
  }

  public int getPublicDs() {
    return publicDs;
  }

  public void setPublicDs(int publicDs) {
    this.publicDs = publicDs;
  }
  
  public void setPublicDsState(SharedState sharedState) {
    this.publicDs = sharedState.state;
  }
  
  public boolean isPublicDataset() {
    if(publicDs == 0) {
      return false;
    }
    return true;
  }

  public int getSharedWith() {
    return sharedWith;
  }

  public void setSharedWith(int sharedWith) {
    this.sharedWith = sharedWith;
  }

  public boolean isSearchable() {
    return searchable;
  }

  public void setSearchable(boolean searchable) {
    this.searchable = searchable;
  }

  public String getZipState() {
    return zipState;
  }

  public void setZipState(String zipState) {
    this.zipState = zipState;
  }
    
  public String getPublicId() {
    return publicId;
  }

  public void setPublicId(String publicId) {
    this.publicId = publicId;
  }
  
  public DatasetType getType() {
    return type;
  }
  
  public void setType(DatasetType type) {
    this.type = type;
  }
  
  @Override
  public int hashCode() {
    int hash = 7;
    hash = 13 * hash + Objects.hashCode(this.name);
    hash = 13 * hash + Objects.hashCode(this.path);
    return hash;
  }

  @Override
  public boolean equals(Object obj) {
    if (obj == null) {
      return false;
    }
    if (getClass() != obj.getClass()) {
      return false;
    }
    final InodeView other = (InodeView) obj;
    if (!Objects.equals(this.name, other.name)) {
      return false;
    }
    return Objects.equals(this.path, other.path);
  }

}
