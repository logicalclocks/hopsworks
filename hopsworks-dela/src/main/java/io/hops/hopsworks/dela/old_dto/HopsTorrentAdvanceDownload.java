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
public class HopsTorrentAdvanceDownload {

  private TorrentId torrentId;
  private KafkaEndpoint kafkaEndpoint;
  private HDFSEndpoint hdfsEndpoint;
  private ExtendedDetails extendedDetails;

  public HopsTorrentAdvanceDownload() {
  }

  public HopsTorrentAdvanceDownload(TorrentId torrentId,
          KafkaEndpoint kafkaEndpoint, HDFSEndpoint hdfsEndpoint,
          ExtendedDetails extendedDetails) {
    this.torrentId = torrentId;
    this.kafkaEndpoint = kafkaEndpoint;
    this.hdfsEndpoint = hdfsEndpoint;
    this.extendedDetails = extendedDetails;
  }

  public TorrentId getTorrentId() {
    return torrentId;
  }

  public void setTorrentId(TorrentId torrentId) {
    this.torrentId = torrentId;
  }

  public KafkaEndpoint getKafkaEndpoint() {
    return kafkaEndpoint;
  }

  public void setKafkaEndpoint(KafkaEndpoint kafkaEndpoint) {
    this.kafkaEndpoint = kafkaEndpoint;
  }

  public ExtendedDetails getExtendedDetails() {
    return extendedDetails;
  }

  public void setExtendedDetails(ExtendedDetails extendedDetails) {
    this.extendedDetails = extendedDetails;
  }

  public HDFSEndpoint getHdfsEndpoint() {
    return hdfsEndpoint;
  }

  public void setHdfsEndpoint(HDFSEndpoint hdfsEndpoint) {
    this.hdfsEndpoint = hdfsEndpoint;
  }

}
