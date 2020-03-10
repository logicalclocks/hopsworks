/*
 * This file is part of Hopsworks
 * Copyright (C) 2020, Logical Clocks AB. All rights reserved
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
package io.hops.hopsworks.persistence.entity.util;

public class Settings {
  public static final int YARN_DEFAULT_APP_MASTER_MEMORY = 2048;
  
  public static final int SPARK_MIN_EXECS = 1;
  public static final int SPARK_MAX_EXECS = 2;
  
  public static final String SPARK_PY_MAINCLASS = "org.apache.spark.deploy.PythonRunner";
  
  public static final String SHARED_FILE_SEPARATOR = "::";
}
