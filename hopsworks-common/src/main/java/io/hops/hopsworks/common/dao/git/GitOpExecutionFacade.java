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
import io.hops.hopsworks.persistence.entity.git.config.GitCommandConfiguration;
import io.hops.hopsworks.persistence.entity.git.GitOpExecution;
import io.hops.hopsworks.persistence.entity.git.config.GitOpExecutionState;
import io.hops.hopsworks.persistence.entity.user.Users;

import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.NoResultException;
import javax.persistence.Query;
import javax.persistence.PersistenceContext;
import javax.persistence.TypedQuery;
import java.util.Date;
import java.util.Optional;
import java.util.logging.Logger;

@Stateless
public class GitOpExecutionFacade extends AbstractFacade<GitOpExecutionFacade> {
  private static final Logger LOGGER = Logger.getLogger(GitOpExecutionFacade.class.getName());

  @PersistenceContext(unitName = "kthfsPU")
  private EntityManager em;

  public GitOpExecutionFacade() {super(GitOpExecutionFacade.class);}

  protected EntityManager getEntityManager() {
    return em;
  }

  public Optional<GitOpExecution> findByIdAndRepository(GitRepository repository, Integer executionId) {
    try {
      TypedQuery<GitOpExecution> query = em.createNamedQuery(
          "GitOpExecution.findByIdAndRepository",
          GitOpExecution.class
      );
      query.setParameter("repository", repository);
      query.setParameter("id", executionId);
      return Optional.of(query.getSingleResult());
    } catch (NoResultException e) {
      return Optional.empty();
    }
  }

  public Optional<GitOpExecution> findRunningInRepository(GitRepository repository) {
    try {
      TypedQuery<GitOpExecution> query = em.createNamedQuery(
          "GitOpExecution.findRunningInRepository",
          GitOpExecution.class
      );
      query.setParameter("repository", repository);
      query.setParameter("finalStates", GitOpExecutionState.getFinalStates());
      return Optional.of(query.getSingleResult());
    } catch (NoResultException e) {
      return Optional.empty();
    }
  }

  public CollectionInfo<GitOpExecution> getAllForRepository(GitRepository repository, Integer limit, Integer offset) {
    Query query = em.createNamedQuery("GitOpExecution.findAllInRepository", GitOpExecution.class);
    query.setParameter("repository", repository);
    setOffsetAndLim(offset, limit, query);
    Query countQuery = em.createQuery("SELECT COUNT(e.id) FROM GitOpExecution e WHERE e.repository = :repository",
        GitOpExecution.class);
    countQuery.setParameter("repository", repository);
    return new CollectionInfo((Long) countQuery.getSingleResult(), query.getResultList());
  }

  public GitOpExecution create(GitCommandConfiguration gitCommandConfiguration, Users user, GitRepository repository,
                               String secret, String hostname) {
    GitOpExecution gitOpExecution = new GitOpExecution(gitCommandConfiguration, new Date(), user, repository,
        GitOpExecutionState.INITIALIZING, secret, hostname);
    gitOpExecution.setExecutionStart(System.currentTimeMillis());
    em.persist(gitOpExecution);
    em.flush();
    return gitOpExecution;
  }

  public GitOpExecution updateState(GitOpExecution exec, GitOpExecutionState newState, String message) {
    exec = em.find(GitOpExecution.class, exec.getId());
    exec.setState(newState);
    exec.setCommandResultMessage(message);
    if (newState.isFinalState()) {
      exec.setExecutionStop(System.currentTimeMillis());
    }
    em.merge(exec);
    em.flush();
    return exec;
  }
}
