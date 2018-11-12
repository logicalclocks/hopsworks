/*
 * Copyright (C) 2013 - 2018, Logical Clocks AB and RISE SICS AB. All rights reserved
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this
 * software and associated documentation files (the "Software"), to deal in the Software
 * without restriction, including without limitation the rights to use, copy, modify, merge,
 * publish, distribute, sublicense, and/or sell copies of the Software, and to permit
 * persons to whom the Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or
 * substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS  OR IMPLIED, INCLUDING
 * BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL  THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM,
 * DAMAGES OR  OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 *
 */
package io.hops.hopsworks.api.user;

import io.hops.hopsworks.common.api.RestDTO;
import io.hops.hopsworks.common.dao.user.Users;

import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement
public class UserDTO extends RestDTO<Users, UserDTO> {
  
  private String firstname;
  private String lastname;
  private String email;
  private String phoneNumber;
  private String userAccountType;
  private Boolean twoFactor;
  private Integer toursState;
  private Integer status;
  private Integer maxNumProjects;
  private Integer numCreatedProjects;
  private Integer numActiveProjects;
  
  public UserDTO() {
  }
  
  public UserDTO(Users user) {
    this.firstname = user.getFname();
    this.lastname = user.getLname();
    this.email = user.getEmail();
    this.phoneNumber = user.getMobile();
    this.toursState = user.getToursState();
    this.status = user.getStatus().getValue();
    this.userAccountType = user.getMode().toString();
    this.maxNumProjects = user.getMaxNumProjects();
    this.numCreatedProjects = user.getNumCreatedProjects();
    this.numActiveProjects = user.getNumActiveProjects();
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
  
  
}
