package io.hops.hopsworks.dela.old_dto;

import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement
public class TorrentExtendedStatusJSON {

  private TorrentId torrentId;
  private String torrentStatus;
  private int downloadSpeed;
  private double percentageCompleted;

  public TorrentExtendedStatusJSON(TorrentId torrentId, String torrentStatus,
          int downloadSpeed,
          double percentageCompleted) {
    this.torrentId = torrentId;
    this.torrentStatus = torrentStatus;
    this.downloadSpeed = downloadSpeed;
    this.percentageCompleted = percentageCompleted;
  }

  public TorrentExtendedStatusJSON() {
  }

  public String getTorrentStatus() {
    return torrentStatus;
  }

  public void setTorrentStatus(String torrentStatus) {
    this.torrentStatus = torrentStatus;
  }

  public int getDownloadSpeed() {
    return downloadSpeed;
  }

  public void setDownloadSpeed(int downloadSpeed) {
    this.downloadSpeed = downloadSpeed;
  }

  public TorrentId getTorrentId() {
    return torrentId;
  }

  public void setTorrentId(TorrentId torrentId) {
    this.torrentId = torrentId;
  }

  public double getPercentageCompleted() {
    return percentageCompleted;
  }

  public void setPercentageCompleted(double percentageCompleted) {
    this.percentageCompleted = percentageCompleted;
  }

}
