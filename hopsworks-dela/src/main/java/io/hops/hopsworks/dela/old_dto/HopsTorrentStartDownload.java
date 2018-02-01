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
