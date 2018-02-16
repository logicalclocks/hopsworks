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
