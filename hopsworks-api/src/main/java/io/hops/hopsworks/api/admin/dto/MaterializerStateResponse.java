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

package io.hops.hopsworks.api.admin.dto;

import javax.xml.bind.annotation.XmlRootElement;
import java.io.Serializable;
import java.util.List;

@XmlRootElement
public class MaterializerStateResponse implements Serializable {
  private static final long serialVersionUID = 1L;
  
  private List<CryptoMaterial> localMaterializedState;
  private List<CryptoMaterial> remoteMaterializedState;
  private List<CryptoMaterial> scheduledRemovals;
  
  public MaterializerStateResponse(
      List<CryptoMaterial> localMaterializedState, List<CryptoMaterial> remoteMaterializedState,
      List<CryptoMaterial> scheduledRemovals) {
    this.localMaterializedState = localMaterializedState;
    this.remoteMaterializedState = remoteMaterializedState;
    this.scheduledRemovals = scheduledRemovals;
  }
  
  public MaterializerStateResponse() {
  }
  
  public List<CryptoMaterial> getLocalMaterializedState() {
    return localMaterializedState;
  }
  
  public void setLocalMaterializedState(
      List<CryptoMaterial> localMaterializedState) {
    this.localMaterializedState = localMaterializedState;
  }
  
  public List<CryptoMaterial> getRemoteMaterializedState() {
    return remoteMaterializedState;
  }
  
  public void setRemoteMaterializedState(List<CryptoMaterial> remoteMaterializedState) {
    this.remoteMaterializedState = remoteMaterializedState;
  }
  
  public List<CryptoMaterial> getScheduledRemovals() {
    return scheduledRemovals;
  }
  
  public void setScheduledRemovals(List<CryptoMaterial> scheduledRemovals) {
    this.scheduledRemovals = scheduledRemovals;
  }
  
  public static class CryptoMaterial {
    private String user;
    private String path;
    private Integer references;
    
    public CryptoMaterial(String user, String path, Integer references) {
      this.user = user;
      this.path = path;
      this.references = references;
    }
    
    public CryptoMaterial() {
    }
    
    public String getUser() {
      return user;
    }
    
    public void setUser(String user) {
      this.user = user;
    }
    
    public String getPath() {
      return path;
    }
    
    public void setPath(String path) {
      this.path = path;
    }
    
    public Integer getReferences() {
      return references;
    }
    
    public void setReferences(Integer references) {
      this.references = references;
    }
  }
}

