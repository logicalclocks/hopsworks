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
import javax.persistence.Basic;
import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.JoinColumns;
import javax.persistence.ManyToOne;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.OneToMany;
import javax.persistence.Table;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlTransient;

import org.codehaus.jackson.annotate.JsonIgnore;
import io.hops.hopsworks.common.dao.project.Project;

@Entity
@Table(name = "project_topics",
        catalog = "hopsworks",
        schema = "")
@XmlRootElement
@NamedQueries({
  @NamedQuery(name = "ProjectTopics.findAll",
          query = "SELECT p FROM ProjectTopics p"),
  @NamedQuery(name = "ProjectTopics.findByTopicName",
          query
          = "SELECT p FROM ProjectTopics p WHERE p.topicName = :topicName"),
  @NamedQuery(name = "ProjectTopics.findByProject",
          query
          = "SELECT p FROM ProjectTopics p WHERE p.project = :project"),
  @NamedQuery(name = "ProjectTopics.findByProjectAndTopicName",
          query
          = "SELECT p FROM ProjectTopics p WHERE p.project = :project " +
              "AND p.topicName = :topicName"),
  @NamedQuery(name = "ProjectTopics.findBySchemaVersion",
          query
          = "SELECT p FROM ProjectTopics p WHERE p.schemaTopics.schemaTopicsPK.name "
          + "= :schema_name AND p.schemaTopics.schemaTopicsPK.version = :schema_version")})
public class ProjectTopics implements Serializable {

  private static final long serialVersionUID = 1L;

  @Id
  @GeneratedValue(strategy = GenerationType.SEQUENCE)
  @Basic(optional = false)
  @Column(name = "id", unique = true)
  private Integer id;

  @Basic(optional = false)
  @NotNull
  @Size(min = 1, max = 255)
  @Column(name = "topic_name")
  private String topicName;

  @JoinColumn(name = "project_id", referencedColumnName = "id")
  @ManyToOne(optional = false, fetch = FetchType.LAZY)
  private Project project;

  @OneToMany(cascade = CascadeType.ALL, mappedBy = "projectTopics")
  private Collection<TopicAcls> topicAclsCollection;

  @JoinColumns({
      @JoinColumn(name = "schema_name", referencedColumnName = "name"),
      @JoinColumn(name = "schema_version", referencedColumnName = "version")})
  @ManyToOne(optional = false)
  private SchemaTopics schemaTopics;

  public ProjectTopics() {
  }

  public ProjectTopics(String topicName, Project project, SchemaTopics schemaTopics) {
    this.topicName = topicName;
    this.project = project;
    this.schemaTopics = schemaTopics;
  }

  public Integer getId() {
    return id;
  }

  public void setId(Integer id) {
    this.id = id;
  }

  @NotNull
  public String getTopicName() {
    return topicName;
  }

  public void setTopicName(@NotNull String topicName) {
    this.topicName = topicName;
  }

  public Project getProject() {
    return project;
  }

  public void setProject(Project project) {
    this.project = project;
  }

  public SchemaTopics getSchemaTopics() {
    return schemaTopics;
  }

  public void setSchemaTopics(SchemaTopics schemaTopics) {
    this.schemaTopics = schemaTopics;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    ProjectTopics topics = (ProjectTopics) o;

    if (id != null ? !id.equals(topics.id) : topics.id != null) return false;
    if (!topicName.equals(topics.topicName)) return false;
    if (project != null ? !project.equals(topics.project) : topics.project != null) return false;
    if (topicAclsCollection != null ?
        !topicAclsCollection.equals(topics.topicAclsCollection) : topics.topicAclsCollection != null)
      return false;
    return schemaTopics != null ? schemaTopics.equals(topics.schemaTopics) : topics.schemaTopics == null;
  }

  @Override
  public int hashCode() {
    int result = id != null ? id.hashCode() : 0;
    result = 31 * result + topicName.hashCode();
    result = 31 * result + (project != null ? project.hashCode() : 0);
    result = 31 * result + (topicAclsCollection != null ? topicAclsCollection.hashCode() : 0);
    result = 31 * result + (schemaTopics != null ? schemaTopics.hashCode() : 0);
    return result;
  }

  @XmlTransient
  @JsonIgnore
  public Collection<TopicAcls> getTopicAclsCollection() {
    return topicAclsCollection;
  }

  public void setTopicAclsCollection(Collection<TopicAcls> topicAclsCollection) {
    this.topicAclsCollection = topicAclsCollection;
  }
}
