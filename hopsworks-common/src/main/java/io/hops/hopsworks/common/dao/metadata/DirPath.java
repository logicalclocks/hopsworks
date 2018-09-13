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
