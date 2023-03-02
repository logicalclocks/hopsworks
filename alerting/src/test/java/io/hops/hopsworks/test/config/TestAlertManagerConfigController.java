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
import io.hops.hopsworks.alerting.config.AlertManagerConfigController;
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

public class TestAlertManagerConfigController {
  private AlertManagerClient client;
  private AlertManagerConfig alertManagerConfigBackup;
  private AlertManagerConfigController alertManagerConfigController;
  
  @Before
  public void setUp()
      throws ServiceDiscoveryException, AlertManagerConfigFileNotFoundException, AlertManagerConfigReadException {
    client = Mockito.mock(AlertManagerClient.class);
    alertManagerConfigController = new AlertManagerConfigController.Builder()
        .withConfigPath(TestAlertManagerConfigController.class.getResource("/alertmanager.yml").getPath())
        .withClient(client)
        .build();
    alertManagerConfigBackup = alertManagerConfigController.read();
  }
  
  @Test
  public void testAddReceiverValidation() throws AlertManagerResponseException, AlertManagerServerException {
    Mockito.when(client.reload()).thenReturn(Response.ok().build());
    List<EmailConfig> emailConfigList = new ArrayList<>();
    Receiver receiver = new Receiver();
    
    Assert.assertThrows(IllegalArgumentException.class, () -> {
      alertManagerConfigController.addReceiver(receiver);
    });
    
    Receiver receiver1 = new Receiver("team-Z-email")
        .withEmailConfigs(emailConfigList);
    
    Assert.assertThrows(IllegalArgumentException.class, () -> {
      alertManagerConfigController.addReceiver(receiver1);
    });
  }
  
  @Test
  public void testAddReceiverEmail()
      throws AlertManagerConfigUpdateException, AlertManagerDuplicateEntryException, AlertManagerResponseException,
      AlertManagerServerException, AlertManagerConfigReadException {
    Mockito.when(client.reload()).thenReturn(Response.ok().build());
    List<EmailConfig> emailConfigList = new ArrayList<>();
    emailConfigList.add(new EmailConfig("team-Z+alerts@example.org"));
    Receiver receiver = new Receiver("team-Z-email")
        .withEmailConfigs(emailConfigList);
    AlertManagerConfig config = alertManagerConfigController.addReceiver(receiver);
    alertManagerConfigController.writeAndReload(config);
    
    AlertManagerConfig alertManagerConfig = this.alertManagerConfigController.read();
    Assert.assertTrue(alertManagerConfig.getReceivers().contains(receiver));
  }
  
  @Test
  public void testAddDuplicateReceiver() throws AlertManagerResponseException, AlertManagerServerException {
    Mockito.when(client.reload()).thenReturn(Response.ok().build());
    List<EmailConfig> emailConfigList = new ArrayList<>();
    emailConfigList.add(new EmailConfig("ermias@kth.se"));
    Receiver receiver = new Receiver("team-X-mails")
        .withEmailConfigs(emailConfigList);
    Assert.assertThrows(AlertManagerDuplicateEntryException.class, () -> {
      alertManagerConfigController.addReceiver(receiver);
    });
  }
  
  @Test
  public void testAddReceiverSlack()
      throws AlertManagerConfigUpdateException, AlertManagerDuplicateEntryException, AlertManagerResponseException,
      AlertManagerServerException, AlertManagerConfigReadException {
    Mockito.when(client.reload()).thenReturn(Response.ok().build());
    List<SlackConfig> slackConfigs = new ArrayList<>();
    slackConfigs.add(new SlackConfig("https://hooks.slack.com/services/1234567890/0987654321", "#general")
        .withIconEmoji("https://gravatar.com/avatar/e3fb1c1d58b043af5e3a6a645b7f569f")
        .withTitle("{{ template \"hopsworks.title\" . }}")
        .withText("{{ template \"hopsworks.text\" . }}"));
    Receiver receiver = new Receiver("team-Z-slack")
        .withSlackConfigs(slackConfigs);
    AlertManagerConfig config = alertManagerConfigController.addReceiver(receiver);
    alertManagerConfigController.writeAndReload(config);
    
    AlertManagerConfig alertManagerConfig = this.alertManagerConfigController.read();
    Assert.assertTrue(alertManagerConfig.getReceivers().contains(receiver));
  }
  
  @Test
  public void testAddEmailValidation() throws AlertManagerResponseException, AlertManagerServerException {
    Mockito.when(client.reload()).thenReturn(Response.ok().build());
    EmailConfig emailConfig = new EmailConfig();
    
    Assert.assertThrows(IllegalArgumentException.class, () -> {
      alertManagerConfigController.addEmailToReceiver("team-X-mails", emailConfig);
    });
  }
  
  @Test
  public void testAddEmailToReceiver()
      throws AlertManagerConfigUpdateException, AlertManagerDuplicateEntryException, AlertManagerNoSuchElementException,
      AlertManagerResponseException, AlertManagerServerException, AlertManagerConfigReadException {
    Mockito.when(client.reload()).thenReturn(Response.ok().build());
    EmailConfig emailConfig = new EmailConfig("team-X+alerts@example.org");
    Receiver receiver = new Receiver("team-X-mails");
    AlertManagerConfig config = alertManagerConfigController.addEmailToReceiver("team-X-mails", emailConfig);
    alertManagerConfigController.writeAndReload(config);
    
    AlertManagerConfig alertManagerConfig = this.alertManagerConfigController.read();
    int index = alertManagerConfig.getReceivers().indexOf(receiver);
    Receiver updatedReceiver = alertManagerConfig.getReceivers().get(index);
    Assert.assertTrue(updatedReceiver.getEmailConfigs().contains(emailConfig));
  }
  
  @Test
  public void testRemoveEmailFromReceiver() throws AlertManagerNoSuchElementException, AlertManagerConfigReadException,
      AlertManagerConfigUpdateException, AlertManagerResponseException, AlertManagerServerException {
    Mockito.when(client.reload()).thenReturn(Response.ok().build());
    EmailConfig emailConfig = new EmailConfig("team-X@example.se");
    Receiver receiver = new Receiver("team-X-mails");
    
    AlertManagerConfig alertManagerConfig = this.alertManagerConfigController.read();
    int index = alertManagerConfig.getReceivers().indexOf(receiver);
    Receiver updatedReceiver = alertManagerConfig.getReceivers().get(index);
    Assert.assertTrue(updatedReceiver.getEmailConfigs().contains(emailConfig));

    AlertManagerConfig config = alertManagerConfigController.removeEmailFromReceiver("team-X-mails", emailConfig);
    alertManagerConfigController.writeAndReload(config);
    
    alertManagerConfig = this.alertManagerConfigController.read();
    index = alertManagerConfig.getReceivers().indexOf(receiver);
    updatedReceiver = alertManagerConfig.getReceivers().get(index);
    Assert.assertFalse(updatedReceiver.getEmailConfigs().contains(emailConfig));
  }
  
  @Test
  public void testAddDuplicateEmailToReceiver() throws AlertManagerResponseException, AlertManagerServerException {
    Mockito.when(client.reload()).thenReturn(Response.ok().build());
    EmailConfig emailConfig = new EmailConfig("team-X@example.se");
    Assert.assertThrows(AlertManagerDuplicateEntryException.class, () -> {
      alertManagerConfigController.addEmailToReceiver("team-X-mails", emailConfig);
    });
  }
  
  @Test
  public void testAddSlackToReceiverValidation() throws AlertManagerResponseException, AlertManagerServerException {
    Mockito.when(client.reload()).thenReturn(Response.ok().build());
    SlackConfig slackConfig = new SlackConfig("https://hooks.slack.com/services/1234567890/0987654321", null);
    
    Assert.assertThrows(IllegalArgumentException.class, () -> {
      alertManagerConfigController.addSlackToReceiver("slack_general", slackConfig);
    });
    
    SlackConfig slackConfig1 = new SlackConfig(null, "#general");
    
    Assert.assertThrows(IllegalArgumentException.class, () -> {
      alertManagerConfigController.addSlackToReceiver("slack_general", slackConfig1);
    });
    
    SlackConfig slackConfig2 = new SlackConfig();
    
    Assert.assertThrows(IllegalArgumentException.class, () -> {
      alertManagerConfigController.addSlackToReceiver("slack_general", slackConfig2);
    });
  }
  
  @Test
  public void testAddSlackToReceiver()
      throws AlertManagerConfigUpdateException, AlertManagerDuplicateEntryException, AlertManagerNoSuchElementException,
      AlertManagerConfigReadException, AlertManagerResponseException, AlertManagerServerException {
    Mockito.when(client.reload()).thenReturn(Response.ok().build());
    SlackConfig slackConfig = new SlackConfig("https://hooks.slack.com/services/1234567890/0987654321", "#general")
        .withIconEmoji("https://gravatar.com/avatar/e3fb1c1d58b043af5e3a6a645b7f569f")
        .withTitle("{{ template \"hopsworks.title\" . }}")
        .withText("{{ template \"hopsworks.text\" . }}");
    Receiver receiver = new Receiver("slack_general");
    AlertManagerConfig config = alertManagerConfigController.addSlackToReceiver("slack_general", slackConfig);
    alertManagerConfigController.writeAndReload(config);
    
    AlertManagerConfig alertManagerConfig = this.alertManagerConfigController.read();
    int index = alertManagerConfig.getReceivers().indexOf(receiver);
    Receiver updatedReceiver = alertManagerConfig.getReceivers().get(index);
    Assert.assertTrue(updatedReceiver.getSlackConfigs().contains(slackConfig));
  }
  
  @Test
  public void testRemoveSlackFromReceiver()
      throws AlertManagerConfigUpdateException, AlertManagerNoSuchElementException, AlertManagerConfigReadException,
      AlertManagerResponseException, AlertManagerServerException {
    Mockito.when(client.reload()).thenReturn(Response.ok().build());
    SlackConfig slackConfig = new SlackConfig("https://hooks.slack.com/services/12345678901/0987654321", "#offtopic");
    Receiver receiver = new Receiver("slack_general");
    
    AlertManagerConfig alertManagerConfig = this.alertManagerConfigController.read();
    int index = alertManagerConfig.getReceivers().indexOf(receiver);
    Receiver updatedReceiver = alertManagerConfig.getReceivers().get(index);
    Assert.assertTrue(updatedReceiver.getSlackConfigs().contains(slackConfig));

    AlertManagerConfig config = alertManagerConfigController.removeSlackFromReceiver("slack_general", slackConfig);
    alertManagerConfigController.writeAndReload(config);
    
    alertManagerConfig = this.alertManagerConfigController.read();
    index = alertManagerConfig.getReceivers().indexOf(receiver);
    updatedReceiver = alertManagerConfig.getReceivers().get(index);
    Assert.assertFalse(updatedReceiver.getSlackConfigs().contains(slackConfig));
  }
  
  @Test
  public void testAddPagerdutyToReceiverValidation() throws AlertManagerResponseException, AlertManagerServerException {
    Mockito.when(client.reload()).thenReturn(Response.ok().build());
    PagerdutyConfig pagerdutyConfig = new PagerdutyConfig().withServiceKey("serviceKey");
    pagerdutyConfig.setRoutingKey("routingKey");
    Assert.assertThrows(IllegalArgumentException.class, () -> {
      alertManagerConfigController.addPagerdutyToReceiver("team-DB-pager", pagerdutyConfig);
    });
    
    PagerdutyConfig pagerdutyConfig1 = new PagerdutyConfig().withServiceKey("<team-DB-key>");
    Assert.assertThrows(AlertManagerDuplicateEntryException.class, () -> {
      alertManagerConfigController.addPagerdutyToReceiver("team-DB-pager", pagerdutyConfig1);
    });
  }
  
  @Test
  public void testAddPagerdutyToReceiver()
      throws AlertManagerConfigUpdateException, AlertManagerDuplicateEntryException, AlertManagerNoSuchElementException,
      AlertManagerConfigReadException, AlertManagerResponseException, AlertManagerServerException {
    Mockito.when(client.reload()).thenReturn(Response.ok().build());
    PagerdutyConfig pagerdutyConfig = new PagerdutyConfig().withServiceKey("serviceKey");
    
    Receiver receiver = new Receiver("team-DB-pager");
    AlertManagerConfig config = alertManagerConfigController.addPagerdutyToReceiver("team-DB-pager", pagerdutyConfig);
    alertManagerConfigController.writeAndReload(config);
    
    AlertManagerConfig alertManagerConfig = this.alertManagerConfigController.read();
    int index = alertManagerConfig.getReceivers().indexOf(receiver);
    Receiver updatedReceiver = alertManagerConfig.getReceivers().get(index);
    Assert.assertTrue(updatedReceiver.getPagerdutyConfigs().contains(pagerdutyConfig));
  }
  
  @Test
  public void testRemovePagerdutyFromReceiver() throws AlertManagerConfigReadException,
      AlertManagerNoSuchElementException, AlertManagerConfigUpdateException, AlertManagerResponseException,
      AlertManagerServerException {
    Mockito.when(client.reload()).thenReturn(Response.ok().build());
    PagerdutyConfig pagerdutyConfig = new PagerdutyConfig().withServiceKey("<team-DB-key>");
    Receiver receiver = new Receiver("team-DB-pager");
    
    AlertManagerConfig alertManagerConfig = this.alertManagerConfigController.read();
    int index = alertManagerConfig.getReceivers().indexOf(receiver);
    Receiver updatedReceiver = alertManagerConfig.getReceivers().get(index);
    Assert.assertTrue(updatedReceiver.getPagerdutyConfigs().contains(pagerdutyConfig));

    AlertManagerConfig config = alertManagerConfigController.removePagerdutyFromReceiver("team-DB-pager", pagerdutyConfig);
    alertManagerConfigController.writeAndReload(config);
    
    alertManagerConfig = this.alertManagerConfigController.read();
    index = alertManagerConfig.getReceivers().indexOf(receiver);
    updatedReceiver = alertManagerConfig.getReceivers().get(index);
    Assert.assertFalse(updatedReceiver.getPagerdutyConfigs().contains(pagerdutyConfig));
  }
  
  @Test
  public void testAddDuplicateSlackToReceiver() throws AlertManagerResponseException, AlertManagerServerException {
    Mockito.when(client.reload()).thenReturn(Response.ok().build());
    SlackConfig slackConfig =
        new SlackConfig("https://hooks.slack.com/services/12345678901/e3fb1c1d58b043af5e3a6a645b7f569f", "#general")
            .withIconEmoji("https://gravatar.com/avatar/e3fb1c1d58b043af5e3a6a645b7f569f")
            .withTitle("{{ template \"hopsworks.title\" . }}")
            .withText("{{ template \"hopsworks.text\" . }}");
    Assert.assertThrows(AlertManagerDuplicateEntryException.class, () -> {
      alertManagerConfigController.addSlackToReceiver("slack_general", slackConfig);
    });
  }
  
  @Test
  public void testRemoveReceiver()
      throws AlertManagerConfigReadException, AlertManagerConfigUpdateException, AlertManagerResponseException,
      AlertManagerServerException {
    Mockito.when(client.reload()).thenReturn(Response.ok().build());
    Receiver receiver = new Receiver("team-Y-mails");
    
    AlertManagerConfig alertManagerConfig = this.alertManagerConfigController.read();
    Assert.assertTrue(alertManagerConfig.getReceivers().contains(receiver));

    AlertManagerConfig config = alertManagerConfigController.removeReceiver("team-Y-mails", true);
    alertManagerConfigController.writeAndReload(config);

    alertManagerConfig = this.alertManagerConfigController.read();
    Assert.assertFalse(alertManagerConfig.getReceivers().contains(receiver));
  }
  
  @Test
  public void testUpdateReceiver()
      throws AlertManagerConfigUpdateException, AlertManagerDuplicateEntryException, AlertManagerNoSuchElementException,
      AlertManagerConfigReadException, AlertManagerResponseException, AlertManagerServerException {
    Mockito.when(client.reload()).thenReturn(Response.ok().build());
    EmailConfig emailConfig = new EmailConfig("team-Y+alerts@example.org");
    List<EmailConfig> emailConfigList = new ArrayList<>();
    emailConfigList.add(emailConfig);
    Receiver receiver = new Receiver("team-Y-mail").withEmailConfigs(emailConfigList);
    
    AlertManagerConfig config = alertManagerConfigController.updateReceiver("team-Y-mails", receiver);
    alertManagerConfigController.writeAndReload(config);
    
    AlertManagerConfig alertManagerConfig = this.alertManagerConfigController.read();
    int index = alertManagerConfig.getReceivers().indexOf(receiver);
    Assert.assertTrue(index > -1);
    Receiver updatedReceiver = alertManagerConfig.getReceivers().get(index);
    Assert.assertTrue(updatedReceiver.getEmailConfigs().contains(emailConfig));
  }
  
  @Test
  public void testUpdateReceiverDuplicate() throws AlertManagerResponseException, AlertManagerServerException {
    Mockito.when(client.reload()).thenReturn(Response.ok().build());
    EmailConfig emailConfig = new EmailConfig("team-Y+alerts@example.org");
    List<EmailConfig> emailConfigList = new ArrayList<>();
    emailConfigList.add(emailConfig);
    Receiver receiver = new Receiver("team-X-mails").withEmailConfigs(emailConfigList);
    
    Assert.assertThrows(AlertManagerDuplicateEntryException.class, () -> {
      alertManagerConfigController.updateReceiver("team-Y-mails", receiver);
    });
  }
  
  @Test
  public void testUpdateReceiverRollback() throws AlertManagerResponseException, AlertManagerServerException,
      AlertManagerConfigReadException {
    Mockito.when(client.reload()).thenThrow(AlertManagerResponseException.class);
    List<EmailConfig> emailConfigList = new ArrayList<>();
    emailConfigList.add(new EmailConfig("team-Z+alerts@example.org"));
    Receiver receiver = new Receiver("team-Z-email").withEmailConfigs(emailConfigList);
    
    Assert.assertThrows(AlertManagerConfigUpdateException.class, () -> {
      AlertManagerConfig config = alertManagerConfigController.addReceiver(receiver);
      alertManagerConfigController.writeAndReload(config);
    });
    
    AlertManagerConfig alertManagerConfig = this.alertManagerConfigController.read();
    Assert.assertFalse(alertManagerConfig.getReceivers().contains(receiver));
  }
  
  @Test
  public void testUpdateReceiverRollbackServerException()
      throws AlertManagerResponseException, AlertManagerServerException, AlertManagerConfigReadException {
    Mockito.when(client.reload()).thenThrow(AlertManagerServerException.class);
    List<EmailConfig> emailConfigList = new ArrayList<>();
    emailConfigList.add(new EmailConfig("team-Z+alerts@example.org"));
    Receiver receiver = new Receiver("team-Z-email").withEmailConfigs(emailConfigList);
    
    Assert.assertThrows(AlertManagerServerException.class, () -> {
      AlertManagerConfig config = alertManagerConfigController.addReceiver(receiver);
      alertManagerConfigController.writeAndReload(config);
    });
    
    AlertManagerConfig alertManagerConfig = this.alertManagerConfigController.read();
    Assert.assertFalse(alertManagerConfig.getReceivers().contains(receiver));
  }
  
  @Test
  public void testAddRouteValidate() throws AlertManagerResponseException, AlertManagerServerException {
    Mockito.when(client.reload()).thenReturn(Response.ok().build());
    Map<String, String> matches = new HashMap<>();
    Route route = new Route();
    Assert.assertThrows(IllegalArgumentException.class, () -> {
      alertManagerConfigController.addRoute(route);
    });
    Route route1 = new Route("team-X-mails").withMatch(matches);
    Assert.assertThrows(IllegalArgumentException.class, () -> {
      alertManagerConfigController.addRoute(route1);
    });
  }
  
  @Test
  public void testAddRoute() throws AlertManagerConfigUpdateException, AlertManagerDuplicateEntryException,
      AlertManagerResponseException, AlertManagerServerException, AlertManagerConfigReadException,
      AlertManagerNoSuchElementException {
    Mockito.when(client.reload()).thenReturn(Response.ok().build());
    Map<String, String> matches = new HashMap<>();
    matches.put("project", "project3");
    Route route = new Route("team-X-mails").withMatch(matches);
    
    AlertManagerConfig config = alertManagerConfigController.addRoute(route);
    alertManagerConfigController.writeAndReload(config);
    
    AlertManagerConfig alertManagerConfig = this.alertManagerConfigController.read();
    Assert.assertTrue(alertManagerConfig.getRoute().getRoutes().contains(route));
  }
  
  @Test
  public void testAddRouteDuplicate() throws AlertManagerResponseException, AlertManagerServerException {
    Mockito.when(client.reload()).thenReturn(Response.ok().build());
    Map<String, String> matches = new HashMap<>();
    matches.put("project", "project1");
    Route route = new Route("slack_general").withMatch(matches);
    
    Assert.assertThrows(AlertManagerDuplicateEntryException.class, () -> {
      alertManagerConfigController.addRoute(route);
    });
  }
  
  @Test
  public void testUpdateRoute() throws AlertManagerConfigUpdateException, AlertManagerDuplicateEntryException,
      AlertManagerNoSuchElementException, AlertManagerConfigReadException, AlertManagerServerException,
      AlertManagerResponseException {
    Mockito.when(client.reload()).thenReturn(Response.ok().build());
    Map<String, String> matches = new HashMap<>();
    matches.put("severity", "critical");
    Route routeToUpdate = new Route("team-Y-mails").withMatch(matches);
    
    matches = new HashMap<>();
    matches.put("project", "project3");
    Route route = new Route("team-Y-mails").withMatch(matches);
    
    AlertManagerConfig config = alertManagerConfigController.updateRoute(routeToUpdate, route);
    alertManagerConfigController.writeAndReload(config);
    
    AlertManagerConfig alertManagerConfig = this.alertManagerConfigController.read();
    Assert.assertTrue(alertManagerConfig.getRoute().getRoutes().contains(route));
  }
  
  @Test
  public void testUpdateRouteDuplicate() throws AlertManagerResponseException, AlertManagerServerException {
    Mockito.when(client.reload()).thenReturn(Response.ok().build());
    
    Map<String, String> matches = new HashMap<>();
    matches.put("severity", "critical");
    Route routeToUpdate = new Route("team-Y-mails").withMatch(matches);
    
    matches = new HashMap<>();
    matches.put("project", "project1");
    Route route = new Route("slack_general").withMatch(matches);
    Assert.assertThrows(AlertManagerDuplicateEntryException.class, () -> {
      alertManagerConfigController.updateRoute(routeToUpdate, route);
    });
  }
  
  @Test
  public void testRemoveRoute()
      throws AlertManagerConfigReadException, AlertManagerConfigUpdateException, AlertManagerResponseException,
      AlertManagerServerException {
    Mockito.when(client.reload()).thenReturn(Response.ok().build());
    Map<String, String> matches = new HashMap<>();
    matches.put("project", "project1");
    Route route = new Route("slack_general").withMatch(matches);
    
    AlertManagerConfig alertManagerConfig = this.alertManagerConfigController.read();
    Assert.assertTrue(alertManagerConfig.getRoute().getRoutes().contains(route));

    AlertManagerConfig config = alertManagerConfigController.removeRoute(route);
    alertManagerConfigController.writeAndReload(config);
    
    alertManagerConfig = this.alertManagerConfigController.read();
    Assert.assertFalse(alertManagerConfig.getRoute().getRoutes().contains(route));
  }
  
  @After
  public void tearDown() throws AlertManagerConfigUpdateException {
    alertManagerConfigController.write(alertManagerConfigBackup);
  }
}
