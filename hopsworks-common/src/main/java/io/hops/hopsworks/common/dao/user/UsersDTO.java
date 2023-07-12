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

package io.hops.hopsworks.common.dao.user;

import io.hops.hopsworks.persistence.entity.user.Users;

import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement
public class UsersDTO {

  private String username;
  private String email;
  private String firstName;
  private String lastName;
  private int status;
  private String secret;
  private String chosenPassword;
  private String repeatedPassword;
  private boolean tos;
  private boolean twoFactor;
  private int toursState;
  private int maxNumProjects;
  private boolean testUser;
  private String userAccountType;
  private int numActiveProjects;
  private int numRemainingProjects;

  public UsersDTO() {
  }

  public UsersDTO(Users user) {
    this.username = user.getUsername();
    this.email = user.getEmail();
    this.firstName = user.getFname();
    this.lastName = user.getLname();
    this.maxNumProjects = user.getMaxNumProjects();
    this.twoFactor = user.getTwoFactor();
    this.toursState = user.getToursState();
    this.userAccountType = user.getMode().toString();
    this.numActiveProjects = user.getNumActiveProjects();
    numRemainingProjects = maxNumProjects-numActiveProjects;
  }

  public String getUsername() { return username; }

  public void setUsername(String username) { this.username = username; }

  public String getEmail() {
    return email;
  }

  public void setEmail(String email) {
    this.email = email;
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

  public int getStatus() {
    return status;
  }

  public void setStatus(int status) {
    this.status = status;
  }

  public String getSecret() {
    return secret;
  }

  public void setSecret(String secret) {
    this.secret = secret;
  }

  public String getChosenPassword() {
    return chosenPassword;
  }

  public void setChosenPassword(String chosenPassword) {
    this.chosenPassword = chosenPassword;
  }

  public String getRepeatedPassword() {
    return repeatedPassword;
  }

  public void setRepeatedPassword(String repeatedPassword) {
    this.repeatedPassword = repeatedPassword;
  }

  public boolean getTos() {
    return tos;
  }

  public void setTos(boolean tos) {
    this.tos = tos;
  }

  public boolean isTwoFactor() {
    return twoFactor;
  }

  public void setTwoFactor(boolean twoFactor) {
    this.twoFactor = twoFactor;
  }

  public int getToursState() {
    return toursState;
  }
  
  public void setToursState(int toursState) {
    this.toursState = toursState;
  }

  public int getMaxNumProjects() {
    return maxNumProjects;
  }

  public void setMaxNumProjects(int maxNumProjects) {
    this.maxNumProjects = maxNumProjects;
    numRemainingProjects = maxNumProjects-numActiveProjects;
  }

  public int getNumRemainingProjects(){
    return numRemainingProjects;
  }
  
  public void setNumRemainingProjects(int numRemainingProjects){
    this.numRemainingProjects = numRemainingProjects;
  }
  
  public int getNumActiveProjects() {
    return numActiveProjects;
  }

  public void setNumActiveProjects(int numActiveProjects) {
    this.numActiveProjects = numActiveProjects;
    numRemainingProjects = maxNumProjects-numActiveProjects;
  }

  public boolean isTestUser() {
    return testUser;
  }

  public void setTestUser(boolean testUser) {
    this.testUser = testUser;
  }

  public String getUserAccountType() {
    return userAccountType;
  }

  public void setUserAccountType(String userAccountType) {
    this.userAccountType = userAccountType;
  }
  
  @Override
  public String toString() {
    return "UserDTO{" +
      "username='" + username + '\'' +
      ", email='" + email + '\'' +
      ", firstName='" + firstName + '\'' +
      ", lastName='" + lastName + '\'' +
      ", status=" + status +
      ", tos=" + tos +
      ", twoFactor=" + twoFactor +
      ", toursState=" + toursState +
      ", maxNumProjects=" + maxNumProjects +
      ", testUser=" + testUser +
      ", userAccountType='" + userAccountType + '\'' +
      ", numActiveProjects=" + numActiveProjects +
      ", numRemainingProjects=" + numRemainingProjects +
      '}';
  }
}
