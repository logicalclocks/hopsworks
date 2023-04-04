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

import io.hops.hopsworks.common.dao.jobhistory.ExecutionFacade;
import io.hops.hopsworks.common.featurestore.featuregroup.FeaturegroupFacade;
import io.hops.hopsworks.common.featurestore.trainingdatasets.TrainingDatasetFacade;
import io.hops.hopsworks.common.serving.ServingController;
import io.hops.hopsworks.common.serving.ServingStatusEnum;
import io.hops.hopsworks.common.serving.ServingWrapper;
import io.hops.hopsworks.common.util.Settings;
import io.hops.hopsworks.persistence.entity.featurestore.Featurestore;
import io.hops.hopsworks.persistence.entity.featurestore.featuregroup.Featuregroup;
import io.hops.hopsworks.persistence.entity.featurestore.featuregroup.cached.CachedFeaturegroup;
import io.hops.hopsworks.persistence.entity.featurestore.featuregroup.ondemand.OnDemandFeaturegroup;
import io.hops.hopsworks.persistence.entity.featurestore.featuregroup.stream.StreamFeatureGroup;
import io.hops.hopsworks.persistence.entity.featurestore.trainingdataset.TrainingDataset;
import io.hops.hopsworks.persistence.entity.jobs.history.Execution;
import io.hops.hopsworks.persistence.entity.project.Project;
import io.hops.hopsworks.persistence.entity.serving.Serving;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mockito.Mockito;

import java.util.ArrayList;
import java.util.List;

public class TestQuotasEnforcement {

  @Rule
  public ExpectedException thrown = ExpectedException.none();

  @Test
  public void testQuotasOnlineEnabledFeaturegroups() throws Exception {
    FeaturegroupFacade featuregroupFacade = Mockito.mock(FeaturegroupFacade.class);
    List<Featuregroup> mockedOnlineEnabledFeaturegroups = new ArrayList<>();
    CachedFeaturegroup onlineCachedFG = new CachedFeaturegroup();
    StreamFeatureGroup onlineStreamFG = new StreamFeatureGroup();
    OnDemandFeaturegroup onlineOnDemandFG = new OnDemandFeaturegroup();

    Featuregroup fg0 = new Featuregroup();
    fg0.setCachedFeaturegroup(onlineCachedFG);
    fg0.setOnlineEnabled(true);
    mockedOnlineEnabledFeaturegroups.add(fg0);
    Featuregroup fg1 = new Featuregroup();
    fg1.setStreamFeatureGroup(onlineStreamFG);
    fg1.setOnlineEnabled(true);
    mockedOnlineEnabledFeaturegroups.add(fg1);
    Featuregroup fg2 = new Featuregroup();
    fg2.setOnDemandFeaturegroup(onlineOnDemandFG);
    fg2.setOnlineEnabled(true);
    mockedOnlineEnabledFeaturegroups.add(fg2);

    Mockito.when(featuregroupFacade.findByFeaturestore(Mockito.any())).thenReturn(mockedOnlineEnabledFeaturegroups);

    Settings settings = Mockito.mock(Settings.class);

    QuotasEnforcement qe = new QuotasEnforcement();
    qe.setFeaturegroupFacade(featuregroupFacade);
    qe.setSettings(settings);

    Featurestore fs = new Featurestore();
    Project project = new Project();
    project.setName("ProjectName");
    fs.setProject(project);

    // Test Online enabled
    Mockito.when(settings.getQuotasOnlineDisabledFeaturegroups()).thenReturn(100L);
    // This time it should go through current: 3 max: 4
    Mockito.when(settings.getQuotasOnlineEnabledFeaturegroups()).thenReturn(4L);
    qe.enforceFeaturegroupsQuota(fs, true);

    // This time it should throw an exception current: 3 max: 3
    Mockito.when(settings.getQuotasOnlineEnabledFeaturegroups()).thenReturn(3L);
    thrown.expect(QuotaEnforcementException.class);
    thrown.expectMessage("Online enabled feature groups quota reached");
    qe.enforceFeaturegroupsQuota(fs, true);
  }

  @Test
  public void testQuotasOnlineDisabledFeaturegroups() throws Exception {
    FeaturegroupFacade featuregroupFacade = Mockito.mock(FeaturegroupFacade.class);
    List<Featuregroup> mockedOnlineDisabledFeaturegroups = new ArrayList<>();
    CachedFeaturegroup offlineCachedFG = new CachedFeaturegroup();
    StreamFeatureGroup offlineStreamFG = new StreamFeatureGroup();
    OnDemandFeaturegroup offlineOnDemandFG = new OnDemandFeaturegroup();

    Featuregroup fg0 = new Featuregroup();
    fg0.setCachedFeaturegroup(offlineCachedFG);
    fg0.setOnlineEnabled(false);
    mockedOnlineDisabledFeaturegroups.add(fg0);
    Featuregroup fg1 = new Featuregroup();
    fg1.setStreamFeatureGroup(offlineStreamFG);
    fg1.setOnlineEnabled(false);
    mockedOnlineDisabledFeaturegroups.add(fg1);
    Featuregroup fg2 = new Featuregroup();
    fg2.setOnDemandFeaturegroup(offlineOnDemandFG);
    fg2.setOnlineEnabled(false);
    mockedOnlineDisabledFeaturegroups.add(fg2);

    Mockito.when(featuregroupFacade.findByFeaturestore(Mockito.any())).thenReturn(mockedOnlineDisabledFeaturegroups);

    Settings settings = Mockito.mock(Settings.class);

    QuotasEnforcement qe = new QuotasEnforcement();
    qe.setFeaturegroupFacade(featuregroupFacade);
    qe.setSettings(settings);

    Featurestore fs = new Featurestore();
    Project project = new Project();
    project.setName("ProjectName");
    fs.setProject(project);

    // Test Online disabled
    Mockito.when(settings.getQuotasOnlineEnabledFeaturegroups()).thenReturn(100L);
    // We make sure On-demand/External online disabled FGs are not counted
    // It should pass with max: 3 even though current: 3 already
    Mockito.when(settings.getQuotasOnlineDisabledFeaturegroups()).thenReturn(3L);
    qe.enforceFeaturegroupsQuota(fs, false);

    // This time it should throw an exception current: 3 max: 2
    Mockito.when(settings.getQuotasOnlineDisabledFeaturegroups()).thenReturn(2L);
    thrown.expect(QuotaEnforcementException.class);
    thrown.expectMessage("Online disabled feature groups quota reached");
    qe.enforceFeaturegroupsQuota(fs, false);
  }

  @Test
  public void testIgnoreFeaturegroupQuotas() throws Exception {
    FeaturegroupFacade featuregroupFacade = Mockito.mock(FeaturegroupFacade.class);

    Settings settings = Mockito.mock(Settings.class);
    QuotasEnforcement qe = new QuotasEnforcement();
    qe.setFeaturegroupFacade(featuregroupFacade);
    qe.setSettings(settings);

    Featurestore fs = new Featurestore();
    Project project = new Project();
    project.setName("ProjectName");
    fs.setProject(project);
    fs.setHiveDbId(1L);

    // Quotas disabled
    Mockito.when(settings.getQuotasOnlineEnabledFeaturegroups()).thenReturn(-1L);
    Mockito.when(settings.getQuotasOnlineDisabledFeaturegroups()).thenReturn(-1L);
    qe.enforceFeaturegroupsQuota(fs, false);
    qe.enforceFeaturegroupsQuota(fs, true);
    Mockito.verify(featuregroupFacade, Mockito.never()).findByFeaturestore(Mockito.any());
  }

  @Test
  public void testQuotasTrainingDatasets() throws Exception {
    TrainingDatasetFacade trainingDatasetFacade = Mockito.mock(TrainingDatasetFacade.class);
    List<TrainingDataset> mockedTrainingDS = new ArrayList<>();
    mockedTrainingDS.add(new TrainingDataset());
    mockedTrainingDS.add(new TrainingDataset());
    Mockito.when(trainingDatasetFacade.findByFeaturestore(Mockito.any())).thenReturn(mockedTrainingDS);

    Settings settings = Mockito.mock(Settings.class);

    QuotasEnforcement qe = new QuotasEnforcement();
    qe.setSettings(settings);
    qe.setTrainingDatasetFacade(trainingDatasetFacade);

    Project project = new Project();
    project.setName("ProjectName");
    Featurestore fs = new Featurestore();
    fs.setProject(project);

    // It should go through
    Mockito.when(settings.getQuotasTrainingDatasets()).thenReturn(4L);
    qe.enforceTrainingDatasetsQuota(fs);

    // This time it should throw an exception
    Mockito.when(settings.getQuotasTrainingDatasets()).thenReturn(2L);
    thrown.expect(QuotaEnforcementException.class);
    thrown.expectMessage("Training datasets quota reached for Project");
    qe.enforceTrainingDatasetsQuota(fs);
  }

  @Test
  public void testIgnoreQuotasTrainingDatasets() throws Exception {
    TrainingDatasetFacade trainingDatasetFacade = Mockito.mock(TrainingDatasetFacade.class);

    Settings settings = Mockito.mock(Settings.class);

    QuotasEnforcement qe = new QuotasEnforcement();
    qe.setSettings(settings);
    qe.setTrainingDatasetFacade(trainingDatasetFacade);

    Project project = new Project();
    project.setName("ProjectName");
    Featurestore fs = new Featurestore();
    fs.setProject(project);
    fs.setHiveDbId(1L);

    // It should go through
    Mockito.when(settings.getQuotasTrainingDatasets()).thenReturn(-1L);
    qe.enforceTrainingDatasetsQuota(fs);
    Mockito.verify(trainingDatasetFacade, Mockito.never()).findByFeaturestore(Mockito.any());
  }

  @Test
  public void testQuotasRunningModelDeployments() throws Exception {
    Settings settings = Mockito.mock(Settings.class);
    List<ServingWrapper> mockServings = new ArrayList<>();
  
    ServingWrapper sw0 = new ServingWrapper(new Serving());
    sw0.setStatus(ServingStatusEnum.CREATED);
    mockServings.add(sw0);
  
    ServingWrapper sw1 = new ServingWrapper(new Serving());
    sw1.setStatus(ServingStatusEnum.STARTING);
    mockServings.add(sw1);
  
    ServingWrapper sw2 = new ServingWrapper(new Serving());
    sw2.setStatus(ServingStatusEnum.FAILED);
    mockServings.add(sw2);

    ServingWrapper sw3 = new ServingWrapper(new Serving());
    sw3.setStatus(ServingStatusEnum.RUNNING);
    mockServings.add(sw3);
  
    ServingWrapper sw4 = new ServingWrapper(new Serving());
    sw4.setStatus(ServingStatusEnum.IDLE);
    mockServings.add(sw4);
    
    ServingWrapper sw5 = new ServingWrapper(new Serving());
    sw5.setStatus(ServingStatusEnum.UPDATING);
    mockServings.add(sw5);

    ServingWrapper sw6 = new ServingWrapper(new Serving());
    sw6.setStatus(ServingStatusEnum.STOPPING);
    mockServings.add(sw6);
  
    ServingWrapper sw7 = new ServingWrapper(new Serving());
    sw7.setStatus(ServingStatusEnum.STOPPED);
    mockServings.add(sw7);

    ServingController servingController = Mockito.mock(ServingController.class);
    Mockito.when(servingController.getAll(Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any())).thenReturn(mockServings);

    QuotasEnforcement qe = new QuotasEnforcement();
    qe.setSettings(settings);
    qe.setServingController(servingController);

    Project project = new Project();
    project.setName("ProjectName");

    // It should go through
    Mockito.when(settings.getQuotasRunningModelDeployments()).thenReturn(6L);
    qe.enforceRunningModelDeploymentsQuota(project);

    // Now it should throw an exception
    Mockito.when(settings.getQuotasRunningModelDeployments()).thenReturn(5L);
    thrown.expect(QuotaEnforcementException.class);
    thrown.expectMessage("Running model deployments quota reached for Project");
    qe.enforceRunningModelDeploymentsQuota(project);
  }

  @Test
  public void testIgnoreQuotasRunningModelDeployments() throws Exception {
    ServingController servingController = Mockito.mock(ServingController.class);
    Settings settings = Mockito.mock(Settings.class);
    Mockito.when(settings.getQuotasRunningModelDeployments()).thenReturn(-1L);
    Project project = new Project();
    project.setName("ProjectName");
    QuotasEnforcement qe = new QuotasEnforcement();
    qe.setSettings(settings);
    qe.setServingController(servingController);
    qe.enforceRunningModelDeploymentsQuota(project);
    Mockito.verify(servingController, Mockito.never()).getAll(Mockito.any(), Mockito.any(), Mockito.any(),
      Mockito.any());
  }

  @Test
  public void testQuotasModelDeployments() throws Exception {
    Settings settings = Mockito.mock(Settings.class);
    List<ServingWrapper> mockServings = new ArrayList<>();
    ServingWrapper sw0 = new ServingWrapper(new Serving());
    sw0.setStatus(ServingStatusEnum.STOPPED);
    mockServings.add(sw0);

    ServingWrapper sw1 = new ServingWrapper(new Serving());
    sw1.setStatus(ServingStatusEnum.RUNNING);
    mockServings.add(sw1);

    ServingController servingController = Mockito.mock(ServingController.class);
    Mockito.when(servingController.getAll(Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any())).thenReturn(mockServings);

    QuotasEnforcement qe = new QuotasEnforcement();
    qe.setSettings(settings);
    qe.setServingController(servingController);

    Project project = new Project();
    project.setName("ProjectName");

    // It should go through
    Mockito.when(settings.getQuotasTotalModelDeployments()).thenReturn(3L);
    qe.enforceModelDeploymentsQuota(project);

    // Now it should throw an exception
    Mockito.when(settings.getQuotasTotalModelDeployments()).thenReturn(2L);
    thrown.expect(QuotaEnforcementException.class);
    thrown.expectMessage("Model deployments quota reached for Project");
    qe.enforceModelDeploymentsQuota(project);
  }

  @Test
  public void testIgnoreQuotasModelDeployments() throws Exception {
    ServingController servingController = Mockito.mock(ServingController.class);
    Settings settings = Mockito.mock(Settings.class);
    Mockito.when(settings.getQuotasTotalModelDeployments()).thenReturn(-1L);
    Project project = new Project();
    project.setName("ProjectName");
    QuotasEnforcement qe = new QuotasEnforcement();
    qe.setSettings(settings);
    qe.setServingController(servingController);
    qe.enforceModelDeploymentsQuota(project);
    Mockito.verify(servingController, Mockito.never()).getAll(Mockito.any(), Mockito.any(), Mockito.any(),
      Mockito.any());
  }

  @Test
  public void testQuotasParallelExecutions() throws Exception {
    Settings settings = Mockito.mock(Settings.class);
    ExecutionFacade executionFacade = Mockito.mock(ExecutionFacade.class);
    Mockito.when(settings.getQuotasMaxParallelExecutions()).thenReturn(2L);

    List<Execution> executions = new ArrayList<>();
    executions.add(new Execution());

    Mockito.when(executionFacade.findByProjectAndNotFinished(Mockito.any())).thenReturn(executions);
    Project project = new Project();
    project.setName("project");

    QuotasEnforcement qe = new QuotasEnforcement();
    qe.setSettings(settings);
    qe.setExecutionFacade(executionFacade);

    // This time should go through
    qe.enforceParallelExecutionsQuota(project);

    executions.add(new Execution());
    thrown.expect(QuotaEnforcementException.class);
    thrown.expectMessage("Parallel executions quota reached for Project");
    qe.enforceParallelExecutionsQuota(project);
  }

  @Test
  public void testIgnoreQuotasParallelExecutions() throws Exception {
    Settings settings = Mockito.mock(Settings.class);
    ExecutionFacade executionFacade = Mockito.mock(ExecutionFacade.class);
    Mockito.when(settings.getQuotasMaxParallelExecutions()).thenReturn(-1L);
    Project project = new Project();
    project.setName("project");
    QuotasEnforcement qe = new QuotasEnforcement();
    qe.setSettings(settings);
    qe.setExecutionFacade(executionFacade);
    qe.enforceParallelExecutionsQuota(project);
    Mockito.verify(executionFacade, Mockito.never()).findByProjectAndNotFinished(Mockito.any());
  }
}

