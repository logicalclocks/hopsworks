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
@Table(name = "hopsworks.address")
@XmlRootElement
@NamedQueries({
  @NamedQuery(name = "Address.findAll",
          query = "SELECT a FROM Address a"),
  @NamedQuery(name = "Address.findByUid",
          query = "SELECT a FROM Address a WHERE a.uid = :uid"),
  @NamedQuery(name = "Address.findByAddress1",
          query = "SELECT a FROM Address a WHERE a.address1 = :address1"),
  @NamedQuery(name = "Address.findByAddress2",
          query = "SELECT a FROM Address a WHERE a.address2 = :address2"),
  @NamedQuery(name = "Address.findByAddress3",
          query = "SELECT a FROM Address a WHERE a.address3 = :address3"),
  @NamedQuery(name = "Address.findByCity",
          query = "SELECT a FROM Address a WHERE a.city = :city"),
  @NamedQuery(name = "Address.findByState",
          query = "SELECT a FROM Address a WHERE a.state = :state"),
  @NamedQuery(name = "Address.findByCountry",
          query = "SELECT a FROM Address a WHERE a.country = :country"),
  @NamedQuery(name = "Address.findByPostalcode",
          query = "SELECT a FROM Address a WHERE a.postalcode = :postalcode"),
  @NamedQuery(name = "Address.findByAddressId",
          query = "SELECT a FROM Address a WHERE a.addressId = :addressId")})
public class Address implements Serializable {

  private static final long serialVersionUID = 1L;
  @Size(max = 50)
  @Column(name = "address1")
  private String address1;
  @Size(max = 50)
  @Column(name = "address2")
  private String address2;
  @Size(max = 50)
  @Column(name = "address3")
  private String address3;
  @Size(max = 30)
  @Column(name = "city")
  private String city;
  @Size(max = 100)
  @Column(name = "state")
  private String state;
  @Size(max = 40)
  @Column(name = "country")
  private String country;
  @Size(max = 10)
  @Column(name = "postalcode")
  private String postalcode;
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Basic(optional = false)
  @Column(name = "address_id")
  private Long addressId;
  @JoinColumn(name = "uid",
          referencedColumnName = "uid")
  @OneToOne(optional = false)
  private Users uid;

  public Address() {
  }

  public Address(Long addressId) {
    this.addressId = addressId;
  }

  public String getAddress1() {
    return address1;
  }

  public void setAddress1(String address1) {
    this.address1 = address1;
  }

  public String getAddress2() {
    return address2;
  }

  public void setAddress2(String address2) {
    this.address2 = address2;
  }

  public String getAddress3() {
    return address3;
  }

  public void setAddress3(String address3) {
    this.address3 = address3;
  }

  public String getCity() {
    return city;
  }

  public void setCity(String city) {
    this.city = city;
  }

  public String getState() {
    return state;
  }

  public void setState(String state) {
    this.state = state;
  }

  public String getCountry() {
    return country;
  }

  public void setCountry(String country) {
    this.country = country;
  }

  public String getPostalcode() {
    return postalcode;
  }

  public void setPostalcode(String postalcode) {
    this.postalcode = postalcode;
  }

  public Long getAddressId() {
    return addressId;
  }

  public void setAddressId(Long addressId) {
    this.addressId = addressId;
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
    hash += (addressId != null ? addressId.hashCode() : 0);
    return hash;
  }

  @Override
  public boolean equals(Object object) {
    // TODO: Warning - this method won't work in the case the id fields are not set
    if (!(object instanceof Address)) {
      return false;
    }
    Address other = (Address) object;
    if ((this.addressId == null && other.addressId != null) || (this.addressId
            != null && !this.addressId.equals(other.addressId))) {
      return false;
    }
    return true;
  }

  @Override
  public String toString() {
    return "se.kth.bbc.security.ua.model.Address[ addressId=" + addressId + " ]";
  }

}
