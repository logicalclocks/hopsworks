/*
 * This file is part of Hopsworks
 * Copyright (C) 2018, Logical Clocks AB. All rights reserved
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
package io.hops.hopsworks.api.user;

import io.hops.hopsworks.common.api.RestDTO;
import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement
public class UserProfileDTO extends RestDTO<UserDTO> {

  private String firstname;
  private String lastname;
  private String email;
  private String username;
  private String phoneNumber;
  private String userAccountType;
  private Boolean twoFactor;
  private Integer toursState;
  private Integer status;
  private Integer maxNumProjects;
  private Integer numCreatedProjects;
  private Integer numActiveProjects;

  public UserProfileDTO() {
  }

  public UserProfileDTO(String firstname, String lastname, String email, String username, String phoneNumber,
    String userAccountType,
      Boolean twoFactor, Integer toursState, Integer status, Integer maxNumProjects, Integer numCreatedProjects,
      Integer numActiveProjects) {
    this.firstname = firstname;
    this.lastname = lastname;
    this.email = email;
    this.username = username;
    this.phoneNumber = phoneNumber;
    this.userAccountType = userAccountType;
    this.twoFactor = twoFactor;
    this.toursState = toursState;
    this.status = status;
    this.maxNumProjects = maxNumProjects;
    this.numCreatedProjects = numCreatedProjects;
    this.numActiveProjects = numActiveProjects;
  }

  public String getFirstname() {
    return firstname;
  }

  public void setFirstname(String firstname) {
    this.firstname = firstname;
  }

  public String getLastname() {
    return lastname;
  }

  public void setLastname(String lastname) {
    this.lastname = lastname;
  }

  public String getEmail() {
    return email;
  }

  public void setEmail(String email) {
    this.email = email;
  }

  public String getPhoneNumber() {
    return phoneNumber;
  }

  public void setPhoneNumber(String phoneNumber) {
    this.phoneNumber = phoneNumber;
  }

  public String getUserAccountType() {
    return userAccountType;
  }

  public void setUserAccountType(String userAccountType) {
    this.userAccountType = userAccountType;
  }

  public Boolean getTwoFactor() {
    return twoFactor;
  }

  public void setTwoFactor(Boolean twoFactor) {
    this.twoFactor = twoFactor;
  }

  public Integer getToursState() {
    return toursState;
  }

  public void setToursState(Integer toursState) {
    this.toursState = toursState;
  }

  public Integer getStatus() {
    return status;
  }

  public void setStatus(Integer status) {
    this.status = status;
  }

  public Integer getMaxNumProjects() {
    return maxNumProjects;
  }

  public void setMaxNumProjects(Integer maxNumProjects) {
    this.maxNumProjects = maxNumProjects;
  }

  public Integer getNumCreatedProjects() {
    return numCreatedProjects;
  }

  public void setNumCreatedProjects(Integer numCreatedProjects) {
    this.numCreatedProjects = numCreatedProjects;
  }

  public Integer getNumActiveProjects() {
    return numActiveProjects;
  }

  public void setNumActiveProjects(Integer numActiveProjects) {
    this.numActiveProjects = numActiveProjects;
  }
  
  public String getUsername() {
    return username;
  }
  
  public void setUsername(String username) {
    this.username = username;
  }
}
