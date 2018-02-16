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
