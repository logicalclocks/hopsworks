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

package io.hops.hopsworks.common.dao.metadata;

import java.io.Serializable;
import javax.persistence.Basic;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.ManyToOne;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.PrimaryKeyJoinColumn;
import javax.persistence.Table;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import javax.xml.bind.annotation.XmlRootElement;

@Entity
@Table(name = "hopsworks.meta_field_predefined_values")
@XmlRootElement
@NamedQueries({
  @NamedQuery(name = "FieldPredefinedValue.findAll",
          query = "SELECT f FROM FieldPredefinedValue f"),
  @NamedQuery(name = "FieldPredefinedValue.findById",
          query = "SELECT f FROM FieldPredefinedValue f WHERE f.id = :id"),
  @NamedQuery(name = "FieldPredefinedValue.findByFieldid",
          query
          = "SELECT f FROM FieldPredefinedValue f WHERE f.fieldid = :fieldid"),
  @NamedQuery(name = "FieldPredefinedValue.findByValue",
          query
          = "SELECT f FROM FieldPredefinedValue f WHERE f.valuee = :valuee")})
public class FieldPredefinedValue implements Serializable, EntityIntf {

  private static final long serialVersionUID = 1L;
  @Id
  @GeneratedValue(strategy = GenerationType.SEQUENCE)
  @Basic(optional = false)
  @Column(name = "id")
  private Integer id;

  @Basic(optional = false)
  @NotNull
  @Column(name = "fieldid")
  private int fieldid;

  @Basic(optional = false)
  @NotNull
  @Size(min = 1,
          max = 250)
  @Column(name = "valuee")
  private String valuee;

  @ManyToOne(optional = false)
  @PrimaryKeyJoinColumn(name = "fieldid",
          referencedColumnName = "fieldid")
  private Field fields;

  public FieldPredefinedValue() {
  }

  public FieldPredefinedValue(Integer id) {
    this.id = id;
  }

  public FieldPredefinedValue(Integer id, int fieldid, String valuee) {
    this.id = id;
    this.fieldid = fieldid;
    this.valuee = valuee;
  }

  @Override
  public void copy(EntityIntf entity) {
    FieldPredefinedValue fpfv = (FieldPredefinedValue) entity;

    this.id = fpfv.getId();
    this.fieldid = fpfv.getFieldid();
    this.valuee = fpfv.getValue();
  }

  @Override
  public Integer getId() {
    return id;
  }

  @Override
  public void setId(Integer id) {
    this.id = id;
  }

  public void setField(Field fields) {
    this.fields = fields;
  }

  public Field getField() {
    return this.fields;
  }

  public int getFieldid() {
    return fieldid;
  }

  public void setFieldid(int fieldid) {
    this.fieldid = fieldid;
  }

  public String getValue() {
    return valuee;
  }

  public void setValue(String value) {
    this.valuee = value;
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
    if (!(object instanceof FieldPredefinedValue)) {
      return false;
    }
    FieldPredefinedValue other = (FieldPredefinedValue) object;
    if ((this.id == null && other.id != null) || (this.id != null && !this.id.
            equals(other.id))) {
      return false;
    }
    return true;
  }

  @Override
  public String toString() {
    return "se.kth.meta.entity.FieldPredefinedValues[ id=" + id + " ]";
  }

}
