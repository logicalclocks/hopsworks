/*
 * Copyright (C) 2020, Logical Clocks AB. All rights reserved
 */
package io.hops.hopsworks.common.remote;

import javax.xml.bind.annotation.XmlRootElement;
import java.util.ArrayList;
import java.util.List;

@XmlRootElement
public class RemoteUsersDTO {
  private List<RemoteUserDTO> validRemoteUsers;
  private List<RemoteUserDTO> invalidRemoteUsers;
  
  public RemoteUsersDTO() {
    this.validRemoteUsers = new ArrayList<>();
    this.invalidRemoteUsers = new ArrayList<>();
  }
  
  public RemoteUsersDTO(List<RemoteUserDTO> validRemoteUsers, List<RemoteUserDTO> invalidRemoteUsers) {
    this.validRemoteUsers = validRemoteUsers;
    this.invalidRemoteUsers = invalidRemoteUsers;
  }
  
  public List<RemoteUserDTO> getValidRemoteUsers() {
    return validRemoteUsers;
  }
  
  public void setValidRemoteUsers(List<RemoteUserDTO> validRemoteUsers) {
    this.validRemoteUsers = validRemoteUsers;
  }
  
  public List<RemoteUserDTO> getInvalidRemoteUsers() {
    return invalidRemoteUsers;
  }
  
  public void setInvalidRemoteUsers(List<RemoteUserDTO> invalidRemoteUsers) {
    this.invalidRemoteUsers = invalidRemoteUsers;
  }
  
  public void add(RemoteUserDTO remoteUserDTO) {
    if (validRemoteUser(remoteUserDTO)) {
      this.validRemoteUsers.add(remoteUserDTO);
    } else {
      this.invalidRemoteUsers.add(remoteUserDTO);
    }
  }
  
  public void addAll(RemoteUsersDTO remoteUsersDTO) {
    this.validRemoteUsers.addAll(remoteUsersDTO.getValidRemoteUsers());
    this.invalidRemoteUsers.addAll(remoteUsersDTO.getInvalidRemoteUsers());
  }
  
  private boolean validRemoteUser(RemoteUserDTO user) {
    return user.getUuid() != null &&
      !user.getUuid().isEmpty() &&
      user.getEmail() != null &&
      !user.getEmail().isEmpty() &&
      user.getGivenName() != null &&
      !user.getGivenName().isEmpty() &&
      user.getSurname() != null &&
      !user.getSurname().isEmpty() &&
      user.isEmailVerified();
  }
  
  @Override
  public String toString() {
    return "RemoteUsersDTO{" +
      "validRemoteUsers=" + validRemoteUsers +
      ", invalidRemoteUsers=" + invalidRemoteUsers +
      '}';
  }
}
