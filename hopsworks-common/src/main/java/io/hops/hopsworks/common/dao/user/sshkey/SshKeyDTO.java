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

package io.hops.hopsworks.common.dao.user.sshkey;

import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement
public class SshKeyDTO {

  private String name;
  private String publicKey;
  private boolean status = false;

  public SshKeyDTO() {
  }

  public SshKeyDTO(SshKeys key) {
    this.name = key.getSshKeysPK().getName();
    this.publicKey = key.getPublicKey();
  }

  public boolean isStatus() {
    return status;
  }

  public void setStatus(boolean status) {
    this.status = status;
  }

  public String getName() {
    return name;
  }

  public String getPublicKey() {
    return publicKey;
  }

  public void setName(String name) {
    this.name = name;
  }

  public void setPublicKey(String publicKey) {
    this.publicKey = publicKey;
  }

  @Override
  public String toString() {
    return "SshkeyDTO{name=" + name + "publicKey=" + publicKey + '}';
  }

}
