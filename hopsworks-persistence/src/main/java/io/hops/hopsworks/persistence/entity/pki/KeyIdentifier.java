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

import javax.persistence.Column;
import javax.persistence.Embeddable;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import java.io.Serializable;

@Embeddable
public class KeyIdentifier implements Serializable {
  private static final long serialVersionUID = 1L;

  @Column(nullable = false)
  private String owner;

  @Enumerated(EnumType.ORDINAL)
  @Column(nullable = false)
  private PKIKey.Type type;

  public KeyIdentifier() {
  }

  public KeyIdentifier(String owner, PKIKey.Type type) {
    this.owner = owner;
    this.type = type;
  }

  public String getOwner() {
    return owner;
  }

  public void setOwner(String owner) {
    this.owner = owner;
  }

  public PKIKey.Type getType() {
    return type;
  }

  public void setType(PKIKey.Type type) {
    this.type = type;
  }
}
