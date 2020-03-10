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

import io.hops.hopsworks.common.dao.AbstractFacade;
import io.hops.hopsworks.persistence.entity.project.Project;
import io.hops.hopsworks.persistence.entity.kafka.schemas.Schemas;
import io.hops.hopsworks.persistence.entity.kafka.schemas.Subjects;

import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.NoResultException;
import javax.persistence.PersistenceContext;
import java.util.List;
import java.util.Optional;

@Stateless
public class SubjectsFacade extends AbstractFacade<Subjects> {
  
  @PersistenceContext(unitName = "kthfsPU")
  private EntityManager em;
  
  @Override
  protected EntityManager getEntityManager() {
    return em;
  }
  
  public SubjectsFacade() {
    super(Subjects.class);
  }
  
  public List<String> getListOfSubjects(Project project) {
    return em.createNamedQuery("Subjects.findSetOfSubjects", String.class)
      .setParameter("project", project)
      .getResultList();
  }
  
  public Optional<Subjects> findSubjectByNameAndVersion(Project project, String subject, Integer version) {
    try {
      return Optional.of(em.createNamedQuery("Subjects.findBySubjectAndVersion", Subjects.class)
        .setParameter("subject", subject)
        .setParameter("version", version)
        .setParameter("project", project)
        .getSingleResult());
    } catch (NoResultException e) {
      return Optional.empty();
    }
  }
  
  public Optional<Subjects> findSubjectByNameAndSchema(Project project, String subject, String schema) {
    try {
      return Optional.of(em.createNamedQuery("Subjects.findBySubjectNameAndSchema", Subjects.class)
        .setParameter("project", project)
        .setParameter("subject", subject)
        .setParameter("schema", schema)
        .getSingleResult());
    } catch (NoResultException e) {
      return Optional.empty();
    }
  }
  
  public void removeBySubjectAndVersion(Project project, String subject, Integer version) {
    em.createNamedQuery("Subjects.deleteBySubjectAndVersion", Subjects.class)
      .setParameter("project", project)
      .setParameter("subject", subject)
      .setParameter("version", version)
      .executeUpdate();
  }
  
  public List<Subjects> findSubjectByName(Project project, String subject) {
    return em.createNamedQuery("Subjects.findBySubject", Subjects.class)
      .setParameter("project", project)
      .setParameter("subject", subject)
      .getResultList();
  }
  
  public Integer insertNewSubject(Project project, String subjectName, Schemas schema, Integer version) {
    Subjects subject = new Subjects(subjectName, version, schema, project);
    save(subject);
    em.flush();
    return schema.getId();
  }
  
  
  public Integer deleteSubject(Project project, String subject) {
    return em.createNamedQuery("Subjects.deleteSubject")
      .setParameter("project", project)
      .setParameter("subject", subject)
      .executeUpdate();
  }
  
  public Optional<Subjects> findSubjectLatestVersion(Project project, String subject) {
    try {
      return Optional.of(em.createNamedQuery("Subjects.findLatestVersionOfSubject", Subjects.class)
        .setParameter("project", project)
        .setParameter("subject", subject)
        .setMaxResults(1)
        .getSingleResult());
    } catch (NoResultException e) {
      return Optional.empty();
    }
  }
}
