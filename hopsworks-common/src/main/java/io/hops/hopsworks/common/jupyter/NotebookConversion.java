/*
 * This file is part of Hopsworks
 * Copyright (C) 2024, Hopsworks AB. All rights reserved
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
package io.hops.hopsworks.common.jupyter;

import java.util.Arrays;
import java.util.Optional;

public enum NotebookConversion {
  PY("PySpark"),
  HTML("Html"),
  PY_JOB("Python");

  private String kernel;
  NotebookConversion(String kernel) {
    this.kernel = kernel;
  }

  public static Optional<NotebookConversion> fromKernel(String kernel) {
    return Arrays.stream(NotebookConversion.values()).filter(k ->
      k.getKernel().equalsIgnoreCase(kernel)).findFirst();
  }

  public String getKernel() {
    return kernel;
  }
}
