/*
 * This file is part of Hopsworks
 * Copyright (C) 2019, Logical Clocks AB. All rights reserved
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
package io.hops.hopsworks.persistence.entity.kafka.schemas;

import io.hops.hopsworks.persistence.entity.project.Project;

import java.io.Serializable;
import java.util.Date;
import javax.persistence.Basic;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import javax.xml.bind.annotation.XmlRootElement;

@Entity
@Table(name = "subjects", catalog = "hopsworks")
@XmlRootElement
@NamedQueries({
  @NamedQuery(name = "Subjects.findAllByProject",
          query = "SELECT s FROM Subjects s WHERE s.project = :project"),
  @NamedQuery(name = "Subjects.findBySubject",
          query
          = "SELECT s FROM Subjects s WHERE s.subject = :subject AND " +
            "s.project = :project"),
  @NamedQuery(name = "Subjects.findByVersion",
          query
          = "SELECT s FROM Subjects s WHERE s.version = :version AND " +
            "s.project = :project"),
  @NamedQuery(name = "Subjects.findBySubjectAndVersion",
          query = "SELECT s FROM Subjects s WHERE s.subject = :subject AND " +
            "s.version = :version AND s.project = :project"),
  @NamedQuery(name = "Subjects.findBySchema",
          query = "SELECT s FROM Subjects s WHERE s.schema = :schema AND " +
            "s.project = :project"),
  @NamedQuery(name = "Subjects.findByCreatedOn",
          query = "SELECT s FROM Subjects s WHERE s.createdOn = :createdOn AND " +
            "s.project = :project"),
  @NamedQuery(name = "Subjects.deleteBySubjectAndVersion",
          query = "DELETE FROM Subjects s WHERE s.subject = :subject AND " +
            "s.version = :version AND s.project = :project"),
  @NamedQuery(name = "Subjects.findBySubjectNameAndSchema",
          query = "SELECT s FROM Subjects s WHERE s.subject = :subject AND " +
            "s.schema.schema = :schema AND s.project = :project"),
  @NamedQuery(name = "Subjects.findSetOfSubjects",
          query = "SELECT DISTINCT(s.subject) FROM Subjects s WHERE s.project = :project"),
  @NamedQuery(name = "Subjects.deleteSubject",
          query = "DELETE FROM Subjects s WHERE s.project = :project AND s.subject = :subject"),
  @NamedQuery(name = "Subjects.findLatestVersionOfSubject",
          query = "SELECT s FROM Subjects s WHERE s.project = :project AND s.subject = :subject " +
            " ORDER BY s.version DESC")})
public class Subjects implements Serializable {

  private static final long serialVersionUID = 1L;
  
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Basic(optional = false)
  @Column(name = "id")
  private Integer id;
  
  @Basic(optional = false)
  @NotNull
  @Size(min = 1,
    max = 255)
  @Column(name = "\"subject\"")
  private String subject;
  
  @Basic(optional = false)
  @NotNull
  @Column(name = "version")
  private Integer version;
  
  @JoinColumn(name = "schema_id", referencedColumnName = "id")
  @ManyToOne(optional = false, fetch = FetchType.LAZY)
  private Schemas schema;
  
  @Basic(optional = false)
  @NotNull
  @Column(name = "created_on")
  @Temporal(TemporalType.TIMESTAMP)
  private Date createdOn;
  
  @JoinColumn(name = "project_id",
    referencedColumnName = "id")
  @ManyToOne(optional = false)
  private Project project;

  public Subjects() {
  }

  public Subjects(String subject, int version, Schemas schema, Date createdOn, Project project) {
    this.subject = subject;
    this.version = version;
    this.project = project;
    this.schema = schema;
    this.createdOn = createdOn;
  }
  
  public Subjects(String subject, int version, Project project) {
    this.subject = subject;
    this.version = version;
    this.project = project;
  }
  
  public Subjects(String subject, int version, Schemas schema, Project project) {
    this.subject = subject;
    this.version = version;
    this.project = project;
    this.schema = schema;
    this.createdOn = new Date(System.currentTimeMillis());
  }
  
  public Integer getId() {
    return id;
  }
  
  public void setId(Integer id) {
    this.id = id;
  }
  
  public String getSubject() {
    return subject;
  }
  
  public void setSubject(String subject) {
    this.subject = subject;
  }
  
  public Integer getVersion() {
    return version;
  }
  
  public void setVersion(Integer version) {
    this.version = version;
  }
  
  public Schemas getSchema() {
    return schema;
  }
  
  public void setSchema(Schemas schema) {
    this.schema = schema;
  }
  
  public Date getCreatedOn() {
    return createdOn;
  }
  
  public void setCreatedOn(Date createdOn) {
    this.createdOn = createdOn;
  }
  
  public Project getProject() {
    return project;
  }
  
  public void setProject(Project project) {
    this.project = project;
  }
  
  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    
    Subjects subjects = (Subjects) o;
    
    if (id != null ? !id.equals(subjects.id) : subjects.id != null) {
      return false;
    }
    if (schema != null ? !schema.equals(subjects.schema) : subjects.schema != null) {
      return false;
    }
    return createdOn != null ? createdOn.equals(subjects.createdOn) : subjects.createdOn == null;
  }
  
  @Override
  public int hashCode() {
    int result = id != null ? id.hashCode() : 0;
    result = 31 * result + (schema != null ? schema.hashCode() : 0);
    result = 31 * result + (createdOn != null ? createdOn.hashCode() : 0);
    return result;
  }
  
  @Override
  public String toString() {
    return "Subjects{" +
      "id=" + id +
      ", schema=" + schema +
      ", createdOn=" + createdOn +
      '}';
  }
}
