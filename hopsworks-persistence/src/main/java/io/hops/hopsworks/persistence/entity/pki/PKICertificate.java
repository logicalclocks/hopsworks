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
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.Table;
import java.io.Serializable;
import java.util.Date;

/*
CREATE TABLE IF NOT EXISTS `pki_certificate` (
  `ca` TINYINT NOT NULL,
  `serial_number` BIGINT NOT NULL,
  `status` TINYINT NOT NULL,
  `subject` VARCHAR(255) NOT NULL,
  `certificate` VARBINARY(10000),
  `not_before` TIMESTAMP NOT NULL,
  `not_after` TIMESTAMP NOT NULL,
  PRIMARY KEY(`status`, `subject`) USING HASH,
  KEY `sn_index` (`serial_number`)
) ENGINE=ndbcluster DEFAULT CHARSET=latin1 COLLATE=latin1_general_cs;
 */
@Entity
@Table(name = "pki_certificate", catalog = "hopsworks")
@NamedQueries({
        @NamedQuery(name = "PKICertificate.findBySerialNumber",
                  query = "SELECT c FROM PKICertificate c WHERE c.serialNumber = :serialNumber"),
        @NamedQuery(name = "PKICertificate.findByStatusAndSubject",
                  query = "SELECT c FROM PKICertificate c WHERE c.certificateId.status = :status AND c.certificateId" +
                      ".subject = :subject"),
        @NamedQuery(name = "PKICertificate.findSubjectByStatusAndCN",
                  query = "SELECT c.certificateId.subject FROM PKICertificate c WHERE c.certificateId.status = " +
                      ":status AND c.certificateId.subject LIKE CONCAT('%CN=', :cn, '%')")
  })
public class PKICertificate implements Serializable {
  private static final long serialVersionUID = 1L;

  public enum Status {
    VALID,
    REVOKED,
    EXPIRED
  }

  @EmbeddedId
  private PKICertificateId certificateId;

  @Basic(optional = false)
  @Enumerated(EnumType.ORDINAL)
  private CAType ca;

  @Basic(optional = false)
  @Column(name = "serial_number", nullable = false)
  private Long serialNumber;

  @Basic(optional = false)
  @Column(name = "not_before", nullable = false)
  private Date notBefore;

  @Basic(optional = false)
  @Column(name = "not_after", nullable = false)
  private Date notAfter;

  private byte[] certificate;

  public PKICertificate() {}

  public PKICertificate(PKICertificateId certificateId, CAType ca, Long serialNumber, byte[] certificate,
      Date notBefore, Date notAfter) {
    this.certificateId = certificateId;
    this.ca = ca;
    this.serialNumber = serialNumber;
    this.certificate = certificate;
    this.notBefore = notBefore;
    this.notAfter = notAfter;
  }

  public PKICertificateId getCertificateId() {
    return certificateId;
  }

  public void setCertificateId(PKICertificateId certificateId) {
    this.certificateId = certificateId;
  }

  public Long getSerialNumber() {
    return serialNumber;
  }

  public void setSerialNumber(Long serialNumber) {
    this.serialNumber = serialNumber;
  }


  public byte[] getCertificate() {
    return certificate;
  }

  public void setCertificate(byte[] certificate) {
    this.certificate = certificate;
  }

  public CAType getCa() {
    return ca;
  }

  public void setCa(CAType ca) {
    this.ca = ca;
  }

  public Date getNotBefore() {
    return notBefore;
  }

  public void setNotBefore(Date notBefore) {
    this.notBefore = notBefore;
  }

  public Date getNotAfter() {
    return notAfter;
  }

  public void setNotAfter(Date notAfter) {
    this.notAfter = notAfter;
  }
}
