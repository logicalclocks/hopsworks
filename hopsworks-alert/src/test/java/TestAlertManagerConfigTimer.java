/*
 * This file is part of Hopsworks
 * Copyright (C) 2021, Logical Clocks AB. All rights reserved
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

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.fasterxml.jackson.dataformat.yaml.YAMLGenerator;
import com.logicalclocks.servicediscoverclient.exceptions.ServiceDiscoveryException;
import io.hops.hopsworks.alert.AlertManagerConfiguration;
import io.hops.hopsworks.alert.dao.AlertManagerConfigFacade;
import io.hops.hopsworks.alert.dao.AlertReceiverFacade;
import io.hops.hopsworks.alert.exception.AlertManagerUnreachableException;
import io.hops.hopsworks.alerting.api.AlertManagerClient;
import io.hops.hopsworks.alerting.config.AlertManagerConfigController;
import io.hops.hopsworks.alerting.config.dto.AlertManagerConfig;
import io.hops.hopsworks.alerting.config.dto.EmailConfig;
import io.hops.hopsworks.alerting.config.dto.PagerdutyConfig;
import io.hops.hopsworks.alerting.config.dto.Receiver;
import io.hops.hopsworks.alerting.config.dto.SlackConfig;
import io.hops.hopsworks.alerting.exceptions.AlertManagerClientCreateException;
import io.hops.hopsworks.alerting.exceptions.AlertManagerConfigCtrlCreateException;
import io.hops.hopsworks.alerting.exceptions.AlertManagerConfigFileNotFoundException;
import io.hops.hopsworks.alerting.exceptions.AlertManagerConfigReadException;
import io.hops.hopsworks.alerting.exceptions.AlertManagerConfigUpdateException;
import io.hops.hopsworks.persistence.entity.alertmanager.AlertManagerConfigEntity;
import io.hops.hopsworks.persistence.entity.alertmanager.AlertReceiver;
import io.hops.hopsworks.persistence.entity.alertmanager.AlertSeverity;
import io.hops.hopsworks.persistence.entity.alertmanager.AlertType;
import io.hops.hopsworks.persistence.entity.featurestore.Featurestore;
import io.hops.hopsworks.persistence.entity.featurestore.featuregroup.Featuregroup;
import io.hops.hopsworks.persistence.entity.featurestore.featuregroup.datavalidation.alert.FeatureGroupAlert;
import io.hops.hopsworks.persistence.entity.featurestore.featuregroup.datavalidation.alert.ValidationRuleAlertStatus;
import io.hops.hopsworks.persistence.entity.jobs.description.JobAlert;
import io.hops.hopsworks.persistence.entity.jobs.description.JobAlertStatus;
import io.hops.hopsworks.persistence.entity.jobs.description.Jobs;
import io.hops.hopsworks.persistence.entity.project.Project;
import io.hops.hopsworks.persistence.entity.project.alert.ProjectServiceAlert;
import io.hops.hopsworks.persistence.entity.project.alert.ProjectServiceAlertStatus;
import io.hops.hopsworks.persistence.entity.project.service.ProjectServiceEnum;
import org.json.JSONObject;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.mockito.stubbing.Answer;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.Random;
import java.util.logging.Level;
import java.util.logging.Logger;

public class TestAlertManagerConfigTimer {
  private final static Logger LOGGER = Logger.getLogger(TestAlertManagerConfigTimer.class.getName());
  private AlertManagerClient client;
  private AlertManagerConfig alertManagerConfigBackup;
  private AlertManagerConfig alertManagerConfigBackupDB;
  private AlertManagerConfigController alertManagerConfigController;
  private AlertManagerConfigFacade alertManagerConfigFacade;
  private AlertReceiverFacade alertReceiverFacade;
  private AlertManagerConfiguration alertManagerConfiguration;
  private String alertManagerConfigDBPath;

  private Random random = new Random();

  private AlertManagerConfig read(File configFile) throws AlertManagerConfigReadException {
    ObjectMapper objectMapper = new ObjectMapper(new YAMLFactory());
    try {
      return objectMapper.readValue(configFile, AlertManagerConfig.class);
    } catch (IOException e) {
      throw new AlertManagerConfigReadException("Failed to read configuration file. Error " + e.getMessage());
    }
  }

  private void write(AlertManagerConfig alertManagerConfig, File configFile) throws AlertManagerConfigUpdateException {
    YAMLFactory yamlFactory = new YAMLFactory().enable(YAMLGenerator.Feature.MINIMIZE_QUOTES);
    ObjectMapper objectMapper = new ObjectMapper(yamlFactory);
    objectMapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
    try {
      objectMapper.writeValue(configFile, alertManagerConfig);
    } catch (IOException e) {
      throw new AlertManagerConfigUpdateException("Failed to update configuration file. Error " + e.getMessage());
    }
  }

  private String toYaml(Object obj) throws JsonProcessingException {
    YAMLFactory yamlFactory = new YAMLFactory().enable(YAMLGenerator.Feature.MINIMIZE_QUOTES);
    ObjectMapper objectMapper = new ObjectMapper(yamlFactory);
    objectMapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
    return objectMapper.writeValueAsString(obj);
  }

  private JSONObject toJson(AlertManagerConfig alertManagerConfig) throws JsonProcessingException {
    ObjectMapper objectMapper = new ObjectMapper();
    return new JSONObject(objectMapper.writeValueAsString(alertManagerConfig));
  }

  private JSONObject toJson(Receiver receiver) throws JsonProcessingException {
    ObjectMapper objectMapper = new ObjectMapper();
    return new JSONObject(objectMapper.writeValueAsString(receiver));
  }

  private AlertManagerConfig fromJson(JSONObject config) throws IOException {
    ObjectMapper objectMapper = new ObjectMapper();
    return objectMapper.readValue(config.toString(), AlertManagerConfig.class);
  }

  private AlertReceiver createAlertReceiver(Integer id, Receiver receiver, AlertType alertType)
      throws JsonProcessingException {
    AlertReceiver alertReceiver = new AlertReceiver();
    alertReceiver.setId(id);
    alertReceiver.setName(receiver.getName());
    alertReceiver.setConfig(toJson(receiver));

    Project project = new Project("project1");
    Jobs job = new Jobs();
    job.setProject(project);
    job.setName("job1");
    Featurestore featurestore = new Featurestore();
    featurestore.setProject(project);
    Featuregroup featuregroup = new Featuregroup();
    featuregroup.setFeaturestore(featurestore);
    featuregroup.setName("fg1");
    List<JobAlert> jobAlerts = new ArrayList<>();

    jobAlerts.add(new JobAlert(random.nextInt(1000), JobAlertStatus.FAILED, alertType, AlertSeverity.CRITICAL,
        new Date(), job, alertReceiver));
    jobAlerts.add(new JobAlert(random.nextInt(1000), JobAlertStatus.KILLED, alertType, AlertSeverity.WARNING,
        new Date(), job, alertReceiver));
    alertReceiver.setJobAlertCollection(jobAlerts);
    List<FeatureGroupAlert> featureGroupAlerts = new ArrayList<>();
    featureGroupAlerts.add(new FeatureGroupAlert(random.nextInt(1000), ValidationRuleAlertStatus.FAILURE, alertType,
        AlertSeverity.CRITICAL, new Date(), featuregroup, alertReceiver));
    featureGroupAlerts.add(new FeatureGroupAlert(random.nextInt(1000), ValidationRuleAlertStatus.WARNING, alertType,
        AlertSeverity.WARNING, new Date(), featuregroup, alertReceiver));
    alertReceiver.setFeatureGroupAlertCollection(featureGroupAlerts);
    List<ProjectServiceAlert> projectServiceAlerts = new ArrayList<>();
    projectServiceAlerts.add(new ProjectServiceAlert(random.nextInt(1000), ProjectServiceEnum.FEATURESTORE,
        ProjectServiceAlertStatus.VALIDATION_FAILURE, alertType, AlertSeverity.WARNING, new Date(), project,
        alertReceiver));
    projectServiceAlerts.add(new ProjectServiceAlert(random.nextInt(1000), ProjectServiceEnum.JOBS,
        ProjectServiceAlertStatus.JOB_FAILED, alertType, AlertSeverity.WARNING, new Date(), project,
        alertReceiver));
    alertReceiver.setProjectServiceAlertCollection(projectServiceAlerts);
    return alertReceiver;
  }

  private List<AlertReceiver> createReceivers() throws JsonProcessingException {
    List<AlertReceiver> alertReceivers = new ArrayList<>();
    List<EmailConfig> emailConfigs = new ArrayList<>();
    emailConfigs.add(new EmailConfig("test@hopsworks.ai"));
    List<SlackConfig> slackConfigs = new ArrayList<>();
    slackConfigs.add(new SlackConfig().withChannel("@test"));
    List<PagerdutyConfig> pagerdutyConfigs = new ArrayList<>();
    pagerdutyConfigs.add(new PagerdutyConfig("serviceKey"));
    alertReceivers.add(createAlertReceiver(1, new Receiver("global-receiver__email").withEmailConfigs(emailConfigs),
        AlertType.GLOBAL_ALERT_EMAIL));
    alertReceivers.add(createAlertReceiver(2, new Receiver("project1__slack").withSlackConfigs(slackConfigs),
        AlertType.PROJECT_ALERT));
    alertReceivers.add(createAlertReceiver(3, new Receiver("global-receiver__slack").withSlackConfigs(slackConfigs),
        AlertType.GLOBAL_ALERT_SLACK));
    alertReceivers.add(
        createAlertReceiver(4, new Receiver("project1__pagerduty").withPagerdutyConfigs(pagerdutyConfigs),
            AlertType.PROJECT_ALERT));
    return alertReceivers;
  }

  @Before
  public void setUp()
      throws ServiceDiscoveryException, AlertManagerConfigFileNotFoundException, AlertManagerConfigReadException,
      JsonProcessingException {
    String alertmanagerConfigPath = TestAlertManagerConfigTimer.class.getResource("/alertmanager.yml").getPath();
    alertManagerConfigDBPath = TestAlertManagerConfigTimer.class.getResource("/alertmanagerDB.yml").getPath();
    client = Mockito.mock(AlertManagerClient.class);
    alertManagerConfigController = new AlertManagerConfigController.Builder()
        .withConfigPath(alertmanagerConfigPath)
        .withClient(client)
        .build();
    alertManagerConfigFacade = Mockito.mock(AlertManagerConfigFacade.class);
    Mockito.doAnswer((Answer<Void>) invocation -> {
      Object[] args = invocation.getArguments();
      LOGGER.log(Level.INFO, "Save to database: {0}.", args);
      return null;
    }).when(alertManagerConfigFacade).save(Mockito.any());
    Mockito.doAnswer((Answer<Void>) invocation -> {
      Object[] args = invocation.getArguments();
      LOGGER.log(Level.INFO, "Update database: {0}.", args);
      return null;
    }).when(alertManagerConfigFacade).update(Mockito.any());
    AlertManagerConfig alertManagerConfig = read(new File(alertManagerConfigDBPath));
    AlertManagerConfigEntity alertManagerConfigEntity = new AlertManagerConfigEntity();
    alertManagerConfigEntity.setId(1);
    alertManagerConfigEntity.setCreated(new Date());
    alertManagerConfigEntity.setContent(toJson(alertManagerConfig));
    Mockito.when(alertManagerConfigFacade.getLatest()).thenReturn(Optional.of(alertManagerConfigEntity));
    alertReceiverFacade = Mockito.mock(AlertReceiverFacade.class);
    List<AlertReceiver> receivers = createReceivers();
    Mockito.when(alertReceiverFacade.findAll()).thenReturn(receivers);
    Mockito.doAnswer((Answer<Optional<AlertReceiver>>) invocation -> {
      Object[] args = invocation.getArguments();
      Optional<AlertReceiver> alertReceiver =
          receivers.stream().filter(receiver -> receiver.getName().equals(args[0])).findFirst();
      return alertReceiver;
    }).when(alertReceiverFacade).findByName(Mockito.any());
    alertManagerConfiguration = new AlertManagerConfiguration(client, alertManagerConfigController,
        alertManagerConfigFacade, alertReceiverFacade);
    alertManagerConfigBackup = alertManagerConfigController.read();
    alertManagerConfigBackupDB = read(new File(alertManagerConfigDBPath));
  }

  @Test
  public void testRestoreBackup()
      throws AlertManagerUnreachableException, AlertManagerConfigCtrlCreateException, AlertManagerConfigUpdateException,
      IOException, AlertManagerConfigReadException, AlertManagerClientCreateException {
    Mockito.doAnswer((Answer<Void>) invocation -> {
      Object[] args = invocation.getArguments();
      if (args != null && args.length > 0 && args[0] != null) {
        AlertManagerConfigEntity alertManagerConfigEntity = (AlertManagerConfigEntity) args[0];
        AlertManagerConfig alertManagerConfig = fromJson(alertManagerConfigEntity.getContent());
        LOGGER.log(Level.INFO, "Update database routes={0}, receivers={1}.",
            new Object[] {alertManagerConfig.getRoute().getRoutes().size(), alertManagerConfig.getReceivers().size()});
        Assert.assertEquals(15, alertManagerConfig.getRoute().getRoutes().size());
        Assert.assertEquals(9, alertManagerConfig.getReceivers().size());
      }
      return null;
    }).when(alertManagerConfigFacade).update(Mockito.any());
    alertManagerConfiguration.restoreFromBackup();
    AlertManagerConfig alertManagerConfig = alertManagerConfigController.read();
    Assert.assertEquals(15, alertManagerConfig.getRoute().getRoutes().size());
    Assert.assertEquals(9, alertManagerConfig.getReceivers().size());
  }

  @Test
  public void testBackupToDb()
      throws AlertManagerUnreachableException, AlertManagerConfigCtrlCreateException, AlertManagerConfigUpdateException,
      IOException, AlertManagerConfigReadException, AlertManagerClientCreateException {
    Mockito.doAnswer((Answer<Void>) invocation -> {
      Object[] args = invocation.getArguments();
      if (args != null && args.length > 0 && args[0] != null) {
        AlertManagerConfigEntity alertManagerConfigEntity = (AlertManagerConfigEntity) args[0];
        AlertManagerConfig alertManagerConfig = fromJson(alertManagerConfigEntity.getContent());
        LOGGER.log(Level.INFO, "Save to database routes={0}, receivers={1}.",
            new Object[] {alertManagerConfig.getRoute().getRoutes().size(), alertManagerConfig.getReceivers().size()});
        Assert.assertEquals(3, alertManagerConfig.getRoute().getRoutes().size());
        Assert.assertEquals(5, alertManagerConfig.getReceivers().size());
      }
      return null;
    }).when(alertManagerConfigFacade).save(Mockito.any());
    Mockito.when(alertManagerConfigFacade.getLatest()).thenReturn(Optional.empty());
    alertManagerConfiguration.restoreFromBackup();
  }

  @Test
  public void testGlobalFromBackup()
      throws AlertManagerConfigReadException, IOException, AlertManagerUnreachableException,
      AlertManagerConfigCtrlCreateException, AlertManagerConfigUpdateException, AlertManagerClientCreateException {
    AlertManagerConfig alertManagerConfig = read(new File(alertManagerConfigDBPath));
    alertManagerConfig.getGlobal().setSlackApiUrl("https://hooks.slack.com/services/1234567/ASEDWRFDE/XXXXXXXXXXXXXX");
    AlertManagerConfigEntity alertManagerConfigEntity = new AlertManagerConfigEntity();
    alertManagerConfigEntity.setId(1);
    alertManagerConfigEntity.setCreated(new Date());
    alertManagerConfigEntity.setContent(toJson(alertManagerConfig));
    Mockito.when(alertManagerConfigFacade.getLatest()).thenReturn(Optional.of(alertManagerConfigEntity));
    alertManagerConfiguration.restoreFromBackup();
    AlertManagerConfig alertManagerConfig1 = alertManagerConfigController.read();
    Assert.assertEquals("https://hooks.slack.com/services/1234567/ASEDWRFDE/XXXXXXXXXXXXXX",
        alertManagerConfig1.getGlobal().getSlackApiUrl());
  }

  @Test
  public void testReceiverFix()
      throws AlertManagerUnreachableException, AlertManagerConfigCtrlCreateException, AlertManagerConfigUpdateException,
      IOException, AlertManagerConfigReadException, AlertManagerClientCreateException {
    alertManagerConfiguration.runFixConfig();
    AlertManagerConfig alertManagerConfig = alertManagerConfigController.read();
    Assert.assertEquals(15, alertManagerConfig.getRoute().getRoutes().size());
    Assert.assertEquals(6, alertManagerConfig.getReceivers().size());
  }

  @After
  public void tearDown() throws AlertManagerConfigUpdateException {
    alertManagerConfigController.write(alertManagerConfigBackup);
    write(alertManagerConfigBackupDB, new File(alertManagerConfigDBPath));
  }
}
