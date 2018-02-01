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

package io.hops.hopsworks.common.dao.jobhistory;

import java.io.Serializable;
import javax.persistence.Column;
import javax.persistence.EmbeddedId;
import javax.persistence.Entity;
import javax.persistence.JoinColumn;
import javax.persistence.Lob;
import javax.persistence.ManyToOne;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.Table;
import javax.validation.constraints.Size;
import javax.xml.bind.annotation.XmlRootElement;

@Entity
@Table(name = "yarn_app_heuristic_result_details",
        catalog = "hopsworks",
        schema = "")
@XmlRootElement
@NamedQueries({
  @NamedQuery(name = "YarnAppHeuristicResultDetails.findAll",
          query = "SELECT y FROM YarnAppHeuristicResultDetails y"),
  @NamedQuery(name
          = "YarnAppHeuristicResultDetails.findByYarnAppHeuristicResultId",
          query = "SELECT y FROM YarnAppHeuristicResultDetails y "
          + "WHERE y.yarnAppHeuristicResultDetailsPK.yarnAppHeuristicResultId "
          + "= :yarnAppHeuristicResultId"),
  @NamedQuery(name = "YarnAppHeuristicResultDetails.findByIdAndName",
          query = "SELECT y FROM YarnAppHeuristicResultDetails y "
          + "WHERE y.yarnAppHeuristicResultDetailsPK.yarnAppHeuristicResultId "
          + "= :yarnAppHeuristicResultId AND y.yarnAppHeuristicResultDetailsPK.name = :name"),
  @NamedQuery(name = "YarnAppHeuristicResultDetails.findByName",
          query = "SELECT y FROM YarnAppHeuristicResultDetails y "
          + "WHERE y.yarnAppHeuristicResultDetailsPK.name = :name")})
public class YarnAppHeuristicResultDetails implements Serializable {

  private static final long serialVersionUID = 1L;
  @EmbeddedId
  protected YarnAppHeuristicResultDetailsPK yarnAppHeuristicResultDetailsPK;
  @Lob
  @Size(max = 65535)
  @Column(name = "value")
  private String value;
  @Lob
  @Size(max = 65535)
  @Column(name = "details")
  private String details;
  @JoinColumn(name = "yarn_app_heuristic_result_id",
          referencedColumnName = "id",
          insertable = false,
          updatable = false)
  @ManyToOne(optional = false)
  private YarnAppHeuristicResult yarnAppHeuristicResult;

  public YarnAppHeuristicResultDetails() {
  }

  public YarnAppHeuristicResultDetails(
          YarnAppHeuristicResultDetailsPK yarnAppHeuristicResultDetailsPK) {
    this.yarnAppHeuristicResultDetailsPK = yarnAppHeuristicResultDetailsPK;
  }

  public YarnAppHeuristicResultDetails(int yarnAppHeuristicResultId, String name) {
    this.yarnAppHeuristicResultDetailsPK = new YarnAppHeuristicResultDetailsPK(
            yarnAppHeuristicResultId, name);
  }

  public YarnAppHeuristicResultDetailsPK getYarnAppHeuristicResultDetailsPK() {
    return yarnAppHeuristicResultDetailsPK;
  }

  public void setYarnAppHeuristicResultDetailsPK(
          YarnAppHeuristicResultDetailsPK yarnAppHeuristicResultDetailsPK) {
    this.yarnAppHeuristicResultDetailsPK = yarnAppHeuristicResultDetailsPK;
  }

  public String getValue() {
    return value;
  }

  public void setValue(String value) {
    this.value = value;
  }

  public String getDetails() {
    return details;
  }

  public void setDetails(String details) {
    this.details = details;
  }

  public YarnAppHeuristicResult getYarnAppHeuristicResult() {
    return yarnAppHeuristicResult;
  }

  public void setYarnAppHeuristicResult(
          YarnAppHeuristicResult yarnAppHeuristicResult) {
    this.yarnAppHeuristicResult = yarnAppHeuristicResult;
  }

  @Override
  public int hashCode() {
    int hash = 0;
    hash += (yarnAppHeuristicResultDetailsPK != null
            ? yarnAppHeuristicResultDetailsPK.hashCode() : 0);
    return hash;
  }

  @Override
  public boolean equals(Object object) {
    // TODO: Warning - this method won't work in the case the id fields are not set
    if (!(object instanceof YarnAppHeuristicResultDetails)) {
      return false;
    }
    YarnAppHeuristicResultDetails other = (YarnAppHeuristicResultDetails) object;
    if ((this.yarnAppHeuristicResultDetailsPK == null
            && other.yarnAppHeuristicResultDetailsPK != null)
            || (this.yarnAppHeuristicResultDetailsPK != null
            && !this.yarnAppHeuristicResultDetailsPK.equals(
                    other.yarnAppHeuristicResultDetailsPK))) {
      return false;
    }
    return true;
  }

  @Override
  public String toString() {
    return "se.kth.bbc.jobs.jobhistory.YarnAppHeuristicResultDetails[ yarnAppHeuristicResultDetailsPK="
            + yarnAppHeuristicResultDetailsPK + " ]";
  }

}
