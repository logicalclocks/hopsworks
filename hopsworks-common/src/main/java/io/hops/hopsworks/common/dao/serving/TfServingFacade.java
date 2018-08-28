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

import io.hops.hopsworks.common.dao.project.Project;
import io.hops.hopsworks.common.serving.tf.TfServingException;

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
public class TfServingFacade {
  private final static Logger logger = Logger.getLogger(TfServingFacade.class.getName());

  private final long LOCK_TIMEOUT = 300000L; // 5 minutes

  @PersistenceContext(unitName = "kthfsPU")
  private EntityManager em;

  private String nodeIP = null;

  @PostConstruct
  private void init() {
    try {
      nodeIP = Inet4Address.getLocalHost().getHostAddress();
    } catch (UnknownHostException e) {
      logger.log(Level.SEVERE, "Could not get host address", e);
    }
  }

  protected EntityManager getEntityManager() {
    return em;
  }

  public TfServingFacade() {}

  public List<TfServing> findForProject(Project project) {
    return em.createNamedQuery("TfServing.findByProject", TfServing.class)
        .setParameter("project", project)
        .getResultList();
  }

  @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
  public void delete(TfServing tfServing) {
    // Fetch again the tfServing instance from the DB as the method that calls this
    // doesn't run within a transaction as it needs to do network ops.
    TfServing refetched = em.find(TfServing.class, tfServing.getId());
    if (refetched != null) {
      em.remove(refetched);
    }
  }

  @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
  public TfServing updateDbObject(TfServing newTfServing, Project project) throws TfServingException {
    // Update request - execute this code within a transaction
    TfServing dbTfServing = findByProjectAndId(project, newTfServing.getId());

    if (newTfServing.getModelName() != null && !newTfServing.getModelName().isEmpty()) {
      dbTfServing.setModelName(newTfServing.getModelName());
    }
    if (newTfServing.getModelPath() != null && !newTfServing.getModelPath().isEmpty()) {
      dbTfServing.setModelPath(newTfServing.getModelPath());
    }
    if (newTfServing.getInstances() != null) {
      dbTfServing.setInstances(newTfServing.getInstances());
    }
    if (newTfServing.getVersion() != null) {
      dbTfServing.setVersion(newTfServing.getVersion());
    }

    if (newTfServing.getLocalPid() != null) {
      dbTfServing.setLocalPid(newTfServing.getLocalPid());
    }
    if (newTfServing.getLocalDir() != null) {
      dbTfServing.setLocalDir(newTfServing.getLocalDir());
    }
    if (newTfServing.getLocalPort() != null) {
      dbTfServing.setLocalPort(newTfServing.getLocalPort());
    }

    return merge(dbTfServing);
  }

  public TfServing merge(TfServing tfServing) {
    return em.merge(tfServing);
  }

  public TfServing findByProjectAndId(Project project, Integer id) {
    try {
      return em.createNamedQuery("TfServing.findByProjectAndId", TfServing.class)
          .setParameter("project", project)
          .setParameter("id", id)
          .getSingleResult();
    } catch (NoResultException e) {
      return null;
    }
  }

  public TfServing findByProjectModelName(Project project, String modelName) {
    try {
      return em.createNamedQuery("TfServing.findByProjectModelName", TfServing.class)
          .setParameter("project", project)
          .setParameter("modelName", modelName)
          .getSingleResult();
    } catch (NoResultException e) {
      return null;
    }
  }

  @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
  public TfServing acquireLock(Project project, Integer id) throws TfServingException {
    int retries = 5;

    if (nodeIP == null) {
      logger.log(Level.SEVERE, "nodeIP is null, cannot acquire lock");
      throw new TfServingException(TfServingException.TfServingExceptionErrors.LIFECYCLEERRORINT);
    }

    // Acquire DB read lock on the row
    while (retries > 0) {
      try {
        TfServing tfServing = em.createNamedQuery("TfServing.findByProjectAndId", TfServing.class)
            .setParameter("project", project)
            .setParameter("id", id)
            .setLockMode(LockModeType.PESSIMISTIC_WRITE)
            .getSingleResult();

        if (tfServing == null) {
          throw new TfServingException(TfServingException.TfServingExceptionErrors.INSTANCENOTFOUND);
        }

        if (tfServing.getLockIP() != null &&
            tfServing.getLockTimestamp() > System.currentTimeMillis() - LOCK_TIMEOUT) {
          // There is another request working on this entry. Wait.
          retries--;
          continue;
        }

        tfServing.setLockIP(nodeIP);
        tfServing.setLockTimestamp(System.currentTimeMillis());

        // Lock acquire, return;
        return em.merge(tfServing);
      } catch (LockTimeoutException e) {
        retries--;
      }
    }

    logger.log(Level.FINE, "Could not acquire lock for instance: " + id.toString());
    throw new TfServingException(TfServingException.TfServingExceptionErrors.LIFECYCLEERRORINT);
  }


  @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
  public TfServing releaseLock(Project project, Integer id) throws TfServingException {
    int retries = 5;

    // Acquire DB read lock on the row
    while (retries > 0) {
      try {
        TfServing tfServing = em.createNamedQuery("TfServing.findByProjectAndId", TfServing.class)
            .setParameter("project", project)
            .setParameter("id", id)
            .setLockMode(LockModeType.PESSIMISTIC_WRITE)
            .getSingleResult();

        tfServing.setLockIP(null);
        tfServing.setLockTimestamp(null);

        return em.merge(tfServing);
      } catch (LockTimeoutException e) {
        retries--;
      }
    }

    // Lock will be claimed
    logger.log(Level.FINE, "Could not release lock for instance: " + id.toString());
    throw new TfServingException(TfServingException.TfServingExceptionErrors.LIFECYCLEERRORINT);
  }

  public List<TfServing> getLocalhostRunning() {
    return em.createNamedQuery("TfServing.findLocalhostRunning", TfServing.class)
        .getResultList();
  }
}
