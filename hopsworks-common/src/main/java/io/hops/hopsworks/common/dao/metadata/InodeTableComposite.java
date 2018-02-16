/*
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
 *
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
