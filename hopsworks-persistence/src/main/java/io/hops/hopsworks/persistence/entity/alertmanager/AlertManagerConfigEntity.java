/*
 * This file is part of Hopsworks
 * Copyright (C) 2021, Logical Clocks AB. All rights reserved
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
package io.hops.hopsworks.persistence.entity.alertmanager;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.json.JSONObject;

import javax.persistence.Basic;
import javax.persistence.Column;
import javax.persistence.Convert;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;
import javax.validation.constraints.NotNull;
import javax.xml.bind.annotation.XmlRootElement;
import java.io.Serializable;
import java.util.Date;

@Entity
@Table(name = "alert_manager_config",
    catalog = "hopsworks",
    schema = "")
@XmlRootElement
@NamedQueries({
  @NamedQuery(name = "AlertManagerConfigEntity.findAllSortedByCreated",
      query = "SELECT a FROM AlertManagerConfigEntity a ORDER BY a.created DESC")
  ,
    @NamedQuery(name = "AlertManagerConfigEntity.findById",
      query = "SELECT a FROM AlertManagerConfigEntity a WHERE a.id = :id")
  ,
    @NamedQuery(name = "AlertManagerConfigEntity.findByCreated",
      query
      = "SELECT a FROM AlertManagerConfigEntity a WHERE a.created = :created")})
public class AlertManagerConfigEntity implements Serializable {

  private static final long serialVersionUID = 1L;
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Basic(optional = false)
  @Column(name = "id")
  private Integer id;
  @Basic(optional = false)
  @NotNull
  @Column(name = "content")
  @Convert(converter = ConfigConverter.class)
  @SuppressFBWarnings(justification="Converter", value="SE_BAD_FIELD")
  private JSONObject content;
  @Basic(optional = false)
  @NotNull
  @Column(name = "created")
  @Temporal(TemporalType.TIMESTAMP)
  private Date created;

  public AlertManagerConfigEntity() {
  }

  public AlertManagerConfigEntity(Integer id) {
    this.id = id;
  }

  public AlertManagerConfigEntity(Integer id, JSONObject content, Date created) {
    this.id = id;
    this.content = content;
    this.created = created;
  }

  public Integer getId() {
    return id;
  }

  public void setId(Integer id) {
    this.id = id;
  }

  public JSONObject getContent() {
    return content;
  }

  public void setContent(JSONObject content) {
    this.content = content;
  }

  public Date getCreated() {
    return created;
  }

  public void setCreated(Date created) {
    this.created = created;
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
    if (!(object instanceof AlertManagerConfigEntity)) {
      return false;
    }
    AlertManagerConfigEntity other = (AlertManagerConfigEntity) object;
    if ((this.id == null && other.id != null) || (this.id != null && !this.id.equals(other.id))) {
      return false;
    }
    return true;
  }

  @Override
  public String toString() {
    return "io.hops.hopsworks.persistence.entity.alertmanager.AlertManagerConfig[ id=" + id + " ]";
  }

}
