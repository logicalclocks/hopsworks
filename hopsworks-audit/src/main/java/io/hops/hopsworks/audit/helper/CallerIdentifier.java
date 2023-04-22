/*
 * Copyright (C) 2022, Logical Clocks AB. All rights reserved
 */
package io.hops.hopsworks.audit.helper;

import io.hops.hopsworks.persistence.entity.user.Users;

public class CallerIdentifier {
  private String username;
  private String email;
  private Integer userId;
  
  public CallerIdentifier() {
  }
  
  public CallerIdentifier(String username, String email, Integer userId) {
    this.username = username;
    this.email = email;
    this.userId = userId;
  }
  
  public CallerIdentifier(String username) {
    this.username = username;
  }
  
  public CallerIdentifier(Users user) {
    this.username = user.getUsername();
    this.email = user.getEmail();
    this.userId = user.getUid();
  }
  
  public CallerIdentifier(Integer userId) {
    this.userId = userId;
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
  
  public Integer getUserId() {
    return userId;
  }
  
  public void setUserId(Integer userId) {
    this.userId = userId;
  }
  
  @Override
  public String toString() {
    return "CallerIdentifier{" +
      "username='" + username + '\'' +
      ", email='" + email + '\'' +
      ", userId=" + userId +
      '}';
  }
}
