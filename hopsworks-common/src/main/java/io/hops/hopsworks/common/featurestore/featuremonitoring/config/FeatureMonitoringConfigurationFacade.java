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

package io.hops.hopsworks.common.featurestore.featuremonitoring.config;

import io.hops.hopsworks.common.dao.AbstractFacade;
import io.hops.hopsworks.persistence.entity.featurestore.featuregroup.Featuregroup;
import io.hops.hopsworks.persistence.entity.featurestore.featuremonitoring.config.FeatureMonitoringConfiguration;
import io.hops.hopsworks.persistence.entity.featurestore.featureview.FeatureView;

import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.NoResultException;
import javax.persistence.PersistenceContext;
import java.util.List;
import java.util.Optional;

@Stateless
public class FeatureMonitoringConfigurationFacade extends AbstractFacade<FeatureMonitoringConfiguration> {
  
  @PersistenceContext(unitName = "kthfsPU")
  private EntityManager em;
  
  @Override
  protected EntityManager getEntityManager() {
    return em;
  }
  
  public FeatureMonitoringConfigurationFacade() {
    super(FeatureMonitoringConfiguration.class);
  }
  
  public List<FeatureMonitoringConfiguration> findByFeatureGroupAndFeatureName(
    Featuregroup featureGroup, String featureName) {
    return em.createNamedQuery(
        "FeatureMonitoringConfiguration.findByFeatureGroupAndFeatureName", FeatureMonitoringConfiguration.class)
      .setParameter("featureGroup", featureGroup)
      .setParameter("featureName", featureName)
      .getResultList();
  }
  
  public List<FeatureMonitoringConfiguration> findByFeatureViewAndFeatureName(
    FeatureView featureView, String featureName) {
    return em.createNamedQuery(
        "FeatureMonitoringConfiguration.findByFeatureViewAndFeatureName", FeatureMonitoringConfiguration.class)
      .setParameter("featureView", featureView)
      .setParameter("featureName", featureName)
      .getResultList();
  }
  
  public List<FeatureMonitoringConfiguration> findByFeatureGroup(Featuregroup featureGroup) {
    return em.createNamedQuery(
        "FeatureMonitoringConfiguration.findByFeatureGroup",
        FeatureMonitoringConfiguration.class)
      .setParameter("featureGroup", featureGroup)
      .getResultList();
  }
  
  public List<FeatureMonitoringConfiguration> findByFeatureView(FeatureView featureView) {
    return em.createNamedQuery(
        "FeatureMonitoringConfiguration.findByFeatureView",
        FeatureMonitoringConfiguration.class)
      .setParameter("featureView", featureView)
      .getResultList();
  }
  
  public Optional<FeatureMonitoringConfiguration> findById(Integer configId) {
    try {
      return Optional.of(
        em.createNamedQuery("FeatureMonitoringConfiguration.findById", FeatureMonitoringConfiguration.class)
          .setParameter("configId", configId).getSingleResult());
    } catch (NoResultException e) {
      return Optional.empty();
    }
  }
  
  public Optional<FeatureMonitoringConfiguration> findByJobId(Integer jobId) {
    try {
      return Optional.of(
        em.createNamedQuery("FeatureMonitoringConfiguration.findByJobId", FeatureMonitoringConfiguration.class)
          .setParameter("jobId", jobId).getSingleResult());
    } catch (NoResultException e) {
      return Optional.empty();
    }
  }
  
  public Optional<FeatureMonitoringConfiguration> findByName(String name) {
    try {
      return Optional.of(
        em.createNamedQuery("FeatureMonitoringConfiguration.findByName", FeatureMonitoringConfiguration.class)
          .setParameter("name", name).getSingleResult());
    } catch (NoResultException e) {
      return Optional.empty();
    }
  }
  
  public Optional<FeatureMonitoringConfiguration> findByFeatureGroupAndName(Featuregroup featureGroup, String name) {
    try {
      return Optional.of(
        em.createNamedQuery("FeatureMonitoringConfiguration.findByFeatureGroupAndName",
            FeatureMonitoringConfiguration.class)
          .setParameter("name", name)
          .setParameter("featureGroup", featureGroup)
          .getSingleResult());
    } catch (NoResultException e) {
      return Optional.empty();
    }
  }
  
  public Optional<FeatureMonitoringConfiguration> findByFeatureViewAndName(FeatureView featureView, String name) {
    try {
      return Optional.of(
        em.createNamedQuery("FeatureMonitoringConfiguration.findByFeatureViewAndName",
            FeatureMonitoringConfiguration.class)
          .setParameter("name", name)
          .setParameter("featureView", featureView)
          .getSingleResult());
    } catch (NoResultException e) {
      return Optional.empty();
    }
  }
}