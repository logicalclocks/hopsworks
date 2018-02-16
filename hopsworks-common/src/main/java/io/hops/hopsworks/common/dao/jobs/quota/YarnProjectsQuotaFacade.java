/*
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
 *
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
