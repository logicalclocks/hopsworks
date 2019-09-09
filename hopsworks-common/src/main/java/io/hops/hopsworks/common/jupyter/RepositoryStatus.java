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
  Integer getUntrackedFiles();
  String getActiveBranch();
  String getLastCommit();
  
  public final class UnmodifiableRepositoryStatus implements RepositoryStatus {
  
    private final STATUS status = STATUS.UNINITIALIZED;
    private final Integer untrackedFiles = -1;
    private final String activeBranch = "UNKNOWN";
    private final String lastCommit = "UNKNOWN";
    
    @Override
    public STATUS getStatus() {
      return status;
    }
  
    @Override
    public Integer getUntrackedFiles() {
      return untrackedFiles;
    }
  
    @Override
    public String getActiveBranch() {
      return activeBranch;
    }
  
    @Override
    public String getLastCommit() {
      return lastCommit;
    }
  }
  
  public class ModifiableRepositoryStatus implements RepositoryStatus {
    private STATUS status;
    private Integer untrackedFiles;
    private String activeBranch;
    private String lastCommit;
    
    public ModifiableRepositoryStatus() {}
    
    @Override
    public STATUS getStatus() {
      return status;
    }
  
    public void setStatus(STATUS status) {
      this.status = status;
    }
    
    @Override
    public Integer getUntrackedFiles() {
      return untrackedFiles;
    }
  
    public void setUntrackedFiles(Integer untrackedFiles) {
      this.untrackedFiles = untrackedFiles;
    }
    
    @Override
    public String getActiveBranch() {
      return activeBranch;
    }
    
    public void setActiveBranch(String activeBranch) {
      this.activeBranch = activeBranch;
    }
  
    @Override
    public String getLastCommit() {
      return lastCommit;
    }
  
    public void setLastCommit(String lastCommit) {
      this.lastCommit = lastCommit;
    }
  }
}
