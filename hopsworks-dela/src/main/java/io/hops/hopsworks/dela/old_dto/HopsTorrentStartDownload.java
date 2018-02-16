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

import io.hops.hopsworks.common.dela.AddressJSON;
import java.util.List;

public class HopsTorrentStartDownload {

  private TorrentId torrentId;
  private String torrentName;
  private Integer projectId;
  private Integer datasetId;
  private HDFSResource manifestHDFSResource;
  private List<AddressJSON> partners;
  private HDFSEndpoint hdfsEndpoint;

  public HopsTorrentStartDownload(TorrentId torrentId, String torrentName,
          Integer projectId, Integer datasetId, HDFSResource manifestHDFSResource, 
          List<AddressJSON> partners, HDFSEndpoint hdfsEndpoint) {
    this.torrentId = torrentId;
    this.torrentName = torrentName;
    this.projectId = projectId;
    this.datasetId = datasetId;
    this.manifestHDFSResource = manifestHDFSResource;
    this.partners = partners;
    this.hdfsEndpoint = hdfsEndpoint;
  }

  public HopsTorrentStartDownload() {
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

  public List<AddressJSON> getPartners() {
    return partners;
  }

  public void setPartners(List<AddressJSON> partners) {
    this.partners = partners;
  }

  public HDFSEndpoint getHdfsEndpoint() {
    return hdfsEndpoint;
  }

  public void setHdfsEndpoint(HDFSEndpoint hdfsEndpoint) {
    this.hdfsEndpoint = hdfsEndpoint;
  }

}
