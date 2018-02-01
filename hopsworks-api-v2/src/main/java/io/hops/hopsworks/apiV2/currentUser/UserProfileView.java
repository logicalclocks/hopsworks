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
package io.hops.hopsworks.apiV2.currentUser;

import io.hops.hopsworks.common.dao.user.Users;

import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement
public class UserProfileView {
  
  private String firstName;
  private String lastName;
  private String telephoneNum;
  private String orgName;
  private String dep;;
  private Integer maxNumProjects;
  private boolean twoFactorAuthEnabled;
  private int toursState;
  private String street;
  private String city;
  private String postCode;
  private String country;
  private Integer uId;
  private String username;
  private String email;
  
  public UserProfileView(){}
  
  public UserProfileView(Users user) {
    this.username = user.getUsername();
    this.email = user.getEmail();
    this.firstName = user.getFname();
    this.lastName = user.getLname();
    this.telephoneNum = user.getMobile();
    if (user.getOrganization() != null) {
      this.orgName = user.getOrganization().getOrgName();
      this.dep = user.getOrganization().getDepartment();
    }
    if (user.getAddress() != null) {
      this.street = user.getAddress().getAddress2();
      this.city = user.getAddress().getCity();
      this.postCode = user.getAddress().getPostalcode();
      this.country = user.getAddress().getCountry();
    }
    this.maxNumProjects = user.getMaxNumProjects();
    this.twoFactorAuthEnabled = user.getTwoFactor();
    this.toursState = user.getToursState();
  }
  
  public String getFirstName() {
    return firstName;
  }
  
  public void setFirstName(String firstName) {
    this.firstName = firstName;
  }
  
  public String getLastName() {
    return lastName;
  }
  
  public void setLastName(String lastName) {
    this.lastName = lastName;
  }
  
  public String getTelephoneNum() {
    return telephoneNum;
  }
  
  public void setTelephoneNum(String telephoneNum) {
    this.telephoneNum = telephoneNum;
  }
  
  public String getOrgName() {
    return orgName;
  }
  
  public void setOrgName(String orgName) {
    this.orgName = orgName;
  }
  
  public String getDep() {
    return dep;
  }
  
  public void setDep(String dep) {
    this.dep = dep;
  }
  
  public Integer getMaxNumProjects() {
    return maxNumProjects;
  }
  
  public void setMaxNumProjects(Integer maxNumProjects) {
    this.maxNumProjects = maxNumProjects;
  }
  
  public boolean isTwoFactorAuthEnabled() {
    return twoFactorAuthEnabled;
  }
  
  public void setTwoFactorAuthEnabled(boolean twoFactorAuthEnabled) {
    this.twoFactorAuthEnabled = twoFactorAuthEnabled;
  }
  
  public int getToursState() {
    return toursState;
  }
  
  public void setToursState(int toursState) {
    this.toursState = toursState;
  }
  
  public String getStreet() {
    return street;
  }
  
  public void setStreet(String street) {
    this.street = street;
  }
  
  public String getCity() {
    return city;
  }
  
  public void setCity(String city) {
    this.city = city;
  }
  
  public String getPostCode() {
    return postCode;
  }
  
  public void setPostCode(String postCode) {
    this.postCode = postCode;
  }
  
  public String getCountry() {
    return country;
  }
  
  public void setCountry(String country) {
    this.country = country;
  }
  
  public Integer getuId() {
    return uId;
  }
  
  public void setuId(Integer uId) {
    this.uId = uId;
  }
  
  public String getUsername() {
    return username;
  }
  
  public void setUsername(String username) {
    this.username = username;
  }
  
  public String getEmail() {
    return email;
  }
  
  public void setEmail(String email) {
    this.email = email;
  }
}
