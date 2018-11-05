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
package io.hops.hopsworks.common.dao.project;

import java.util.Date;
import java.util.List;
import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.NoResultException;
import javax.persistence.PersistenceContext;
import javax.persistence.TypedQuery;
import io.hops.hopsworks.common.dao.user.Users;
import io.hops.hopsworks.common.dao.AbstractFacade;

@Stateless
public class ProjectFacade extends AbstractFacade<Project> {

  @PersistenceContext(unitName = "kthfsPU")
  private EntityManager em;

  @Override
  protected EntityManager getEntityManager() {
    return em;
  }

  public ProjectFacade() {
    super(Project.class);
  }

  @Override
  public List<Project> findAll() {
    TypedQuery<Project> query = em.createNamedQuery("Project.findAll",
        Project.class);
    return query.getResultList();
  }

  public Project find(Integer id) {
    return em.find(Project.class, id);
  }

  public Project findByInodeId(Integer parentId, String name) {
    TypedQuery<Project> query = this.em.
        createNamedQuery("Project.findByInodeId", Project.class).
        setParameter("parentid", parentId).setParameter("name", name);

    try {
      return query.getSingleResult();
    } catch (NoResultException e) {
      return null;
    }
  }

  /**
   * Find all the studies for which the given user is owner. This implies that
   * this user created all the returned studies.
   * <p/>
   * @param user The user for whom studies are sought.
   * @return List of all the studies that were created by this user.
   */
  public List<Project> findByUser(Users user) {
    TypedQuery<Project> query = em.createNamedQuery(
        "Project.findByOwner", Project.class).setParameter(
            "owner", user);
    return query.getResultList();
  }

  /**
   * Find all the studies for which the user with given email is owner. This
   * implies that this user created all the returned studies.
   * <p/>
   * @param email The email of the user for whom studies are sought.
   * @return List of all the studies that were created by this user.
   * @deprecated use findByUser(User user) instead.
   */
  public List<Project> findByUser(String email) {
    TypedQuery<Users> query = em.createNamedQuery(
        "Users.findByEmail", Users.class).setParameter(
            "email", email);
    Users user = query.getSingleResult();
    return findByUser(user);
  }

  /**
   * Get the project with the given name created by the given User.
   * <p/>
   * @param projectname The name of the project.
   * @param user The owner of the project.
   * @return The project with given name created by given user, or null if such
   * does not exist.
   */
  public Project findByNameAndOwner(String projectname, Users user) {
    TypedQuery<Project> query = em.
        createNamedQuery("Project.findByOwnerAndName",
            Project.class).setParameter("name", projectname).
        setParameter("owner",
            user);
    try {
      return query.getSingleResult();
    } catch (NoResultException e) {
      return null;
    }
  }

  /**
   * Get the project with the given name created by the User with given email.
   * <p/>
   * @param projectname The name of the project.
   * @param email The email of the owner of the project.
   * @return The project with given name created by given user, or null if such
   * does not exist.
   * @deprecated use findByNameAndOwner(String projectname, User user) instead.
   */
  public Project findByNameAndOwnerEmail(String projectname, String email) {
    TypedQuery<Users> query = em.createNamedQuery("Users.findByEmail",
        Users.class).setParameter("email", email);
    Users user = query.getSingleResult();
    return findByNameAndOwner(projectname, user);
  }

  /**
   * Get the owner of the given project.
   * <p/>
   * @param project The project for which to get the current owner.
   * @return The primary key of the owner of the project.
   * @deprecated Use project.getOwner().getEmail(); instead.
   */
  public String findOwner(Project project) {
    return project.getOwner().getEmail();
  }

  /**
   * Find all the studies the given user is a member of.
   * <p/>
   * @param user
   * @return
   */
  public List<Project> findAllMemberStudies(Users user) {
    TypedQuery<Project> query = em.createNamedQuery(
        "ProjectTeam.findAllMemberStudiesForUser",
        Project.class);
    query.setParameter("user", user);
    return query.getResultList();
  }

  /**
   * Find all studies created (and owned) by this user.
   * <p/>
   * @param user
   * @return
   */
  public List<Project> findAllPersonalStudies(Users user) {
    TypedQuery<Project> query = em.createNamedQuery("Project.findByOwner",
        Project.class);
    query.setParameter("owner", user);
    return query.getResultList();
  }

  /**
   * Get all the studies this user has joined, but not created.
   * <p/>
   * @param user
   * @return
   */
  public List<Project> findAllJoinedStudies(Users user) {
    TypedQuery<Project> query = em.createNamedQuery(
        "ProjectTeam.findAllJoinedStudiesForUser",
        Project.class);
    query.setParameter("user", user);
    return query.getResultList();
  }

  public void persistProject(Project project) {
    em.persist(project);
  }

  public void flushEm() {
    em.flush();
  }

  /**
   * Mark the project <i>project</i> as deleted.
   * <p/>
   * @param project
   */
  public void removeProject(Project project) {
    project.setDeleted(Boolean.TRUE);
    em.merge(project);
  }

  /**
   * Check if a project with this name already exists.
   * <p/>
   * @param name
   * @return
   */
  public boolean projectExists(String name) {
    return !(em.createNamedQuery("Project.findByNameCaseInsensitive", Project.class)
        .setParameter("name", name)
        .getResultList().isEmpty());
  }

  /**
   * Check if a project with this name already exists for a user.
   * <p/>
   * @param name
   * @param owner
   * @return
   */
  public boolean projectExistsForOwner(String name, Users owner) {
    TypedQuery<Project> query = em.
        createNamedQuery("Project.findByOwnerAndName",
            Project.class);
    query.setParameter("owner", owner).setParameter("name", name);
    return !query.getResultList().isEmpty();
  }

  /**
   * Merge the new project.
   * <p/>
   * @param newProject
   */
  public void mergeProject(Project newProject) {
    em.merge(newProject);
  }

  public void archiveProject(Project project) {
    project.setArchived(true);
    em.merge(project);
  }

  public void enableConda(Project project) {
    if (project != null) {
      project.setConda(true);
    }
    em.merge(project);
  }

  public void enableLogs(Project project) {
    if (project != null) {
      project.setLogs(true);
    }
    em.merge(project);
  }

  public void unarchiveProject(Project project) {
    project.setArchived(false);
    em.merge(project);
  }

  public boolean updateRetentionPeriod(String name, Date date) {
    Project project = findByName(name);
    if (project != null) {
      project.setRetentionPeriod(date);
      em.merge(project);
      return true;
    }
    return false;
  }

  public Date getRetentionPeriod(String name) {
    Project project = findByName(name);
    if (project != null) {
      return project.getRetentionPeriod();
    }
    return null;
  }

  public Project findByName(String name) {
    TypedQuery<Project> query = em.createNamedQuery("Project.findByName",
        Project.class);
    query.setParameter("name", name);
    try {
      return query.getSingleResult();
    } catch (NoResultException e) {
      return null;
    }
  }

  public boolean numProjectsLimitReached(Users user) {
    if (user.getMaxNumProjects() > 0 && user.getNumCreatedProjects() >= user.getMaxNumProjects()) {
      return true;
    }
    return false;
  }

  public void setTimestampQuotaUpdate(Project project, Date timestamp) {
    project.setLastQuotaUpdate(timestamp);
    em.merge(project);
    em.flush();
  }

  public void changeKafkaQuota(Project project, int numTopics) {
    project.setKafkaMaxNumTopics(numTopics);
    em.merge(project);
  }
  
  /**
   * Find all Projects which are Conda enabled
   *
   * @return list of Conda enabled projects
   */
  public List<Project> findAllCondaEnabled() {
    TypedQuery<Project> query = em.createNamedQuery("Project.findAllCondaEnabled", Project.class);
    return query.getResultList();
  }
}
