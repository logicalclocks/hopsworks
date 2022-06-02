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
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.Table;
import javax.validation.constraints.NotNull;
import javax.xml.bind.annotation.XmlRootElement;
import java.io.Serializable;
import java.util.Objects;

@Entity
@Table(name = "expectation", catalog = "hopsworks")
@NamedQueries({
  @NamedQuery(name="Expectation.findById", query="SELECT e FROM Expectation e WHERE e.id=:id")})
@XmlRootElement
public class Expectation implements Serializable {
  private static final long serialVersionUID = 1L;
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Basic(optional = false)
  @Column(name = "id")
  private Integer id;
  
  @JoinColumn(name = "expectation_suite_id", referencedColumnName = "id")
  @ManyToOne(optional = false)
  private ExpectationSuite expectationSuite;
  
  @Basic(optional = false)
  @NotNull
  @Column(name = "kwargs")
  private String kwargs;
  
  @Basic
  @Column(name = "meta")
  private String meta;

  @Basic
  @Column(name = "expectation_type")
  private String expectationType;
  
  public Integer getId() {
    return id;
  }
  
  public void setId(Integer id) {
    this.id = id;
  }
  
  public ExpectationSuite getExpectationSuite() {
    return expectationSuite;
  }
  
  public void setExpectationSuite(
    ExpectationSuite expectationSuite) {
    this.expectationSuite = expectationSuite;
  }
  
  public String getKwargs() {
    return kwargs;
  }
  
  public void setKwargs(String kwargs) {
    this.kwargs = kwargs;
  }
  
  public String getMeta() {
    return meta;
  }
  
  public void setMeta(String meta) {
    this.meta = meta;
  }

  public String getExpectationType() {
    return expectationType;
  }
  
  public void setExpectationType(String expectationType) {
    this.expectationType = expectationType;
  }
  
  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    Expectation that = (Expectation) o;
    return Objects.equals(id, that.id) && Objects.equals(expectationSuite, that.expectationSuite) &&
      Objects.equals(kwargs, that.kwargs) && Objects.equals(meta, that.meta);
  }
  
  @Override
  public int hashCode() {
    return Objects.hash(id, expectationSuite.getId(), kwargs, meta);
  }
}
