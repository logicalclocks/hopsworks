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
package io.hops.hopsworks.api.dataset.inode;

import io.hops.hopsworks.api.dataset.inode.attribute.InodeAttributeDTO;
import io.hops.hopsworks.common.api.RestDTO;
import io.hops.hopsworks.common.dataset.FilePreviewDTO;

import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement
public class InodeDTO extends RestDTO<InodeDTO> {
  
  private InodeAttributeDTO attributes;
  private FilePreviewDTO preview;
  private String zipState = "NONE";
  
  public InodeDTO() {
  }
  
  public FilePreviewDTO getPreview() {
    return preview;
  }
  
  public void setPreview(FilePreviewDTO preview) {
    this.preview = preview;
  }
  
  public InodeAttributeDTO getAttributes() {
    return attributes;
  }
  
  public void setAttributes(InodeAttributeDTO attributes) {
    this.attributes = attributes;
  }
  
  public String getZipState() {
    return zipState;
  }
  
  public void setZipState(String zipState) {
    this.zipState = zipState;
  }
  
  @Override
  public String toString() {
    return "InodeDTO{" +
      "attributes=" + attributes +
      '}';
  }
}