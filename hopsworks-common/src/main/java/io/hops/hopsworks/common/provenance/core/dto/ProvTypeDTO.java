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

import io.hops.hopsworks.persistence.entity.hdfs.inode.Inode;
import io.hops.hopsworks.common.provenance.core.ProvXAttrs;
import io.hops.hopsworks.common.provenance.core.Provenance;
import io.hops.hopsworks.exceptions.ProvenanceException;
import io.hops.hopsworks.restutils.RESTCodes;

import javax.ws.rs.QueryParam;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import java.util.Objects;
import java.util.logging.Level;

@XmlRootElement
public class ProvTypeDTO {
  @QueryParam("metaStatus")
  @XmlElement(name= ProvXAttrs.PROV_CORE_META_STATUS_KEY)
  private Inode.MetaStatus metaStatus;
  @QueryParam("provStatus")
  @XmlElement(name= ProvXAttrs.PROV_CORE_STATUS_KEY)
  private Provenance.OpStore provStatus;
  
  public ProvTypeDTO() {}
  
  public ProvTypeDTO(Inode.MetaStatus metaStatus, Provenance.OpStore provStatus) {
    this.metaStatus = metaStatus;
    this.provStatus = provStatus;
  }
  
  public Inode.MetaStatus getMetaStatus() {
    return metaStatus;
  }
  
  public void setMetaStatus(Inode.MetaStatus metaStatus) {
    this.metaStatus = metaStatus;
  }
  
  public Provenance.OpStore getProvStatus() {
    return provStatus;
  }
  
  public void setProvStatus(Provenance.OpStore provStatus) {
    this.provStatus = provStatus;
  }
  
  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    ProvTypeDTO that = (ProvTypeDTO) o;
    return metaStatus == that.metaStatus &&
      provStatus == that.provStatus;
  }
  
  @Override
  public int hashCode() {
    return Objects.hash(metaStatus, provStatus);
  }
  
  public static Provenance.Type provTypeFromString(String aux) throws ProvenanceException {
    try {
      return Provenance.Type.valueOf(aux.toUpperCase());
    } catch (IllegalArgumentException e) {
      throw new ProvenanceException(RESTCodes.ProvenanceErrorCode.UNSUPPORTED, Level.INFO,
        "malformed type", "malformed type", e);
    }
  }
}
