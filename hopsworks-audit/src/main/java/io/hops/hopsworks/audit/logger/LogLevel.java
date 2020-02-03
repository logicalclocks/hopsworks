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
package io.hops.hopsworks.audit.logger;

import java.util.logging.Level;

public enum LogLevel {
  INFO("INFO", Level.INFO),
  WARNING("WARNING", Level.WARNING),
  SEVERE("SEVERE", Level.SEVERE),
  CONFIG("CONFIG", Level.CONFIG),
  FINE("FINE", Level.FINE),
  FINER("FINER", Level.FINER),
  FINEST("FINEST", Level.FINEST),
  OFF("OFF", Level.OFF),
  ALL("ALL", Level.ALL);
  
  private final String name;
  private final Level level;
  
  LogLevel(String name, Level level) {
    this.name = name;
    this.level = level;
  }
  
  public Level getLevel() {
    return this.level;
  }
  
  public static LogLevel fromString(String name) {
    return valueOf(name.toUpperCase());
  }
}
