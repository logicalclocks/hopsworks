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


import com.fasterxml.jackson.annotation.JsonIgnore;

import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlTransient;
import java.util.List;

@XmlRootElement
public class RemoteUserDTO {

  private String uuid;//Universally unique identifier
  private String uid;//username in remote
  private String givenName;
  private String surname;
  private List<String> email;
  private List<String> groups;
  private boolean emailVerified;
  private boolean consentRequired;

  public RemoteUserDTO() {
  }

  public RemoteUserDTO(String uuid, String uid, String givenName, String surname, List<String> email,
      boolean emailVerified) {
    this.uuid = uuid;
    this.uid = uid;
    this.givenName = givenName;
    this.surname = surname;
    this.email = email;
    this.emailVerified = emailVerified;
  }
  
  public RemoteUserDTO(String uuid, String givenName, String surname, List<String> email) {
    this.uuid = uuid;
    this.givenName = givenName;
    this.surname = surname;
    this.email = email;
  }
  
  @XmlTransient
  @JsonIgnore
  public String getUuid() {
    return uuid;
  }

  public void setUuid(String uuid) {
    this.uuid = uuid;
  }

  public String getUid() {
    return uid;
  }

  public void setUid(String uid) {
    this.uid = uid;
  }

  public String getGivenName() {
    return givenName;
  }

  public void setGivenName(String givenName) {
    this.givenName = givenName;
  }

  public String getSurname() {
    return surname;
  }

  public void setSurname(String surname) {
    this.surname = surname;
  }

  public List<String> getEmail() {
    return email;
  }

  public void setEmail(List<String> email) {
    this.email = email;
  }

  public List<String> getGroups() {
    return groups;
  }

  public void setGroups(List<String> groups) {
    this.groups = groups;
  }

  public boolean isEmailVerified() {
    return emailVerified;
  }

  public void setEmailVerified(boolean emailVerified) {
    this.emailVerified = emailVerified;
  }
  
  public boolean isConsentRequired() {
    return consentRequired;
  }
  
  public void setConsentRequired(boolean consentRequired) {
    this.consentRequired = consentRequired;
  }
  
  public boolean needToChooseEmail() {
    return this.email != null && this.email.size() > 1;
  }
  
  @Override
  public String toString() {
    String s = "uuid: " + this.uuid + " uid: " + this.uid + " givenName: " + this.givenName + " surname: "
        + this.surname + " email: " + this.email + " groups: ";
    if (groups != null) {
      for (String grp : groups) {
        s = s + grp + ", ";
      }
    }
    return s;
  }

}
