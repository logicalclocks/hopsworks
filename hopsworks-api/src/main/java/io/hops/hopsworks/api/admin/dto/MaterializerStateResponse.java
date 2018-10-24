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

package io.hops.hopsworks.api.admin.dto;

import javax.xml.bind.annotation.XmlRootElement;
import java.io.Serializable;
import java.util.List;
import java.util.Map;

@XmlRootElement
public class MaterializerStateResponse implements Serializable {
  private static final long serialVersionUID = 1L;
  
  private List<CryptoMaterial> localMaterializedState;
  private List<CryptoMaterial> remoteMaterializedState;
  private List<CryptoMaterial> scheduledRemovals;
  private Map<String, Boolean> materialKeyLocks;
  
  public MaterializerStateResponse(
      List<CryptoMaterial> localMaterializedState, List<CryptoMaterial> remoteMaterializedState,
      List<CryptoMaterial> scheduledRemovals, Map<String, Boolean> materialKeyLocks) {
    this.localMaterializedState = localMaterializedState;
    this.remoteMaterializedState = remoteMaterializedState;
    this.scheduledRemovals = scheduledRemovals;
    this.materialKeyLocks = materialKeyLocks;
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
  
  public Map<String, Boolean> getMaterialKeyLocks() {
    return materialKeyLocks;
  }
  
  public void setMaterialKeyLocks(Map<String, Boolean> materialKeyLocks) {
    this.materialKeyLocks = materialKeyLocks;
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

