/*
 * This file is part of HopsWorks
 *
 * Copyright (C) 2013 - 2018, Logical Clocks AB and RISE SICS AB. All rights reserved.
 *
 * HopsWorks is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * HopsWorks is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with HopsWorks.  If not, see <http://www.gnu.org/licenses/>.
 */

package io.hops.hopsworks.common.dao.jobs.quota;

import java.io.Serializable;
import javax.persistence.Basic;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.Table;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import javax.xml.bind.annotation.XmlRootElement;

@Entity
@Table(name = "hops.yarn_price_multiplicator")
@XmlRootElement
@NamedQueries({
  @NamedQuery(name = "YarnPriceMultiplicator.findAll",
          query
          = "SELECT y FROM YarnPriceMultiplicator y"),
  @NamedQuery(name = "YarnPriceMultiplicator.findById",
          query
          = "SELECT y FROM YarnPriceMultiplicator y WHERE y.id = :id"),
  @NamedQuery(name = "YarnPriceMultiplicator.findByMultiplicator",
          query
          = "SELECT y FROM YarnPriceMultiplicator y WHERE y.multiplicator = :multiplicator")})
public class YarnPriceMultiplicator implements Serializable {

  private static final long serialVersionUID = 1L;
  @Id
  @Basic(optional = false)
  @NotNull
  @Size(min = 1,
          max = 255)
  @Column(name = "id")
  private String id;
  @Basic(optional = false)
  @NotNull
  @Column(name = "multiplicator")
  private float multiplicator;

  public YarnPriceMultiplicator() {
  }

  public YarnPriceMultiplicator(String id) {
    this.id = id;
  }

  public YarnPriceMultiplicator(String id, float multiplicator) {
    this.id = id;
    this.multiplicator = multiplicator;
  }

  public String getId() {
    return id;
  }

  public void setId(String id) {
    this.id = id;
  }

  public float getMultiplicator() {
    return multiplicator;
  }

  public void setMultiplicator(float multiplicator) {
    this.multiplicator = multiplicator;
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
    if (!(object instanceof YarnPriceMultiplicator)) {
      return false;
    }
    YarnPriceMultiplicator other = (YarnPriceMultiplicator) object;
    if ((this.id == null && other.id != null) || (this.id != null && !this.id.
            equals(other.id))) {
      return false;
    }
    return true;
  }

  @Override
  public String toString() {
    return "se.kth.bbc.jobs.quota.YarnPriceMultiplicator[ id=" + id + " ]";
  }

}
