/*
 * This file is part of HopsWorks
 *
 * Copyright (C) 2013 - 2018, Logical Clocks AB and RISE SICS AB. All rights reserved.
 *
 * HopsWorks is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * HopsWorks is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with HopsWorks.  If not, see <http://www.gnu.org/licenses/>.
 */

package io.hops.hopsworks.common.dao.user.cluster;

import io.hops.hopsworks.common.dao.user.Users;
import java.io.Serializable;
import java.util.Date;
import javax.persistence.Basic;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlTransient;
import org.codehaus.jackson.annotate.JsonIgnore;

@Entity
@Table(name = "cluster_cert",
    catalog = "hopsworks",
    schema = "")
@XmlRootElement
@NamedQueries({
  @NamedQuery(name = "ClusterCert.findAll",
      query = "SELECT c FROM ClusterCert c")
  ,
    @NamedQuery(name = "ClusterCert.findById",
      query = "SELECT c FROM ClusterCert c WHERE c.id = :id")
  ,
    @NamedQuery(name = "ClusterCert.findByCommonName",
      query
      = "SELECT c FROM ClusterCert c WHERE c.commonName = :commonName")
  ,
    @NamedQuery(name = "ClusterCert.findByOrganizationName",
      query
      = "SELECT c FROM ClusterCert c WHERE c.organizationName = :organizationName")
  ,
    @NamedQuery(name = "ClusterCert.findByOrganizationalUnitName",
      query
      = "SELECT c FROM ClusterCert c WHERE c.organizationalUnitName = :organizationalUnitName")
  ,
    @NamedQuery(name = "ClusterCert.findByOrgUnitNameAndOrgName",
      query
      = "SELECT c FROM ClusterCert c WHERE c.organizationName = :organizationName AND "
      + "c.organizationalUnitName = :organizationalUnitName")
  ,
    @NamedQuery(name = "ClusterCert.findBySerialNumber",
      query
      = "SELECT c FROM ClusterCert c WHERE c.serialNumber = :serialNumber")
  ,
    @NamedQuery(name = "ClusterCert.findByRegistrationStatus",
      query
      = "SELECT c FROM ClusterCert c WHERE c.registrationStatus = :registrationStatus")
  ,
    @NamedQuery(name = "ClusterCert.findByAgent",
      query
      = "SELECT c FROM ClusterCert c WHERE c.agentId = :agentId")
  ,
    @NamedQuery(name = "ClusterCert.findByValidationKeyDate",
      query
      = "SELECT c FROM ClusterCert c WHERE c.validationKeyDate = :validationKeyDate")})
public class ClusterCert implements Serializable {

  private static final long serialVersionUID = 1L;
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Basic(optional = false)
  @Column(name = "id")
  private Integer id;
  @Basic(optional = false)
  @NotNull
  @Size(min = 1,
      max = 64) //RFC3280 Naming attributes of type X520CommonName
  @Column(name = "common_name")
  private String commonName;
  @Basic(optional = false)
  @NotNull
  @Size(min = 1,
      max = 64)
  @Column(name = "organization_name")
  private String organizationName;
  @Basic(optional = false)
  @NotNull
  @Size(min = 1,
      max = 64)
  @Column(name = "organizational_unit_name")
  private String organizationalUnitName;
  @Column(name = "serial_number")
  private String serialNumber;
  @Basic(optional = false)
  @NotNull
  @Size(min = 1,
      max = 45)
  @Enumerated(EnumType.STRING)
  @Column(name = "registration_status")
  private RegistrationStatusEnum registrationStatus;
  @Size(min = 1,
      max = 128)
  @Column(name = "validation_key")
  private String validationKey;
  @Column(name = "validation_key_date")
  @Temporal(TemporalType.TIMESTAMP)
  private Date validationKeyDate;
  @Column(name = "registration_date")
  @Temporal(TemporalType.TIMESTAMP)
  private Date registrationDate;
  @JoinColumn(name = "agent_id",
      referencedColumnName = "uid")
  @ManyToOne(optional = false)
  private Users agentId;

  public ClusterCert() {
  }

  public ClusterCert(Integer id) {
    this.id = id;
  }

  public ClusterCert(String commonName, String organizationName, String organizationalUnitName,
      RegistrationStatusEnum registrationStatus, Users agentId) {
    this.commonName = commonName;
    this.organizationName = organizationName;
    this.organizationalUnitName = organizationalUnitName;
    this.registrationStatus = registrationStatus;
    this.agentId = agentId;
  }

  public Integer getId() {
    return id;
  }

  public void setId(Integer id) {
    this.id = id;
  }

  public String getCommonName() {
    return commonName;
  }

  public void setCommonName(String commonName) {
    this.commonName = commonName;
  }

  public String getOrganizationName() {
    return organizationName;
  }

  public void setOrganizationName(String organizationName) {
    this.organizationName = organizationName;
  }

  public String getOrganizationalUnitName() {
    return organizationalUnitName;
  }

  public void setOrganizationalUnitName(String organizationalUnitName) {
    this.organizationalUnitName = organizationalUnitName;
  }

  public String getSerialNumber() {
    return serialNumber;
  }

  public void setSerialNumber(String serialNumber) {
    this.serialNumber = serialNumber;
  }

  public RegistrationStatusEnum getRegistrationStatus() {
    return registrationStatus;
  }

  public void setRegistrationStatus(RegistrationStatusEnum registrationStatus) {
    this.registrationStatus = registrationStatus;
  }

  @XmlTransient
  @JsonIgnore
  public String getValidationKey() {
    return validationKey;
  }

  public void setValidationKey(String validationKey) {
    this.validationKey = validationKey;
  }

  public Date getValidationKeyDate() {
    return validationKeyDate;
  }

  public void setValidationKeyDate(Date validationKeyDate) {
    this.validationKeyDate = validationKeyDate;
  }

  public Date getRegistrationDate() {
    return registrationDate;
  }

  public void setRegistrationDate(Date registrationDate) {
    this.registrationDate = registrationDate;
  }

  @XmlTransient
  @JsonIgnore
  public Users getAgentId() {
    return agentId;
  }

  public void setAgentId(Users agentId) {
    this.agentId = agentId;
  }

  @Override
  public int hashCode() {
    int hash = 0;
    hash += (id != null ? id.hashCode() : 0);
    return hash;
  }

  @Override
  public boolean equals(Object object) {
    // TODO: Warning - this method won't work in the case the id fields are not set
    if (!(object instanceof ClusterCert)) {
      return false;
    }
    ClusterCert other = (ClusterCert) object;
    if ((this.id == null && other.id != null) || (this.id != null && !this.id.equals(other.id))) {
      return false;
    }
    return true;
  }

  @Override
  public String toString() {
    return "io.hops.hopsworks.common.dao.user.cluster.ClusterCert[ id=" + id + " ]";
  }

}
