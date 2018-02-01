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

package io.hops.hopsworks.common.dao.user.cluster;

import io.hops.hopsworks.common.dao.AbstractFacade;
import io.hops.hopsworks.common.dao.user.Users;
import java.util.List;
import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.NoResultException;
import javax.persistence.PersistenceContext;
import javax.persistence.TypedQuery;

@Stateless
public class ClusterCertFacade extends AbstractFacade<ClusterCert> {

  @PersistenceContext(unitName = "kthfsPU")
  private EntityManager em;

  @Override
  protected EntityManager getEntityManager() {
    return em;
  }

  public ClusterCertFacade() {
    super(ClusterCert.class);
  }

  public ClusterCert getByOrgUnitNameAndOrgName(String orgName, String orgUnitName) {
    TypedQuery<ClusterCert> query = em.createNamedQuery("ClusterCert.findByOrgUnitNameAndOrgName", ClusterCert.class).
        setParameter("organizationName", orgName).
        setParameter("organizationalUnitName", orgUnitName);
    try {
      return query.getSingleResult();
    } catch (NoResultException e) {
      return null;
    }
  }

  public ClusterCert getBySerialNumber(Integer serialNum) {
    TypedQuery<ClusterCert> query = em.createNamedQuery("ClusterCert.findBySerialNumber", ClusterCert.class).
        setParameter("serialNumber", serialNum);
    try {
      return query.getSingleResult();
    } catch (NoResultException e) {
      return null;
    }
  }

  public List<ClusterCert> getByAgent(Users agent) {
    TypedQuery<ClusterCert> query = em.createNamedQuery("ClusterCert.findByAgent", ClusterCert.class).
        setParameter("agentId", agent);
    return query.getResultList();
  }
}
