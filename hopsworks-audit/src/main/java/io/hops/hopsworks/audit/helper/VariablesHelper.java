/*
 * Copyright (C) 2022, Logical Clocks AB. All rights reserved
 */
package io.hops.hopsworks.audit.helper;

import io.hops.hopsworks.common.dao.util.VariablesFacade;

import javax.annotation.PostConstruct;
import javax.ejb.EJB;
import javax.ejb.Lock;
import javax.ejb.Singleton;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;

import static javax.ejb.LockType.READ;

@Singleton
public class VariablesHelper {
  private final static Logger LOGGER = Logger.getLogger(VariablesHelper.class.getName());
  private static final String AUDIT_LOG_FILE_VAR = "audit_log_dir";
  private static final String AUDIT_LOG_FILE_FORMAT = "audit_log_file_format";
  private static final String AUDIT_LOG_FILE_SIZE = "audit_log_size_limit";
  private static final String AUDIT_LOG_FILE_COUNT = "audit_log_count";
  private static final String AUDIT_LOG_FILE_TYPE = "audit_log_file_type";
  private static final String AUDIT_LOG_DATE_FORMAT = "audit_log_date_format";
  private static final String DEFAULT_DATE_FORMAT = "yyyy-MM-dd HH:mm:ss";
  
  private String auditLogDirPath;
  private String auditLogFileFormat;
  private int auditLogFileSize;
  private int auditLogFileCount;
  private String auditLogFileType;
  private String auditLogDateFormat;
  
  @EJB
  private VariablesFacade variablesFacade;
  
  @PostConstruct
  private void init() {
    auditLogDirPath = getAuditLogDirPathInternal();
    auditLogFileFormat = getAuditLogFileFormatInternal();
    auditLogFileSize = getAuditLogFileSizeInternal();
    auditLogFileCount = getAuditLogFileCountInternal();
    auditLogFileType = getAuditLogFileTypeInternal();
    auditLogDateFormat = getAuditLogDateFormatInternal();
  }
  
  @Lock(READ)
  public String getAuditLogDirPath() {
    return auditLogDirPath;
  }
  
  @Lock(READ)
  public String getAuditLogFileFormat() {
    return auditLogFileFormat;
  }
  
  @Lock(READ)
  public int getAuditLogFileSize() {
    return auditLogFileSize;
  }
  
  @Lock(READ)
  public int getAuditLogFileCount() {
    return auditLogFileCount;
  }
  
  @Lock(READ)
  public String getAuditLogFileType() {
    return auditLogFileType;
  }
  
  @Lock(READ)
  public String getAuditLogDateFormat() {
    return auditLogDateFormat;
  }
  
  private String getAuditLogDirPathInternal() {
    String logDirPath = getStrValue(AUDIT_LOG_FILE_VAR, "/srv/hops/domains/domain1/logs/audit");
    return logDirPath.endsWith("/")? logDirPath : logDirPath + "/";
  }
  
  private String getAuditLogFileFormatInternal() {
    String logFileFormat = getStrValue(AUDIT_LOG_FILE_FORMAT, "server_audit_log%g.log");
    return logFileFormat.startsWith("/")? logFileFormat.substring(1) : logFileFormat;
  }
  
  private int getAuditLogFileSizeInternal() {
    return getIntValue(AUDIT_LOG_FILE_SIZE, 256000000);//256MB
  }
  
  private int getAuditLogFileCountInternal() {
    return getIntValue(AUDIT_LOG_FILE_COUNT, 10);//10 files
  }
  
  private String getAuditLogFileTypeInternal() {
    return getStrValue(AUDIT_LOG_FILE_TYPE, SimpleFormatter.class.getName());
  }
  
  private String getAuditLogDateFormatInternal() {
    return getStrValue(AUDIT_LOG_DATE_FORMAT, DEFAULT_DATE_FORMAT);
  }
  
  public int getIntValue(String id, int defaultVal) {
    String val = variablesFacade.getVariableValue(id);
    if (val == null) {
      return defaultVal;
    }
    try {
      return Integer.parseInt(val);
    } catch (NumberFormatException e) {
      LOGGER.log(Level.WARNING, "Variable {0} is not an integer. Using default {1}", new Object[]{id, defaultVal});
    }
    return defaultVal;
  }
  
  public String getStrValue(String id, String defaultVal) {
    String val = variablesFacade.getVariableValue(id);
    if (val == null) {
      return defaultVal;
    }
    return val;
  }
}
