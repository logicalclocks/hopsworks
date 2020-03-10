/*
 * This file is part of Hopsworks
 * Copyright (C) 2019, Logical Clocks AB. All rights reserved
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

package io.hops.hopsworks.common.agent;

import io.hops.hopsworks.persistence.entity.host.Hosts;
import io.hops.hopsworks.common.dao.host.HostsFacade;
import io.hops.hopsworks.common.util.RemoteCommand;
import io.hops.hopsworks.common.util.RemoteCommandExecutor;
import io.hops.hopsworks.common.util.RemoteCommandResult;
import io.hops.hopsworks.common.util.Settings;
import io.hops.hopsworks.exceptions.ServiceException;
import io.hops.hopsworks.restutils.RESTCodes;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import javax.ejb.DependsOn;
import javax.ejb.EJB;
import javax.ejb.Singleton;
import javax.ejb.Timeout;
import javax.ejb.TimerConfig;
import javax.ejb.TimerService;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.enterprise.concurrent.ManagedExecutorService;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.time.LocalTime;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

@Singleton
@TransactionAttribute(TransactionAttributeType.NEVER)
@DependsOn("Settings")
public class AgentLivenessMonitor {
  private static final Logger LOGGER = Logger.getLogger(AgentLivenessMonitor.class.getName());
  private final Map<String, LocalTime> agentsHeartbeat = new ConcurrentHashMap<>();
  private final ArrayBlockingQueue<String> agentsToRestart = new ArrayBlockingQueue<>(200);
  private final static String KAGENT_COMMAND_TEMPLATE = "sudo systemctl %s kagent";
  
  @EJB
  private Settings settings;
  @EJB
  private RemoteCommandExecutor remoteCommandExecutor;
  @EJB
  private HostsFacade hostsFacade;
  @Resource
  private TimerService timerService;
  @Resource(lookup = "concurrent/hopsExecutorService")
  private ManagedExecutorService executorService;
  
  private long thresholdInSeconds;
  private String kagentUser;
  private Path identityFile;
  private String restartCommand;
  
  private String startCommand;
  private String stopCommand;
  
  @PostConstruct
  public void init() {
    kagentUser = settings.getKagentUser();
    identityFile = Paths.get(System.getProperty("user.home"), ".ssh", "id_rsa");
    
    restartCommand = String.format(KAGENT_COMMAND_TEMPLATE, "restart");
    stopCommand = String.format(KAGENT_COMMAND_TEMPLATE, "stop");
    startCommand = String.format(KAGENT_COMMAND_TEMPLATE, "start");
    
    if (settings.isKagentLivenessMonitorEnabled()) {
      Long time = settings.getConfTimeValue(settings.getKagentLivenessThreshold());
      TimeUnit unit = settings.getConfTimeTimeUnit(settings.getKagentLivenessThreshold());
  
      thresholdInSeconds = Math.max(TimeUnit.SECONDS.convert(time, unit), 1L);
      long livenessInterval = Math.max(thresholdInSeconds * 1000 / 2, 1000L);
      timerService.createIntervalTimer(10000L, livenessInterval, new TimerConfig("kagent liveness monitor", false));
      executorService.submit(new AgentRestartConsumer());
    }
  }
  
  public void alive(Hosts host) {
    if (!settings.isKagentLivenessMonitorEnabled()) {
      return;
    }
    LOGGER.log(Level.FINEST, "Agent@" + host + " is alive");
    agentsHeartbeat.put(host.getHostname(), getNow());
  }
  
  @Timeout
  @TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
  public void isAlive() {
    try {
      LocalTime now = getNow();
      for (Map.Entry<String, LocalTime> entry : agentsHeartbeat.entrySet()) {
        String host = entry.getKey();
        LocalTime lastHeartbeat = entry.getValue();
        Duration duration = Duration.between(lastHeartbeat, now);
        if (!duration.minusSeconds(thresholdInSeconds).isNegative()) {
          LOGGER.log(Level.WARNING, "kagent in " + host + " is not alive, restarting it");
          agentsToRestart.offer(host, 5L, TimeUnit.SECONDS);
        }
      }
    } catch (Exception ex) {
      LOGGER.log(Level.SEVERE, "Error checking liveness of kagent", ex);
    }
  }
  
  public RemoteCommandResult start(Hosts host) throws ServiceException {
    if (host.getRegistered()) {
      LOGGER.log(Level.FINE, "Starting kagent@" + host);
      RemoteCommand command = constructRemoteCommand(host, startCommand);
      return remoteCommandExecutor.execute(command);
    }
    throwHostNotRegisteredException(host);
    // Never reaching this point
    return null;
  }
  
  public Future<RemoteCommandResult> startAsync(Hosts host) throws ServiceException {
    if (host.getRegistered()) {
      LOGGER.log(Level.FINE, "Submitting start kagent@" + host);
      RemoteCommand command = constructRemoteCommand(host, startCommand);
      return remoteCommandExecutor.submit(command);
    }
    throwHostNotRegisteredException(host);
    // Never reaching this point
    return null;
  }
  
  public RemoteCommandResult stop(Hosts host) throws ServiceException {
    if (host.getRegistered()) {
      LOGGER.log(Level.FINE, "Stopping kagent@" + host);
      agentsHeartbeat.remove(host.getHostname());
      agentsToRestart.remove(host.getHostname());
      RemoteCommand command = constructRemoteCommand(host, stopCommand);
      return remoteCommandExecutor.execute(command);
    }
    throwHostNotRegisteredException(host);
    // Never reaching this point
    return null;
  }
  
  public Future<RemoteCommandResult> stopAsync(Hosts host) throws ServiceException {
    if (host.getRegistered()) {
      LOGGER.log(Level.FINE, "Submitting stop kagent@" + host);
      agentsHeartbeat.remove(host.getHostname());
      agentsToRestart.remove(host.getHostname());
      RemoteCommand command = constructRemoteCommand(host, stopCommand);
      return remoteCommandExecutor.submit(command);
    }
    throwHostNotRegisteredException(host);
    // Never reaching this point
    return null;
  }
  
  public RemoteCommandResult restart(Hosts host) throws ServiceException {
    if (host.getRegistered()) {
      LOGGER.log(Level.FINE, "Restarting kagent@" + host);
      return restartAgentInternal(host.getHostname());
    }
    throwHostNotRegisteredException(host);
    // Never reaching this point
    return null;
  }
  
  public Future<RemoteCommandResult> restartAsync(Hosts host) throws ServiceException {
    if (host.getRegistered()) {
      LOGGER.log(Level.FINE, "Submitting restart kagent@" + host);
      RemoteCommand command = constructRemoteCommand(host, restartCommand);
      return remoteCommandExecutor.submit(command);
    }
    throwHostNotRegisteredException(host);
    // Never reaching this point
    return null;
  }
  
  private void throwHostNotRegisteredException(Hosts host) throws ServiceException {
    throw new ServiceException(RESTCodes.ServiceErrorCode.HOST_NOT_REGISTERED, Level.FINE, "Host is not registered",
        "Host " + host + " is not registered with Hopsworks");
  }
  
  private RemoteCommand constructRemoteCommand(Hosts host, String command) {
    return new RemoteCommand.Builder()
        .setHost(host.getHostname())
        .setUser(kagentUser)
        .setIdentity(identityFile)
        .setCommand(command)
        .setConnectTimeoutMS(10000)
        .setExecutionTimeoutS(20)
        .build();
  }
  
  private LocalTime getNow() {
    return LocalTime.now();
  }
  
  private RemoteCommandResult restartAgentInternal(String host) throws ServiceException {
    RemoteCommand command = new RemoteCommand.Builder()
        .setHost(host)
        .setUser(kagentUser)
        .setIdentity(identityFile)
        .setCommand(restartCommand)
        .setConnectTimeoutMS(10000)
        .setExecutionTimeoutS(20)
        .build();
    RemoteCommandResult result = remoteCommandExecutor.execute(command);
    if (result.getExitCode() != 0) {
      LOGGER.log(Level.WARNING, "Failed to restart kagent reason: " + result.getStdout()
          + " Exit code: " + result.getExitCode());
    }
    return result;
  }
  
  private class AgentRestartConsumer implements Runnable {
  
    @Override
    public void run() {
      while (!Thread.currentThread().isInterrupted()) {
        try {
          String agentToRestart = agentsToRestart.take();
          Optional<Hosts> optionalHost = hostsFacade.findByHostname(agentToRestart);
          if (!optionalHost.isPresent() || !optionalHost.get().getRegistered()) {
            // Agent might have been deleted
            agentsHeartbeat.remove(agentToRestart);
          } else if (agentsHeartbeat.containsKey(agentToRestart)) {
            // Agent might have been stopped and removed from the heartbeat map
            restartAgentInternal(agentToRestart);
          }
        } catch (ServiceException ex) {
          LOGGER.log(Level.WARNING, "Failed to restart kagent", ex);
        } catch (InterruptedException ex) {
          Thread.currentThread().interrupt();
        } catch (Exception ex) {
          // Do nothing
        }
      }
    }
  }
}
