/*
 * This file is part of Hopsworks
 * Copyright (C) 2022, Hopsworks AB. All rights reserved
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
package io.hops.hopsworks.common.executors;

import javax.annotation.PostConstruct;
import javax.ejb.Singleton;
import javax.ejb.Startup;
import javax.enterprise.concurrent.ManagedExecutorService;
import javax.naming.InitialContext;
import java.util.logging.Level;
import java.util.logging.Logger;

@Singleton
@Startup
public class ExecutorPoolManager {
  private static final Logger LOGGER = Logger.getLogger(ExecutorPoolManager.class.getName());
  
  @PostConstruct
  private void init() {
    ManagedExecutorService jupyterExecutorService;
    try {
      jupyterExecutorService = InitialContext.doLookup("concurrent/jupyterExecutorService");
      jupyterExecutorService.submit(() -> LOGGER.log(Level.INFO, "Initialized jupyter executor service"));
    } catch (Exception e) {
      LOGGER.log(Level.SEVERE, "Error initializing jupyter executor service", e);
    }
  
    ManagedExecutorService hopsExecutorService;
    try {
      hopsExecutorService = InitialContext.doLookup("concurrent/hopsExecutorService");
      hopsExecutorService.submit(() -> LOGGER.log(Level.INFO, "Initialized hops executor service"));
    } catch (Exception e) {
      LOGGER.log(Level.SEVERE, "Error initializing hops executor service", e);
    }
  }
}
