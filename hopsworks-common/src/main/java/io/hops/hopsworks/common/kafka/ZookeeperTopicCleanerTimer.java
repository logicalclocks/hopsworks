/*
 * Changes to this file committed after and not including commit-id: ccc0d2c5f9a5ac661e60e6eaf138de7889928b8b
 * are released under the following license:
 *
 * This file is part of Hopsworks
 * Copyright (C) 2018, Logical Clocks AB. All rights reserved
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
 *
 * Changes to this file committed before and including commit-id: ccc0d2c5f9a5ac661e60e6eaf138de7889928b8b
 * are released under the following license:
 *
 * Copyright (C) 2013 - 2018, Logical Clocks AB and RISE SICS AB. All rights reserved
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this
 * software and associated documentation files (the "Software"), to deal in the Software
 * without restriction, including without limitation the rights to use, copy, modify, merge,
 * publish, distribute, sublicense, and/or sell copies of the Software, and to permit
 * persons to whom the Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or
 * substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS  OR IMPLIED, INCLUDING
 * BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL  THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM,
 * DAMAGES OR  OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package io.hops.hopsworks.common.kafka;

import com.logicalclocks.servicediscoverclient.exceptions.ServiceDiscoveryException;
import io.hops.hopsworks.common.dao.kafka.HopsKafkaAdminClient;
import io.hops.hopsworks.common.hosts.ServiceDiscoveryController;
import io.hops.hopsworks.common.util.Settings;
import io.hops.hopsworks.persistence.entity.kafka.ProjectTopics;
import org.apache.zookeeper.KeeperException;
import org.apache.zookeeper.WatchedEvent;
import org.apache.zookeeper.Watcher;
import org.apache.zookeeper.ZooKeeper;

import javax.ejb.EJB;
import javax.ejb.Schedule;
import javax.ejb.Singleton;
import javax.ejb.Timer;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Periodically sync zookeeper with the database for
 * <p>
 */
@Singleton
public class ZookeeperTopicCleanerTimer {

  private final static Logger LOGGER = Logger.getLogger(
      ZookeeperTopicCleanerTimer.class.getName());
  
  private final static String offsetTopic = "__consumer_offsets";
  
  @PersistenceContext(unitName = "kthfsPU")
  private EntityManager em;

  @EJB
  private ServiceDiscoveryController serviceDiscoveryController;
  @EJB
  private KafkaBrokers kafkaBrokers;
  @EJB
  private HopsKafkaAdminClient hopsKafkaAdminClient;

  private ZooKeeper zk = null;

  // Run once per hour 
  @Schedule(persistent = false,
      minute = "0",
      hour = "*")
  public void execute(Timer timer) {
    LOGGER.log(Level.FINE, "Running ZookeeperTopicCleanerTimer.");

    try {
      String zkConnectionString = kafkaBrokers.getZookeeperConnectionString();
      Set<String> zkTopics = new HashSet<>();
      try {
        zk = new ZooKeeper(zkConnectionString, Settings.ZOOKEEPER_SESSION_TIMEOUT_MS, new ZookeeperWatcher());
        List<String> topics = zk.getChildren("/brokers/topics", false);
        zkTopics.addAll(topics);
      } catch (IOException ex) {
        LOGGER.log(Level.SEVERE, "Unable to find the zookeeper server: ", ex.toString());
      } catch (KeeperException | InterruptedException ex) {
        LOGGER.log(Level.SEVERE, "Cannot retrieve topic list from Zookeeper", ex);
      } finally {
        if (zk != null) {
          try {
            zk.close();
          } catch (InterruptedException ex) {
            LOGGER.log(Level.SEVERE, "Unable to close zookeeper connection", ex);
          }
          zk = null;
        }
      }

      List<ProjectTopics> dbProjectTopics = em.createNamedQuery("ProjectTopics.findAll").getResultList();
      Set<String> dbTopics = new HashSet<>();

      for (ProjectTopics pt : dbProjectTopics) {
        dbTopics.add(pt.getTopicName());
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
      zkTopics.removeAll(dbTopics);

      // DON'T remove offset topic
      zkTopics.remove(offsetTopic);

      if (!zkTopics.isEmpty()) {
        // blocks until all are deleted
        try {
          hopsKafkaAdminClient.deleteTopics(zkTopics).all().get();
          LOGGER.log(Level.INFO, "Removed topics {0} from Kafka", new Object[]{zkTopics});
        } catch (ExecutionException | InterruptedException ex) {
          LOGGER.log(Level.SEVERE, "Error dropping topics from Kafka", ex);
        }
      }
    } catch (ServiceDiscoveryException ex) {
      LOGGER.log(Level.SEVERE, "Could not discover Zookeeper server addresses", ex);
    } catch (Exception ex) {
      LOGGER.log(Level.SEVERE, "Got an exception while cleaning up kafka topics", ex);
    }
  }

  /**
   * Get kafka broker endpoints and update them in KafkaBrokers
   * These topics are used for passing broker endpoints to HopsUtil and to Kafka controllers.
   */
  @Schedule(persistent = false,
      minute = "*/1",
      hour = "*")
  public void getBrokers() {
    // TODO: This should be removed by HOPSWORKS-2798 and usages of this method should simply call
    //  kafkaBrokers.getBrokerEndpoints() directly
    try {
      kafkaBrokers.setInternalKafkaBrokers(
        kafkaBrokers.getBrokerEndpoints(KafkaBrokers.KAFKA_BROKER_PROTOCOL_INTERNAL));
    } catch (Exception ex) {
      LOGGER.log(Level.SEVERE, null, ex);
    }
  }

  private class ZookeeperWatcher implements Watcher {

    @Override
    public void process(WatchedEvent we) {
    }
  }
}
