/*
 * This file is part of HopsWorks
 *
 * Copyright (C) 2013 - 2018, Logical Clocks AB and RISE SICS AB. All rights reserved.
 *
 * HopsWorks is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * HopsWorks is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with HopsWorks.  If not, see <http://www.gnu.org/licenses/>.
 */

package io.hops.hopsworks.common.jobs.jobhistory;

import java.util.EnumSet;
import org.apache.hadoop.yarn.api.records.YarnApplicationState;

public enum JobState {

  INITIALIZING("Initializing"),
  INITIALIZATION_FAILED("Initialization failed"),
  FINISHED("Finished"),
  RUNNING("Running"),
  ACCEPTED("Accepted"),
  FAILED("Failed"),
  KILLED("Killed"),
  NEW("New"),
  NEW_SAVING("New, saving"),
  SUBMITTED("Submitted"),
  AGGREGATING_LOGS("Aggregating logs"),
  FRAMEWORK_FAILURE("Framework failure"),
  STARTING_APP_MASTER("Starting Application Master"),
  APP_MASTER_START_FAILED("Failed starting AM");

  private final String readable;

  private JobState(String readable) {
    this.readable = readable;
  }

  @Override
  public String toString() {
    return readable;
  }

  public static JobState getJobState(YarnApplicationState yarnstate) {
    switch (yarnstate) {
      case RUNNING:
        return JobState.RUNNING;
      case ACCEPTED:
        return JobState.ACCEPTED;
      case FAILED:
        return JobState.FAILED;
      case FINISHED:
        return JobState.FINISHED;
      case KILLED:
        return JobState.KILLED;
      case NEW:
        return JobState.NEW;
      case NEW_SAVING:
        return JobState.NEW_SAVING;
      case SUBMITTED:
        return JobState.SUBMITTED;
      default:
        throw new IllegalArgumentException("Invalid enum constant"); // can never happen        
    }
  }

  public boolean isFinalState() {
    switch (this) {
      case FINISHED:
      case FAILED:
      case KILLED:
      case FRAMEWORK_FAILURE:
      case APP_MASTER_START_FAILED:
      case INITIALIZATION_FAILED:
        return true;
      default:
        return false;
    }
  }

  public static EnumSet<JobState> getRunningStates() {
    return EnumSet.
        of(INITIALIZING, RUNNING, ACCEPTED, NEW, NEW_SAVING, SUBMITTED, STARTING_APP_MASTER, AGGREGATING_LOGS);
  }

}
