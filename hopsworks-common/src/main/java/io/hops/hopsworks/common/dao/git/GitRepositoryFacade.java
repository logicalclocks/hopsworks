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
import io.hops.hopsworks.persistence.entity.git.GitRepository;
import io.hops.hopsworks.persistence.entity.git.config.GitProvider;
import io.hops.hopsworks.persistence.entity.hdfs.inode.Inode;
import io.hops.hopsworks.persistence.entity.project.Project;
import io.hops.hopsworks.persistence.entity.user.Users;

import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.NoResultException;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;
import java.util.List;
import java.util.Optional;
import java.util.logging.Logger;

@Stateless
public class GitRepositoryFacade extends AbstractFacade<GitRepository> {
  private static final Logger LOGGER = Logger.getLogger(GitRepositoryFacade.class.getName());

  @PersistenceContext(unitName = "kthfsPU")
  private EntityManager em;

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

  public CollectionInfo<GitRepository> getAllInProject(Project project, Integer limit, Integer offset) {
    Query repoQuery = em.createNamedQuery(
            "GitRepository.findAllInProject",
            GitRepository.class);
    repoQuery.setParameter("project", project);
    setOffsetAndLim(offset, limit, repoQuery);
    Query countQuery = em.createQuery("SELECT COUNT(r.id) FROM GitRepository r WHERE r.project = :project",
        GitRepository.class);
    countQuery.setParameter("project", project);
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

  public Optional<GitRepository> findByInode(Inode inode) {
    try {
      return Optional.of(em.createNamedQuery(
              "GitRepository.findByInode",
              GitRepository.class)
          .setParameter("inode", inode)
          .getSingleResult());
    } catch (NoResultException e) {
      return Optional.empty();
    }
  }

  public GitRepository create(Inode inode, Project project, GitProvider gitProvider, Users user) {
    GitRepository gitRepository = new GitRepository(inode, project,  gitProvider, user);
    em.persist(gitRepository);
    em.flush();
    return gitRepository;
  }

  public GitRepository updateRepository(GitRepository repository) {
    return super.update(repository);
  }

  public GitRepository updateRepositoryCid(GitRepository repository, String pid) {
    repository.setCid(pid);
    return updateRepository(repository);
  }
}
