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

import javax.persistence.Basic;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.Table;
import javax.xml.bind.annotation.XmlRootElement;
import java.io.Serializable;
import java.util.Objects;

@Entity
@Table(name = "great_expectation", catalog = "hopsworks")
@XmlRootElement
@NamedQueries({
  @NamedQuery(
    name = "GreatExpectation.findById",
    query = "SELECT ge FROM GreatExpectation ge WHERE ge.id=:id"
  ),
  @NamedQuery(
    name = "GreatExpectation.findAll",
    query = "SELECT ge FROM GreatExpectation ge"
  ),
  @NamedQuery(
    name = "GreatExpectation.countAll",
    query = "SELECT COUNT(ge.id) from GreatExpectation ge"
  ),
  @NamedQuery(
    name = "GreatExpectation.findByExpectationType",
    query = "SELECT ge FROM GreatExpectation ge WHERE ge.expectationType=:expectationType"
  )}
)
public class GreatExpectation implements Serializable {
  private static final long serialVersionUID = 1L;
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Basic(optional = false)
  @Column(name = "id")
  private Integer id;

  @Basic(optional = false)
  @Column(name = "expectation_type")
  private String expectationType;

  @Basic(optional = false)
  @Column(name = "kwargs_template")
  private String kwargsTemplate;

  public Integer getId() {
    return id;
  }

  public void setId(Integer id) {
    this.id = id;
  }

  public String getExpectationType() {
    return expectationType;
  }

  public void setExpectationType(String expectationType) {
    this.expectationType = expectationType;
  }

  public String getKwargsTemplate() {
    return kwargsTemplate;
  }

  public void setKwargsTemplate(String kwargsTemplate) {
    this.kwargsTemplate = kwargsTemplate;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    GreatExpectation that = (GreatExpectation) o;
    return Objects.equals(id, that.id) && Objects.equals(expectationType, that.expectationType) &&
      Objects.equals(kwargsTemplate, that.kwargsTemplate);
  }

  @Override
  public int hashCode() {
    return Objects.hash(id, expectationType, kwargsTemplate);
  }
}
