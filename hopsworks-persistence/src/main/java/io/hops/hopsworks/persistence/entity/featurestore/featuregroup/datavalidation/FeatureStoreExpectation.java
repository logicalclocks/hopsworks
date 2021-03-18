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

package io.hops.hopsworks.persistence.entity.featurestore.featuregroup.datavalidation;

import io.hops.hopsworks.persistence.entity.featurestore.Featurestore;

import javax.persistence.Basic;
import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Convert;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.JoinTable;
import javax.persistence.ManyToMany;
import javax.persistence.ManyToOne;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.OneToMany;
import javax.persistence.Table;
import javax.validation.constraints.NotNull;
import javax.xml.bind.annotation.XmlRootElement;
import java.util.Collection;
import java.util.Set;

@Entity
@Table(name = "feature_store_expectation", catalog = "hopsworks")
@XmlRootElement
@NamedQueries({
  @NamedQuery(name = "FeatureStoreExpectation.findAll",
    query = "SELECT fse FROM FeatureStoreExpectation fse"),
  @NamedQuery(name = "FeatureStoreExpectation.findByFeaturestoreAndRule",
    query = "SELECT fse FROM FeatureStoreExpectation fse WHERE " +
      "fse.featureStore = :featureStore AND fse.validationRules = :validationRules"),
  @NamedQuery(name = "FeatureStoreExpectation.findByFeaturestoreAndName",
    query = "SELECT fse FROM FeatureStoreExpectation fse WHERE " +
      "fse.featureStore = :featureStore AND fse.name = :name")})
public class FeatureStoreExpectation {
  
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Basic(optional = false)
  @Column(name = "id")
  private Integer id;
  
  @Basic(optional = false)
  @NotNull
  @Column(name = "name")
  private String name;
  
  @Basic
  @Column(name = "description")
  private String description;
  
  @JoinColumn(name = "feature_store_id",
    referencedColumnName = "id")
  @ManyToOne(optional = false)
  private Featurestore featureStore;

  @OneToMany(cascade = CascadeType.ALL, mappedBy = "featureStoreExpectation")
  private Collection<FeatureGroupExpectation> featureGroupExpectations;

  @JoinTable(name = "hopsworks.feature_store_expectation_rule",
          joinColumns
                  = {
                  @JoinColumn(name = "feature_store_expectation_id",
                          insertable= false,
                          updatable = false,
                          referencedColumnName = "id")},
          inverseJoinColumns
                  = {
                  @JoinColumn(name = "validation_rule_id",
                          insertable= false,
                          updatable = false,
                          referencedColumnName = "id")})
  @ManyToMany(fetch = FetchType.EAGER)
  private Set<ValidationRule> validationRules;
  
  @NotNull
  @Column(name = "assertions")
  @Convert(converter = RuleAssertionsConverter.class)
  private Expectation expectation;
  
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
  
  public String getDescription() {
    return description;
  }
  
  public void setDescription(String description) {
    this.description = description;
  }
  
  public Featurestore getFeatureStore() {
    return featureStore;
  }
  
  public void setFeatureStore(Featurestore featureStore) {
    this.featureStore = featureStore;
  }
  
  public Set<ValidationRule> getValidationRules() {
    return validationRules;
  }
  
  public void setValidationRules(Set<ValidationRule> validationRules) {
    this.validationRules = validationRules;
  }

  public Collection<FeatureGroupExpectation> getFeatureGroupExpectations() {
    return featureGroupExpectations;
  }

  public void setFeatureGroupExpectations(Collection<FeatureGroupExpectation> featureGroupExpectations) {
    this.featureGroupExpectations = featureGroupExpectations;
  }

  public Expectation getExpectation() {
    return expectation;
  }
  
  public void setExpectation(Expectation expectation) {
    this.expectation = expectation;
  }
}
