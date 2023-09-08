/*
 * This file is part of Hopsworks
 * Copyright (C) 2021, Logical Clocks AB. All rights reserved
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
package io.hops.hopsworks.persistence.entity.git.config;

import javax.xml.bind.annotation.XmlEnum;
import javax.xml.bind.annotation.XmlEnumValue;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.Set;

@XmlEnum
public enum GitOpExecutionState {
  @XmlEnumValue("Initializing")
  INITIALIZING("Initializing"),
  @XmlEnumValue("Initialization failed")
  INITIALIZATION_FAILED("Initialization failed"),
  @XmlEnumValue("Success")
  SUCCESS("Success"),
  @XmlEnumValue("Running")
  RUNNING("Running"),
  @XmlEnumValue("Failed")
  FAILED("Failed"),
  @XmlEnumValue("Killed")
  KILLED("Killed"),
  @XmlEnumValue("Submitted")
  SUBMITTED("Submitted"),
  @XmlEnumValue("Timedout")
  TIMEDOUT("Timedout"),
  @XmlEnumValue("Cancelled")
  CANCELLED("Cancelled");

  private final String executionState;

  GitOpExecutionState(String executionState) {
    this.executionState = executionState;
  }

  @Override
  public String toString() {
    return executionState;
  }

  public String getExecutionState() { return executionState; }

  public static GitOpExecutionState fromValue(String v) throws IllegalArgumentException {
    return Arrays.stream(GitOpExecutionState.values())
        .filter(s -> s.getExecutionState().equalsIgnoreCase(v.toUpperCase())).findFirst()
        .orElseThrow(() -> new IllegalArgumentException("Invalid git operation execution state provided"));
  }

  public boolean isFinalState() {
    switch (this) {
      case SUCCESS:
      case FAILED:
      case KILLED:
      case INITIALIZATION_FAILED:
      case TIMEDOUT:
      case CANCELLED:
        return true;
      default:
        return false;
    }
  }

  public static Set<GitOpExecutionState> getFinalStates() {
    return EnumSet.
        of(SUCCESS, FAILED, KILLED, INITIALIZATION_FAILED, TIMEDOUT, CANCELLED);
  }
}
