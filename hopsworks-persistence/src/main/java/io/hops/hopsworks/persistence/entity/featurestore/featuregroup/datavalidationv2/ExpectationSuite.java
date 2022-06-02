/*
 * This file is part of Hopsworks
 * Copyright (C) 2022, Logical Clocks AB. All rights reserved
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

package io.hops.hopsworks.persistence.entity.featurestore.featuregroup.datavalidationv2;

import io.hops.hopsworks.persistence.entity.featurestore.featuregroup.Featuregroup;

import javax.persistence.Basic;
import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.OneToMany;
import javax.persistence.Table;
import javax.validation.constraints.NotNull;
import javax.xml.bind.annotation.XmlRootElement;
import java.io.Serializable;
import java.util.Collection;
import java.util.Objects;

@Entity
@Table(name = "expectation_suite", catalog = "hopsworks")
@NamedQueries({
  @NamedQuery(
    name = "ExpectationSuite.findByFeatureGroup",
    query = "SELECT es FROM ExpectationSuite es WHERE es.featuregroup=:featureGroup"
  )})
@XmlRootElement
public class ExpectationSuite implements Serializable {
  private static final long serialVersionUID = 1L;
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Basic(optional = false)
  @Column(name = "id")
  private Integer id;

  @JoinColumn(name = "feature_group_id", referencedColumnName = "id")
  private Featuregroup featuregroup;

  @Basic(optional = false)
  @NotNull
  @Column(name = "name")
  private String name;

  @Basic
  @Column(name = "meta")
  private String meta;

  @Basic
  @Column(name = "ge_cloud_id")
  private String geCloudId;

  @Basic
  @Column(name = "data_asset_type")
  private String dataAssetType;

  @Basic
  @Column(name = "run_validation")
  private boolean runValidation;

  @Enumerated(EnumType.STRING)
  @Column(name="validation_ingestion_policy")
  private ValidationIngestionPolicy validationIngestionPolicy;

  @OneToMany(cascade = CascadeType.ALL, mappedBy = "expectationSuite")
  private Collection<Expectation> expectations;

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

  public String getMeta() {
    return meta;
  }

  public void setMeta(String meta) {
    this.meta = meta;
  }

  public String getDataAssetType() {
    return dataAssetType;
  }

  public void setDataAssetType(String dataAssetType) {
    this.dataAssetType = dataAssetType;
  }

  public String getGeCloudId() {
    return geCloudId;
  }

  public void setGeCloudId(String geCloudId) {
    this.geCloudId = geCloudId;
  }

  public boolean getRunValidation() {
    return runValidation;
  }

  public void setRunValidation(boolean runValidation) {
    this.runValidation = runValidation;
  }

  public ValidationIngestionPolicy getValidationIngestionPolicy() {
    return validationIngestionPolicy;
  }

  public void setValidationIngestionPolicy(ValidationIngestionPolicy validationIngestionPolicy) {
    this.validationIngestionPolicy = validationIngestionPolicy;
  }

  public Featuregroup getFeaturegroup() {
    return featuregroup;
  }

  public void setFeaturegroup(Featuregroup featuregroup) {
    this.featuregroup = featuregroup;
  }

  public Collection<Expectation> getExpectations() {
    return expectations;
  }

  public void setExpectations(
    Collection<Expectation> expectations) {
    this.expectations = expectations;
  }


  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    ExpectationSuite that = (ExpectationSuite) o;
    return Objects.equals(id, that.id) && Objects.equals(name, that.name) &&
      Objects.equals(meta, that.meta) && Objects.equals(expectations, that.expectations) &&
      Objects.equals(dataAssetType, that.dataAssetType) && Objects.equals(geCloudId, that.geCloudId) &&
      Objects.equals(runValidation, that.runValidation) &&
      Objects.equals(validationIngestionPolicy, that.validationIngestionPolicy);
  }

  @Override
  public int hashCode() {
    return Objects.hash(id, name, meta, expectations, dataAssetType,
      geCloudId, runValidation, validationIngestionPolicy);
  }
}
