/*
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
 */

package io.hops.hopsworks.common.dao.featurestore.dependencies;

import io.hops.hopsworks.common.dao.hdfs.inode.Inode;
import io.hops.hopsworks.common.dao.hdfs.inode.InodeFacade;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;
import java.math.BigInteger;
import java.util.Date;
import java.util.List;
import java.util.Optional;

/**
 * DTO containing the human-readable information of a featurestore dependency,
 * can be converted to JSON or XML representation using jaxb.
 */
@XmlRootElement
@XmlType(propOrder = {"path", "modification", "inodeId", "dir"})
public class FeaturestoreDependencyDTO {

  private String path;
  private Date modification;
  private Long inodeId;
  private boolean dir;

  public FeaturestoreDependencyDTO(){}

  public FeaturestoreDependencyDTO(String path, Date modification, Long inodeId, boolean dir) {
    this.path = path;
    this.modification = modification;
    this.inodeId = inodeId;
    this.dir = dir;
  }

  public FeaturestoreDependencyDTO(Inode inode, InodeFacade inodeFacade) {
    this.path = inodeFacade.getPath(inode);
    this.dir = inode.isDir();
    this.modification = new Date(inode.getModificationTime().longValue());
    this.inodeId = inode.getId();
    setModificationDateForDir(inode, inodeFacade);
  }

  private void setModificationDateForDir(Inode inode, InodeFacade inodeFacade){
    if(!inode.isDir())
      return;
    List<Inode> children = inodeFacade.getChildren(inode);
    if(!children.isEmpty()){
      Optional<BigInteger> optionalLatestModification =
          children.stream().map(i -> i.getModificationTime()).max(BigInteger::compareTo);
      if(optionalLatestModification.isPresent())
        this.modification = new Date(optionalLatestModification.get().longValue());
    }
  }

  @XmlElement
  public String getPath() {
    return path;
  }

  @XmlElement
  public Date getModification() {
    return modification;
  }

  @XmlElement
  public Long getInodeId() {
    return inodeId;
  }

  @XmlElement
  public boolean isDir() {
    return dir;
  }

  public void setPath(String path) {
    this.path = path;
  }

  public void setModification(Date modification) {
    this.modification = modification;
  }

  public void setInodeId(Long inodeId) {
    this.inodeId = inodeId;
  }

  public void setDir(boolean dir) {
    this.dir = dir;
  }

  @Override
  public String toString() {
    return "FeaturestoreDependencyDTO{" +
        ", path='" + path + '\'' +
        ", modification=" + modification +
        ", inodeId=" + inodeId +
        ", dir=" + dir +
        '}';
  }
}
