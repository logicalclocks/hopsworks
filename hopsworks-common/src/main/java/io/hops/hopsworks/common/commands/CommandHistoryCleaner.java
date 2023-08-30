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
package io.hops.hopsworks.common.commands;

import io.hops.hopsworks.common.dao.commands.search.SearchFSCommandHistoryFacade;
import io.hops.hopsworks.common.util.PayaraClusterManager;
import io.hops.hopsworks.common.util.Settings;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import javax.ejb.DependsOn;
import javax.ejb.EJB;
import javax.ejb.Singleton;
import javax.ejb.Startup;
import javax.ejb.Timeout;
import javax.ejb.TimerConfig;
import javax.ejb.TimerService;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import java.util.logging.Level;
import java.util.logging.Logger;

@Startup
@Singleton
@DependsOn("Settings")
@TransactionAttribute(TransactionAttributeType.NEVER)
public class CommandHistoryCleaner {
  private static final Logger LOGGER = Logger.getLogger(CommandHistoryCleaner.class.getName());
  
  @Resource
  private TimerService timerService;
  @EJB
  private Settings settings;
  @EJB
  private PayaraClusterManager payaraClusterManager;
  @EJB
  private SearchFSCommandHistoryFacade commandHistoryFacade;
  
  @PostConstruct
  public void init() {
    schedule();
  }
  
  private void schedule() {
    LOGGER.log(Level.INFO, "schedule");
    timerService.createSingleActionTimer(settings.commandSearchFSHistoryCleanPeriod(),
      new TimerConfig("command history cleaner", false));
  }
  
  @Timeout
  @TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
  public void clean() {
    try {
      cleanInt();
    } catch (Exception t) {
      LOGGER.log(Level.INFO, "Command history cleaning failed with error:", t);
    }
    schedule();
  }
  
  private void cleanInt() throws CommandException {
    if (!payaraClusterManager.amIThePrimary()) {
      LOGGER.log(Level.INFO, "not primary");
      return;
    }
    if(settings.commandSearchFSHistoryEnabled()) {
      commandHistoryFacade.deleteOlderThan(settings.commandSearchFSHistoryWindow());
    }
  }
}
