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
package io.hops.hopsworks.common.dao.kafka.schemas;

import java.io.Serializable;
import java.util.Date;
import javax.persistence.Basic;
import javax.persistence.Column;
import javax.persistence.EmbeddedId;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.PrimaryKeyJoinColumn;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;
import javax.validation.constraints.NotNull;
import javax.xml.bind.annotation.XmlRootElement;

import io.hops.hopsworks.common.dao.project.Project;

@Entity
@Table(name = "hopsworks.subjects")
@XmlRootElement
@NamedQueries({
  @NamedQuery(name = "Subjects.findAllByProject",
          query = "SELECT s FROM Subjects s WHERE s.project = :project"),
  @NamedQuery(name = "Subjects.findBySubject",
          query
          = "SELECT s FROM Subjects s WHERE s.subjectsPK.subject = :subject AND " +
            "s.project = :project"),
  @NamedQuery(name = "Subjects.findByVersion",
          query
          = "SELECT s FROM Subjects s WHERE s.subjectsPK.version = :version AND " +
            "s.project = :project"),
  @NamedQuery(name = "Subjects.findBySubjectAndVersion",
          query = "SELECT s FROM Subjects s WHERE s.subjectsPK.subject = :subject AND " +
            "s.subjectsPK.version = :version AND s.project = :project"),
  @NamedQuery(name = "Subjects.findBySchema",
          query = "SELECT s FROM Subjects s WHERE s.schema = :schema AND " +
            "s.project = :project"),
  @NamedQuery(name = "Subjects.findByCreatedOn",
          query = "SELECT s FROM Subjects s WHERE s.createdOn = :createdOn AND " +
            "s.project = :project"),
  @NamedQuery(name = "Subjects.deleteBySubjectAndVersion",
          query = "DELETE FROM Subjects s WHERE s.subjectsPK.subject = :subject AND " +
            "s.subjectsPK.version = :version AND s.project = :project"),
  @NamedQuery(name = "Subjects.findBySubjectNameAndSchema",
          query = "SELECT s FROM Subjects s WHERE s.subjectsPK.subject = :subject AND " +
            "s.schema.schema = :schema AND s.project = :project"),
  @NamedQuery(name = "Subjects.findSetOfSubjects",
          query = "SELECT DISTINCT(s.subjectsPK.subject) FROM Subjects s WHERE s.project = :project"),
  @NamedQuery(name = "Subjects.deleteSubject",
          query = "DELETE FROM Subjects s WHERE s.project = :project AND s.subjectsPK.subject = :subject"),
  @NamedQuery(name = "Subjects.findLatestVersionOfSubject",
          query = "SELECT s FROM Subjects s WHERE s.project = :project AND s.subjectsPK.subject = :subject " +
            " ORDER BY s.subjectsPK.version DESC")})
public class Subjects implements Serializable {

  private static final long serialVersionUID = 1L;
  
  @EmbeddedId
  private SubjectsPK subjectsPK;
  
  @JoinColumn(name = "schema_id", referencedColumnName = "id")
  @ManyToOne(optional = false, fetch = FetchType.LAZY)
  private Schemas schema;
  
  @Basic(optional = false)
  @NotNull
  @Column(name = "created_on")
  @Temporal(TemporalType.TIMESTAMP)
  private Date createdOn;
  
  @PrimaryKeyJoinColumn(name = "project_id", referencedColumnName = "id")
  @ManyToOne(optional = false, fetch = FetchType.LAZY)
  private Project project;

  public Subjects() {
  }

  public Subjects(String subject, int version, Schemas schema, Date createdOn, Project project) {
    this.subjectsPK = new SubjectsPK(subject, version, project.getId());
    this.project = project;
    this.schema = schema;
    this.createdOn = createdOn;
  }
  
  public Subjects(String subject, int version, Project project) {
    this.subjectsPK = new SubjectsPK(subject, version, project.getId());
  }
  
  public Subjects(String subject, int version, Schemas schema, Project project) {
    this.subjectsPK = new SubjectsPK(subject, version, project.getId());
    this.project = project;
    this.schema = schema;
    this.createdOn = new Date(System.currentTimeMillis());
  }
  
  public SubjectsPK getSubjectsPK() {
    return subjectsPK;
  }
  
  public void setSubjectsPK(SubjectsPK subjectsPK) {
    this.subjectsPK = subjectsPK;
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
  
  public Integer getVersion() {
    return this.subjectsPK.getVersion();
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
    
    if (subjectsPK != null ? !subjectsPK.equals(subjects.subjectsPK) : subjects.subjectsPK != null) {
      return false;
    }
    if (schema != null ? !schema.equals(subjects.schema) : subjects.schema != null) {
      return false;
    }
    return createdOn != null ? createdOn.equals(subjects.createdOn) : subjects.createdOn == null;
  }
  
  @Override
  public int hashCode() {
    int result = subjectsPK != null ? subjectsPK.hashCode() : 0;
    result = 31 * result + (schema != null ? schema.hashCode() : 0);
    result = 31 * result + (createdOn != null ? createdOn.hashCode() : 0);
    return result;
  }
  
  @Override
  public String toString() {
    return "Subjects{" +
      "subjectsPK=" + subjectsPK.toString() +
      ", schema=" + schema +
      ", createdOn=" + createdOn +
      '}';
  }
}
