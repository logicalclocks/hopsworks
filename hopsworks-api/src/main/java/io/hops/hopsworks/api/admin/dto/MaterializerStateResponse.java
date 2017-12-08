/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.hops.hopsworks.api.admin.dto;

import javax.xml.bind.annotation.XmlRootElement;
import java.io.Serializable;
import java.util.List;
import java.util.Set;

@XmlRootElement
public class MaterializerStateResponse implements Serializable {
  private static final long serialVersionUID = 1L;
  
  private List<CryptoMaterial> materializedState;
  private Set<String> scheduledRemovals;
  
  public MaterializerStateResponse(
      List<CryptoMaterial> materializedState, Set<String> scheduledRemovals) {
    this.materializedState = materializedState;
    this.scheduledRemovals = scheduledRemovals;
  }
  
  public MaterializerStateResponse() {
  }
  
  public List<CryptoMaterial> getMaterializedState() {
    return materializedState;
  }
  
  public void setMaterializedState(
      List<CryptoMaterial> materializedState) {
    this.materializedState = materializedState;
  }
  
  public Set<String> getScheduledRemovals() {
    return scheduledRemovals;
  }
  
  public void setScheduledRemovals(Set<String> scheduledRemovals) {
    this.scheduledRemovals = scheduledRemovals;
  }
  
  public static class CryptoMaterial {
    private String user;
    private Integer references;
    
    public CryptoMaterial(String user, Integer references) {
      this.user = user;
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
    
    public Integer getReferences() {
      return references;
    }
    
    public void setReferences(Integer references) {
      this.references = references;
    }
  }
}

