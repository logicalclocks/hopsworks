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

import javax.persistence.Basic;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.Table;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import javax.xml.bind.annotation.XmlRootElement;
import java.io.Serializable;

@Entity
@Table(name = "subjects_compatibility",
  catalog = "hopsworks",
  schema = "")
@XmlRootElement
@NamedQueries({
  @NamedQuery(name = "SubjectsCompatibility.setProjectCompatibility",
    query = "UPDATE SubjectsCompatibility s SET s.compatibility = :compatibility " +
      "WHERE s.subject = 'projectcompatibility' AND s.project = :project"),
  @NamedQuery(name = "SubjectsCompatibility.getProjectCompatibility",
    query = "SELECT s FROM SubjectsCompatibility s WHERE s.project = :project AND " +
      "s.subject = 'projectcompatibility'"),
  @NamedQuery(name = "SubjectsCompatibility.getSubjectCompatibility",
    query = "SELECT s FROM SubjectsCompatibility s WHERE s.project = :project AND " +
      "s.subject = :subject"),
  @NamedQuery(name = "SubjectsCompatibility.setSubjectCompatibility",
    query = "UPDATE SubjectsCompatibility s SET s.compatibility = :compatibility " +
      "WHERE s.subject = :subject AND s.project = :project"),
  @NamedQuery(name = "SubjectsCompatibility.findBySubject",
    query = "SELECT s FROM SubjectsCompatibility s WHERE s.project = :project AND " +
      "s.subject = :subject")})
public class SubjectsCompatibility implements Serializable {
  
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Basic(optional = false)
  @Column(name = "id")
  private Integer id;
  
  @Basic(optional = false)
  @NotNull
  @Size(min = 1,
    max = 255)
  @Column(name = "subject")
  private String subject;
  
  @Basic(optional = false)
  @NotNull
  @Enumerated(EnumType.STRING)
  @Column(name = "compatibility")
  private SchemaCompatibility compatibility;
  
  @JoinColumn(name = "project_id",
    referencedColumnName = "id")
  @ManyToOne(optional = false)
  private Project project;
  
  
  public SubjectsCompatibility() {
  }
  
  public SubjectsCompatibility(String subject, Project project, SchemaCompatibility compatibility) {
    this.subject = subject;
    this.project = project;
    this.compatibility = compatibility;
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
  
  public SchemaCompatibility getCompatibility() {
    return compatibility;
  }
  
  public void setCompatibility(SchemaCompatibility compatibility) {
    this.compatibility = compatibility;
  }
  
  public Project getProject() {
    return project;
  }
  
  public void setProject(Project project) {
    this.project = project;
  }
  
  @Override
  public int hashCode() {
    int hash = 0;
    hash += (id != null ? id.hashCode() : 0);
    return hash;
  }
  
  @Override
  public String toString() {
    return "io.hops.hopsworks.persistence.entity.kafka.schemas.SubjectsCompatibility[ subject=" +
      subject + ", project_id=" + project.getId() +
      ", compatibility=" + compatibility +"]";
  }
}
