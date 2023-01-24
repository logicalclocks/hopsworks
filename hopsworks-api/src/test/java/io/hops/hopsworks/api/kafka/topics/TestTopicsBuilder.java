/*
 * This file is part of Hopsworks
 * Copyright (C) 2023, Hopsworks AB. All rights reserved
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
package io.hops.hopsworks.api.kafka.topics;

import io.hops.hopsworks.common.kafka.KafkaController;
import io.hops.hopsworks.exceptions.KafkaException;
import io.hops.hopsworks.persistence.entity.project.Project;
import org.apache.kafka.common.KafkaFuture;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import javax.ws.rs.core.UriBuilder;
import javax.ws.rs.core.UriInfo;
import java.util.concurrent.TimeUnit;

public class TestTopicsBuilder {
  
  private UriInfo mockUriInfo;
  private TopicsBuilder topicsBuilder;
  private KafkaController mockKafkaController;
  private Project project;
  
  @Before
  public void setup() {
    
    mockUriInfo = Mockito.mock(UriInfo.class);
    UriBuilder mockUriBuilder = Mockito.mock(UriBuilder.class);
    Mockito.when(mockUriBuilder.path(Mockito.anyString()))
      .thenReturn(mockUriBuilder);
    Mockito.when(mockUriInfo.getBaseUriBuilder())
      .thenReturn(mockUriBuilder);
    
    mockKafkaController = Mockito.mock(KafkaController.class);
    
    topicsBuilder = new TopicsBuilder(mockKafkaController);
    project = new Project();
    project.setId(1);
  }
  
  @Test
  public void testBuildTopicDetails_missingFeatureGroupTopic() throws Exception {
    String topicName = "119_20_ge_transactions_fg_1";
    String topicNameOnline = "119_22_transactions_4h_aggs_1_onlinefs";
  
    KafkaFuture mockKafkaFuture = Mockito.mock(KafkaFuture.class);
  
    Mockito.when(mockKafkaController.getTopicDetails(Mockito.any(), Mockito.any())).thenReturn(mockKafkaFuture);
    Mockito.when(mockKafkaFuture.get(3000, TimeUnit.MILLISECONDS)).thenThrow(new InterruptedException());
  
    KafkaException e = Assert.assertThrows(
      KafkaException.class, () -> topicsBuilder.buildTopicDetails(mockUriInfo, project, topicName));
    
    Assert.assertEquals("Details for Feature Group Topic `119_20_ge_transactions_fg_1` can currently not be shown. Write to the corresponding Feature Group to auto create the topic.", e.getUsrMsg());
  
    e = Assert.assertThrows(
      KafkaException.class, () -> topicsBuilder.buildTopicDetails(mockUriInfo, project, topicNameOnline));
  
    Assert.assertEquals("Details for Feature Group Topic `119_22_transactions_4h_aggs_1_onlinefs` can currently not be shown. Write to the corresponding Feature Group to auto create the topic.", e.getUsrMsg());
  }
}
