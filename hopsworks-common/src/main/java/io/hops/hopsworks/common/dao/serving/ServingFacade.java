/*
 * This file is part of Hopsworks
 * Copyright (C) 2018, Logical Clocks AB. All rights reserved
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

package io.hops.hopsworks.common.dao.serving;

import io.hops.hopsworks.persistence.entity.project.Project;
import io.hops.hopsworks.persistence.entity.serving.Serving;
import io.hops.hopsworks.restutils.RESTCodes;
import io.hops.hopsworks.exceptions.ServingException;

import javax.annotation.PostConstruct;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.persistence.EntityManager;
import javax.persistence.LockModeType;
import javax.persistence.LockTimeoutException;
import javax.persistence.NoResultException;
import javax.persistence.PersistenceContext;
import java.net.Inet4Address;
import java.net.UnknownHostException;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

@Stateless
public class ServingFacade {
  private static final Logger LOGGER = Logger.getLogger(ServingFacade.class.getName());

  private static final long LOCK_TIMEOUT = 60000L; // 1 minutes

  @PersistenceContext(unitName = "kthfsPU")
  private EntityManager em;

  private String nodeIP = null;

  @PostConstruct
  private void init() {
    try {
      nodeIP = Inet4Address.getLocalHost().getHostAddress();
    } catch (UnknownHostException e) {
      LOGGER.log(Level.SEVERE, "Could not get host address", e);
    }
  }

  protected EntityManager getEntityManager() {
    return em;
  }

  public ServingFacade() {}

  public List<Serving> findForProject(Project project) {
    return em.createNamedQuery("Serving.findByProject", Serving.class)
        .setParameter("project", project)
        .getResultList();
  }

  public Serving findById(Integer id) {
    return em.createNamedQuery("Serving.findById", Serving.class)
        .setParameter("id", id)
        .getSingleResult();
  }

  @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
  public void delete(Serving serving) {
    // Fetch again the serving instance from the DB as the method that calls this
    // doesn't run within a transaction as it needs to do network ops.
    Serving refetched = em.find(Serving.class, serving.getId());
    if (refetched != null) {
      em.remove(refetched);
    }
  }

  @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
  public Serving updateDbObject(Serving newServing, Project project) throws ServingException {
    // Update request - execute this code within a transaction
    Serving dbServing = findByProjectAndId(project, newServing.getId());

    if (newServing.getName() != null && !newServing.getName().isEmpty()) {
      dbServing.setName(newServing.getName());
    }
    if (newServing.getArtifactPath() != null && !newServing.getArtifactPath().isEmpty()) {
      dbServing.setArtifactPath(newServing.getArtifactPath());
    }
    if (newServing.getInstances() != null) {
      dbServing.setInstances(newServing.getInstances());
    }
    if (newServing.getVersion() != null) {
      dbServing.setVersion(newServing.getVersion());
    }

    dbServing.setKafkaTopic(newServing.getKafkaTopic());

    if (newServing.getLocalPid() != null) {
      dbServing.setLocalPid(newServing.getLocalPid());
    }
    if (newServing.getLocalDir() != null) {
      dbServing.setLocalDir(newServing.getLocalDir());
    }
    if (newServing.getLocalPort() != null) {
      dbServing.setLocalPort(newServing.getLocalPort());
    }

    if (newServing.isBatchingEnabled() != null) {
      dbServing.setBatchingEnabled(newServing.isBatchingEnabled());
    }

    if (newServing.getServingType() != null && newServing.getServingType() != dbServing.getServingType()) {
      throw new ServingException(RESTCodes.ServingErrorCode.UPDATE_SERVING_TYPE_ERROR, Level.FINE);
    }

    return merge(dbServing);
  }

  public Serving merge(Serving serving) {
    return em.merge(serving);
  }

  public Serving findByProjectAndId(Project project, Integer id) {
    try {
      return em.createNamedQuery("Serving.findByProjectAndId", Serving.class)
          .setParameter("project", project)
          .setParameter("id", id)
          .getSingleResult();
    } catch (NoResultException e) {
      return null;
    }
  }

  public Serving findByProjectAndName(Project project, String servingName) {
    try {
      return em.createNamedQuery("Serving.findByProjectAndName", Serving.class)
          .setParameter("project", project)
          .setParameter("name", servingName)
          .getSingleResult();
    } catch (NoResultException e) {
      return null;
    }
  }

  @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
  public Serving acquireLock(Project project, Integer id) throws ServingException {
    int retries = 5;

    if (nodeIP == null) {
      throw new ServingException(RESTCodes.ServingErrorCode.LIFECYCLEERRORINT, Level.SEVERE);
    }

    // Acquire DB read lock on the row
    while (retries > 0) {
      try {
        Serving serving = em.createNamedQuery("Serving.findByProjectAndId", Serving.class)
            .setParameter("project", project)
            .setParameter("id", id)
            .setLockMode(LockModeType.PESSIMISTIC_WRITE)
            .getSingleResult();

        if (serving == null) {
          throw new ServingException(RESTCodes.ServingErrorCode.INSTANCENOTFOUND, Level.WARNING);
        }

        if (serving.getLockIP() != null &&
            serving.getLockTimestamp() > System.currentTimeMillis() - LOCK_TIMEOUT) {
          // There is another request working on this entry. Wait.
          retries--;
          continue;
        }

        serving.setLockIP(nodeIP);
        serving.setLockTimestamp(System.currentTimeMillis());

        // Lock acquire, return;
        return em.merge(serving);
      } catch (LockTimeoutException e) {
        retries--;
      }
    }

    throw new ServingException(RESTCodes.ServingErrorCode.LIFECYCLEERRORINT, Level.FINE);
  }


  @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
  public Serving releaseLock(Project project, Integer id) throws ServingException {
    int retries = 5;
    // Acquire DB read lock on the row
    while (retries > 0) {
      try {
        Serving serving = em.createNamedQuery("Serving.findByProjectAndId", Serving.class)
            .setParameter("project", project)
            .setParameter("id", id)
            .setLockMode(LockModeType.PESSIMISTIC_WRITE)
            .getSingleResult();

        serving.setLockIP(null);
        serving.setLockTimestamp(null);

        return em.merge(serving);
      } catch (LockTimeoutException e) {
        retries--;
      }
    }

    // Lock will be claimed
    throw new ServingException(RESTCodes.ServingErrorCode.LIFECYCLEERRORINT, Level.FINE);
  }

  public List<Serving> getLocalhostRunning() {
    return em.createNamedQuery("Serving.findLocalhostRunning", Serving.class)
        .getResultList();
  }
}
