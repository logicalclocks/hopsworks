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

package io.hops.hopsworks.api.project;

import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement
public class FileTemplateDTO {

  private String inodePath;
  private int templateId;

  public FileTemplateDTO() {
  }

  public FileTemplateDTO(String inodePath, int templateId) {
    this.inodePath = inodePath;
    this.templateId = templateId;
  }

  public void setInodePath(String inodePath) {
    this.inodePath = inodePath;
  }

  public void setTemplateId(int templateId) {
    this.templateId = templateId;
  }

  public String getInodePath() {
    return this.inodePath;
  }

  public int getTemplateId() {
    return this.templateId;
  }

  @Override
  public String toString() {
    return "FileTemplateDTO{" + "inodePath=" + this.inodePath + ", templateId="
            + this.templateId + "}";
  }
}
