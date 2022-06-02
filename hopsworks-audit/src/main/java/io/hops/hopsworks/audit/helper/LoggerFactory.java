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
package io.hops.hopsworks.audit.helper;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.ejb.EJB;
import javax.ejb.Singleton;
import java.io.File;
import java.io.IOException;
import java.util.logging.FileHandler;
import java.util.logging.Formatter;
import java.util.logging.Level;
import java.util.logging.Logger;

@Singleton
public class LoggerFactory {

  @EJB
  private VariablesHelper variablesHelper;
  
  private FileHandler fileHandler;
  private Logger logger;
  
  @PostConstruct
  private void init() {
    this.logger = Logger.getLogger(LoggerFactory.class.getName());
    this.logger.setUseParentHandlers(false);

    String logDirPath = variablesHelper.getAuditLogDirPath();
    File logDir = new File(logDirPath);
    if(!logDir.exists()) {
      logDir.mkdirs();
    }

    String logFileFormat = variablesHelper.getAuditLogFileFormat();

    int logFileSize = variablesHelper.getAuditLogFileSize();
    int logFileCount = variablesHelper.getAuditLogFileCount();
    String logFileType = variablesHelper.getAuditLogFileType();

    try {
      fileHandler = new FileHandler( logDirPath + logFileFormat, logFileSize, logFileCount, true);
      fileHandler.setFormatter((Formatter) Class.forName(logFileType).newInstance());
      fileHandler.setLevel(Level.FINEST); //Log levels INFO and higher will be automatically written to glassfish log.
      this.logger.addHandler(fileHandler);
    } catch (
        SecurityException | IOException | ClassNotFoundException | InstantiationException | IllegalAccessException e
    ) {
      logger.log(Level.SEVERE, "Error creating file handler for audit logger. {0}", e.getMessage());
    }
  }
  
  @PreDestroy
  public void destroy() {
    if (this.fileHandler != null) {
      this.fileHandler.flush();
      this.fileHandler.close();
    }
  }

  public Logger getLogger() {
    return this.logger;
  }
}
