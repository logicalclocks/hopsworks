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
import io.hops.hopsworks.persistence.entity.git.GitRepositoryRemote;

import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.persistence.EntityManager;
import javax.persistence.NoResultException;
import javax.persistence.PersistenceContext;
import java.util.List;
import java.util.Optional;
import java.util.logging.Logger;

@Stateless
public class GitRepositoryRemotesFacade extends AbstractFacade<GitRepositoryRemote> {
  private static final Logger LOGGER = Logger.getLogger(GitRepositoryRemotesFacade.class.getName());

  @PersistenceContext(unitName = "kthfsPU")
  private EntityManager em;

  public GitRepositoryRemotesFacade() {super(GitRepositoryRemote.class);}

  protected EntityManager getEntityManager() {
    return em;
  }

  public List<GitRepositoryRemote> findAllForRepository(GitRepository repository) {
    return em.createNamedQuery("GitRepositoryRemote.findAllInRepository", GitRepositoryRemote.class)
        .setParameter("repository", repository)
        .getResultList();
  }

  public Optional<GitRepositoryRemote> findByNameAndRepository(GitRepository repository, String name) {
    try {
      return Optional.of(em.createNamedQuery("GitRepositoryRemote.findByNameAndRepository", GitRepositoryRemote.class)
          .setParameter("repository", repository)
              .setParameter("name", name)
          .getSingleResult());
    } catch (NoResultException e) {
      return Optional.empty();
    }
  }

  public void deleteAllInRepository(GitRepository repository) {
    em.createNamedQuery("GitRepositoryRemote.deleteAllInRepository")
        .setParameter("repository", repository)
        .executeUpdate();
  }

  @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
  public void updateRepositoryRemotes(List<GitRepositoryRemote> remotes, GitRepository repository) {
    deleteAllInRepository(repository);
    for (GitRepositoryRemote remote : remotes) {
      save(remote);
    }
  }

  public GitRepositoryRemote create(GitRepository repository, String name, String url) {
    GitRepositoryRemote gitRepositoryRemote = new GitRepositoryRemote(repository, name, url);
    em.persist(gitRepositoryRemote);
    em.flush();
    return gitRepositoryRemote;
  }
}
