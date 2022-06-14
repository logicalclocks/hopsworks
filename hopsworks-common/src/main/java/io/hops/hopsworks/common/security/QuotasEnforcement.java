/*
 * This file is part of Hopsworks
 * Copyright (C) 2022, Logical Clocks AB. All rights reserved
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

package io.hops.hopsworks.common.security;

import com.google.common.annotations.VisibleForTesting;
import io.hops.hopsworks.common.dao.jobhistory.ExecutionFacade;
import io.hops.hopsworks.common.featurestore.featuregroup.FeaturegroupFacade;
import io.hops.hopsworks.common.featurestore.trainingdatasets.TrainingDatasetFacade;
import io.hops.hopsworks.common.serving.ServingController;
import io.hops.hopsworks.common.serving.ServingStatusEnum;
import io.hops.hopsworks.common.serving.ServingWrapper;
import io.hops.hopsworks.common.util.Settings;
import io.hops.hopsworks.exceptions.CryptoPasswordNotFoundException;
import io.hops.hopsworks.exceptions.KafkaException;
import io.hops.hopsworks.exceptions.ServingException;
import io.hops.hopsworks.persistence.entity.featurestore.Featurestore;
import io.hops.hopsworks.persistence.entity.featurestore.featuregroup.Featuregroup;
import io.hops.hopsworks.persistence.entity.featurestore.featuregroup.cached.CachedFeaturegroup;
import io.hops.hopsworks.persistence.entity.featurestore.featuregroup.ondemand.OnDemandFeaturegroup;
import io.hops.hopsworks.persistence.entity.featurestore.featuregroup.stream.StreamFeatureGroup;
import io.hops.hopsworks.persistence.entity.featurestore.trainingdataset.TrainingDataset;
import io.hops.hopsworks.persistence.entity.jobs.history.Execution;
import io.hops.hopsworks.persistence.entity.project.Project;

import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.inject.Inject;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

@Stateless
@TransactionAttribute(TransactionAttributeType.REQUIRED)
public class QuotasEnforcement {
  private static final Logger LOGGER = Logger.getLogger(QuotasEnforcement.class.getName());

  private static final long NO_QUOTA = -1L;

  @Inject
  private ServingController servingController;
  @EJB
  private Settings settings;
  @EJB
  private FeaturegroupFacade featuregroupFacade;
  @EJB
  private TrainingDatasetFacade trainingDatasetFacade;
  @EJB
  private ExecutionFacade executionFacade;

  private static final String FEATUREGROUPS_QUOTA_EXCEEDED = "Online %s feature groups quota reached for Project %s. " +
          "Current: %d Max: %d";

  public void enforceFeaturegroupsQuota(Featurestore featurestore, boolean onlineEnabled)
          throws QuotaEnforcementException {
    LOGGER.log(Level.FINE, "Enforcing feature groups quota for Project " + featurestore.getProject().getName());

    if (onlineEnabled) {
      long maxFeaturegroups = getMaxNumberOfOnlineEnabledFeaturegroups();
      if (!shouldIgnoreQuota(maxFeaturegroups)) {
        List<Featuregroup> featuregroups = getFeaturegroups(featurestore);
        enforceFeaturegroupsQuotaInternal(featurestore, featuregroups, maxFeaturegroups, true);
      } else {
        LOGGER.log(Level.FINE, "Skip quotas enforcement for online enabled feature groups because " +
                "configured quota is: " + NO_QUOTA);
      }
    } else {
      long maxFeaturegroups = getMaxNumberOfOnlineDisabledFeaturegroups();
      if (!shouldIgnoreQuota(maxFeaturegroups)) {
        List<Featuregroup> featuregroups = getFeaturegroups(featurestore);
        enforceFeaturegroupsQuotaInternal(featurestore, featuregroups, maxFeaturegroups, false);
      } else {
        LOGGER.log(Level.FINE, "Skip quotas enforcement for online disabled feature groups because " +
                "configured quota is: " + NO_QUOTA);
      }
    }
  }

  private List<Featuregroup> getFeaturegroups(Featurestore featurestore) {
    return featuregroupFacade.findByFeaturestore(featurestore);
  }

  public void enforceTrainingDatasetsQuota(Featurestore featurestore) throws QuotaEnforcementException {
    LOGGER.log(Level.FINE, "Enforcing training dataset quota for Project " + featurestore.getProject().getName());
    long maxNumberOfTrainingDatasets = getMaxNumberOfTrainingDatasets();
    if (shouldIgnoreQuota(maxNumberOfTrainingDatasets)) {
      LOGGER.log(Level.FINE, "Skip quotas enforcement for training datasets because configured quota is " + NO_QUOTA);
      return;
    }
    List<TrainingDataset> trainingDatasets = getTrainingDatasets(featurestore);
    LOGGER.log(Level.FINE,
            "Enforcing quotas for training datasets. Current number of training datasets: " +
                    trainingDatasets.size() + " Configured quota: " + maxNumberOfTrainingDatasets);
    if (quotaExceed(trainingDatasets.size(), maxNumberOfTrainingDatasets)) {
      String exceptionMsg = String.format("Training datasets quota reached for Project %s. Current: %d Max: %d",
              featurestore.getProject().getName(), trainingDatasets.size(), maxNumberOfTrainingDatasets);
      throw new QuotaEnforcementException(exceptionMsg);
    }
  }

  private List<TrainingDataset> getTrainingDatasets(Featurestore featurestore) {
    return trainingDatasetFacade.findByFeaturestore(featurestore);
  }

  public void enforceRunningModelDeploymentsQuota(Project project) throws QuotaEnforcementException {
    LOGGER.log(Level.FINE, "Enforcing Running Model Deployments quota for Project " + project.getName());
    long maxNumberOfRunningDeployments = getMaxNumberOfRunningModelDeployments();
    if (shouldIgnoreQuota(maxNumberOfRunningDeployments)) {
      LOGGER.log(Level.FINE,
              "Skip quotas enforcement for running model deployments because configured quota is " + NO_QUOTA);
      return;
    }
    try {
      long runningDeployments = getAllServings(project).stream().filter(this::isDeploymentRunning).count();
      if (quotaExceed(runningDeployments, maxNumberOfRunningDeployments)) {
        throw new QuotaEnforcementException(String.format("Running model deployments quota reached for Project: %s. " +
                "Current: %s Max: %d", project.getName(), runningDeployments, maxNumberOfRunningDeployments));
      }
    } catch (ServingException | KafkaException | CryptoPasswordNotFoundException ex) {
      String msg = "Failed to enforce Running Model Deployments quotas for Project " + project.getName();
      LOGGER.log(Level.SEVERE, msg, ex);
      throw new QuotaEnforcementException(msg, ex);
    }
  }

  public void enforceModelDeploymentsQuota(Project project) throws QuotaEnforcementException {
    LOGGER.log(Level.FINE, "Enforcing Model Deployments quota for Project: " + project.getName());
    long maxNumberOfModelDeployments = getMaxNumberOfModelDeployments();
    if (shouldIgnoreQuota(maxNumberOfModelDeployments)) {
      LOGGER.log(Level.FINE, "Skip quotas enforcement for model deployments because configured quota is "
              + NO_QUOTA);
      return;
    }
    try {
      List<ServingWrapper> deployments = getAllServings(project);
      if (quotaExceed(deployments.size(), maxNumberOfModelDeployments)) {
        throw new QuotaEnforcementException(String.format("Model deployments quota reached for Project: %s. " +
                "Current: %d Max: %d", project.getName(), deployments.size(), maxNumberOfModelDeployments));
      }
    } catch (ServingException | KafkaException | CryptoPasswordNotFoundException ex) {
      String msg = "Failed to enforce Model Deployments quotas for Project " + project.getName();
      LOGGER.log(Level.SEVERE, msg, ex);
      throw new QuotaEnforcementException(msg, ex);
    }
  }

  public void enforceParallelExecutionsQuota(Project project) throws QuotaEnforcementException {
    LOGGER.log(Level.FINE, "Enforcing Max parallel executions quota for Project: " + project.getName());
    long maxParallelExecutions = getMaxParallelExecutions();
    if (shouldIgnoreQuota(maxParallelExecutions)) {
      LOGGER.log(Level.FINE, "Skip quotas enforcement for parallel executions because configured quota is " + NO_QUOTA);
      return;
    }
    long nonFinishedExecutions = getNonFinishedExecutions(project).size();
    if (quotaExceed(nonFinishedExecutions, maxParallelExecutions)) {
      throw new QuotaEnforcementException(String.format("Parallel executions quota reached for Project: %s " +
          "Current %d Max: %d", project.getName(), nonFinishedExecutions, maxParallelExecutions));
    }
  }

  private List<Execution> getNonFinishedExecutions(Project project) {
    return executionFacade.findByProjectAndNotFinished(project);
  }

  private List<ServingWrapper> getAllServings(Project project) throws ServingException, KafkaException,
          CryptoPasswordNotFoundException {
    return servingController.getAll(project, null, null);
  }

  private boolean isDeploymentRunning(ServingWrapper serving) {
    return serving.getStatus().equals(ServingStatusEnum.RUNNING)
            ||serving.getStatus().equals(ServingStatusEnum.STARTED)
            || serving.getStatus().equals(ServingStatusEnum.STARTING);
  }

  private void enforceFeaturegroupsQuotaInternal(Featurestore featurestore, List<Featuregroup> featuregroups,
          long maxFeaturegroups, boolean online) throws QuotaEnforcementException {
    String typeForException;
    if (online) {
      typeForException = "enabled";
    } else {
      typeForException = "disabled";
    }

    long numFeaturegroups = featuregroups.stream().filter(fg -> {
      CachedFeaturegroup cfg = fg.getCachedFeaturegroup();
      if (cfg != null) {
        return cfg.isOnlineEnabled() == online;
      }
      StreamFeatureGroup sfg = fg.getStreamFeatureGroup();
      if (sfg != null) {
        return sfg.isOnlineEnabled() == online;
      }
      OnDemandFeaturegroup dfg = fg.getOnDemandFeaturegroup();
      if (dfg != null) {
        // External Feature Groups do not count
        return false;
      }
      // Failsafe, anything else (?) will count regardless
      return true;
    }).count();
    LOGGER.log(Level.FINE,
            "Enforcing quotas for online " + typeForException + " feature groups. Current number of feature groups:" +
                    numFeaturegroups + " Configured quota: " + maxFeaturegroups);
    if (quotaExceed(numFeaturegroups, maxFeaturegroups)) {
      throw new QuotaEnforcementException(String.format(FEATUREGROUPS_QUOTA_EXCEEDED, typeForException,
              featurestore.getProject().getName(), numFeaturegroups, maxFeaturegroups));
    }
  }

  private boolean quotaExceed(long current, long max) {
    return current + 1 > max;
  }

  private boolean shouldIgnoreQuota(long quota) {
    return quota == NO_QUOTA;
  }
  private long getMaxNumberOfOnlineEnabledFeaturegroups() {
    return settings.getQuotasOnlineEnabledFeaturegroups();
  }

  private long getMaxNumberOfOnlineDisabledFeaturegroups() {
    return settings.getQuotasOnlineDisabledFeaturegroups();
  }

  private long getMaxNumberOfTrainingDatasets() {
    return settings.getQuotasTrainingDatasets();
  }

  private long getMaxNumberOfRunningModelDeployments() {
    return settings.getQuotasRunningModelDeployments();
  }

  private long getMaxNumberOfModelDeployments() {
    return settings.getQuotasTotalModelDeployments();
  }

  private long getMaxParallelExecutions() {
    return settings.getQuotasMaxParallelExecutions();
  }

  @VisibleForTesting
  public void setFeaturegroupFacade(FeaturegroupFacade featuregroupFacade) {
    this.featuregroupFacade = featuregroupFacade;
  }

  @VisibleForTesting
  public void setSettings(Settings settings) {
    this.settings = settings;
  }

  @VisibleForTesting
  public void setTrainingDatasetFacade(TrainingDatasetFacade trainingDatasetFacade) {
    this.trainingDatasetFacade = trainingDatasetFacade;
  }

  @VisibleForTesting
  public void setServingController(ServingController servingController) {
    this.servingController = servingController;
  }

  @VisibleForTesting
  public void setExecutionFacade(ExecutionFacade executionFacade) {
    this.executionFacade = executionFacade;
  }
}

