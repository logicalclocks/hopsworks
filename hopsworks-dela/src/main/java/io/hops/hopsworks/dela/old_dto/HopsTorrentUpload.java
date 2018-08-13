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

package io.hops.hopsworks.dela.old_dto;

import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement
public class HopsTorrentUpload {

  private TorrentId torrentId;
  private String torrentName;
  private Integer projectId;
  private Integer datasetId;
  private HDFSResource manifestHDFSResource;
  private HDFSEndpoint hdfsEndpoint;

  public HopsTorrentUpload(TorrentId torrentId, String torrentName,
          Integer projectId, Integer datasetId, 
          HDFSResource manifestHDFSResource, 
          HDFSEndpoint hdfsEndpoint) {
    this.torrentId = torrentId;
    this.torrentName = torrentName;
    this.projectId = projectId;
    this.datasetId = datasetId;
    this.manifestHDFSResource = manifestHDFSResource;
    this.hdfsEndpoint = hdfsEndpoint;
  }

  public HopsTorrentUpload() {
  }

  public String getTorrentName() {
    return torrentName;
  }

  public void setTorrentName(String torrentName) {
    this.torrentName = torrentName;
  }

  public TorrentId getTorrentId() {
    return torrentId;
  }

  public void setTorrentId(TorrentId torrentId) {
    this.torrentId = torrentId;
  }

  public Integer getProjectId() {
    return projectId;
  }

  public void setProjectId(Integer projectId) {
    this.projectId = projectId;
  }

  public Integer getDatasetId() {
    return datasetId;
  }

  public void setDatasetId(Integer datasetId) {
    this.datasetId = datasetId;
  }
  
  public HDFSResource getManifestHDFSResource() {
    return manifestHDFSResource;
  }

  public void setManifestHDFSResource(HDFSResource manifestHDFSResource) {
    this.manifestHDFSResource = manifestHDFSResource;
  }

  public HDFSEndpoint getHdfsEndpoint() {
    return hdfsEndpoint;
  }

  public void setHdfsEndpoint(HDFSEndpoint hdfsEndpoint) {
    this.hdfsEndpoint = hdfsEndpoint;
  }

  @Override
  public String toString() {
    return "HopsTorrentUpload{" + "torrentId=" + torrentId + ", torrentName=" +
            torrentName + ", manifestHDFSResource=" + manifestHDFSResource +
            ", hdfsEndpoint=" + hdfsEndpoint + '}';
  }

}
