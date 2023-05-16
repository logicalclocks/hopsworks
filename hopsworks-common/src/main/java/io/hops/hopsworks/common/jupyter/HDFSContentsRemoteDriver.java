/*
 * This file is part of Hopsworks
 * Copyright (C) 2023, Hopsworks AB. All rights reserved
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
 */
package io.hops.hopsworks.common.jupyter;

public class HDFSContentsRemoteDriver implements RemoteFSDriver {
  private String namenodeIp;
  private String namenodePort;
  private String hdfsUser;
  private String contentsManager;
  private String baseDirectory;

  public HDFSContentsRemoteDriver(String namenodeIp, String namenodePort, String hdfsUser, String contentsManager,
                                  String baseDirectory) {
    this.namenodeIp = namenodeIp;
    this.namenodePort = namenodePort;
    this.hdfsUser = hdfsUser;
    this.contentsManager = contentsManager;
    this.baseDirectory = baseDirectory;
  }

  public String getNamenodeIp() { return namenodeIp; }

  public void setNamenodeIp(String namenodeIp) { this.namenodeIp = namenodeIp; }

  public String getNamenodePort() { return namenodePort; }

  public void setNamenodePort(String namenodePort) { this.namenodePort = namenodePort; }

  public String getHdfsUser() { return hdfsUser; }

  public void setHdfsUser(String hdfsUser) { this.hdfsUser = hdfsUser; }

  public String getContentsManager() { return contentsManager; }

  public void setContentsManager(String contentsManager) { this.contentsManager = contentsManager; }

  public String getBaseDirectory() { return baseDirectory; }

  public void setBaseDirectory(String baseDirectory) { this.baseDirectory = baseDirectory; }
}
