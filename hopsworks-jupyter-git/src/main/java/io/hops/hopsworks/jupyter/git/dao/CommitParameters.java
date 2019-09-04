/*
 * Copyright (C) 2019, Logical Clocks AB. All rights reserved
 */

package io.hops.hopsworks.jupyter.git.dao;

public class CommitParameters {
  private String message;
  private Author author;
  
  public CommitParameters() {}
  
  public CommitParameters(String message, Author author) {
    this.message = message;
    this.author = author;
  }
  
  public String getMessage() {
    return message;
  }
  
  public void setMessage(String message) {
    this.message = message;
  }
  
  public Author getAuthor() {
    return author;
  }
  
  public void setAuthor(Author author) {
    this.author = author;
  }
  
  public static class Author {
    private String username;
    private String email;
    
    public Author() {}
  
    public Author(String username, String email) {
      this.username = username;
      this.email = email;
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
}
