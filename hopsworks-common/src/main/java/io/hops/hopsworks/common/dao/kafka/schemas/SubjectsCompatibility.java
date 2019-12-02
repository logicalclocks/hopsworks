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

import io.hops.hopsworks.common.dao.project.Project;

import javax.persistence.Basic;
import javax.persistence.Column;
import javax.persistence.EmbeddedId;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.Table;
import javax.validation.constraints.NotNull;
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
      "WHERE s.subjectsCompatibilityPK.subject = 'projectcompatibility' AND s.project = :project"),
  @NamedQuery(name = "SubjectsCompatibility.getProjectCompatibility",
    query = "SELECT s FROM SubjectsCompatibility s WHERE s.project = :project AND " +
      "s.subjectsCompatibilityPK.subject = 'projectcompatibility'"),
  @NamedQuery(name = "SubjectsCompatibility.getSubjectCompatibility",
    query = "SELECT s FROM SubjectsCompatibility s WHERE s.project = :project AND " +
      "s.subjectsCompatibilityPK.subject = :subject"),
  @NamedQuery(name = "SubjectsCompatibility.setSubjectCompatibility",
    query = "UPDATE SubjectsCompatibility s SET s.compatibility = :compatibility " +
      "WHERE s.subjectsCompatibilityPK.subject = :subject AND s.project = :project")})
public class SubjectsCompatibility implements Serializable {
  
  @Basic(optional = false)
  @NotNull
  @Enumerated(EnumType.STRING)
  @Column(name = "compatibility")
  private SchemaCompatibility compatibility;
  
  @JoinColumn(name = "project_id",
    referencedColumnName = "id",
    insertable = false,
    updatable = false)
  @ManyToOne(optional = false)
  private Project project;
  
  @EmbeddedId
  private SubjectsCompatibilityPK subjectsCompatibilityPK;
  
  public SubjectsCompatibility() {
  }
  
  public SubjectsCompatibility(String subject, Project project, SchemaCompatibility compatibility) {
    this.subjectsCompatibilityPK = new SubjectsCompatibilityPK(subject, project.getId());
    this.compatibility = compatibility;
  }
  
  public SubjectsCompatibilityPK getSubjectsCompatibilityPK() {
    return subjectsCompatibilityPK;
  }
  
  public void setSubjectsCompatibilityPK(
    SubjectsCompatibilityPK subjectsCompatibilityPK) {
    this.subjectsCompatibilityPK = subjectsCompatibilityPK;
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
    hash += (subjectsCompatibilityPK != null ? subjectsCompatibilityPK.hashCode() : 0);
    return hash;
  }
  
  @Override
  public String toString() {
    return "io.hops.hopsworks.common.dao.kafka.schemas.SubjectsCompatibility[ subject=" +
      subjectsCompatibilityPK.getSubject() + ", project_id=" + subjectsCompatibilityPK.getProjectId() +
      ", compatibility=" + compatibility +"]";
  }
}
