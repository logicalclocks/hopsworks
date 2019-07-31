/*
 * This file is part of Hopsworks
 * Copyright (C) 2019, Logical Clocks AB. All rights reserved
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
 */

package io.hops.hopsworks.common.dao.rstudio;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement

public class RStudioDTO {

  private int port=0;
  private long pid=0;
  private String hostIp="";
  private String secret="";
  private String certificatesDir;
  
  public RStudioDTO() {
  }

  public RStudioDTO(int port, long pid, String secret, String certificatesDir) {
    this.port = port;
    this.pid = pid;
    this.secret = secret;
    try {
      this.hostIp = InetAddress.getLocalHost().getHostAddress();
    } catch (UnknownHostException ex) {
      Logger.getLogger(RStudioDTO.class.getName()).log(Level.SEVERE, null, ex);
    }
    this.certificatesDir = certificatesDir;
  }

  public String getHostIp() {
    return hostIp;
  }

  public void setHostIp(String host) {
    this.hostIp = host;
  }
  
  public long getPid() {
    return pid;
  }

  public void setPid(long pid) {
    this.pid = pid;
  }

  public int getPort() {
    return port;
  }

  public void setPort(int port) {
    this.port = port;
  }

  public String getSecret() {
    return secret;
  }

  public void setSecret(String secret) {
    this.secret = secret;
  }
  
  public String getCertificatesDir() {
    return certificatesDir;
  }
}
