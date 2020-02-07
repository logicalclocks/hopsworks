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

import io.hops.hopsworks.common.dao.util.VariablesFacade;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.ejb.EJB;
import javax.ejb.Singleton;
import java.io.File;
import java.io.IOException;
import java.util.logging.FileHandler;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;

@Singleton
public class LoggerFactory {
  private static final String AUDIT_LOG_FILE_VAR = "audit_log_dir";
  private static final String AUDIT_LOG_FILE_FORMAT = "audit_log_file_format";
  private static final String AUDIT_LOG_FILE_SIZE = "audit_log_size_limit";
  private static final String AUDIT_LOG_FILE_COUNT = "audit_log_count";
  private static final String AUDIT_LOG_FILE_TYPE = "audit_log_file_type";
  
  @EJB
  private VariablesFacade variablesFacade;
  
  private FileHandler fileHandler;
  private Logger logger;
  
  @PostConstruct
  private void init() {
    this.logger = Logger.getLogger(LoggerFactory.class.getName());
    this.logger.setUseParentHandlers(false);
    String logDirPath = getStrValue(AUDIT_LOG_FILE_VAR, "/srv/hops/domains/domain1/logs/audit");
    logDirPath = logDirPath.endsWith("/")? logDirPath : logDirPath + "/";
    String logFileFormat = getStrValue(AUDIT_LOG_FILE_FORMAT, "server_audit_log%g.log");
    logFileFormat = logFileFormat.startsWith("/")? logFileFormat.substring(1) : logFileFormat;
    int logFileSize = getIntValue(AUDIT_LOG_FILE_SIZE, 256000000);//256MB
    int logFileCount = getIntValue(AUDIT_LOG_FILE_COUNT, 10);//10 files
    String logFileType = getStrValue(AUDIT_LOG_FILE_TYPE, "Text");
    File logDir = new File(logDirPath);
    if(!logDir.exists()) {
      logDir.mkdirs();
    }
    try {
      fileHandler = new FileHandler( logDirPath + logFileFormat, logFileSize, logFileCount, true);
      if ("Html".equalsIgnoreCase(logFileType)) {
        HtmlLogFormatter formatter = new HtmlLogFormatter();
        fileHandler.setFormatter(formatter);
      } else {
        SimpleFormatter formatter = new SimpleFormatter();
        fileHandler.setFormatter(formatter);
      }
      fileHandler.setLevel(Level.FINEST); //Log levels INFO and higher will be automatically written to glassfish log.
      this.logger.addHandler(fileHandler);
    } catch (SecurityException | IOException e) {
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
  
  private int getIntValue(String id, int defaultVal) {
    String val = variablesFacade.getVariableValue(id);
    if (val == null) {
      return defaultVal;
    }
    try {
      return Integer.parseInt(val);
    } catch (NumberFormatException e) {
    
    }
    return defaultVal;
  }
  
  private String getStrValue(String id, String defaultVal) {
    String val = variablesFacade.getVariableValue(id);
    if (val == null) {
      return defaultVal;
    }
    return val;
  }
  
}
