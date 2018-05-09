/*
 * Copyright (C) 2013 - 2018, Logical Clocks AB and RISE SICS AB. All rights reserved
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this
 * software and associated documentation files (the "Software"), to deal in the Software
 * without restriction, including without limitation the rights to use, copy, modify, merge,
 * publish, distribute, sublicense, and/or sell copies of the Software, and to permit
 * persons to whom the Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or
 * substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS  OR IMPLIED, INCLUDING
 * BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL  THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM,
 * DAMAGES OR  OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 *
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
  APP_MASTER_START_FAILED("Failed starting AM"),
  GENERATING_CERTS("Generating certificate");

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
      case GENERATING_CERTS:
        return JobState.GENERATING_CERTS;
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
