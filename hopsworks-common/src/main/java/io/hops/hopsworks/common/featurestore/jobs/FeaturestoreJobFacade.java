/*
 * This file is part of Hopsworks
 * Copyright (C) 2019, Logical Clocks AB. All rights reserved
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

package io.hops.hopsworks.common.featurestore.jobs;

import io.hops.hopsworks.common.dao.AbstractFacade;
import io.hops.hopsworks.persistence.entity.featurestore.featuregroup.Featuregroup;
import io.hops.hopsworks.persistence.entity.featurestore.jobs.FeaturestoreJob;
import io.hops.hopsworks.persistence.entity.featurestore.trainingdataset.TrainingDataset;
import io.hops.hopsworks.persistence.entity.jobs.description.Jobs;

import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import java.util.List;

/**
 * A facade for the feature_store_job table in the Hopsworks database,
 * use this interface when performing database operations against the table.
 */
@Stateless
@TransactionAttribute(TransactionAttributeType.REQUIRED)
public class FeaturestoreJobFacade extends AbstractFacade<FeaturestoreJob> {

  @PersistenceContext(unitName = "kthfsPU")
  private EntityManager em;

  public FeaturestoreJobFacade() {
    super(FeaturestoreJob.class);
  }


  /**
   * Insert a list of Hopsworks Jobs as Feature Store Jobs linked to a training dataset
   *
   * @param trainingDataset the training dataset to link the jobs to
   * @param jobs the jobs to insert
   */
  public void insertJobs(TrainingDataset trainingDataset, List<Jobs> jobs){
    if(jobs != null){
      jobs.stream().forEach(job -> {
        if(!isJobExists((List) trainingDataset.getJobs(), job)) {
          FeaturestoreJob featurestoreJob = new FeaturestoreJob();
          featurestoreJob.setTrainingDataset(trainingDataset);
          featurestoreJob.setJob(job);
          featurestoreJob.setFeaturegroup(null);
          em.persist(featurestoreJob);
        }
      });
      em.flush();
    }
  }

  /**
   * Insert a list of Hopsworks Jobs as Feature Store Jobs linked to a feature group
   *
   * @param featuregroup the featuregroup to link the jobs to
   * @param jobs the jobs to insert
   */
  public void insertJobs(Featuregroup featuregroup, List<Jobs> jobs){
    if(jobs != null){
      jobs.stream().forEach(job -> {
        if(!isJobExists((List) featuregroup.getJobs(), job)) {
          FeaturestoreJob featurestoreJob = new FeaturestoreJob();
          featurestoreJob.setTrainingDataset(null);
          featurestoreJob.setJob(job);
          featurestoreJob.setFeaturegroup(featuregroup);
          em.persist(featurestoreJob);
        }
      });
      em.flush();
    }
  }

  /**
   * Get all the jobs for a specific feature group
   * @param featuregroup
   * @return
   */
  public List<FeaturestoreJob> getByFeatureGroup(Featuregroup featuregroup) {
    return em.createNamedQuery("FeaturestoreJob.findByFeaturegroup", FeaturestoreJob.class)
        .setParameter("featuregroup", featuregroup)
        .getResultList();
  }

  /**
   * Get all the jobs for a specific feature group
   * @param trainingDataset
   * @return
   */
  public List<FeaturestoreJob> getByTrainingDataset(TrainingDataset trainingDataset) {
    return em.createNamedQuery("FeaturestoreJob.findByTrainingDataset", FeaturestoreJob.class)
        .setParameter("trainingDataset", trainingDataset)
        .getResultList();
  }

  /**
   * Check if a job exists in a list of jobs
   *
   * @param jobs the list of jobs to search
   * @param job the job to search for
   * @return true if there is a match, otherwise false
   */
  private Boolean isJobExists(List<FeaturestoreJob> jobs, Jobs job) {
    return jobs.stream().anyMatch(fsjob -> fsjob.getJob().getId().equals(job.getId()));
  }

  /**
   * Gets the entity manager of the facade
   *
   * @return entity manager
   */
  @Override
  protected EntityManager getEntityManager() {
    return em;
  }

}
