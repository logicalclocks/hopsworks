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

import io.hops.hopsworks.common.provenance.core.ProvXAttrs;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement
public class ProvCoreDTO {
  @XmlElement(name= ProvXAttrs.PROV_CORE_TYPE_KEY)
  private ProvTypeDTO type;
  @XmlElement(name= ProvXAttrs.PROV_CORE_PROJECT_IID_KEY)
  private Long projectIId;
  
  public ProvCoreDTO() {
  }
  
  public ProvCoreDTO(ProvTypeDTO type, Long projectIId) {
    this.type = type;
    this.projectIId = projectIId;
  }
  
  public ProvTypeDTO getType() {
    return type;
  }
  
  public void setType(ProvTypeDTO type) {
    this.type = type;
  }
  
  public Long getProjectIId() {
    return projectIId;
  }
  
  public void setProjectIId(Long projectIId) {
    this.projectIId = projectIId;
  }
}
