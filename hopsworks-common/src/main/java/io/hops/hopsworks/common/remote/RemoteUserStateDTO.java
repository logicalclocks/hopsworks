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
 */
package io.hops.hopsworks.common.remote;

import io.hops.hopsworks.persistence.entity.remote.user.RemoteUser;

import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement
public class RemoteUserStateDTO {

  private boolean saved;
  private RemoteUser remoteUser;
  private RemoteUserDTO remoteUserDTO;

  public RemoteUserStateDTO(boolean saved, RemoteUser remoteUser, RemoteUserDTO remoteUserDTO) {
    this.saved = saved;
    this.remoteUser = remoteUser;
    this.remoteUserDTO = remoteUserDTO;
  }

  public boolean isSaved() {
    return saved;
  }

  public void setSaved(boolean saved) {
    this.saved = saved;
  }

  public RemoteUser getRemoteUser() {
    return remoteUser;
  }

  public void setRemoteUser(RemoteUser remoteUser) {
    this.remoteUser = remoteUser;
  }

  public RemoteUserDTO getRemoteUserDTO() {
    return remoteUserDTO;
  }

  public void setRemoteUserDTO(RemoteUserDTO remoteUserDTO) {
    this.remoteUserDTO = remoteUserDTO;
  }

  @Override
  public String toString() {
    return "RemoteUserStateDTO{" + "saved=" + saved + ", remoteUser=" + remoteUser + ", remoteUserDTO="
        + remoteUserDTO + '}';
  }

}
