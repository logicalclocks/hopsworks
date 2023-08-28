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

import io.hops.hopsworks.common.dao.kafka.HopsKafkaAdminClient;
import io.hops.hopsworks.common.util.PayaraClusterManager;
import io.hops.hopsworks.persistence.entity.kafka.ProjectTopics;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.annotation.Resource;
import javax.ejb.EJB;
import javax.ejb.Schedule;
import javax.ejb.ScheduleExpression;
import javax.ejb.Singleton;
import javax.ejb.Startup;
import javax.ejb.Timeout;
import javax.ejb.Timer;
import javax.ejb.TimerConfig;
import javax.ejb.TimerService;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

/**
 * Periodically sync zookeeper with the database for
 * <p>
 */
@Startup
@Singleton
public class ZookeeperTopicCleanerTimer {

  private final static Logger LOGGER = Logger.getLogger(ZookeeperTopicCleanerTimer.class.getName());

  private final static String offsetTopic = "__consumer_offsets";

  @PersistenceContext(unitName = "kthfsPU")
  private EntityManager em;

  @EJB
  private KafkaBrokers kafkaBrokers;
  @EJB
  private HopsKafkaAdminClient hopsKafkaAdminClient;
  @EJB
  private PayaraClusterManager payaraClusterManager;
  @Resource
  private TimerService timerService;
  private Timer timer;

  @PostConstruct
  public void init() {
    ScheduleExpression schedule = new ScheduleExpression();
    schedule.hour("*");
    timer = timerService.createCalendarTimer(schedule, new TimerConfig("Zookeeper Topic Cleaner", false));
  }

  @PreDestroy
  public void destroy() {
    if (timer != null) {
      timer.cancel();
    }
  }

  // Run once per hour
  @Timeout
  public void execute(Timer timer) {
    if (!payaraClusterManager.amIThePrimary()) {
      return;
    }
    LOGGER.log(Level.FINE, "Running ZookeeperTopicCleanerTimer.");

    try {
      Set<String> zkTopics = hopsKafkaAdminClient.listTopics().names().get();
      Set<String> dbTopics = em.createNamedQuery("ProjectTopics.findAll", ProjectTopics.class)
              .getResultList()
              .stream()
              .map(ProjectTopics::getTopicName)
              .collect(Collectors.toSet());

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
    }  catch (Exception ex) {
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
  public void setBrokers() {
    // TODO: This should be removed by HOPSWORKS-2798 and usages of this method should simply call
    //  kafkaBrokers.getBrokerEndpoints() directly
    try {
      kafkaBrokers.setBrokerEndpoints();
    } catch (Exception ex) {
      LOGGER.log(Level.SEVERE, null, ex);
    }
  }
}