/*
 * This file is part of Hopsworks
 * Copyright (C) 2022, Hopsworks AB. All rights reserved
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
package io.hops.hopsworks.persistence.entity.provenance;

import io.hops.hopsworks.persistence.entity.featurestore.featuregroup.Featuregroup;

import javax.persistence.Basic;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.OneToOne;
import javax.persistence.Table;
import javax.xml.bind.annotation.XmlRootElement;
import java.io.Serializable;
import java.util.Objects;

/**
 * Entity class representing the feature_group_link table in Hopsworks database.
 * An instance of this class represents a row in the table.
 */
@Entity
@Table(name = "feature_group_link", catalog = "hopsworks")
@XmlRootElement
@NamedQueries({
                @NamedQuery(name = "FeatureGroupLink.findByChildren",
                            query = "SELECT l FROM FeatureGroupLink l LEFT JOIN FETCH l.parentFeatureGroup " +
                              "WHERE l.featureGroup IN :children " +
                              "ORDER BY l.parentFeatureGroupName ASC, l.id DESC"),
                @NamedQuery(name = "FeatureGroupLink.findByParents",
                            query = "SELECT l FROM FeatureGroupLink l LEFT JOIN FETCH l.featureGroup " +
                              "WHERE l.parentFeatureGroup IN :parents " +
                              "ORDER BY l.featureGroup.name ASC, l.featureGroup.version DESC, l.id DESC")})
public class FeatureGroupLink implements ProvExplicitNode, Serializable {
  
  private static final long serialVersionUID = 1L;
  
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Basic(optional = false)
  @Column(name = "id")
  private Integer id;
  @JoinColumn(name = "feature_group_id", referencedColumnName = "id")
  @OneToOne(fetch = FetchType.LAZY)
  private Featuregroup featureGroup;
  @JoinColumn(name = "parent_feature_group_id", referencedColumnName = "id")
  @OneToOne(fetch = FetchType.LAZY)
  private Featuregroup parentFeatureGroup;
  @Basic(optional = false)
  @Column(name = "parent_feature_store")
  private String parentFeatureStore;
  @Basic(optional = false)
  @Column(name = "parent_feature_group_name")
  private String parentFeatureGroupName;
  @Basic(optional = false)
  @Column(name = "parent_feature_group_version")
  private Integer parentFeatureGroupVersion;
  
  public FeatureGroupLink() {}
  
  public void setId(Integer id) {
    this.id = id;
  }
  
  public Featuregroup getFeatureGroup() {
    return featureGroup;
  }
  
  public void setFeatureGroup(Featuregroup featureGroup) {
    this.featureGroup = featureGroup;
  }
  
  public Featuregroup getParentFeatureGroup() {
    return parentFeatureGroup;
  }
  
  public void setParentFeatureGroup(Featuregroup parentFeatureGroup) {
    this.parentFeatureGroup = parentFeatureGroup;
  }
  
  public String getParentFeatureStore() {
    return parentFeatureStore;
  }
  
  public void setParentFeatureStore(String parentProjectName) {
    this.parentFeatureStore = parentProjectName;
  }
  
  public String getParentFeatureGroupName() {
    return parentFeatureGroupName;
  }
  
  public void setParentFeatureGroupName(String parentFeatureGroupName) {
    this.parentFeatureGroupName = parentFeatureGroupName;
  }
  
  public Integer getParentFeatureGroupVersion() {
    return parentFeatureGroupVersion;
  }
  
  public void setParentFeatureGroupVersion(Integer parentFeatureGroupVersion) {
    this.parentFeatureGroupVersion = parentFeatureGroupVersion;
  }
  
  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof FeatureGroupLink)) {
      return false;
    }
    FeatureGroupLink that = (FeatureGroupLink) o;
    return id.equals(that.id) &&
      featureGroup.equals(that.featureGroup) &&
      Objects.equals(parentFeatureGroup, that.parentFeatureGroup) &&
      parentFeatureStore.equals(that.parentFeatureStore) &&
      parentFeatureGroupName.equals(that.parentFeatureGroupName) &&
      parentFeatureGroupVersion.equals(that.parentFeatureGroupVersion);
  }
  
  @Override
  public int hashCode() {
    return Objects.hash(id, featureGroup, parentFeatureGroup, parentFeatureStore, parentFeatureGroupName,
      parentFeatureGroupVersion);
  }
  
  @Override
  public String parentProject() {
    return parentFeatureStore;
  }
  
  @Override
  public String parentName() {
    return parentFeatureGroupName;
  }
  
  @Override
  public Integer parentVersion() {
    return parentFeatureGroupVersion;
  }
  
  @Override
  public Integer parentId() {
    return parentFeatureGroup != null ? parentFeatureGroup.getId() : null;
  }
}