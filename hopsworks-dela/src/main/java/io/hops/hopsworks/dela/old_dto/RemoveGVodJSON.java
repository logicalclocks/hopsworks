package io.hops.hopsworks.dela.old_dto;

import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement
public class RemoveGVodJSON {

  private TorrentId torrentId;

  public RemoveGVodJSON() {
  }

  public RemoveGVodJSON(TorrentId torrentId) {
    this.torrentId = torrentId;
  }

  public TorrentId getTorrentId() {
    return torrentId;
  }

  public void setTorrentId(TorrentId torrentId) {
    this.torrentId = torrentId;
  }

}
