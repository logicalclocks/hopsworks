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
package io.hops.hopsworks.util.models;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Date;

public class User {
  
  private Integer uid;
  private String username;
  private String password;
  private String email;
  private String fname;
  private String lname;
  private Date activated;
  private String title;
  private String orcid;
  private int falseLogin;
  private String status;
  private int isonline;
  private String secret;
  private String validationKey;
  private Date validationKeyUpdated;
  private String validationKeyType;
  private String securityQuestion;
  private String securityAnswer;
  private String mode;
  private Date passwordChanged;
  private String notes;
  private String mobile;
  private Integer maxNumProjects;
  private Integer numCreatedProjects = 0;
  private Integer numActiveProjects = 0;
  private boolean twoFactor;
  private String salt;
  private int toursState;
  
  public User(ResultSet rs) throws SQLException {
    this.uid = rs.getInt("uid");
    this.username = rs.getString("username");
    this.password = rs.getString("password");
    this.email = rs.getString("email");
    this.fname = rs.getString("fname");
    this.lname = rs.getString("lname");
    this.activated = rs.getDate("activated");
    this.title = rs.getString("title");
    this.orcid = rs.getString("orcid");
    this.falseLogin = rs.getInt("false_login");
    this.status = rs.getString("status");
    this.isonline = rs.getInt("isonline");
    this.secret = rs.getString("secret");
    this.validationKey = rs.getString("validation_key");
    this.validationKeyUpdated = rs.getDate("validation_key_updated");
    this.validationKeyType = rs.getString("validation_key_type");
    this.securityQuestion = rs.getString("security_question");
    this.securityAnswer = rs.getString("security_answer");
    this.mode = rs.getString("mode");
    this.passwordChanged = rs.getDate("password_changed");
    this.notes = rs.getString("notes");
    this.mobile = rs.getString("mobile");
    this.maxNumProjects = rs.getInt("max_num_projects");
    this.numCreatedProjects = rs.getInt("num_created_projects");
    this.numActiveProjects = rs.getInt("num_active_projects");
    this.twoFactor = rs.getBoolean("two_factor");
    this.salt = rs.getString("salt");
    this.toursState = rs.getInt("tours_state");
  }
  
  public Integer getUid() {
    return uid;
  }
  
  public void setUid(Integer uid) {
    this.uid = uid;
  }
  
  public String getUsername() {
    return username;
  }
  
  public void setUsername(String username) {
    this.username = username;
  }
  
  public String getPassword() {
    return password;
  }
  
  public void setPassword(String password) {
    this.password = password;
  }
  
  public String getEmail() {
    return email;
  }
  
  public void setEmail(String email) {
    this.email = email;
  }
  
  public String getFname() {
    return fname;
  }
  
  public void setFname(String fname) {
    this.fname = fname;
  }
  
  public String getLname() {
    return lname;
  }
  
  public void setLname(String lname) {
    this.lname = lname;
  }
  
  public Date getActivated() {
    return activated;
  }
  
  public void setActivated(Date activated) {
    this.activated = activated;
  }
  
  public String getTitle() {
    return title;
  }
  
  public void setTitle(String title) {
    this.title = title;
  }
  
  public String getOrcid() {
    return orcid;
  }
  
  public void setOrcid(String orcid) {
    this.orcid = orcid;
  }
  
  public int getFalseLogin() {
    return falseLogin;
  }
  
  public void setFalseLogin(int falseLogin) {
    this.falseLogin = falseLogin;
  }
  
  public String getStatus() {
    return status;
  }
  
  public void setStatus(String status) {
    this.status = status;
  }
  
  public int getIsonline() {
    return isonline;
  }
  
  public void setIsonline(int isonline) {
    this.isonline = isonline;
  }
  
  public String getSecret() {
    return secret;
  }
  
  public void setSecret(String secret) {
    this.secret = secret;
  }
  
  public String getValidationKey() {
    return validationKey;
  }
  
  public void setValidationKey(String validationKey) {
    this.validationKey = validationKey;
  }
  
  public Date getValidationKeyUpdated() {
    return validationKeyUpdated;
  }
  
  public void setValidationKeyUpdated(Date validationKeyUpdated) {
    this.validationKeyUpdated = validationKeyUpdated;
  }
  
  public String getValidationKeyType() {
    return validationKeyType;
  }
  
  public void setValidationKeyType(String validationKeyType) {
    this.validationKeyType = validationKeyType;
  }
  
  public String getSecurityQuestion() {
    return securityQuestion;
  }
  
  public void setSecurityQuestion(String securityQuestion) {
    this.securityQuestion = securityQuestion;
  }
  
  public String getSecurityAnswer() {
    return securityAnswer;
  }
  
  public void setSecurityAnswer(String securityAnswer) {
    this.securityAnswer = securityAnswer;
  }
  
  public String getMode() {
    return mode;
  }
  
  public void setMode(String mode) {
    this.mode = mode;
  }
  
  public Date getPasswordChanged() {
    return passwordChanged;
  }
  
  public void setPasswordChanged(Date passwordChanged) {
    this.passwordChanged = passwordChanged;
  }
  
  public String getNotes() {
    return notes;
  }
  
  public void setNotes(String notes) {
    this.notes = notes;
  }
  
  public String getMobile() {
    return mobile;
  }
  
  public void setMobile(String mobile) {
    this.mobile = mobile;
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
  
  public boolean isTwoFactor() {
    return twoFactor;
  }
  
  public void setTwoFactor(boolean twoFactor) {
    this.twoFactor = twoFactor;
  }
  
  public String getSalt() {
    return salt;
  }
  
  public void setSalt(String salt) {
    this.salt = salt;
  }
  
  public int getToursState() {
    return toursState;
  }
  
  public void setToursState(int toursState) {
    this.toursState = toursState;
  }
  
  @Override
  public String toString() {
    return "User{" +
      "uid=" + uid +
      ", username='" + username + '\'' +
      ", password='" + password + '\'' +
      ", email='" + email + '\'' +
      ", fname='" + fname + '\'' +
      ", lname='" + lname + '\'' +
      ", activated=" + activated +
      ", title='" + title + '\'' +
      ", orcid='" + orcid + '\'' +
      ", falseLogin=" + falseLogin +
      ", status='" + status + '\'' +
      ", isonline=" + isonline +
      ", secret='" + secret + '\'' +
      ", validationKey='" + validationKey + '\'' +
      ", validationKeyUpdated=" + validationKeyUpdated +
      ", validationKeyType='" + validationKeyType + '\'' +
      ", securityQuestion='" + securityQuestion + '\'' +
      ", securityAnswer='" + securityAnswer + '\'' +
      ", mode='" + mode + '\'' +
      ", passwordChanged=" + passwordChanged +
      ", notes='" + notes + '\'' +
      ", mobile='" + mobile + '\'' +
      ", maxNumProjects=" + maxNumProjects +
      ", numCreatedProjects=" + numCreatedProjects +
      ", numActiveProjects=" + numActiveProjects +
      ", twoFactor=" + twoFactor +
      ", salt='" + salt + '\'' +
      ", toursState=" + toursState +
      '}';
  }
}
