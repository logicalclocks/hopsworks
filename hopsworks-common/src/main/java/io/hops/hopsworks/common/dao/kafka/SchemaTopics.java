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

package io.hops.hopsworks.common.dao.kafka;

import java.io.Serializable;
import java.util.Collection;
import java.util.Date;
import javax.persistence.Basic;
import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.EmbeddedId;
import javax.persistence.Entity;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.OneToMany;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlTransient;
import org.codehaus.jackson.annotate.JsonIgnore;

@Entity
@Table(name = "schema_topics",
        catalog = "hopsworks",
        schema = "")
@XmlRootElement
@NamedQueries({
  @NamedQuery(name = "SchemaTopics.findAll",
          query = "SELECT s FROM SchemaTopics s"),
  @NamedQuery(name = "SchemaTopics.findByName",
          query
          = "SELECT s FROM SchemaTopics s WHERE s.schemaTopicsPK.name = :name"),
  @NamedQuery(name = "SchemaTopics.findByVersion",
          query
          = "SELECT s FROM SchemaTopics s WHERE s.schemaTopicsPK.version = :version"),
  @NamedQuery(name = "SchemaTopics.findByNameAndVersion",
      query = "SELECT s FROM SchemaTopics s WHERE s.schemaTopicsPK.name = :name AND " +
          "s.schemaTopicsPK.version = :version"),
  @NamedQuery(name = "SchemaTopics.findByContents",
          query = "SELECT s FROM SchemaTopics s WHERE s.contents = :contents"),
  @NamedQuery(name = "SchemaTopics.findByCreatedOn",
          query = "SELECT s FROM SchemaTopics s WHERE s.createdOn = :createdOn")})
public class SchemaTopics implements Serializable {

  @OneToMany(cascade = CascadeType.PERSIST,
          mappedBy = "schemaTopics")
  private Collection<ProjectTopics> projectTopicsCollection;

  private static final long serialVersionUID = 1L;
  @EmbeddedId
  protected SchemaTopicsPK schemaTopicsPK;
  @Basic(optional = false)
  @NotNull
  @Size(min = 1,
          max = 10000)
  @Column(name = "contents")
  private String contents;
  @Basic(optional = false)
  @NotNull
  @Column(name = "created_on")
  @Temporal(TemporalType.TIMESTAMP)
  private Date createdOn;

  public SchemaTopics() {
  }

  public SchemaTopics(String name, int version, String contents, Date createdOn) {
    this.schemaTopicsPK = new SchemaTopicsPK(name, version);
    this.contents = contents;
    this.createdOn = createdOn;
  }

  public SchemaTopics(String name, int version) {
    this.schemaTopicsPK = new SchemaTopicsPK(name, version);
  }

  public SchemaTopicsPK getSchemaTopicsPK() {
    return schemaTopicsPK;
  }

  public void setSchemaTopicsPK(SchemaTopicsPK schemaTopicsPK) {
    this.schemaTopicsPK = schemaTopicsPK;
  }

  public String getContents() {
    return contents;
  }

  public void setContents(String contents) {
    this.contents = contents;
  }

  public Date getCreatedOn() {
    return createdOn;
  }

  public void setCreatedOn(Date createdOn) {
    this.createdOn = createdOn;
  }

  @Override
  public int hashCode() {
    int hash = 0;
    hash += (schemaTopicsPK != null ? schemaTopicsPK.hashCode() : 0);
    return hash;
  }

  @Override
  public boolean equals(Object object) {
    // TODO: Warning - this method won't work in the case the id fields are not set
    if (!(object instanceof SchemaTopics)) {
      return false;
    }
    SchemaTopics other = (SchemaTopics) object;
    if ((this.schemaTopicsPK == null && other.schemaTopicsPK != null)
            || (this.schemaTopicsPK != null && !this.schemaTopicsPK.equals(
                    other.schemaTopicsPK))) {
      return false;
    }
    return true;
  }

  @Override
  public String toString() {
    return "io.hops.kafka.SchemaTopics[ schemaTopicsPK=" + schemaTopicsPK + " ]";
  }

  @XmlTransient
  @JsonIgnore
  public Collection<ProjectTopics> getProjectTopicsCollection() {
    return projectTopicsCollection;
  }

  public void setProjectTopicsCollection(
          Collection<ProjectTopics> projectTopicsCollection) {
    this.projectTopicsCollection = projectTopicsCollection;
  }

}
