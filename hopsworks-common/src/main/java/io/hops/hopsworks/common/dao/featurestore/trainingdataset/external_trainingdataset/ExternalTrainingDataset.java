/*
 * This file is part of Hopsworks
 * Copyright (C) 2019, Logical Clocks AB. All rights reserved
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

package io.hops.hopsworks.common.dao.featurestore.trainingdataset.external_trainingdataset;

import io.hops.hopsworks.common.dao.featurestore.storageconnector.s3.FeaturestoreS3Connector;

import javax.persistence.Basic;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.Table;
import javax.xml.bind.annotation.XmlRootElement;
import java.io.Serializable;

/**
 * Entity class representing the external_training_dataset table in Hopsworks database.
 * An instance of this class represents a row in the database.
 */
@Entity
@Table(name = "external_training_dataset", catalog = "hopsworks")
@XmlRootElement
@NamedQueries({
    @NamedQuery(name = "ExternalTrainingDataset.findAll", query = "SELECT s3Td FROM " +
      "ExternalTrainingDataset s3Td"),
    @NamedQuery(name = "ExternalTrainingDataset.findById",
        query = "SELECT s3Td FROM ExternalTrainingDataset s3Td WHERE s3Td.id = :id")})
public class ExternalTrainingDataset implements Serializable {
  private static final long serialVersionUID = 1L;
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Basic(optional = false)
  @Column(name = "id")
  private Integer id;
  @JoinColumn(name = "s3_connector_id", referencedColumnName = "id")
  private FeaturestoreS3Connector featurestoreS3Connector;
  @Basic(optional = false)
  @Column(name = "name")
  private String name;
  
  
  public Integer getId() {
    return id;
  }
  
  public void setId(Integer id) {
    this.id = id;
  }
  
  public FeaturestoreS3Connector getFeaturestoreS3Connector() {
    return featurestoreS3Connector;
  }
  
  public void setFeaturestoreS3Connector(
    FeaturestoreS3Connector featurestoreS3Connector) {
    this.featurestoreS3Connector = featurestoreS3Connector;
  }
  
  public String getName() {
    return name;
  }
  
  public void setName(String name) {
    this.name = name;
  }
  
  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof ExternalTrainingDataset)) {
      return false;
    }
    
    ExternalTrainingDataset that = (ExternalTrainingDataset) o;
    
    if (!id.equals(that.id)) {
      return false;
    }
    if (!featurestoreS3Connector.equals(that.featurestoreS3Connector)) {
      return false;
    }
    return name.equals(that.name);
  }
  
  @Override
  public int hashCode() {
    int result = id.hashCode();
    result = 31 * result + featurestoreS3Connector.hashCode();
    result = 31 * result + name.hashCode();
    return result;
  }
}
