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

/**
 * Represents a path in the file system. Whenever there is metadata associated
 * to an inode in this path, then this inode has to be reinserted in the
 * metadata logs table. The front end sends a message request with this path
 */
public class DirPath implements EntityIntf {

  private String path;

  public DirPath(String path) {
    this.path = path;
  }

  public void setPath(String path) {
    this.path = path;
  }

  public String getPath() {
    return this.path;
  }

  public int getLength() {
    return this.path.length();
  }

  private String getDirectoryPart(String path) {
    int lastSlash = path.lastIndexOf("/");
    int startName = (lastSlash > -1) ? lastSlash + 1 : 0;
    return path.substring(0, startName);
  }

  /**
   * Extracts the last directory name from the current path
   * <p/>
   * @return
   */
  public String getParent() {
    //get the last two file separator positions
    String dirPart = getDirectoryPart(this.path);
    int lastSlash = dirPart.lastIndexOf("/");
    int secondLastSlash = dirPart.lastIndexOf("/", lastSlash - 1);

    return dirPart.substring(secondLastSlash + 1, lastSlash);
  }

  /**
   * Extracts the last directory name from the given path
   * <p/>
   * @param path
   * @return
   */
  public String getParent(String path) {

    int dirIndex = path.indexOf(path);
    int previousSlash = path.lastIndexOf("/", dirIndex - 1);

    return path.substring(previousSlash + 1, dirIndex - 1);
  }

  /**
   * Returns the dir path without the file name, if there is one
   *
   * @param path
   * @return
   */
  public String getDirPart(String path) {

    String dirPart = getDirectoryPart(path);
    return dirPart.substring(0, dirPart.length() - 1);
  }

  /**
   * Returns the dir path until the given point.
   *
   * @param part
   * @return
   */
  public String getDirPartUntil(String part) {

    int partIndex = this.path.indexOf(part);
    return this.path.substring(0, partIndex);
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
}
