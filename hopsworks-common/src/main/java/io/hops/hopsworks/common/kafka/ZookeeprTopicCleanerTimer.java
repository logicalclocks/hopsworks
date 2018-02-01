/*
 * This file is part of HopsWorks
 *
 * Copyright (C) 2013 - 2018, Logical Clocks AB and RISE SICS AB. All rights reserved.
 *
 * HopsWorks is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * HopsWorks is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with HopsWorks.  If not, see <http://www.gnu.org/licenses/>.
 */

package io.hops.hopsworks.common.kafka;

import io.hops.hopsworks.common.dao.kafka.ProjectTopics;
import io.hops.hopsworks.common.dao.kafka.KafkaFacade;
import io.hops.hopsworks.common.exception.AppException;
import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.ejb.EJB;
import javax.ejb.Schedule;
import javax.ejb.Singleton;
import javax.ejb.Timer;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import kafka.admin.AdminUtils;
import kafka.common.TopicAlreadyMarkedForDeletionException;
import kafka.utils.ZKStringSerializer$;
import kafka.utils.ZkUtils;
import org.I0Itec.zkclient.ZkClient;
import org.I0Itec.zkclient.ZkConnection;
import org.apache.zookeeper.KeeperException;
import org.apache.zookeeper.WatchedEvent;
import org.apache.zookeeper.Watcher;
import org.apache.zookeeper.ZooKeeper;
import io.hops.hopsworks.common.util.Settings;

/**
 * Periodically sync zookeeper with the database for
 * <p>
 */
@Singleton
public class ZookeeprTopicCleanerTimer {

  private final static Logger LOGGER = Logger.getLogger(
      ZookeeprTopicCleanerTimer.class.getName());

  public final int connectionTimeout = 90 * 1000;// 30 seconds

  public int sessionTimeoutMs = 30 * 1000;//30 seconds

  @PersistenceContext(unitName = "kthfsPU")
  private EntityManager em;

  @EJB
  Settings settings;
  @EJB
  KafkaFacade kafkaFacade;

  ZkClient zkClient = null;
  ZkConnection zkConnection = null;
  ZooKeeper zk = null;

  // Run once per hour 
  @Schedule(persistent = false,
      minute = "0",
      hour = "*")
  public void execute(Timer timer) {

    Set<String> zkTopics = new HashSet<>();
    try {
      if (zk == null || !zk.getState().isConnected()) {
        if (zk != null) {
          zk.close();
        }
        zk = new ZooKeeper(settings.getZkConnectStr(),
            sessionTimeoutMs, new ZookeeperWatcher());
      }
      List<String> topics = zk.getChildren("/brokers/topics", false);
      zkTopics.addAll(topics);
    } catch (IOException ex) {
      LOGGER.log(Level.SEVERE, "Unable to find the zookeeper server: ", ex.toString());
    } catch (KeeperException | InterruptedException ex) {
      LOGGER.log(Level.SEVERE, "Cannot retrieve topic list from Zookeeper", ex);
    }

    List<ProjectTopics> dbProjectTopics = em.createNamedQuery("ProjectTopics.findAll").getResultList();
    Set<String> dbTopics = new HashSet<>();

    for (ProjectTopics pt : dbProjectTopics) {
      try {
        dbTopics.add(pt.getProjectTopicsPK().getTopicName());
      } catch (UnsupportedOperationException e) {
        LOGGER.log(Level.SEVERE, e.toString());
      }
    }

    /*
     * To remove topics from zookeeper which do not exist in database. This
     * situation
     * happens when a hopsworks project is deleted, because all the topics in
     * the project
     * will be deleted (cascade delete) without deleting them from the Kafka
     * cluster.
     * 1. get all topics from zookeeper
     * 2. get the topics which exist in zookeeper, but not in database
     * zkTopics.removeAll(dbTopics);
     * 3. remove those topics
     */
    try {
      if (zkClient == null) {
        zkClient = new ZkClient(kafkaFacade.
            getIp(settings.getZkConnectStr()).getHostName(),
            sessionTimeoutMs, connectionTimeout,
            ZKStringSerializer$.MODULE$);
      }
      if (!zkTopics.isEmpty()) {
        zkTopics.removeAll(dbTopics);
        for (String topicName : zkTopics) {
          if (zkConnection == null) {
            zkConnection = new ZkConnection(settings.getZkConnectStr());
          }
          ZkUtils zkUtils = new ZkUtils(zkClient, zkConnection, false);

          try {
            AdminUtils.deleteTopic(zkUtils, topicName);
            LOGGER.log(Level.INFO, "{0} is removed from Zookeeper",
                new Object[]{topicName});
          } catch (TopicAlreadyMarkedForDeletionException ex) {
            LOGGER.log(Level.INFO, "{0} is already marked for deletion",
                new Object[]{topicName});
          }
        }
      }

    } catch (AppException ex) {
      LOGGER.log(Level.SEVERE, "Unable to get zookeeper ip address ", ex.
          toString());
    } finally {
      if (zkClient != null) {
        zkClient.close();
      }
      try {
        if (zkConnection != null) {
          zkConnection.close();
        }
      } catch (InterruptedException ex) {
        LOGGER.log(Level.SEVERE, null, ex);
      }
    }
  }

  /**
   * Get kafka broker endpoints and update them in Settings.
   * These topics are used for passing broker endpoints to HopsUtil and to KafkaFacade.
   */
  @Schedule(persistent = false,
      minute = "*/1",
      hour = "*")
  public void getBrokers() {
    try {
      settings.setKafkaBrokers(settings.getBrokerEndpoints());
    } catch (AppException ex) {
      LOGGER.log(Level.SEVERE, null, ex);
    }
  }

  private class ZookeeperWatcher implements Watcher {

    @Override
    public void process(WatchedEvent we) {
    }
  }
}
