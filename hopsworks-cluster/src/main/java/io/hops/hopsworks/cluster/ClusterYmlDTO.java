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
package io.hops.hopsworks.cluster;

import io.hops.hopsworks.common.dao.user.cluster.RegistrationStatusEnum;
import java.util.Date;
import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement
public class ClusterYmlDTO {
  private String email;
  private String commonName;
  private String organizationName;
  private String organizationalUnitName;
  private RegistrationStatusEnum registrationStatus;
  private Date registrationDate;
  private String serialNumber;

  public ClusterYmlDTO() {
  }

  public ClusterYmlDTO(String email, String commonName, String organizationName, String organizationalUnitName,
      RegistrationStatusEnum registrationStatus, Date registrationDate, String serialNumber) {
    this.email = email;
    this.commonName = commonName;
    this.organizationName = organizationName;
    this.organizationalUnitName = organizationalUnitName;
    this.registrationStatus = registrationStatus;
    this.registrationDate = registrationDate;
    this.serialNumber = serialNumber;
  }


  public String getCommonName() {
    return commonName;
  }

  public void setCommonName(String commonName) {
    this.commonName = commonName;
  }


  public String getEmail() {
    return email;
  }

  public void setEmail(String email) {
    this.email = email;
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

  public RegistrationStatusEnum getRegistrationStatus() {
    return registrationStatus;
  }

  public void setRegistrationStatus(RegistrationStatusEnum registrationStatus) {
    this.registrationStatus = registrationStatus;
  }

  public Date getRegistrationDate() {
    return registrationDate;
  }

  public void setRegistrationDate(Date registrationDate) {
    this.registrationDate = registrationDate;
  }

  public String getSerialNumber() {
    return serialNumber;
  }

  public void setSerialNumber(String serialNumber) {
    this.serialNumber = serialNumber;
  }
  
  
}
