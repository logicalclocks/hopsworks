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

package io.hops.hopsworks.test.config;

import com.logicalclocks.servicediscoverclient.exceptions.ServiceDiscoveryException;
import io.hops.hopsworks.alerting.api.AlertManagerClient;
import io.hops.hopsworks.alerting.api.ClientWrapper;
import io.hops.hopsworks.alerting.api.alert.dto.PostableAlert;
import io.hops.hopsworks.alerting.config.AlertManagerConfigController;
import io.hops.hopsworks.alerting.config.ConfigUpdater;
import io.hops.hopsworks.alerting.config.dto.AlertManagerConfig;
import io.hops.hopsworks.alerting.config.dto.EmailConfig;
import io.hops.hopsworks.alerting.config.dto.PagerdutyConfig;
import io.hops.hopsworks.alerting.config.dto.Receiver;
import io.hops.hopsworks.alerting.config.dto.Route;
import io.hops.hopsworks.alerting.config.dto.SlackConfig;
import io.hops.hopsworks.alerting.exceptions.AlertManagerConfigFileNotFoundException;
import io.hops.hopsworks.alerting.exceptions.AlertManagerConfigReadException;
import io.hops.hopsworks.alerting.exceptions.AlertManagerConfigUpdateException;
import io.hops.hopsworks.alerting.exceptions.AlertManagerDuplicateEntryException;
import io.hops.hopsworks.alerting.exceptions.AlertManagerException;
import io.hops.hopsworks.alerting.exceptions.AlertManagerNoSuchElementException;
import io.hops.hopsworks.alerting.exceptions.AlertManagerResponseException;
import io.hops.hopsworks.alerting.exceptions.AlertManagerServerException;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import javax.ws.rs.core.Response;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

public class TestAlertManagerConfigController {
  private AlertManagerClient client;
  private AlertManagerConfig alertManagerConfigBackup;
  private AlertManagerConfigController alertManagerConfigController;
  
  @Before
  public void setUp() throws AlertManagerConfigFileNotFoundException, AlertManagerConfigReadException,
      AlertManagerServerException, AlertManagerResponseException {
    client = Mockito.mock(AlertManagerClient.class);
    Mockito.when(client.reload()).thenReturn(Response.ok().build());
    alertManagerConfigController = new AlertManagerConfigController.Builder()
      .withConfigPath(TestAlertManagerConfigController.class.getResource("/alertmanager.yml").getPath())
      .build();
    alertManagerConfigBackup = alertManagerConfigController.read();
  }
  
  @Test
  public void testAddReceiverValidation() throws AlertManagerConfigReadException {
    List<EmailConfig> emailConfigList = new ArrayList<>();
    Receiver receiver = new Receiver();
    AlertManagerConfig alertManagerConfig = alertManagerConfigController.read();
    Assert.assertThrows(IllegalArgumentException.class, () -> {
      ConfigUpdater.addReceiver(alertManagerConfig, receiver);
    });
    
    Receiver receiver1 = new Receiver("team-Z-email")
      .withEmailConfigs(emailConfigList);
    AlertManagerConfig alertManagerConfig1 = alertManagerConfigController.read();
    Assert.assertThrows(IllegalArgumentException.class, () -> {
      ConfigUpdater.addReceiver(alertManagerConfig1, receiver1);
    });
  }
  
  @Test
  public void testAddReceiverEmail()
    throws AlertManagerConfigUpdateException, AlertManagerDuplicateEntryException, AlertManagerServerException,
    AlertManagerConfigReadException {
    List<EmailConfig> emailConfigList = new ArrayList<>();
    emailConfigList.add(new EmailConfig("team-Z+alerts@example.org"));
    Receiver receiver = new Receiver("team-Z-email")
      .withEmailConfigs(emailConfigList);
    AlertManagerConfig alertManagerConfig = alertManagerConfigController.read();
    AlertManagerConfig config = ConfigUpdater.addReceiver(alertManagerConfig, receiver);
    alertManagerConfigController.writeAndReload(config, client);
    
    AlertManagerConfig updatedAlertManagerConfig = this.alertManagerConfigController.read();
    Assert.assertTrue(updatedAlertManagerConfig.getReceivers().contains(receiver));
  }
  
  @Test
  public void testAddDuplicateReceiver() throws AlertManagerConfigReadException {
    List<EmailConfig> emailConfigList = new ArrayList<>();
    emailConfigList.add(new EmailConfig("ermias@kth.se"));
    Receiver receiver = new Receiver("team-X-mails")
      .withEmailConfigs(emailConfigList);
    AlertManagerConfig alertManagerConfig = alertManagerConfigController.read();
    Assert.assertThrows(AlertManagerDuplicateEntryException.class, () -> {
      ConfigUpdater.addReceiver(alertManagerConfig, receiver);
    });
  }
  
  @Test
  public void testAddReceiverSlack()
    throws AlertManagerConfigUpdateException, AlertManagerDuplicateEntryException, AlertManagerServerException,
    AlertManagerConfigReadException {
    List<SlackConfig> slackConfigs = new ArrayList<>();
    slackConfigs.add(new SlackConfig("https://hooks.slack.com/services/1234567890/0987654321", "#general")
      .withIconEmoji("https://gravatar.com/avatar/e3fb1c1d58b043af5e3a6a645b7f569f")
      .withTitle("{{ template \"hopsworks.title\" . }}")
      .withText("{{ template \"hopsworks.text\" . }}"));
    Receiver receiver = new Receiver("team-Z-slack")
      .withSlackConfigs(slackConfigs);
    AlertManagerConfig alertManagerConfig = alertManagerConfigController.read();
    AlertManagerConfig config = ConfigUpdater.addReceiver(alertManagerConfig, receiver);
    alertManagerConfigController.writeAndReload(config, client);
    
    AlertManagerConfig updatedAlertManagerConfig = this.alertManagerConfigController.read();
    Assert.assertTrue(updatedAlertManagerConfig.getReceivers().contains(receiver));
  }
  
  @Test
  public void testAddEmailValidation() throws AlertManagerConfigReadException {
    EmailConfig emailConfig = new EmailConfig();
    AlertManagerConfig alertManagerConfig = alertManagerConfigController.read();
    Assert.assertThrows(IllegalArgumentException.class, () -> {
      ConfigUpdater.addEmailToReceiver(alertManagerConfig, "team-X-mails", emailConfig);
    });
  }
  
  @Test
  public void testAddEmailToReceiver()
    throws AlertManagerConfigUpdateException, AlertManagerDuplicateEntryException, AlertManagerNoSuchElementException,
    AlertManagerServerException, AlertManagerConfigReadException {
    EmailConfig emailConfig = new EmailConfig("team-X+alerts@example.org");
    Receiver receiver = new Receiver("team-X-mails");
    AlertManagerConfig alertManagerConfig = alertManagerConfigController.read();
    AlertManagerConfig config = ConfigUpdater.addEmailToReceiver(alertManagerConfig, "team-X-mails", emailConfig);
    alertManagerConfigController.writeAndReload(config, client);
    
    AlertManagerConfig updatedAlertManagerConfig = this.alertManagerConfigController.read();
    int index = updatedAlertManagerConfig.getReceivers().indexOf(receiver);
    Receiver updatedReceiver = updatedAlertManagerConfig.getReceivers().get(index);
    Assert.assertTrue(updatedReceiver.getEmailConfigs().contains(emailConfig));
  }
  
  @Test
  public void testRemoveEmailFromReceiver() throws AlertManagerNoSuchElementException, AlertManagerConfigReadException,
    AlertManagerConfigUpdateException, AlertManagerServerException {
    EmailConfig emailConfig = new EmailConfig("team-X@example.se");
    Receiver receiver = new Receiver("team-X-mails");
    
    AlertManagerConfig alertManagerConfig = this.alertManagerConfigController.read();
    int index = alertManagerConfig.getReceivers().indexOf(receiver);
    Receiver updatedReceiver = alertManagerConfig.getReceivers().get(index);
    Assert.assertTrue(updatedReceiver.getEmailConfigs().contains(emailConfig));
    
    AlertManagerConfig config = ConfigUpdater.removeEmailFromReceiver(alertManagerConfig, "team-X-mails", emailConfig);
    alertManagerConfigController.writeAndReload(config, client);
    
    alertManagerConfig = this.alertManagerConfigController.read();
    index = alertManagerConfig.getReceivers().indexOf(receiver);
    updatedReceiver = alertManagerConfig.getReceivers().get(index);
    Assert.assertFalse(updatedReceiver.getEmailConfigs().contains(emailConfig));
  }
  
  @Test
  public void testAddDuplicateEmailToReceiver() throws AlertManagerConfigReadException {
    EmailConfig emailConfig = new EmailConfig("team-X@example.se");
    AlertManagerConfig alertManagerConfig = this.alertManagerConfigController.read();
    Assert.assertThrows(AlertManagerDuplicateEntryException.class, () -> {
      ConfigUpdater.addEmailToReceiver(alertManagerConfig, "team-X-mails", emailConfig);
    });
  }
  
  @Test
  public void testAddSlackToReceiverValidation() throws AlertManagerConfigReadException {
    SlackConfig slackConfig = new SlackConfig("https://hooks.slack.com/services/1234567890/0987654321", null);
    AlertManagerConfig alertManagerConfig = this.alertManagerConfigController.read();
    Assert.assertThrows(IllegalArgumentException.class, () -> {
      ConfigUpdater.addSlackToReceiver(alertManagerConfig, "slack_general", slackConfig);
    });
    
    SlackConfig slackConfig1 = new SlackConfig(null, "#general");
    AlertManagerConfig alertManagerConfig1 = this.alertManagerConfigController.read();
    Assert.assertThrows(IllegalArgumentException.class, () -> {
      ConfigUpdater.addSlackToReceiver(alertManagerConfig1, "slack_general", slackConfig1);
    });
    
    SlackConfig slackConfig2 = new SlackConfig();
    AlertManagerConfig alertManagerConfig2 = this.alertManagerConfigController.read();
    Assert.assertThrows(IllegalArgumentException.class, () -> {
      ConfigUpdater.addSlackToReceiver(alertManagerConfig2, "slack_general", slackConfig2);
    });
  }
  
  @Test
  public void testAddSlackToReceiver()
    throws AlertManagerConfigUpdateException, AlertManagerDuplicateEntryException, AlertManagerNoSuchElementException,
    AlertManagerConfigReadException, AlertManagerServerException {
    SlackConfig slackConfig = new SlackConfig("https://hooks.slack.com/services/1234567890/0987654321", "#general")
      .withIconEmoji("https://gravatar.com/avatar/e3fb1c1d58b043af5e3a6a645b7f569f")
      .withTitle("{{ template \"hopsworks.title\" . }}")
      .withText("{{ template \"hopsworks.text\" . }}");
    Receiver receiver = new Receiver("slack_general");
    AlertManagerConfig alertManagerConfig = this.alertManagerConfigController.read();
    AlertManagerConfig config = ConfigUpdater.addSlackToReceiver(alertManagerConfig, "slack_general", slackConfig);
    alertManagerConfigController.writeAndReload(config, client);
    
    AlertManagerConfig updatedAlertManagerConfig = this.alertManagerConfigController.read();
    int index = updatedAlertManagerConfig.getReceivers().indexOf(receiver);
    Receiver updatedReceiver = updatedAlertManagerConfig.getReceivers().get(index);
    Assert.assertTrue(updatedReceiver.getSlackConfigs().contains(slackConfig));
  }
  
  @Test
  public void testRemoveSlackFromReceiver()
    throws AlertManagerConfigUpdateException, AlertManagerNoSuchElementException, AlertManagerConfigReadException,
    AlertManagerServerException {
    SlackConfig slackConfig = new SlackConfig("https://hooks.slack.com/services/12345678901/0987654321", "#offtopic");
    Receiver receiver = new Receiver("slack_general");
    
    AlertManagerConfig alertManagerConfig = this.alertManagerConfigController.read();
    int index = alertManagerConfig.getReceivers().indexOf(receiver);
    Receiver updatedReceiver = alertManagerConfig.getReceivers().get(index);
    Assert.assertTrue(updatedReceiver.getSlackConfigs().contains(slackConfig));
    
    AlertManagerConfig config = ConfigUpdater.removeSlackFromReceiver(alertManagerConfig, "slack_general", slackConfig);
    alertManagerConfigController.writeAndReload(config, client);
    
    alertManagerConfig = this.alertManagerConfigController.read();
    index = alertManagerConfig.getReceivers().indexOf(receiver);
    updatedReceiver = alertManagerConfig.getReceivers().get(index);
    Assert.assertFalse(updatedReceiver.getSlackConfigs().contains(slackConfig));
  }
  
  @Test
  public void testAddPagerdutyToReceiverValidation() throws AlertManagerConfigReadException {
    PagerdutyConfig pagerdutyConfig = new PagerdutyConfig().withServiceKey("serviceKey");
    pagerdutyConfig.setRoutingKey("routingKey");
    AlertManagerConfig alertManagerConfig = this.alertManagerConfigController.read();
    Assert.assertThrows(IllegalArgumentException.class, () -> {
      ConfigUpdater.addPagerdutyToReceiver(alertManagerConfig, "team-DB-pager", pagerdutyConfig);
    });
    
    PagerdutyConfig pagerdutyConfig1 = new PagerdutyConfig().withServiceKey("<team-DB-key>");
    AlertManagerConfig alertManagerConfig1 = this.alertManagerConfigController.read();
    Assert.assertThrows(AlertManagerDuplicateEntryException.class, () -> {
      ConfigUpdater.addPagerdutyToReceiver(alertManagerConfig1, "team-DB-pager", pagerdutyConfig1);
    });
  }
  
  @Test
  public void testAddPagerdutyToReceiver()
    throws AlertManagerConfigUpdateException, AlertManagerDuplicateEntryException, AlertManagerNoSuchElementException,
    AlertManagerConfigReadException, AlertManagerServerException {
    PagerdutyConfig pagerdutyConfig = new PagerdutyConfig().withServiceKey("serviceKey");
    AlertManagerConfig alertManagerConfig = alertManagerConfigController.read();
    Receiver receiver = new Receiver("team-DB-pager");
    AlertManagerConfig config = ConfigUpdater.addPagerdutyToReceiver(alertManagerConfig, "team-DB-pager",
      pagerdutyConfig);
    alertManagerConfigController.writeAndReload(config, client);
    
    alertManagerConfig = this.alertManagerConfigController.read();
    int index = alertManagerConfig.getReceivers().indexOf(receiver);
    Receiver updatedReceiver = alertManagerConfig.getReceivers().get(index);
    Assert.assertTrue(updatedReceiver.getPagerdutyConfigs().contains(pagerdutyConfig));
  }
  
  @Test
  public void testRemovePagerdutyFromReceiver() throws AlertManagerConfigReadException,
    AlertManagerNoSuchElementException, AlertManagerConfigUpdateException, AlertManagerServerException {
    PagerdutyConfig pagerdutyConfig = new PagerdutyConfig().withServiceKey("<team-DB-key>");
    Receiver receiver = new Receiver("team-DB-pager");
    
    AlertManagerConfig alertManagerConfig = this.alertManagerConfigController.read();
    int index = alertManagerConfig.getReceivers().indexOf(receiver);
    Receiver updatedReceiver = alertManagerConfig.getReceivers().get(index);
    Assert.assertTrue(updatedReceiver.getPagerdutyConfigs().contains(pagerdutyConfig));
    
    AlertManagerConfig config =
      ConfigUpdater.removePagerdutyFromReceiver(alertManagerConfig, "team-DB-pager", pagerdutyConfig);
    alertManagerConfigController.writeAndReload(config, client);
    
    alertManagerConfig = this.alertManagerConfigController.read();
    index = alertManagerConfig.getReceivers().indexOf(receiver);
    updatedReceiver = alertManagerConfig.getReceivers().get(index);
    Assert.assertFalse(updatedReceiver.getPagerdutyConfigs().contains(pagerdutyConfig));
  }
  
  @Test
  public void testAddDuplicateSlackToReceiver() throws AlertManagerConfigReadException {
    SlackConfig slackConfig =
      new SlackConfig("https://hooks.slack.com/services/12345678901/e3fb1c1d58b043af5e3a6a645b7f569f", "#general")
        .withIconEmoji("https://gravatar.com/avatar/e3fb1c1d58b043af5e3a6a645b7f569f")
        .withTitle("{{ template \"hopsworks.title\" . }}")
        .withText("{{ template \"hopsworks.text\" . }}");
    AlertManagerConfig alertManagerConfig = this.alertManagerConfigController.read();
    Assert.assertThrows(AlertManagerDuplicateEntryException.class, () -> {
      ConfigUpdater.addSlackToReceiver(alertManagerConfig, "slack_general", slackConfig);
    });
  }
  
  @Test
  public void testRemoveReceiver()
    throws AlertManagerConfigReadException, AlertManagerConfigUpdateException, AlertManagerServerException {
    Receiver receiver = new Receiver("team-Y-mails");
    
    AlertManagerConfig alertManagerConfig = this.alertManagerConfigController.read();
    Assert.assertTrue(alertManagerConfig.getReceivers().contains(receiver));
    
    AlertManagerConfig config = ConfigUpdater.removeReceiver(alertManagerConfig, "team-Y-mails", true);
    alertManagerConfigController.writeAndReload(config, client);
    
    alertManagerConfig = this.alertManagerConfigController.read();
    Assert.assertFalse(alertManagerConfig.getReceivers().contains(receiver));
  }
  
  @Test
  public void testUpdateReceiver()
    throws AlertManagerConfigUpdateException, AlertManagerDuplicateEntryException, AlertManagerNoSuchElementException,
    AlertManagerConfigReadException, AlertManagerServerException {
    EmailConfig emailConfig = new EmailConfig("team-Y+alerts@example.org");
    List<EmailConfig> emailConfigList = new ArrayList<>();
    emailConfigList.add(emailConfig);
    Receiver receiver = new Receiver("team-Y-mail").withEmailConfigs(emailConfigList);
    AlertManagerConfig alertManagerConfig = this.alertManagerConfigController.read();
    AlertManagerConfig config = ConfigUpdater.updateReceiver(alertManagerConfig, "team-Y-mails", receiver);
    alertManagerConfigController.writeAndReload(config, client);
    
    alertManagerConfig = this.alertManagerConfigController.read();
    int index = alertManagerConfig.getReceivers().indexOf(receiver);
    Assert.assertTrue(index > -1);
    Receiver updatedReceiver = alertManagerConfig.getReceivers().get(index);
    Assert.assertTrue(updatedReceiver.getEmailConfigs().contains(emailConfig));
  }
  
  @Test
  public void testUpdateReceiverDuplicate() throws AlertManagerConfigReadException {
    EmailConfig emailConfig = new EmailConfig("team-Y+alerts@example.org");
    List<EmailConfig> emailConfigList = new ArrayList<>();
    emailConfigList.add(emailConfig);
    Receiver receiver = new Receiver("team-X-mails").withEmailConfigs(emailConfigList);
    AlertManagerConfig alertManagerConfig = this.alertManagerConfigController.read();
    Assert.assertThrows(AlertManagerDuplicateEntryException.class, () -> {
      ConfigUpdater.updateReceiver(alertManagerConfig, "team-Y-mails", receiver);
    });
  }
  
  @Test
  public void testUpdateReceiverRollback() throws AlertManagerResponseException, AlertManagerServerException,
      AlertManagerConfigReadException, AlertManagerDuplicateEntryException {
    Mockito.when(client.reload()).thenThrow(AlertManagerResponseException.class);
    List<EmailConfig> emailConfigList = new ArrayList<>();
    emailConfigList.add(new EmailConfig("team-Z+alerts@example.org"));
    Receiver receiver = new Receiver("team-Z-email").withEmailConfigs(emailConfigList);
    AlertManagerConfig alertManagerConfig = this.alertManagerConfigController.read();
    AlertManagerConfig config = ConfigUpdater.addReceiver(alertManagerConfig, receiver);
    Assert.assertThrows(AlertManagerConfigUpdateException.class, () -> {
      alertManagerConfigController.writeAndReload(config, client);
    });
    
    AlertManagerConfig alertManagerConfig1 = this.alertManagerConfigController.read();
    Assert.assertFalse(alertManagerConfig1.getReceivers().contains(receiver));
  }
  
  @Test
  public void testUpdateReceiverRollbackServerException()
    throws AlertManagerResponseException, AlertManagerServerException, AlertManagerConfigReadException,
    AlertManagerDuplicateEntryException {
    Mockito.when(client.reload()).thenThrow(AlertManagerServerException.class);
    List<EmailConfig> emailConfigList = new ArrayList<>();
    emailConfigList.add(new EmailConfig("team-Z+alerts@example.org"));
    Receiver receiver = new Receiver("team-Z-email").withEmailConfigs(emailConfigList);
    AlertManagerConfig alertManagerConfig = this.alertManagerConfigController.read();
    AlertManagerConfig config = ConfigUpdater.addReceiver(alertManagerConfig, receiver);
    Assert.assertThrows(AlertManagerServerException.class, () -> {
      alertManagerConfigController.writeAndReload(config, client);
    });
    
    AlertManagerConfig alertManagerConfig1 = this.alertManagerConfigController.read();
    Assert.assertFalse(alertManagerConfig1.getReceivers().contains(receiver));
  }
  
  @Test
  public void testAddRouteValidate() throws AlertManagerConfigReadException {
    Map<String, String> matches = new HashMap<>();
    Route route = new Route();
    AlertManagerConfig alertManagerConfig = this.alertManagerConfigController.read();
    Assert.assertThrows(IllegalArgumentException.class, () -> {
      ConfigUpdater.addRoute(alertManagerConfig, route);
    });
    Route route1 = new Route("team-X-mails").withMatch(matches);
    AlertManagerConfig alertManagerConfig1 = this.alertManagerConfigController.read();
    Assert.assertThrows(IllegalArgumentException.class, () -> {
      ConfigUpdater.addRoute(alertManagerConfig1, route1);
    });
  }
  
  @Test
  public void testAddRoute() throws AlertManagerConfigUpdateException, AlertManagerDuplicateEntryException,
    AlertManagerServerException, AlertManagerConfigReadException, AlertManagerNoSuchElementException {
    Map<String, String> matches = new HashMap<>();
    matches.put("project", "project3");
    Route route = new Route("team-X-mails").withMatch(matches);
    AlertManagerConfig alertManagerConfig = this.alertManagerConfigController.read();
    AlertManagerConfig config = ConfigUpdater.addRoute(alertManagerConfig, route);
    alertManagerConfigController.writeAndReload(config, client);
    
    alertManagerConfig = this.alertManagerConfigController.read();
    Assert.assertTrue(alertManagerConfig.getRoute().getRoutes().contains(route));
  }
  
  @Test
  public void testAddRouteDuplicate() throws AlertManagerConfigReadException {
    Map<String, String> matches = new HashMap<>();
    matches.put("project", "project1");
    Route route = new Route("slack_general").withMatch(matches);
    AlertManagerConfig alertManagerConfig = this.alertManagerConfigController.read();
    Assert.assertThrows(AlertManagerDuplicateEntryException.class, () -> {
      ConfigUpdater.addRoute(alertManagerConfig, route);
    });
  }
  
  @Test
  public void testUpdateRoute() throws AlertManagerConfigUpdateException, AlertManagerDuplicateEntryException,
    AlertManagerNoSuchElementException, AlertManagerConfigReadException, AlertManagerServerException {
    Map<String, String> matches = new HashMap<>();
    matches.put("severity", "critical");
    Route routeToUpdate = new Route("team-Y-mails").withMatch(matches);
    
    matches = new HashMap<>();
    matches.put("project", "project3");
    Route route = new Route("team-Y-mails").withMatch(matches);
    AlertManagerConfig alertManagerConfig = this.alertManagerConfigController.read();
    AlertManagerConfig config = ConfigUpdater.updateRoute(alertManagerConfig, routeToUpdate, route);
    alertManagerConfigController.writeAndReload(config, client);
    
    alertManagerConfig = this.alertManagerConfigController.read();
    Assert.assertTrue(alertManagerConfig.getRoute().getRoutes().contains(route));
  }
  
  @Test
  public void testUpdateRouteDuplicate() throws AlertManagerConfigReadException {
    Map<String, String> matches = new HashMap<>();
    matches.put("severity", "critical");
    Route routeToUpdate = new Route("team-Y-mails").withMatch(matches);
    
    matches = new HashMap<>();
    matches.put("project", "project1");
    Route route = new Route("slack_general").withMatch(matches);
    AlertManagerConfig alertManagerConfig = this.alertManagerConfigController.read();
    
    Assert.assertThrows(AlertManagerDuplicateEntryException.class, () -> {
      ConfigUpdater.updateRoute(alertManagerConfig, routeToUpdate, route);
    });
  }
  
  @Test
  public void testRemoveRoute()
    throws AlertManagerConfigReadException, AlertManagerConfigUpdateException, AlertManagerServerException {
    Map<String, String> matches = new HashMap<>();
    matches.put("project", "project1");
    Route route = new Route("slack_general").withMatch(matches);
    
    AlertManagerConfig alertManagerConfig = this.alertManagerConfigController.read();
    Assert.assertTrue(alertManagerConfig.getRoute().getRoutes().contains(route));
    
    AlertManagerConfig config = ConfigUpdater.removeRoute(alertManagerConfig, route);
    alertManagerConfigController.writeAndReload(config, client);
    
    alertManagerConfig = this.alertManagerConfigController.read();
    Assert.assertFalse(alertManagerConfig.getRoute().getRoutes().contains(route));
  }
  
  @Test
  public void testPostAlert() throws AlertManagerException {
    List<PostableAlert> postableAlerts = new ArrayList<>();
    ClientWrapper clientWrapper = Mockito.mock(ClientWrapper.class);
    Mockito.when(clientWrapper.postAlerts(postableAlerts)).thenReturn(Response.ok().build());
    List<ClientWrapper> peerClients = new ArrayList<>();
    ClientWrapper clientWrapper1 = Mockito.mock(ClientWrapper.class);
    Mockito.when(clientWrapper1.postAlerts(postableAlerts)).thenReturn(Response.ok().build());
    peerClients.add(clientWrapper1);
    ClientWrapper clientWrapper2 = Mockito.mock(ClientWrapper.class);
    Mockito.when(clientWrapper2.postAlerts(postableAlerts)).thenReturn(Response.ok().build());
    peerClients.add(clientWrapper2);
    
    AlertManagerClient client = new AlertManagerClient(clientWrapper, peerClients);
    client.postAlerts(postableAlerts);
    
    verify(clientWrapper, times(1)).postAlerts(postableAlerts);
    verify(clientWrapper1, times(1)).postAlerts(postableAlerts);
    verify(clientWrapper2, times(1)).postAlerts(postableAlerts);
  }
  
  @Test
  public void testPostAlertOnlyOne() throws AlertManagerException {
    List<PostableAlert> postableAlerts = new ArrayList<>();
    ClientWrapper clientWrapper = Mockito.mock(ClientWrapper.class);
    Mockito.when(clientWrapper.postAlerts(postableAlerts)).thenThrow(new AlertManagerResponseException());
    List<ClientWrapper> peerClients = new ArrayList<>();
    ClientWrapper clientWrapper1 = Mockito.mock(ClientWrapper.class);
    Mockito.when(clientWrapper1.postAlerts(postableAlerts)).thenReturn(Response.ok().build());
    peerClients.add(clientWrapper1);
    ClientWrapper clientWrapper2 = Mockito.mock(ClientWrapper.class);
    Mockito.when(clientWrapper2.postAlerts(postableAlerts)).thenThrow(new AlertManagerResponseException());
    peerClients.add(clientWrapper2);
    
    AlertManagerClient client = new AlertManagerClient(clientWrapper, peerClients);
    client.postAlerts(postableAlerts);
    
    verify(clientWrapper, times(1)).postAlerts(postableAlerts);
    verify(clientWrapper1, times(1)).postAlerts(postableAlerts);
    verify(clientWrapper2, times(1)).postAlerts(postableAlerts);
  }
  
  @Test
  public void testPostAlertResponseFailure() throws AlertManagerException {
    List<PostableAlert> postableAlerts = new ArrayList<>();
    ClientWrapper clientWrapper = Mockito.mock(ClientWrapper.class);
    Mockito.when(clientWrapper.postAlerts(postableAlerts)).thenThrow(new AlertManagerResponseException());
    
    List<ClientWrapper> peerClients = new ArrayList<>();
    ClientWrapper clientWrapper1 = Mockito.mock(ClientWrapper.class);
    Mockito.when(clientWrapper1.postAlerts(postableAlerts)).thenThrow(new AlertManagerResponseException());
    peerClients.add(clientWrapper1);
    ClientWrapper clientWrapper2 = Mockito.mock(ClientWrapper.class);
    Mockito.when(clientWrapper2.postAlerts(postableAlerts)).thenThrow(new AlertManagerResponseException());
    peerClients.add(clientWrapper2);
    
    AlertManagerClient client = new AlertManagerClient(clientWrapper, peerClients);
    Assert.assertThrows(AlertManagerResponseException.class, () -> {
      client.postAlerts(postableAlerts);
    });
    
    verify(clientWrapper, times(1)).postAlerts(postableAlerts);
    verify(clientWrapper1, times(1)).postAlerts(postableAlerts);
    verify(clientWrapper2, times(1)).postAlerts(postableAlerts);
  }
  
  @Test
  public void testPostAlertServiceFailure() throws AlertManagerException {
    List<PostableAlert> postableAlerts = new ArrayList<>();
    ClientWrapper clientWrapper = Mockito.mock(ClientWrapper.class);
    Mockito.when(clientWrapper.postAlerts(postableAlerts)).thenThrow(new AlertManagerServerException());
    
    List<ClientWrapper> peerClients = new ArrayList<>();
    ClientWrapper clientWrapper1 = Mockito.mock(ClientWrapper.class);
    Mockito.when(clientWrapper1.postAlerts(postableAlerts)).thenThrow(new AlertManagerServerException());
    peerClients.add(clientWrapper1);
    ClientWrapper clientWrapper2 = Mockito.mock(ClientWrapper.class);
    Mockito.when(clientWrapper2.postAlerts(postableAlerts)).thenThrow(new AlertManagerServerException());
    peerClients.add(clientWrapper2);
    
    AlertManagerClient client = new AlertManagerClient(clientWrapper, peerClients);
    Assert.assertThrows(AlertManagerServerException.class, () -> {
      client.postAlerts(postableAlerts);
    });
    
    verify(clientWrapper, times(1)).postAlerts(postableAlerts);
    verify(clientWrapper1, times(1)).postAlerts(postableAlerts);
    verify(clientWrapper2, times(1)).postAlerts(postableAlerts);
  }
  
  @Test
  public void testPostAlertResponseAndServiceFailure() throws AlertManagerException {
    List<PostableAlert> postableAlerts = new ArrayList<>();
    ClientWrapper clientWrapper = Mockito.mock(ClientWrapper.class);
    Mockito.when(clientWrapper.postAlerts(postableAlerts)).thenThrow(new AlertManagerServerException());
    
    List<ClientWrapper> peerClients = new ArrayList<>();
    ClientWrapper clientWrapper1 = Mockito.mock(ClientWrapper.class);
    Mockito.when(clientWrapper1.postAlerts(postableAlerts)).thenThrow(new AlertManagerServerException());
    peerClients.add(clientWrapper1);
    ClientWrapper clientWrapper2 = Mockito.mock(ClientWrapper.class);
    Mockito.when(clientWrapper2.postAlerts(postableAlerts)).thenThrow(new AlertManagerResponseException());
    peerClients.add(clientWrapper2);
    
    AlertManagerClient client = new AlertManagerClient(clientWrapper, peerClients);
    //If there is a Response should throw Response exception
    Assert.assertThrows(AlertManagerResponseException.class, () -> {
      client.postAlerts(postableAlerts);
    });
    
    verify(clientWrapper, times(1)).postAlerts(postableAlerts);
    verify(clientWrapper1, times(1)).postAlerts(postableAlerts);
    verify(clientWrapper2, times(1)).postAlerts(postableAlerts);
  }
  
  @After
  public void tearDown() throws AlertManagerConfigUpdateException {
    alertManagerConfigController.write(alertManagerConfigBackup);
  }
}
