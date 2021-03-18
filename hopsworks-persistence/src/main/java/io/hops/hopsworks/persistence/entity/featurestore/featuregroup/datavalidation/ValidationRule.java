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
import javax.xml.bind.annotation.XmlRootElement;
import java.io.Serializable;
import java.util.Objects;

@Entity
@Table(name = "validation_rule", catalog = "hopsworks")
@XmlRootElement
@NamedQueries({
    @NamedQuery(name = "ValidationRule.findAll",
        query = "SELECT vr FROM ValidationRule vr"),
    @NamedQuery(name = "ValidationRule.findByNameAndPredicate",
        query = "SELECT vr FROM ValidationRule vr WHERE vr.name = :name AND vr.predicate = :predicate"),
    @NamedQuery(name = "ValidationRule.findByName",
        query = "SELECT vr FROM ValidationRule vr WHERE vr.name = :name")})
public class ValidationRule implements Serializable {

  private static final long serialVersionUID = 1L;

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Basic(optional = false)
  @Column(name = "id")
  private Integer id;

  @NotNull
  @Enumerated(EnumType.STRING)
  @Column(name = "name")
  private Name name;

  @NotNull
  @Enumerated(EnumType.STRING)
  @Column(name = "predicate")
  private Predicate predicate;

  @NotNull
  @Enumerated(EnumType.STRING)
  @Column(name = "accepted_type")
  private AcceptedType acceptedType;

  @Basic
  @Column(name = "description")
  private String description;

  public Integer getId() {
    return id;
  }

  public void setId(Integer id) {
    this.id = id;
  }

  public Name getName() {
    return name;
  }

  public void setName(Name name) {
    this.name = name;
  }

  public Predicate getPredicate() {
    return predicate;
  }

  public void setPredicate(Predicate predicate) {
    this.predicate = predicate;
  }

  public AcceptedType getAcceptedType() {
    return acceptedType;
  }

  public void setAcceptedType(AcceptedType acceptedType) {
    this.acceptedType = acceptedType;
  }

  public String getDescription() {
    return description;
  }

  public void setDescription(String description) {
    this.description = description;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    ValidationRule that = (ValidationRule) o;
    return name == that.name;
  }

  @Override
  public int hashCode() {
    return Objects.hash(name);
  }

}
