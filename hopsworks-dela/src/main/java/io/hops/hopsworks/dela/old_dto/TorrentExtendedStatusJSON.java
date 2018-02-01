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
