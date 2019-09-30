/*
 * Copyright (C) 2019, Logical Clocks AB. All rights reserved
 */

package io.hops.hopsworks.jupyter.git.dao;

import java.util.List;

public class JupyterGitStatusResponse extends JupyterGitResponse {
  private List<FileStatus> files;
  private String command;
  private String branch;
  
  public List<FileStatus> getFiles() {
    return files;
  }
  
  public void setFiles(List<FileStatus> files) {
    this.files = files;
  }
  
  public String getCommand() {
    return command;
  }
  
  public void setCommand(String command) {
    this.command = command;
  }
  
  public String getBranch() {
    return branch;
  }
  
  public void setBranch(String branch) {
    this.branch = branch;
  }
  
  public class FileStatus {
    private String x;
    private String y;
    private String to;
    private String from;
  
    public String getX() {
      return x;
    }
  
    public void setX(String x) {
      this.x = x;
    }
  
    public String getY() {
      return y;
    }
  
    public void setY(String y) {
      this.y = y;
    }
  
    public String getTo() {
      return to;
    }
  
    public void setTo(String to) {
      this.to = to;
    }
  
    public String getFrom() {
      return from;
    }
  
    public void setFrom(String from) {
      this.from = from;
    }
  }
}
