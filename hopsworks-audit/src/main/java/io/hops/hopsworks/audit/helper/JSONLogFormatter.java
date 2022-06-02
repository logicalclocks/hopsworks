/*
 * Copyright (C) 2022, Logical Clocks AB. All rights reserved
 */
package io.hops.hopsworks.audit.helper;

import java.util.logging.Formatter;
import java.util.logging.LogRecord;

public class JSONLogFormatter extends Formatter {
  @Override
  public String format(LogRecord logRecord) {
    //Already in json format, so just get message
    return logRecord.getMessage() + "\n";
  }
}
