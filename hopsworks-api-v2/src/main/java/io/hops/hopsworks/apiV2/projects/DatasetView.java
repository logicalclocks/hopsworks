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
