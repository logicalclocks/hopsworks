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
package io.hops.hopsworks.common.provenance.core.dto;

import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement
public class ProvDatasetDTO {
  private String name;
  private Long inodeId;
  private ProvTypeDTO status;
  
  public ProvDatasetDTO() {}
  
  public ProvDatasetDTO(String name, Long inodeId, ProvTypeDTO status) {
    this.name = name;
    this.inodeId = inodeId;
    this.status = status;
  }
  
  public String getName() {
    return name;
  }
  
  public void setName(String name) {
    this.name = name;
  }
  
  public long getInodeId() {
    return inodeId;
  }
  
  public void setInodeId(long inodeId) {
    this.inodeId = inodeId;
  }
  
  public ProvTypeDTO getStatus() {
    return status;
  }
  
  public void setStatus(ProvTypeDTO status) {
    this.status = status;
  }
}
