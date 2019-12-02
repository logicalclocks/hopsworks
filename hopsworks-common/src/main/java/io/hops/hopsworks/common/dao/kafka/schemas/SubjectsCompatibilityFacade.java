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
import io.hops.hopsworks.common.dao.project.Project;
import io.hops.hopsworks.common.util.Settings;

import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.NoResultException;
import javax.persistence.PersistenceContext;
import java.util.Optional;

@Stateless
public class SubjectsCompatibilityFacade extends AbstractFacade<SubjectsCompatibility> {
  
  @PersistenceContext(unitName = "kthfsPU")
  private EntityManager em;
  
  @Override
  protected EntityManager getEntityManager() {
    return em;
  }
  
  public SubjectsCompatibilityFacade() {
    super(SubjectsCompatibility.class);
  }
  
  public Optional<SubjectsCompatibility> getProjectCompatibility(Project project) {
    try {
      return Optional.of(
        em.createNamedQuery("SubjectsCompatibility.getProjectCompatibility", SubjectsCompatibility.class)
          .setParameter("project", project)
          .getSingleResult());
    } catch (NoResultException e) {
      return Optional.empty();
    }
  }
  
  public void setProjectCompatibility(Project project, SchemaCompatibility sc) {
    updateSubjectCompatibility(project, Settings.PROJECT_COMPATIBILITY_SUBJECT, sc);
  }
  
  public void updateSubjectCompatibility(Project project, String subject, SchemaCompatibility sc) {
    SubjectsCompatibility subjectsCompatibility = find(new SubjectsCompatibilityPK(subject, project.getId()));
    boolean newSubjectsCompatibility = false;
    if (subjectsCompatibility == null) {
      subjectsCompatibility = new SubjectsCompatibility(subject, project, sc);
      newSubjectsCompatibility = true;
    }
    
    subjectsCompatibility.setCompatibility(sc);
    
    if (newSubjectsCompatibility) {
      save(subjectsCompatibility);
    } else {
      update(subjectsCompatibility);
    }
    em.flush();
  }
  
  public Optional<SubjectsCompatibility> getSubjectCompatibility(Project project, String subject) {
    try {
      return Optional.of(
        em.createNamedQuery("SubjectsCompatibility.getSubjectCompatibility", SubjectsCompatibility.class)
          .setParameter("project", project)
          .setParameter("subject", subject)
          .getSingleResult());
    } catch (NoResultException e) {
      return Optional.empty();
    }
  }
}
