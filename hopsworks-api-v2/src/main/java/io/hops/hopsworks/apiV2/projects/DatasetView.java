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
package io.hops.hopsworks.apiV2.projects;

import io.hops.hopsworks.common.dao.dataset.Dataset;

import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement
public class DatasetView {
  private Integer id;
  private String name;
  private String description;
  private boolean isShared;
  
  public DatasetView(){
  }
  
  public DatasetView(Dataset ds){
    this.id = ds.getId();
    this.name = ds.getName();
    this.description = ds.getDescription();
    this.isShared = ds.isShared();
  }
  
  public Integer getId() {
    return id;
  }
  
  public void setId(Integer id) {
    this.id = id;
  }
  
  public String getName() {
    return name;
  }
  
  public void setName(String name) {
    this.name = name;
  }
  
  public String getDescription() {
    return description;
  }
  
  public void setDescription(String description) {
    this.description = description;
  }
  
  public boolean isShared() {
    return isShared;
  }
  
  public void setShared(boolean shared) {
    isShared = shared;
  }
}
