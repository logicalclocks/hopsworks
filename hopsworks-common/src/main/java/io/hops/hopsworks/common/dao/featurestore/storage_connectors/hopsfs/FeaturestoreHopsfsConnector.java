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

package io.hops.hopsworks.common.dao.featurestore.storage_connectors.hopsfs;

import io.hops.hopsworks.common.dao.dataset.Dataset;
import io.hops.hopsworks.common.dao.featurestore.Featurestore;

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
import javax.xml.bind.annotation.XmlRootElement;
import java.io.Serializable;

/**
 * Entity class representing the feature_store_hopsfs table in Hopsworks database.
 * An instance of this class represents a row in the database.
 */
@Entity
@Table(name = "feature_store_hopsfs_connector", catalog = "hopsworks")
@XmlRootElement
@NamedQueries({
    @NamedQuery(name = "FeaturestoreHopsfsConnector.findAll", query = "SELECT fshopsfs FROM " +
      "FeaturestoreHopsfsConnector " +
      "fshopsfs"),
    @NamedQuery(name = "FeaturestoreHopsfsConnector.findById",
        query = "SELECT fshopsfs FROM FeaturestoreHopsfsConnector fshopsfs WHERE fshopsfs.id = :id")})
public class FeaturestoreHopsfsConnector implements Serializable {
  private static final long serialVersionUID = 1L;
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Basic(optional = false)
  @Column(name = "id")
  private Integer id;
  @JoinColumn(name = "feature_store_id", referencedColumnName = "id")
  private Featurestore featurestore;
  @JoinColumn(name = "hopsfs_dataset", referencedColumnName = "id")
  @ManyToOne(optional = false)
  private Dataset hopsfsDataset;
  @Column(name = "description")
  private String description;
  @Basic(optional = false)
  @Column(name = "name")
  private String name;

  public static long getSerialVersionUID() {
    return serialVersionUID;
  }

  public Integer getId() {
    return id;
  }

  public void setId(Integer id) {
    this.id = id;
  }
  
  public Featurestore getFeaturestore() {
    return featurestore;
  }
  
  public void setFeaturestore(Featurestore featurestore) {
    this.featurestore = featurestore;
  }
  
  public Dataset getHopsfsDataset() {
    return hopsfsDataset;
  }
  
  public void setHopsfsDataset(Dataset hopsfsDataset) {
    this.hopsfsDataset = hopsfsDataset;
  }
  
  public String getDescription() {
    return description;
  }
  
  public void setDescription(String description) {
    this.description = description;
  }
  
  public String getName() {
    return name;
  }
  
  public void setName(String name) {
    this.name = name;
  }
  
  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (!(o instanceof FeaturestoreHopsfsConnector)) return false;
    
    FeaturestoreHopsfsConnector
      that = (FeaturestoreHopsfsConnector) o;
    
    if (!id.equals(that.id)) return false;
    if (!featurestore.equals(that.featurestore)) return false;
    return hopsfsDataset.equals(that.hopsfsDataset);
  }
  
  @Override
  public int hashCode() {
    int result = id.hashCode();
    result = 31 * result + featurestore.hashCode();
    result = 31 * result + hopsfsDataset.hashCode();
    return result;
  }
}
