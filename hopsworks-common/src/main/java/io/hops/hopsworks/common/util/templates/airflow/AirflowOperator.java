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

package io.hops.hopsworks.common.util.templates.airflow;

import java.util.Arrays;

public abstract class AirflowOperator {
  private final String projectName;
  private final String id;
  private String upstream;
  private static final String[] PYTHON_KEY_WORDS = {"False", "None", "True", "and", "as", "assert",
    "async", "await", "break", "class", "continue", "def", "del", "elif", "else", "except", "finally", "for",
    "from", "global", "if", "import", "in", "is", "lambda", "nonlocal", "not", "or", "pass", "raise", "return",
    "try", "while", "with", "yield"};
  
  public AirflowOperator(String projectName, String id) {
    this.projectName = projectName;
    this.id = this.sanitizeId(id);
  }
  
  public String getProjectName() {
    return projectName;
  }
  
  public String getId() {
    return id;
  }
  
  public String getUpstream() {
    return upstream;
  }
  
  public void setUpstream(String upstream) {
    this.upstream = upstream;
  }
  
  public static String sanitizeId(String id) {
    String sanitizedId = id.replaceAll("[^a-zA-Z0-9_]", "_");
    if(Arrays.stream(PYTHON_KEY_WORDS).anyMatch(s -> s.equals(id))){
      sanitizedId = "_".concat(sanitizedId);
    }
    if(Character.isDigit(sanitizedId.charAt(0))) {
      sanitizedId = "_".concat(sanitizedId);
    }
    return sanitizedId;
  }
}
