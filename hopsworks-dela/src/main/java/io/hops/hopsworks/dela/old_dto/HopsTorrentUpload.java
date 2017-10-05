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
