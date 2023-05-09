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
package io.hops.hopsworks.common.security;

import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.topic.ITopic;
import com.hazelcast.topic.Message;
import com.hazelcast.topic.MessageListener;
import io.hops.hopsworks.common.util.Settings;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.ejb.ConcurrencyManagement;
import javax.ejb.ConcurrencyManagementType;
import javax.ejb.EJB;
import javax.ejb.Singleton;
import javax.ejb.Startup;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.inject.Inject;
import java.io.File;
import java.io.IOException;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

import static io.hops.hopsworks.common.security.CertificatesMgmService.PASSWORD_UPDATED_TOPIC_NAME;

@Startup
@Singleton
@TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
@ConcurrencyManagement(ConcurrencyManagementType.BEAN)
public class MasterEncryptionPasswordMsgListener {
  private final Logger LOGGER = Logger.getLogger(MasterEncryptionPasswordMsgListener.class.getName());
  @EJB
  private CertificatesMgmService certificatesMgmService;
  @EJB
  private Settings settings;
  @Inject
  private HazelcastInstance hazelcastInstance;
  private UUID myUUID;
  private ITopic<String> masterPasswordUpdatedTopic;
  private File masterPasswordFile;
  
  @PostConstruct
  public void init() {
    masterPasswordFile = new File(settings.getHopsworksMasterEncPasswordFile());
    if (!masterPasswordFile.exists()) {
      throw new IllegalStateException("Master encryption file does not exist");
    }
    if (hazelcastInstance != null) {
      masterPasswordUpdatedTopic = hazelcastInstance.getReliableTopic(PASSWORD_UPDATED_TOPIC_NAME);
      myUUID = masterPasswordUpdatedTopic.addMessageListener(new MessageListenerImpl());
    }
  }
  
  @PreDestroy
  public void destroy() {
    if (masterPasswordUpdatedTopic != null) {
      //needed for redeploy to remove the listener
      masterPasswordUpdatedTopic.removeMessageListener(myUUID);
    }
  }
  
  public class MessageListenerImpl implements MessageListener<String> {
    @Override
    public void onMessage(Message<String> message) {
      LOGGER.log(Level.INFO, "Got master password updated notification from UUID: {0}, This: {1}",
        new Object[]{message.getPublishingMember().getUuid(), message.getPublishingMember().localMember()});
      if (!message.getPublishingMember().localMember()) {
        try {
          certificatesMgmService.updateMasterEncryptionPassword(message.getMessageObject());
        } catch (IOException e) {
          //What should we do if some nodes fail to update???
          LOGGER.log(Level.SEVERE,
            "*** Failed to write new encryption password to file: {0}, Update request PublishingMember: {1}",
            new Object[]{masterPasswordFile.getAbsolutePath(), message.getPublishingMember()});
        }
      }
    }
  }
}
