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

package io.hops.hopsworks.common.dao.dela.certs;

import java.util.List;
import java.util.Optional;
import java.util.logging.Logger;
import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.NoResultException;
import javax.persistence.PersistenceContext;
import javax.persistence.TypedQuery;

@Stateless
public class ClusterCertificateFacade {

  private final static Logger LOG = Logger.getLogger(ClusterCertificateFacade.class.getName());

  @PersistenceContext(unitName = "kthfsPU")
  private EntityManager em;

  protected EntityManager getEntityManager() {
    return em;
  }

  public void saveClusterCerts(String clusterName, byte[] keystore, byte[] truststore, String certPswd) {
    ClusterCertificate sc = new ClusterCertificate();
    sc.setClusterName(clusterName);
    sc.setClusterKey(keystore);
    sc.setClusterCert(truststore);
    sc.setCertificatePassword(certPswd);
    em.persist(sc);
    em.flush();
  }

  public void saveClusterCerts(ClusterCertificate clusterCertificate) {
    em.persist(clusterCertificate);
  }
  
  public Optional<ClusterCertificate> getClusterCert(String clusterName) {
    TypedQuery<ClusterCertificate> query = em.createNamedQuery(ClusterCertificate.QUERY_BY_NAME,
      ClusterCertificate.class)
      .setParameter(ClusterCertificate.CLUSTER_NAME, clusterName);
    try {
      ClusterCertificate res = query.getSingleResult();
      return Optional.of(res);
    } catch (NoResultException ex) {
      return Optional.empty();
    }
  }
  
  public Optional<List<ClusterCertificate>> getAllClusterCerts() {
    TypedQuery<ClusterCertificate> query = em.createNamedQuery("ClusterCertificate.findAll",
        ClusterCertificate.class);
    try {
      List<ClusterCertificate> result = query.getResultList();
      return Optional.of(result);
    } catch (NoResultException ex) {
      return Optional.empty();
    }
  }
}
