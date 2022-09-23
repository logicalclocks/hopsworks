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
import javax.persistence.Embeddable;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import java.io.Serializable;

@Embeddable
public class PKICertificateId implements Serializable {
  private static final long serialVersionUID = 1L;

  @Enumerated(EnumType.ORDINAL)
  private PKICertificate.Status status;

  @Basic(optional = false)
  @Column(nullable = false)
  private String subject;

  public PKICertificateId() {}

  public PKICertificateId(PKICertificate.Status status, String subject) {
    this.status = status;
    this.subject = subject;
  }

  public PKICertificate.Status getStatus() {
    return status;
  }

  public void setStatus(PKICertificate.Status status) {
    this.status = status;
  }

  public String getSubject() {
    return subject;
  }

  public void setSubject(String subject) {
    this.subject = subject;
  }
}
