/*
 * This file is part of Hopsworks
 * Copyright (C) 2022, Hopsworks AB. All rights reserved
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
package io.hops.hopsworks.persistence.entity.pki;

import javax.persistence.Basic;
import javax.persistence.Column;
import javax.persistence.EmbeddedId;
import javax.persistence.Entity;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.Table;
import java.io.Serializable;

/*
CREATE TABLE IF NOT EXISTS `pki_key` (
	`owner` VARCHAR(100) NOT NULL,
	`type` TINYINT NOT NULL,
	`key` VARBINARY(8192) NOT NULL,
	PRIMARY KEY (`owner`, `type`)
	) ENGINE=ndbcluster DEFAULT CHARSET=latin1 COLLATE=latin1_general_cs;
 */
@Entity
@Table(name = "pki_key", catalog = "hopsworks")
@NamedQueries({
    @NamedQuery(name = "PKIKey.findById",
        query = "SELECT k FROM PKIKey k WHERE k.identifier.owner = :owner AND k.identifier.type = :type")
  })
public class PKIKey implements Serializable {
  public enum Type {
    PRIVATE,
    PUBLIC
  }

  private static final long serialVersionUID = 1L;

  @EmbeddedId
  private KeyIdentifier identifier;

  @Basic(optional = false)
  @Column(name = "\"key\"")
  private byte[] key;

  public PKIKey() {
  }

  public PKIKey(KeyIdentifier identifier, byte[] key) {
    this.identifier = identifier;
    this.key = key;
  }

  public KeyIdentifier getIdentifier() {
    return identifier;
  }

  public void setIdentifier(KeyIdentifier identifier) {
    this.identifier = identifier;
  }

  public byte[] getKey() {
    return key;
  }

  public void setKey(byte[] key) {
    this.key = key;
  }
}
