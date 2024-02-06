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
package io.hops.hopsworks.common.featurestore.featuremonitoring;

import io.hops.hopsworks.common.featurestore.featuremonitoring.alert.FeatureMonitoringAlertController;
import io.hops.hopsworks.exceptions.FeaturestoreException;
import io.hops.hopsworks.persistence.entity.featurestore.featuremonitoring.config.FeatureMonitoringConfiguration;
import io.hops.hopsworks.persistence.entity.featurestore.featuremonitoring.result.FeatureMonitoringResult;
import io.hops.hopsworks.persistence.entity.jobs.scheduler.JobScheduleV2;

import org.junit.Assert;
import org.junit.Test;

public class TestMonitoringAlertValidation {
  
  @Test
  public void testValidateInput() {
    FeatureMonitoringConfiguration config = new FeatureMonitoringConfiguration();
    FeatureMonitoringResult result = new FeatureMonitoringResult();
    FeatureMonitoringAlertController controller = new FeatureMonitoringAlertController();
    
    // assertThrows exception if config is null
    FeaturestoreException exp = Assert.assertThrows(FeaturestoreException.class, () -> {
      controller.triggerAlertsByStatus(null, result);
    });
    Assert.assertEquals("Feature Monitoring Config should not be null.", exp.getUsrMsg());
    
    // assertThrows exception if result is null
    exp = Assert.assertThrows(FeaturestoreException.class, () -> {
      controller.triggerAlertsByStatus(config, null);
    });
    Assert.assertEquals("Feature Monitoring Result should not be null.", exp.getUsrMsg());
    
    // assert throws exception if config is enabled but result is null
    exp = Assert.assertThrows(FeaturestoreException.class, () -> {
      JobScheduleV2 jobScheduleV2 = new JobScheduleV2();
      jobScheduleV2.setEnabled(false);
      config.setJobSchedule(jobScheduleV2);
      controller.triggerAlertsByStatus(config, result);
    });
    Assert.assertEquals("Feature Monitoring Config is disabled, skipping triggering alert.", exp.getUsrMsg());
    
  }
  
}