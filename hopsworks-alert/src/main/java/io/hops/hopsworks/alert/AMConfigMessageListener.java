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
package io.hops.hopsworks.alert;

import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.topic.ITopic;
import com.hazelcast.topic.Message;
import com.hazelcast.topic.MessageListener;
import io.hops.hopsworks.alert.util.Constants;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.ejb.EJB;
import javax.ejb.Singleton;
import javax.ejb.Startup;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.inject.Inject;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

@Startup
@Singleton
@TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
public class AMConfigMessageListener {
  private final static Logger LOGGER = Logger.getLogger(AMConfigMessageListener.class.getName());
  private ITopic<String> configUpdatedTopic;
  private UUID msgListenerId;

  @Inject
  private HazelcastInstance hazelcastInstance;
  @EJB
  private AlertManagerConfiguration alertManagerConfiguration;

  @PostConstruct
  public void init() {
    // hazelcastInstance == null if Hazelcast is Disabled
    if (hazelcastInstance != null) {
      configUpdatedTopic = hazelcastInstance.getTopic(Constants.AM_CONFIG_UPDATED_TOPIC_NAME);
      msgListenerId = configUpdatedTopic.addMessageListener(new MessageListenerImpl());
    }
  }

  @PreDestroy
  public void destroy() {
    if (configUpdatedTopic != null) {
      configUpdatedTopic.removeMessageListener(msgListenerId);
    }
  }

  public class MessageListenerImpl implements MessageListener<String> {

    @Override
    public void onMessage(Message<String> message) {
      LOGGER.log(Level.INFO, "Got notification from UUID: {0}, This: {1}, Message: {2}",
        new Object[]{message.getPublishingMember().getUuid(), message.getPublishingMember().localMember(),
          message.getMessageObject()});
      if (!message.getPublishingMember().localMember()) {
        try {
          alertManagerConfiguration.restoreFromDb();
        } catch (Exception e) {
          LOGGER.log(Level.WARNING, "Failed to update alert manager configuration from database. Got notification " +
            "from UUID={0}. {1}", new Object[]{message.getPublishingMember().getUuid(), e.getMessage()});
        }
      }
    }
  }
}
