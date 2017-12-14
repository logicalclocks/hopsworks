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

import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement
public class CreateDatasetView {
  private String description;
  private int template;
  private String name;
  private boolean searchable;
  private boolean generateReadme;
  
  public CreateDatasetView(){}
  
  public String getDescription() {
    return description;
  }
  
  public void setDescription(String description) {
    this.description = description;
  }
  
  public int getTemplate() {
    return template;
  }
  
  public void setTemplate(int template) {
    this.template = template;
  }
  
  public String getName() {
    return name;
  }
  
  public void setName(String name) {
    this.name = name;
  }
  
  public boolean isSearchable() {
    return searchable;
  }
  
  public void setSearchable(boolean searchable) {
    this.searchable = searchable;
  }
  
  public boolean isGenerateReadme() {
    return generateReadme;
  }
  
  public void setGenerateReadme(boolean generateReadme) {
    this.generateReadme = generateReadme;
  }
}
