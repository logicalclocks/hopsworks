/*
 * Changes to this file committed after and not including commit-id: ccc0d2c5f9a5ac661e60e6eaf138de7889928b8b
 * are released under the following license:
 *
 * This file is part of Hopsworks
 * Copyright (C) 2018, Logical Clocks AB. All rights reserved
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
 *
 * Changes to this file committed before and including commit-id: ccc0d2c5f9a5ac661e60e6eaf138de7889928b8b
 * are released under the following license:
 *
 * Copyright (C) 2013 - 2018, Logical Clocks AB and RISE SICS AB. All rights reserved
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this
 * software and associated documentation files (the "Software"), to deal in the Software
 * without restriction, including without limitation the rights to use, copy, modify, merge,
 * publish, distribute, sublicense, and/or sell copies of the Software, and to permit
 * persons to whom the Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or
 * substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS  OR IMPLIED, INCLUDING
 * BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL  THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM,
 * DAMAGES OR  OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package io.hops.hopsworks.common.dao.jobhistory;

import java.io.Serializable;
import java.util.Collection;
import javax.persistence.Basic;
import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Lob;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.OneToMany;
import javax.persistence.Table;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlTransient;
import org.codehaus.jackson.annotate.JsonIgnore;

@Entity
@Table(name = "yarn_app_heuristic_result",
        catalog = "hopsworks",
        schema = "")
@XmlRootElement
@NamedQueries({
  @NamedQuery(name = "YarnAppHeuristicResult.findAll",
          query = "SELECT y FROM YarnAppHeuristicResult y"),
  @NamedQuery(name = "YarnAppHeuristicResult.findById",
          query = "SELECT y FROM YarnAppHeuristicResult y WHERE y.id = :id"),
  @NamedQuery(name = "YarnAppHeuristicResult.findByYarnAppResultId",
          query
          = "SELECT y FROM YarnAppHeuristicResult y WHERE y.yarnAppResultId = :yarnAppResultId"),
  @NamedQuery(name = "YarnAppHeuristicResult.findByHeuristicName",
          query
          = "SELECT y FROM YarnAppHeuristicResult y WHERE y.heuristicName = :heuristicName"),
  @NamedQuery(name = "YarnAppHeuristicResult.findBySeverity",
          query
          = "SELECT y FROM YarnAppHeuristicResult y WHERE y.severity = :severity"),
  @NamedQuery(name = "YarnAppHeuristicResult.findByIdAndHeuristicClass",
          query
          = "SELECT y FROM YarnAppHeuristicResult y WHERE y.yarnAppResultId "
          + "= :yarnAppResultId AND y.heuristicClass = :heuristicClass"),
  @NamedQuery(name = "YarnAppHeuristicResult.findByScore",
          query
          = "SELECT y FROM YarnAppHeuristicResult y WHERE y.score = :score")})
public class YarnAppHeuristicResult implements Serializable {

  private static final long serialVersionUID = 1L;
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Basic(optional = false)
  @Column(name = "id")
  private Integer id;
  @Basic(optional = false)
  @NotNull
  @Size(min = 1,
          max = 50)
  @Column(name = "yarn_app_result_id")
  private String yarnAppResultId;
  @Lob
  @Size(max = 65535)
  @Column(name = "heuristic_class")
  private String heuristicClass;
  @Basic(optional = false)
  @NotNull
  @Size(min = 1,
          max = 128)
  @Column(name = "heuristic_name")
  private String heuristicName;
  @Basic(optional = false)
  @NotNull
  @Column(name = "severity")
  private short severity;
  @Column(name = "score")
  private Integer score;
  @OneToMany(cascade = CascadeType.ALL,
          mappedBy = "yarnAppHeuristicResult")
  private Collection<YarnAppHeuristicResultDetails> yarnAppHeuristicResultDetailsCollection;

  public YarnAppHeuristicResult() {
  }

  public YarnAppHeuristicResult(Integer id) {
    this.id = id;
  }

  public YarnAppHeuristicResult(Integer id, String yarnAppResultId,
          String heuristicName, short severity) {
    this.id = id;
    this.yarnAppResultId = yarnAppResultId;
    this.heuristicName = heuristicName;
    this.severity = severity;
  }

  public Integer getId() {
    return id;
  }

  public void setId(Integer id) {
    this.id = id;
  }

  public String getYarnAppResultId() {
    return yarnAppResultId;
  }

  public void setYarnAppResultId(String yarnAppResultId) {
    this.yarnAppResultId = yarnAppResultId;
  }

  public String getHeuristicClass() {
    return heuristicClass;
  }

  public void setHeuristicClass(String heuristicClass) {
    this.heuristicClass = heuristicClass;
  }

  public String getHeuristicName() {
    return heuristicName;
  }

  public void setHeuristicName(String heuristicName) {
    this.heuristicName = heuristicName;
  }

  public short getSeverity() {
    return severity;
  }

  public void setSeverity(short severity) {
    this.severity = severity;
  }

  public Integer getScore() {
    return score;
  }

  public void setScore(Integer score) {
    this.score = score;
  }

  @XmlTransient
  @JsonIgnore
  public Collection<YarnAppHeuristicResultDetails> getYarnAppHeuristicResultDetailsCollection() {
    return yarnAppHeuristicResultDetailsCollection;
  }

  public void setYarnAppHeuristicResultDetailsCollection(
          Collection<YarnAppHeuristicResultDetails> yarnAppHeuristicResultDetailsCollection) {
    this.yarnAppHeuristicResultDetailsCollection
            = yarnAppHeuristicResultDetailsCollection;
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
    if (!(object instanceof YarnAppHeuristicResult)) {
      return false;
    }
    YarnAppHeuristicResult other = (YarnAppHeuristicResult) object;
    if ((this.id == null && other.id != null) || (this.id != null && !this.id.
            equals(other.id))) {
      return false;
    }
    return true;
  }

  @Override
  public String toString() {
    return "se.kth.bbc.jobs.jobhistory.YarnAppHeuristicResult[ id=" + id + " ]";
  }

}
