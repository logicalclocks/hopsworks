/*
 * This file is part of HopsWorks
 *
 * Copyright (C) 2013 - 2018, Logical Clocks AB and RISE SICS AB. All rights reserved.
 *
 * HopsWorks is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * HopsWorks is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with HopsWorks.  If not, see <http://www.gnu.org/licenses/>.
 */

package io.hops.hopsworks.common.dao.metadata;

import java.io.Serializable;

/**
 * Represents a composite entity holding a table id and an inode primary key.
 * It's necessary when asking for the metadata of a specific file
 */
public class InodeTableComposite implements Serializable, EntityIntf {

  private static final long serialVersionUID = 1L;

  private int tableid;
  private int inodePid;
  private String inodeName;

  public InodeTableComposite() {
  }

  public InodeTableComposite(int tableid, int inodePid, String inodeName) {
    if (inodeName == null) {
      throw new NullPointerException("inodeName");
    }
    this.tableid = tableid;
    this.inodePid = inodePid;
    this.inodeName = inodeName;
  }

  public int getTableid() {
    return this.tableid;
  }

  public int getInodePid() {
    return this.inodePid;
  }

  public String getInodeName() {
    return this.inodeName;
  }

  public void setTableid(int tableid) {
    this.tableid = tableid;
  }

  public void setInodePid(int inodePid) {
    this.inodePid = inodePid;
  }

  public void setInodeName(String inodeName) {
    this.inodeName = inodeName;
  }

  @Override
  public Integer getId() {
    throw new UnsupportedOperationException("Not necessary for this entity.");
  }

  @Override
  public void setId(Integer id) {
    throw new UnsupportedOperationException("Not necessary for this entity.");
  }

  @Override
  public void copy(EntityIntf entity) {
    throw new UnsupportedOperationException("Not necessary for this entity.");
  }

  @Override
  public String toString() {
    return "se.kth.hopsworks.meta.entity.InodeTableComposite [ inodepid="
            + this.inodePid + ", inodename=" + this.inodeName + ", tableid="
            + this.tableid + "]";
  }

}
