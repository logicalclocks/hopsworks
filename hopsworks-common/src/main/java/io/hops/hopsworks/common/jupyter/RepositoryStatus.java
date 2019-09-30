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

package io.hops.hopsworks.common.jupyter;

public interface RepositoryStatus {
  enum STATUS {
    DIRTY,
    CLEAN,
    UNINITIALIZED
  }
  
  STATUS getStatus();
  Integer getModifiedFiles();
  String getRepository();
  String getBranch();
  
  public final class UnmodifiableRepositoryStatus implements RepositoryStatus {
  
    private final STATUS status = STATUS.UNINITIALIZED;
    private final Integer modifiedFiles = -1;
    private final String repository = "UNKNOWN";
    private final String branch = "UNKNOWN";
    
    @Override
    public STATUS getStatus() {
      return status;
    }
  
    @Override
    public Integer getModifiedFiles() {
      return modifiedFiles;
    }
  
    @Override
    public String getRepository() {
      return repository;
    }
  
    @Override
    public String getBranch() {
      return branch;
    }
  }
  
  public class ModifiableRepositoryStatus implements RepositoryStatus {
    private STATUS status;
    private Integer modifiedFiles;
    private String repository;
    private String branch;
    
    public ModifiableRepositoryStatus() {}
    
    @Override
    public STATUS getStatus() {
      return status;
    }
  
    public void setStatus(STATUS status) {
      this.status = status;
    }
    
    @Override
    public Integer getModifiedFiles() {
      return modifiedFiles;
    }
  
    public void setModifiedFiles(Integer modifiedFiles) {
      this.modifiedFiles = modifiedFiles;
    }
  
    @Override
    public String getRepository() {
      return repository;
    }
  
    public void setRepository(String repository) {
      this.repository = repository;
    }
  
    @Override
    public String getBranch() {
      return branch;
    }
  
    public void setBranch(String branch) {
      this.branch = branch;
    }
  }
}
