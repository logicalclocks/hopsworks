/*
 * This file is part of HopsWorks
 *
 * Copyright (C) 2013 - 2018, Logical Clocks AB and RISE SICS AB. All rights reserved.
 *
 * HopsWorks is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * HopsWorks is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with HopsWorks.  If not, see <http://www.gnu.org/licenses/>.
 */
package io.hops.hopsworks.common.dao.jobs.quota;

import io.hops.hopsworks.common.dao.AbstractFacade;
import io.hops.metadata.yarn.entity.quota.PriceMultiplicator;
import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.NoResultException;
import javax.persistence.PersistenceContext;
import javax.persistence.TypedQuery;
import java.util.List;

@Stateless
public class YarnProjectsQuotaFacade extends
        AbstractFacade<YarnProjectsQuota> {

  @PersistenceContext(unitName = "kthfsPU")
  private EntityManager em;

  @Override
  protected EntityManager getEntityManager() {
    return em;
  }

  public void persistYarnProjectsQuota(YarnProjectsQuota yarnProjectsQuota) {
    em.persist(yarnProjectsQuota);
  }

  public YarnProjectsQuotaFacade() {
    super(YarnProjectsQuota.class);
  }

  public YarnProjectsQuota findByProjectName(String projectname) {
    TypedQuery<YarnProjectsQuota> query = em.
            createNamedQuery("YarnProjectsQuota.findByProjectname",
                    YarnProjectsQuota.class).setParameter("projectname",
                    projectname);
    try {
      return query.getSingleResult();
    } catch (NoResultException e) {
      return null;
    }
  }

  public void flushEm() {
    em.flush();
  }

  @Override
  public List<YarnProjectsQuota> findAll() {
    TypedQuery<YarnProjectsQuota> query = em.createNamedQuery(
            "YarnProjectsQuota.findAll",
            YarnProjectsQuota.class);
    return query.getResultList();
  }

  public void changeYarnQuota(String projectName, float quota) {
    YarnProjectsQuota project = findByProjectName(projectName);
    if (project != null) {
      project.setQuotaRemaining(quota);
      em.merge(project);
    }
  }

  public YarnPriceMultiplicator getMultiplicator(PriceMultiplicator.MultiplicatorType multiplicatorType) {
    try {
      TypedQuery<YarnPriceMultiplicator> query = em.
          createNamedQuery("YarnPriceMultiplicator.findById", YarnPriceMultiplicator.class).setParameter("id",
          multiplicatorType.name());
      return query.getSingleResult();
    } catch (NoResultException e) {
      return null;
    }

  }

  public List<YarnPriceMultiplicator> getMultiplicators() {
    try {
      TypedQuery<YarnPriceMultiplicator> query = em.createNamedQuery("YarnPriceMultiplicator.findAll",
          YarnPriceMultiplicator.class);
      return query.getResultList();
    } catch (NoResultException e) {
      return null;
    }
  }
}
