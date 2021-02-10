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

package io.hops.hopsworks.common.featurestore.featuregroup.cached;

import io.hops.hopsworks.common.dao.AbstractFacade;
import io.hops.hopsworks.persistence.entity.featurestore.featuregroup.cached.FeatureGroupCommit;
import io.hops.hopsworks.persistence.entity.featurestore.featuregroup.datavalidation.FeatureGroupValidation;

import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.NoResultException;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;
import javax.persistence.TypedQuery;
import java.sql.Timestamp;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * A facade for the feature_group_commit table in the Hopsworks database,
 * use this interface when performing database operations against the table.
 */
@Stateless
public class FeatureGroupCommitFacade extends AbstractFacade<FeatureGroupCommit>{

  @PersistenceContext(unitName = "kthfsPU")
  private EntityManager em;

  public FeatureGroupCommitFacade() {
    super(FeatureGroupCommit.class);
  }

  /**
   * Persist FeatureGroup Commit
   * @param featureGroupCommit
   * @return
   */
  public void createFeatureGroupCommit(FeatureGroupCommit featureGroupCommit) {
    em.persist(featureGroupCommit);
    em.flush();
  }

  public Optional<FeatureGroupCommit> findClosestDateCommit(Integer featureGroupId, Long wallclocktime) {
    Date requestedPointInTime = new Timestamp(wallclocktime);
    Query fgcQuery =  em.createNamedQuery("FeatureGroupCommit.findByLatestCommittedOn", FeatureGroupCommit.class)
        .setParameter("featureGroupId", featureGroupId)
        .setParameter("requestedPointInTime", requestedPointInTime);

    try {
      return Optional.of((FeatureGroupCommit) fgcQuery.getSingleResult());
    } catch (NoResultException e) {
      return Optional.empty();
    }

  }

  public Optional<FeatureGroupCommit> findLatestDateCommit(Integer featureGroupId) {
    Query fgcQuery =  em.createNamedQuery("FeatureGroupCommit.findLatestCommit", FeatureGroupCommit.class)
        .setParameter("featureGroupId", featureGroupId);

    try {
      return Optional.of((FeatureGroupCommit) fgcQuery.getSingleResult());
    } catch (NoResultException e) {
      return Optional.empty();
    }
  }

  public List<FeatureGroupCommit> getCommitDetailsByDate(Integer featureGroupId, Long wallclocktime, Integer limit) {
    Date requestedPointInTime = new Timestamp(wallclocktime);
    TypedQuery<FeatureGroupCommit> q = em.createNamedQuery("FeatureGroupCommit.findByCommittedOn",
        FeatureGroupCommit.class)
        .setParameter("featureGroupId", featureGroupId)
        .setParameter("requestedPointInTime", requestedPointInTime)
        .setMaxResults(limit);
    return q.getResultList();
  }

  public CollectionInfo getCommitDetails(Integer featureGroupId, Integer limit, Integer offset,
                                                   Set<? extends SortBy> sort) {
    String queryStr = buildQuery("SELECT fgc FROM FeatureGroupCommit fgc ", null, sort,
        "fgc.featureGroupCommitPK.featureGroupId = :featureGroupId");
    Query query = em.createQuery(queryStr, FeatureGroupCommit.class)
        .setParameter("featureGroupId", featureGroupId);
    String queryCountStr = buildQuery("SELECT COUNT(fgc.featureGroupCommitPK.commitId) FROM " +
        "FeatureGroupCommit fgc ", null, sort, "fgc.featureGroupCommitPK.featureGroupId = :featureGroupId"
    );
    Query queryCount = em.createQuery(queryCountStr, FeatureGroupCommit.class)
        .setParameter("featureGroupId", featureGroupId);
    query.setFirstResult(offset);
    if (limit != null){
      query.setMaxResults(limit);
    }
    return new CollectionInfo((Long) queryCount.getSingleResult(), query.getResultList());
  }

  public Optional<FeatureGroupCommit> findByValidation(FeatureGroupValidation featureGroupValidation) {
    Query fgcQuery =  em.createNamedQuery("FeatureGroupCommit.findByValidation", FeatureGroupCommit.class)
            .setParameter("validation", featureGroupValidation);
    try {
      return Optional.of((FeatureGroupCommit) fgcQuery.getSingleResult());
    } catch (NoResultException e) {
      return Optional.empty();
    }
  }

  /**
   * Gets the entity manager of the facade
   *
   * @return entity manager
   */
  @Override
  protected EntityManager getEntityManager() {
    return em;
  }

  public enum Sorts {
    COMMITTED_ON("COMMITTED_ON", " fgc.committedOn ", "DESC");

    private final String value;
    private final String sql;
    private final String defaultParam;

    Sorts(String value, String sql, String defaultParam) {
      this.value = value;
      this.sql = sql;
      this.defaultParam = defaultParam;
    }

    public String getValue() {
      return value;
    }

    public String getSql() {
      return sql;
    }

    public String getDefaultParam() {
      return defaultParam;
    }

    @Override
    public String toString() {
      return value;
    }
  }
}
