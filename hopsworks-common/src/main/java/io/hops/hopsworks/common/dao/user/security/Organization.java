/*
 * Changes to this file committed after and not including commit-id: ccc0d2c5f9a5ac661e60e6eaf138de7889928b8b
 * are released under the following license:
 *
 * This file is part of Hopsworks
 * Copyright (C) 2018, Logical Clocks AB. All rights reserved
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
 *
 * Changes to this file committed before and including commit-id: ccc0d2c5f9a5ac661e60e6eaf138de7889928b8b
 * are released under the following license:
 *
 * Copyright (C) 2013 - 2018, Logical Clocks AB and RISE SICS AB. All rights reserved
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this
 * software and associated documentation files (the "Software"), to deal in the Software
 * without restriction, including without limitation the rights to use, copy, modify, merge,
 * publish, distribute, sublicense, and/or sell copies of the Software, and to permit
 * persons to whom the Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or
 * substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS  OR IMPLIED, INCLUDING
 * BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL  THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM,
 * DAMAGES OR  OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package io.hops.hopsworks.common.dao.user.security;

import java.io.Serializable;
import javax.persistence.Basic;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.OneToOne;
import javax.persistence.Table;
import javax.validation.constraints.Size;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlTransient;
import org.codehaus.jackson.annotate.JsonIgnore;
import io.hops.hopsworks.common.dao.user.Users;

@Entity
@Table(name = "hopsworks.organization")
@XmlRootElement
@NamedQueries({
  @NamedQuery(name = "Organization.findAll",
          query = "SELECT o FROM Organization o"),
  @NamedQuery(name = "Organization.findById",
          query = "SELECT o FROM Organization o WHERE o.id = :id"),
  @NamedQuery(name = "Organization.findByUid",
          query = "SELECT o FROM Organization o WHERE o.uid = :uid"),
  @NamedQuery(name = "Organization.findByOrgName",
          query = "SELECT o FROM Organization o WHERE o.orgName = :orgName"),
  @NamedQuery(name = "Organization.findByWebsite",
          query = "SELECT o FROM Organization o WHERE o.website = :website"),
  @NamedQuery(name = "Organization.findByContactPerson",
          query
          = "SELECT o FROM Organization o WHERE o.contactPerson = :contactPerson"),
  @NamedQuery(name = "Organization.findByContactEmail",
          query
          = "SELECT o FROM Organization o WHERE o.contactEmail = :contactEmail"),
  @NamedQuery(name = "Organization.findByDepartment",
          query
          = "SELECT o FROM Organization o WHERE o.department = :department"),
  @NamedQuery(name = "Organization.findByPhone",
          query = "SELECT o FROM Organization o WHERE o.phone = :phone"),
  @NamedQuery(name = "Organization.findByFax",
          query = "SELECT o FROM Organization o WHERE o.fax = :fax")})
public class Organization implements Serializable {

  private static final long serialVersionUID = 1L;
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Basic(optional = false)
  @Column(name = "id")
  private Integer id;
  @Size(max = 100)
  @Column(name = "org_name")
  private String orgName;
  @Size(max = 200)
  @Column(name = "website")
  private String website;
  @Size(max = 100)
  @Column(name = "contact_person")
  private String contactPerson;
  @Size(max = 100)
  @Column(name = "contact_email")
  private String contactEmail;
  @Size(max = 100)
  @Column(name = "department")
  private String department;
  // @Pattern(regexp="^\\(?(\\d{3})\\)?[- ]?(\\d{3})[- ]?(\\d{4})$", message="Invalid 
  //phone/fax format, should be as xxx-xxx-xxxx")//if the field contains phone or fax 
  //number consider using this annotation to enforce field validation
  @Size(max = 20)
  @Column(name = "phone")
  private String phone;
  // @Pattern(regexp="^\\(?(\\d{3})\\)?[- ]?(\\d{3})[- ]?(\\d{4})$", message="Invalid 
  //phone/fax format, should be as xxx-xxx-xxxx")//if the field contains phone or fax 
  //number consider using this annotation to enforce field validation
  @Size(max = 20)
  @Column(name = "fax")
  private String fax;
  @JoinColumn(name = "uid",
          referencedColumnName = "uid")
  @OneToOne(optional = false)
  private Users uid;

  public Organization() {
  }

  public Organization(Integer id) {
    this.id = id;
  }

  public Integer getId() {
    return id;
  }

  public void setId(Integer id) {
    this.id = id;
  }

  public String getOrgName() {
    return orgName;
  }

  public void setOrgName(String orgName) {
    this.orgName = orgName;
  }

  public String getWebsite() {
    return website;
  }

  public void setWebsite(String website) {
    this.website = website;
  }

  public String getContactPerson() {
    return contactPerson;
  }

  public void setContactPerson(String contactPerson) {
    this.contactPerson = contactPerson;
  }

  public String getContactEmail() {
    return contactEmail;
  }

  public void setContactEmail(String contactEmail) {
    this.contactEmail = contactEmail;
  }

  public String getDepartment() {
    return department;
  }

  public void setDepartment(String department) {
    this.department = department;
  }

  public String getPhone() {
    return phone;
  }

  public void setPhone(String phone) {
    this.phone = phone;
  }

  public String getFax() {
    return fax;
  }

  public void setFax(String fax) {
    this.fax = fax;
  }

  @XmlTransient
  @JsonIgnore
  public Users getUid() {
    return uid;
  }

  public void setUid(Users uid) {
    this.uid = uid;
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
    if (!(object instanceof Organization)) {
      return false;
    }
    Organization other = (Organization) object;
    if ((this.id == null && other.id != null) || (this.id != null && !this.id.
            equals(other.id))) {
      return false;
    }
    return true;
  }

  @Override
  public String toString() {
    return "se.kth.bbc.security.ua.model.Organization[ id=" + id + " ]";
  }

}
