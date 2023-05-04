/*
 * This file is part of Hopsworks
 * Copyright (C) 2021, Logical Clocks AB. All rights reserved
 *
 * Hopsworks is free software: you can redistribute it and/or modify it under the terms of
 * the GNU Affero General Public License as published by the Free Software Foundation,
 * either version 3 of the License, or (at your option) any later version.
 *
 * Hopsworks is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 *  without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR
 *  PURPOSE.  See the GNU Affero General Public License for more details.
 *
 *  You should have received a copy of the GNU Affero General Public License along with this program.
 *  If not, see <https://www.gnu.org/licenses/>.
 */

package io.hops.hopsworks.persistence.entity.featurestore.transformationFunction;
import io.hops.hopsworks.persistence.entity.featurestore.Featurestore;
import io.hops.hopsworks.persistence.entity.user.Users;

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
 * Entity class representing the training_dataset table in Hopsworks database.
 * An instance of this class represents a row in the database.
 */
@Entity
@Table(name = "transformation_function", catalog = "hopsworks")
@XmlRootElement
@NamedQueries({
    @NamedQuery(name = "TransformationFunction.findAll", query = "SELECT tsfn FROM " +
        "TransformationFunction tsfn"),
    @NamedQuery(name = "TransformationFunction.findById",
        query = "SELECT tsfn FROM TransformationFunction tsfn WHERE " +
            "tsfn.id = :id"),
    @NamedQuery(name = "TransformationFunction.findByFeaturestore",
        query = "SELECT tsfn FROM TransformationFunction tsfn WHERE " +
            "tsfn.featurestore = :featurestore"),
    @NamedQuery(name = "TransformationFunction.countByFeaturestore",
        query = "SELECT COUNT(tsfn.featurestore) FROM TransformationFunction tsfn " +
            "WHERE tsfn.featurestore = :featurestore"),
    @NamedQuery(name = "TransformationFunction.findByNameVersionAndFeaturestore",
        query = "SELECT tsfn FROM TransformationFunction tsfn WHERE tsfn.featurestore = :featurestore " +
            "AND tsfn.name= :name AND tsfn.version = :version"),
    @NamedQuery(name = "TransformationFunction.findByNameAndFeaturestoreOrderedDescVersion",
        query = "SELECT tsfn FROM TransformationFunction tsfn WHERE tsfn.featurestore = :featurestore " +
            "AND tsfn.name = :name ORDER BY tsfn.version DESC")})
public class TransformationFunction implements Serializable  {
  private static final long serialVersionUID = 1L;
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Basic(optional = false)
  @Column(name = "id")
  private Integer id;
  @Column(name = "name")
  private String name;
  @Column(name = "output_type")
  private String outputType;
  @Column(name = "version")
  private Integer version;
  @JoinColumn(name = "feature_store_id", referencedColumnName = "id")
  @ManyToOne(optional = false)
  private Featurestore featurestore;
  @Basic(optional = false)
  @Column(name = "created")
  @Temporal(TemporalType.TIMESTAMP)
  private Date created;
  @JoinColumn(name = "creator", referencedColumnName = "uid")
  @ManyToOne(optional = false)
  private Users creator;

  public TransformationFunction() {}

  // for testing
  public TransformationFunction(String name, Integer version) {
    this.name = name;
    this.version = version;
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

  public String getOutputType() {
    return outputType;
  }

  public void setOutputType(String outputType) {
    this.outputType = outputType;
  }

  public Integer getVersion() {
    return version;
  }

  public void setVersion(Integer version) {
    this.version = version;
  }

  public Featurestore getFeaturestore() {
    return featurestore;
  }

  public void setFeaturestore(Featurestore featurestore) {
    this.featurestore = featurestore;
  }

  public Date getCreated() {
    return created;
  }

  public void setCreated(Date created) {
    this.created = created;
  }

  public Users getCreator() {
    return creator;
  }

  public void setCreator(Users creator) {
    this.creator = creator;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    TransformationFunction that = (TransformationFunction) o;

    return Objects.equals(id, that.id);
  }

  @Override
  public int hashCode() {
    return id != null ? id.hashCode() : 0;
  }
}
