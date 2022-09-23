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

import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.Id;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.Table;
import javax.validation.constraints.NotNull;
import java.io.Serializable;

/*
CREATE TABLE IF NOT EXISTS `pki_serial_number` (
  `type` VARCHAR(20) NOT NULL,
  `number` BIGINT NOT NULL,
  PRIMARY KEY(`type`) USING HASH
) ENGINE=ndbcluster DEFAULT CHARSET=latin1 COLLATE=latin1_general_cs;
 */
@Entity
@Table(name = "pki_serial_number", catalog = "hopsworks")
@NamedQueries({
        @NamedQuery(name = "SerialNumber.forCAType",
                  query = "SELECT sn FROM SerialNumber sn WHERE sn.type = :type")
  })
public class SerialNumber implements Serializable {
  private static final long serialVersionUID = 1L;


  @Id
  @Enumerated(EnumType.STRING)
  private CAType type;

  @NotNull
  private Long number;


  public SerialNumber() {
  }

  public SerialNumber(CAType type,Long number) {
    this.type = type;
    this.number = number;
  }

  public CAType getType() {
    return type;
  }

  public void setType(CAType type) {
    this.type = type;
  }

  public Long getNumber() {
    return number;
  }

  public void setNumber(Long number) {
    this.number = number;
  }
}
