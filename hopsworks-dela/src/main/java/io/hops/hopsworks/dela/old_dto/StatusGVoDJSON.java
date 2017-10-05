package io.hops.hopsworks.dela.old_dto;

import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement
public class StatusGVoDJSON {

  private TorrentId torrentId;
  private String datasetPath;

  public StatusGVoDJSON() {
  }

  public StatusGVoDJSON(String datasetPath, TorrentId torrentId) {
    this.torrentId = torrentId;
    this.datasetPath = datasetPath;
  }

  public TorrentId getTorrentId() {
    return torrentId;
  }

  public void setTorrentId(TorrentId torrentId) {
    this.torrentId = torrentId;
  }

  public String getDatasetPath() {
    return datasetPath;
  }

  public void setDatasetPath(String datasetPath) {
    this.datasetPath = datasetPath;
  }
}
