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
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.Id;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.Table;
import java.io.Serializable;

/*
CREATE TABLE IF NOT EXISTS `pki_crl` (
  `type` VARCHAR(20) NOT NULL,
  `crl` VARBINARY(10000) NOT NULL,
  PRIMARY KEY(`type`) USING HASH
) ENGINE=ndbcluster DEFAULT CHARSET=latin1 COLLATE=latin1_general_cs;
 */
@Entity
@Table(name = "pki_crl", catalog = "hopsworks")
@NamedQueries({
    @NamedQuery(name = "PKICrl.forCAType",
              query = "SELECT crl FROM PKICrl crl WHERE crl.type = :type")
  })
public class PKICrl implements Serializable {
  private static final long serialVersionUID = 1L;

  @Id
  @Enumerated(EnumType.STRING)
  private CAType type;

  @Basic(optional = false)
  private byte[] crl;

  public PKICrl() {}

  public PKICrl(CAType type, byte[] crl) {
    this.type = type;
    this.crl = crl;
  }

  public CAType getType() {
    return type;
  }

  public void setType(CAType type) {
    this.type = type;
  }

  public byte[] getCrl() {
    return crl;
  }

  public void setCrl(byte[] crl) {
    this.crl = crl;
  }
}
