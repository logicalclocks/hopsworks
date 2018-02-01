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
