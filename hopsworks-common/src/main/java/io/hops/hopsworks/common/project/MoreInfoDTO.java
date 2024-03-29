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

package io.hops.hopsworks.common.project;

import java.util.Date;
import java.util.Objects;
import javax.xml.bind.annotation.XmlRootElement;
import io.hops.hopsworks.persistence.entity.project.Project;
import io.hops.hopsworks.persistence.entity.hdfs.inode.Inode;
import io.hops.hopsworks.common.hdfs.Utils;

@XmlRootElement
public class MoreInfoDTO {

  private Long inodeid;
  private String user;
  private double size;
  private Date createDate;
  private Date uploadDate;
  private int votes;
  private long downloads;
  private String path;

  public MoreInfoDTO() {
  }

  public MoreInfoDTO(Project proj, Inode projectInode) {
    this.inodeid = projectInode.getId();
    this.user = proj.getOwner().getFname() + " " + proj.getOwner().getFname();
    this.size = 0;
    this.createDate = proj.getCreated();
    this.uploadDate = null;
    this.path = Utils.getProjectPath(proj.getName());
  }

  public MoreInfoDTO(Inode inode) {
    this.inodeid = inode.getId();
    this.user = inode.getHdfsUser().getUsername();
    this.size = inode.getSize();
    this.createDate = new Date(inode.getModificationTime().longValue());
    this.uploadDate = null;
  }

  public MoreInfoDTO(Long inodeid, String user, double size, Date createDate,
          Date uploadDate, int votes, long downloads) {
    this.inodeid = inodeid;
    this.user = user;
    this.size = size;
    this.createDate = createDate;
    this.uploadDate = uploadDate;
    this.votes = votes;
    this.downloads = downloads;
  }

  public String getUser() {
    return user;
  }

  public Long getInodeid() {
    return inodeid;
  }

  public void setInodeid(Long inodeid) {
    this.inodeid = inodeid;
  }

  public void setUser(String user) {
    this.user = user;
  }

  public double getSize() {
    return size;
  }

  public void setSize(double size) {
    this.size = size;
  }

  public Date getCreateDate() {
    return createDate;
  }

  public void setCreateDate(Date createDate) {
    this.createDate = createDate;
  }

  public Date getUploadDate() {
    return uploadDate;
  }

  public void setUploadDate(Date uploadDate) {
    this.uploadDate = uploadDate;
  }

  public int getVotes() {
    return votes;
  }

  public void setVotes(int votes) {
    this.votes = votes;
  }

  public long getDownloads() {
    return downloads;
  }

  public void setDownloads(long downloads) {
    this.downloads = downloads;
  }

  public String getPath() {
    return path;
  }

  public void setPath(String path) {
    this.path = path;
  }

  @Override
  public int hashCode() {
    int hash = 7;
    hash = 79 * hash + Objects.hashCode(this.inodeid);
    return hash;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj == null) {
      return false;
    }
    if (getClass() != obj.getClass()) {
      return false;
    }
    final MoreInfoDTO other = (MoreInfoDTO) obj;
    if (!Objects.equals(this.inodeid, other.inodeid)) {
      return false;
    }
    return true;
  }

  @Override
  public String toString() {
    return "MoreInfoDTO{" + "inodeid=" + inodeid + ", user=" + user + ", size="
            + size + ", createDate=" + createDate + ", uploadDate=" + uploadDate
            + ", votes=" + votes + ", downloads=" + downloads + '}';
  }
}
