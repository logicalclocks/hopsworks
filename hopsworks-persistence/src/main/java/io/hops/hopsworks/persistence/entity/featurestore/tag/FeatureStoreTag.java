/*
 * Copyright (C) 2020, Logical Clocks AB. All rights reserved
 */
package io.hops.hopsworks.persistence.entity.featurestore.tag;

import java.io.Serializable;
import javax.persistence.Basic;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.Table;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import javax.xml.bind.annotation.XmlRootElement;

@Entity
@Table(name = "feature_store_tag", catalog = "hopsworks")
@XmlRootElement
@NamedQueries({
  @NamedQuery(name = "FeatureStoreTag.findAll",
      query = "SELECT f FROM FeatureStoreTag f")
  ,
    @NamedQuery(name = "FeatureStoreTag.findById",
      query = "SELECT f FROM FeatureStoreTag f WHERE f.id = :id")
  ,
    @NamedQuery(name = "FeatureStoreTag.findByName",
      query = "SELECT f FROM FeatureStoreTag f WHERE f.name = :name")
  ,
    @NamedQuery(name = "FeatureStoreTag.findByType",
      query = "SELECT f FROM FeatureStoreTag f WHERE f.type = :type")})
public class FeatureStoreTag implements Serializable {

  private static final long serialVersionUID = 1L;
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Basic(optional = false)
  @Column(name = "id")
  private Integer id;
  @Basic(optional = false)
  @NotNull
  @Size(min = 1,
      max = 255)
  @Column(name = "name")
  private String name;
  @Basic(optional = false)
  @NotNull
  @Column(name = "type")
  @Enumerated(EnumType.STRING)
  private TagType type;

  public FeatureStoreTag() {
  }

  public FeatureStoreTag(Integer id) {
    this.id = id;
  }

  public FeatureStoreTag(String name, TagType type) {
    this.name = name;
    this.type = type;
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
  
  public TagType getType() {
    return type;
  }

  public void setType(TagType type) {
    this.type = type;
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
    if (!(object instanceof FeatureStoreTag)) {
      return false;
    }
    FeatureStoreTag other = (FeatureStoreTag) object;
    if ((this.id == null && other.id != null) || (this.id != null && !this.id.equals(other.id))) {
      return false;
    }
    return true;
  }

  @Override
  public String toString() {
    return "io.hops.hopsworks.persistence.entity.featurestore.tag.FeatureStoreTag[ id=" + id + " ]";
  }
  
}
