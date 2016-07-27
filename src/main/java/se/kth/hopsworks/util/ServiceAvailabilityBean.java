/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package se.kth.hopsworks.util;

import io.hops.hdfs.HdfsLeDescriptors;
import io.hops.hdfs.HdfsLeDescriptorsFacade;
import io.hops.kafka.KafkaFacade;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.Set;
import java.util.logging.Logger;
import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import javax.ejb.EJB;
import javax.ejb.Singleton;
import javax.ejb.Timeout;
import javax.ejb.Timer;
import javax.ejb.TimerConfig;
import javax.ejb.TimerService;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.service.Service;
import org.apache.hadoop.yarn.client.api.YarnClient;
import org.apache.hadoop.yarn.client.api.impl.YarnClientImpl;
import org.apache.oozie.client.OozieClient;
import org.apache.zookeeper.ZooKeeper;
import org.elasticsearch.ElasticsearchTimeoutException;
import org.elasticsearch.action.admin.cluster.health.ClusterHealthResponse;
import org.elasticsearch.action.admin.cluster.health.ClusterHealthStatus;
import org.elasticsearch.client.Client;
import org.elasticsearch.client.transport.TransportClient;
import org.elasticsearch.common.transport.InetSocketTransportAddress;
import org.elasticsearch.common.unit.TimeValue;
import se.kth.hopsworks.rest.AppException;

@Singleton
public class ServiceAvailabilityBean {

  private final static Logger logger = Logger.getLogger(ServiceAvailabilityBean.class.getName());


  private static long INTERVAL_MS_BETWEEN_SERVICE_CHECKS = 30 * 1000l;
  public int zkSessionTimeoutMs = 30 * 1000;//30 seconds


  private boolean elasticsearch;
  private boolean namenode;
  private boolean resourcemanager;
  private boolean livy;
  private boolean oozie;
  private boolean elasticIndexer;
  private boolean p2p;
  private boolean ndb;
  private boolean jhs;
  private boolean sparkHistoryServer;
  private boolean zookeeper;
  private boolean kafka;
  
  @EJB
  private KafkaFacade kafkaFacade;
  @EJB
  private Settings settings;
  @EJB
  private HdfsLeDescriptorsFacade hdfsLeDescriptorsFacade;

//    @Schedule(dayOfWeek = "Mon-Sun", month = "*", hour = "*", dayOfMonth = "*", year = "*", minute = "*", second = "0", persistent = true)    
//    public void queryServices() {
//    }
  @Resource
  private TimerService timerSvc;

  @Timeout
  public void checkServices(Timer t) {
    try {
      doActualServiceChecks(t);
    } catch (Exception e) {
      logger.warning("Error: " + e.getMessage());
    }
    scheduleCheckingServices();
  }

  private void doActualServiceChecks(Timer t) {
    // Check NDB

    // Check NameNode
    HdfsLeDescriptors nn = hdfsLeDescriptorsFacade.findEndpoint();
    if (nn == null) {
      this.namenode = false;
    } else {
      this.namenode = true;
    }
    // Check ResourceManager
    try {
      YarnClient yarnClient = new YarnClientImpl();
      Configuration conf = new Configuration();
      yarnClient.init(conf);
      yarnClient.start();
      Service.STATE state = yarnClient.getServiceState();
      if (state == Service.STATE.STARTED) {
        this.resourcemanager = true;
      } else {
        this.resourcemanager = false;
      }
    } catch (Throwable e) {
      this.resourcemanager = false;
      logger.warning("Resourcemanager appears to be down.");
    }
// Check Elastic
 try {
    String addr = this.settings.getElasticIp();

    final org.elasticsearch.common.settings.Settings settings
        = org.elasticsearch.common.settings.Settings.settingsBuilder()
        .put("client.transport.sniff", true) //being able to retrieve other nodes 
        .put("cluster.name", "hops").build();

    Client client = TransportClient.builder().settings(settings).build()
        .addTransportAddress(new InetSocketTransportAddress(new InetSocketAddress(addr, Settings.ELASTIC_PORT)));

   
      final ClusterHealthResponse healthResponse = client.admin().cluster().prepareHealth()
          .setWaitForStatus(ClusterHealthStatus.GREEN).setTimeout(TimeValue.timeValueSeconds(5)).execute().actionGet();
      if (healthResponse.isTimedOut()) {
        elasticsearch = false;
      } else {
        elasticsearch = true;
      }
    } catch (final ElasticsearchTimeoutException e) {
      elasticsearch = false;
    }


// Check P2P Downloader
    // TODO - Call some REST API
// Check Livy
    // TODO
// Check Ooozie
    // TODO

    try{
      OozieClient oozieClient = new OozieClient("http://" + this.settings.getOozieIp() + ":11000/oozie/");
      oozieClient.getSystemMode();
      this.oozie = true;
    }catch(Exception e){
      this.oozie = false;
    }
  }

    
    
        try {
            //Check Kafka

            Set<String> kafkaBrokerEndpoints = kafkaFacade.getBrokerEndpoints();
            if (!kafkaBrokerEndpoints.isEmpty()) {
                kafka = true;
            } else {
                kafka = false;
            }
        } catch (AppException ex) {
            kafka = false;
        }
        
        
      try {
          ZooKeeper zk =  new ZooKeeper(settings.getZkConnectStr(), zkSessionTimeoutMs, null);
          zookeeper = true;
       
      } catch (IOException ex) {
          zookeeper = false;
      }
    }


  @PostConstruct
  public void initialise() {

    scheduleCheckingServices();
  }

  private void scheduleCheckingServices() {
    // When finished checking the availability of services, schedule another check 10 seconds later
    timerSvc.createSingleActionTimer(ServiceAvailabilityBean.INTERVAL_MS_BETWEEN_SERVICE_CHECKS, new TimerConfig());
  }

  public void stop() {
    for (Timer timer : timerSvc.getTimers()) {
      timer.cancel();
    }
  }

  public boolean isElasticsearch() {

    return elasticsearch;
  }

  public boolean isLivy() {
    return livy;
  }

  public boolean isNamenode() {
    return namenode;
  }

  public boolean isNdb() {
    return ndb;
  }

  public boolean isResourcemanager() {
    return resourcemanager;
  }

  public boolean isElasticIndexer() {
    return elasticIndexer;
  }

  public boolean isOozie() {
    return oozie;
  }

  public boolean isP2p() {
    return p2p;
  }

  public boolean isJhs() {
    return jhs;
  }

  public boolean isSparkHistoryServer() {
    return sparkHistoryServer;
  }
  
  public boolean isZookeeper(){
    return zookeeper;
  }
  
  public boolean isKafka(){
    return kafka;
  }

}
