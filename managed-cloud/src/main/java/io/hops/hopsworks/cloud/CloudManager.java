/*
 * Copyright (C) 2020, Logical Clocks AB. All rights reserved
 */
package io.hops.hopsworks.cloud;

import com.google.common.collect.Lists;
import io.hops.hopsworks.cloud.dao.heartbeat.HeartbeatRequest;
import io.hops.hopsworks.cloud.dao.heartbeat.HeartbeatResponse;
import io.hops.hopsworks.cloud.dao.heartbeat.commands.CloudCommandType;
import io.hops.hopsworks.cloud.dao.heartbeat.commands.CommandStatus;
import io.hops.hopsworks.cloud.dao.heartbeat.commands.RemoveNodesCommand;
import com.google.common.annotations.VisibleForTesting;
import com.logicalclocks.servicediscoverclient.exceptions.ServiceDiscoveryException;
import com.logicalclocks.servicediscoverclient.service.Service;
import io.hops.hopsworks.cloud.dao.heartbeat.DecommissionStatus;
import io.hops.hopsworks.common.dao.host.HostDTO;
import io.hops.hopsworks.common.dao.host.HostsFacade;
import io.hops.hopsworks.common.dao.user.UserFacade;
import io.hops.hopsworks.common.hdfs.DistributedFileSystemOps;
import io.hops.hopsworks.common.hdfs.DistributedFsService;
import io.hops.hopsworks.common.hosts.HostsController;
import io.hops.hopsworks.common.hosts.ServiceDiscoveryController;
import io.hops.hopsworks.common.proxies.CAProxy;
import io.hops.hopsworks.common.util.Settings;
import io.hops.hopsworks.common.yarn.YarnClientService;
import io.hops.hopsworks.common.yarn.YarnClientWrapper;
import io.hops.hopsworks.persistence.entity.host.Hosts;
import io.hops.hopsworks.persistence.entity.user.Users;
import io.hops.hopsworks.persistence.entity.user.security.ua.UserAccountStatus;
import java.io.IOException;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import javax.ejb.EJB;
import javax.ejb.Singleton;
import javax.ejb.Startup;
import javax.ejb.Timeout;
import javax.ejb.TimerConfig;
import javax.ejb.TimerService;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.util.Tool;
import org.apache.hadoop.util.ToolRunner;
import org.apache.hadoop.yarn.api.records.NodeReport;
import org.apache.hadoop.yarn.client.api.YarnClient;
import org.apache.hadoop.yarn.client.cli.RMAdminCLI;
import org.apache.hadoop.yarn.exceptions.YarnException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

@Singleton
@Startup
@TransactionAttribute(TransactionAttributeType.NEVER)
public class CloudManager {
  private static final Logger LOG = Logger.getLogger(CloudManager.class.getName());

  @Resource
  private TimerService timerService;
  @EJB
  private CloudClient cloudClient;
  @EJB
  private HostsController hostsController;
  @EJB
  private HostsFacade hostsFacade;
  @EJB
  private CAProxy caProxy;
  @EJB
  private YarnClientService yarnClientService;
  @EJB
  private Settings settings;
  @EJB
  private DistributedFsService dfsService;
  @EJB
  private UserFacade userFacade;
  @EJB
  private ServiceDiscoveryController serviceDiscoveryController;

  private DecommissionStatus toSend = new DecommissionStatus();
  final Set<CloudNode> decommissionedNodes = new HashSet<>();
  private final Map<Long, CommandStatus> commandsStatus = new HashMap<>();
  private boolean firstHeartbeat = true;
  private Instant beginningOfHeartbeat;
  private boolean shouldLookForMissingNodes = false;
  
  @PostConstruct
  public void init() {
    LOG.log(Level.INFO, "Hopsworks@Cloud - Initializing CloudManager");
    timerService.createIntervalTimer(0, 3000, new TimerConfig("Cloud heartbeat", false));
  }

  @Timeout
  @TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
  public void heartbeat() {
    try {
      if (firstHeartbeat) {
        beginningOfHeartbeat = Instant.now();
      }
      //send heartbeat to hopsworks-cloud
      HeartbeatRequest request = new HeartbeatRequest(new ArrayList<>(toSend.getDecommissioned()),
          new ArrayList<>(toSend.getDecommissioning()), commandsStatus, firstHeartbeat);
      
      toSend = new DecommissionStatus();
          
      HeartbeatResponse response;
      try{
        response = cloudClient.sendHeartbeat(request);
      }catch (Exception ex){
        firstHeartbeat = true;
        throw ex;
      }

      for (Map.Entry<Long, CommandStatus> commandStatus : commandsStatus.entrySet()) {
        if (CommandStatus.isFinal(commandStatus.getValue().getStatus())) {
          commandsStatus.remove(commandStatus.getKey());
        }
      }

      Map<String, CloudNode> workers = addWorkers(response);
      
      checkUsers(response.getBlockedUsers());
      
      // If it's finally removed by the list of cluster nodes, it's safe to forget them
      // from decommissionedNodes
      decommissionedNodes.removeIf(host -> !response.getWorkers().contains(host));
      
      try {
        //check if the resource manager and the namenode are up
        Service rm = serviceDiscoveryController
            .getAnyAddressOfServiceWithDNS(ServiceDiscoveryController.HopsworksService.RESOURCEMANAGER);
        Service nm = serviceDiscoveryController
            .getAnyAddressOfServiceWithDNS(ServiceDiscoveryController.HopsworksService.RPC_NAMENODE);
        
        final List<RemoveNodesCommand> requests = response.getCommands().stream()
            .filter(cc -> cc.getType().equals(CloudCommandType.REMOVE_NODES))
            .map(cc -> (RemoveNodesCommand) cc).collect(Collectors.toList());

        toSend = setAndGetDecommission(requests, workers);

        if (firstHeartbeat) {
          firstHeartbeat = false;
        }
      } catch (ServiceDiscoveryException ex) {
        //the resource manager or the namenode is not up
        LOG.log(Level.WARNING, "The NN or RM is not up yet, not handling commands");
        // we could not handle the request trigger the recovery mechanism
        firstHeartbeat = true;
      }
    } catch (Exception ex) {
      LOG.log(Level.SEVERE, "Error in Cloud Heartbeat", ex);
    }
  }
  
  private void checkUsers(List<String> blockedUsers) {
    for (String email : blockedUsers) {
      Users user = userFacade.findByEmail(email);
      if (user != null && !user.getStatus().equals(UserAccountStatus.BLOCKED_ACCOUNT)) {
        LOG.log(Level.INFO, "Blocking user " + user.getEmail());
        user.setStatus(UserAccountStatus.BLOCKED_ACCOUNT);
        userFacade.update(user);
      }
    }
  }
  
  /**
   * add worker nodes to host table if they are not present
   *
   */
  private Map<String, CloudNode> addWorkers(HeartbeatResponse response) {
    Map<String, CloudNode> workers = new HashMap<>(response.getWorkers().size());
    for (CloudNode worker : response.getWorkers()) {
      workers.put(worker.getHost(), worker);
      // Do not put back nodes that were removed by the previous heartbeat
      // but not yet shutdown
      if (!worker.getInstanceState().equals("error") && 
          !decommissionedNodes.contains(worker) && !hostsFacade.findByHostIp(worker.getIp()).isPresent()) {
        LOG.log(Level.INFO, "Adding new worker to the database " + worker.getHost());
        HostDTO hostDTO = new HostDTO();
        hostDTO.setHostname(worker.getHost());
        hostDTO.setHostIp(worker.getIp());
        hostDTO.setNumGpus(worker.getNumGPUs());
        hostsController.addOrUpdateClusterNode(worker.getHost(), hostDTO);
      }
    }
    return workers;
  }

  enum Status {
    NOPRESENT,
    UNUSABLE,
    EMPTY,
    NOMASTER,
    OTHER,
    ONGOING
  }

  Set<CloudNode> getAndSet(Status status, Map<Status, Set<CloudNode>> workerPerStatus) {
    Set<CloudNode> set = workerPerStatus.get(status);
    if (set == null) {
      set = new HashSet<>();
      workerPerStatus.put(status, set);
    }
    return set;
  }

  int addToRemove(Status status, Map<Status, Set<CloudNode>> workerPerStatus, Map<String, CloudNode> toRemove, int max,
      Comparator<CloudNode> comparator) {
    int count = 0;
    if (workerPerStatus.get(status) != null) {
      List<CloudNode> ws = new ArrayList<>(workerPerStatus.get(status));
      if (comparator != null) {
        ws.sort(comparator);
      }
      for (CloudNode worker : ws) {
        toRemove.put(worker.getHost(), worker);
        count++;
        if (count >= max) {
          break;
        }
      }
    }
    return count;
  }

  private DecommissionStatus setAndGetDecommission(List<RemoveNodesCommand> commands,
      Map<String, CloudNode> workers) throws InterruptedException {
    Configuration conf = settings.getConfiguration();
    YarnClientWrapper yarnClientWrapper = null;
    DistributedFileSystemOps dfsOps = null;
    try {
      dfsOps = dfsService.getDfsOps();
      yarnClientWrapper = yarnClientService.getYarnClientSuper(conf);
      //we pass yarnClient, dfsOps, caProxy and hostsController as argument to be able to mock them in testing
      return setAndGetDecommission(commands, workers, yarnClientWrapper.getYarnClient(),
          dfsOps, conf, caProxy, hostsController);
    } finally {
      dfsService.closeDfsClient(dfsOps);
      yarnClientService.closeYarnClient(yarnClientWrapper);
    }
  }
  
  @VisibleForTesting
  DecommissionStatus setAndGetDecommission(List<RemoveNodesCommand> commands,
      Map<String, CloudNode> workers, YarnClient yarnClient, DistributedFileSystemOps dfsOps, Configuration conf,
      CAProxy caProxy, HostsController hostsController) throws InterruptedException {

    try {
      int nbTries = 0;
      List<NodeReport> nodeReports = getNodeReports(yarnClient);

      //nodes that were already decommissioned and are still decommissioned
      //we need to keep trace of them because we need to put them back in the yarn configuration
      Map<String, CloudNode> oldDecommissioned = new HashMap<>();
      //nodes that where not decommissioned in the last heartbeat and are now decommissioned
      Map<String, CloudNode> decommissioned = new HashMap<>();
      //nodes that are decommissioning
      Map<String, CloudNode> decommissioning = new HashMap<>();
      //nodes that are active in yarn
      Map<String, NodeReport> activeNodeReports = new HashMap<>();
      //Store the workers according to there type and status in yarn
      Map<String, Map<Status, Set<CloudNode>>> workerPerType = new HashMap<>();
      //nodes that need to be removed from yarn and hdfs
      Set<String> toRemove = new HashSet<>();
      
      for (NodeReport report : nodeReports) {
        handleRepport(report, workers, toRemove, oldDecommissioned, decommissioned, decommissioning, activeNodeReports,
            workerPerType);
      }

      // These are nodes which haven't heartbeated for more than 2 minutes
      // 5 minutes after Hopsworks has started
      Set<String> missingNodes = getMissingNodes();
      toRemove.addAll(missingNodes);

      //find workers that have no report. They may not have register to yarn yet or be in an error state
      //add them to unusable as yarn can't use them right now.
      workers.values().forEach(worker -> {
        String host = worker.getHost();
        if (!activeNodeReports.containsKey(host) && !decommissioning.containsKey(host) && !decommissioned.
            containsKey(host) && !decommissionedNodes.contains(worker)) {
          Map<Status, Set<CloudNode>> workerPerStatus = workerPerType.get(worker.getInstanceType());
          if (workerPerStatus == null) {
            workerPerStatus = new HashMap<>();
            workerPerType.put(worker.getInstanceType(), workerPerStatus);
          }
          getAndSet(Status.NOPRESENT, workerPerStatus).add(worker);
        }
      });

      Map<String, CloudNode> toDecom = selectNodeToDecommission(commands,
          activeNodeReports, workerPerType);
      

      //as we overwrite the config files for yarn and hdfs we need a list of all the nodes that should still be in
      //the decommissioning list
      List<String> nodes = new ArrayList<>(toDecom.size() + decommissioned.size() + decommissioning.size()
          + oldDecommissioned.size());
      
      nodes.addAll(oldDecommissioned.keySet());
      
      nodes.addAll(decommissioned.keySet());
      
      nodes.addAll(decommissioning.keySet());
            
      toDecom.forEach((host, worker) -> {
        if (workerPerType.get(worker.getInstanceType()).get(Status.NOPRESENT) != null && workerPerType.get(worker.
            getInstanceType()).get(Status.NOPRESENT).contains(worker)) {
          //if the node selected to be decommisined is not present in yarn it is directly decommissioned without
          //decommissioning phase
          decommissioned.put(host, worker);
        } else {
          decommissioning.put(host, worker);
        }
        nodes.add(host);
      });

      if (!toRemove.isEmpty()) {
        try {
          for (String node : toRemove) {
            //for newly decommissioned nodes we should remove them from hopsworks.
            caProxy.revokeHostX509(node);
            hostsController.removeByHostname(node);
          }
          //remove nodes from yarn
          execute(new RMAdminCLI(conf), new String[]{"-removeNodes", String.join(",", toRemove)});
          //remove nodes form hdfs
          dfsOps.removeAndWipeNodes(Lists.newArrayList(toRemove), true);
        } catch (Exception ex) {
          LOG.log(Level.SEVERE, "Failed to remove node.", ex);
        }
      }
      
      yarnDecommission(nodes, conf);

      try {
        //decomission nodes on hdfs
        dfsOps.updateExcludeList(String.join(System.getProperty("line.separator"), nodes));
        dfsOps.refreshNodes();
      } catch (Exception ex) {
        LOG.log(Level.SEVERE, "Failed to decommission nodes in hdfs.", ex);
        //The most important is to decommission in Yarn, it is ok to swallow this exception.
      }

      if (commands != null) {
        for (RemoveNodesCommand cmd : commands) {
          if (!commandsStatus.containsKey(cmd.getId())) {
            commandsStatus.put(cmd.getId(), new CommandStatus(CommandStatus.CLOUD_COMMAND_STATUS.SUCCEED,
                "Successfully started the decommission of " + decommissioning.size() + " nodes"));
          }
        }
      }
      return new DecommissionStatus(decommissioning.values(), decommissioned.values());
    } catch (InterruptedException ex){
      throw ex;
    } catch (Exception ex) {
      //if we arrive here it means that none of the decommissioning request has been correctly processed
      //set all of them as failed
      LOG.log(Level.SEVERE, "Failed to decommission node.", ex);
      if (commands != null) {
        for (RemoveNodesCommand cmd : commands) {
          commandsStatus.put(cmd.getId(), new CommandStatus(CommandStatus.CLOUD_COMMAND_STATUS.FAILED,
              ex.getMessage()));
        }
      }
      return new DecommissionStatus();
    }
  }

  private Set<String> getMissingNodes() {
    if (!shouldLookForMissingNodes) {
      // If we've just restarted the cluster, give the agents some time to catch up
      if (ChronoUnit.MINUTES.between(getBeginningOfHeartbeat(), Instant.now()) >= 5) {
        shouldLookForMissingNodes = true;
      }
      return Collections.EMPTY_SET;
    }
    List<Hosts> allHosts = hostsFacade.findAll();
    Instant now = Instant.now();
    return allHosts.stream()
            .filter(h -> {
              Instant lastHeartbeat = Instant.ofEpochMilli(h.getLastHeartbeat());
              return ChronoUnit.SECONDS.between(lastHeartbeat, now) >= 90;
            })
            .map(Hosts::getHostname)
            .collect(Collectors.toSet());

  }

  Instant getBeginningOfHeartbeat() {
    return beginningOfHeartbeat;
  }
  
  private List<NodeReport> getNodeReports(YarnClient yarnClient)
      throws IOException, InterruptedException, YarnException {
    return yarnClient.getNodeReports();
  }
  
  private void yarnDecommission(List<String> nodes, Configuration conf) throws Exception {
    int nbTries = 0;
    while (true) {
      try {
        //decomission nodes on yar
        String xml = createXML(nodes);
        execute(new RMAdminCLI(conf), new String[]{"-updateExcludeList", xml});
        execute(new RMAdminCLI(conf), new String[]{
          "-refreshNodes",
          "-g",
          "-server"});
        break;
      } catch (Exception ex) {
        LOG.log(Level.SEVERE, "Failed to decommission node in Yarn.", ex);
        nbTries++;
        if (nbTries == 3) {
          throw ex;
        }
        Thread.sleep(500);
      }
    }
  }
  
  /**
   * Handle the yarn report and put the worker info in the proper map.
   * @param report
   * @param workers
   * @param toRemove
   * @param oldDecommissioned
   * @param decommissioned
   * @param decommissioning
   * @param activeNodeReports
   * @param workerPerType 
   */
  private void handleRepport(NodeReport report, Map<String, CloudNode> workers, Set<String> toRemove,
      Map<String, CloudNode> oldDecommissioned, Map<String, CloudNode> decommissioned,
      Map<String, CloudNode> decommissioning, Map<String, NodeReport> activeNodeReports,
      Map<String, Map<Status, Set<CloudNode>>> workerPerType) {

    CloudNode worker = workers.get(report.getNodeId().getHost());
    if (worker == null) {
      /*
       * Hopsworks-cloud does not know about this node
       * the node was shut down but yarn has not detected it yet.
       * we should remove it from yarn and hdfs
       */
      LOG.log(Level.INFO, "Removing worker " + report.getNodeId().getHost());
      toRemove.add(report.getNodeId().getHost());
      return;
    }
    if (decommissionedNodes.contains(worker)) {
      /*
       * The node has been decommissioned but not removed yet
       * it remain decommissioned for now
       */
      oldDecommissioned.put(worker.getHost(), worker);
      return;
    }
    
    Map<Status, Set<CloudNode>> workerPerStatus = workerPerType.get(worker.getInstanceType());
    if (workerPerStatus == null) {
      workerPerStatus = new HashMap<>();
      workerPerType.put(worker.getInstanceType(), workerPerStatus);
    }
    
    switch (report.getNodeState()) {
      case DECOMMISSIONED:
        //the node is decommissioned in yarn and was not previously.
        decommissionedNodes.add(worker);
        decommissioned.put(worker.getHost(), worker);
        if(firstHeartbeat){
          getAndSet(Status.ONGOING, workerPerStatus).add(worker);
        }
        break;
      case DECOMMISSIONING:
        decommissioning.put(worker.getHost(), worker);
        if(firstHeartbeat){
          getAndSet(Status.ONGOING, workerPerStatus).add(worker);
        }
        break;
      case LOST:
      case SHUTDOWN:
      case NEW:
      case REBOOTED:
      case RUNNING:
      case UNHEALTHY:
        activeNodeReports.put(report.getNodeId().getHost(), report);
        
        if (report.getNodeState().isUnusable()) {
          //the node is either lost, shutdown or unhealthy and no container will be allocated to it
          getAndSet(Status.UNUSABLE, workerPerStatus).add(worker);
        } else if (report.getNumContainers() == 0) {
          //the node is not running any container
          getAndSet(Status.EMPTY, workerPerStatus).add(worker);
        } else if (report.getNumApplicationMasters() == 0) {
          //the node is running containers but no application master
          getAndSet(Status.NOMASTER, workerPerStatus).add(worker);
        } else {
          //the node is running some application masters
          getAndSet(Status.OTHER, workerPerStatus).add(worker);
        }
        break;
      default:
        throw new IllegalStateException("unknow state for the node " + report.getNodeState());
    }
  }

  private Map<String, CloudNode> selectNodeToDecommission(List<RemoveNodesCommand> commands,
      Map<String, NodeReport> activeNodeReports, Map<String, Map<Status, Set<CloudNode>>> workerPerType) {
    Map<String, CloudNode> toDecom = new HashMap<>();
    for (RemoveNodesCommand cmd : commands) {
      Map<String, CloudNode> toDecomCMD = new HashMap<>();
      if (cmd != null && cmd.getNodesToRemove() != null) {
        for (Map.Entry<String, Integer> req : cmd.getNodesToRemove().entrySet()) {
          String type = req.getKey();
          int number = req.getValue();
          Map<Status, Set<CloudNode>> workerPerStatus = workerPerType.get(type);
          if (workerPerStatus == null) {
            //this should not happen. Mark this command as failed and treat the other commands
            LOG.log(Level.SEVERE,
                "Trying to decomission more node of type {0} than there is in the cluster missing {1}"
                + " nodes to remove", new Object[]{type, number});
            commandsStatus.put(cmd.getId(), new CommandStatus(CommandStatus.CLOUD_COMMAND_STATUS.FAILED,
              "Trying to decomission more nodes of type " + type + " than there is in the cluster. Missing " + number + 
                " nodes to remove"));
            continue;
          }
          
          if(firstHeartbeat){
            // check if there are already decommissioning requests
            Map<String, CloudNode> toRemoveOngoing = new HashMap<>();
            number -= addToRemove(Status.ONGOING, workerPerStatus, toRemoveOngoing, number, null);
            if(workerPerStatus.get(Status.ONGOING) != null){
              workerPerStatus.get(Status.ONGOING).removeAll(
                  toRemoveOngoing.values());
            }
            if(number <=0){
              continue;
            }
          }
          
          //first try to select node that are not present in yarn
          number -= addToRemove(Status.NOPRESENT, workerPerStatus, toDecomCMD, number, null);
          if (number <= 0) {
            toDecom.putAll(toDecomCMD);
            continue;
          }
          //first try to select node that are unusable
          number -= addToRemove(Status.UNUSABLE, workerPerStatus, toDecomCMD, number, null);
          if (number <= 0) {
            toDecom.putAll(toDecomCMD);
            continue;
          }
          //then select nodes that are empty to avoid interefering with running applications
          number -= addToRemove(Status.EMPTY, workerPerStatus, toDecomCMD, number, null);
          if (number <= 0) {
            toDecom.putAll(toDecomCMD);
            continue;
          }
          //then select nodes running no application master and the least number of containers to minimize 
          //interfering with running application
          number -= addToRemove(Status.NOMASTER, workerPerStatus, toDecomCMD, number, 
            (CloudNode cn1, CloudNode cn2) -> {
              Integer cn1NumContainers
                = activeNodeReports.get(cn1.getHost()).getNumContainers();
              Integer cn2NumContainers
                = activeNodeReports.get(cn2.getHost()).getNumContainers();
              return cn1NumContainers.compareTo(cn2NumContainers);
            });
          if (number <= 0) {
            toDecom.putAll(toDecomCMD);
            continue;
          }
          //finally select nodes running the least number of application masters to minimize interferences
          number -= addToRemove(Status.OTHER, workerPerStatus, toDecomCMD, number, (CloudNode cn1, CloudNode cn2) -> {
            Integer cn1NumAppMaster = activeNodeReports.get(cn1.getHost()).
                getNumApplicationMasters();
            Integer cn2NumAppMaster = activeNodeReports.get(cn2.getHost()).
                getNumApplicationMasters();
            if (cn1NumAppMaster.equals(cn2NumAppMaster)) {
              Integer cn1NumContainers
                  = activeNodeReports.get(cn1.getHost()).getNumContainers();
              Integer cn2NumContainers
                  = activeNodeReports.get(cn2.getHost()).getNumContainers();
              return cn1NumContainers.compareTo(cn2NumContainers);
            }
            return cn1NumAppMaster.compareTo(cn2NumAppMaster);
          });
          if (number <= 0) {
            toDecom.putAll(toDecomCMD);
            continue;
          }
          //this should not happen. Mark this command as failed and treat the other commands
          LOG.log(Level.SEVERE,
              "Trying to decomission more node of type {0} than there is in the cluster missing {1} nodes to remove",
              new Object[]{type, number});
          commandsStatus.put(cmd.getId(), new CommandStatus(CommandStatus.CLOUD_COMMAND_STATUS.FAILED,
              "Trying to decomission more nodes of type " + type + " than there is in the cluster. Missing " + number + 
                " nodes to remove"));
        }
      }
    }
    return toDecom;
  }
  
  
  void execute(Tool tool, String[] command) throws Exception{
    ToolRunner.run(tool, command);
  }
  
  String createXML(List<String> toRemove) throws TransformerConfigurationException, TransformerException,
      ParserConfigurationException {
    DocumentBuilderFactory documentFactory = DocumentBuilderFactory.newInstance();

    DocumentBuilder documentBuilder = documentFactory.newDocumentBuilder();

    Document document = documentBuilder.newDocument();

    // root element
    Element root = document.createElement("hosts");
    document.appendChild(root);

    toRemove.forEach(hostName -> {
      Element host = document.createElement("host");

      root.appendChild(host);

      Element name = document.createElement("name");
      name.appendChild(document.createTextNode(hostName));
      host.appendChild(name);

      Element timeOut = document.createElement("timeout");
      timeOut.appendChild(document.createTextNode("36000"));
      host.appendChild(timeOut);
    });

    DOMSource domSource = new DOMSource(document);
    StringWriter writer = new StringWriter();
    StreamResult result = new StreamResult(writer);
    TransformerFactory tf = TransformerFactory.newInstance();
    Transformer transformer = tf.newTransformer();
    transformer.transform(domSource, result);
    return writer.toString();
  }

}
