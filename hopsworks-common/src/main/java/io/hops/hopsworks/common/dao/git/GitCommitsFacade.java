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

import com.google.common.base.Strings;
import io.hops.hopsworks.common.dao.AbstractFacade;
import io.hops.hopsworks.persistence.entity.git.GitCommit;
import io.hops.hopsworks.persistence.entity.git.GitRepository;

import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.NoResultException;
import javax.persistence.Query;
import javax.persistence.TypedQuery;
import java.util.List;
import java.util.Optional;
import java.util.logging.Logger;

@Stateless
public class GitCommitsFacade extends AbstractFacade<GitCommit> {
  private static final Logger LOGGER = Logger.getLogger(GitCommitsFacade.class.getName());

  @PersistenceContext(unitName = "kthfsPU")
  private EntityManager em;

  public GitCommitsFacade() { super(GitCommit.class); }

  protected EntityManager getEntityManager() {
    return em;
  }

  public Optional<GitCommit> findByHashAndRepository(GitRepository repository) {
    TypedQuery<GitCommit> query = em.createNamedQuery(
        "GitCommit.findByCommitHashAndRepository",
        GitCommit.class
    );
    query.setParameter("repository", repository);
    query.setParameter("hash", repository.getCurrentCommit());
    query.setMaxResults(1);
    try {
      return Optional.of(query.getSingleResult());
    } catch (NoResultException e) {
      return Optional.empty();
    }
  }

  public CollectionInfo<GitCommit> getBranchCommits(GitRepository repository, String branch, Integer limit,
                                                    Integer offset) {
    String queryStr = "SELECT gc FROM GitCommit gc WHERE gc.branch = :branch AND gc.repository = :repository ORDER " +
        "BY gc.id";
    Query commitsQuery =  em.createQuery(queryStr, GitCommit.class);
    commitsQuery.setParameter("branch", branch)
        .setParameter("repository", repository);
    setOffsetAndLim(offset, limit, commitsQuery);
    Query countQuery = em.createQuery("SELECT COUNT(gc.id) FROM GitCommit gc WHERE gc.branch = :branch AND gc" +
        ".repository = :repository");
    countQuery.setParameter("branch", branch)
        .setParameter("repository", repository);
    return new CollectionInfo((Long) countQuery.getSingleResult(), commitsQuery.getResultList());
  }

  public void deleteAllInBranchAndRepository(String branch, GitRepository repository) {
    em.createNamedQuery(
            "GitCommit.deleteAllForBranchAndRepository",
            GitCommit.class)
        .setParameter("branch", branch)
        .setParameter("repository", repository)
        .executeUpdate();
  }

  public GitCommit create(GitCommit commit) {
    if(!Strings.isNullOrEmpty(commit.getMessage()) && commit.getMessage().length() > 1000) {
      commit.setMessage(commit.getMessage().substring(0, 996) + "...");
    }
    super.save(commit);
    em.flush();
    return commit;
  }

  public CollectionInfo<String> getRepositoryBranches(GitRepository repository, Integer limit, Integer offset) {
    Query branchQuery = em.createNamedQuery("GitCommit.findBranchesForRepository")
        .setParameter("repository", repository);
    setOffsetAndLim(offset, limit, branchQuery);
    Query allQuery = em.createNamedQuery("GitCommit.findBranchesForRepository")
        .setParameter("repository", repository);
    List<String> allBranches = allQuery.getResultList();
    return new CollectionInfo((long) allBranches.size(), branchQuery.getResultList());
  }
}
