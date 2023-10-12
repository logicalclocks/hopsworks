/*
 * This file is part of Hopsworks
 * Copyright (C) 2021, Logical Clocks AB. All rights reserved
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
package io.hops.hopsworks.common.dao.git;

import io.hops.hopsworks.common.dao.AbstractFacade;
import io.hops.hopsworks.common.dao.user.UserFacade;
import io.hops.hopsworks.persistence.entity.git.GitRepository;
import io.hops.hopsworks.persistence.entity.git.config.GitProvider;
import io.hops.hopsworks.persistence.entity.project.Project;
import io.hops.hopsworks.persistence.entity.user.Users;

import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.NoResultException;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.logging.Logger;

@Stateless
public class GitRepositoryFacade extends AbstractFacade<GitRepository> {
  private static final Logger LOGGER = Logger.getLogger(GitRepositoryFacade.class.getName());

  @PersistenceContext(unitName = "kthfsPU")
  private EntityManager em;
  @EJB
  private UserFacade userFacade;

  public GitRepositoryFacade() { super(GitRepository.class); }

  protected EntityManager getEntityManager() {
    return em;
  }

  public Optional<GitRepository> findById(Integer repoId) {
    try {
      return Optional.of(em.createNamedQuery(
              "GitRepository.findById",
              GitRepository.class)
          .setParameter("id", repoId)
          .getSingleResult());
    } catch (NoResultException e) {
      return Optional.empty();
    }
  }

  public CollectionInfo<GitRepository> getAllInProjectForUser(Project project, Users user,
                                                              Set<? extends FilterBy> filters,
                                                              Set<? extends AbstractFacade.SortBy> sorts, Integer limit,
                                                              Integer offset) {
    String repoQueryStr = buildQuery("SELECT r FROM GitRepository r ", filters, sorts,
        "r.project = :project AND r.creator = :creator");
    String countQueryStr = buildQuery("SELECT COUNT(r.id) FROM GitRepository r ", filters, sorts,
        "r.project = :project AND r.creator = :creator");
    Query countQuery = em.createQuery(countQueryStr, GitRepository.class);
    countQuery.setParameter("project", project);
    countQuery.setParameter("creator",  user);
    Query repoQuery = em.createQuery(repoQueryStr, GitRepository.class);
    repoQuery.setParameter("project", project);
    repoQuery.setParameter("creator",  user);
    setFilter(filters, countQuery);
    setFilter(filters, repoQuery);
    setOffsetAndLim(offset, limit, repoQuery);
    return new CollectionInfo((Long) countQuery.getSingleResult(), repoQuery.getResultList());
  }

  public List<GitRepository> findAllWithOngoingOperations() {
    return em.createNamedQuery(
        "GitRepository.findAllWithRunningOperation",
        GitRepository.class)
        .getResultList();
  }

  public Optional<GitRepository> findByIdAndProject(Project project, Integer repoId) {
    try {
      return Optional.of(em.createNamedQuery(
              "GitRepository.findByIdAndProject",
              GitRepository.class)
          .setParameter("id", repoId)
          .setParameter("project", project)
          .getSingleResult());
    } catch (NoResultException e) {
      return Optional.empty();
    }
  }

  public Optional<GitRepository> findByPath(String path) {
    try {
      return Optional.of(em.createNamedQuery(
              "GitRepository.findByPath",
              GitRepository.class)
          .setParameter("path", path)
          .getSingleResult());
    } catch (NoResultException e) {
      return Optional.empty();
    }
  }

  public GitRepository create(Project project, GitProvider gitProvider, Users user, String name, String repositoryPath)
  {
    GitRepository gitRepository = new GitRepository(project,  gitProvider, user, name, repositoryPath);
    em.persist(gitRepository);
    em.flush();
    return gitRepository;
  }

  public GitRepository updateRepository(GitRepository repository) {
    repository = em.merge(repository);
    em.flush();
    return repository;
  }

  public GitRepository updateRepositoryCid(GitRepository repository, String pid) {
    repository.setCid(pid);
    return updateRepository(repository);
  }

  public void deleteRepository(GitRepository repository) {
    if (repository != null) {
      em.remove(repository);
    }
  }

  private void setFilter(Set<? extends AbstractFacade.FilterBy> filter, Query q) {
    if (filter == null || filter.isEmpty()) {
      return;
    }
    for (FilterBy aFilter : filter) {
      setFilterQuery(aFilter, q);
    }
  }

  private void setFilterQuery(AbstractFacade.FilterBy filterBy, Query q) {
    switch (GitRepositoryFacade.Filters.valueOf(filterBy.getValue())) {
      case NAME:
        q.setParameter(filterBy.getField(), filterBy.getParam().toLowerCase());
        break;
      case USER:
        q.setParameter(filterBy.getField(), userFacade.findByUsername(filterBy.getParam()));
        break;
      default:
        break;
    }
  }

  public enum Sorts {
    ID("ID", "r.id ", "DESC"),
    NAME("NAME", "r.name ", "ASC");

    private final String value;
    private final String sql;
    private final String defaultParam;

    private Sorts(String value, String sql, String defaultParam) {
      this.value = value;
      this.sql = sql;
      this.defaultParam = defaultParam;
    }

    public String getValue() { return value; }

    public String getSql() { return sql; }

    public String getDefaultParam() { return defaultParam; }

    public String getJoin() { return null; }

    @Override
    public String toString() { return value; }

  }

  public enum Filters {
    NAME("NAME", "LOWER(r.name) LIKE CONCAT('%', :name, '%') ", "name", " "),
    USER("USER", "r.creator = :user ", "user", " ");

    private final String value;
    private final String sql;
    private final String field;
    private final String defaultParam;

    private Filters(String value, String sql, String field, String defaultParam) {
      this.value = value;
      this.sql = sql;
      this.field = field;
      this.defaultParam = defaultParam;
    }

    public String getDefaultParam() { return defaultParam; }

    public String getValue() { return value; }

    public String getSql() { return sql; }

    public String getField() { return field; }

    @Override
    public String toString() { return value; }

  }
}
