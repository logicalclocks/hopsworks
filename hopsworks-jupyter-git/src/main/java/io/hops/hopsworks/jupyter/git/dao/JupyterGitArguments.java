/*
 * Copyright (C) 2019, Logical Clocks AB. All rights reserved
 */

package io.hops.hopsworks.jupyter.git.dao;

import java.util.Map;

public class JupyterGitArguments {
  private String currentPath;
  private String cloneUrl;
  
  // For checkout
  private String topRepoPath;
  private Boolean checkoutBranch;
  private String branchname;
  private Boolean newCheck;
  
  // For config
  private String path;
  private Map<String, String> options;
  
  // For add
  private Boolean addAll;
  
  // For commit
  private String commitMsg;
  
  // For autopush
  private String remote;
  
  public JupyterGitArguments() {}
  
  public JupyterGitArguments(String currentPath, String cloneUrl) {
    this.currentPath = currentPath;
    this.cloneUrl = cloneUrl;
  }
  
  public String getCurrentPath() {
    return currentPath;
  }
  
  public void setCurrentPath(String currentPath) {
    this.currentPath = currentPath;
  }
  
  public String getCloneUrl() {
    return cloneUrl;
  }
  
  public void setCloneUrl(String cloneUrl) {
    this.cloneUrl = cloneUrl;
  }
  
  public String getTopRepoPath() {
    return topRepoPath;
  }
  
  public void setTopRepoPath(String topRepoPath) {
    this.topRepoPath = topRepoPath;
  }
  
  public Boolean getCheckoutBranch() {
    return checkoutBranch;
  }
  
  public void setCheckoutBranch(Boolean checkoutBranch) {
    this.checkoutBranch = checkoutBranch;
  }
  
  public String getBranchname() {
    return branchname;
  }
  
  public void setBranchname(String branchname) {
    this.branchname = branchname;
  }
  
  public Boolean getNewCheck() {
    return newCheck;
  }
  
  public void setNewCheck(Boolean newCheck) {
    this.newCheck = newCheck;
  }
  
  public String getPath() {
    return path;
  }
  
  public void setPath(String path) {
    this.path = path;
  }
  
  public Map<String, String> getOptions() {
    return options;
  }
  
  public void setOptions(Map<String, String> options) {
    this.options = options;
  }
  
  public Boolean getAddAll() {
    return addAll;
  }
  
  public void setAddAll(Boolean addAll) {
    this.addAll = addAll;
  }
  
  public String getCommitMsg() {
    return commitMsg;
  }
  
  public void setCommitMsg(String commitMsg) {
    this.commitMsg = commitMsg;
  }
  
  public String getRemote() {
    return remote;
  }
  
  public void setRemote(String remote) {
    this.remote = remote;
  }
}
