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

package io.hops.hopsworks.persistence.entity.airflow;

import javax.persistence.EmbeddedId;
import javax.persistence.Entity;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.Table;
import java.io.Serializable;

@Entity
@Table(name = "materialized_jwt", catalog = "hopsworks")
@NamedQueries({
    @NamedQuery(name = "MaterializedJWT.findAll",
                query = " SELECT a FROM MaterializedJWT a"),
    @NamedQuery(name = "MaterializedJWT.findByUsage",
                query = "SELECT a FROM MaterializedJWT a WHERE a.identifier.usage = :usage")
  })
public class MaterializedJWT implements Serializable {
  private static final long serialVersionUID = 1L;
  
  @EmbeddedId
  private MaterializedJWTID identifier;
  
  public MaterializedJWT() {
  }
  
  public MaterializedJWT(MaterializedJWTID identifier) {
    this.identifier = identifier;
  }
  
  public MaterializedJWTID getIdentifier() {
    return identifier;
  }
  
  public void setIdentifier(MaterializedJWTID identifier) {
    this.identifier = identifier;
  }
}
