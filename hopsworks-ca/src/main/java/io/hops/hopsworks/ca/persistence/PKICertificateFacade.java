/*
 * This file is part of Hopsworks
 * Copyright (C) 2022, Hopsworks AB. All rights reserved
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
package io.hops.hopsworks.ca.persistence;

import io.hops.hopsworks.persistence.entity.pki.PKICertificate;
import io.hops.hopsworks.persistence.entity.pki.PKICertificateId;

import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.NoResultException;
import javax.persistence.PersistenceContext;
import java.util.List;
import java.util.Optional;

@Stateless
public class PKICertificateFacade {

  @PersistenceContext(unitName = "kthfsPU")
  private EntityManager em;

  public PKICertificate findBySerialNumber(Long serialNumber) {
    return em.createNamedQuery("PKICertificate.findBySerialNumber", PKICertificate.class)
        .setParameter("serialNumber", serialNumber)
        .getSingleResult();
  }

  public Optional<PKICertificate> findBySubjectAndStatus(String subject, PKICertificate.Status status) {
    try {
      return Optional.of(em.createNamedQuery("PKICertificate.findByStatusAndSubject", PKICertificate.class)
          .setParameter("subject", subject)
          .setParameter("status", status)
          .getSingleResult());
    } catch (NoResultException ex) {
      return Optional.empty();
    }
  }

  public List<String> findAllSubjectsWithStatusAndPartialSubject(String partialSubject, PKICertificate.Status status) {
    return em.createNamedQuery("PKICertificate.findSubjectByStatusAndPartialSubject", String.class)
        .setParameter("subject", partialSubject)
        .setParameter("status", status)
        .getResultList();
  }

  public void saveCertificate(PKICertificate certificate) {
    em.persist(certificate);
  }

  public Optional<PKICertificate> findById(PKICertificateId id) {
    return Optional.ofNullable(em.find(PKICertificate.class, id));
  }

  public void updateCertificate(PKICertificate certificate) {
    em.merge(certificate);
  }

  public void deleteCertificate(PKICertificate certificate) {
    em.remove(em.merge(certificate));
  }
}
