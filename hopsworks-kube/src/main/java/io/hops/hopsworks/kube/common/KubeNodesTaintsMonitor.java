/*
 * Copyright (C) 2022, Logical Clocks AB. All rights reserved
 */
package io.hops.hopsworks.kube.common;

import io.fabric8.kubernetes.api.model.Node;
import io.fabric8.kubernetes.api.model.NodeAddress;
import io.fabric8.kubernetes.api.model.Taint;

import com.google.common.base.Strings;
import io.hops.hopsworks.persistence.entity.util.Variables;
import io.hops.hopsworks.common.dao.util.VariablesFacade;
import io.hops.hopsworks.persistence.entity.util.VariablesVisibility;
import io.hops.hopsworks.common.util.Settings;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import javax.ejb.ConcurrencyManagement;
import javax.ejb.ConcurrencyManagementType;
import javax.ejb.EJB;
import javax.ejb.Singleton;
import javax.ejb.Timeout;
import javax.ejb.TimerConfig;
import javax.ejb.Timer;
import javax.ejb.TimerService;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import java.util.concurrent.TimeUnit;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.StringJoiner;
import java.util.Optional;

@Singleton
@TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
@ConcurrencyManagement(ConcurrencyManagementType.BEAN)
public class KubeNodesTaintsMonitor {
  private final static Logger LOGGER = Logger.getLogger(KubeNodesTaintsMonitor.class.getName());
  @EJB
  private Settings settings;
  @EJB
  private KubeClientService kubeClientService;
  @EJB
  private VariablesFacade variablesFacade;
  @Resource
  private TimerService timerService;

  private static final String VARIABLE_KUBE_TAINTED_NODES = "kube_tainted_nodes";
  private static final String VARIABLE_KUBE_MASTER_NODE_UNTAINTED = "kube_master_node_untainted";

  @PostConstruct
  public void init() {
    String rawInterval = settings.getKubeTaintedMonitorInterval();
    Long intervalValue = settings.getConfTimeValue(rawInterval);
    TimeUnit intervalTimeunit = settings.getConfTimeTimeUnit(rawInterval);
    intervalValue = intervalTimeunit.toMillis(intervalValue);
    timerService.createIntervalTimer(intervalValue, intervalValue,
        new TimerConfig("Kube Node Timeout Monitor", false));
  }

  @Timeout
  public void rotate(Timer timer) {
    LOGGER.log(Level.INFO, "Running KubeNodesTaintsMonitor");
    List<Node> nodes = kubeClientService.getNodeList();
    StringJoiner joiner = new StringJoiner(",");
    for (Node node: nodes) {
      Optional<Taint> taintOptional = node.getSpec().getTaints().stream().filter(taint -> taint.getEffect().equals(
          "NoSchedule")).findFirst();
      if (taintOptional.isPresent()) {
        Optional<NodeAddress> nodeAddressOptional =
            node.getStatus().getAddresses().stream().filter(address -> address.getType().equals(
            "InternalIP")).findFirst();
        if (nodeAddressOptional.isPresent()) {
          joiner.add(nodeAddressOptional.get().getAddress());
        }
      }
    }
    String taintedNodes = joiner.toString();
    if (!Strings.isNullOrEmpty(taintedNodes)) {
      Variables taintesNodesVariable =
          new Variables(VARIABLE_KUBE_TAINTED_NODES, taintedNodes, VariablesVisibility.ADMIN, false);
      variablesFacade.update(taintesNodesVariable);
    }
  }
}