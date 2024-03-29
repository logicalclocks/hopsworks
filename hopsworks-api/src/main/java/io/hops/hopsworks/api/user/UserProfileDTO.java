/*
 * Changes to this file committed after and not including commit-id: ccc0d2c5f9a5ac661e60e6eaf138de7889928b8b
 * are released under the following license:
 *
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
 *
 * Changes to this file committed before and including commit-id: ccc0d2c5f9a5ac661e60e6eaf138de7889928b8b
 * are released under the following license:
 *
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
 */
package io.hops.hopsworks.api.user;

import io.hops.hopsworks.common.api.RestDTO;
import io.hops.hopsworks.persistence.entity.user.BbcGroup;
import io.hops.hopsworks.persistence.entity.user.Users;

import javax.xml.bind.annotation.XmlRootElement;
import java.util.Collection;
import java.util.Date;

@XmlRootElement
public class UserProfileDTO extends RestDTO<UserProfileDTO> {

  private Integer id;
  private String firstname;
  private String lastname;
  private String email;
  private String username;
  private String accountType;
  private Boolean twoFactor;
  private Integer toursState;
  private Integer status;
  private Integer maxNumProjects;
  private Integer numActiveProjects;
  private Date activated;
  private Collection<BbcGroup> role;

  public UserProfileDTO() {
  }
  
  public UserProfileDTO(Users user) {
    this.id = user.getUid();
    this.firstname = user.getFname();
    this.lastname = user.getLname();
    this.email = user.getEmail();
    this.username = user.getUsername();
    this.accountType = user.getMode().toString();
    this.twoFactor = user.getTwoFactor();
    this.toursState = user.getToursState();
    this.status = user.getStatus().getValue();
    this.maxNumProjects = user.getMaxNumProjects();
    this.numActiveProjects = user.getNumActiveProjects();
    this.activated = user.getActivated();
    this.role = user.getBbcGroupCollection();
  }
  
  public Integer getId() {
    return id;
  }
  
  public void setId(Integer id) {
    this.id = id;
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

  public String getAccountType() {
    return accountType;
  }

  public void setAccountType(String accountType) {
    this.accountType = accountType;
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

  public Date getActivated() {
    return activated;
  }

  public void setActivated(Date activated) {
    this.activated = activated;
  }

  public Collection<BbcGroup> getRole() {
    return role;
  }

  public void setRole(Collection<BbcGroup> role) {
    this.role = role;
  }

  @Override
  public String toString() {
    return "UserProfileDTO{" +
      "id=" + id +
      ", firstname='" + firstname + '\'' +
      ", lastname='" + lastname + '\'' +
      ", email='" + email + '\'' +
      ", username='" + username + '\'' +
      ", accountType='" + accountType + '\'' +
      ", twoFactor=" + twoFactor +
      ", toursState=" + toursState +
      ", status=" + status +
      ", maxNumProjects=" + maxNumProjects +
      ", numActiveProjects=" + numActiveProjects +
      ", activated=" + activated +
      ", role=" + role +
      '}';
  }
}
