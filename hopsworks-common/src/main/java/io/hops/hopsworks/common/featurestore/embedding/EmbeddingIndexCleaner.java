/*
 * This file is part of Hopsworks
 * Copyright (C) 2024, Hopsworks AB. All rights reserved
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

package io.hops.hopsworks.common.featurestore.embedding;

import com.google.common.collect.Sets;
import io.hops.hopsworks.common.dao.project.ProjectFacade;
import io.hops.hopsworks.common.util.PayaraClusterManager;
import io.hops.hopsworks.exceptions.FeaturestoreException;
import io.hops.hopsworks.persistence.entity.project.Project;
import io.hops.hopsworks.vectordb.Index;
import io.hops.hopsworks.vectordb.VectorDatabaseException;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.annotation.Resource;
import javax.ejb.EJB;
import javax.ejb.Singleton;
import javax.ejb.Startup;
import javax.ejb.Timeout;
import javax.ejb.Timer;
import javax.ejb.TimerConfig;
import javax.ejb.TimerService;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

@Singleton
@Startup
@TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
public class EmbeddingIndexCleaner {

  private final static Logger LOGGER = Logger.getLogger(EmbeddingIndexCleaner.class.getName());

  @Resource
  private TimerService timerService;
  private Timer timer;
  @EJB
  private ProjectFacade projectFacade;
  @EJB
  private VectorDatabaseClient vectorDatabaseClient;
  @EJB
  private EmbeddingController embeddingController;
  @EJB
  private PayaraClusterManager payaraClusterManager;

  @PostConstruct
  public void init() {
    // Schedule the cleaner to run every 6 hours
    timer = timerService.createIntervalTimer(10 * 60 * 1000, 6 * 60 * 60 * 1000,
        new TimerConfig("EmbeddingIndexCleaner", false));

  }

  @PreDestroy
  private void destroyTimer() {
    if (timer != null) {
      timer.cancel();
    }
  }

  @TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
  @Timeout
  public void cleanExpiredIndexes() {
    if (!payaraClusterManager.amIThePrimary()) {
      return;
    }
    LOGGER.log(Level.INFO, "Checking index to be removed");

    try {
      Set<Index> indexesToRemove = getIndexesToRemove();
      for (Index index : indexesToRemove) {
        vectorDatabaseClient.getClient().deleteIndex(index);
        LOGGER.log(Level.INFO, "Removed embedding index: " + index.getName());
      }
    } catch (Exception e) {
      LOGGER.log(Level.SEVERE, "Error occurred while cleaning embedding indexes", e);
    }
  }

  private Set<Index> getIndexesToRemove() throws VectorDatabaseException, FeaturestoreException {
    Set<Index> indexesToRemove = Sets.newHashSet();
    Set<Integer> projectIds = Sets.newHashSet();
    for (Project project : projectFacade.findAll()) {
      projectIds.add(project.getId());
    }
    for (Index index : vectorDatabaseClient.getClient().getAllIndices()) {
      if (embeddingController.isEmbeddingIndex(index.getName()) &&
          !projectIds.contains(embeddingController.getProjectId(index.getName()))) {
        indexesToRemove.add(index);
      }
    }
    return indexesToRemove;
  }
}
