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

package io.hops.hopsworks.common.jobs.yarn;

import javax.xml.bind.annotation.XmlRootElement;

/**
 * Class containing LocalResource properties required by YarnJobs.
 * <p>
 */
@XmlRootElement
public class LocalResourceDTO {

  private String name;
  private String path;
  private String visibility;
  private String type;
  //User provided pattern is used if the LocalResource is of type Pattern
  private String pattern;

  public LocalResourceDTO() {
  }

  public LocalResourceDTO(String name, String path, String visibility,
          String type, String pattern) {
    this.name = name;
    this.path = path;
    this.visibility = visibility;
    this.type = type;
    this.pattern = pattern;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public String getPath() {
    return path;
  }

  public void setPath(String path) {
    this.path = path;
  }

  public String getVisibility() {
    return visibility;
  }

  public void setVisibility(String visibility) {
    this.visibility = visibility;
  }

  public String getType() {
    return type;
  }

  public void setType(String type) {
    this.type = type;
  }

  public String getPattern() {
    return pattern;
  }

  public void setPattern(String pattern) {
    this.pattern = pattern;
  }

  @Override
  public String toString() {
    return "LocalResourceDTO{" + "name=" + name + ", path=" + path
            + ", visibility=" + visibility + ", type=" + type + ", pattern="
            + pattern + '}';
  }

}
