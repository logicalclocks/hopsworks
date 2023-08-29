/*
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
 */

package io.hops.hopsworks.persistence.entity.featurestore;

import io.hops.hopsworks.persistence.entity.project.Project;

import javax.persistence.Basic;
import javax.persistence.Column;
import javax.persistence.Entity;
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
import javax.xml.bind.annotation.XmlRootElement;
import java.io.Serializable;
import java.util.Date;
import java.util.Objects;

/**
 * Entity class representing the feature_store table in Hopsworks database.
 * An instance of this class represents a row in the database.
 */
@Entity
@Table(name = "feature_store", catalog = "hopsworks")
@XmlRootElement
@NamedQueries({
    @NamedQuery(name = "Featurestore.findAll", query = "SELECT f FROM Featurestore f"),
    @NamedQuery(name = "Featurestore.findByProject", query = "SELECT f FROM Featurestore f " +
        "WHERE f.project = :project"),
    @NamedQuery(name = "Featurestore.findById", query = "SELECT f FROM Featurestore f WHERE f.id = :id")})
public class Featurestore implements Serializable {
  
  private static final long serialVersionUID = 1L;
  
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Basic(optional = false)
  @Column(name = "id")
  private Integer id;

  @Basic(optional = false)
  @Column(name = "name")
  private String name;
  @JoinColumn(name = "project_id", referencedColumnName = "id")
  @ManyToOne(optional = false)
  private Project project;
  @Basic(optional = false)
  @Column(name = "created")
  @Temporal(TemporalType.TIMESTAMP)
  private Date created;

  public Featurestore() {
  }

  public static long getSerialVersionUID() {
    return serialVersionUID;
  }

  public Integer getId() {
    return id;
  }

  public void setId(Integer id) {
    this.id = id;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public Project getProject() {
    return project;
  }

  public void setProject(Project project) {
    this.project = project;
  }

  public Date getCreated() {
    return created;
  }

  public void setCreated(Date created) {
    this.created = created;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    Featurestore that = (Featurestore) o;
    return Objects.equals(id, that.id)
       && Objects.equals(name, that.name)
       && Objects.equals(project, that.project)
       && Objects.equals(created, that.created);
  }

  @Override
  public int hashCode() {
    return Objects.hash(id, name, project, created);
  }
}
