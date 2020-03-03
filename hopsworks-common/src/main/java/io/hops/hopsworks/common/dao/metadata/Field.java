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
import java.util.LinkedList;
import java.util.List;
import javax.persistence.Basic;
import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.ManyToOne;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.OneToMany;
import javax.persistence.PrimaryKeyJoinColumn;
import javax.persistence.Table;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import javax.xml.bind.annotation.XmlRootElement;

@Entity
@Table(name = "hopsworks.meta_fields")
@XmlRootElement
@NamedQueries({
  @NamedQuery(name = "Field.findAll",
          query = "SELECT f FROM Field f"),
  @NamedQuery(name = "Field.findById",
          query = "SELECT f FROM Field f WHERE f.id = :id"),
  @NamedQuery(name = "Field.findByTableid",
          query = "SELECT f FROM Field f WHERE f.tableid = :tableid"),
  @NamedQuery(name = "Field.findByName",
          query = "SELECT f FROM Field f WHERE f.name = :name"),
  @NamedQuery(name = "Field.findByType",
          query = "SELECT f FROM Field f WHERE f.type = :type"),
  @NamedQuery(name = "Field.findByMaxsize",
          query = "SELECT f FROM Field f WHERE f.maxsize = :maxsize"),
  @NamedQuery(name = "Field.findBySearchable",
          query = "SELECT f FROM Field f WHERE f.searchable = :searchable"),
  @NamedQuery(name = "Field.findByRequired",
          query = "SELECT f FROM Field f WHERE f.required = :required"),
  @NamedQuery(name = "Field.findByFieldTypeId",
          query = "SELECT f FROM Field f WHERE f.fieldtypeid = :fieldtypeid")})
public class Field implements Serializable, EntityIntf, Comparable<Field> {

  private static final long serialVersionUID = 1L;
  @Id
  @GeneratedValue(strategy = GenerationType.SEQUENCE)
  @Basic(optional = false)
  @Column(name = "fieldid")
  private Integer id;

  @Basic(optional = false)
  @NotNull
  @Column(name = "tableid")
  private int tableid;

  @ManyToOne(optional = false)
  @PrimaryKeyJoinColumn(name = "tableid",
          referencedColumnName = "tableid")
  private MTable table;

  @OneToMany(mappedBy = "fields",
          targetEntity = RawData.class,
          fetch = FetchType.LAZY,
          cascade = {CascadeType.ALL})
  private List<RawData> raw;

  @Basic(optional = false)
  @NotNull
  @Column(name = "fieldtypeid")
  private int fieldtypeid;

  @ManyToOne(optional = false)
  @PrimaryKeyJoinColumn(name = "fieldtypeid",
          referencedColumnName = "id")
  private FieldType fieldTypes;

  @OneToMany(mappedBy = "fields",
          targetEntity = FieldPredefinedValue.class,
          fetch = FetchType.LAZY,
          cascade = CascadeType.ALL)
  private List<FieldPredefinedValue> fieldPredefinedValues;

  @Basic(optional = false)
  @NotNull
  @Size(min = 1,
          max = 50)
  @Column(name = "name")
  private String name;

  @Basic(optional = false)
  @NotNull
  @Size(min = 1,
          max = 50)
  @Column(name = "type")
  private String type;

  @Basic(optional = false)
  @NotNull
  @Column(name = "maxsize")
  private int maxsize;

  @Basic(optional = false)
  @NotNull
  @Column(name = "searchable")
  private boolean searchable;

  @Basic(optional = false)
  @NotNull
  @Column(name = "required")
  private boolean required;

  @Basic(optional = false)
  @NotNull
  @Column(name = "description")
  private String description;

  @Basic(optional = false)
  @NotNull
  @Column(name = "position")
  private int position;

  public Field() {
  }

  public Field(Integer id) {
    this.id = id;
    this.raw = new LinkedList<>();
  }

  public Field(Integer id, int tableid, String name, String type, int maxsize,
          boolean searchable, boolean required, String description,
          int fieldtypeid,
          int position) {
    this.id = id;
    this.tableid = tableid;
    this.name = name;
    this.type = type;
    this.maxsize = maxsize;
    this.searchable = searchable;
    this.required = required;
    this.description = description;
    this.fieldtypeid = fieldtypeid;
    this.position = position;
    this.raw = new LinkedList<>();
    this.fieldPredefinedValues = new LinkedList<>();
  }

  @Override
  public void copy(EntityIntf fields) {
    Field f = (Field) fields;

    this.id = f.getId();
    this.tableid = f.getTableid();
    this.name = f.getName();
    this.type = f.getType();
    this.maxsize = f.getMaxsize();
    this.searchable = f.getSearchable();
    this.required = f.getRequired();
    this.raw = f.getRawData();
    this.description = f.getDescription();
    this.fieldtypeid = f.getFieldTypeId();
    this.position = f.getPosition();
    this.fieldPredefinedValues = f.getFieldPredefinedValues();
  }

  @Override
  public Integer getId() {
    return id;
  }

  @Override
  public void setId(Integer id) {
    this.id = id;
  }

  public int getTableid() {
    return tableid;
  }

  public void setTableid(int tableid) {
    this.tableid = tableid;
  }

  public int getFieldTypeId() {
    return this.fieldtypeid;
  }

  public void setFieldTypeId(int fieldtypeid) {
    this.fieldtypeid = fieldtypeid;
  }

  public int getPosition() {
    return this.position;
  }

  public void setPosition(int position) {
    this.position = position;
  }

  /*
   * get and set the parent entities
   */
  public MTable getMTable() {
    return this.table;
  }

  public void setMTable(MTable table) {
    this.table = table;
  }

  public FieldType getFieldTypes() {
    return this.fieldTypes;
  }

  public void setFieldTypes(FieldType fieldTypes) {
    this.fieldTypes = fieldTypes;
  }

  /*
   * get and set the child entities
   */
  public List<RawData> getRawData() {
    return this.raw;
  }

  public void setRawData(List<RawData> raw) {
    this.raw = raw;
  }

  public List<FieldPredefinedValue> getFieldPredefinedValues() {
    return this.fieldPredefinedValues;
  }

  public void setFieldPredefinedValues(List<FieldPredefinedValue> pValues) {
    this.fieldPredefinedValues = pValues;
  }

  public void resetFieldPredefinedValues() {
    this.fieldPredefinedValues.clear();
  }

  public void addPredefinedValue(FieldPredefinedValue value) {
    this.fieldPredefinedValues.add(value);
    if (value != null) {
      value.setField(this);
    }
  }

  public void removePredefinedValue(FieldPredefinedValue value) {
    this.fieldPredefinedValues.remove(value);
    if (value != null) {
      value.setField(null);
    }
  }

  public void addRawData(RawData raw) {
    this.raw.add(raw);
    if (raw != null) {
      raw.setField(this);
    }
  }

  public void removeRawData(RawData raw) {
    this.raw.remove(raw);
    if (raw != null) {
      raw.setField(null);
    }
  }

  /*
   * -------------------------------
   */
  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public String getType() {
    return type;
  }

  public void setType(String type) {
    this.type = type;
  }

  public int getMaxsize() {
    return maxsize;
  }

  public void setMaxsize(int maxsize) {
    this.maxsize = maxsize;
  }

  public boolean getSearchable() {
    return searchable;
  }

  public void setSearchable(boolean searchable) {
    this.searchable = searchable;
  }

  public void setDescription(String description) {
    this.description = description;
  }

  public String getDescription() {
    return this.description;
  }

  public boolean getRequired() {
    return required;
  }

  public void setRequired(boolean required) {
    this.required = required;
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
    if (!(object instanceof Field)) {
      return false;
    }
    Field other = (Field) object;
    if ((this.id == null && other.id != null) || (this.id != null && !this.id.
            equals(other.id))) {
      return false;
    }
    return true;
  }

  @Override
  public String toString() {
    return "entity.Fields[ id=" + id + " ]";
  }

  /**
   * Fields have to be ordered according to their order, which is user defined
   * <p/>
   * @param field
   * @return
   */
  @Override
  public int compareTo(Field field) {

    //if the field has not gotten a position index on the table 
    //(it is newly created) sort it by id. Otherwise sort it by position 
    switch (this.getPosition()) {
      case 0:
        if (this.getId() > field.getId()) {
          return 1;
        } else if (this.getId() < field.getId()) {
          return -1;
        }
        break;
      default:
        if (this.getPosition() > field.getPosition()) {
          return 1;
        } else if (this.getPosition() < field.getPosition()) {
          return -1;
        }
    }

    return 0;
  }

}
