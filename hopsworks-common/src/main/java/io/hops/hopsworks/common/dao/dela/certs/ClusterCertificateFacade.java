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
